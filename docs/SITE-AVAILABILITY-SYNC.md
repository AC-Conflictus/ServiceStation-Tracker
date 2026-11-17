# Connecting individual Google Form availability

This is slice 4 of [the event-site plan](EVENT-SITE-REGISTRATION-PLAN.md). The sender is
[GoogleFormAutoCounter PR #1](https://github.com/jorj-pineda/GoogleFormAutoCounter/pull/1).
Merging code does not publish Apps Script or change any live office form. First use a disposable
copied form and synthetic responses; actual Google authorization and delivery still need that check.

## Website setup

1. Configure `SSTATION_SITE_SYNC_MASTER_SECRET` in your deployment's secret settings: a random value
   of at least 32 characters (for example, generate one with `openssl rand -base64 48`). Leave blank
   to disable receiving/provisioning and show availability unavailable. All app instances must share
   the same value. Docker compose passes this variable through; local `bootRun` also reads it.
   Never use the checked-in CI-only value outside its isolated test stack, or commit a real key.
2. As an admin, open Events → Sites → Availability for the specific site and event occurrence.
   “Create / replace connection” generates a new random site key and shows a derived signing secret
   in that response only. Copy both directly to that site's Apps Script Script Properties. A later
   GET never displays the secret; replacing the connection is how to recover a lost credential.
   This page uses `Cache-Control: no-store`. Do not share, screenshot or record the credentials.
3. Set the sender's `SSTATION_SYNC_URL` to your public HTTPS origin plus `/api/site-availability`.
   The endpoint must be reachable by Google without an interactive login, proxy redirect, or Vercel
   deployment-protection page. Do not put credentials in the URL. Keep the site's student-facing
   registration URL pointed at the individual Google Form, not at this machine endpoint.
4. Follow the counter repository's [copied-form rollout](https://github.com/jorj-pineda/GoogleFormAutoCounter/blob/main/docs/WEBSITE-SYNC.md).
   Its `totalSpots` is the individual pool **after early groups**; do not subtract group allocations
   again. One response means one individual. Verify title/closure behavior before enabling reporting.
5. Run the counter manually, submit synthetic responses, and verify counts on the student page.
   Test full, office-closed with positive remaining, manually reopened with remaining, and a stopped
   trigger. Inspect trigger configuration: on-submit is prompt, while a single one-minute refresh
   keeps observations fresh and recovers missed deliveries. Do not repeatedly create duplicate timers.

Master secret → HMAC-SHA256 over `site-availability-key-v1\n` + site key → lowercase hexadecimal
per-site secret. Both HMAC keys are literal UTF-8 strings, not base64/hex decoded. Only the site key
and aggregate observations persist in the database; the master and per-site secrets do not. An Apps
Script editor holds only its site's derived key, which cannot authenticate another site's updates.
Each new event occurrence gets new sites/connections. Protect the master like other deployment secrets.

When replacing a site’s Google Form, replace its connection as well so old observations cannot describe the new form.
Replacing a connection revokes its old key and clears observations. Disconnecting also clears them
and rejects further old updates. Deleting a site/event cascades its observations. Replacing the master
invalidates every existing derived key: disconnect/recreate each site's connection and update its
Script Properties; until then earlier observations can remain visible until they age out. Removing
the master hides all counts immediately. No participant records or internal signups are created.

## Contract and freshness

- Anonymous machine POST only at `/api/site-availability`, JSON, maximum 4096 body bytes. This exact
  endpoint has no browser session/CSRF requirement; browser routes retain normal role and CSRF checks.
- Header `X-Sstation-Signature: sha256=<64 lowercase hex digits>` authenticates HMAC-SHA256 over
  `v1\n` plus the **exact raw request body bytes**, with the matching per-site secret. Here `\n`
  means a single newline byte. Verification uses a constant-time digest comparison.
- Exactly six fields: integer `version:1`, `siteKey`, nonnegative integer `capacity`, integer
  `remaining` between zero and capacity, boolean `acceptingResponses`, and ISO instant `observedAt`.
  No form IDs, responses, participant names/emails, or secrets belong in the payload.
- Observations older than 10 minutes or more than 60 seconds in the future are rejected. Sync the
  server clock. Atomic conditional database updates prevent delayed/duplicate observations or requests
  using a rotated key from replacing current data. Timestamps are normalized to database microsecond
  precision before comparison; observations within the same microsecond are treated as duplicates.
  Authenticated superseded/duplicate updates inside the time window receive 204 without mutation. Only HTTP 204 acknowledges a valid delivery.
- Missing/bad signatures and unknown/revoked keys return 401; invalid schema/time returns 400;
  oversized bodies 413, wrong content type 415, and disabled sync 503. Responses do not echo credentials.
  Delivery failures preserve the previous observation; the next trigger sends a fresh one.
- Counts are current for 10 minutes after observation. Positive/open shows “5 of 8 individual spots
  left”; zero is full; positive/closed is “Registration closed”. Current full/closed disables the
  form link. Stale counts are explicitly “Last known”, and a “Check registration form” link remains.
  Counts also expire while a browser page stays open. With JavaScript disabled, the dated observation
  remains displayed until the page is refreshed. Unsynced sites say “Availability unavailable”.
- The form remains the final authority and may fill between page load and submission. A link click
  reserves nothing, and the sender does not make Google Forms capacity checks atomic.

## Verification

The shared [synthetic fixture](../sstation-next/src/test/resources/site-availability-v1.json) is copied
unchanged from the counter repository. Java verifies its HMAC; receiver tests cover real filter-chain
requests, validation, ordering, site key isolation, replacement/disconnect and admin permissions.
PostgreSQL migration coverage checks upgrades and cascading deletion. Browser tests provision a site,
send signed anonymous HTTP updates, inspect desktop/mobile states and advance the browser clock to
prove that a full site's link becomes available when its observation goes stale. CI secrets are fake.
These checks do not prove live Apps Script permissions, installed triggers, or public HTTPS delivery.
