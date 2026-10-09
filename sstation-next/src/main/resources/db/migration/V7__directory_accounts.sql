-- AC credentials sign-in (TC-124).
--
-- Records who owns an account's password. LOCAL accounts sign in with a password stored here (the
-- placeholder login, the day-one bootstrap admin, break-glass access). DIRECTORY accounts are
-- created the first time someone signs in with their AC user name and password; their password
-- belongs to AC IT, so this app never accepts a local password for them, and the reset and change
-- flows refuse them.
--
-- V6 is taken by the spring-session tables on the TC-122 branch (PR #15), hence V7.

ALTER TABLE users ADD COLUMN auth_source VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
