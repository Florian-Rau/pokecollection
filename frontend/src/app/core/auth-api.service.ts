import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject } from '@angular/core';
import { of, tap } from 'rxjs';
import { AuthStateService } from './auth-state.service';
import { ProblemDetail } from './problem-detail';

export interface RegisterPayload {
  name: string;
  password: string;
}

export interface LoginPayload {
  name: string;
  password: string;
}

export interface AuthenticationResponse {
  username: string;
  token: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly authState = inject(AuthStateService);

  readonly username = computed(() => this.authState.username());
  readonly isAuthenticated = computed(() => this.authState.token() !== null);

  register(payload: RegisterPayload) {
    return this.http.post<AuthenticationResponse>('/api/auth/register', payload).pipe(
      tap((session) => this.storeSession(session))
    );
  }

  login(payload: LoginPayload) {
    return this.http.post<AuthenticationResponse>('/api/auth/login', payload).pipe(
      tap((session) => this.storeSession(session))
    );
  }

  logout() {
    return this.http.post<void>('/api/auth/logout', {}).pipe(
      tap(() => this.clearSession())
    );
  }

  clearSession() {
    this.authState.clear();
  }

  private storeSession(session: AuthenticationResponse) {
    if (!session.token) {
      throw new Error('The authentication response did not contain a token.');
    }

    this.authState.setSession(session.token, session.username);
  }

  static extractDetail(error: unknown, fallback: string): string {
    if (typeof error === 'object' && error !== null && 'error' in error) {
      const payload = (error as { error?: ProblemDetail }).error;
      if (payload?.detail) {
        return payload.detail;
      }
    }

    return fallback;
  }
}
