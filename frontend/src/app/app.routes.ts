import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'students' },
  { path: 'students', loadChildren: () => import('./features/students/students.routes') },
  { path: 'courses', loadChildren: () => import('./features/courses/courses.routes') },
  { path: '**', title: 'Not found', loadComponent: () => import('./shared/not-found/not-found') },
];
