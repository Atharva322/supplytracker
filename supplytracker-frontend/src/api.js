// src/api.js
import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - Add JWT token to all requests
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor - Handle errors globally
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response) {
      // Server responded with error status
      switch (error.response.status) {
        case 401:
          // Unauthorized - clear auth and redirect to login
          localStorage.removeItem('token');
          localStorage.removeItem('username');
          localStorage.removeItem('roles');
          window.location.href = '/';
          break;
        case 403:
          console.error('Access forbidden');
          break;
        case 404:
          console.error('Resource not found');
          break;
        case 500:
          console.error('Server error');
          break;
        default:
          console.error('Error:', error.response.status);
      }
    } else if (error.request) {
      // Request made but no response received
      console.error('Network error - no response received');
    } else {
      // Error setting up the request
      console.error('Request setup error:', error.message);
    }
    return Promise.reject(error);
  }
);

export async function login(username, password) {
  const res = await apiClient.post('/auth/login', { username, password });
  return res.data;
}

export async function register(username, email, password) {
  const res = await apiClient.post('/auth/register', { username, email, password });
  return res.data;
}

export async function getProducts(page = 0, size = 10, sortBy = "name", sortDir = "asc") {
  const res = await apiClient.get('/products', {
    params: { page, size, sortBy, sortDir }
  });
  return res.data;
}

export async function createProduct(product) {
  const res = await apiClient.post('/products', product);
  return res.data;
}

export async function updateProduct(id, product) {
  const res = await apiClient.put(`/products/${id}`, product);
  return res.data;
}

export async function deleteProduct(id) {
  await apiClient.delete(`/products/${id}`);
}

export async function addTrackingStage(productId, trackingStage) {
  const res = await apiClient.post(`/products/${productId}/tracking`, trackingStage);
  return res.data;
}

export async function getTrackingHistory(productId) {
  const res = await apiClient.get(`/products/${productId}/tracking`);
  return res.data;
}

export async function getDashboardStats() {
  const res = await apiClient.get('/products/stats');
  return res.data;
}

// Farm API functions
export async function getFarms() {
  const res = await apiClient.get('/farms');
  return res.data;
}

export async function createFarm(farm) {
  const res = await apiClient.post('/farms', farm);
  return res.data;
}

export async function updateFarm(id, farm) {
  const res = await apiClient.put(`/farms/${id}`, farm);
  return res.data;
}

export async function deleteFarm(id) {
  await apiClient.delete(`/farms/${id}`);
}

// Export the configured axios instance for direct use if needed
export { apiClient };
