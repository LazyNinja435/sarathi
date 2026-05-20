# Sarathi Web Memory Design

Short-term memory is kept in frontend session state or recomputed from recent messages. It includes current emotion, current concern, important context, preferred guidance style, and the last guidance theme.

Long-term memory is stored in Firestore at `/users/{uid}/memory/main`. Defaults are simple language, quote plus simple meaning plus practical guidance, Krishna-inspired practical tone, and an empty saved notes array.

Privacy rule: Sarathi may store basic preferences automatically. It must not automatically store sensitive personal, emotional, health, relationship, family, or spiritual struggle details as long-term memory. Personal long-term memory should be saved only when the user clearly says phrases such as "remember this", "save this", "keep this in mind", or "remember that".
