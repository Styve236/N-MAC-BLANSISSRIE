import { computed, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { finalize, Observable, shareReplay, tap } from 'rxjs';
import { ConnexionResponse, LoginRequest, RefreshTokenRequestDTO } from '../models/auth.model';
import { API_URL } from '../config/constants';

const ACCESS_KEY = 'press_access_token';
const REFRESH_KEY = 'press_refresh_token';
const USER_KEY = 'press_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly accessToken = signal<string | null>(null);
  private readonly currentUser = signal<ConnexionResponse | null>(null);

  /**
   * Refresh en vol partage entre tous les appelants. Sans lui, N requetes qui
   * recoivent un 401 en meme temps declencheraient N appels /auth/refresh : le
   * backend traite le 2e usage du meme refresh token comme un vol et revoque
   * toutes les sessions (deconnexion en masse).
   */
  private refreshEnCours: Observable<ConnexionResponse> | null = null;

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly role = computed(() => this.currentUser()?.role ?? null);
  readonly nom = computed(() => this.currentUser()?.nom ?? '');

  constructor(private readonly http: HttpClient) {
    this.restaurerSession();
  }

  login(credentials: LoginRequest): Observable<ConnexionResponse> {
    return this.http
      .post<ConnexionResponse>(`${API_URL}/api/auth/login`, credentials)
      .pipe(tap((r) => this.garderSession(r)));
  }

  refresh(): Observable<ConnexionResponse> {
    if (!this.refreshEnCours) {
      const body: RefreshTokenRequestDTO = { refreshToken: this.getRefreshToken() ?? '' };
      this.refreshEnCours = this.http
        .post<ConnexionResponse>(`${API_URL}/api/auth/refresh`, body)
        .pipe(
          tap((r) => this.garderSession(r)),
          finalize(() => {
            this.refreshEnCours = null;
          }),
          shareReplay({ bufferSize: 1, refCount: false })
        );
    }
    return this.refreshEnCours;
  }

  logout(): Observable<void> {
    const refreshToken = this.getRefreshToken();
    this.viderSession();
    if (!refreshToken) {
      return new Observable((s) => {
        s.next();
        s.complete();
      });
    }
    return this.http.post<void>(`${API_URL}/api/auth/logout`, { refreshToken });
  }

  getToken(): string | null {
    return this.accessToken();
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  hasRole(...roles: string[]): boolean {
    const role = this.role();
    return role !== null && roles.includes(role);
  }

  private garderSession(r: ConnexionResponse): void {
    if (r.token) {
      this.accessToken.set(r.token);
      localStorage.setItem(ACCESS_KEY, r.token);
    }
    if (r.refresh_token) {
      localStorage.setItem(REFRESH_KEY, r.refresh_token);
    }
    this.currentUser.set(r);
    localStorage.setItem(USER_KEY, JSON.stringify(r));
  }

  private viderSession(): void {
    this.accessToken.set(null);
    this.currentUser.set(null);
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
  }

  private restaurerSession(): void {
    const token = localStorage.getItem(ACCESS_KEY);
    const raw = localStorage.getItem(USER_KEY);
    if (!token || !raw) {
      return;
    }
    try {
      const session = JSON.parse(raw) as ConnexionResponse;
      this.accessToken.set(token);
      this.currentUser.set(session);
      // Jeton d'acces deja expire au chargement : renouvellement silencieux
      // pour ne pas afficher l'ecran de connexion pour un simple CNF.
      if (this.tokenExpiRe(token)) {
        this.rafraichirSilencieusement();
      }
    } catch {
      this.viderSession();
    }
  }

  /** Lit la date d'expiration (claim "exp") sans verifier la signature. */
  private tokenExpiRe(token: string): boolean {
    try {
      const partie = token.split('.')[1];
      if (!partie) return false;
      const payload = JSON.parse(
        decodeURIComponent(
          atob(partie.replace(/-/g, '+').replace(/_/g, '/'))
            .split('')
            .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
            .join('')
        )
      );
      const exp = payload?.exp as number | undefined;
      return typeof exp === 'number' && exp * 1000 <= Date.now();
    } catch {
      return false;
    }
  }

  private rafraichirSilencieusement(): void {
    if (!this.getRefreshToken()) {
      this.viderSession();
      return;
    }
    this.accessToken.set(null);
    this.currentUser.set(null);
    this.refresh().subscribe({ error: () => this.viderSession() });
  }
}