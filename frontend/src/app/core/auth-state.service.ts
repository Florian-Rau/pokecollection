import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthStateService {
  readonly token = signal<string | null>(null);
  readonly username = signal<string | null>(null);

  setSession(token: string, username: string) {
    this.token.set(token);
    this.username.set(username);
  }

  clear() {
    this.token.set(null);
    this.username.set(null);
  }
}
