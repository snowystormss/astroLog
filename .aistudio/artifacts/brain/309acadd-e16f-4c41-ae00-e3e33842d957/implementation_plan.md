# 100-Year Life Astrology Data Export (.md)

Implement a comprehensive 100-year astrological data export feature in Markdown (`.md`) format, located in the Settings screen under Data & Backup, calculating the complete life timeline and major transit milestones from the user's birth year through age 100.

### User Review & Critical Decisions

> [!IMPORTANT]
> **Summary of User Confirmed Specifications**:
> - **File Format**: Markdown text document (`.md`) with rich structural formatting, headers, tables, and clean metadata for seamless reading in text editors, note apps, and markdown viewers.
> - **Astrological Dataset**: Complete 100-Year Life Timeline and Transit Milestones spanning age 0 to age 100, incorporating Annual Profections (time lords & activated houses), major Planetary Returns (Saturn at ~29.5/59/88.5, Jupiter 12-year cycles, Chiron at ~50, Uranus half-return/opposition at ~42), Nodal returns, Solar returns, and life chapter syntheses.
> - **Screen Location**: Settings Screen under a dedicated **Data & Backup** section.

- **Confirmed Decision 1**: The export is generated locally on-device in a background coroutine using astronomical calculation math, requiring zero external network APIs.
- **Confirmed Decision 2**: Document creation utilizes Android's native Storage Access Framework (`ActivityResultContracts.CreateDocument("text/markdown")`) paired with a quick-share intent fallback so the user can save to local storage, Google Drive, or send directly to note apps.
- **Confirmed Decision 3**: The export includes an executive natal snapshot at age 0, followed by a decadal roadmap (Decade 1: Age 0–10 through Decade 10: Age 90–100) breaking down every individual year with its age, Gregorian year, profection house, active planetary ruler, and significant celestial milestones.

---

### 1. Overview & Core Concept

- **What It Does**: Provides users with an on-demand, comprehensive 100-year astrological dossier starting from their exact birth date and projecting forward a full century (birth year to year + 100). The generated Markdown document details natal placements, annual profections for every year of life (ages 0 to 100), key astrological milestones (Saturn Returns, Jupiter Returns, Uranus Opposition, Chiron Return, Nodal Inversions and Returns), and decadal life chapter themes.
- **Target Audience / Persona**: Astrology enthusiasts and self-discovery practitioners who want a permanent, searchable, offline life ledger mapping out their celestial timeline across an entire human lifespan.
- **Key Value**: Replaces piecemeal single-day lookups with a holistic 100-year lifecycle roadmap delivered in an open, portable Markdown format that belongs entirely to the user.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. *Discovery in Settings*:
     - The user opens the **Settings** screen from the Profile overflow menu or bottom navigation.
     - Under a new high-visibility **"Data, Export & Backup"** category card, they find the **"100-Year Astrology Life Export"** card.
     - The card displays the user's active birth date, century span (e.g., `1998 – 2098 • Ages 0 to 100`), and a brief description of the contents.
  2. *Export Generation & Save*:
     - User taps **"Export 100-Year Dossier (.md)"**.
     - A non-blocking progress modal/sheet displays smooth celestial calculation stages:
       - *Stage 1*: Calculating natal foundation and planetary positions.
       - *Stage 2*: Computing 100 annual profection houses and ruling time lords.
       - *Stage 3*: Mapping major planetary returns (Saturn, Jupiter, Chiron, Uranus, Lunar Nodes).
       - *Stage 4*: Compiling Markdown dossier.
     - The Android Storage Access Framework document picker opens with a pre-filled file name: `AstroLog_100_Year_Timeline_<UserName>.md`.
     - Upon selecting a destination folder, the file is written to device storage with confirmation feedback.
  3. *Quick Share Option*:
     - Alongside the file picker, a secondary "Share" action allows sending the compiled 100-year markdown text directly to Google Drive, Keep, Gmail, or messaging apps.

