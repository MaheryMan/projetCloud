import { createApp } from 'vue'
import App from './App.vue'
import router from './router';

import { IonicVue } from '@ionic/vue';

// Import icon utilities
import { addIcons } from 'ionicons';

// Import specific icons you're using in your app
import {
  mapOutline,
  personOutline,
  globeOutline,
  alertCircleOutline,
  checkmarkCircleOutline,
  timeOutline,
  locateOutline,
  addOutline,
  locationOutline,
  closeOutline,
  listOutline,
  documentTextOutline,
  informationCircleOutline,
  constructOutline,
  swapHorizontalOutline,
  checkmarkOutline,
  // Add these if you're using them in other components:
  homeOutline,
  settingsOutline,
  logOutOutline,
  arrowBackOutline,
  menuOutline,
  searchOutline,
  filterOutline,
  trashOutline,
  eyeOutline,
  eyeOffOutline,
  mailOutline,
  lockClosedOutline,
  logInOutline,
  personAddOutline
} from 'ionicons/icons';

// Register all icons
addIcons({
  'map-outline': mapOutline,
  'person-outline': personOutline,
  'globe-outline': globeOutline,
  'alert-circle-outline': alertCircleOutline,
  'checkmark-circle-outline': checkmarkCircleOutline,
  'time-outline': timeOutline,
  'locate-outline': locateOutline,
  'add-outline': addOutline,
  'location-outline': locationOutline,
  'close-outline': closeOutline,
  'list-outline': listOutline,
  'document-text-outline': documentTextOutline,
  'information-circle-outline': informationCircleOutline,
  'construct-outline': constructOutline,
  'swap-horizontal-outline': swapHorizontalOutline,
  'checkmark-outline': checkmarkOutline,
  // Optional: add these for other pages
  'home-outline': homeOutline,
  'settings-outline': settingsOutline,
  'log-out-outline': logOutOutline,
  'arrow-back-outline': arrowBackOutline,
  'menu-outline': menuOutline,
  'search-outline': searchOutline,
  'filter-outline': filterOutline,
  'trash-outline': trashOutline,
  'eye-outline': eyeOutline,
  'eye-off-outline': eyeOffOutline,
  'mail-outline': mailOutline,
  'lock-closed-outline': lockClosedOutline,
  'log-in-outline': logInOutline,
  'person-add-outline': personAddOutline
});

/* Core CSS required for Ionic components to work properly */
import '@ionic/vue/css/core.css';

/* Basic CSS for apps built with Ionic */
import '@ionic/vue/css/normalize.css';
import '@ionic/vue/css/structure.css';
import '@ionic/vue/css/typography.css';

/* Optional CSS utils that can be commented out */
import '@ionic/vue/css/padding.css';
import '@ionic/vue/css/float-elements.css';
import '@ionic/vue/css/text-alignment.css';
import '@ionic/vue/css/text-transformation.css';
import '@ionic/vue/css/flex-utils.css';
import '@ionic/vue/css/display.css';

import 'leaflet/dist/leaflet.css';

/**
 * Ionic Dark Mode
 * -----------------------------------------------------
 * For more info, please see:
 * https://ionicframework.com/docs/theming/dark-mode
 */

/* @import '@ionic/vue/css/palettes/dark.always.css'; */
/* @import '@ionic/vue/css/palettes/dark.class.css'; */
import '@ionic/vue/css/palettes/dark.system.css';

/* Theme variables */
import './theme/variables.css';

const app = createApp(App)
  .use(IonicVue)
  .use(router);

router.isReady().then(() => {
  app.mount('#app');
});