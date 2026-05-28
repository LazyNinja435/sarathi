# Sarathi Server-Managed Cloud QA

- Practice mode on: send a chat message and confirm Sarathi uses practice/offline guidance without calling the server-managed cloud path.
- Practice mode off with network available: send a chat message and confirm Android calls the Sarathi API, not Gemini, DeepSeek, or OpenRouter directly.
- Network unavailable: enable airplane mode, send a chat message, and confirm Sarathi falls back gracefully to offline/practice guidance without crashing.
- Settings: confirm there is no Google AI Studio API-key toggle, key field, save button, or clear button.
- Developer diagnostics: confirm the runtime label says `Sarathi online` for server-managed guidance and does not mention user API keys.
- Security scan: confirm no provider key is stored in Android preferences, app code, BuildConfig, or committed docs.
