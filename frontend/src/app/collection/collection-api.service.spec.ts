import { provideHttpClient, withXsrfConfiguration } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CollectionApiService } from './collection-api.service';

describe('CollectionApiService', () => {
  let service: CollectionApiService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CollectionApiService,
        provideHttpClient(withXsrfConfiguration({
          cookieName: 'XSRF-TOKEN',
          headerName: 'X-XSRF-TOKEN'
        })),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(CollectionApiService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('loads the authenticated trainer collection', () => {
    service.getCollection().subscribe();

    const request = httpTestingController.expectOne('/api/collection');
    expect(request.request.method).toBe('GET');
    request.flush([
      {
        pokemonId: 25,
        pokemonName: 'pikachu',
        addedAt: '2026-09-19T09:15:00Z'
      }
    ]);
  });

  it('posts a pokemon id to add it to the authenticated trainer collection', () => {
    service.addToCollection(25).subscribe((response) => {
      expect(response).toBeNull();
    });

    const request = httpTestingController.expectOne('/api/collection');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ pokemonId: 25 });
    request.flush(null);
  });
});
