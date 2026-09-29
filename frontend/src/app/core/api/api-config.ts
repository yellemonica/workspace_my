import { HttpContextToken } from '@angular/common/http';
import { InjectionToken } from '@angular/core';

export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => 'http://localhost:8080/api',
});

/** Marks requests that run in the background: no global loading indicator and no error snackbar. */
export const BACKGROUND_REQUEST = new HttpContextToken<boolean>(() => false);
