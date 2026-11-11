"""Export the HATI² project brain as an Obsidian vault (brain/).

Sources of truth stay where the agents read them (.claude/skills, brain/Team log.md,
web/README.md, HatiPalette.kt, web/tests/scenarios.md). This script mirrors them into
linked Obsidian notes. Re-run after changes:  python3 -I tools/brain/export.py
"""
import json, re, shutil, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "brain"
sys.path.insert(0, str(ROOT / "web/tools"))
from export_palettes import palettes  # noqa: E402

SKILLS = sorted(p.parent.name for p in (ROOT / ".claude/skills").glob("*/SKILL.md"))
LINK_RE = re.compile(r"(?<![\w\[/-])`?(" + "|".join(map(re.escape, sorted(SKILLS, key=len, reverse=True))) + r")`?(?![\w-])")

def linkify(text):
    return LINK_RE.sub(lambda m: f"[[{m.group(1)}]]", text)

def note(path, title, body, tags=(), extra=None):
    p = OUT / path; p.parent.mkdir(parents=True, exist_ok=True)
    fm = {"title": title, "tags": list(tags), **(extra or {})}
    head = "---\n" + "\n".join(f"{k}: {json.dumps(v, ensure_ascii=False)}" for k, v in fm.items()) + "\n---\n\n"
    p.write_text(head + body.rstrip() + "\n")

def section(text, start, stop_prefix="\n## "):
    i = text.find(start)
    if i < 0: return ""
    j = text.find(stop_prefix, i + len(start))
    return text[i:j if j > 0 else None]

LIVE = {"Team log.md", "Roadmap.md"}  # live files edited by the team; never regenerated
OUT.mkdir(exist_ok=True)
for f in list(OUT.rglob("*.md")):
    if f.relative_to(OUT).as_posix() not in LIVE:
        f.unlink()
for d in sorted((x for x in OUT.rglob("*") if x.is_dir() and ".obsidian" not in x.parts), reverse=True):
    if not any(d.iterdir()):
        d.rmdir()

# --- Skills -------------------------------------------------------------
role_of = {}
for name in SKILLS:
    raw = (ROOT / f".claude/skills/{name}/SKILL.md").read_text()
    fm = re.match(r"---\n(.*?)\n---\n", raw, re.S)
    desc = re.search(r"description:\s*(.*)", fm.group(1)).group(1).strip().strip('"') if fm else ""
    body = raw[fm.end():] if fm else raw
    role_of[name] = desc
    note(f"Skills/{name}.md", name,
         f"> {desc}\n\nSource: `.claude/skills/{name}/SKILL.md` · Back to [[HATI Brain]] · [[Skills index]]\n\n" + linkify(body),
         tags=("hati/skill",))

note("Skills index.md", "Skills index",
     "Every role and shared tool the team loop uses. Each skill starts with the goal, the \"deeper version\" questions, and its rewritten prompt.\n\n"
     + "\n".join(f"- [[{n}]]: {role_of[n]}" for n in SKILLS) + "\n\nBack to [[HATI Brain]]", tags=("hati/index",))

# --- Goal, decisions, team log -------------------------------------------
web_readme = (ROOT / "web/README.md").read_text()
note("Goal.md", "Goal and definition of done",
     linkify(section(web_readme, "## Goal") or web_readme) + "\n\n" + linkify(section(web_readme, "## Invariants")) +
     "\n\nSee also [[Decisions]], [[Team loop]], [[HATI Brain]]", tags=("hati/goal",))

log = (ROOT / "brain/Team log.md").read_text()
note("Decisions.md", "Decisions and owners",
     "Live view of the settled decisions and who owns each kind. The text lives in [[Team log]]; edit it there.\n\n"
     "![[Team log#Decision owners]]\n\n![[Team log#Decisions (settled; don't reopen without new evidence)]]\n\nBack to [[HATI Brain]]",
     tags=("hati/decisions",))

