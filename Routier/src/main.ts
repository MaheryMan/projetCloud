import { createApp } from 'vue'
import App from './App.vue'
import router from './router';

import { IonicVue } from '@ionic/vue';
import { createPinia } from 'pinia'

// Import icon utilities
import { addIcons } from 'ionicons';

// Import specific icons you're using in your app
import {
  mapOutline,
  personOutline,
  globeOutline,
  alertCircleOutline,
  checkmarkCircleOutline,
  checkmarkCircle,
  timeOutline,
  layersOutline,
  locate,
  locateOutline,
  addOutline,
  addCircleOutline,
  removeOutline,
  personCircleOutline,
  locationOutline,
  location,
  closeOutline,
  listOutline,
  documentTextOutline,
  informationCircleOutline,
  arrowForwardOutline,
  constructOutline,
  createOutline,
  swapHorizontalOutline,
  checkmarkOutline,
  warningOutline,
  calendarOutline,
  fingerPrintOutline,
  shieldCheckmarkOutline,
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
  'person-circle-outline': personCircleOutline,
  'checkmark-circle-outline': checkmarkCircleOutline,
  'checkmark-circle': checkmarkCircle,
  'time-outline': timeOutline,
  'layers-outline': layersOutline,
  'locate': locate,
  'locate-outline': locateOutline,
  'add-outline': addOutline,
  'add-circle-outline': addCircleOutline,
  'remove-outline': removeOutline,
  'location-outline': locationOutline,
  'location': location,
  'close-outline': closeOutline,
  'list-outline': listOutline,
  'document-text-outline': documentTextOutline,
  'information-circle-outline': informationCircleOutline,
  'arrow-forward-outline': arrowForwardOutline,
  'construct-outline': constructOutline,
  'create-outline': createOutline,
  'swap-horizontal-outline': swapHorizontalOutline,
  'checkmark-outline': checkmarkOutline,
  'warning-outline': warningOutline,
  'calendar-outline': calendarOutline,
  'finger-print-outline': fingerPrintOutline,
  'shield-checkmark-outline': shieldCheckmarkOutline,
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
  .use(createPinia())
  .use(router);

router.isReady().then(() => {
  app.mount('#app');
});