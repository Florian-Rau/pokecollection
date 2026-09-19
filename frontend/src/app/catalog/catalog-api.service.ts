import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface PokemonSummary {
  pokemonId: number;
  name: string;
  spriteUrl: string;
}

export interface PokemonPage {
  results: PokemonSummary[];
  count: number;
  next: string | null;
  previous: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class CatalogApiService {
  private readonly http = inject(HttpClient);

  getPage(limit?: number, offset?: number) {
    let params = new HttpParams();

    if (limit !== undefined) {
      params = params.set('limit', limit);
    }

    if (offset !== undefined) {
      params = params.set('offset', offset);
    }

    return this.http.get<PokemonPage>('/api/pokemon', { params });
  }

  findByName(name: string) {
    return this.http.get<PokemonSummary>(`/api/pokemon/${encodeURIComponent(name.trim().toLowerCase())}`);
  }
}
