import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { CollectionComponent } from './collection/collection.component';
import { RegisterComponent } from './auth/register/register.component';
import { CatalogComponent } from './catalog/catalog.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'collection' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'collection', component: CollectionComponent },
  { path: 'browse', component: CatalogComponent },
  { path: '**', redirectTo: 'register' }
];
