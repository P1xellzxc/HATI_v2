---
title: "hati-money-parity"
tags: ["hati/skill"]
---

> Prove the HATI² web money math matches the Android CalculateDebtsUseCase and always keeps its invariants, using fuzzing and, where possible, the real Kotlin code on the JVM. Use when auditing or changing splits, balances, settlements or debt simplification.

Source: `.claude/skills/hati-money-parity/SKILL.md` · Back to [[HATI Brain]] · [[Skills index]]


# HATI² money parity

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. This is G2: money must be right on both platforms.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task
1. **Top professional:** doesn't trust example-based tests for money; states invariants and hammers them with random inputs, and compares against the reference implementation instead of re-deriving it.
2. **Rawest material:** `app/src/main/java/com/hativ2/domain/usecase/CalculateDebtsUseCase.kt` (reference), the JS engine in `web/src/app.src.html` (`splitEqual`, `splitWeighted`, `balances`, `simplify`, `pairwise`), and saved user data shapes.
3. **Methods/tools:** a node fuzz script that extracts the JS functions from the built page and runs thousands of random volumes; invariants below; for parity, compile the pure-Kotlin use case on the JVM (Maven Central is reachable: kotlin-compiler-embeddable plus stdlib; Android SDK is not needed for that file) and compare outputs on the same random inputs. If the Kotlin route fails, port the Kotlin logic line by line into a node reference and compare.
4. **What shows it's right:** zero invariant violations over ≥10,000 random cases; identical transactions (to the cent) between JS and Kotlin on the same inputs; a seed printed for any failure.


## Rewritten prompt (this is how you operate)

For your assignment, write a node fuzz harness that loads the built page's money functions and checks the invariants below over at least 10,000 random volumes (print the seed of any failure). Then compare against `CalculateDebtsUseCase.kt`, on the JVM if you can get the Kotlin compiler from Maven Central, otherwise via a line-by-line node port of the Kotlin. Report every mismatch with its inputs and both outputs.

## Invariants
- Every split sums exactly to its expense amount (integer cents).
- Balances across a volume sum to zero.
- Applying all suggested payments as settlements leaves every balance within 1 cent of zero.
- Simplify never produces more payments than members − 1; pairwise never has both A→B and B→A.
- Kotlin rounds doubles to 2 decimals; differences of 1 cent from that rounding must be explained, not ignored.
