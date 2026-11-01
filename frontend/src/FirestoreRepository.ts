import { db } from './firebaseConfig';
import { collection, onSnapshot, query, where, DocumentData } from 'firebase/firestore';

export class FirestoreRepository {
  
  // Real-time listener for user settings
  subscribeToUserSettings(userId: string, callback: (data: DocumentData) => void) {
    const q = query(collection(db, "users"), where("userId", "==", userId));
    return onSnapshot(q, (snapshot) => {
      snapshot.forEach((doc) => callback(doc.data()));
    });
  }

  // Real-time listener for chat threads
  subscribeToChatThreads(userId: string, callback: (data: DocumentData[]) => void) {
    const q = query(collection(db, `users/${userId}/threads`));
    return onSnapshot(q, (snapshot) => {
      const threads = snapshot.docs.map(doc => doc.data());
      callback(threads);
    });
  }

  // Real-time listener for system metrics
  subscribeToMetrics(callback: (data: DocumentData) => void) {
    return onSnapshot(collection(db, "system"), (snapshot) => {
      snapshot.forEach((doc) => {
        if(doc.id === "metrics") callback(doc.data());
      });
    });
  }
}
