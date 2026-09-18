import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'dateFr' })
export class DateFrPipe implements PipeTransform {
  transform(valeur?: string | null, mode: 'date' | 'heure' | 'datetime' = 'datetime'): string {
    if (!valeur) return '—';
    const d = new Date(valeur);
    if (Number.isNaN(d.getTime())) return valeur;
    const date = d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    if (mode === 'date') return date;
    const heure = d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    if (mode === 'heure') return heure;
    return `${date} ${heure}`;
  }
}