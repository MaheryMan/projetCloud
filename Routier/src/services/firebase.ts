import { initializeApp } from 'firebase/app'
import { getAuth } from 'firebase/auth'
import { getFirestore } from 'firebase/firestore'

const firebaseConfig = {
  apiKey: "AIzaSyCQVfKLVG7Ip47ode7bZ6DMiifP3gIFJyA",
  authDomain: "s5-routier.firebaseapp.com",
  projectId: "s5-routier",
  storageBucket: "s5-routier.firebasestorage.app",
  messagingSenderId: "709870854675",
  appId: "1:709870854675:web:b3422df365df0683e5d5e6",
  measurementId: "G-KL1XFCXQTG"
}

const app = initializeApp(firebaseConfig)

export const auth = getAuth(app)
export const db = getFirestore(app)
