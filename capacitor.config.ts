import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.xideck.mediaplayer',
  appName: 'XI Media Deck',
  webDir: 'dist',
  android: {
    allowMixedContent: true,
    backgroundColor: '#05090e',
  },
  server: {
    androidScheme: 'https',
  },
};

export default config;
