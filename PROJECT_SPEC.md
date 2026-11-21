# MultiOs — Project Specification & Architecture Blueprint

## 1. Vision & Core Concept

**MultiOs** is a modular, Windows-hosted "Meta-OS" (Universal Runtime Orchestrator) written in Java.
Its goal is to act as a single desktop shell capable of launching software built for completely different target platforms (Modern Windows, Legacy Windows 98 / DOS, older/newer Java versions, Linux, Android, retro consoles such as PlayStation 2, etc.) from one unified interface.

**Core Design Philosophy:**
MultiOs does **not** reimplement OS kernels or emulators from scratch. Instead, it acts as an intelligent orchestration layer ("The Brain") that manages:

1. A unified desktop shell and application library.
2. Modular platform plugins (**Runners**) wrapping proven underlying engines (e.g., Portable JREs, DOSBox-X, PCSX2, WSL2, Android Emulator).
3. An isolated local runtime storage (**Runtime Vault**) so the host Windows OS stays clean.
4. Process supervision, lifecycle control, and centralized logging.

## 2. Simplified User Workflow (MVP Scope)

To avoid fragile OS auto-detection heuristics in early versions, the user explicitly declares the target platform:

1. **Add Software:** The user selects a local folder containing the software/game files **and** chooses its original target platform from a predefined list (e.g., `Windows Native`, `Java (Multi-Version)`, `Windows 98 / DOS`, `PlayStation 2`, `Linux`, `Android`).
2. **Entry-Point Resolution:** The selected platform plugin scans the chosen folder to locate candidate launch files (e.g., `.exe`, `.jar`, `.iso`, `.apk`). If multiple candidates exist, the UI lets the user pick the main entry point once and saves it.
3. **Runtime Check & Provisioning:** Before launching, MultiOs verifies that the required runtime/emulator exists inside its isolated `.multios/runtimes/` directory. If missing, the provisioner guides or automates downloading/extracting it.
4. **Launch & Supervise:** The plugin executes the software through its configured runtime while MultiOs tracks process status, streams `stdout`/`stderr` logs into an internal console, and allows clean termination.

## 3. Technology Stack & Constraints

- **Host OS:** Windows 10 / Windows 11 (x64).
- **Language:** Java 21 (LTS).
- **Build System:** Apache Maven (Multi-Module architecture).
- **UI Framework (MVP):** JavaFX 21 (cleanly decoupled from core logic so it can be replaced later if needed).
- **Local Persistence:** JSON (Jackson) or lightweight SQLite for storing the user's configured apps, paths, and runtime profiles inside `.multios/`.
- **Testing:** JUnit 5.
- **Coding Conventions:** All code, identifiers, comments, logs, and documentation must be written strictly in English.

## 4. Modular Architecture (Maven Multi-Module)

The project MUST enforce strict separation of concerns across four top-level modules:

```text
multios/
├── pom.xml                  # Parent POM managing dependencies & Java 21 config
├── PROJECT_SPEC.md          # This architectural source of truth
├── multios-api/             # Contracts, interfaces, and immutable domain models ONLY
├── multios-core/            # Orchestration engine, plugin loader, runtime vault, process manager
├── multios-plugins/         # Independent submodules, one per target platform
│   ├── plugin-win-native/   # Launches standard modern Windows executables
│   ├── plugin-java-multi/   # Launches JARs using isolated portable JREs (e.g., Java 8, 17, 21)
│   ├── plugin-retro-dos/    # Wraps DOSBox-X / Win98 profiles (Phase 2)
│   └── plugin-ps2/          # Wraps PCSX2 for PS2 ISOs (Phase 2)
└── multios-ui/              # JavaFX Desktop Shell (depends ONLY on multios-api & multios-core)

Key Domain Contracts (multios-api)
PlatformType (Enum or extensible descriptor): Identifies supported platforms (e.g., WINDOWS_NATIVE, JAVA_8, JAVA_21, WINDOWS_98, PLAYSTATION_2, LINUX, ANDROID).

AppEntry (Record): Persisted software item containing id, displayName, folderPath, entryPointPath, platformType, and optional platform-specific metadata (Map<String, String>).

RuntimePlugin (Interface):

PlatformType getSupportedPlatform()

List<Path> findEntryPointCandidates(Path appFolder)

RuntimeStatus checkRuntimeAvailability(Path runtimeVaultDir)

ProcessLaunchPlan buildLaunchPlan(AppEntry app, Path runtimeVaultDir)

ProcessSession / ProcessSupervisor: Wraps Java Process execution, asynchronously captures stdout/stderr streams without deadlocks, exposes live status (RUNNING, EXITED, FAILED), and supports clean process-tree termination.

5. Strict Rules for AI Development Assistants
Zero Platform Hardcoding in Core: multios-core and multios-ui must NEVER contain if (platform == PS2) or emulator-specific CLI flags. All platform knowledge belongs strictly inside its RuntimePlugin implementation.

Plugin Discovery: Register plugins via Java ServiceLoader (SPI) so adding a new platform requires only adding a new plugin module without modifying core orchestration code.

Host Isolation: Never modify Windows system environment variables (PATH, Registry) or install global dependencies. All runtimes, configs, and app databases live inside a dedicated directory (e.g., %USERPROFILE%/.multios/).

Safe Process Execution: Always use ProcessBuilder with explicit argument lists (never raw shell string concatenation) and always consume process output streams on background threads/virtual threads to prevent hangs.

Incremental Delivery: Keep classes small, cohesive, and testable. Every core service and plugin's findEntryPointCandidates / buildLaunchPlan logic must include JUnit 5 tests.

6. Phased Implementation Roadmap
Phase 1 — Foundation & MVP (Current Target):

Set up Maven multi-module skeleton (api, core, plugins, ui).

Implement AppLibraryRepository (JSON persistence), PluginRegistry (SPI), and ProcessSupervisor (with async log streaming).

Build the first two plugins: plugin-win-native and plugin-java-multi.

Build a minimal JavaFX Shell: App grid, "Add App" dialog (pick folder + pick platform), Launch button, and Live Log viewer.

Phase 2 — Retro & Legacy Platforms:

Add RuntimeVault auto-download/extraction mechanism (with progress reporting).

Implement plugin-retro-dos (DOSBox-X wrapper) and plugin-ps2 (PCSX2 CLI wrapper).

Phase 3 — Heavyweight Platforms:

Implement plugin-linux (WSL2 bridge) and plugin-android (Android Emulator + ADB bridge).

Phase 4 — Polish & Distribution:

Add app icons/metadata scraping, internal Task Manager (CPU/RAM monitoring per launched app), and package MultiOs as a standalone Windows installer via jpackage.


---
```
