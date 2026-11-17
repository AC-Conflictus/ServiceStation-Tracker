# UI improvement backlog

Captured during the 2026-11-17 screenshot audit. Planning notes, not completed work.
Audit: 46 local synthetic-data views, including the PR #24 import preview and five mobile views.
The screenshot bundle was delivered in the conversation; it is not committed to this repository.

## Suggested order

- [ ] **Mobile layouts and navigation:** let headings/actions wrap cleanly (especially Students),
      make important table details/actions easier to reach without excessive horizontal scrolling,
      and identify the active navigation section.
- [ ] **Dashboard hierarchy:** surface pending work with a clear link, group date filters, and reduce
      chart height on phones so the page is easier to scan.
- [ ] **Forms:** group related fields, consistently explain required fields, and align save/cancel
      actions. Start with the long service-hour form.
- [ ] **Import preview:** use distinct Add/Update/Skip badges, separate warnings from validation
      errors, and improve spacing around counts and confirmation.
- [ ] **Product copy:** replace the implementation-oriented footer (Spring Boot/Lane 7 wording)
      with office-facing copy while preserving appropriate attribution.

Keep these as focused UI PRs with desktop/mobile browser verification. The GDS/GreenServe site
registration workflow in [the event plan](EVENT-SITE-REGISTRATION-PLAN.md) takes priority now.