# --- Process --------------------------------------------------------------
note("Team loop.md", "Team loop",
     """Plan → do → audit → revise rounds on the web app until the [[Goal]] is met (max 3 per run).

1. **Plan:** [[hati-plan]] picks at most 5 items, each with a check that proves it; a critic (code + design view) amends.
2. **Do:** [[hati-design]], then [[hati-code]] (same file, so in sequence).
3. **Ponytail:** [[hati-ponytail]] reviews the change.
4. **Audit, in parallel:** [[hati-code-audit]], [[hati-design-audit]], iOS tester ([[hati-ios]]), scenario tester ([[hati-scenarios]]), [[hati-pixel-perfectionist]].
5. **Revise:** designer and coder fix serious findings; the finding's auditor re-checks.
6. **Ponytail again:** review of the round's diff plus an audit of `web/`; must-fix items get fixed.
7. **Ship:** [[hati-ship]] tests, commits, pushes.
8. **Judge:** [[hati-goal-judge]] decides from its own evidence whether the goal is reached.

Every agent first applies the "deeper version" prompt to its own assignment, writes a handoff in the [[Team log]], decides what falls in its specialty ([[Decisions]]), and runs the [[hati-lazy-check]] in design work.

Back to [[HATI Brain]]""", tags=("hati/process",))

# --- Design ---------------------------------------------------------------
pal = palettes()
rows = "\n".join(f"| {p['name']} | {('No. %03d' % p['n']) if p['n'] else 'original'} | " + " ".join(f"`{c}`" for c in p['sw']) + f" | `{p['acc']}` | `{p['ink']}` |" for p in pal)
note("Design/Wada themes.md", "Wada themes",
     "13 color themes. Twelve come from Sanzo Wada's *A Dictionary of Color Combinations* (1918); roles (ink, paper, accent, tints) are derived under WCAG contrast rules. Source: `app/src/main/java/com/hativ2/ui/theme/HatiPalette.kt` (the web build reads the same file).\n\n"
     "| Theme | Plate | Wada swatches | Accent | Ink |\n|---|---|---|---|---|\n" + rows + "\n\nChecked by `HatiPaletteContrastTest` (Android) and `web/tests/contrast.js` (web). Back to [[Design language]]",
     tags=("hati/design",))
note("Design language.md", "Design language",
     linkify(section(web_readme, "## Invariants")).split("- Design language:")[-1].strip() and
     "Manga ink on paper. Flat paper, 2px ink borders, 3px corners, hard offset shadow only on pressable things, ruled ledger lists, numbered volume spines, screentone on the balance panel, drawn line icons, Black Han Sans + DM Sans.\n\n"
     "- Avoid AI-slop tells: see [[hati-slop-rubric]] (manga acetate glass allowed only for floating layers).\n"
     "- Every screen passes the [[hati-lazy-check]] and the [[hati-pixel-perfectionist]] measurements.\n"
     "- Words follow [[hati-copy]].\n- Colors: [[Design/Wada themes|Wada themes]].\n- Full reference: `DESIGN.md` in the repo.\n\nBack to [[HATI Brain]]",
     tags=("hati/design",))

# --- Tech -----------------------------------------------------------------
tbl = section((ROOT / "README.md").read_text(), "### Android app", "\n---")
note("Tech/Android build.md", "Android build",
     linkify(tbl) + "\n\nOwner: [[hati-android-build]] (restore known-good, upgrade one group at a time, CI as the compiler). Back to [[HATI Brain]]",
     tags=("hati/tech",))
note("Tech/Web app.md", "Web app",
     linkify(section(web_readme, "# HATI² web app", "\n## Invariants")) + "\n\niOS rules: [[hati-ios]] · Shipping: [[hati-ship]] · Back to [[HATI Brain]]",
     tags=("hati/tech",))
