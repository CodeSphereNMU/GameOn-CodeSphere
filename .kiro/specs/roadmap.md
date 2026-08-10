# GameOn - Feature Roadmap

## Use-Case Catalogue

All 24 formal use cases as defined in the project Backlog Ownership Chart (BOC).

**Source:** `BOC2026.xlsx`, worksheet "Development Schedule", Use Case Glossary section.

| ID | Official Name (from BOC) | Owner | Status | Kiro Spec |
|----|--------------------------|-------|--------|-----------|
| A100 | Create Game Listing | Lihlumelo | Implemented, unit tested, manually verified | Yes (detailed) |
| A200 | Browse Listings | Lihlumelo | Schema-supported | No |
| A300 | Send Join request | Lihlumelo | Schema-supported | No |
| A400 | Leave Game listing | Lihlumelo | Schema-supported | No |
| A500 | Hide expired listings | Lihlumelo | Planned | No |
| A600 | Send game reminders | Lihlumelo | Planned | No |
| A700 | Confirm session | Lihlumelo | Planned | No |
| B100 | Create posts | Zane | Schema-supported | No |
| B200 | Manage posts | Zane | Schema-supported | No |
| B300 | Browse posts | Zane | Planned | No |
| B400 | View Reports | Zane | Schema-supported | No |
| B500 | View Leaderboards | Zane | Planned | No |
| C100 | Record Match result | Gerard | Schema-supported | No |
| C200 | Update Match Result | Gerard | Schema-supported | No |
| C300 | Manage Game listing | Gerard | Schema-supported | No |
| C400 | View match result | Gerard | Schema-supported | No |
| C500 | Manage join request | Gerard | Schema-supported | No |
| D100 | Register user | Robert | Not implemented | Yes (partial — covers login foundation) |
| D200 | Manage user profile | Robert | Schema-supported | Yes (needs work) |
| D300 | Add sport | Robert | Schema-supported | Yes (needs work) |
| D400 | View user profile | Robert | Schema-supported | Yes (needs work) |
| D500 | View notifications | Robert | Schema-supported | No |
| D600 | Report User | Robert | Schema-supported | No |
| D700 | Report post | Robert | Schema-supported | No |

**Note on A500/A600:** The marked functional specification contains contradictory A500/A600 mappings between its use-case table and detailed narratives. The BOC provides clear official assignments (A500 = "Hide expired listings", A600 = "Send game reminders") and these are used here for planning purposes. If the functional specification's detailed narratives imply different behaviour, that discrepancy should be resolved by the group.

### Status Definitions

| Status | Meaning |
|--------|---------|
| Planned | No schema or code exists for this specific use case |
| Specified | Requirements and design documented but not implemented |
| Schema-supported | Database tables and/or seed data exist to support this feature, but no application code implements the use case |
| Partially implemented | Some application code exists but the use case is not complete |
| Implemented | Backend and frontend code complete for the use case |
| Unit tested | Automated tests pass for the service/business logic |
| Manually verified | End-to-end testing completed by a developer |

### Login and Session Foundation (not a BOC use case)

The following authentication foundation work is implemented but does not correspond to a single BOC use case:

- `POST /api/auth/login`: implemented
- `GET /api/auth/me`: implemented
- Session creation (Javalin/Jetty in-memory): implemented
- Login page (`index.html`): implemented
- AuthService and AuthServiceTest: implemented and passing

This foundation supports D100 (Register user) and other use cases but is not D100 itself.

### Kiro Spec Packages

| Feature | Spec Package | State |
|---------|-------------|-------|
| Authentication (Login foundation + D100) | `.kiro/specs/authentication/` | Login foundation implemented; D100 not implemented; spec aligned |
| Game Listings (A100) | `.kiro/specs/game-listings/` | Implemented; spec aligned |
| Player Profiles (D200, D300, D400) | `.kiro/specs/player-profiles/` | Schema-supported only; spec aligned |

The remaining 19 use cases do not have detailed Kiro spec packages. Create them shortly before implementation begins, not all at once.

## Implementation Phases

### Phase 1: Foundation
- [x] Project structure and configuration
- [x] Health endpoint and static frontend
- [x] Steering documents and spec structure
- [x] V1 migration (schema baseline from Spring Boot era)
- [x] V2 migration (seed sports, formats, positions)
- [x] V3 migration (align schema with confirmed rules)
- [x] Login and session foundation (AuthService, AuthController, login page, tests)

### Phase 2: Core Identity
- [ ] **D100 Register user** — Robert
  - Blocked by: group decisions on validation rules, sport selection timing
- [ ] **Logout** — Robert
- [ ] **Global auth middleware** — Robert
  - Blocked by: group decision on public routes
- [ ] **D200 Manage user profile, D300 Add sport, D400 View user profile** — Robert
  - Foundation exists (models, DAOs built for A100); profile UI and services not implemented

### Phase 3: Core Game Organisation
- [x] **A100 Create Game Listing** — Lihlumelo (Implemented, unit tested, manually verified)
- [ ] **A200 Browse Listings** — Lihlumelo
- [x] **A300 Send Join request** — Lihlumelo
- [ ] **A400 Leave Game listing** — Lihlumelo
- [ ] **C300 Manage Game listing** — Gerard
- [ ] **C500 Manage join request** — Gerard

### Phase 4: System Automation
- [ ] **A500 Hide expired listings** — Lihlumelo
- [ ] **A600 Send game reminders** — Lihlumelo
- [ ] **A700 Confirm session** — Lihlumelo
  - Automation mechanism undecided (see unresolved questions #16)

### Phase 5: Match Results
- [ ] **C100 Record Match result** — Gerard
- [ ] **C200 Update Match Result** — Gerard
- [ ] **C400 View match result** — Gerard

### Phase 6: Community Layer
- [ ] **B100 Create posts** — Zane
- [ ] **B200 Manage posts** — Zane
- [ ] **B300 Browse posts** — Zane
- [ ] **D500 View notifications** — Robert
- [ ] **D600 Report User** — Robert
- [ ] **D700 Report post** — Robert
- [ ] **B400 View Reports** — Zane
- [ ] **B500 View Leaderboards** — Zane

### Phase 7: Polish
- [ ] Private listings and invites (access rules undecided)
- [ ] UI refinements and responsive design
- [ ] Final testing and evidence capture
- [ ] Documentation: ERD, sequence diagrams, UI screenshots

## Notes

- This order ensures each feature builds on stable, tested foundations.
- Team members can work in parallel where dependencies allow.
- Specs should be reviewed by the group before implementation begins.
- The roadmap may be adjusted based on group priorities or assessment deadlines.
- Do not create detailed spec packages for all remaining use cases now. Create each one shortly before its implementation begins.

## Known Documentation Gaps

- `docs/diagrams/` contains only a placeholder README. An ERD, use-case diagram, and sequence diagrams should be generated before final submission.
- `docs/evidence/` contains only a placeholder README. Test evidence must be captured.
- The functional specification (FSSB) is not stored in this repository. The canonical use-case catalogue is maintained in the BOC.
