# DazBones - Player CRUD Specification

## 1. Overview

This document defines the Player management feature (CRUD).

- List players
- Add player
- Edit player
- Delete player

---

## 2. Entity: Player

| Field        | Type        | Required | Description |
|--------------|------------|----------|-------------|
| id           | Integer    | Yes (PK) | Auto increment ID |
| name         | String     | Yes      | Player name |
| position     | String     | No       | Player position |
| number       | Integer    | No       | Jersey number (0–99) |
| photoPath    | String     | No       | Image file path |
| comment      | String     | No       | Free comment |
| createdAt    | DateTime   | Auto     | Created timestamp |
| updatedAt    | DateTime   | Auto     | Updated timestamp |

---

## 3. URL Mapping

### 3.1 List
GET `/player`

### 3.2 Add
GET `/player/add`  
POST `/player/add`

### 3.3 Edit
GET `/player/edit/{id}`  
POST `/player/edit`

### 3.4 Delete
POST `/player/delete/{id}`

---

## 4. Validation Rules

| Field    | Rule |
|----------|------|
| name     | Required, max 100 chars |
| position | Max 50 chars |
| number   | 0–99 |
| comment  | Max 1000 chars |

---

## 5. Authorization

| Role    | Permission |
|---------|------------|
| admin   | Full access |
| editor  | CRUD allowed |
| viewer  | View only |

---

## 6. Flow

### Add Flow
1. Input form
2. Validate
3. Save
4. Redirect to list

### Edit Flow
1. Load by ID
2. Display form
3. Validate
4. Update
5. Redirect to list

### Delete Flow
1. Confirm
2. Delete by ID
3. Redirect to list

---

## 7. Future Improvements

- Image upload
- Logical delete option
- Pagination
- Search function
- API versioning