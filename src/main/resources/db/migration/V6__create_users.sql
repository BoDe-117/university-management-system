CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL,
    student_id    UUID,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_student UNIQUE (student_id),

    CONSTRAINT fk_users_student
        FOREIGN KEY (student_id)
        REFERENCES students (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_users_role
        CHECK (role IN ('ADMIN', 'STAFF', 'STUDENT')),

    CONSTRAINT ck_users_student_role_match
        CHECK (
            (role = 'STUDENT' AND student_id IS NOT NULL)
            OR
            (role IN ('ADMIN', 'STAFF') AND student_id IS NULL)
        )
);
