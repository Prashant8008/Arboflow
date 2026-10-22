# ArborFlow — iOS & Cross-Platform Companion Suite Upgrade

ArborFlow's real-time Android command hub is being extended with an **iOS & Cross-Platform Interoperability Suite** so seniors and teammates on iPhones, iPads, and desktop web browsers can view live dependency trees, subscribe to Apple Calendar reminders, and connect an iOS SwiftUI client to the shared Firestore backend.

### User Review & Critical Decisions

> [!IMPORTANT]
> Review the cross-platform iOS interoperability plan below and click **Proceed** to build these capabilities into ArborFlow.

- **Confirmed Decision 1 — Shareable HTML Dashboard & Tree Report for iOS/Web**: Generates a self-contained, mobile-responsive dark-mode HTML5 executive report (complete with KPI cards, active blocker cascade alerts, and an interactive collapsible dependency tree) that can be shared via iMessage, Slack, Email, or opened directly in iOS Safari.
- **Confirmed Decision 2 — Apple Calendar `.ICS` Invite & `webcal://` / Universal Calendar Link Generator**: Enhances the calendar engine with Apple Calendar-optimized iCalendar headers (`X-WR-CALNAME`, `X-APPLE-STRUCTURED-LOCATION`, and native `VALARM` triggers) plus one-tap `webcal://` and universal calendar deep-link generation for iPhone teammates.
- **Confirmed Decision 3 — Cross-Platform Firebase Config & iOS SwiftUI Kit Exporter**: Adds an interactive **iOS SwiftUI & Firebase Bridge** panel that exports your live Firebase Project ID, custom Firestore Enterprise Database ID, and ready-to-compile Swift `Codable` models + `Firestore.firestore(database:)` listener code.

---

### 1. Overview & Core Concept

- **What It Does**: Bridges Android and iOS team members without requiring extra backend infrastructure:
  1. **Zero-Install iOS Executive View**: Android leads or members can generate and share a rich, interactive HTML5 snapshot of the current dependency tree, roll-up progress percentages, and active blocker cascades that renders natively in iOS Safari and QuickLook.
  2. **iOS Apple Calendar & Reminder Interop**: Exports Apple-tuned `.ics` calendar invites and universal calendar subscription links so iPhone users receive native iOS lock-screen deadline notifications.
  3. **Direct iOS SwiftUI Cloud Sync**: Provides the exact Firebase connection parameters and Swift `Codable` repository code wired to the same `tasks`, `blockers`, `members`, and `integrations` collections.
- **Target Audience / Persona**: Cross-platform engineering teams where some members or executives use iPhones/iPads or macOS while collaborating with Android users in real time.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **iOS & Web Companion Hub (in Sync & Cal Tab + Dashboard Quick Share)**:
     - **Generate iOS / Web HTML Dashboard**: One tap compiles the live Firestore task tree, roll-up metrics, workload bars, and active blocker cascades into a styled HTML5 document, previews it in-app, copies it to clipboard, or launches the Android Share Sheet to send to iOS teammates.
     - **Apple Calendar `.ICS` & Universal Link Generator**: Lets users generate Apple Calendar `.ics` payloads with custom iOS `VALARM` lead-time triggers or copy universal calendar event URLs for any task node.
     - **iOS SwiftUI Firebase Bridge Inspector**: Displays and copies the live Firebase `projectId`, named `firestore_database_id`, and generated Swift `struct TeamTask: Codable, Identifiable` + `Firestore` real-time listener snippet.
- **Visual Identity & Theme**:
  - Matches ArborFlow's **Precision Executive & Tactical Graph Studio** aesthetic (`#0B0F17` Obsidian background, `#6366F1` Electric Indigo, `#06B6D4` Cyan Telemetry, `#10B981` Emerald Pulse, and `#F43F5E` Crimson Alert) both inside the Android app and inside the generated iOS/Web HTML5 dashboard report.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Self-Contained Responsive HTML5 Report for Instant iOS Viewing**
  - *Chosen Approach*: Generate a zero-dependency, responsive HTML5 + CSS Grid/Flexbox document with embedded SVG progress rings and collapsible tree branches.
  - *Why*: Opens instantaneously in iOS Safari, iOS Mail QuickLook, Slack, or desktop browsers without requiring iOS teammates to install an app first.
- **Decision 2: Apple-Extended RFC-5545 `.ICS` + Universal Deep Links**
  - *Chosen Approach*: Include `X-WR-CALNAME:ArborFlow Sprint Tree`, `X-WR-TIMEZONE:UTC`, and both `DISPLAY` and `AUDIO` `VALARM` blocks alongside universal calendar registration links.
  - *Why*: Ensures iOS Calendar and macOS Calendar import all task deadlines and reminders with native iPhone lock-screen alerts.
- **Decision 3: Exact Schema & Named Database Parity in Swift Generator**
  - *Chosen Approach*: Generate Swift `Codable` structs with `@DocumentID` and `@ServerTimestamp` matching the exact 18-field `TeamTask` and 13-field `BlockerAlert` schemas enforced by `firestore.rules`.
  - *Why*: Guarantees that an iOS SwiftUI client passes all zero-trust Firestore security rule validations (`isValidTeamTask`, `isValidBlockerAlert`) on the first try.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                  ARBORFLOW ANDROID + iOS INTEROP LAYER                       │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │ CalendarAndIntegrationsScreen (Extended with iOS & Web Bridge Card)    │  │
│  ├──────────────────────┬─────────────────────────┬───────────────────────┤  │
│  │ iOS Safari / Web     │ Apple Calendar .ICS &   │ iOS SwiftUI Firebase  │  │
│  │ HTML5 Report Studio  │ WebCal Link Generator   │ Config & Code Exporter│  │
│  └──────────┬───────────┴────────────┬────────────┴───────────┬───────────┘  │
└─────────────┼────────────────────────┼────────────────────────┼──────────────┘
              ▼                        ▼                        ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                 IntegrationAndReminderManager (Domain Engine)                │
│  - generateIosWebDashboardHtml(tasks, blockers, members, rollUpPercent)      │
│  - generateAppleCalendarIcsAndLinks(tasks)                                   │
│  - generateSwiftUiFirebaseBridgeCode(projectId, databaseId)                  │
└──────────────────────────────────────────────────────────────────────────────┘
```

- **Interactive Component & State Mapping**:
  - **iOS & Web HTML Dashboard Generator**: Compiles live `TeamTask` tree hierarchy, `BlockerAlert` feed, and `TeamMember` workload into an interactive HTML5 string; opens an in-app preview dialog and triggers `Intent.ACTION_SEND` (`text/html` / `text/plain`) or clipboard copy.
  - **Apple Calendar Invite & WebCal Generator**: Produces Apple-tuned `.ics` files and universal calendar URLs per task node; allows instant sharing or copying for iOS teammates.
  - **iOS SwiftUI Firebase Setup Exporter**: Reads `R.string.firestore_database_id` from resources, formats the complete Swift `ArborFlowiOSBridge.swift` source file, and provides a preview + one-tap copy/share action.
