# Firebase Setup

Project display name: Sarathi Web.

Project ID: `sarathi-web-pruthvi`.

The preferred ID `sarathi-web` was already taken globally, so `sarathi-web-pruthvi` is used.

## Web App

Firebase Web app name: `Sarathi Web`.

Firebase Web app ID: `1:530482515410:web:e4c16c3bca62dcfb5d0c0b`.

Copy the SDK config into environment variables:

- `VITE_FIREBASE_API_KEY`
- `VITE_FIREBASE_AUTH_DOMAIN`
- `VITE_FIREBASE_PROJECT_ID`
- `VITE_FIREBASE_APP_ID`
- `VITE_FIREBASE_STORAGE_BUCKET`
- `VITE_FIREBASE_MESSAGING_SENDER_ID`

## Auth

Enable Firebase Authentication with Google sign-in. Authorized domains:

- `talkto.sreekrishna.uk`
- `localhost`

Google sign-in was initialized for this project. If `talkto.sreekrishna.uk` is not visible under Firebase Auth authorized domains in the console, add it manually before public login testing.

Configured authorized domains:

- `sarathi-web-pruthvi.firebaseapp.com`
- `sarathi-web-pruthvi.web.app`
- `talkto.sreekrishna.uk`
- `localhost`

## Firestore

Collections:

- `/users/{uid}/profile/main`
- `/users/{uid}/preferences/main`
- `/users/{uid}/memory/main`
- `/users/{uid}/conversations/{conversationId}`
- `/users/{uid}/conversations/{conversationId}/messages/{messageId}`

Rules summary: authenticated users can read and write only their own `/users/{uid}` document tree.

Rules file: `web/firestore.rules`.

Active rule:

```text
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null
                         && request.auth.uid == userId;
    }
  }
}
```

## Backend Token Verification

The API verifies Firebase ID tokens. For v1 it can verify tokens with Google's public Firebase Secure Token keys when `FIREBASE_PROJECT_ID` is set, so a service account private key is not required for the Pi deployment.

Optional Firebase Admin service account env vars are still supported:

- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

Never commit service account JSON or private keys.
