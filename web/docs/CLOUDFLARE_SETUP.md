# Cloudflare Setup

Zone: `sreekrishna.uk`.

Zone ID: `7816de7efb11776fe638991548de9b7a`.

Existing tunnel: `lazyninja`.

Tunnel ID: `29e595bc-7225-4bb4-bab8-b165a9a77a3f`.

Public hostname:

`talkto.sreekrishna.uk`

Tunnel target:

`http://127.0.0.1:3410`

Required ingress rule:

```json
{
  "hostname": "talkto.sreekrishna.uk",
  "service": "http://127.0.0.1:3410",
  "originRequest": {}
}
```

Required DNS record:

```text
type: CNAME
name: talkto.sreekrishna.uk
target: 29e595bc-7225-4bb4-bab8-b165a9a77a3f.cfargotunnel.com
proxied: true
```

The Pi already runs a token-based `cloudflared` systemd tunnel. Add only this public hostname to the existing tunnel through Cloudflare Zero Trust/API. Do not change existing hostnames for other services.

Firebase Auth handles app login for v1; Cloudflare Access is not required unless a future release needs an additional gate.

Attempted MCP update on 2026-05-19 failed with Cloudflare API authentication error, while read access succeeded. Complete the ingress and CNAME update with a token that has Zone DNS edit and Cloudflare Tunnel configuration edit permissions.
