# TC-111 — Office acceptance session

Status: ready to use; **no stakeholder sign-off recorded**.
Use alongside the [feature coverage checklist](TC-111-parity-checklist.md).
Allow about 45–60 minutes; record anything unfinished as Not tested.

## Prepare now, without Supabase

A project maintainer hosts the session with a Service Station staff member who knows the daily work.
From `sstation-next/`, with JDK 21 installed, run `./gradlew bootRun` and open
`http://localhost:8080`. This activates `dev`, uses an in-memory H2 database and seeds fake data.
See [local setup](../demo.md) for prerequisites and troubleshooting. Stop the Java process when done;
the database disappears when that process exits. This checks office workflow, not production
Postgres or AC directory integration.

Default local logins are `admin` / `admin_secret`, `moderator` / `moderator_secret`,
and `student` / `student_secret` (unless the host overrides the dev passwords).
Sign out between roles. Do not put passwords in the session record.
The hosted Vercel demo can be used for the synthetic walkthrough once configured; use its supplied
demo credentials instead. It must contain fake data only.

Record a copy of this document privately for each session:

- Reviewer name and office role: __________
- Facilitator and date: __________
- Build tested (`git rev-parse HEAD`) and local/hosted environment: __________
- Session result: Pending / Changes needed / Accepted

## Guided walkthrough (synthetic data)

For each row record Pass, Fail or Not tested and a finding ID where needed. A facilitator can
navigate directly to the paths shown. Use disposable records with an obvious `Office review` name;
never edit real office records during this walkthrough.

| ID | Role and action | Expected result | Result / finding |
| --- | --- | --- | --- |
| W1 | Admin signs in; open `/admin`, change dashboard date range | Admin landing, populated KPIs/charts; totals reflect the selected range | |
| W2 | Admin opens `/reports`; try all six reports and their selectors; download CSV | Summary, semester, year, event, community and campus reports render; downloads match the chosen report | |
| W3 | Admin creates then edits an event in `/admin/events` | Saved fields persist; blank required fields give a useful error | |
| W4 | Admin creates a disposable student and a service hour through management links | Saved student/hour can be found and edited | |
| W5 | Admin opens `/admin/hours/pending`, approves/rejects synthetic hours and opens Audit; tries bulk status action | Status changes persist; audit records who changed what | |
| W6 | Admin manages a disposable campus org and agency | Create/edit works; deletion detaches references rather than deleting associated hours | |
| W7 | Moderator signs in; opens `/reports` and `/admin/students`; visits `/admin` directly | Reports and student management available; admin dashboard denied; no approve/reject controls | |
| W8 | Student signs in; opens `/student` and `/student/report`; downloads CSV and PDF | Own totals and hours appear; downloads open and describe the same student | |
| W9 | Student signs up at `/student/events`; admin checks that event’s roster | Sign-up appears on roster; repeat sign-up does not create a duplicate | |
| W10 | Student visits `/admin` and `/admin/students/import` directly; then signs out | Access denied; signed-out pages require login | |
| W11 | Admin previews a synthetic registrar CSV at `/admin/students/import`, confirms valid rows, then repeats | Preview saves nothing and shows mapped fields/warnings; confirmation adds records; re-import updates without duplicates; invalid rows have reasons | |

For W11, create a local UTF-8 file using the example below. These are invented records.
On a fresh database expect **1 added, 0 updated, 1 skipped**; on re-import expect
**0 added, 1 updated, 1 skipped**. Header names are illustrative: the importer uses positions.

```csv
acid,ignored,firstname,lastname,status,acbox,classification,ignored2,email
REVIEW001,,Office,Example,A,999,JR,,office-review@example.invalid
REVIEW002,,Invalid,Example,A,998,FR,,not-an-email
```

## Private real registrar-file check

Do this separately from the public demo. The staff member keeps custody of the source file.
Use a fresh local `bootRun` process with its default in-memory database, on an office-approved
machine. From `sstation-next/`, start it with a one-off admin password supplied in the host’s
shell environment (do not write the password in the command or session record):

