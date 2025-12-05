// src/api.js
import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

export async function getProducts() {
  const res = await axios.get(`${API_BASE_URL}/products`);
  return res.data;
}
