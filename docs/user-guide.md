# LabLink User Guide

LabLink lets you reserve lab rooms and equipment at Campbellsville University. This guide walks
through everything you can do, organized by role. Each section names the menu item or button to click.

- [Getting started](#1-getting-started)
- [Students and faculty](#2-students-and-faculty)
- [Lab managers](#3-lab-managers)
- [Rules LabLink enforces](#4-rules-lablink-enforces)
- [Messages you might see](#5-messages-you-might-see)
- [Frequently asked questions](#6-frequently-asked-questions)

## Who can do what

| | Student | Faculty | Lab manager |
|---|:-:|:-:|:-:|
| Browse and search labs and equipment, see lab availability | ✔ | ✔ | ✔ |
| Request reservations | ✔ (needs approval) | ✔ (approved automatically) | ✔ (approved automatically) |
| Reschedule own **pending** reservations, cancel own reservations | ✔ | ✔ | ✔ |
| Change own password | ✔ | ✔ | ✔ |
| Add, edit and delete labs and equipment | | | ✔ |
| Approve or reject student requests | | | ✔ |
| Create faculty and lab-manager accounts, change roles | | | ✔ |

## 1. Getting started

### Create an account (students)

1. Open LabLink and click **Register** on the login page.
2. Enter your full name, your email, and a password of **at least 8 characters**, then type the password again.
3. Click **Register**. You return to the login page with *"Account created. Please log in."*

Self-registration always creates a **student** account. Faculty and lab-manager accounts are created by a
lab manager (see [3.4](#34-manage-user-accounts)); if you are faculty, ask your lab manager for an account.

### Log in and out

- Enter your email and password on the login page and click **Log in**. You land on your **Dashboard**.
- Click **Log out** at the top right when you are done, especially on a shared computer.

### The Dashboard

The Dashboard greets you by name, shows your role, and lists **Your upcoming reservations** with their status.
Lab managers also see how many requests are waiting for review, with a **Review them** link.

The menu at the top is the same on every page: **Dashboard · Labs · Equipment · My reservations · Account**,
plus **Manage requests** and **Users** for lab managers.

## 2. Students and faculty

### 2.1 Find a lab or a piece of equipment

1. Click **Labs** (or **Equipment**) in the menu.
2. Type part of a name, building or description in the search box, optionally pick a department
   (or a category and status for equipment), and click **Search**. **Clear** removes the filters.

### 2.2 Check when a lab is free

On the **Labs** page, click **Availability** next to a lab. You see one week, day by day, with every
pending or approved booking and its time. Use **← Previous week** and **Next week →** to move around.

### 2.3 Request a reservation

1. On the **Labs** or **Equipment** page, click **Reserve** next to the item you want.
2. Pick a **start** and **end** date and time, and describe the **purpose** (for example "CS450 team meeting").
3. Click **Submit request**.

What happens next depends on your role:

- **Students:** the request is **PENDING** until a lab manager approves or rejects it.
- **Faculty and lab managers:** the request is **APPROVED** immediately.

If the time overlaps an existing booking, or breaks another rule (see [section 4](#4-rules-lablink-enforces)),
the form stays open with a red message explaining why, and nothing is saved.

### 2.4 Track your reservations

Click **My reservations**. Each row shows what you booked, when, the purpose, and a status badge:

| Status | Meaning |
|---|---|
| **PENDING** | Waiting for a lab manager |
| **APPROVED** | Confirmed. The time is yours |
| **REJECTED** | A lab manager declined it |
| **CANCELLED** | You cancelled it |

### 2.5 Reschedule a pending request

On **My reservations**, click **Reschedule** next to a **PENDING** reservation, choose the new start and end,
and click **Save new time**. The same double-booking rules apply to the new time. Approved reservations
can't be rescheduled; cancel and request a new time instead.

### 2.6 Cancel a reservation

On **My reservations**, click **Cancel** next to a pending or approved reservation. The status changes to
**CANCELLED** and the time slot is freed for others.

### 2.7 Change your password

Click **Account**, enter your current password and your new password twice (at least 8 characters, different
from the current one), and click **Change password**.

## 3. Lab managers

### 3.1 Add, edit or delete a lab

- **Add:** **Labs** → **Add lab**. Fill in name, building, room number, capacity, optional department and
  description, and tick **Available for reservations** if people may book it. Click **Save**.
- **Edit:** click **Edit** next to the lab, change the fields, click **Save**. Untick
  **Available for reservations** to stop new bookings (for example during renovation) without deleting history.
- **Delete:** click **Delete**. A lab that still has equipment or reservations can't be deleted; untick
  **Available for reservations** instead.

Lab names must be unique, and so must the building + room number combination.

### 3.2 Add, edit or delete equipment

**Equipment** → **Add equipment**: name, serial number (unique), category, status, and the lab it lives in.
Set the status to **MAINTENANCE** or **RETIRED** to stop new reservations. The category decides the
maximum loan length (for example 8 hours for 3D printers, 72 hours for laptops).

### 3.3 Review requests

1. Click **Manage requests** (or **Review them** on the Dashboard).
2. **Pending requests** lists every student request waiting for you, oldest first, with who asked, what,
   when and why.
3. Click **Approve** or **Reject**. The student sees the new status on their **My reservations** page, and
   the decision and your name are kept in the reservation's history.

**All reservations** further down the page shows every reservation and who reviewed it.

### 3.4 Manage user accounts

- **Create an account:** **Users** → **Add account**. Enter name, email, a temporary password (8+ characters)
  and the role (Faculty or Lab manager). Give the user the temporary password and ask them to change it
  on their **Account** page after their first login.
- **Change a role:** on **Users**, pick the new role next to the person and save. You can't change your own
  role, so there is always at least one lab manager.

## 4. Rules LabLink enforces

1. A reservation is for exactly one lab **or** one piece of equipment.
2. It must start in the future and end after it starts.
3. **No double booking:** a request is refused if it overlaps any pending or approved reservation for the same
   lab or item.
4. Equipment can't be borrowed longer than its category allows.
5. Unavailable labs, and equipment in maintenance or retired, can't be reserved.
6. Only the person who made a reservation can reschedule or cancel it; only lab managers can approve or reject.

## 5. Messages you might see

| Message | What it means | What to do |
|---|---|---|
| *... is already booked during that time* | Someone else has that slot | Check **Availability** and pick another time |
| *Reservations must start in the future* | The start time has passed | Choose a later start |
| *End time must be after start time* | End is before or equal to start | Fix the times |
| *... items can be reserved for at most N hours at a time* | The loan is longer than the category allows | Shorten it or make two bookings |
| *... is not available (MAINTENANCE)* | The item is being repaired or retired | Choose another item |
| *Only your own pending reservations can be rescheduled* | It's approved, reviewed, or not yours | Cancel and request a new time |
| *Wrong email or password.* | Login failed | Check the email and password (passwords are case-sensitive) |
| An error page with status 403 | That page is for lab managers only | Use the menu items you can see |

## 6. Frequently asked questions

**Why is my request still pending?** Student requests wait for a lab manager. Faculty requests are approved
automatically.

**Can I see who booked a lab?** The availability page shows *when* a lab is busy, not who booked it.

**I forgot my password.** There is no password reset in version 1.0 (it is listed under *Known issues* in the
release notes). Contact your lab manager, who can ask the system administrator for help.

**Does LabLink email me when a request is approved?** Not yet. Check **My reservations** or your Dashboard.
Email notifications are planned for a future release.

**Is there an API?** Yes. Other campus systems can read lab availability as JSON without logging in.
See the [API reference](api.md).
