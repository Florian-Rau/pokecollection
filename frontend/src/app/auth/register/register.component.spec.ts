import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthApiService } from '../../core/auth-api.service';
import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let authApiService: Pick<AuthApiService, 'register' | 'username' | 'isAuthenticated'>;
  let router: Pick<Router, 'navigate'>;

  beforeEach(async () => {
    authApiService = {
      register: vi.fn(),
      username: signal<string | null>(null),
      isAuthenticated: signal(false)
    };
    router = {
      navigate: vi.fn().mockResolvedValue(true)
    };

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        { provide: AuthApiService, useValue: authApiService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('shows a client-side mismatch error without calling the API', () => {
    component.form.setValue({
      name: 'Chase',
      password: 'hunter22',
      confirmPassword: 'hunter33'
    });

    component.submit();

    expect(component.serverError()).toBe('Passwörter müssen übereinstimmen.');
    expect(authApiService.register).not.toHaveBeenCalled();
  });

  it('navigates to the collection route after successful registration', () => {
    vi.mocked(authApiService.register).mockReturnValue(of({ username: 'Chase', token: 'token' }));
    component.form.setValue({
      name: 'Chase',
      password: 'hunter22',
      confirmPassword: 'hunter22'
    });

    component.submit();

    expect(authApiService.register).toHaveBeenCalledWith({
      name: 'Chase',
      password: 'hunter22'
    });
    expect(router.navigate).toHaveBeenCalledWith(['/collection']);
  });

  it('surfaces problem detail text from failed registration responses', () => {
    vi.mocked(authApiService.register).mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { detail: 'Trainername \'Chase\' ist bereits registriert.' }
    })));
    component.form.setValue({
      name: 'Chase',
      password: 'hunter22',
      confirmPassword: 'hunter22'
    });

    component.submit();

    expect(component.serverError()).toBe("Trainername 'Chase' ist bereits registriert.");
  });
});
