# GitHub repository safety (pre-release)

Date: 2026-05-14  
Workspace: `D:\MyProjects\Sarathi`

## Git remotes

Command: `git remote -v`

Result: **no remotes configured** in this workspace clone.

Expected upstream:

`https://github.com/LazyNinja435/sarathi`

### Fix (do not force-push)

```powershell
git remote add origin https://github.com/LazyNinja435/sarathi.git
git fetch origin
```

If `origin` already points elsewhere, use `git remote set-url origin https://github.com/LazyNinja435/sarathi.git` instead of `add`.

## `.gitignore` coverage (release hygiene)

Confirmed patterns present (subset aligned with release requirements):

- `local-models/`
- `dist/`
- `*.litertlm`
- `*.task`
- `*.keystore` / `*.jks` (plus `release-secrets/`, `signing.properties`, `.github/tmp/`)

## `git check-ignore` probes

| Path | Ignored by |
| --- | --- |
| `local-models/gemma4-e2b/gemma-4-E2B-it.litertlm` | `local-models/` |
| `dist/sarathi-pixel-bundle/gemma-4-E2B-it.litertlm` | `dist/` |
| `dist/github-release/gemma-4-E2B-it.litertlm.part001` | `dist/` |

## Secret scan (heuristic)

Scanned `app/src`, `scripts`, and `tools` for common GitHub token patterns (`ghp_*`, `github_pat_*`). **No matches** in this pass.

## Working tree note

`git status --short` at report time showed many modified/untracked files from ongoing development; this does not change ignore rules. Before tagging a release, ensure **no** `*.litertlm`, `*.apk`, keystores, or `dist/` artifacts are staged.

## Conclusion

- **Model / dist / keystore paths are ignored** as required.
- **No GitHub token literals** were found by the quick scan.
- **Configure `origin`** to `LazyNinja435/sarathi` before pushing release automation or tags.
