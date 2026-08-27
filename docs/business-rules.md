# Business Rules

## Purpose

This document defines the business rules governing GameOn-CodeSphere operations. These rules ensure data consistency, enforce domain logic, and guide system behaviour.

---

## User Management Rules

| ID | Rule |
|----|------|
| BR-U01 | A user must register with a unique email address and username. |
| BR-U02 | A user must verify their email before accessing platform features. |
| BR-U03 | A user can follow or unfollow other users at any time. |
| BR-U04 | A user can add one or more sports to their profile. |
| BR-U05 | A reported user account is reviewed; repeated violations may lead to suspension. |

---

## Listing Rules

| ID | Rule |
|----|------|
| BR-L01 | Only registered users can create a game listing. |
| BR-L02 | A game listing must specify a sport, format, date, time, and location. |
| BR-L03 | The listing creator is automatically assigned as the host. |
| BR-L04 | A listing has a maximum player capacity defined by its sport format. |
| BR-L05 | Expired listings (past date/time) are hidden from browse results. |
| BR-L06 | The host can send reminders to confirmed joiners up to 24 hours before the session. |
| BR-L07 | A user may leave a listing at any time before the session starts. |
| BR-L08 | A session is confirmed when the host explicitly confirms it or the minimum players are reached. |
| BR-L09 | A listing cannot be cancelled within 1 hour of the scheduled match time. |

---

## Join Request Rules

| ID | Rule |
|----|------|
| BR-J01 | A user can send a join request to any open listing they are not already part of. |
| BR-J02 | The host must approve or reject join requests. |
| BR-J03 | A user cannot join a listing that is already at capacity. |
| BR-J04 | A user cannot join the same listing more than once. |

---

## Match Result Rules

| ID | Rule |
|----|------|
| BR-M01 | Only the host can record a match result for a confirmed session. |
| BR-M02 | A match result can only be recorded after the session date/time has passed. |
| BR-M03 | The host can update a match result within 48 hours of recording. |
| BR-M04 | Match results contribute to user leaderboard rankings. |

---

## Social / Posts Rules

| ID | Rule |
|----|------|
| BR-S01 | Any registered user can create a post. |
| BR-S02 | A post can contain text and/or an image. |
| BR-S03 | The post author can edit or delete their own posts. |
| BR-S04 | Any user can like or comment on a post. |
| BR-S05 | A user can report a post for inappropriate content. |
| BR-S06 | Reported posts are flagged for admin review. |

---

## Notification Rules

| ID | Rule |
|----|------|
| BR-N01 | Users receive notifications for join request updates (approved/rejected). |
| BR-N02 | Users receive notifications for game reminders. |
| BR-N03 | Users receive notifications when someone follows them. |
| BR-N04 | Users receive notifications for likes and comments on their posts. |

---

## Leaderboard Rules

| ID | Rule |
|----|------|
| BR-LB01 | Leaderboards are calculated per sport. |
| BR-LB02 | Rankings are based on recorded match results (wins, draws, losses). |
| BR-LB03 | Leaderboard data refreshes after each new match result is recorded. |
