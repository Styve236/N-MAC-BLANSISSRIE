import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'cfa' })
export class CfaPipe implements PipeTransform {
  transform(valeur?: number | null): string {
    if (valeur === null || valeur === undefined) return '0 FCFA';
    return `${valeur.toLocaleString('fr-FR')} FCFA`;
  }
}