import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../shared/icon/icon';
import { ROLES } from '../../core/config/constants';
import { UserService } from '../../core/services/user.service';
import { UserDTO, UserRequestDTO } from '../../core/models/users.model';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-users',
  imports: [FormsModule, Icon],
  templateUrl: './users.html',
  styleUrl: '../_feature.scss',
})
export class Users {
  private readonly service = inject(UserService);

  readonly utilisateurs = signal<UserDTO[]>([]);
  readonly enChargement = signal(true);
  readonly erreur = signal('');
  readonly message = signal('');

  readonly showForm = signal(false);
  readonly form = signal<Required<Omit<UserRequestDTO, 'password'>> & { password?: string }>({
    nom: '',
    tels: '',
    email: '',
    role: ROLES.RECEPTIONNISTE,
  });
  readonly enregistrement = signal(false);

  readonly showResetMdp = signal(false);
  readonly resetId = signal<number | null>(null);
  readonly resetPassword = signal('');
  readonly enCoursReset = signal(false);

  constructor() {
    this.charger();
  }

  charger(): void {
    this.enChargement.set(true);
    this.service.lister().subscribe({
      next: (u) => {
        this.utilisateurs.set(u ?? []);
        this.enChargement.set(false);
      },
      error: () => this.enChargement.set(false),
    });
  }

  ouvrirNouveau(): void {
    this.form.set({ nom: '', tels: '', email: '', role: ROLES.RECEPTIONNISTE, password: '' });
    this.showForm.set(true);
    this.erreur.set('');
  }

  ouvrirEdition(u: UserDTO): void {
    this.form.set({ nom: u.nom, tels: u.tels, email: u.email, role: u.role, password: '' });
    this.showForm.set(true);
    this.erreur.set('');
  }

  enregistrer(): void {
    const f = this.form();
    if (!f.nom || !f.tels || !f.email) {
      this.erreur.set('Nom, téléphone et email sont obligatoires.');
      return;
    }
    this.enregistrement.set(true);
    const dto: UserRequestDTO = {
      nom: f.nom,
      tels: f.tels,
      email: f.email,
      role: f.role,
      password: f.password || undefined,
    };
    this.service.creer(dto).subscribe({
      next: () => {
        this.enregistrement.set(false);
        this.showForm.set(false);
        this.message.set('Utilisateur créé.');
        this.charger();
      },
      error: (err) => {
        this.enregistrement.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }

  basculerStatut(u: UserDTO): void {
    if (!u.idusers) return;
    this.service.changerStatut(u.idusers, !u.actif).subscribe({
      next: () => {
        this.message.set(`${u.nom} est maintenant ${u.actif ? 'désactivé' : 'activé'}.`);
        this.charger();
      },
      error: (err) => this.erreur.set(extraireMessageErreur(err)),
    });
  }

  ouvrirReset(u: UserDTO): void {
    this.resetId.set(u.idusers ?? null);
    this.resetPassword.set('');
    this.showResetMdp.set(true);
    this.erreur.set('');
  }

  confirmerReset(): void {
    const id = this.resetId();
    const mdp = this.resetPassword();
    if (!id || !mdp) {
      this.erreur.set('Mot de passe requis.');
      return;
    }
    this.enCoursReset.set(true);
    this.service.changerMotDePasse(id, mdp).subscribe({
      next: () => {
        this.enCoursReset.set(false);
        this.showResetMdp.set(false);
        this.message.set('Mot de passe réinitialisé.');
      },
      error: (err) => {
        this.enCoursReset.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }
}