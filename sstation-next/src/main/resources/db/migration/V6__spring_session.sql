-- HTTP sessions out of instance memory (TC-122a).
--
-- Vercel's Fluid compute may serve any request from any instance, so in-memory Tomcat sessions
-- would return a signed-in user to the login page at random. spring-session-jdbc keeps sessions
-- in the same Postgres the app already runs on; these are the tables it reads and writes.
--
-- Written in the Postgres/H2-PG common subset (the V1 convention): dev boots H2 in PostgreSQL
-- compatibility mode and runs the same migration prod does. `spring.session.jdbc.initialize-
-- schema` is `never` so Spring Session never tries to create these itself — Flyway is the schema
-- source of truth here, and Hibernate's ddl-auto=validate ignores tables it does not map.
--
-- Shape is Spring Session's own schema-postgresql. JdbcIndexedSessionRepository queries these
-- by name (PRINCIPAL_NAME feeds its findByPrincipalName index), so do not rename columns.

CREATE TABLE SPRING_SESSION (
    PRIMARY_ID            CHAR(36)     NOT NULL,
    SESSION_ID            CHAR(36)     NOT NULL,
    CREATION_TIME         BIGINT       NOT NULL,
    LAST_ACCESS_TIME      BIGINT       NOT NULL,
    MAX_INACTIVE_INTERVAL INTEGER      NOT NULL,
    EXPIRY_TIME           BIGINT       NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36)      NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200)  NOT NULL,
    ATTRIBUTE_BYTES    BYTEA         NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK
        FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);

CREATE INDEX SPRING_SESSION_ATTRIBUTES_IX1 ON SPRING_SESSION_ATTRIBUTES (SESSION_PRIMARY_ID);
