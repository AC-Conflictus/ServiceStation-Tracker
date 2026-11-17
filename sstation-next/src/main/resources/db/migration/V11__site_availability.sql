CREATE TABLE event_site_availability (
    site_id BIGINT PRIMARY KEY REFERENCES event_sites(id) ON DELETE CASCADE,
    sync_key VARCHAR(100) NOT NULL UNIQUE,
    capacity INTEGER,
    remaining INTEGER,
    accepting_responses BOOLEAN,
    observed_at TIMESTAMP WITH TIME ZONE,
    CHECK (capacity IS NULL OR capacity >= 0),
    CHECK (remaining IS NULL OR (remaining >= 0 AND remaining <= capacity))
);
