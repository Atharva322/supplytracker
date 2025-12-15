// src/api.js
import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

// Add JWT token to all requests
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export async function login(username, password) {
  const res = await axios.post(`${API_BASE_URL}/auth/login`, { username, password });
  return res.data;
}

export async function register(username, email, password) {
  const res = await axios.post(`${API_BASE_URL}/auth/register`, { username, email, password });
  return res.data;
}

export async function getProducts(page = 0, size = 10, sortBy = "name", sortDir = "asc") {
  const res = await axios.get(`${API_BASE_URL}/products`, {
    params: { page, size, sortBy, sortDir }
  });
  return res.data;
}

export async function createProduct(product) {
  const res = await axios.post(`${API_BASE_URL}/products`, product);
  return res.data;
}

export async function updateProduct(id, product) {
  const res = await axios.put(`${API_BASE_URL}/products/${id}`, product);
  return res.data;
}

export async function deleteProduct(id) {
  await axios.delete(`${API_BASE_URL}/products/${id}`);
}

export async function addTrackingStage(productId, trackingStage) {
  const res = await axios.post(`${API_BASE_URL}/products/${productId}/tracking`, trackingStage);
  return res.data;
}

export async function getTrackingHistory(productId) {
  const res = await axios.get(`${API_BASE_URL}/products/${productId}/tracking`);
  return res.data;
}

export async function getDashboardStats() {
  const res = await axios.get(`${API_BASE_URL}/products/stats`);
  return res.data;
}

// Farm API functions
export async function getFarms() {
  const res = await axios.get(`${API_BASE_URL}/farms`);
  return res.data;
}

export async function createFarm(farm) {
  const res = await axios.post(`${API_BASE_URL}/farms`, farm);
  return res.data;
}

export async function updateFarm(id, farm) {
  const res = await axios.put(`${API_BASE_URL}/farms/${id}`, farm);
  return res.data;
}

export async function deleteFarm(id) {
  await axios.delete(`${API_BASE_URL}/farms/${id}`);
}
