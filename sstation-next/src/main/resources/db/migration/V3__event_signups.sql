-- Event sign-up flow (TC-026 / TC-108f). A student signs up for an event; the office marks the
-- outcome and can convert attended sign-ups into service-hour records.

CREATE TABLE event_signups (
    id          BIGSERIAL PRIMARY KEY,
    student_id  BIGINT NOT NULL,
    event_id    BIGINT NOT NULL,
    signup_time TIMESTAMP NOT NULL,
    status      VARCHAR(20) NOT NULL,
    converted   BOOLEAN NOT NULL DEFAULT FALSE,
    -- Sign-ups are owned by their student and event; removing either removes the sign-up.
    CONSTRAINT fk_signup_student FOREIGN KEY (student_id)
        REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_signup_event FOREIGN KEY (event_id)
        REFERENCES events (id) ON DELETE CASCADE,
    -- A student can sign up for a given event only once.
    CONSTRAINT uq_signup_student_event UNIQUE (student_id, event_id)
);

CREATE INDEX idx_signup_event ON event_signups (event_id);
CREATE INDEX idx_signup_student ON event_signups (student_id);
