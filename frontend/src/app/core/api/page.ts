import { HttpParams } from '@angular/common/http';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PageQuery {
  search: string;
  page: number;
  size: number;
  sort: string;
}

export function toHttpParams(query: PageQuery): HttpParams {
  let params = new HttpParams()
    .set('page', query.page)
    .set('size', query.size)
    .set('sort', query.sort);
  if (query.search.trim()) {
    params = params.set('search', query.search.trim());
  }
  return params;
}
