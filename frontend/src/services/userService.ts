// src/services/userService.ts – BẢN CUỐI CÙNG, KHÔNG BAO GIỜ LỖI NỮA
import api from './api';
import { User } from '../types/index';

const USER_API_BASE = '/v1/users'; // ← base path chuẩn cho user service

export const userService = {
  async register(data: any): Promise<User> {
    const res = await api.post(`${USER_API_BASE}/register`, data);
    const userRes = res.data as User;

    this.setUserInfo(userRes.userId, userRes.email, userRes.name || '');
    return userRes;
  },

  async login(credentials: { email: string; password: string }): Promise<User> {
    const res = await api.post(`${USER_API_BASE}/login`, credentials);

    if (!res.data || !res.data.user) {
      throw new Error('Invalid login response from server');
    }

    const userData: User = res.data.user;
    this.setUserInfo(userData.userId, userData.email, userData.name || '');
    return userData;
  },

  setUserInfo(userId: string, email: string, name: string = '') {
    const user: User = { userId, email, name };
    localStorage.setItem('currentUser', JSON.stringify(user));
    localStorage.setItem('userEmail', email);
  },

  getCurrentUser(): User | null {
    try {
      const data = localStorage.getItem('currentUser');
      return data ? JSON.parse(data) : null;
    } catch (e) {
      console.warn('Failed to parse currentUser from localStorage', e);
      return null;
    }
  },

  logout() {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('userEmail');
  },

  // Lấy tất cả ticket của user hiện tại
  async getMyTickets(userId: string) {
    const res = await api.get(`${USER_API_BASE}/${userId}/tickets`);
    return res.data;
  },
};
