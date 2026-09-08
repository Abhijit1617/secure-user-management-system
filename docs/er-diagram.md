# Entity Relationship Diagram

This is the complete schema as of the final production release. For the
original phase 1 core (users/roles/permissions/departments/employees/
projects/tasks) with full normalization notes, see
[`database-design.md`](database-design.md); this document adds
everything built in the phases since — organizations, comments,
attachments, notifications and audit.

```mermaid
erDiagram
    ORGANIZATION ||--o{ DEPARTMENT : contains
    ORGANIZATION ||--o{ EMPLOYEE : employs
    ORGANIZATION }o--|| USER : "owned by"

    USER ||--o{ USER_ROLE : has
    ROLE ||--o{ USER_ROLE : has
    ROLE ||--o{ ROLE_PERMISSION : has
    PERMISSION ||--o{ ROLE_PERMISSION : has
    USER ||--o| EMPLOYEE : "is linked to"
    USER ||--o{ REFRESH_TOKEN : owns
    USER ||--o{ NOTIFICATION : receives
    USER ||--o| NOTIFICATION_PREFERENCE : configures
    USER ||--o{ AUDIT_LOG : performs
    USER ||--o{ COMMENT : authors
    USER ||--o{ ATTACHMENT : uploads

    DEPARTMENT ||--o{ DEPARTMENT : "parent of"
    DEPARTMENT ||--o{ EMPLOYEE : employs
    EMPLOYEE ||--o| DEPARTMENT : heads
    EMPLOYEE ||--o{ EMPLOYEE : "manages (self)"

    DEPARTMENT ||--o{ PROJECT : owns
    EMPLOYEE ||--o{ PROJECT : manages
    PROJECT ||--o{ PROJECT_MEMBER : has
    EMPLOYEE ||--o{ PROJECT_MEMBER : "assigned to"

    PROJECT ||--o{ TASK : contains
    EMPLOYEE ||--o{ TASK : "assigned"
    EMPLOYEE ||--o{ TASK : "reported"
    TASK ||--o{ TASK_HISTORY : "changes tracked in"

    COMMENT ||--o{ COMMENT : "replies to"
    COMMENT ||--o{ COMMENT_REVISION : "edited, preserved in"
    COMMENT }o--o{ USER : mentions

    ORGANIZATION {
        uuid id PK
        varchar slug UK
        uuid owner_id FK
        varchar status
    }
    DEPARTMENT {
        uuid id PK
        uuid organization_id FK
        varchar code
        uuid parent_department_id FK
        uuid head_employee_id FK
    }
    EMPLOYEE {
        uuid id PK
        uuid organization_id FK
        varchar employee_code UK
        uuid user_id FK
        uuid department_id FK
        uuid reporting_manager_id FK
    }
    PROJECT {
        uuid id PK
        varchar code UK
        uuid department_id FK
        uuid project_manager_id FK
    }
    TASK {
        uuid id PK
        uuid project_id FK
        uuid assignee_id FK
        uuid reporter_id FK
    }
    TASK_HISTORY {
        uuid id PK
        uuid task_id FK
        varchar field_changed
        uuid changed_by_id FK
    }
    COMMENT {
        uuid id PK
        varchar owner_type
        uuid owner_id
        uuid author_id FK
        uuid parent_comment_id FK
        boolean deleted
    }
    COMMENT_REVISION {
        uuid id PK
        uuid comment_id FK
        text previous_content
        uuid edited_by_id FK
    }
    ATTACHMENT {
        uuid id PK
        varchar owner_type
        uuid owner_id
        varchar storage_path
        uuid uploaded_by_id FK
    }
    NOTIFICATION {
        uuid id PK
        uuid recipient_id FK
        varchar type
        boolean is_read
    }
    NOTIFICATION_PREFERENCE {
        uuid id PK
        uuid user_id FK
        boolean email_enabled
        boolean in_app_enabled
    }
    AUDIT_LOG {
        uuid id PK
        uuid user_id FK
        varchar action
        varchar entity_name
        uuid entity_id
    }
```

## Notes on the polymorphic tables

`COMMENT.owner_type`/`owner_id` and `ATTACHMENT.owner_type`/`owner_id` are
deliberately **not** foreign keys — a comment or attachment can belong to
a `TASK`, `PROJECT` or (for attachments) `EMPLOYEE` record, and Postgres
doesn't support a foreign key that conditionally points at one of several
tables. Referential integrity for these two columns is enforced in the
service layer (`CommentServiceImpl`, `AttachmentServiceImpl`) rather than
the database — the same trade-off already documented in
[`database-design.md`](database-design.md) for `notifications.reference_id`,
applied consistently across every polymorphic reference in the schema.
