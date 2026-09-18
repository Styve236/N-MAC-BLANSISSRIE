import type { Role } from '../config/constants';

export interface UserRequestDTO {
  nom: string;
  tels: string;
  email: string;
  password?: string;
  role?: Role;
}

export interface UserDTO {
  idusers?: number;
  nom: string;
  tels: string;
  email: string;
  role: Role;
  actif: boolean;
  dateCreation?: string;
}