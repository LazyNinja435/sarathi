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

The Pi `.env` contains only Firebase public web config and backend non-secret settings by default. The Gemini API key is not stored on the Pi; users enter it in the browser.
