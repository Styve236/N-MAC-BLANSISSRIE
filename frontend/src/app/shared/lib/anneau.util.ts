export interface SegmentAnneau {
  cle: string;
  legende: string;
  valeur: number;
  couleur: string;
  dasharray: string;
  dashoffset: string;
}

export const RAYON = 40;
export const CIRCONFERENCE = 2 * Math.PI * RAYON;

export function construireAnneau(
  entrees: { cle: string; legende: string; valeur: number; couleur: string }[],
): SegmentAnneau[] {
  const total = entrees.reduce((s, e) => s + e.valeur, 0);
  if (total <= 0) return [];
  let cumul = 0;
  return entrees.map((e) => {
    const debut = cumul / total;
    cumul += e.valeur;
    return {
      cle: e.cle,
      legende: e.legende,
      valeur: e.valeur,
      couleur: e.couleur,
      dasharray: `${(e.valeur / total) * CIRCONFERENCE} ${CIRCONFERENCE}`,
      dashoffset: `${-debut * CIRCONFERENCE}`,
    };
  });
}

export const COULEUR_STATUT: Record<string, string> = {
  RECU: 'var(--ink-soft)',
  INVENTAIRE: 'var(--indigo)',
  EN_LAVAGE: 'var(--teal)',
  REPASSAGE: 'var(--amber)',
  PRET: 'var(--indigo)',
  RECUPERE: 'var(--teal)',
  LIVRE: 'var(--indigo)',
  PAYEE: 'var(--teal)',
};

export const COULEUR_MOYEN: Record<string, string> = {
  ESPECES: 'var(--teal)',
  ORANGE_MONEY: 'var(--amber)',
  MTN_MONEY: 'var(--indigo)',
};