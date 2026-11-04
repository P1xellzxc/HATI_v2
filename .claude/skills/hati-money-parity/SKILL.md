---
name: hati-money-parity
description: Prove the HATI² web money math matches the Android CalculateDebtsUseCase and always keeps its invariants, using fuzzing and, where possible, the real Kotlin code on the JVM. Use when auditing or changing splits, balances, settlements or debt simplification.
---

# HATI² money parity

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. This is G2: money must be right on both platforms.

## Deeper version of this task
1. **Top professional:** doesn't trust example-based tests for money; states invariants and hammers them with random inputs, and compares against the reference implementation instead of re-deriving it.
2. **Rawest material:** `app/src/main/java/com/hativ2/domain/usecase/CalculateDebtsUseCase.kt` (reference), the JS engine in `web/src/app.src.html` (`splitEqual`, `splitWeighted`, `balances`, `simplify`, `pairwise`), and saved user data shapes.
3. **Methods/tools:** a node fuzz script that extracts the JS functions from the built page and runs thousands of random volumes; invariants below; for parity, compile the pure-Kotlin use case on the JVM (Maven Central is reachable: kotlin-compiler-embeddable plus stdlib; Android SDK is not needed for that file) and compare outputs on the same random inputs. If the Kotlin route fails, port the Kotlin logic line by line into a node reference and compare.
4. **What shows it's right:** zero invariant violations over ≥10,000 random cases; identical transactions (to the cent) between JS and Kotlin on the same inputs; a seed printed for any failure.

## Invariants
- Every split sums exactly to its expense amount (integer cents).
- Balances across a volume sum to zero.
- Applying all suggested payments as settlements leaves every balance within 1 cent of zero.
- Simplify never produces more payments than members − 1; pairwise never has both A→B and B→A.
- Kotlin rounds doubles to 2 decimals; differences of 1 cent from that rounding must be explained, not ignored.

