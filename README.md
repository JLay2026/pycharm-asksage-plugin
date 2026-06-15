# AskSage Plugin

An IntelliJ Platform plugin that integrates the [AskSage.ai](https://asksage.ai) API directly into JetBrains IDEs, providing conversational AI assistance with user-selectable internet access modes and dynamic model selection.

## Features

- **Live Mode Toggle** — Switch between No Live (offline), Live (web search), and Live+ (deep research) modes
- **Model Selection** — Choose from all available AI models, dynamically fetched from AskSage
- **Chat Interface** — Streaming responses with markdown rendering, multi-turn conversation history, persona and dataset selectors, and clickable follow-up questions
- **Editor Context Actions** — Right-click menu: Explain Code, Refactor, Generate Docs, Ask About File, Send Selection to AskSage
- **Keyboard Shortcuts** — `Ctrl+Shift+Alt+E` (Explain), `Ctrl+Shift+Alt+R` (Refactor), `Ctrl+Shift+Alt+D` (Docs), `Ctrl+Shift+Alt+A` (Ask), `Ctrl+Shift+Alt+K` (Add to Knowledge Base). Send Selection has no default shortcut — assign one in **Settings > Keymap** if desired
- **Plugin Browser** — Browse and execute AskSage plugins with results rendered inline
- **Knowledge Base Training** — Add code from the editor directly to your AskSage datasets
- **Token Usage** — View monthly token usage for the current application
- **Status Bar Widget** — Current live mode and model displayed at the bottom of the IDE
- **Secure Authentication** — API key stored securely via IntelliJ PasswordSafe with automatic 24-hour token exchange

## Data & Privacy

The plugin sends your prompts and any code you explicitly share (selections, files sent via context actions, knowledge-base uploads) to the [Ask Sage](https://asksage.ai) service. No code is transmitted without an explicit user action. Use is governed by the Ask Sage Terms of Service and Privacy Policy; a paid Ask Sage account and API key are required.

### Credentials & uninstalling

Your API key and email are stored in the IDE's secure storage (IntelliJ PasswordSafe), and your preferences (server URL, defaults) are stored in the IDE configuration. **IntelliJ does not remove this data when the plugin is uninstalled.** To remove it, open **Settings > Tools > AskSage** and use **Clear Credentials / Sign Out** (removes the stored API key and email) and **Reset Settings** (restores preferences to defaults) before uninstalling.

## Requirements

- IntelliJ IDEA 2025.2+ (or any JetBrains IDE based on the IntelliJ Platform)
- An [AskSage.ai](https://asksage.ai) account with API key

## Getting Started

1. Install the plugin from the JetBrains Marketplace (or build from source)
2. Go to **Settings > Tools > AskSage**
3. Enter your AskSage email and API key
4. Open the **AskSage** tool window (right sidebar)
5. Select a model, choose your live mode, and start chatting

## Building from Source

```bash
./gradlew build
```

To run the plugin in a development IDE instance:

```bash
./gradlew runIde
```

To run tests:

```bash
./gradlew check
```

## Architecture

```
src/main/kotlin/asksage/
  api/                  # AskSage API client, endpoints, auth, DTOs
  actions/              # Editor context actions (Explain, Refactor, Docs, etc.)
  services/             # Registry services, chat session, settings state
  ui/
    toolwindow/         # Chat panel, plugin browser, token usage, selectors
    settings/           # Settings configurable UI
    statusbar/          # Status bar widget
  util/                 # Markdown renderer, live mode enum, notification helper
```

## API Coverage

| Endpoint | Status |
|----------|--------|
| `/user/get-token-with-api-key` | Implemented |
| `/server/get-models` | Implemented |
| `/server/get-personas` | Implemented |
| `/server/get-datasets` | Implemented |
| `/server/query` | Implemented (standard + streaming) |
| `/server/follow-up-questions` | Implemented |
| `/server/get-plugins` | Implemented |
| `/server/execute-plugin` | Implemented |
| `/server/train` | Implemented |
| `/user/count-monthly-tokens` | Implemented |

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
Copyright 2026 BigBear.ai LLC. See [NOTICE](NOTICE) for attribution and
trademark information, including authorized use of the Ask Sage wordmark.
