import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-auth-layout',
  imports: [RouterOutlet, Icon],
  template: `
    <div class="auth-shell">
      <div class="auth-brand">
        <span class="auth-logo"><app-icon nom="washing-machine" [taille]="56" /></span>
        <h1>N-MAC Blanchisserie</h1>
        <p>Gestion de pressing</p>
      </div>
      <router-outlet />
    </div>
  `,
  styles: [
    `
      .auth-shell {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        background: #0f172a;
        color: #f1f5f9;
        gap: 24px;
        padding: 24px;
      }
      .auth-brand {
        text-align: center;
      }
      .auth-logo {
        width: 88px;
        height: 88px;
        border-radius: 24px;
        background: #2563eb;
        color: #fff;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }
      h1 {
        margin: 8px 0 4px;
        font-size: 26px;
      }
      p {
        color: #94a3b8;
        margin: 0;
      }
    `,
  ],
})
export class AuthLayout {}