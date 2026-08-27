# Shared Components

## Overview

This folder contains reusable components, global styles, and utility scripts shared across all frontend pages.

---

## Planned Files

| File | Purpose |
|------|---------|
| `styles.css` | Global CSS (colours, typography, layout, resets) |
| `navbar.html` | Shared navigation bar (included via JS inject or iframe) |
| `footer.html` | Shared footer |
| `navbar.js` | Navbar logic (active link highlighting, auth state) |
| `api.js` | Shared `fetch()` wrapper for API calls |
| `auth.js` | Session/auth helpers (check login, redirect) |
| `utils.js` | Common utilities (date formatting, validation) |

---

## Ownership

**Collaborative** — all team members contribute to and use shared components.

---

## Usage

Include shared styles in every HTML page:

```html
<link rel="stylesheet" href="../shared/styles.css">
```

Include shared scripts:

```html
<script src="../shared/api.js"></script>
<script src="../shared/auth.js"></script>
```
