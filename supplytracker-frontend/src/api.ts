import axios, { AxiosError } from "axios";
import type {
  AuthResponse,
  DashboardStats,
  Farm,
  FarmWriteRequest,
  Facility,
  InspectionJob,
  LineageEdge,
  Organization,
  Product,
  ProductBatch,
  ProductPage,
  ProductWriteRequest,
  RecallCase,
  TrackingStage,
  UploadSlot,
} from "./types/api";
import { clearAuthState } from "./lib/auth";
import { normalizeApiError } from "./lib/apiErrors";

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
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
  (error: AxiosError) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor - Handle errors globally
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error: AxiosError) => {
    if (error.response) {
      // Server responded with error status
      switch (error.response.status) {
        case 401:
          // Unauthorized - clear auth and redirect to login
          clearAuthState();
          window.location.href = '/';
          break;
        case 403:
          console.error(normalizeApiError(error).message);
          break;
        case 404:
          console.error(normalizeApiError(error).message);
          break;
        case 500:
          console.error(normalizeApiError(error).message);
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

export async function login(username: string, password: string): Promise<AuthResponse> {
  const res = await apiClient.post<AuthResponse>('/auth/login', { username, password });
  return res.data;
}

export async function register(username: string, email: string, password: string): Promise<AuthResponse> {
  const res = await apiClient.post<AuthResponse>('/auth/register', { username, email, password });
  return res.data;
}

export async function getProducts(page = 0, size = 10, sortBy = "name", sortDir: "asc" | "desc" = "asc"): Promise<ProductPage> {
  const res = await apiClient.get<ProductPage>('/products', {
    params: { page, size, sortBy, sortDir }
  });
  return res.data;
}

export async function createProduct(product: ProductWriteRequest): Promise<Product> {
  const res = await apiClient.post<Product>('/products', product);
  return res.data;
}

export async function updateProduct(id: string, product: ProductWriteRequest): Promise<Product> {
  const res = await apiClient.put<Product>(`/products/${id}`, product);
  return res.data;
}

export async function deleteProduct(id: string): Promise<void> {
  await apiClient.delete(`/products/${id}`);
}

export async function addTrackingStage(productId: string, trackingStage: TrackingStage): Promise<Product> {
  const res = await apiClient.post<Product>(`/products/${productId}/tracking`, trackingStage);
  return res.data;
}

export async function getTrackingHistory(productId: string): Promise<TrackingStage[]> {
  const res = await apiClient.get<TrackingStage[]>(`/products/${productId}/tracking`);
  return res.data;
}

export async function getDashboardStats(): Promise<DashboardStats> {
  const res = await apiClient.get<DashboardStats>('/products/stats');
  return res.data;
}

// Farm API functions
export async function getFarms(): Promise<Farm[]> {
  const res = await apiClient.get<Farm[]>('/farms');
  return res.data;
}

export async function createFarm(farm: FarmWriteRequest): Promise<Farm> {
  const res = await apiClient.post<Farm>('/farms', farm);
  return res.data;
}

export async function updateFarm(id: string, farm: FarmWriteRequest): Promise<Farm> {
  const res = await apiClient.put<Farm>(`/farms/${id}`, farm);
  return res.data;
}

export async function deleteFarm(id: string): Promise<void> {
  await apiClient.delete(`/farms/${id}`);
}

export async function getOrganizations(): Promise<Organization[]> {
  const res = await apiClient.get<Organization[]>('/v2/organizations/mine');
  return res.data;
}

export async function createOrganization(name: string, slug: string): Promise<Organization> {
  const res = await apiClient.post<Organization>('/v2/organizations', { name, slug });
  return res.data;
}

export async function getFacilities(organizationId: string): Promise<Facility[]> {
  const res = await apiClient.get<Facility[]>(`/v2/organizations/${organizationId}/facilities`);
  return res.data;
}

export async function getBatches(organizationId: string): Promise<ProductBatch[]> {
  const res = await apiClient.get<ProductBatch[]>('/v2/batches', { params: { organizationId } });
  return res.data;
}

export async function getBatch(batchId: string): Promise<ProductBatch> {
  const res = await apiClient.get<ProductBatch>(`/v2/batches/${batchId}`);
  return res.data;
}

export async function getInspectionJobs(organizationId: string): Promise<InspectionJob[]> {
  const res = await apiClient.get<InspectionJob[]>('/v2/inspection-jobs', { params: { organizationId } });
  return res.data;
}

export async function requestInspectionUploadSlot(request: {
  organizationId: string;
  filename?: string;
  contentType: string;
  sizeBytes: number;
}): Promise<UploadSlot> {
  const res = await apiClient.post<UploadSlot>('/v2/inspection-jobs/upload-slot', request);
  return res.data;
}

export async function reviewInspectionJob(jobId: string, request: {
  action: 'ACCEPT' | 'CORRECT' | 'REJECT';
  correctedLabels?: string[];
  correctedClassification?: string;
  reason?: string;
}): Promise<InspectionJob> {
  const res = await apiClient.post<InspectionJob>(`/v2/inspection-jobs/${jobId}/reviews`, request);
  return res.data;
}

export async function getLineage(batchId: string): Promise<LineageEdge[]> {
  const res = await apiClient.get<LineageEdge[]>(`/v2/lineage/batches/${batchId}/downstream`);
  return res.data;
}

export async function createRecall(request: { sourceBatchId: string; reason: string; simulation: boolean }): Promise<RecallCase> {
  const res = await apiClient.post<RecallCase>('/v2/recalls', request);
  return res.data;
}

export async function getRecalls(organizationId: string): Promise<RecallCase[]> {
  const res = await apiClient.get<RecallCase[]>('/v2/recalls', { params: { organizationId } });
  return res.data;
}

// Export the configured axios instance for direct use if needed
export { apiClient };
