import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface CollectionEntryDto {
  pokemonId: number;
  pokemonName: string;
  spriteUrl: string;
  addedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class CollectionApiService {
  private readonly http = inject(HttpClient);

  getCollection() {
    return this.http.get<CollectionEntryDto[]>('/api/collection');
  }

  addToCollection(pokemonId: number): Observable<void> {
    return this.http.post<void>('/api/collection', { pokemonId });
  }
}
