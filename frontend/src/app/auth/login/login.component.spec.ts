import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthApiService } from '../../core/auth-api.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authApiService: Pick<AuthApiService, 'login' | 'username' | 'isAuthenticated'>;

  beforeEach(async () => {
    authApiService = {
      login: vi.fn(),
      username: signal<string | null>(null),
      isAuthenticated: signal(false)
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthApiService, useValue: authApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('does not call the API when required fields are missing', () => {
    component.form.setValue({
      name: '',
      password: ''
    });

    component.submit();

    expect(authApiService.login).not.toHaveBeenCalled();
  });

  it('navigates to the collection route after successful login', () => {
    vi.mocked(authApiService.login).mockReturnValue(of({ username: 'Chase', token: 'token' }));
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.form.setValue({
      name: ' Chase ',
      password: 'hunter22'
    });

    component.submit();

    expect(authApiService.login).toHaveBeenCalledWith({
      name: 'Chase',
      password: 'hunter22'
    });
    expect(navigateSpy).toHaveBeenCalledWith(['/collection']);
  });

  it('surfaces problem detail text from failed login responses', () => {
    vi.mocked(authApiService.login).mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 401,
      error: { detail: 'Ungültiger Trainer Name oder Passwort.' }
    })));

    component.form.setValue({
      name: 'Chase',
      password: 'wrongpass'
    });

    component.submit();

    expect(component.serverError()).toBe('Ungültiger Trainer Name oder Passwort.');
  });
});
