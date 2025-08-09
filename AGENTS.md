# Repository Guidelines

## Project Structure & Module Organization
- Source code: `src/main/java/pwn/noobs/trouserstreak/**` (modules under `commands`, `events`, `hud`, `mixin`, `modules`).
- Resources: `src/main/resources/**` (assets, `fabric.mod.json`, mixins, `streak-addon.accesswidener`).
- Build config: `build.gradle`, `gradle.properties`, `settings.gradle` (Java 21, Fabric Loom).
- Output artifacts: `build/libs/*.jar` after a successful build.

## Build, Test, and Development Commands
- `./gradlew build`: Compiles and packages the mod JAR into `build/libs/`.
- `./gradlew runClient`: Launches a Fabric dev client with the addon loaded (via Loom).
- `./gradlew clean`: Removes build outputs.
Tip: Use Java 21 (matches `sourceCompatibility`/`targetCompatibility`).

## Coding Style & Naming Conventions
- Java: 4‑space indentation, UTF‑8 encoding (configured in Gradle).
- Packages: lowercase (`pwn.noobs.trouserstreak`). Classes: PascalCase (e.g., `AutoStaircase`). Methods/fields: camelCase.
- Files live under feature folders (e.g., `modules/`, `commands/`). Keep related logic together and avoid cross‑package coupling.
- No formatter is enforced; align with existing style and keep imports organized. Prefer small, focused classes.

## Testing Guidelines
- There is no `src/test` suite at present. Validate changes with `./gradlew runClient` in a test world and exercise affected modules.
- If adding tests, place them under `src/test/java` using JUnit 5; name tests `<ClassName>Test` and target deterministic logic (parsers, math, utilities).

## Commit & Pull Request Guidelines
- Commits: keep them scoped and descriptive (e.g., `AutoStaircase: fix step timing` or `build: bump to 1.21.8`). Group refactors separately from feature changes.
- PRs: include a clear summary, user‑visible changes, and before/after notes or screenshots when UI/HUD is affected. Link related issues. Outline manual test steps (world, commands, toggles used).
- Versioning: update `gradle.properties` (`mod_version`, `minecraft_version`) and reflect changes in `fabric.mod.json` expansion if applicable.

## Security & Configuration Tips
- Keep secrets out of source; do not hardcode server addresses or tokens.
- Respect access widener and mixin scope; changes in `streak-addon.accesswidener` and `*.mixins.json` should be minimal and justified.
- Dependencies are declared in `build.gradle`; prefer pinned versions and document upgrades in PRs.

## GitHub CLI PRs
- Base branch: for versioned work, branch from `1.21.5` (mirrors upstream).
- Create branch: `git checkout -b ci/1.21.5-build 1.21.5`
- Commit CI change: edit `.github/workflows/build.yml` to build `1.21.5`, then `git add -A && git commit -m "ci: add GitHub Actions build for 1.21.5"`.
- Push branch: `git push -u origin ci/1.21.5-build`
- Open PR: `gh pr create --base 1.21.5 --head ci/1.21.5-build --title "ci: build for 1.21.5" --body "Builds 1.21.5 and uploads artifacts."`
