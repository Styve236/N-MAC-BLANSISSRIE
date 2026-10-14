const { app, BrowserWindow, Menu, shell } = require('electron');
const path = require('path');

// URL de l'application web (Vercel). Surchargeable au lancement :
//   set APP_URL=https://mon-front.vercel.app && npm start
const APP_URL = process.env.APP_URL || 'https://nmac-blanchisserie.vercel.app';
const APP_ORIGIN = new URL(APP_URL).origin;

function createWindow() {
  const win = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 1024,
    minHeight: 700,
    backgroundColor: '#F8FAFC',
    title: 'N MAC Blanchisserie',
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });

  win.loadURL(APP_URL);

  // Les liens externes (WhatsApp, tel:, mailto:…) s'ouvrent hors de l'app.
  win.webContents.setWindowOpenHandler(({ url }) => {
    const externe =
      (url.startsWith('http') && !url.startsWith(APP_ORIGIN)) ||
      url.startsWith('tel:') ||
      url.startsWith('mailto:') ||
      url.startsWith('whatsapp:');
    if (externe) {
      shell.openExternal(url);
      return { action: 'deny' };
    }
    return { action: 'allow' };
  });

  // Navigation interne : tout départ vers une autre origine reste dans l'app ?
  // Non — on l'ouvre dans le navigateur pour ne pas perdre le contexte.
  win.webContents.on('will-navigate', (event, url) => {
    if (!url.startsWith(APP_ORIGIN) && url.startsWith('http')) {
      event.preventDefault();
      shell.openExternal(url);
    }
  });
}

app.whenReady().then(() => {
  Menu.setApplicationMenu(null);
  createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
