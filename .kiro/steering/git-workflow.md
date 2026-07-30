# GameOn - Git Workflow

## Repository

- Single repository: `GameOn-CodeSphere` on GitHub.
- `main` branch is the stable, working version.
- Never push directly to `main`.

## Branching Strategy

```
main
 └── feature/<use-case-id>-<short-name>
 └── bugfix/<short-description>
 └── docs/<short-description>
```

Examples:
- `feature/D100-user-registration`
- `feature/A100-create-game-listing`
- `bugfix/fix-login-redirect`
- `docs/update-readme`

## Workflow

1. Pull latest `main`.
2. Create a feature branch from `main`.
3. Work on the feature, committing regularly with clear messages.
4. Run `cd backend && .\mvnw.cmd clean test` locally before pushing.
5. Push and create a Pull Request to `main`.
6. At least one other team member reviews and approves.
7. Merge via squash merge or regular merge (group preference).
8. Delete the feature branch after merge.

## Commit Messages

Use clear, descriptive messages:

```
feat: implement user registration endpoint
fix: prevent duplicate usernames on registration
docs: add API documentation for auth endpoints
test: add service tests for join request validation
refactor: extract listing validation to service layer
```

Prefix with type: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`.

## Branch Protection (Recommended)

- Require PR reviews before merging to `main`.
- Require passing tests (CI) before merge (if GitHub Actions are set up).
- No force-pushes to `main`.

## Conflict Resolution

- Rebase your feature branch on `main` before creating a PR.
- Resolve conflicts locally before pushing.
- If unsure, ask the team member whose code conflicts.

## Use Case Allocation

Each team member works on their allocated use cases:
- **Lihlumelo:** A100-A700 (Listings, join requests, reminders, confirmation)
- **Zane:** B100-B500 (Posts, moderation, leaderboards)
- **Gerard:** C100-C500 (Match results, listing management, join request approval)
- **Robert:** D100-D700 (Registration, profiles, sports, notifications, reporting)

Shared infrastructure (config, base classes, migrations) should be coordinated to avoid conflicts.