```bash
read -s SSTATION_DEV_ADMIN_PASSWORD
export SSTATION_DEV_ADMIN_PASSWORD
./gradlew bootRun --args='--server.address=127.0.0.1'
unset SSTATION_DEV_ADMIN_PASSWORD
```

Enter the one-off password at the `read` prompt and press Enter. The app is reachable only on this
machine at `http://127.0.0.1:8080`; do not expose it through a tunnel or proxy.
Do not enable `prod`/`demo` profiles or configure an external database or SMTP relay.
Do not put real exports, screenshots containing students, or download artifacts in Git, PRs,
CI, the Vercel/Supabase showcase, or this chat. Keep the completed evidence in the office’s
approved private location; only sanitized findings belong in project issues.

1. Before upload, compare the original export’s positions with the table below. The first row is
   always skipped; UTF-8 and at least nine columns are expected. If the layout differs, stop and
   record a sanitized mapping issue rather than importing into a live roster.
2. As admin, preview the real export at `/admin/students/import`. Inspect the mapped values and warnings before confirming valid rows. Record added/updated/skipped counts
   and reason categories, without copying row values. A successful import is not proof of mapping.
   First-run updates can reflect duplicate IDs in the file or collisions with seeded fake IDs
   (`AC50000`–`AC59999`); investigate them instead of assuming every first-run row was added.
3. Open `/admin/students`, then Edit for representative rows from each classification/status in
   the source. Compare every mapped field below with the original. Verify ignored columns really
   are dispensable. Inspect classifications carefully: words such as `Junior` become `OTHER`;
   status uses only the first character (`Active` becomes `A`). Record any mismatch as a finding.
4. Re-import the same file; valid existing IDs should update, not duplicate. Account for duplicate
   IDs in the source when comparing counts. Record only aggregate results.
5. Stop the Java process and close the session. Remove temporary copies/downloads according to
   office policy; keep the official source export in its approved location. Do not commit evidence
   containing student data. A later retest needs a fresh process and a new session record.

| Spreadsheet / zero-based column | Mapping to compare on student Edit form | Confirmed / finding |
| --- | --- | --- |
| A / 0 | Student ID (ACID); key for add-or-update | |
| B / 1 | Ignored | |
| C, D / 2, 3 | First name, last name | |
| E / 4 | Status; required, first character only | |
| F / 5 | AC box | |
| G / 6 | Classification: FR, SO, JR, SR, GRAD, OTHER; unknown/blank → OTHER | |
| H / 7 | Ignored | |
| I / 8 | AC email | |

- Real-file check reviewer/date/build: __________
- Added / updated / skipped (first run and re-import): __________
- Mapping and classification/status sample comparison: Pass / Fail / Not tested
- Sanitized findings: __________

## Findings and office decision

Keep each observation actionable. Record steps, expected and observed behavior using fake examples.
Use disposition **Blocks acceptance**, **Fix before handoff**, **Accepted difference**, or **Not a bug**;
the office decides whether a difference is acceptable. Link a sanitized issue for fixes.

| Finding | Step | Expected / observed | Disposition and office rationale | Issue / retest result |
| --- | --- | --- | --- | --- |
| | | | | |

Do not mark TC-111 complete merely because automated tests pass or this guide was merged.
Acceptance requires the walkthrough and real-file mapping check, resolution/retest of blocking
findings, and an explicit office decision. Not tested items remain open; an accepted difference
needs the reviewer’s rationale. SMTP delivery and real AC directory authentication remain separate
AC IT deployment checks; a local session cannot validate them.

- Outstanding steps or findings: __________
- Accepted differences and rationale: __________
- Office decision: Pending / Changes needed / Accepted
- Staff reviewer name, role, date: __________
- Build and environment accepted: __________

Once accepted, reference the private office record in the project handoff using a sanitized summary.
Leave TC-111 open until then; [TC-112](../TRELLO_CARDS.md#tc-112--decommission-the-grails-app)
(decommissioning Grails) follows acceptance.
