# Frontend

## Overview

The GameOn-CodeSphere frontend is a **multi-page application** built with HTML, CSS, and vanilla JavaScript. Each feature area has its own folder containing dedicated HTML pages, styles, and scripts.

---

## Tech Stack

| Technology | Usage |
|------------|-------|
| HTML5 | Page structure and semantics |
| CSS3 | Styling, responsive design |
| JavaScript (ES6+) | DOM manipulation, API calls via `fetch()` |

---

## Folder Structure

```
frontend/
├── shared/          → Navbar, footer, global styles, utility JS
├── auth/            → Login, registration pages
├── listings/        → Create, browse, manage game listings
├── social/          → Posts feed, create/edit posts
├── profile/         → User profile, sport management
├── lobby/           → Game lobby, join request management
├── notifications/   → Notification centre
├── leaderboard/     → Leaderboard views
└── admin/           → Admin panel (reports)
```

---

## Ownership Map

| Folder | Owner | Use Cases |
|--------|-------|-----------|
| `auth/` | Robert Lloyd | D100 (Register), Login |
| `listings/` | Lihlumelo Mgijima | A100–A700 |
| `social/` | Zane Griesel | B100–B300 |
| `profile/` | Robert Lloyd | D200–D400 |
| `lobby/` | Gerard Mc Loughlin / Lihlumelo | A300, A700, C500 |
| `notifications/` | Robert Lloyd | D500 |
| `leaderboard/` | Zane Griesel | B500 |
| `admin/` | Zane Griesel | B400 |
| `shared/` | All (collaborative) | Navbar, footer, styles |

---

## Conventions

- Each page is a standalone `.html` file that includes shared components.
- JavaScript files use `fetch()` to communicate with the backend API.
- CSS follows a shared base (`shared/styles.css`) with page-specific overrides.
- File naming: `kebab-case` (e.g., `create-listing.html`, `browse-posts.js`).

---

## Running Locally

Open any HTML file directly in a browser, or use a local dev server:

```bash
# Using VS Code Live Server extension (recommended)
# Or Python's built-in server:
python -m http.server 5500 --directory frontend/
```

Ensure the backend is running on `http://localhost:8080` for API calls to work.
