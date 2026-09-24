import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { CatalogApiService } from './catalog-api.service';
import { authInterceptor } from '../core/auth.interceptor';
import { AuthStateService } from '../core/auth-state.service';

describe('CatalogApiService', () => {
  let service: CatalogApiService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CatalogApiService,
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(CatalogApiService);
    httpTestingController = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthStateService).setSession('test-token', 'Chase');
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('loads catalog pages with explicit paging params', () => {
    service.getPage(20, 40).subscribe();

    const request = httpTestingController.expectOne('/api/pokemon?limit=20&offset=40');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer test-token');
    request.flush({
      results: [],
      count: 0,
      next: null,
      previous: null
    });
  });

  it('normalizes exact-name lookups', () => {
    service.findByName(' Pikachu ').subscribe();

    const request = httpTestingController.expectOne('/api/pokemon/pikachu');
    expect(request.request.method).toBe('GET');
    request.flush({
      pokemonId: 25,
      name: 'pikachu',
      spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png'
    });
  });
});
