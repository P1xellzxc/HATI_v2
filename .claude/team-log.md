# HATI² team log

Shared notes between the persona agents (planner, critic, designer, coder, code auditor, design auditor). Newest round at the bottom. Read it before you start; append your handoff when you finish.

## Decisions (settled; don't reopen without new evidence)

- The web app (`web/src/app.src.html` → `docs/app/`) is the product for both Android and iOS users; iOS installs via Add to Home Screen. No native iOS build.
- Kotlin/Android code is out of scope for the loop until the owner chooses Hilt 2.52 vs AGP 9 (CI cannot build it today).
- Design language is manga ink on paper (see `web/README.md`). Glassmorphism and blurred gradient blobs were removed as AI-slop tells and stay out.
- Every persona decides from the goal in `web/README.md` (G1 both phones, G2 real use, G3 not AI slop) and the lazy-user check is part of every design step.
- Money is integer cents; debt math mirrors `CalculateDebtsUseCase.kt`.

## How to write an entry

```
### Round N · <role>
- Goal moved: G1/G2/G3 and how
- Did / found: ...
- For @planner / @critic / @designer / @coder / @code-auditor / @design-auditor: questions or requests, one per line
- Disagree: what and why (the planner settles it against the goal and records it under Decisions)
```

## Rounds
