# HATI² — read the brain first

The project's brain is the Obsidian vault in `brain/`. Start every session and every task there:

1. `brain/HATI Brain.md` — map of everything.
2. `brain/Goal.md` — what "done" means (G1 both phones, G2 real use, G3 not AI slop).
3. `brain/Team log.md` — settled decisions, who owns each kind of decision, and per-round handoffs. Record new decisions and handoffs here (not in chat). `.claude/team-log.md` is a link to this file.

Always use the brain (owner's standing rule) — this applies to the main session, every agent and every skill:
- Before any task: read `brain/HATI Brain.md`, `brain/Goal.md` and the Decisions in `brain/Team log.md`.
- During work: decisions, findings and handoffs go into `brain/Team log.md`, not only into chat.
- After work: run `python3 -I tools/brain/export.py`, then commit and push `brain/` with the change.
- If something you need isn't in the brain, add it there.

Always discuss with the skills (owner's standing rule, 2026-11-09): before planning, deciding or building anything, consult the skills in `.claude/skills/` whose specialty it touches, and let each one speak and challenge from its own specialty before you decide. For a plan or roadmap, consult all of them. Record what they said in `brain/Team log.md`. If usage is tight, have one agent voice the skills instead of spawning one agent per skill.

Rules that apply to every session:
- Decisions go to the skill whose specialty they are (`.claude/skills/`); decide, record in the team log, carry on. Ask the owner only for things outside the repo.
- After changing skills, `web/README.md`, `HatiPalette.kt` or `web/tests/scenarios.md`, run `python3 -I tools/brain/export.py` so the vault stays current.
- Keep replies short.
