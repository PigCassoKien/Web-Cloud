// User types
export interface User {
  userId: string;
  email: string;
  phone: string;
  name: string;
  emailNotificationEnabled: boolean;
  smsNotificationEnabled: boolean;
  createdAt: string;
  lastLoginAt: string;
  isActive: boolean;
}

export interface CreateUserRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
  emailNotificationEnabled?: boolean;
  smsNotificationEnabled?: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserResponse {
  userId: string;
  name: string;
  email: string;
  phone: string;
  emailNotificationEnabled: boolean;
  smsNotificationEnabled: boolean;
  createdAt: string;
  lastLoginAt: string;
  isActive: boolean;
}

// Queue types
export interface QueueInfo {
  queueId: string;
  name: string;
  description: string;
  isActive: boolean;
  currentSize: number;
  openSlots: number;
  serviceRateEma: number;
  createdAt: string;
  updatedAt: string;
}

export interface Ticket {
  ticketId: string;
  queueId: string;
  userId: string;
  userEmail: string;
  userPhone: string;
  userName: string;
  status: 'WAITING' | 'SERVED' | 'CANCELLED' | 'EXPIRED' | 'NOTIFIED';
  position: number;
  joinedAt: string;
  updatedAt?: string;
  servedAt?: string;
  estimatedWaitMinutes: number;
}

export interface JoinQueueRequest {
  userId: string;
}

export interface JoinQueueResponse {
  ticketId: string;
  queueId: string;
  position: number;
  estimatedWaitMinutes: number;
  message: string;
}

export interface QueueStatusResponse {
  ticketId: string;
  queueId: string;
  status: string;
  position: number;
  estimatedWaitMinutes: number;
  p90WaitMinutes: number;
  p50WaitMinutes: number;
  updatedAt: string;
}

// ETA types
export interface EtaResponse {
  queueId: string;
  ticketId: string;
  estimatedWaitMinutes: number;
  p90WaitMinutes: number;
  p50WaitMinutes: number;
  serviceRate: number;
  updatedAt: string;
}

// API Response wrapper
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}