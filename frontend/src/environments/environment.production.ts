// Environnement de production (Vercel).
// Le frontend appelle directement le backend Render (pas de proxy Vercel, qui
// limiterait les uploads de photos à ~4,5 Mo). CORS est géré côté backend par
// APP_CORS_ALLOWED_ORIGINS.
export const environment = {
  production: true,
  apiUrl: 'https://nmac-pressing-api.onrender.com',
};
