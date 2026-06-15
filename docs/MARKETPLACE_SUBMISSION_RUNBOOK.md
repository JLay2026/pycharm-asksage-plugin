# AskSage — JetBrains Marketplace Submission Runbook

A step-by-step guide to publishing the AskSage plugin on the [JetBrains Marketplace](https://plugins.jetbrains.com). Written for first-time submitters — no prior marketplace experience needed.

> **Gates before first upload:**
> 1. Local-install smoke test of the built ZIP must pass (Step 9d).
> 2. Legal validation issue [#14](https://github.com/JLay2026/pycharm-asksage-plugin/issues/14) must be closed (license, copyright holder, wordmark authorization on file, repo public).

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Create a JetBrains Account](#2-create-a-jetbrains-account)
3. [Create a Vendor Profile](#3-create-a-vendor-profile)
4. [Generate Plugin Signing Keys](#4-generate-plugin-signing-keys)
5. [Generate a Marketplace Publish Token](#5-generate-a-marketplace-publish-token)
6. [Add GitHub Secrets](#6-add-github-secrets)
7. [Plugin Icon Requirements](#7-plugin-icon-requirements)
8. [Plugin Metadata Checklist](#8-plugin-metadata-checklist)
9. [Build the Plugin Distribution](#9-build-the-plugin-distribution)
10. [First-Time Manual Upload](#10-first-time-manual-upload)
11. [Automated Publishing (After First Upload)](#11-automated-publishing-after-first-upload)
12. [Post-Submission: What to Expect](#12-post-submission-what-to-expect)
13. [Updating Your Plugin](#13-updating-your-plugin)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Prerequisites

Before you begin, make sure you have the following ready:

- [ ] **A computer with a terminal** (macOS Terminal, Windows WSL/PowerShell, or Linux terminal)
- [ ] **OpenSSL installed** — needed to generate signing keys
  - macOS: Already installed. Verify with `openssl version`
  - Windows: Use WSL (`wsl` in PowerShell) or install [Git for Windows](https://git-scm.com/download/win) which includes OpenSSL
  - Linux: Already installed. Verify with `openssl version`
- [ ] **Git** installed and configured
- [ ] **Java 21** installed (needed to build the plugin)
- [ ] **The repository cloned** to your machine:
  ```bash
  git clone https://github.com/JLay2026/pycharm-asksage-plugin.git
  cd pycharm-asksage-plugin
  ```

---

## 2. Create a JetBrains Account

You need a free JetBrains account to publish plugins.

1. Go to [https://account.jetbrains.com/login](https://account.jetbrains.com/login)
2. Click **"Create Account"** (or sign in if you already have one)
3. Fill in your name, email, and password
4. Verify your email by clicking the link JetBrains sends you
5. Once verified, go to [https://plugins.jetbrains.com](https://plugins.jetbrains.com) and click **"Sign In"** in the top-right corner

> **Tip:** Use the same email you plan to use for your vendor profile. This email will be associated with the plugin.

---

## 3. Create a Vendor Profile

A "Vendor" is the person or organization listed as the plugin publisher. **Ours is BigBear.ai LLC** (decision D1, 2026-06-12).

1. Go to [https://plugins.jetbrains.com](https://plugins.jetbrains.com) and sign in
2. Click your **profile icon** (top-right) → **"Upload plugin"**
3. If this is your first time, you'll be asked to:
   - **Accept the JetBrains Marketplace Developer Agreement** — must be approved on behalf of BigBear.ai LLC (see legal gate #14)
   - **Create a Vendor profile** — fill in:
     - **Vendor Name:** `BigBear.ai LLC` (must match the `<vendor>` tag in `plugin.xml`)
     - **Vendor URL:** `https://github.com/JLay2026/pycharm-asksage-plugin`
     - **Vendor Email:** `jason.layman@bigbear.ai`
4. Click **"Save"**

> **Important:** The Vendor Name on the marketplace must match what's in the `plugin.xml` file. Our plugin uses `BigBear.ai LLC`. Vendor profile details are difficult to change after publication — confirm with legal before creating.

---

## 4. Generate Plugin Signing Keys

Plugin signing is **required** for publishing. It proves the plugin hasn't been tampered with. You'll generate three things:

- A **private key** (kept secret — never share this)
- A **certificate chain** (public — proves your identity)
- A **private key password** (protects the private key)

### Step 4a: Choose a password

Pick a strong password for your private key. You'll need it later. Write it down somewhere safe. We'll refer to it as `YOUR_PASSWORD` below.

### Step 4b: Generate the private key

Open your terminal and run these commands one at a time:

```bash
# Create a folder to store your keys (keep this folder safe!)
mkdir -p ~/asksage-signing-keys
cd ~/asksage-signing-keys

# Generate an encrypted private key (you'll be prompted for your password)
openssl genpkey \
  -aes-256-cbc \
  -algorithm RSA \
  -out private_encrypted.pem \
  -pkeyopt rsa_keygen_bits:4096
```

When prompted, type `YOUR_PASSWORD` and press Enter. You'll be asked to confirm it.

### Step 4c: Create an unencrypted copy (needed for the build tool)

```bash
# Decrypt the private key (enter your password when prompted)
openssl rsa \
  -in private_encrypted.pem \
  -out private.pem
```

### Step 4d: Generate the certificate (public key)

```bash
# Generate a self-signed certificate (valid for 10 years)
openssl req \
  -key private.pem \
  -new \
  -x509 \
  -days 3650 \
  -out chain.crt
```

You'll be prompted for several fields. Here's what to enter:

| Field | What to enter |
|-------|---------------|
| Country Name | `US` |
| State or Province Name | Your state (e.g., `Virginia`) |
| Locality Name | Your city (e.g., `Springfield`) |
| Organization Name | `BigBear.ai LLC` |
| Organizational Unit Name | Press Enter to skip |
| Common Name | `AskSage Plugin Signing` |
| Email Address | `jason.layman@bigbear.ai` |

### Step 4e: Verify your files

You should now have three files in `~/asksage-signing-keys/`:

```
~/asksage-signing-keys/
├── private_encrypted.pem   ← Encrypted private key (backup copy)
├── private.pem             ← Unencrypted private key (used by build)
└── chain.crt               ← Certificate chain (used by build)
```

> **Security Warning:** Keep `private.pem` and `private_encrypted.pem` secure. Never commit them to Git. Never share them publicly. Back them up in a password manager or secure storage.

---

## 5. Generate a Marketplace Publish Token

The publish token lets the CI/CD pipeline upload new versions automatically.

1. Go to [https://plugins.jetbrains.com](https://plugins.jetbrains.com) and sign in
2. Click your **profile icon** (top-right) → **"My Tokens"**
   - Direct link: [https://plugins.jetbrains.com/author/me/tokens](https://plugins.jetbrains.com/author/me/tokens)
3. Click **"Create New Token"**
4. Give it a name like `asksage-publish`
5. Click **"Generate Token"**
6. **Copy the token immediately** — it will only be shown once!
7. Save it somewhere secure (password manager recommended)

> **Note:** This token starts with `perm:` — you need the entire string including that prefix.

---

## 6. Add GitHub Secrets

The CI/CD pipeline (`release.yml`) needs four secrets to sign and publish the plugin. You'll add them to the GitHub repository.

1. Go to [https://github.com/JLay2026/pycharm-asksage-plugin/settings/secrets/actions](https://github.com/JLay2026/pycharm-asksage-plugin/settings/secrets/actions)
2. Click **"New repository secret"** for each of the following:

| Secret Name | What to paste | How to get the value |
|-------------|---------------|----------------------|
| `CERTIFICATE_CHAIN` | The **file path** to `chain.crt` | In the CI runner, this should be the path to the certificate file. For GitHub Actions, you'll need to write the file contents to a temp file. See [Step 6a](#step-6a-encoding-file-secrets-for-github-actions) below. |
| `PRIVATE_KEY` | The **file path** to `private.pem` | Same approach as above — write contents to a temp file. |
| `PRIVATE_KEY_PASSWORD` | Your password from Step 4a | Paste the plain text password |
| `PUBLISH_TOKEN` | The token from Step 5 | Paste the full token (including `perm:` prefix) |

### Step 6a: Encoding file secrets for GitHub Actions

Our `build.gradle.kts` uses `certificateChainFile` and `privateKeyFile` (file-based), so the GitHub Actions workflow writes the secret contents to temporary files. The existing `release.yml` expects the secrets to contain **file paths**.

The simplest approach is to update the release workflow to write the file contents first. Add these steps **before** the "Publish Plugin" step in `.github/workflows/release.yml`:

```yaml
      # Write signing files from secrets
      - name: Write Signing Files
        env:
          CERTIFICATE_CHAIN_CONTENT: ${{ secrets.CERTIFICATE_CHAIN }}
          PRIVATE_KEY_CONTENT: ${{ secrets.PRIVATE_KEY }}
        run: |
          echo "$CERTIFICATE_CHAIN_CONTENT" > /tmp/chain.crt
          echo "$PRIVATE_KEY_CONTENT" > /tmp/private.pem
          echo "CERTIFICATE_CHAIN=/tmp/chain.crt" >> $GITHUB_ENV
          echo "PRIVATE_KEY=/tmp/private.pem" >> $GITHUB_ENV
```

Then update the "Publish Plugin" step's env block to use the file paths:

```yaml
      - name: Publish Plugin
        env:
          PUBLISH_TOKEN: ${{ secrets.PUBLISH_TOKEN }}
          CERTIFICATE_CHAIN: ${{ env.CERTIFICATE_CHAIN }}
          PRIVATE_KEY: ${{ env.PRIVATE_KEY }}
          PRIVATE_KEY_PASSWORD: ${{ secrets.PRIVATE_KEY_PASSWORD }}
        run: ./gradlew publishPlugin
```

With this approach, your GitHub secrets should contain:

| Secret Name | What to paste |
|-------------|---------------|
| `CERTIFICATE_CHAIN` | The **full text contents** of `chain.crt` (open the file in a text editor, select all, copy) |
| `PRIVATE_KEY` | The **full text contents** of `private.pem` (open the file in a text editor, select all, copy) |
| `PRIVATE_KEY_PASSWORD` | Your plain text password |
| `PUBLISH_TOKEN` | Your marketplace token (including `perm:` prefix) |

> **How to copy file contents on different platforms:**
> - macOS/Linux: `cat ~/asksage-signing-keys/chain.crt | pbcopy` (macOS) or `xclip -selection clipboard < ~/asksage-signing-keys/chain.crt` (Linux)
> - Windows: Open the file in Notepad, Ctrl+A, Ctrl+C

---

## 7. Plugin Icon Requirements

The plugin icon is what users see in the marketplace and in their IDE's plugin manager. JetBrains has **strict requirements**:

### Required specifications

| Requirement | Value |
|-------------|-------|
| **File name** | `pluginIcon.svg` |
| **Location** | `src/main/resources/META-INF/pluginIcon.svg` |
| **Format** | SVG (Scalable Vector Graphics) only — no PNG, JPG, or ICO |
| **Size** | Exactly **40px × 40px** (width and height in the SVG `viewBox`) |
| **Padding** | At least **2px transparent padding** around the edges |
| **Dark theme variant** (optional but recommended) | `pluginIcon_dark.svg` in the same folder |

### Design rules

- **Must be different** from the default IntelliJ Platform Plugin Template icon
- **Must NOT resemble** any JetBrains product logos (IntelliJ, PyCharm, etc.)
- **Must NOT use** the JetBrains name or logo
- Should be recognizable at both 40×40 (list view) and 80×80 (detail view) — the IDE scales it automatically
- Use filled/solid shapes — thin lines and small details won't be visible at small sizes
- Avoid text in the icon (it becomes unreadable at small sizes)

### Current status

Our plugin already includes `pluginIcon.svg` at the correct location. If you want to replace it with a custom design:

1. Create a 40×40 SVG file using a tool like [Figma](https://www.figma.com), [Inkscape](https://inkscape.org) (free), or [Sketch](https://www.sketch.com)
2. Name it exactly `pluginIcon.svg`
3. Place it at `src/main/resources/META-INF/pluginIcon.svg`
4. (Optional) Create a dark theme variant named `pluginIcon_dark.svg` in the same folder
5. Test that it looks good at both 40×40 and 80×80 by opening the SVG in a browser and zooming

### Example SVG template (40×40)

```xml
<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40">
  <!-- 2px padding: keep your design within the 4,4 to 36,36 area -->
  <rect x="4" y="4" width="32" height="32" rx="6" fill="#4A90D9"/>
  <!-- Your icon design goes here -->
</svg>
```

---

## 8. Plugin Metadata Checklist

Before submitting, verify all metadata is correct. These values come from `plugin.xml` and `gradle.properties`:

### In `src/main/resources/META-INF/plugin.xml`

- [ ] **`<id>`** — Unique plugin identifier: `asksage`
  - Must be globally unique across all JetBrains plugins
  - Cannot use `org.jetbrains` or `com.jetbrains` prefixes (reserved)
  - Cannot change after first publication (it's your permanent ID)
- [ ] **`<name>`** — Display name: `AskSage`
  - 1–4 words recommended (max 60 characters)
  - Do not include "Plugin", "JetBrains", or IDE names
  - Must be unique on the marketplace
  - Ask Sage wordmark use is authorized (see `NOTICE`); keep written authorization on file
- [ ] **`<vendor>`** — `BigBear.ai LLC` with `email` and `url` attributes
- [ ] **`<description>`** — HTML description wrapped in `<![CDATA[...]]>`
  - Must be at least 100 characters
  - Should describe what the plugin does, key features, and how to get started
  - Must keep the **Data & Privacy** disclosure (code is sent to the Ask Sage API; links to Ask Sage ToS/Privacy)
  - HTML tags allowed: `<p>`, `<b>`, `<ul>`, `<li>`, `<a>`, `<h3>`, `<ol>`, `<code>`
- [ ] **`<depends>`** — At minimum: `com.intellij.modules.platform`

### In `gradle.properties`

- [ ] **`version`** — `1.0.0` (current). Uses semantic versioning: `MAJOR.MINOR.PATCH`
- [ ] **`group`** — `asksage`
- [ ] **`pluginRepositoryUrl`** — `https://github.com/JLay2026/pycharm-asksage-plugin`

### In `build.gradle.kts`

- [ ] **`sinceBuild`** — `252` (IntelliJ 2025.2+). This means the plugin requires IntelliJ 2025.2 or newer.
- [ ] **No `untilBuild`** — The plugin is forward-compatible with all future IDE versions.
- [ ] **`changeNotes`** — rendered automatically from `CHANGELOG.md` (current version section, falls back to Unreleased)

### Licensing files

- [ ] **`LICENSE`** — Apache License 2.0, copyright 2026 BigBear.ai LLC
- [ ] **`NOTICE`** — attribution + Ask Sage wordmark authorization statement (preserved by downstream redistributors under Apache §4(d))

### In `CHANGELOG.md`

- [ ] Contains entries for all milestones under `[Unreleased]`
- [ ] Follows [Keep a Changelog](https://keepachangelog.com) format

---

## 9. Build the Plugin Distribution

Before uploading, build the plugin ZIP file locally to make sure everything compiles correctly.

### Step 9a: Build the plugin

```bash
cd pycharm-asksage-plugin

# Build the plugin distribution ZIP
./gradlew buildPlugin
```

This creates a ZIP file at:
```
build/distributions/AskSage-1.0.2.zip
```

### Step 9b: Verify the build

```bash
# Run all tests to make sure nothing is broken
./gradlew check
```

All 136 tests should pass.

### Step 9c: (Optional) Sign locally for testing

If you've set up your signing keys (Step 4), you can test signing locally:

```bash
# Set environment variables pointing to your key files
export CERTIFICATE_CHAIN=~/asksage-signing-keys/chain.crt
export PRIVATE_KEY=~/asksage-signing-keys/private.pem
export PRIVATE_KEY_PASSWORD=YOUR_PASSWORD

# Build and sign
./gradlew signPlugin
```

The signed plugin will be at `build/distributions/` with a `-signed` suffix.

### Step 9d: Local-install smoke test (REQUIRED GATE before account/key setup)

Install the actual packaged ZIP into a clean IDE — this exercises the real distribution in a way `runIde` and CI do not.

1. Open **PyCharm** (primary target; repeat in IntelliJ IDEA if available)
2. **Settings → Plugins → ⚙ → Install Plugin from Disk…** → select `build/distributions/AskSage-1.0.2.zip` → restart
   - An "unsigned plugin" warning is expected for local installs — signing happens at Marketplace upload
3. Smoke checklist:
   - [ ] Plugin appears in the Plugins list with correct name, icon, and description
   - [ ] **Settings → Tools → AskSage** opens; credentials save and persist
   - [ ] AskSage tool window opens; model list loads; one chat round-trip succeeds
   - [ ] Each editor context action fires: Explain / Refactor / Docs / Ask / Send Selection / Add to KB
   - [ ] Remapped shortcuts work (`Ctrl+Shift+Alt+E/R/D/A/K`) and `Ctrl+Alt+S` still opens Settings
   - [ ] Status bar widget renders; token dashboard loads
   - [ ] No plugin errors in **Help → Show Log** / IDE event log
4. Uninstall cleanly (no errors on removal/restart)

Only proceed to vendor profile / signing keys (Steps 2–6) once this passes.

---

## 10. First-Time Manual Upload

> **Important:** The very first version of a plugin **must** be uploaded manually through the web UI. After that, you can use automated publishing.
>
> **Pre-flight:** Step 9d smoke test passed · legal issue [#14](https://github.com/JLay2026/pycharm-asksage-plugin/issues/14) closed · repository is **public** (required for the open-source license's source link).

### Step 10a: Upload the plugin

1. Go to [https://plugins.jetbrains.com](https://plugins.jetbrains.com) and sign in
2. Click your **profile icon** → **"Upload plugin"**
3. Fill in the form:
   - **Plugin file:** Click "Choose File" and select the ZIP from `build/distributions/AskSage-1.0.2.zip`
   - **License:** Select **Apache License 2.0** (the repository license) and provide the source code link `https://github.com/JLay2026/pycharm-asksage-plugin` — open-source submissions require a public source link
   - **Tags:** Select relevant tags:
     - `AI Assistant`
     - `Code Review`
     - `Editor`
   - **Channel:** Leave as `Stable` (default) for a production release, or type `beta` for a test release
   - **Hidden:** Check this box if you want to keep it hidden after approval (you can un-hide later)
4. Click **"Upload"**

### Step 10b: Wait for approval

- JetBrains manually reviews every new plugin (and every update)
- Typical review time: **1–3 business days**
- You'll get an email when:
  - Your plugin is **approved** and live on the marketplace
  - Or if JetBrains requests **changes** before approval
- If you haven't heard back in 3–4 business days, email `marketplace@jetbrains.com`

### Common reasons for rejection

| Reason | How to fix |
|--------|-----------|
| Icon looks like a JetBrains logo | Design a unique icon that doesn't resemble IntelliJ/PyCharm logos |
| Icon is missing or wrong size | Must be exactly 40×40 SVG at `META-INF/pluginIcon.svg` |
| Description too short | Write at least 100 characters describing what the plugin does |
| Plugin name includes "Plugin" or IDE name | Remove those words from `<name>` in plugin.xml |
| No license specified | Add a license during upload |
| Third-party brand without authorization | Ask Sage wordmark use is authorized — keep the written authorization on file in case moderators ask |
| Plugin contains ads without declaration | Set `containsAds=true` if applicable (ours doesn't have ads) |

---

## 11. Automated Publishing (After First Upload)

Once the first version is uploaded manually and approved, all future versions can be published automatically through GitHub Actions.

### How it works

1. You push code to `main` → the **Build** workflow runs
2. If Build/Test/Verify pass, a **Release Draft** is created on the [GitHub Releases page](https://github.com/JLay2026/pycharm-asksage-plugin/releases)
3. You review the draft release and click **"Publish release"**
4. Publishing the release triggers the **Release** workflow, which:
   - Signs the plugin with your keys
   - Uploads it to JetBrains Marketplace via `./gradlew publishPlugin`
   - Attaches the plugin ZIP to the GitHub release

### To publish a new version

1. Update the `version` in `gradle.properties` (e.g., `1.0.0` → `1.1.0`)
2. Add release notes to `CHANGELOG.md` under a new version header
3. Commit, push to `main`, and wait for CI to pass
4. Go to [GitHub Releases](https://github.com/JLay2026/pycharm-asksage-plugin/releases)
5. Find the new draft release and click **"Edit"**
6. Review the release notes
7. Click **"Publish release"**
8. The Release workflow will sign and upload the plugin automatically

### Version numbering

| Version type | Example | When to use |
|-------------|---------|-------------|
| Patch | `1.0.1` | Bug fixes only |
| Minor | `1.1.0` | New features, backward-compatible |
| Major | `2.0.0` | Breaking changes |
| Beta | `1.1.0-beta` | Pre-release testing (publishes to `beta` channel) |

> **Channel routing:** Our `build.gradle.kts` automatically publishes versions containing `-` (like `1.1.0-beta`) to the `beta` channel and clean versions (like `1.1.0`) to the `default` (stable) channel.

---

## 12. Post-Submission: What to Expect

### Approval timeline

- **New plugins:** 1–3 business days for first review
- **Updates to approved plugins:** Usually faster (same day to 1–2 days)
- **Contact if delayed:** Email `marketplace@jetbrains.com`

### After approval

- Your plugin will appear at: `https://plugins.jetbrains.com/plugin/XXXXX-asksage` (XXXXX is your numeric plugin ID)
- Users can install it from **Settings → Plugins → Marketplace** in any IntelliJ-based IDE (IntelliJ IDEA, PyCharm, WebStorm, etc.)
- You'll be able to see download stats on your vendor dashboard

### Marketplace listing quality tips

- Add **screenshots** of the plugin in action (upload via the marketplace plugin edit page)
- Write clear **release notes** with each update
- Respond to user **reviews and ratings** on the plugin page
- Keep the plugin updated — JetBrains may flag stale plugins

---

## 13. Updating Your Plugin

### Quick update checklist

1. [ ] Make your code changes on a feature branch
2. [ ] Update `version` in `gradle.properties` (e.g., `1.0.0` → `1.1.0`)
3. [ ] Add release notes to `CHANGELOG.md`
4. [ ] Run `./gradlew check` locally to verify all tests pass
5. [ ] Merge to `main`
6. [ ] Go to GitHub Releases → find the draft → click "Publish release"
7. [ ] Wait for JetBrains approval (updates are usually faster than first submission)

### Updating the plugin icon

If you need to change the icon after publishing:

1. Replace `src/main/resources/META-INF/pluginIcon.svg` with your new 40×40 SVG
2. (Optional) Update `pluginIcon_dark.svg` for dark theme
3. Bump the version, rebuild, and publish a new version
4. The marketplace will show the new icon after the update is approved

---

## 14. Troubleshooting

### "Invalid plugin descriptor" error during build

- Make sure `plugin.xml` is valid XML
- Check that `<id>` doesn't use reserved prefixes (`org.jetbrains`, `com.jetbrains`)
- Verify `<name>`, `<vendor>`, and `<description>` are present

### Signing fails with "Invalid argument"

- Make sure `CERTIFICATE_CHAIN` and `PRIVATE_KEY` environment variables point to **file paths**, not file contents
- If using GitHub Actions, ensure the "Write Signing Files" step runs before the publish step (see Step 6a)

### "Unauthorized" error during publishing

- Verify the `PUBLISH_TOKEN` secret is set correctly (including the `perm:` prefix)
- Check that the token hasn't expired — generate a new one at [My Tokens](https://plugins.jetbrains.com/author/me/tokens)

### Plugin verification fails in CI (ClosedFileSystemException)

- This is a known upstream bug in IntelliJ Plugin Verifier when verifying against IntelliJ 2025.2
- It does **not** affect plugin functionality or marketplace submission
- The "Verify plugin" CI check is informational only and not required for publishing

### Plugin rejected for icon issues

- Must be exactly `pluginIcon.svg` (case-sensitive filename)
- Must be in `src/main/resources/META-INF/`
- Must be 40×40 pixels (check `width="40" height="40"` in the SVG)
- Must be SVG format (not PNG/JPG embedded in an SVG wrapper)
- Must not look like any JetBrains product logo

### Can't find the plugin after approval

- Search for "AskSage" in **Settings → Plugins → Marketplace** in your IDE
- Make sure your IDE version is 2025.2 or newer (the `sinceBuild` is set to `252`)
- If you uploaded to the `beta` channel, users need to add the beta channel in their IDE plugin settings

---

## Quick Reference: File Locations

| File | Purpose |
|------|---------|
| `src/main/resources/META-INF/plugin.xml` | Plugin metadata (ID, name, description, vendor) |
| `src/main/resources/META-INF/pluginIcon.svg` | Plugin icon (40×40 SVG) |
| `gradle.properties` | Version number, group ID, repository URL |
| `build.gradle.kts` | Build config, change notes, signing, publishing |
| `LICENSE` | Apache License 2.0 (copyright BigBear.ai LLC) |
| `NOTICE` | Attribution + Ask Sage wordmark authorization |
| `CHANGELOG.md` | Release notes (source of plugin change notes) |
| `.github/workflows/build.yml` | CI/CD: build, test, verify, create release draft |
| `.github/workflows/release.yml` | CI/CD: sign and publish to marketplace on release |

## Quick Reference: URLs

| Resource | URL |
|----------|-----|
| JetBrains Account | [https://account.jetbrains.com](https://account.jetbrains.com) |
| JetBrains Marketplace | [https://plugins.jetbrains.com](https://plugins.jetbrains.com) |
| My Tokens (publish token) | [https://plugins.jetbrains.com/author/me/tokens](https://plugins.jetbrains.com/author/me/tokens) |
| Plugin Signing Docs | [https://plugins.jetbrains.com/docs/intellij/plugin-signing.html](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html) |
| Marketplace Approval Guidelines | [https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html) |
| Plugin Icon Docs | [https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html](https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html) |
| GitHub Repo | [https://github.com/JLay2026/pycharm-asksage-plugin](https://github.com/JLay2026/pycharm-asksage-plugin) |
| GitHub Repo Secrets | [https://github.com/JLay2026/pycharm-asksage-plugin/settings/secrets/actions](https://github.com/JLay2026/pycharm-asksage-plugin/settings/secrets/actions) |
| GitHub Releases | [https://github.com/JLay2026/pycharm-asksage-plugin/releases](https://github.com/JLay2026/pycharm-asksage-plugin/releases) |
| Legal gate issue | [https://github.com/JLay2026/pycharm-asksage-plugin/issues/14](https://github.com/JLay2026/pycharm-asksage-plugin/issues/14) |
