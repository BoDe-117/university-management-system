CREATE TABLE students (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  VARCHAR(20) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    program_id  UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_students_student_id UNIQUE (student_id),
    CONSTRAINT uk_students_email UNIQUE (email),

    CONSTRAINT fk_students_program
        FOREIGN KEY (program_id)
        REFERENCES programs (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_students_program_id
    ON students (program_id);
