import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from '../core/auth-api.service';
import { CollectionApiService, CollectionEntryDto } from './collection-api.service';

@Component({
  selector: 'app-collection',
  imports: [CommonModule, RouterLink],
  templateUrl: './collection.component.html',
  styleUrl: './collection.component.css'
})
export class CollectionComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly collectionApi = inject(CollectionApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly username = computed(() => this.authApi.username());
  readonly sessionLoading = signal(true);
  readonly collectionLoading = signal(false);
  readonly loading = computed(() => this.sessionLoading() || this.collectionLoading());
  readonly unavailable = signal(false);
  readonly collection = signal<CollectionEntryDto[]>([]);
  private collectionRequestId = 0;

  ngOnInit() {
    if (this.authApi.isAuthenticated()) {
      this.sessionLoading.set(false);
      this.loadCollection();
      return;
    }

    this.sessionLoading.set(false);
    void this.router.navigate(['/login']);
  }

  retry() {
    this.loadCollection();
  }

  logout() {
    this.authApi.logout()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          void this.router.navigate(['/login']);
        },
        error: () => {
          // Treat a failed logout request the same as a
          // successful one: the user's intent is to end up logged out, so clear any local
          // state and send them to /login rather than leaving them stuck on this page.
          this.authApi.clearSession();
          void this.router.navigate(['/login']);
        }
      });
  }

  private loadCollection() {
    this.collectionLoading.set(true);
    this.unavailable.set(false);
    const requestId = ++this.collectionRequestId;

    this.collectionApi.getCollection()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (entries) => {
          if (requestId !== this.collectionRequestId) {
            return;
          }
          this.collectionLoading.set(false);
          this.collection.set(entries);
        },
        error: () => {
          if (requestId !== this.collectionRequestId) {
            return;
          }
          this.collectionLoading.set(false);
          this.collection.set([]);
          this.unavailable.set(true);
        }
      });
  }
}
