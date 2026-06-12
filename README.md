# Pymatic AskSage Plugin

An IntelliJ Platform plugin that integrates the [AskSage.ai](https://asksage.ai) API directly into JetBrains IDEs, providing conversational AI assistance with user-selectable internet access modes and dynamic model selection.

## Features

- **Live Mode Toggle** — Switch between No Live (offline), Live (web search), and Live+ (deep research) modes
- **Model Selection** — Choose from all available AI models, dynamically fetched from AskSage
- **Chat Interface** — Streaming responses with markdown rendering, multi-turn conversation history, persona and dataset selectors, and clickable follow-up questions
- **Editor Context Actions** — Right-click menu: Explain Code, Refactor, Generate Docs, Ask About File, Send Selection to AskSage
- **Keyboard Shortcuts** — `Ctrl+Alt+E` (Explain), `Ctrl+Alt+R` (Refactor), `Ctrl+Alt+D` (Docs), `Ctrl+Alt+A` (Ask), `Ctrl+Alt+S` (Send Selection), `Ctrl+Alt+K` (Add to Knowledge Base)
- **Plugin Browser** — Browse and execute AskSage plugins with results rendered inline
- **Agent Execution** — Run AskSage agents for complex multi-step tasks
- **Knowledge Base Training** — Add code from the editor directly to your AskSage datasets
- **Token Usage Dashboard** — Monitor monthly, daily, and remaining token usage with a color-coded progress bar
- **Status Bar Widget** — Current live mode and model displayed at the bottom of the IDE
- **Secure Authentication** — API key stored securely via IntelliJ PasswordSafe with automatic 24-hour token exchange

## Requirements

- IntelliJ IDEA 2025.2+ (or any JetBrains IDE based on the IntelliJ Platform)
- An [AskSage.ai](https://asksage.ai) account with API key

## Getting Started

1. Install the plugin from the JetBrains Marketplace (or build from source)
2. Go to **Settings > Tools > Pymatic AskSage**
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
src/main/kotlin/org/jetbrains/plugins/template/
  api/                  # AskSage API client, endpoints, auth, DTOs
  actions/              # Editor context actions (Explain, Refactor, Docs, etc.)
  services/             # Registry services, chat session, settings state
  ui/
    toolwindow/         # Chat panel, plugin browser, agent panel, token usage, selectors
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
| `/server/list-agents` | Implemented |
| `/server/execute-agent` | Implemented |
| `/server/execute-plugin` | Implemented |
| `/server/train` | Implemented |
| `/user/count-monthly-tokens` | Implemented |

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
Copyright 2026 BigBear.ai LLC. See [NOTICE](NOTICE) for attribution and
trademark information, including authorized use of the Ask Sage wordmark.
