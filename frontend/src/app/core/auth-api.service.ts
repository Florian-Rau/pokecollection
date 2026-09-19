import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { tap } from 'rxjs';
import { ProblemDetail } from './problem-detail';

export interface RegisterPayload {
  name: string;
  password: string;
}

export interface LoginPayload {
  name: string;
  password: string;
}

export interface TrainerSession {
  username: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly usernameSignal = signal<string | null>(null);

  readonly username = computed(() => this.usernameSignal());
  readonly isAuthenticated = computed(() => this.usernameSignal() !== null);

  register(payload: RegisterPayload) {
    return this.http.post<TrainerSession>('/api/auth/register', payload).pipe(
      tap((session) => this.usernameSignal.set(session.username))
    );
  }

  login(payload: LoginPayload) {
    return this.http.post<TrainerSession>('/api/auth/login', payload).pipe(
      tap((session) => this.usernameSignal.set(session.username))
    );
  }

  logout() {
    return this.http.post<void>('/api/auth/logout', {}).pipe(
      tap(() => this.clearSession())
    );
  }

  loadSession() {
    return this.http.get<TrainerSession>('/api/auth/session').pipe(
      tap((session) => this.usernameSignal.set(session.username))
    );
  }

  clearSession() {
    this.usernameSignal.set(null);
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
