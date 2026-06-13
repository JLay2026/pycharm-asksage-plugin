# Pymatic AskSage — Local Build & Smoke-Test Runbook

How to compile the plugin from source and verify it by installing the built ZIP into a JetBrains IDE. For publishing to the JetBrains Marketplace, see [`MARKETPLACE_SUBMISSION_RUNBOOK.md`](MARKETPLACE_SUBMISSION_RUNBOOK.md).

---

## 1. Prerequisites

| Requirement | Notes |
|---|---|
| **JDK 21** | Temurin/Adoptium 21 matches CI. Verify with `java -version` (must print `21.x`). |
| **Git** | To clone/pull the repository. |
| **A JetBrains IDE 2025.2+** | PyCharm (primary target) or IntelliJ IDEA, for the install smoke test. |

No separate Gradle install is needed — the Gradle wrapper (`gradlew` / `gradlew.bat`) downloads the correct Gradle and the IntelliJ Platform dependency on first run.

---

## 2. Get the source

```bash
git clone https://github.com/JLay2026/pycharm-asksage-plugin.git
cd pycharm-asksage-plugin
# or, if already cloned:
git checkout main
git pull origin main
```

---

## 3. Configure Java (one-time)

### macOS / Linux
```bash
java -version            # confirm 21.x; if not, install a JDK 21 and set JAVA_HOME
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"   # macOS example
```

### Windows (PowerShell)
```powershell
# Point JAVA_HOME at your JDK 21 install (adjust the folder name to match what's installed)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Persist for future terminals (user-level)
[Environment]::SetEnvironmentVariable("JAVA_HOME", $env:JAVA_HOME, "User")

java -version   # should print openjdk version "21.0.x"
```

> **Windows gotcha:** if `gradlew.bat` reports `JAVA_HOME is set to an invalid directory` with garbage characters, an existing `JAVA_HOME` (often machine-level) is corrupted. Fix it via **Start → "Edit the system environment variables"** → Environment Variables, and set `JAVA_HOME` to the JDK path above (delete any malformed value). A new terminal may inherit a machine-level value over a user-level one.

---

## 4. Build the plugin

### Windows
```powershell
.\gradlew.bat buildPlugin
```

### macOS / Linux
```bash
./gradlew buildPlugin
```

**Output:** `build/distributions/Pymatic AskSage-1.0.0.zip` — this is the installable/distributable artifact.

Notes:
- The **first build is slow** (several minutes): it downloads IntelliJ IDEA 2025.2 and dependencies. Later builds are fast (Gradle build + configuration cache are enabled).
- `buildSearchableOptions` briefly launches a headless IDE to index settings — this is normal, not a hang. A JVM `cds`/`PathClassLoader` warning during this step is harmless.

### Optional: run tests and verification
```bash
./gradlew check          # compile + unit tests
./gradlew verifyPlugin   # IntelliJ Plugin Verifier (binary compatibility)
```

### Optional: launch a sandbox IDE with the plugin loaded
```bash
./gradlew runIde
```

---

## 5. Local-install smoke test (the real verification)

Installing the built ZIP exercises the packaged distribution — proving the plugin's classes wire up at runtime, which `runIde` and CI alone do not fully guarantee.

1. Open **PyCharm** (repeat in IntelliJ IDEA if available).
2. **Settings → Plugins → ⚙ (gear) → Install Plugin from Disk…** → select `build/distributions/Pymatic AskSage-1.0.0.zip` → **restart**.
   - An **"unsigned plugin" warning is expected** for local installs; signing happens only at Marketplace upload.
3. Smoke checklist:
   - [ ] Plugin appears in the Plugins list with correct name, icon, and description
   - [ ] **Settings → Tools → Pymatic AskSage** opens; email + API key save and persist across restart
   - [ ] **AskSage** tool window opens (right sidebar); model list loads; one chat round-trip succeeds
   - [ ] Editor context actions fire: Explain Code / Refactor / Generate Documentation / Ask About This File / Send Selection / Add to Knowledge Base
   - [ ] Remapped shortcuts work: `Ctrl+Shift+Alt+E/R/D/A/K`; and `Ctrl+Alt+S` still opens IDE Settings (no conflict)
   - [ ] Status bar widget shows current live mode + model
   - [ ] Token usage dashboard loads
   - [ ] No plugin errors in **Help → Show Log in Explorer/Finder** or the IDE event log
4. Uninstall cleanly (no errors on removal/restart).

If all boxes pass, the build is good to proceed toward signing and Marketplace submission.

---

## 6. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `JAVA_HOME is not set` / invalid directory | JDK 21 not on PATH or `JAVA_HOME` corrupted — see §3. |
| `Unsupported class file major version` / Java version error | Wrong JDK. Must be Java 21. |
| Build fails in `:patchPluginXml` with config-cache serialization error | Change notes must be rendered eagerly (already handled in `build.gradle.kts`). If reintroduced, avoid capturing the `changelog` extension inside a `Provider` lambda. |
| Kotlin compiler `OOMErrorException` | Raise heap: add `kotlin.daemon.jvmargs=-Xmx4g` (or higher) to `gradle.properties`, or pass `-Dkotlin.daemon.jvmargs=-Xmx4g`. |
| `buildSearchableOptions` seems stuck | It boots a headless IDE; give it 1–2 minutes. The `cds`/D-Bus warnings are benign. |
| Plugin Verifier `ClosedFileSystemException` | Known upstream issue verifying against IntelliJ 2025.2; informational, not a publish blocker. |

---

## Quick reference

```bash
# Full local validation, start to finish
git pull origin main
./gradlew clean buildPlugin check verifyPlugin
# → build/distributions/Pymatic AskSage-1.0.0.zip
# → install from disk in PyCharm and run the §5 checklist
```
