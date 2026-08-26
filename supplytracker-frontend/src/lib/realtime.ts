export function apiBaseUrl(): string {
  return import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
}

export function websocketBaseUrl(): string {
  return apiBaseUrl().replace(/\/api\/?$/, '');
}
