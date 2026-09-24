import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CollectionApiService } from '../collection/collection-api.service';
import { AuthApiService } from '../core/auth-api.service';
import { CatalogApiService, PokemonPage, PokemonSummary } from './catalog-api.service';

interface LoadedPage {
  offset: number;
  page: PokemonPage;
}

@Component({
  selector: 'app-catalog',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.css'
})
export class CatalogComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly catalogApi = inject(CatalogApiService);
  private readonly collectionApi = inject(CollectionApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly username = computed(() => this.authApi.username());
  readonly sessionLoading = signal(true);
  readonly catalogLoading = signal(false);
  readonly searchingExactName = signal(false);
  readonly query = signal('');
  readonly unavailable = signal(false);
  readonly noResultsMessage = signal<string | null>(null);
  readonly pageLoadError = signal<string | null>(null);
  readonly addError = signal<string | null>(null);
  readonly currentPage = signal<PokemonPage | null>(null);
  readonly loadedPages = signal<LoadedPage[]>([]);
  readonly exactMatch = signal<PokemonSummary | null>(null);
  readonly ownedPokemonIds = signal<Set<number>>(new Set());
  readonly addingPokemonId = signal<number | null>(null);
  readonly currentLimit = signal(20);
  readonly currentOffset = signal(0);
  readonly retryLimit = signal(20);
  readonly retryOffset = signal(0);

  readonly loadedResults = computed(() => {
    const byId = new Map<number, PokemonSummary>();

    for (const loadedPage of this.loadedPages()) {
      for (const pokemon of loadedPage.page.results) {
        byId.set(pokemon.pokemonId, pokemon);
      }
    }

    return [...byId.values()].sort((left, right) => left.pokemonId - right.pokemonId);
  });

  readonly filteredLoadedResults = computed(() => {
    const term = this.normalizedQuery();
    const currentPage = this.currentPage();

    if (!term) {
      return currentPage?.results ?? [];
    }

    return this.loadedResults().filter((pokemon) => pokemon.name.toLowerCase().includes(term));
  });

  readonly visibleResults = computed(() => {
    const results = [...this.filteredLoadedResults()];
    const exactMatch = this.exactMatch();

    if (exactMatch && !results.some((pokemon) => pokemon.pokemonId === exactMatch.pokemonId)) {
      results.unshift(exactMatch);
    }

    return results;
  });

  readonly totalCount = computed(() => this.currentPage()?.count ?? 0);
  readonly currentRangeStart = computed(() => {
    const currentPage = this.currentPage();
    if (!currentPage || currentPage.results.length === 0) {
      return 0;
    }

    return this.currentOffset() + 1;
  });
  readonly currentRangeEnd = computed(() => this.currentOffset() + (this.currentPage()?.results.length ?? 0));

  ngOnInit() {
    if (this.authApi.isAuthenticated()) {
      this.sessionLoading.set(false);
      this.loadCollectionOwnershipAndPage();
      return;
    }

    this.sessionLoading.set(false);
    void this.router.navigate(['/login']);
  }

  onQueryChange(value: string) {
    this.query.set(value);
    this.noResultsMessage.set(null);

    const normalizedQuery = this.normalizedQuery();
    if (!normalizedQuery || this.exactMatch()?.name !== normalizedQuery) {
      this.exactMatch.set(null);
    }
  }

  submitSearch() {
    const normalizedQuery = this.normalizedQuery();
    this.noResultsMessage.set(null);
    this.pageLoadError.set(null);

    if (!normalizedQuery) {
      this.exactMatch.set(null);
      return;
    }

    const hasExactLoadedMatch = this.loadedResults()
      .some((pokemon) => pokemon.name.toLowerCase() === normalizedQuery);
    if (hasExactLoadedMatch) {
      this.exactMatch.set(null);
      return;
    }

    this.searchingExactName.set(true);
    const requestedTerm = normalizedQuery;

    this.catalogApi.findByName(requestedTerm)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (pokemon) => {
          this.searchingExactName.set(false);

          if (this.normalizedQuery() !== requestedTerm) {
            return;
          }

          this.unavailable.set(false);
          this.noResultsMessage.set(null);
          this.exactMatch.set(pokemon);
        },
        error: (error: unknown) => {
          this.searchingExactName.set(false);

          if (this.normalizedQuery() !== requestedTerm) {
            return;
          }

          if (error instanceof HttpErrorResponse && error.status === 404) {
            this.unavailable.set(false);
            this.exactMatch.set(null);
            this.noResultsMessage.set('Kein Pokémon mit diesem Namen gefunden.');
            return;
          }

          this.showCatalogUnavailable();
        }
      });
  }

  addToCollection(pokemon: PokemonSummary) {
    if (this.ownedPokemonIds().has(pokemon.pokemonId) || this.addingPokemonId() !== null) {
      return;
    }

    this.addError.set(null);
    this.addingPokemonId.set(pokemon.pokemonId);

    this.collectionApi.addToCollection(pokemon.pokemonId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.addingPokemonId.set(null);
          this.ownedPokemonIds.update((ownedPokemonIds) => {
            const nextOwnedPokemonIds = new Set(ownedPokemonIds);
            nextOwnedPokemonIds.add(pokemon.pokemonId);
            return nextOwnedPokemonIds;
          });
        },
        error: (error: unknown) => {
          this.addingPokemonId.set(null);

          if (error instanceof HttpErrorResponse && error.status === 409) {
            this.addError.set('Das Pokémon ist schon in Deiner Sammlung');
            this.ownedPokemonIds.update((ownedPokemonIds) => {
              const nextOwnedPokemonIds = new Set(ownedPokemonIds);
              nextOwnedPokemonIds.add(pokemon.pokemonId);
              return nextOwnedPokemonIds;
            });
            return;
          }

          this.addError.set("Konnte das Pokémon gerade nicht hinzufügen, bitte versuche es erneut.");
        }
      });
  }

  retry() {
    if (this.currentPage() === null && this.ownedPokemonIds().size === 0 && this.loadedPages().length === 0) {
      this.loadCollectionOwnershipAndPage();
      return;
    }

    this.loadPage(this.retryLimit(), this.retryOffset());
  }

  loadNextPage() {
    const nextPage = this.parsePagingLink(this.currentPage()?.next ?? null);
    if (nextPage) {
      this.loadPage(nextPage.limit, nextPage.offset);
    }
  }

  loadPreviousPage() {
    const previousPage = this.parsePagingLink(this.currentPage()?.previous ?? null);
    if (previousPage) {
      this.loadPage(previousPage.limit, previousPage.offset);
    }
  }

  logout() {
    this.authApi.logout()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          void this.router.navigate(['/login']);
        },
        error: () => {
          this.authApi.clearSession();
          void this.router.navigate(['/login']);
        }
      });
  }

  private loadPage(limit: number, offset: number) {
    const hasLoadedPage = this.currentPage() !== null;
    this.catalogLoading.set(true);
    this.unavailable.set(false);
    this.addError.set(null);
    this.retryLimit.set(limit);
    this.retryOffset.set(offset);

    this.catalogApi.getPage(limit, offset)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.catalogLoading.set(false);
          this.pageLoadError.set(null);
          this.noResultsMessage.set(null);
          this.exactMatch.set(null);
          this.currentLimit.set(limit);
          this.currentOffset.set(offset);
          this.currentPage.set(page);
          this.loadedPages.update((loadedPages) => {
            const remainingPages = loadedPages.filter((loadedPage) => loadedPage.offset !== offset);
            return [...remainingPages, { offset, page }];
          });
        },
        error: () => {
          this.catalogLoading.set(false);

          if (!hasLoadedPage) {
            this.showCatalogUnavailable();
            return;
          }

          this.pageLoadError.set("Konnte die Seite nicht laden. Bitte versuche es erneut.");
        }
      });
  }

  private normalizedQuery() {
    return this.query().trim().toLowerCase();
  }

  private loadCollectionOwnershipAndPage() {
    this.catalogLoading.set(true);
    this.unavailable.set(false);
    this.addError.set(null);

    this.collectionApi.getCollection()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (entries) => {
          this.ownedPokemonIds.set(new Set(entries.map((entry) => entry.pokemonId)));
          this.loadPage(20, 0);
        },
        error: () => {
          this.catalogLoading.set(false);
          this.showCatalogUnavailable();
        }
      });
  }

  private parsePagingLink(link: string | null) {
    if (!link) {
      return null;
    }

    try {
      const url = new URL(link);
      const limit = Number(url.searchParams.get('limit') ?? this.currentLimit());
      const offset = Number(url.searchParams.get('offset') ?? 0);

      if (!Number.isFinite(limit) || !Number.isFinite(offset)) {
        return null;
      }

      return { limit, offset };
    } catch {
      return null;
    }
  }

  private showCatalogUnavailable() {
    this.currentPage.set(null);
    this.loadedPages.set([]);
    this.exactMatch.set(null);
    this.addError.set(null);
    this.noResultsMessage.set(null);
    this.pageLoadError.set(null);
    this.unavailable.set(true);
  }
}
