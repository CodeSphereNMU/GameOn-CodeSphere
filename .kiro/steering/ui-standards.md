# GameOn - UI Standards

## Technology

- Plain HTML5, CSS3, and vanilla JavaScript.
- No frontend frameworks (React, Vue, Angular) unless the group changes direction.
- Use browser `fetch()` for API calls via the shared `api.js` helper.

## File Organisation

```
frontend/
├── index.html              # Landing/status page
├── css/
│   └── main.css            # Shared styles and variables
├── js/
│   ├── api.js              # Shared fetch helper (Api.get, Api.post, etc.)
│   └── <page>.js           # Page-specific scripts
└── pages/
    ├── login.html
    ├── register.html
    ├── dashboard.html
    └── ...
```

At build time, Maven copies `frontend/` into the backend classpath so Javalin serves them as static files.

## CSS Guidelines

- Use CSS custom properties (variables) for colours, spacing, and radii.
- Follow the existing variable scheme in `main.css` (--color-primary, --color-bg, etc.).
- Use kebab-case for class names: `.game-listing-card`.
- Mobile-first responsive design (but desktop is primary for this project).
- Avoid inline styles.

## HTML Guidelines

- Semantic HTML: use `<header>`, `<nav>`, `<main>`, `<section>`, `<footer>`.
- All pages must have `lang="en"` on `<html>`.
- All images must have `alt` attributes.
- Form inputs must have associated `<label>` elements.
- Use `<button>` for actions, `<a>` for navigation.

## JavaScript Guidelines

- Use `const` and `let` (never `var`).
- Use the shared `Api` object from `api.js` for all server calls.
- Handle loading states and errors visually (show the user what's happening).
- Page scripts run after `DOMContentLoaded`.
- No global variable pollution; wrap in IIFE or use modules if needed.

## Navigation & Layout (Planned)

Based on FSSB designs, the app will have:
- A bottom navigation bar (mobile-style) or sidebar with main sections.
- Main sections: Home/Dashboard, Listings, Social, Notifications, Profile.
- Consistent header with app branding.

## Accessibility

- Sufficient colour contrast (WCAG AA minimum).
- Keyboard navigable forms and buttons.
- Visible focus indicators.
- ARIA labels where semantic HTML is insufficient.

## Design Language (from FSSB)

- Clean, card-based UI for listings and posts.
- Team rosters displayed in a clear two-column (Team A / Team B) layout.
- Status indicators using colour (green = healthy/active, yellow = warning, red = error).
- Rounded corners, subtle shadows for card depth.
