-- Day-one admin bootstrap (TC-121).
--
-- An account created by the prod bootstrap runner starts with a password that came from an
-- environment variable, which stays readable in the deployment's own configuration (compose file,
-- systemd unit, task definition). That password is therefore single-use by design: the flag below
-- forces the holder onto /change-password before they can reach anything else.
--
-- Deliberately NOT reusing the existing password_expired column. That one is mapped to Spring
-- Security's credentialsExpired, which fails authentication outright — it would lock AC IT out of
-- the app on their first login rather than prompting them, and with SMTP unconfigured there would
-- be no way back in.

ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
