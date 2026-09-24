import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthApiService } from '../core/auth-api.service';
import { CollectionApiService, CollectionEntryDto } from './collection-api.service';
import { CollectionComponent } from './collection.component';

describe('CollectionComponent', () => {
  let fixture: ComponentFixture<CollectionComponent>;
  let component: CollectionComponent;
  let authApiService: Pick<AuthApiService, 'logout' | 'clearSession' | 'username' | 'isAuthenticated'>;
  let collectionApiService: Pick<CollectionApiService, 'getCollection'>;

  const collectionEntries: CollectionEntryDto[] = [
    {
      pokemonId: 25,
      pokemonName: 'pikachu',
      spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png',
      addedAt: '2026-09-19T09:15:00Z'
    },
    {
      pokemonId: 133,
      pokemonName: 'eevee',
      spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/133.png',
      addedAt: '2026-09-20T11:30:00Z'
    }
  ];

  function getButton(label: string) {
    return [...fixture.nativeElement.querySelectorAll('button')]
      .find((button) => button.textContent?.includes(label)) as HTMLButtonElement | undefined;
  }

  async function createComponent(service: typeof authApiService) {
    await TestBed.configureTestingModule({
      imports: [CollectionComponent],
      providers: [
        provideRouter([]),
        { provide: AuthApiService, useValue: service },
        { provide: CollectionApiService, useValue: collectionApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CollectionComponent);
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    collectionApiService = {
      getCollection: vi.fn().mockReturnValue(of(collectionEntries))
    };
  });

  it('loads the collection immediately when already authenticated', async () => {
    authApiService = {
      logout: vi.fn().mockReturnValue(of(void 0)),
      clearSession: vi.fn(),
      username: signal<string | null>('Chase'),
      isAuthenticated: signal(true)
    };
    await createComponent(authApiService);

    fixture.detectChanges();

    expect(collectionApiService.getCollection).toHaveBeenCalled();
    expect(component.loading()).toBe(false);
    expect(component.collection()).toEqual(collectionEntries);
    expect(fixture.nativeElement.textContent).toContain('pikachu');
    expect(fixture.nativeElement.textContent).toContain('eevee');

    const timeElements = [...fixture.nativeElement.querySelectorAll('time')] as HTMLTimeElement[];
    expect(timeElements).toHaveLength(2);
    expect(timeElements[0].getAttribute('datetime')).toBe('2026-09-19T09:15:00Z');
    expect(timeElements[1].getAttribute('datetime')).toBe('2026-09-20T11:30:00Z');
  });

  it('redirects to login when no local token is present', async () => {
    authApiService = {
      logout: vi.fn().mockReturnValue(of(void 0)),
      clearSession: vi.fn(),
      username: signal<string | null>(null),
      isAuthenticated: signal(false)
    };
    await createComponent(authApiService);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();

    expect(collectionApiService.getCollection).not.toHaveBeenCalled();
    expect(component.loading()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('shows the empty state when the trainer has not collected any Pokémon yet', async () => {
    collectionApiService = {
      getCollection: vi.fn().mockReturnValue(of([]))
    };
    authApiService = {
      logout: vi.fn().mockReturnValue(of(void 0)),
      clearSession: vi.fn(),
      username: signal<string | null>('Chase'),
      isAuthenticated: signal(true)
    };
    await createComponent(authApiService);

    fixture.detectChanges();

    expect(component.collection()).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain("Du hast noch keine Pokémon gefangen.");
  });

  it('shows the unavailable state when the collection fetch fails', async () => {
    collectionApiService = {
      getCollection: vi.fn().mockReturnValue(throwError(() => new Error('network error')))
    };
    authApiService = {
      logout: vi.fn().mockReturnValue(of(void 0)),
      clearSession: vi.fn(),
      username: signal<string | null>('Chase'),
      isAuthenticated: signal(true)
    };
    await createComponent(authApiService);

    fixture.detectChanges();

    expect(component.unavailable()).toBe(true);
    expect(component.collection()).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain('Deine Sammlung ist momentan nicht verfügbar.');
  });

  it('retries loading the collection after a fetch failure', async () => {
    collectionApiService = {
      getCollection: vi.fn()
        .mockReturnValueOnce(throwError(() => new Error('network error')))
        .mockReturnValueOnce(of(collectionEntries))
    };
    authApiService = {
      logout: vi.fn().mockReturnValue(of(void 0)),
      clearSession: vi.fn(),
      username: signal<string | null>('Chase'),
      isAuthenticated: signal(true)
    };
    await createComponent(authApiService);

    fixture.detectChanges();
    getButton('Erneut versuchen')?.click();
    fixture.detectChanges();

    expect(collectionApiService.getCollection).toHaveBeenCalledTimes(2);
    expect(component.unavailable()).toBe(false);
    expect(component.collection()).toEqual(collectionEntries);
  });

  it('redirects to /login when no local token is present', async () => {
    authApiService = {
      logout: vi.fn().mockReturnValue(of(void 0)),
      clearSession: vi.fn(),
      username: signal<string | null>(null),
      isAuthenticated: signal(false)
    };
    await createComponent(authApiService);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();

    expect(authApiService.clearSession).not.toHaveBeenCalled();
    expect(component.loading()).toBe(false);
    expect(collectionApiService.getCollection).not.toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('logs out and redirects to /login when the action is clicked', async () => {
    authApiService = {
      logout: vi.fn().mockReturnValue(of(void 0)),
      clearSession: vi.fn(),
      username: signal<string | null>('Chase'),
      isAuthenticated: signal(true)
    };
    await createComponent(authApiService);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();
    getButton('Ausloggen')?.click();

    expect(authApiService.logout).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('clears the local session and still redirects to /login when the logout request fails', async () => {
    authApiService = {
      logout: vi.fn().mockReturnValue(throwError(() => new Error('network error'))),
      clearSession: vi.fn(),
      username: signal<string | null>('Chase'),
      isAuthenticated: signal(true)
    };
    await createComponent(authApiService);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();
    getButton('Ausloggen')?.click();

    expect(authApiService.logout).toHaveBeenCalled();
    expect(authApiService.clearSession).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });
});
