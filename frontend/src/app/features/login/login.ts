import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { extraireMessageErreur } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly enChargement = signal(false);
  readonly erreur = signal<string | null>(null);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  soumettre(): void {
    if (this.form.invalid || this.enChargement()) return;
    this.enChargement.set(true);
    this.erreur.set(null);

    const { email, password } = this.form.value;
    this.auth.login({ email: email!, password: password! }).subscribe({
      next: (session) => {
        this.enChargement.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.enChargement.set(false);
        this.erreur.set(extraireMessageErreur(err));
      },
    });
  }
}