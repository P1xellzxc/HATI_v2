---
title: "Skills index"
tags: ["hati/index"]
---

Every role and shared tool the team loop uses. Each skill starts with the goal, the "deeper version" questions, and its rewritten prompt.

- [[hati-a11y]]: Check and fix HATI² accessibility (keyboard, screen reader semantics, focus, contrast in all 13 palettes and both themes, zoom, reduced motion) with measured results. Use when auditing or changing UI structure, colors or interaction.
- [[hati-android-build]]: Own every Android build and dependency decision for HATI² (Gradle, AGP, Kotlin, KSP, Hilt, AndroidX versions, compileSdk) and get CI green: restore known-good states, upgrade one step at a time, and verify each step in CI. Use when the Android build or CI fails, or before any dependency change.
- [[hati-code]]: Implement planned HATI² web app changes in web/src/app.src.html with a test for each, keeping money math, escaping, and both build outputs correct. Use for the "do" step of an iteration, or to fix a reproduced code-audit finding.
- [[hati-code-audit]]: Adversarially audit the HATI² web app code for money errors, data loss, security holes, broken navigation and accessibility semantics, with an executable reproduction for every finding. Use for the "audit" step of each iteration.
- [[hati-copy]]: Write and review every word in the HATI² web app (labels, buttons, errors, empty states, toasts, share instructions) in one consistent, plain voice. Use when changing UI text or auditing for generic, AI-sounding or inconsistent copy.
- [[hati-design]]: Design and implement visual changes to the HATI² web app in its manga ink-and-paper language, avoiding AI-slop tells, across both themes and all 13 Wada palettes. Use for design-owned plan items or design-audit findings.
- [[hati-design-audit]]: Audit the rendered HATI² web app against an AI-slop rubric, the manga ink-and-paper language, contrast, and first-time comprehension, from fresh screenshots in both themes and several palettes. Use for the "audit" step of each iteration.
- [[hati-goal-judge]]: Independently decide at the end of each HATI² round whether each done criterion in web/README.md is met, from fresh evidence, and settle disagreements recorded in the team log. Use only as the round's judge; never for doing the work being judged.
- [[hati-ios]]: Check and fix how the HATI² web app behaves as an iPhone home-screen app (WebKit, Add to Home Screen, storage, safe areas, input zoom, offline). Use when planning, coding, or auditing anything that affects iOS users.
- [[hati-lazy-check]]: Run the lazy-but-meticulous user check on the HATI² web app (understood in 3 seconds, goal in one tap, nothing unfinished) across first screen, one-tap paths, phone fit, states and copy. Use in every design step, every design audit, and when planning against the done criteria.
- [[hati-money-parity]]: Prove the HATI² web money math matches the Android CalculateDebtsUseCase and always keeps its invariants, using fuzzing and, where possible, the real Kotlin code on the JVM. Use when auditing or changing splits, balances, settlements or debt simplification.
- [[hati-pixel-perfectionist]]: Inspect the HATI² web app with an obsessive eye for detail — misaligned text and edges, uneven spacing, off-grid sizes, off-center icons, visual imbalance, inconsistent borders, radii and type — measured in pixels, not guessed. Use after every design or layout change and in every design audit.
- [[hati-plan]]: Plan the next HATI² iteration toward the goal (installable on Android and iOS, usable for real group expenses, not AI-slop-looking). Use at the start of each plan→do→audit→revise round, or when audit findings need to be turned into a ranked, testable work list.
- [[hati-ponytail]]: Run a ponytail quality review on every HATI² change (code and design diffs) and a ponytail audit of web/ before each ship — correctness, safety, load, missing tests, speed and what to cut, in plain English with fixes. Use after every Do and Revise step, especially code changes, and before shipping.
- [[hati-scenarios]]: Run and extend the library of real-use HATI² scenarios (whole jobs like splitting a dinner or sharing a trip between two phones) as end-to-end checks, measuring taps, errors and dead ends. Use when planning, coding acceptance checks, or auditing usability.
- [[hati-ship]]: Ship a HATI² round safely: run the tests, rebuild deterministically, make sure installed copies update, commit with the house format, push with retries, and republish the preview. Use for the ship step of each round.
- [[hati-shots]]: Take the standard HATI² screenshot set (every view, sheet and key state at 360/390 px, light/dark, three palettes) with one command, so every role judges the same evidence. Use before any design decision, design audit, plan, or before/after comparison.
- [[hati-slop-rubric]]: Score the rendered HATI² web app against the AI-slop rubric (0 absent, 1 partly, 2 present per tell) with screenshot evidence, and know the manga-native alternatives. Use in every design step and design audit.

Back to [[HATI Brain]]
