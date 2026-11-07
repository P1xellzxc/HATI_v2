# HATI² — read the brain first

The project's brain is the Obsidian vault in `brain/`. Start every session and every task there:

1. `brain/HATI Brain.md` — map of everything.
2. `brain/Goal.md` — what "done" means (G1 both phones, G2 real use, G3 not AI slop).
3. `brain/Team log.md` — settled decisions, who owns each kind of decision, and per-round handoffs. Record new decisions and handoffs here (not in chat). `.claude/team-log.md` is a link to this file.

Rules that apply to every session:
- Decisions go to the skill whose specialty they are (`.claude/skills/`); decide, record in the team log, carry on. Ask the owner only for things outside the repo.
- After changing skills, `web/README.md`, `HatiPalette.kt` or `web/tests/scenarios.md`, run `python3 -I tools/brain/export.py` so the vault stays current.
- Keep replies short.
