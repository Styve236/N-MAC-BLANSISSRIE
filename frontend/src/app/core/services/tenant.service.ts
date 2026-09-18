import { computed, inject, Injectable } from '@angular/core';
import { AuthService } from './auth.service';
import { ROLES } from '../config/constants';

@Injectable({ providedIn: 'root' })
export class TenantService {
  private readonly auth = inject(AuthService);

  readonly role = this.auth.role;
  readonly nom = this.auth.nom;
  readonly estAdmin = computed(() => this.auth.role() === ROLES.ADMIN);
  readonly estReceptionniste = computed(() => this.auth.role() === ROLES.RECEPTIONNISTE);
  readonly estAgentProduction = computed(() => this.auth.role() === ROLES.AGENT_PRODUCTION);
  readonly estLivreur = computed(() => this.auth.role() === ROLES.LIVREUR);
  readonly peutEcrire = computed(() => this.estAdmin() || this.estReceptionniste());
  readonly peutGererStatuts = computed(() =>
    this.estAdmin() || this.estReceptionniste() || this.estAgentProduction() || this.estLivreur()
  );
}