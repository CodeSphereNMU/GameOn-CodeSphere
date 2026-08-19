# User Management Specification

## Owner

**Robert Lloyd**

## Overview

The User Management module handles registration, profile management, social interactions (follow/unfollow), notifications, and reporting functionality.

---

## Use Cases

### D100 — Register User

| Field | Detail |
|-------|--------|
| **Actor** | Unregistered Visitor |
| **Description** | A new user registers an account by providing personal details and credentials. |
| **Preconditions** | User does not have an existing account. |
| **Postconditions** | A new User record is created; user can log in. |
| **Triggers** | User clicks "Register" and submits the registration form. |

**Basic Flow:**
1. User navigates to the Registration page.
2. User enters first name, last name, email, username, and password.
3. User submits the form.
4. System validates uniqueness of email and username.
5. System hashes password and creates User record.
6. System redirects to login page with success message.

**Alternative Flows:**
- 4a. Email already exists → system displays "Email taken" error.
- 4b. Username already exists → system displays "Username taken" error.
- 4c. Password does not meet requirements → system displays validation error.

**Validation Rules:**
- Email: valid format, unique
- Username: 3–50 characters, alphanumeric + underscores, unique
- Password: minimum 8 characters, at least 1 uppercase, 1 number

---

### D200 — Manage User Profile

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User views and updates their own profile information (name, bio, profile image). |
| **Preconditions** | User is logged in. |
| **Postconditions** | User profile is updated. |
| **Triggers** | User navigates to "My Profile" and clicks "Edit." |

**Basic Flow:**
1. User navigates to their profile page.
2. User clicks "Edit Profile."
3. System displays editable form with current data.
4. User updates fields (first name, last name, bio, profile image).
5. User submits changes.
6. System validates and updates the User record.

---

### D300 — Add Sport

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User adds a sport to their profile, optionally specifying skill level and preferred position. |
| **Preconditions** | User is logged in; sport is not already on user's profile. |
| **Postconditions** | A UserSportProfile record is created. |
| **Triggers** | User clicks "Add Sport" on their profile. |

**Basic Flow:**
1. User navigates to their profile sports section.
2. User clicks "Add Sport."
3. System displays available sports not yet on profile.
4. User selects a sport, skill level, and preferred position.
5. User submits.
6. System creates UserSportProfile record.

**Alternative Flows:**
- 3a. User already has all available sports → system displays message.

---

### D400 — View User Profile (Follow/Unfollow)

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User views another user's profile and can follow or unfollow them. |
| **Preconditions** | User is logged in; target user exists. |
| **Postconditions** | Follow/Unfollow record is created or removed; target user is notified on follow. |
| **Triggers** | User clicks on another user's name/avatar. |

**Basic Flow:**
1. User clicks on another user's profile link.
2. System displays the target user's public profile (name, bio, sports, stats).
3. User clicks "Follow" (or "Unfollow" if already following).
4. System creates or removes Follow record.
5. System notifies target user on new follow.

---

### D500 — View Notifications

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User views their notifications (join request updates, reminders, follows, likes, comments). |
| **Preconditions** | User is logged in. |
| **Postconditions** | Notifications are displayed; user can mark as read. |
| **Triggers** | User clicks notification bell or navigates to Notifications page. |

**Basic Flow:**
1. User clicks the notification icon.
2. System displays list of notifications (newest first).
3. Unread notifications are highlighted.
4. User clicks a notification to view details or navigate to related item.
5. System marks notification as read.

**Additional Actions:**
- "Mark All as Read" button to clear all unread.

---

### D600 — Report User

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User reports another user for inappropriate behaviour. |
| **Preconditions** | User is logged in; cannot report themselves. |
| **Postconditions** | A Report record is created with reported_user_id; admin is notified. |
| **Triggers** | User clicks "Report" on another user's profile. |

**Basic Flow:**
1. User views another user's profile.
2. User clicks "Report User."
3. System displays report form with reason field.
4. User enters reason and submits.
5. System creates Report record.
6. Report appears in admin queue.

---

### D700 — Report Post

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User reports a post for inappropriate content. |
| **Preconditions** | User is logged in; cannot report own post. |
| **Postconditions** | A Report record is created with reported_post_id; admin is notified. |
| **Triggers** | User clicks "Report" on a post. |

**Basic Flow:**
1. User views a post on the feed.
2. User clicks "Report Post."
3. System displays report form with reason field.
4. User enters reason and submits.
5. System creates Report record.
6. Report appears in admin queue.