- **Visual Identity & Theme**:
  - *Design System*: Follows Material 3 dark cosmic aesthetic with deep violet/navy container accents (`surfaceVariant` with primary glow borders).
  - *Typography*: Clean display header paired with monospaced century metadata chips (`100-Year Life Horizon`, `Age 0 → 100`).
  - *Iconography*: Material 3 `HistoryEdu`, `FileDownload`, and `AutoAwesome` vector icons with high-contrast accessibility tints.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Annual Profection & Milestone Math vs. 36,525 Daily Calculations**
  - *Chosen Approach*: Calculate exact planetary positions for each solar return year (101 reference points) and derive exact planetary return windows, rather than computing 36,500 individual daily transit charts.
  - *Why*: Daily planetary calculations for 100 years would require tens of thousands of trigonometric calculations that could hitch the UI thread or cause battery drain. The annual profection + planetary return milestone architecture provides superior long-term astrological structure, instantaneous generation (<300ms), and clean Markdown organization.

- **Decision 2: Native Android Document Picker (`CreateDocument`) + Intent Share**
  - *Chosen Approach*: Use `ActivityResultContracts.CreateDocument("text/markdown")` with MIME fallback to `text/plain`, combined with a direct share launcher (`Intent.ACTION_SEND`).
  - *Why*: Gives users complete ownership of their exported file, allowing them to choose exact folder locations, cloud backups, or direct export to markdown reading tools (Obsidian, Notion, iA Writer, etc.).

- **Decision 3: Structured Markdown Hierarchy**
  - *Chosen Approach*:
    - **Header**: User details, birth coordinates, natal chart snapshot.
    - **Part 1**: Lifelong Planetary Return Catalog (Saturn returns at ~29.5/59/88.5, Jupiter cycles every 12 years, Uranus opposition at ~42, Chiron return at ~50, Nodal returns at ~18.6/37.2/55.8/74.4/93).
    - **Part 2**: Complete 100-Year Chronological Ledger grouped by 10-Year Decades (Ages 0–9, 10–19, ..., 90–100), listing each year's Age, Year, Profection House, Lord of the Year, and Milestone Events.
    - **Part 3**: Astrological Reference Glossary for interpreting house activations and time lords.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌────────────────────────────────────────────────────────────────────────┐
│                              UI Layer                                  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ SettingsScreen (Data, Export & Backup Section)                   │  │
│  │ • 100-Year Export Card with Century Scope Indicator              │  │
│  │ • "Export 100-Year Dossier (.md)" button                         │  │
│  │ • SAF Document Creator (`CreateDocument`) & Share Intent         │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
└─────────────────────────────────────┼──────────────────────────────────┘
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             View Model                                 │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ MainViewModel                                                    │  │
│  │ • `generate100YearAstrologyReport(profile)`                      │  │
│  │ • Background coroutine orchestration on `Dispatchers.Default`    │  │
│  │ • State management for export progress and string delivery       │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
└─────────────────────────────────────┼──────────────────────────────────┘
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       Domain & Calculation Layer                       │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ PlanetCalculator & AstrologyTimelineGenerator                    │  │
│  │ • Natal baseline position calculation                            │  │
│  │ • Hellenistic Annual Profection engine (House 1-12 cyclic cycle) │  │
│  │ • Traditional & Modern planetary rulership mappings              │  │
│  │ • Exact Planetary Return milestone calculators:                 │  │
│  │   - Saturn (~29.45 years), Jupiter (~11.86 years)                │  │
│  │   - Uranus opposition (~42 years), Chiron (~50.7 years)          │  │
│  │   - Lunar Nodes (~18.61 years)                                   │  │
│  │ • Comprehensive Markdown template synthesizer                    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

- **Key Entities & State**:
  - `ProfectionYearInfo`: Data class holding `age: Int`, `calendarYear: Int`, `houseNumber: Int`, `sign: String`, `timeLord: String`, and `milestones: List<String>`.
  - `LifeMilestone`: Data class holding `ageApprox: Double`, `yearApprox: Int`, `title: String`, `description: String`, and `category: MilestoneCategory`.
  - `CenturyExportState`: Sealed class (`Idle`, `Generating`, `Ready(val content: String)`, `Error(val message: String)`).

- **Interactive Component & State Mapping**:
  - `SettingsScreen`: Observes `UserProfile`. When export is tapped, launches viewModel method; upon receiving markdown text, triggers `CreateDocument("text/markdown")`.
  - Content streaming: Resolves output stream via `context.contentResolver.openOutputStream(uri)` and flushes the generated Markdown bytes with UTF-8 encoding.