sc = (ROOT / "web/tests/scenarios.md").read_text()
note("Scenarios.md", "Real-use scenarios", linkify(sc.split("\n", 1)[1]) + "\n\nRun by [[hati-scenarios]]. Back to [[HATI Brain]]", tags=("hati/tests",))

note("Glossary.md", "Glossary", """- **Volume:** one trip, household or event; holds members, expenses, payments.
- **Expense:** money one member paid, split among members (equal, exact, percent, shares). Integer cents.
- **Payment / settlement:** one member paying another back.
- **Fewest payments / pairwise:** the two debt strategies from `CalculateDebtsUseCase.kt`.
- **Share code:** JSON for one volume, sent between phones; importing merges by id.
- **G1 / G2 / G3:** works on both phones / real use with correct money / not AI slop ([[Goal]]).
- **Lazy-user check:** understood in 3 s, goal in one tap, nothing unfinished ([[hati-lazy-check]]).
- **Slop tell:** a design pattern that reads as AI-generated ([[hati-slop-rubric]]).
- **Ponytail:** change review and repo audit for bugs, risk, missing tests and code to cut ([[hati-ponytail]]).

Back to [[HATI Brain]]""", tags=("hati/glossary",))

# --- Home -----------------------------------------------------------------
note("HATI Brain.md", "HATI Brain", f"""# HATI² brain

Everything the HATI² team knows and has decided, as linked notes. This vault is the team's brain: [[Team log]] (decisions and handoffs) is edited here directly; skill notes and the other derived notes are regenerated from the repo by `tools/brain/export.py`.

## Open in the Obsidian app
- **Live (recommended):** clone the repo, then in Obsidian choose *Open folder as vault* and pick the repo folder. Code folders are hidden; this note is bookmarked. Install the community plugin **Git** (by Vinzent) once: it pulls the team's latest notes on start and every 10 minutes.
- **Snapshot:** unzip the vault you were sent and open the `HATI Brain` folder as a vault.

## Start here
- [[Goal]]: what "done" means (G1 both phones, G2 real use, G3 not AI slop)
- [[Decisions]]: settled decisions and which skill owns each kind
- [[Team loop]]: plan → do → audit → revise
- [[Team log]]: per-round handoffs between roles
- [[Roadmap]]: what to build next (Now / Next / Later), from the 2026-11-09 all-skills discussion

## Team
- [[Skills index]] ({len(SKILLS)} skills)
- Roles: [[hati-plan]] · [[hati-design]] · [[hati-code]] · [[hati-code-audit]] · [[hati-design-audit]] · [[hati-goal-judge]]
- Specialists: [[hati-ios]] · [[hati-scenarios]] · [[hati-money-parity]] · [[hati-copy]] · [[hati-a11y]] · [[hati-pixel-perfectionist]] · [[hati-ponytail]] · [[hati-android-build]] · [[hati-ship]]
- Shared checks: [[hati-lazy-check]] · [[hati-slop-rubric]] · [[hati-shots]]

## Product
- [[Design language]] · [[Design/Wada themes|Wada themes]]
- [[Tech/Web app|Web app]] · [[Tech/Android build|Android build]]
- [[Scenarios]] · [[Glossary]]
""", tags=("hati/home",))

(OUT / ".obsidian").mkdir(exist_ok=True)
(OUT / ".obsidian/app.json").write_text(json.dumps({"newLinkFormat": "shortest", "useMarkdownLinks": False, "showFrontmatter": False}, indent=2))
(OUT / ".obsidian/graph.json").write_text(json.dumps({"colorGroups": [
    {"query": "tag:#hati/skill", "color": {"a": 1, "rgb": 16707722}},
    {"query": "tag:#hati/design", "color": {"a": 1, "rgb": 2797485}},
    {"query": "tag:#hati/decisions", "color": {"a": 1, "rgb": 14566656}}]}, indent=2))
n = sum(1 for _ in OUT.rglob("*.md"))
print(f"brain/: {n} notes, {len(SKILLS)} skills")
