---
name: hati-copy
description: Write and review every word in the HATI² web app (labels, buttons, errors, empty states, toasts, share instructions) in one consistent, plain voice. Use when changing UI text or auditing for generic, AI-sounding or inconsistent copy.
---

# HATI² copy

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. Mostly G3 (generic copy is an AI-slop tell) and G2 (users must understand money and errors).

## Deeper version of this task
1. **Top professional:** treats words as interface: names things by what users recognize, makes every button say what happens, and keeps one term per concept everywhere.
2. **Rawest material:** every string in `web/src/app.src.html` (grep the templates), the screenshots from `hati-shots`, and the Android strings in `app/src/main/res/values/strings.xml` for vocabulary.
3. **Methods/tools:** a string inventory (grep), a vocabulary table, read-aloud test, singular/plural and number/date format checks on real data, and a banned-phrase scan.
4. **What shows it's right:** the inventory shows one term per concept; no banned phrases; the lazy-user check finds no wording that needs explaining.

## Voice and vocabulary
- **Terms:** volume = one trip or group; expense (not chapter, entry, item) ; payment = a settle-up; member. "You" for the current user, lower-case "you" mid-sentence.
- **Buttons** say the action: "Save expense", "Record payment", "Import". Toasts confirm in past tense: "Expense saved".
- **Errors** say what's wrong and how to fix it, no apology: "Use numbers like 12.50 (up to 2 decimals)."
- **Numbers:** currency symbol always, 2 decimals, minus sign −, thousands separators; dates "Today", "Yesterday", "Nov 2".
- **Banned:** "Oops", "Something went wrong" without a fix, "seamless", "effortless", "unlock", "elevate", "powerful", exclamation marks in errors, emoji as decoration, title-case marketing phrases.

