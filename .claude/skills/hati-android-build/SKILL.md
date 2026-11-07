---
name: hati-android-build
description: Own every Android build and dependency decision for HATI² (Gradle, AGP, Kotlin, KSP, Hilt, AndroidX versions, compileSdk) and get CI green: restore known-good states, upgrade one step at a time, and verify each step in CI. Use when the Android build or CI fails, or before any dependency change.
---

# HATI² Android build owner

## Goal first

Use this skill for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS, **G2** real group use with correct money, **G3** not AI slop. The native Android app is part of G1 and G2, and a build that can't compile blocks every Kotlin change, including the palette contrast test.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given, every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous, never more scope.

## Deeper version of this task

1. **What a top professional does that nobody asked for.** A senior Android build engineer never bulk-upgrades: they find the last state that provably built, restore it, and then move one dependency (or one tightly coupled group, like AGP + Gradle + Kotlin + KSP) per step, reading each library's release notes for its minimum AGP, compileSdk and Kotlin. They keep the toolchain matrix written down so nobody has to rediscover it.
2. **The rawest material.** The raw CI logs (`mcp__github__get_job_logs`), `gradle/libs.versions.toml`, `app/build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`, `git log -p` of those files, and old build evidence in history (for example the pre-upgrade `build_result.txt`).
3. **Methods and tools.** Bisect version changes through history; the CI run as the compiler (no Android SDK or Google Maven access in this sandbox); one change per push; read the first error only and fix its root cause; record the working matrix (AGP, Gradle, Kotlin, KSP, compileSdk, Hilt, Compose BOM) in the team log.
4. **What shows the result is right, other than you or the user.** CI "Lint + Unit Tests" and "Instrumented Tests (API 34)" both green on the pushed commit; the same matrix rebuilds green on the next push.

## Rewritten prompt (this is how you operate)

Read the latest failing CI job log. If the build has never been green on the current version set, restore the last version set with evidence of a successful build, push, and read CI. Then fix the first remaining error at its root, one change per push, until both CI jobs are green. Only then upgrade, one coupled group at a time, each confirmed by CI. Record every decision and the working toolchain matrix under Decisions in `brain/Team log.md`.

Known history: commit `4000388` bulk-upgraded 13 libraries without building; Hilt 2.59.2 requires AGP 9 while the project uses AGP 8.7.2. The pre-upgrade set (Hilt 2.52, Room 2.6.1, Compose BOM 2025.01.01, core-ktx 1.15.0, lifecycle 2.8.7, activity-compose 1.10.0, navigation-compose 2.8.5, datastore 1.1.1) built successfully on the owner's machine.
