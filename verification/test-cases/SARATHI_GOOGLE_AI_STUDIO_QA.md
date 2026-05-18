# Sarathi Google AI Studio QA

- Google AI Studio off by default: open Settings and confirm the toggle is off.
- Enable Google AI Studio without saving a key: send a chat message; Sarathi should use the offline/default path and should not call Gemini.
- Paste a valid Google AI Studio API key, save it, keep the toggle enabled, and send a chat message; Sarathi should respond through `gemini-flash-lite-latest`.
- Clear the key: the API key status should return to not configured and chat should return to the offline/default path.
- Save an invalid key with the toggle enabled: chat should gracefully fall back to offline/default guidance, with only sanitized diagnostic text.
- Disable the toggle after saving a key: chat should not call Gemini and should use the existing offline/default model behavior.
