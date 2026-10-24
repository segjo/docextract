import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/import/import').then((m) => m.Import),
  },
  {
    path: 'processes/:processId',
    loadComponent: () => import('./features/process/process-page').then((m) => m.ProcessPage),
  },
  { path: '**', redirectTo: '' },
];
