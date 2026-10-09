import { initializeApp, getApps, getApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: "AIzaSyA2ZUarRUOu29P9akgJ0jiUHfg1yXCNvNY",
  authDomain: "innogenai.firebaseapp.com",
  projectId: "innogenai",
  storageBucket: "innogenai.firebasestorage.app",
  messagingSenderId: "556742587384",
  appId: "1:556742587384:web:c1ce393b70e2627e281c0d"
};

const app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);

