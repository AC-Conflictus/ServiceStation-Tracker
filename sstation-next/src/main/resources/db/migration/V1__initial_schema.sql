-- Initial schema for the Service Station Hours app (Lane 7 rewrite, TC-103).
-- Matches the JPA entities under edu.austincollege.sstation.domain. Written in the
-- common subset of PostgreSQL and H2's PostgreSQL-compatibility mode so the same
-- migration runs in prod (Postgres) and in local dev / tests (H2).
--
-- Hibernate runs with ddl-auto=validate, so this file is the single source of truth
-- for the schema; the entities must stay in sync with it.

-- ---------------------------------------------------------------------------
-- Security: roles, users (with the real FK to students), and the join table.
-- ---------------------------------------------------------------------------

CREATE TABLE roles (
    id        BIGSERIAL PRIMARY KEY,
    authority VARCHAR(255) NOT NULL,
    CONSTRAINT uq_roles_authority UNIQUE (authority)
);

CREATE TABLE students (
    id             BIGSERIAL PRIMARY KEY,
    firstname      VARCHAR(255) NOT NULL,
    lastname       VARCHAR(255) NOT NULL,
    acid           VARCHAR(255) NOT NULL,
    ac_email       VARCHAR(255) NOT NULL,
    ac_box         VARCHAR(255),
    phone          VARCHAR(255),
    ac_year        INTEGER,
    status         CHAR(1) NOT NULL,
    classification VARCHAR(20),
    is_moderator   BOOLEAN,
    CONSTRAINT uq_students_acid UNIQUE (acid)
);

CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    username         VARCHAR(255) NOT NULL,
    password         VARCHAR(255) NOT NULL,
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    account_expired  BOOLEAN NOT NULL DEFAULT FALSE,
    account_locked   BOOLEAN NOT NULL DEFAULT FALSE,
    password_expired BOOLEAN NOT NULL DEFAULT FALSE,
    -- The real AcUser -> AcStudent FK the Grails app never had (TC-009). Nullable:
    -- admin/moderator accounts have no student profile.
    student_id       BIGINT,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT fk_users_student FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

-- ---------------------------------------------------------------------------
-- Reference data: events, campus orgs, community agencies, contacts.
-- ---------------------------------------------------------------------------

CREATE TABLE events (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(10000) NOT NULL,
    contact       VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL
);

CREATE TABLE campus_orgs (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(10000) NOT NULL,
    contact       VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL
);

CREATE TABLE community_agencies (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    address       VARCHAR(255) NOT NULL,
    description   VARCHAR(10000) NOT NULL,
    contact       VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL
);

CREATE TABLE contacts (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(255),
    phone VARCHAR(255),
    email VARCHAR(255)
);

-- ---------------------------------------------------------------------------
-- The central record: service hours.
-- ---------------------------------------------------------------------------

CREATE TABLE service_hours (
    id                  BIGSERIAL PRIMARY KEY,
    description         VARCHAR(10000) NOT NULL,
    event_contact_name  VARCHAR(255),
    event_contact_phone VARCHAR(255),
    event_contact_email VARCHAR(255),
    status              VARCHAR(20) NOT NULL,
    duration            DOUBLE PRECISION NOT NULL,
    start_time          TIMESTAMP NOT NULL,
    last_modified       TIMESTAMP NOT NULL,
    student_id          BIGINT NOT NULL,
    -- Nullable per the TC-103 decision (matches Grails + the other_* free-text fields).
    campus_org_id       BIGINT,
    comm_ag_id          BIGINT,
    event_id            BIGINT,
    other_cam_org       VARCHAR(255),
    other_comm_ag       VARCHAR(255),
    CONSTRAINT fk_service_hours_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_service_hours_campus_org FOREIGN KEY (campus_org_id) REFERENCES campus_orgs (id),
    CONSTRAINT fk_service_hours_comm_ag FOREIGN KEY (comm_ag_id) REFERENCES community_agencies (id),
    CONSTRAINT fk_service_hours_event FOREIGN KEY (event_id) REFERENCES events (id)
);

-- Indexes on the FKs most reports filter/group by.
CREATE INDEX idx_service_hours_student ON service_hours (student_id);
CREATE INDEX idx_service_hours_status ON service_hours (status);
CREATE INDEX idx_service_hours_start_time ON service_hours (start_time);
