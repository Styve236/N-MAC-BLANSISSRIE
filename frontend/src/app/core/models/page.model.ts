/**
 * Reponse paginee de Spring Data (PageImpl), telle que serialisee par le backend.
 * Seuls les champs utilises par le frontend sont declares.
 */
export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements?: number;
  number?: number;
  size?: number;
}