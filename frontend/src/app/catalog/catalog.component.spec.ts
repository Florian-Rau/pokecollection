import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CollectionApiService } from '../collection/collection-api.service';
import { AuthApiService } from '../core/auth-api.service';
import { CatalogApiService, PokemonPage } from './catalog-api.service';
import { CatalogComponent } from './catalog.component';

describe('CatalogComponent', () => {
  let fixture: ComponentFixture<CatalogComponent>;
  let component: CatalogComponent;
  let authApiService: Pick<AuthApiService, 'logout' | 'clearSession' | 'username' | 'isAuthenticated'>;
  let catalogApiService: Pick<CatalogApiService, 'getPage' | 'findByName'>;
  let collectionApiService: Pick<CollectionApiService, 'getCollection' | 'addToCollection'>;

  const bulbasaurPage: PokemonPage = {
    results: [
      {
        pokemonId: 1,
        name: 'bulbasaur',
        spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png'
      },
      {
        pokemonId: 4,
        name: 'charmander',
        spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png'
      }
    ],
    count: 1302,
    next: 'https://pokeapi.co/api/v2/pokemon?offset=20&limit=20',
    previous: null
  };
  const mewtwoPage: PokemonPage = {
    results: [
      {
        pokemonId: 150,
        name: 'mewtwo',
        spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/150.png'
      }
    ],
    count: 1302,
    next: 'https://pokeapi.co/api/v2/pokemon?offset=20&limit=20',
    previous: null
  };

  function getButton(label: string) {
    return [...fixture.nativeElement.querySelectorAll('button')]
      .find((button) => button.textContent?.includes(label)) as HTMLButtonElement | undefined;
  }

  function getPokemonActionButton(label: string) {
    return [...fixture.nativeElement.querySelectorAll('.catalog-item button')]
      .find((button) => button.textContent?.includes(label)) as HTMLButtonElement | undefined;
  }

  async function createComponent() {
    await TestBed.configureTestingModule({
      imports: [CatalogComponent],
      providers: [
        provideRouter([]),
        { provide: AuthApiService, useValue: authApiService },
        { provide: CatalogApiService, useValue: catalogApiService },
        { provide: CollectionApiService, useValue: collectionApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogComponent);
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    authApiService = {
      logout: vi.fn().mockReturnValue(of(void 0)),
      clearSession: vi.fn(),
      username: signal<string | null>('Chase'),
      isAuthenticated: signal(true)
    };

    catalogApiService = {
      getPage: vi.fn().mockReturnValue(of(bulbasaurPage)),
      findByName: vi.fn()
    };

    collectionApiService = {
      getCollection: vi.fn().mockReturnValue(of([])),
      addToCollection: vi.fn().mockReturnValue(of(void 0))
    };
  });

  it('loads the first catalog page when the trainer is already authenticated', async () => {
    await createComponent();

    fixture.detectChanges();

    expect(collectionApiService.getCollection).toHaveBeenCalled();
    expect(catalogApiService.getPage).toHaveBeenCalledWith(20, 0);
    expect(component.visibleResults()).toEqual(bulbasaurPage.results);
    expect(fixture.nativeElement.textContent).toContain('bulbasaur');
  });

  it('filters already loaded Pokémon client-side without an exact-name lookup', async () => {
    await createComponent();

    fixture.detectChanges();
    component.onQueryChange('char');
    fixture.detectChanges();

    expect(component.visibleResults().map((pokemon) => pokemon.name)).toEqual(['charmander']);
    expect(catalogApiService.findByName).not.toHaveBeenCalled();
  });

  it('falls back to an exact-name lookup when no loaded Pokémon match the search term', async () => {
    vi.mocked(catalogApiService.findByName).mockReturnValue(of({
      pokemonId: 6,
      name: 'charizard',
      spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/6.png'
    }));
    await createComponent();

    fixture.detectChanges();
    component.onQueryChange('charizard');
    component.submitSearch();
    fixture.detectChanges();

    expect(catalogApiService.findByName).toHaveBeenCalledWith('charizard');
    expect(component.visibleResults().map((pokemon) => pokemon.name)).toEqual(['charizard']);
  });

  it('still performs an exact-name lookup when the query is only a substring of a loaded Pokémon name', async () => {
    catalogApiService = {
      getPage: vi.fn().mockReturnValue(of(mewtwoPage)),
      findByName: vi.fn().mockReturnValue(of({
        pokemonId: 151,
        name: 'mew',
        spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/151.png'
      }))
    };
    await createComponent();

    fixture.detectChanges();
    component.onQueryChange('mew');
    component.submitSearch();
    fixture.detectChanges();

    expect(catalogApiService.findByName).toHaveBeenCalledWith('mew');
    expect(component.visibleResults().map((pokemon) => pokemon.name)).toEqual(['mew', 'mewtwo']);
  });

  it('shows the dedicated no-results message when the exact-name lookup returns 404', async () => {
    vi.mocked(catalogApiService.findByName).mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 404,
      error: { detail: 'Kein Pokémon mit dem Namen \'missingno\' gefunden.' }
    })));
    await createComponent();

    fixture.detectChanges();
    component.onQueryChange('missingno');
    component.submitSearch();
    fixture.detectChanges();

    expect(component.noResultsMessage()).toBe('Kein Pokémon mit diesem Namen gefunden.');
    expect(component.unavailable()).toBe(false);
  });

  it('marks the catalog unavailable when the exact-name lookup fails with a non-404 error', async () => {
    vi.mocked(catalogApiService.findByName).mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 502
    })));
    await createComponent();

    fixture.detectChanges();
    component.onQueryChange('pikachu');
    component.submitSearch();
    fixture.detectChanges();

    expect(component.unavailable()).toBe(true);
  });

  it('ignores stale exact-name lookup responses after the query changes', async () => {
    const response = new Subject<{ pokemonId: number; name: string; spriteUrl: string }>();
    vi.mocked(catalogApiService.findByName).mockReturnValue(response.asObservable());
    await createComponent();

    fixture.detectChanges();
    component.onQueryChange('pikachu');
    component.submitSearch();
    component.onQueryChange('eevee');

    response.next({
      pokemonId: 25,
      name: 'pikachu',
      spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png'
    });
    response.complete();
    fixture.detectChanges();

    expect(component.exactMatch()).toBeNull();
    expect(component.visibleResults().map((pokemon) => pokemon.name)).toEqual([]);
  });

  it('retries loading the collection ownership when the initial preload fails', async () => {
    const getCollection = vi.fn()
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 502 })))
      .mockReturnValueOnce(of([{ pokemonId: 1, pokemonName: 'bulbasaur', addedAt: '2026-09-19T00:00:00Z' }]));
    collectionApiService = {
      getCollection,
      addToCollection: vi.fn().mockReturnValue(of(void 0))
    };
    await createComponent();

    fixture.detectChanges();
    expect(component.unavailable()).toBe(true);

    component.retry();
    fixture.detectChanges();

    expect(getCollection).toHaveBeenCalledTimes(2);
    expect(component.unavailable()).toBe(false);
    expect(component.ownedPokemonIds().has(1)).toBe(true);
    expect(getPokemonActionButton('Gefangen')?.disabled).toBe(true);
  });

  it('disables every add control while any add request is in flight', async () => {
    const addSubject = new Subject<void>();
    collectionApiService = {
      getCollection: vi.fn().mockReturnValue(of([])),
      addToCollection: vi.fn().mockReturnValue(addSubject.asObservable())
    };
    await createComponent();

    fixture.detectChanges();
    getPokemonActionButton('Hinzufügen')?.click();
    fixture.detectChanges();

    const addButtons = [...fixture.nativeElement.querySelectorAll('.catalog-item button')] as HTMLButtonElement[];
    expect(addButtons.length).toBeGreaterThan(1);
    expect(addButtons.every((button) => button.disabled)).toBe(true);

    addSubject.next();
    addSubject.complete();
    fixture.detectChanges();
  });

  it('shows the catalog unavailable state and clears stale results when loading fails', async () => {
    catalogApiService = {
      getPage: vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({ status: 502 }))),
      findByName: vi.fn()
    };
    await createComponent();

    fixture.detectChanges();

    expect(component.unavailable()).toBe(true);
    expect(component.visibleResults()).toEqual([]);
    expect(fixture.nativeElement.textContent).toContain('Katalog ist momentan nicht verfügbar.');
  });

  it('ignores malformed paging links when advancing to the next page', async () => {
    const malformedNextPage: PokemonPage = {
      ...bulbasaurPage,
      next: 'not-a-valid-url'
    };
    catalogApiService = {
      getPage: vi.fn().mockReturnValue(of(malformedNextPage)),
      findByName: vi.fn()
    };
    await createComponent();

    fixture.detectChanges();
    const nextButton = getButton('Nächste Seite');

    expect(nextButton).toBeDefined();
    expect(() => nextButton?.click()).not.toThrow();
    expect(catalogApiService.getPage).toHaveBeenCalledTimes(1);
  });

  it('keeps the current page visible and shows an inline error when a later page load fails', async () => {
    const getPage = vi.fn()
      .mockReturnValueOnce(of(bulbasaurPage))
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 502 })));
    catalogApiService = {
      getPage,
      findByName: vi.fn()
    };
    await createComponent();

    fixture.detectChanges();
    const nextButton = getButton('Nächste Seite');
    nextButton?.click();
    fixture.detectChanges();

    expect(component.currentPage()).toEqual(bulbasaurPage);
    expect(component.visibleResults()).toEqual(bulbasaurPage.results);
    expect(component.pageLoadError()).toBe("Konnte die Seite nicht laden. Bitte versuche es erneut.");
    expect(component.unavailable()).toBe(false);
  });

  it('redirects to login when no local token is present', async () => {
    authApiService = {
      logout: vi.fn().mockReturnValue(of(void 0)),
      clearSession: vi.fn(),
      username: signal<string | null>(null),
      isAuthenticated: signal(false)
    };
    await createComponent();
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();

    expect(authApiService.clearSession).not.toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });


  it('preloads owned ids and disables add for pokemon already in the collection', async () => {
    collectionApiService = {
      getCollection: vi.fn().mockReturnValue(of([
        {
          pokemonId: 1,
          pokemonName: 'bulbasaur',
          addedAt: '2026-09-19T09:15:00Z'
        }
      ])),
      addToCollection: vi.fn().mockReturnValue(of(void 0))
    };
    await createComponent();

    fixture.detectChanges();

    const ownedButton = getPokemonActionButton('Gefangen');
    const addButton = getPokemonActionButton('Hinzufügen');

    expect(component.ownedPokemonIds().has(1)).toBe(true);
    expect(ownedButton?.disabled).toBe(true);
    expect(addButton?.disabled).toBe(false);
  });

  it('marks a pokemon as owned immediately after a successful add without reloading the page', async () => {
    await createComponent();

    fixture.detectChanges();
    getPokemonActionButton('Hinzufügen')?.click();
    fixture.detectChanges();

    expect(collectionApiService.addToCollection).toHaveBeenCalledWith(1);
    expect(component.ownedPokemonIds().has(1)).toBe(true);
    expect(catalogApiService.getPage).toHaveBeenCalledTimes(1);
    expect(collectionApiService.getCollection).toHaveBeenCalledTimes(1);
    expect(getPokemonActionButton('Gefangen')?.disabled).toBe(true);
  });

  it('marks the row owned when add returns 409 since the server is authoritative', async () => {
    collectionApiService = {
      getCollection: vi.fn().mockReturnValue(of([])),
      addToCollection: vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({
        status: 409,
        error: { detail: 'Pokémon mit der ID \'1\' befindet sich bereits in deiner Sammlung.' }
      })))
    };
    await createComponent();

    fixture.detectChanges();
    getPokemonActionButton('Hinzufügen')?.click();
    fixture.detectChanges();

    expect(component.addError()).toBe('Das Pokémon ist schon in Deiner Sammlung');
    expect(component.ownedPokemonIds().has(1)).toBe(true);
    expect(getPokemonActionButton('Gefangen')?.disabled).toBe(true);
  });

  it('shows an inline error when adding a pokemon fails due to service unavailability', async () => {
    collectionApiService = {
      getCollection: vi.fn().mockReturnValue(of([])),
      addToCollection: vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({ status: 502 })))
    };
    await createComponent();

    fixture.detectChanges();
    getPokemonActionButton('Hinzufügen')?.click();
    fixture.detectChanges();

    expect(component.addError()).toBe("Konnte das Pokémon gerade nicht hinzufügen, bitte versuche es erneut.");
    expect(component.ownedPokemonIds().has(1)).toBe(false);
  });
});
