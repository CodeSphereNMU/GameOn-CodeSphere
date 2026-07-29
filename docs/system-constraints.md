# System Constraints

## Purpose

This document outlines the technical and organisational constraints that shape the design and implementation of GameOn-CodeSphere.

---

## Technical Constraints

| Constraint | Description |
|------------|-------------|
| Frontend Technology | HTML, CSS, and vanilla JavaScript (multi-page application). No frontend frameworks (React, Angular, Vue). |
| Backend Technology | Java (Spring Boot or generic Java servlets). |
| Database | Microsoft SQL Server (database name: `GameOnDb`). |
| Authentication | Session-based authentication managed server-side. |
| Hosting | Local development only — no hosted server infrastructure available. |
| Database Hosting | Offline/local SQL Server instance per developer (no remote or shared DB server). |
| Browser Support | Latest versions of Chrome, Firefox, and Edge. |
| Version Control | Git with GitHub; feature-branch workflow. |

---

## Organisational Constraints

| Constraint | Description |
|------------|-------------|
| Team Size | 4 developers with assigned feature areas. |
| Timeline | University semester project with defined submission deadline. |
| Communication | Team coordination via agreed channels (e.g., WhatsApp, Discord, GitHub Issues). |
| Code Ownership | Each member owns their assigned use cases but may collaborate on shared components. |

---

## Design Constraints

| Constraint | Description |
|------------|-------------|
| Multi-Page Architecture | Each feature area maps to dedicated HTML pages (no SPA routing). |
| Shared Components | Common elements (navbar, footer, styles) live in `frontend/shared/`. |
| RESTful API | Backend exposes RESTful endpoints consumed by frontend via `fetch()` or `XMLHttpRequest`. |
| Database Normalisation | Database schema follows at least 3NF (Third Normal Form). |
| No ORM Requirement | Data access may use JDBC directly or Spring Data JPA depending on framework choice. |

---

## Security Constraints

| Constraint | Description |
|------------|-------------|
| Password Storage | Bcrypt hashing (or equivalent) — never plain text. |
| SQL Injection | All queries use parameterised statements. |
| XSS Prevention | User-generated content is sanitised before rendering. |
| Session Management | Sessions expire after inactivity timeout; tokens are HTTP-only. |
| Input Validation | All inputs validated server-side regardless of client-side validation. |

---

## Performance Constraints

| Constraint | Description |
|------------|-------------|
| Page Load | Target < 3 seconds on standard broadband. |
| Database Queries | Indexed columns on frequently queried fields (e.g., user ID, listing date). |
| Image Uploads | Maximum file size of 5 MB per image. |
