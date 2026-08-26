const TOKEN_KEY = 'token';
const USERNAME_KEY = 'username';
const ROLES_KEY = 'roles';

export interface AuthState {
  token: string | null;
  username: string | null;
  roles: string[];
}

export function getAuthState(): AuthState {
  return {
    token: localStorage.getItem(TOKEN_KEY),
    username: localStorage.getItem(USERNAME_KEY),
    roles: parseRoles(localStorage.getItem(ROLES_KEY)),
  };
}

export function clearAuthState(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem(ROLES_KEY);
}

export function hasAnyRole(requiredRoles: string[], roles = getAuthState().roles): boolean {
  return requiredRoles.length === 0 || requiredRoles.some((role) => roles.includes(role));
}

function parseRoles(raw: string | null): string[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed.map(String);
  } catch {
    return raw.split(',').map((role) => role.trim()).filter(Boolean);
  }
  return [];
}
