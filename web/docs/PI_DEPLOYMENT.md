# Raspberry Pi Deployment

Sarathi Web should be isolated under:

`/home/evolve4422/services/sarathi-web`

Recommended checkout:

`/home/evolve4422/services/sarathi-web/repo`

Compose file:

`/home/evolve4422/services/sarathi-web/repo/web/docker-compose.yml`

Current deployed path:

`/home/evolve4422/services/sarathi-web/repo/web`

Ports:

- Frontend: `127.0.0.1:3410 -> 3000`
- API: `127.0.0.1:3411 -> 3001`

Deployment commands:

```bash
cd /home/evolve4422/services/sarathi-web/repo/web
cp .env.example .env
docker compose -p sarathi-web up -d --build
docker compose -p sarathi-web ps
curl http://127.0.0.1:3410
curl http://127.0.0.1:3411/api/health
```

Do not edit shared files such as `/home/evolve4422/services/.env`, existing tunnel settings, Pi-hole, firewall, DNS, existing compose stacks, or global systemd units for this deployment.

Provider API keys are server-managed in the Pi/API environment. The browser never stores Gemini, DeepSeek, or OpenRouter keys.

At minimum, configure provider keys in `.env` as available:

```env
GEMINI_DEMO_API_KEY=
DEEPSEEK_DEMO_API_KEY=
DEEPSEEK_DEMO_MODEL=deepseek-chat
OPENROUTER_DEMO_API_KEY=
```

## Knowledge Files

The API container reads the canonical enriched Gita JSONL from the Pi repo checkout through a read-only Docker volume:

`/home/evolve4422/services/sarathi-web/repo/shared/knowledge/sources/gita/processed/gita_verses.jsonl`

The compose file mounts `../shared/knowledge` to `/app/knowledge` and sets `SARATHI_KNOWLEDGE_ROOT=/app`, so the API does not depend on a Windows development machine at runtime.

Whenever `shared/knowledge/sources/gita/processed/gita_verses.jsonl` changes on Windows, copy it to the same path in the Pi repo and compare SHA-256 hashes before deployment:

```powershell
Get-FileHash shared\knowledge\sources\gita\processed\gita_verses.jsonl -Algorithm SHA256
ssh raspberry-pi "sha256sum /home/evolve4422/services/sarathi-web/repo/shared/knowledge/sources/gita/processed/gita_verses.jsonl"
```
