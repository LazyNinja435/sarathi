# Sarathi Web

Sarathi Web is the browser version of Sarathi, built in the same repository as the Android app. It uses Firebase Auth and Firestore for v1 user state, and a Pi-hosted Fastify API for Gemini calls with a user-provided Google AI Studio key.

## Apps

- `apps/frontend`: React, Vite, TypeScript.
- `apps/api`: Fastify, TypeScript.
- `packages/shared-persona`: Sarathi persona and prompt builder.
- `packages/shared-types`: shared chat, memory, preference, and provider types.
- `packages/shared-config`: shared constants.

## Shared Prompt Contract

The source of truth for Sarathi persona and response-shape rules is `../shared/persona/sarathi_prompt_contract.json`.

After editing it, run from the repo root:

```powershell
node tools/generate_sarathi_prompt_contract.mjs
```

This updates both Android and web generated prompt constants so response style stays consistent across Android, web, and future clients.

## Shared Brand

The canonical logo is `../shared/brand/sarathi-logo.png`, generated from the Android launch-center flute + peacock feather assets.

After changing brand assets, run from the repo root:

```powershell
python tools/generate_sarathi_canonical_logo.py
node tools/sync_sarathi_brand_assets.mjs
```

## Local Commands

```powershell
cd web
npm install
npm run build
npm run test
```

Create `web/.env` from `.env.example` for local API and Docker use. Do not commit real secrets.
