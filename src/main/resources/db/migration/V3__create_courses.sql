CREATE TABLE courses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(20) NOT NULL,
    name        VARCHAR(150) NOT NULL,
    credits     INTEGER NOT NULL,
    program_id  UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_courses_code UNIQUE (code),

    CONSTRAINT fk_courses_program
        FOREIGN KEY (program_id)
        REFERENCES programs (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_courses_credits
        CHECK (credits BETWEEN 1 AND 12)
);

CREATE INDEX idx_courses_program_id
    ON courses (program_id);
