-- Audit trail for ServiceHour status changes (TC-027 / TC-106c).
-- Written on every status mutation (create / edit / quick approve-reject).

CREATE TABLE service_hour_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    service_hour_id BIGINT NOT NULL,
    actor_id        BIGINT,
    from_status     VARCHAR(20),
    to_status       VARCHAR(20) NOT NULL,
    changed_at      TIMESTAMP NOT NULL,
    note            VARCHAR(500),
    -- If the service hour (or its owning student) is deleted, its audit rows go with it.
    CONSTRAINT fk_audit_service_hour FOREIGN KEY (service_hour_id)
        REFERENCES service_hours (id) ON DELETE CASCADE,
    -- Keep the audit row if the acting user is ever removed; just forget who.
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id)
        REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_service_hour ON service_hour_audit_log (service_hour_id);
