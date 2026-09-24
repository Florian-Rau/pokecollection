import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { CollectionComponent } from './collection/collection.component';
import { provideRouter } from '@angular/router';
import { AuthStateService } from './core/auth-state.service';

describe('app routes', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthStateService).setSession('test-token', 'Chase');
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('redirects the root path to /collection, which resolves to CollectionComponent', async () => {
    const harness = await RouterTestingHarness.create('/');

    httpMock.expectOne('/api/collection').flush([]);

    expect(harness.routeDebugElement?.componentInstance).toBeInstanceOf(CollectionComponent);
  });

  it('resolves /collection directly to CollectionComponent', async () => {
    const harness = await RouterTestingHarness.create('/collection');

    httpMock.expectOne('/api/collection').flush([]);

    expect(harness.routeDebugElement?.componentInstance).toBeInstanceOf(CollectionComponent);
  });
});
