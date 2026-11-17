# GDS and GreenServe: registration by nonprofit site

Status: admin setup [PR #26](https://github.com/AC-Conflictus/ServiceStation-Tracker/pull/26), student browsing [PR #27](https://github.com/AC-Conflictus/ServiceStation-Tracker/pull/27), and the [counter bridge](https://github.com/jorj-pineda/GoogleFormAutoCounter/pull/1) are merged. Website integration (slice 4) is in progress, 2026-11-17. No live Google Forms changed.

## Product goal and confirmed decisions

Great Day of Service and GreenServe differ from ordinary events: students choose a nonprofit site
with their group, rather than joining the whole event with one app button. GDS commonly has 30+
Texoma sites; GreenServe uses the same workflow with fewer sites. Site locations and capacities
vary. Most sites share an event time, with exceptions possible.

- Open the event to browse its sites and availability, then follow that site's Google Form.
- Admins can configure a registration link per site. A missing link is allowed during setup;
  show a disabled “Registration link coming soon” control rather than a broken empty anchor.
- This website and its linked site forms handle **individual registration only**: one response
  represents one participant. The existing response-counting rule fits this flow.
- Group leaders receive an early email with a specialized form and enter each group member there.
  This separate group-registration process stays outside this feature; do not send groups to the
  individual forms or count group-form responses as individual registrations.
- Availability means **remaining / total**: 0/10 is full, 2/10 means two spots remain, 10/10 is empty.
  Prefer explicit labels such as **2 of 10 spots left** to avoid ambiguity.
- Reuse the existing Google form counter where possible; do not require manual counts as the
  default without first evaluating its bridge.
- Ordinary events retain the current signup/attendance/service-hour flow.

## Existing code and integration findings

Before these slices, the app had an Event entity and a student event list with one direct signup POST per
event. Slices 1–2 added EventSite and a student site-detail page. External-form registrations must
not create an internal EventSignup merely because a student clicked a link. Existing internal
roster/attendance-to-hours functionality cannot infer external registrations.

Initial review before slice 3 covered [GoogleFormAutoCounter](https://github.com/jorj-pineda/GoogleFormAutoCounter), specifically
[Counter.gs](https://github.com/jorj-pineda/GoogleFormAutoCounter/blob/main/Counter.gs),
[Front.html](https://github.com/jorj-pineda/GoogleFormAutoCounter/blob/main/Front.html), and its manifest.

- Counter.gs opens one configured Google Form, computes `totalSpots - responses.length`, updates
  its title, and closes responses when full. Capacity is currently a local hardcoded value.
- At that initial review, the repository exposed no count API or outbound notification. Slice 3 now adds optional signed aggregate updates. Front.html calls
  `getRemainingSpots`, but that function and a `doGet` handler are absent from the checked-in code.
  Do not assume the deployed scripts exactly match the repository.
- The trigger helper creates a one-minute timer; the README also describes on-submit triggers.
  Confirm actual deployment configuration before changing live scripts.
- Counts mean responses. The user confirmed one individual-form response represents one person.
  Group signups use a separate early-registration form. The remaining integration question is how
  the individual form limit is reduced to account for early group allocations.
- The non-full branch does not explicitly reopen a closed form. Do not silently change the office's
  reopening policy while adding count reporting.

## Proposed approach

Keep Forms/counter responsible for registration and capacity enforcement. Extend the counter with
an optional authenticated server-to-server update carrying only a stable site key, total capacity,
remaining individual places, form-open state and observation time. The app displays the latest aggregate.
Keep credentials in deployment settings / Apps Script properties, never in public links or Git.
The signed POST contract is documented in [SITE-AVAILABILITY-SYNC.md](SITE-AVAILABILITY-SYNC.md). A disposable-form deployment check remains a rollout step.

The app must not scrape a form title, expose participant responses, or decrement availability on
link clicks. If updates fail, show “Availability unavailable” or a clearly dated last-known value;
never turn an outage into “Full” or present old counts as live. The form remains the final authority,
since seats may fill between viewing the site and opening the form. Receiving counts does not
reserve seats or populate the app's attendance roster.

Use an event registration mode (ordinary signup vs site forms), not name matching for GDS or
GreenServe. Each event occurrence owns its own site list and external mappings, so another year's
signup data cannot overwrite this year's counts. Create a new event record for each occurrence
(for example, “Great Day of Service 2027”), rather than repurposing last year's event and sites.

Candidate site fields: nonprofit/site name, description/requirements, address/location, display
order, optional registration URL, and event-level default date/start/end with optional site time
overrides. Linked sites use the counter's **individual signup limit** as the displayed source of truth,
not automatically the nonprofit's full physical capacity. Label it “individual spots left” when
that distinction matters. For example, a site with 20 places and 12 assigned to groups offers an
individual pool of 8; after 3 individual responses it shows **5 of 8 individual spots left**.
Do not subtract the 12 twice if the form counter was already configured with a limit of 8. Unsynced sites
must explicitly show availability as unavailable until configured. A normal event defaults to the
existing signup mode after migration.

## Proposed delivery: four focused PRs

| PR | Scope | Proof and completion boundary |
| --- | --- | --- |
| 1 — Site setup | Migration/entities and admin-only site CRUD under an event; event time defaults/site overrides; optional form link. Keep existing student registration unchanged until PR2 exposes the mode switch. | Upgrade existing database; validate location/time/link input (capacity remains owned by the counter); verify admin permissions and site/event ownership; browser create/edit/remove with synthetic sites. |
| 2 — Student site browsing | Enable site-based registration mode, event detail with 30+ site cards/list, location/requirements/time, registration-link placeholder and unavailable state (full-state display follows live counts in PR4). Reject legacy direct-signup POSTs for site-mode events. | Desktop/mobile browsing, missing link, unavailable counts, empty site list, ordinary signup regression, and no internal signup created by external navigation. Before sync, clearly show availability unavailable. |
| 3 — Counter bridge | Separate PR in GoogleFormAutoCounter: extract reusable site/capacity config and publish aggregate updates without changing working registration rules. Document installation and use a disposable form first. | Synthetic submissions update counts; one response consumes one individual place; signed update contract has deterministic tests; secrets/participant data never included. Live installation is a separate rollout step. |
| 4 — Availability integration | App receiver and admin mapping/configuration; authenticate updates, validate bounds and site ownership, reject outdated observations, expose freshness and error states. | Contract tests with PR3 payloads; fake/stale/unauthorized updates; full and reopened/closed states; browser availability display. Real Google integration still needs a test deployment. |

PR1/PR2 can proceed without Supabase. PR3/PR4 can be implemented with local fixtures; live Apps
Script publishing and connecting a reachable app endpoint require the appropriate accounts and
rollout configuration. Nothing here authorizes editing the office's running forms without a
reviewed deployment plan. Keep PRs open for user merge decisions.

## Decisions to settle before implementation

- [x] **Counting unit:** one individual-form submission per person; `responses.length` is appropriate.
      Early group forms are separate and outside this website flow.
- [x] **Capacity allocation:** staff set each individual counter's `totalSpots` to the places
      remaining after early groups are assigned. Use that limit directly; do not subtract group
      allocations again or require group-allocation fields in the app.
- [ ] Confirm whether existing deployed counters differ from the repository and which trigger is
      installed; use a disposable form, not live office registrations, for the bridge experiment.
- [x] Full/closed sites disable registration while counts are current. Once stale, allow checking
      the form. Waitlists remain outside this feature.
- [x] Site management is ADMIN-only, matching the request for admin-managed links.
- [x] Students remain signed into the app to browse; Google Forms access/login is
      governed by that form's settings. Do not promise that app login signs them into Google.

## Deferred

In-app group reservations, participant synchronization, attendance imports, automated hour creation
from Google Forms, waitlist management, and general UI polish are separate follow-ups. UI ideas
are tracked in [UI-IMPROVEMENTS.md](UI-IMPROVEMENTS.md).

## Using site registration (slice 2)

An admin opens Events → Sites and selects “Choose a nonprofit site and open its individual form”.
The default remains direct signup for every existing event. Site events show “View sites” to
students; ordinary events keep “Sign up”. Event editors who are moderators cannot change this mode,
including by posting a mode value through the ordinary event form.

The student page uses a responsive site-card grid, site order, descriptions, locations, requirements,
and either site-specific times or the event default. Blank links are disabled placeholders.
Configured links open in a new tab. Neither viewing a site nor opening a form creates an internal
signup or attendance entry. Existing internal signups are retained when an admin changes the mode.
The old direct-signup endpoint refuses site events. The availability integration adds current counts, full/closed states and stale observations;
unsynced sites still show availability unavailable. The external form remains the final authority.
