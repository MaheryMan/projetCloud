// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";
import { getAnalytics } from "firebase/analytics";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyCQVfKLVG7Ip47ode7bZ6DMiifP3gIFJyA",
  authDomain: "s5-routier.firebaseapp.com",
  projectId: "s5-routier",
  storageBucket: "s5-routier.firebasestorage.app",
  messagingSenderId: "709870854675",
  appId: "1:709870854675:web:b3422df365df0683e5d5e6",
  measurementId: "G-KL1XFCXQTG"
};
console.log('Firebase config:', firebaseConfig); // Debug
// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();
