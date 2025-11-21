import api, { etaApi } from './api';
import { JoinQueueRequest, Ticket, QueueInfo, EtaResponse } from '../types';

type CachedEta = EtaResponse & { savedAt: number; updatedAt?: string | number | null };

const cacheKeyFor = (ticketId: string) => `eta_${ticketId}`;

const readCache = (ticketId: string): CachedEta | null => {
  const raw = localStorage.getItem(cacheKeyFor(ticketId));
  if (!raw) return null;
  try {
    const obj = JSON.parse(raw);
    // Backfill savedAt if missing
    if (obj && typeof obj.savedAt !== 'number') {
      obj.savedAt = Date.now();
    }
    return obj;
  } catch {
    return null;
  }
};

const writeCache = (ticketId: string, eta: CachedEta) => {
  localStorage.setItem(cacheKeyFor(ticketId), JSON.stringify(eta));
};

// Compute local countdown from cache without calling API
const computeLocalRemaining = (cached: CachedEta): EtaResponse => {
  const base =
    (cached.remainingMinutes ?? cached.estimatedWaitMinutes ?? 0) as number;
  const minutesPassed = Math.floor((Date.now() - cached.savedAt) / 60000);
  const newRemaining = Math.max(0, base - Math.max(0, minutesPassed));
  const result: CachedEta = {
    estimatedWaitMinutes: cached.estimatedWaitMinutes ?? null,
    remainingMinutes: newRemaining,
    savedAt: Date.now(),
    updatedAt: cached.updatedAt ?? null
  };
  return result;
};

export const queueService = {
  async getQueues(): Promise<QueueInfo[]> {
    const response = await api.get('/queues');
    return response.data;
  },

  async getQueue(queueId: string): Promise<QueueInfo> {
    const response = await api.get(`/queues/${queueId}`);
    return response.data;
  },

  async joinQueue(queueId: string, userId: string): Promise<Ticket> {
    const joinRequest: JoinQueueRequest = { userId };
    const response = await api.post(`/queues/${queueId}/join`, joinRequest);
    return response.data;
  },

  async getQueueStatus(queueId: string, ticketId: string): Promise<Ticket> {
    const response = await api.get(`/queues/${queueId}/status/${ticketId}`);
    return response.data;
  },

  // Single method: calls /eta/track only when explicitly used (e.g., after join)
  async trackETA(
    queueId: string,
    ticketId: string,
    position: number,
    customerEmail: string,
    force: boolean = false
  ): Promise<EtaResponse> {
    const cacheKey = cacheKeyFor(ticketId);
    if (!force) {
      const cached = readCache(ticketId);
      if (cached) {
        const computed = computeLocalRemaining(cached) as CachedEta;
        writeCache(ticketId, { ...computed, savedAt: Date.now() } as CachedEta);
        return computed;
      }
    }

    const params = { queueId, ticketId, position, customerEmail };
    const response = await etaApi.get('/eta/track', { params });

    const eta: CachedEta = {
      estimatedWaitMinutes:
        response.data.remainingMinutes ??
        response.data.estimatedWaitMinutes ??
        response.data.eta ??
        null,
      remainingMinutes:
        response.data.remainingMinutes ??
        response.data.estimatedWaitMinutes ??
        null,
      updatedAt: response.data.updatedAt ?? null,
      savedAt: Date.now()
    };

    writeCache(ticketId, eta);
    return eta;
  },

  // Expose a helper to get ETA from cache only
  getCachedEta(ticketId: string): EtaResponse | null {
    const cached = readCache(ticketId);
    if (!cached) return null;
    const computed = computeLocalRemaining(cached) as CachedEta;
    // Persist the recomputed value so future reads are incremental
    writeCache(ticketId, { ...computed, savedAt: Date.now() } as CachedEta);
    return computed;
  },

  async processNext(queueId: string, count: number = 1): Promise<any> {
    const response = await api.post(`/queues/${queueId}/next`, { count });
    return response.data;
  },

  async createQueue(queueData: Partial<QueueInfo>): Promise<QueueInfo> {
    const response = await api.post('/queues', queueData);
    return response.data;
  },
};
