export interface LoginResponse {
  accessToken: string
}

export interface RegisterRequest {
  email: string
  fullName: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface User{
  id: string
  email: string
  fullName: string,
  phone?: string,
  photoUrl?: string
}


export interface UpdateProfileCommand{
  email: string
  fullName: string,
  phone?: string,
  photoUrl?: string
}

export interface ChangePasswordCommand{
  currentPassword: string,
  newPassword: string,
  confirmPassword: string,
}