// src/services/api.ts – PHIÊN BẢN CUỐI CÙNG, ĐẸP NHẤT, ĐÚNG NHẤT
import axios from 'axios';

const API_BASE_URL = '/api';      // User + Queue service (cùng backend)
const ETA_BASE_URL = '/';  // ETA riêng

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

export const etaApi = axios.create({
  baseURL: ETA_BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// Tự động logout khi 401
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.clear();
      window.location.href = '/';
    }
    return Promise.reject(err);
  }
);

export default api;