// src/types/index.ts
export interface User {
  userId: string;
  email: string;
  name: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface CreateUserRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
  emailNotificationEnabled?: boolean;
  smsNotificationEnabled?: boolean;
}

export interface QueueInfo {
  queueId: string;
  name: string;
  description: string;
  currentWaitingCount: number;
  averageServiceTimeMinutes: number;
  isActive: boolean;
}

export interface Ticket {
  ticketId: string;
  queueId: string;
  userId: string;
  userEmail: string;
  userName: string;
  userPhone?: string;
  status: 'WAITING' | 'SERVED' | 'CANCELLED' | 'NOTIFIED';
  position: number;
  joinedAt: string;
  estimatedWaitMinutes?: number;
}

export interface EtaResponse {
  estimatedWaitMinutes: number;
  p90WaitMinutes?: number;
  p50WaitMinutes?: number;
}

export interface JoinQueueRequest {
  userId: string;
}