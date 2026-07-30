# GameOn - Feature Roadmap

## Implementation Order

Features should be implemented in this order due to dependencies:

### Phase 1: Foundation (Current)
- [x] Project structure and configuration
- [x] Health endpoint and static frontend
- [x] Steering documents and spec structure

### Phase 2: Core Identity
- [ ] **Authentication** (D100 + Login/Logout) — Robert
  - User registration, login, logout, session management
  - Dependency: None (first feature)

- [ ] **Player Profiles & Sports** (D200, D300) — Robert
  - Profile viewing, sport management, follow/unfollow
  - Dependency: Authentication

### Phase 3: Core Game Organisation
- [ ] **Game Listings** (A100, A200, A300, A400, C300, C500) — Lihlumelo + Gerard
  - Create, browse, join, manage listings
  - Dependency: Player Profiles (need sports on profile)

- [ ] **Match Results** (C100, C200, C400) — Gerard
  - Record and update match results
  - Dependency: Game Listings (need completed listings)

### Phase 4: System Automation
- [ ] **Listing Lifecycle** (A500, A600, A700) — Lihlumelo
  - Expiry, reminders, session confirmation
  - Dependency: Game Listings

### Phase 5: Community Layer
- [ ] **Posts & Social Feed** (B100, B200, B300) — Zane
  - Create, manage, browse posts
  - Dependency: Authentication

- [ ] **Notifications** (D500) — Robert
  - Notification system for all events
  - Dependency: Authentication (used by many features)

- [ ] **Reporting & Moderation** (D600, D700, B400) — Robert + Zane
  - Report users/posts, moderator dashboard
  - Dependency: Posts, Profiles

- [ ] **Leaderboards** (B500) — Zane
  - Win rate leaderboards by sport
  - Dependency: Match Results

### Phase 6: Polish
- [ ] View User Profile / Follow-Unfollow (D400) — Robert
- [ ] Private listings and invites
- [ ] UI refinements and responsive design
- [ ] Final testing and evidence capture

## Notes

- This order ensures each feature builds on stable, tested foundations.
- Team members can work in parallel where dependencies allow (e.g., Zane can start Posts once Auth is done).
- Specs should be reviewed by the group before implementation begins.
- The roadmap may be adjusted based on group priorities or assessment deadlines.
