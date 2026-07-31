# GameOn - Security Guidelines

## Credentials & Secrets

- Never commit `.env`, passwords, connection strings, or API keys to Git.
- Use `.env.example` with placeholder names only.
- Each developer maintains their own `.env` locally.

## Authentication (Planned)

- Server-side sessions (Javalin session handling or a simple session token table).
- Passwords stored as plain text (university project requirement; hashing is prohibited).
- Session tokens should be random, unguessable, and expire.
- No JWT unless the group explicitly decides otherwise.

**Assumption (requires group confirmation):** Session-based auth using a `sessions` table with a random token stored in a cookie. Alternative: Javalin's built-in session support.

## SQL Injection Prevention

- All database queries MUST use parameterised PreparedStatements.
- Never construct SQL by string concatenation with user input.
- Never use `Statement.execute(userString)`.

## Input Validation

- Validate ALL input on the server side, regardless of frontend checks.
- Reject requests with missing or invalid fields (return 400 with a message).
- Validate types, lengths, ranges, and formats.
- Sanitise text that will be displayed to prevent XSS (escape HTML entities).

## Error Handling

- Never expose stack traces, SQL errors, or internal paths to the client.
- Use the central exception handler in `JavalinConfig`.
- Log detailed errors server-side; return generic messages to the user.
- The `ApiException` class provides controlled error responses.

## Access Control

- Protected endpoints must check authentication first.
- Resource-level authorisation: verify the requesting user owns/has access to the resource.
- Example: only the listing creator can accept join requests for their listing.
- Moderator-only actions must check user type before proceeding.

## Frontend Security

- Frontend checks are for UX only. They do not replace server enforcement.
- Do not store sensitive data in localStorage or expose it in JavaScript.
- Use `HttpOnly` and `Secure` flags on session cookies (when implementing auth).

## CORS

- In development, CORS is limited to `localhost:7070`.
- Tighten further if deployment happens.

## Reporting & Moderation

- Reports should not expose reporter identity to the reported user.
- Only moderators can view reports and take action.
