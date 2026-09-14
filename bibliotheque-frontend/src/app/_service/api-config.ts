export function apiUrl(): string {
  return (window.__env && window.__env.apiUrl) || 'http://localhost:8080';
}
