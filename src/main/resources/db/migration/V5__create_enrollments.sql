CREATE TABLE enrollments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id   UUID NOT NULL,
    course_id    UUID NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_enrollments_student_course
        UNIQUE (student_id, course_id),

    CONSTRAINT fk_enrollments_student
        FOREIGN KEY (student_id)
        REFERENCES students (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_enrollments_course
        FOREIGN KEY (course_id)
        REFERENCES courses (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_enrollments_status
        CHECK (status IN ('ENROLLED', 'DROPPED', 'COMPLETED'))
);

CREATE INDEX idx_enrollments_course_id
    ON enrollments (course_id);
