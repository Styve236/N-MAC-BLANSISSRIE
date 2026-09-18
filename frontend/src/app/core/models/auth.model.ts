import type { Role } from '../config/constants';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshTokenRequestDTO {
  refreshToken: string;
}

export interface ConnexionResponse {
  token?: string;
  refresh_token?: string;
  email: string;
  nom: string;
  role: Role;
  actif: boolean;
  expires_in?: number;
}