# ADR-003: Documentation Alignment (August 2026)

## Status
Accepted

## Date
2026-08-06

## Context
A documentation-alignment pass was performed at commit `f224c05` (descendant of checkpoint `1657f27`) to correct accumulated drift between the active specifications and the actual implementation. The project had evolved through A100 implementation while earlier spec drafts remained unchanged.

## Superseded Material Removed from Active Specifications

The following material was removed from active implementation documents because it contradicted the confirmed implementation or schema. Git retains the historical versions.

### Authentication Spec (previously in tasks.md)
- **Task "Create V2__create_user_table.sql"** — Removed. The `users` table was established in V1. No migration is needed.
- **INT identifiers in design.md** — Replaced with BIGINT to match actual V1 schema.
- **`[User]` bracketed table name in design.md** — Replaced with `dbo.users` (actual name).
- **Duplicate "Task 4" numbering** — Fixed.

### Player Profiles Spec (previously in all three files)
- **Sports list: Soccer, Cricket, Hockey, Badminton, Volleyball** — Replaced with the confirmed 5: Padel, Tennis, Basketball, Rugby, Football.
- **`V3__create_sport_tables.sql` migration task** — Removed. These tables exist in V1.
- **`V4__seed_sports.sql` migration task** — Removed. Seed data was applied in V2.
- **`Sport.noPlayers` column** — Removed. The `sport` table has only `sport_id` and `sport_name`. Capacity belongs to `sport_format.no_players`.
- **INT identifiers throughout** — Replaced with BIGINT.
- **"User cannot remove their last sport" stated as confirmed** — Changed to unresolved. No authoritative decision exists.
- **"All profiles are public" assumption** — Changed to unresolved.

### Game Listings Spec (previously in requirements.md)
- **"Minimum Lead Time — Current Implemented Placeholder"** — Replaced with confirmed 3-hour rule. The 3-hour creation lead time is a confirmed business rule, not a placeholder.

### Roadmap (previously in roadmap.md)
- **All Phase 2+ items shown as unchecked** — Corrected. Login is partially implemented, A100 is fully implemented and verified.
- **Authentication described as fully unimplemented** — Corrected to show partial implementation.

## Decisions Recorded

This alignment pass also recorded the following confirmed rules that were previously undocumented or scattered:

1. **3-hour creation lead time** — confirmed, not a placeholder.
2. **2-hour lock-in** — separate from creation lead time; triggers CONFIRMED or CANCELLED_INSUFFICIENT_PLAYERS.
3. **Invitations are courtesy only** — do not reserve capacity, do not auto-accept.
4. **Invited users must still submit join requests** — creator approval required.
5. **Invitation priority** — invited user's request goes to front of queue with "Invited" tag; does not bypass rules.
6. **Users may be invited to sports not on their profile** — must add sport before requesting to join.
7. **Invitations expire at lock-in.**
8. **Rejected users may resubmit** — another join request is allowed.
9. **No attendance tracking** — after lock-in, no further stage exists.
10. **Match results use whole-number team scores; draws allowed.**
11. **Reported users notified only on moderation action.**
12. **Social-feed community filtering allows multiple selections.**
13. **Browsing limited to sports on user's profile.**
14. **A100 transaction must be atomic** — listing, creator joiner, invitations, notifications all or nothing.

## Consequences
- Active specs now reflect the real implementation and confirmed rules.
- Obsolete migration tasks cannot accidentally be executed via Kiro's "Start task" feature.
- Remaining tasks are genuine and actionable.
- Unresolved questions remain clearly marked and are not silently presented as decided.
- The A500/A600 naming was resolved using the BOC (A500 = "Hide expired listings", A600 = "Send game reminders") while noting the FSSB narrative contradiction.
- The use-case catalogue now uses official BOC names and IDs.
- D100 (Register user) is documented as not implemented, separate from the login foundation.
- Sport-removal blocking was reclassified from confirmed to unresolved (no authoritative source confirmed it as a business rule).
