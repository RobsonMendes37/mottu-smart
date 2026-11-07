export type UserRole = 'MANAGER' | 'MECHANIC' | 'RIDER';
export type AccountStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'REJECTED';

export interface AuthUser {
  userId: string;
  fullName: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  userId: string;
  role: UserRole;
  fullName: string;
}

export interface RegisterRequest {
  fullName: string;
  cpf: string;
  phone: string;
  email: string;
  password: string;
  role: 'RIDER' | 'MECHANIC';
}

export interface RegisterResponse {
  userId: string;
  status: AccountStatus;
  message: string;
}

export interface PublicConfigResponse {
  publicDemoMode: boolean;
}
