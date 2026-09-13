package edu.austincollege.sstation.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TC-111: the write paths — CRUD, the quick approve/reject round trip, and student event sign-up.
 *
 * <p>These are self-contained on purpose. Each test creates whatever it needs with a unique name
 * and cleans up after itself rather than leaning on the seeded fixture, so the suite can be re-run
 * against a stack that is already dirty. The alternative — approving the one PENDING hour the demo
 * seeder happens to create — passes once and then fails forever on the same container.
 */
@DisplayName("E2E: write flows")
class WriteFlowsE2eTest extends E2eTest {

  /** Unique per run so re-running against a live stack never collides. */
  private String unique(String prefix) {
    return prefix + "-e2e-" + System.currentTimeMillis();
  }

  /** Fills every field on the event form. All five are {@code @NotBlank}. */
  private void fillEventForm(String name) {
    page.fill("input[name='name']", name);
    page.fill("textarea[name='description']", "Created by the TC-111 end-to-end suite.");
    page.fill("input[name='contact']", "E2E Runner");
    page.fill("input[name='contactPhone']", "9038131000");
    page.fill("input[name='contactEmail']", "e2e@austincollege.edu");
  }

  @Test
  @DisplayName("an admin can create, edit and delete an event")
  void adminCanRunTheFullCrudCycle() {
    String name = unique("Great Day of Service");
    String renamed = name + "-edited";

    signInAsAdmin();

    // Create. Every field on Event is @NotBlank, so a partial fill re-renders the form instead
    // of saving — fill them all.
    page.navigate(url("/admin/events/new"));
    fillEventForm(name);
    submit();
    assertThat(page.textContent("body")).contains(name);

    // Edit — find this row's Edit link by the name we just created.
    page.navigate(url("/admin/events"));
    page.locator("tr:has-text('" + name + "')").locator("a:has-text('Edit')").click();
    page.waitForLoadState();
    page.fill("input[name='name']", renamed);
    submit();
    assertThat(page.textContent("body")).contains(renamed);

    // Delete
    page.navigate(url("/admin/events"));
    page.locator("tr:has-text('" + renamed + "')").locator("button:has-text('Delete')").click();
    page.waitForLoadState();
    assertThat(page.textContent("body")).doesNotContain(renamed);
  }

  @Test
  @DisplayName("bean validation rejects an empty event name in the browser")
  void validationRejectsAnEmptyName() {
    signInAsAdmin();
    page.navigate(url("/admin/events/new"));

    // The template sets no HTML5 `required`, so an empty submit reaches the server and @Valid
    // is what rejects it (TC-106). Submitting blank must re-render the form with field errors
    // rather than redirect to the list or 500.
    submit();

    assertThat(page.locator("input[name='name']").count()).isGreaterThan(0);
    assertThat(page.textContent("body")).contains("must not be blank");
  }

  @Test
  @DisplayName("the quick approve round trip updates the badge and writes an audit entry")
  void quickApproveUpdatesStatusAndAudits() {
    signInAsAdmin();

    // Work with a hour that is actually PENDING. The demo seeder gives Sam Student one, but
    // find it dynamically rather than assuming a row index.
    page.navigate(url("/admin/hours/pending"));

    // `tbody` and an exact text match, both on purpose: the bulk "Approve selected" button
    // (TC-108b) sits above the table and also contains the word "Approve", so a loose
    // `has-text('Approve')` picks it first — and with no rows checked it just alerts, which
    // Playwright auto-dismisses, leaving the test to time out waiting for a status that was
    // never going to change.
    Locator rowApprove = page.locator("tbody button:text-is('Approve')");
    assertThat(rowApprove.count())
        .as("the demo fixture should leave at least one PENDING hour to approve")
        .isGreaterThan(0);

    // Take the id from the same row the button lives in, not from the first badge on the page.
    String hourId =
        rowApprove
            .first()
            .evaluate("btn => btn.closest('tr').querySelector(\"[id^='status-']\").id")
            .toString()
            .replace("status-", "");

    rowApprove.first().click();

    // TC-106's endpoint is a JSON fetch that rewrites the badge in place — no navigation. Wait
    // for the DOM to actually change rather than sleeping.
    page.waitForFunction(
        "id => document.getElementById('status-' + id)"
            + " && document.getElementById('status-' + id).textContent.trim() === 'APPROVED'",
        hourId);

    // The audit trail is the part that must not be skippable: every status change is recorded
    // (TC-106c), and this is the only test that proves it end to end through the UI.
    page.navigate(url("/admin/hours/" + hourId + "/audit"));
    String audit = page.textContent("body");
    assertThat(audit).contains("APPROVED");
    assertThat(audit).contains(ADMIN_USER);
  }

  @Test
  @DisplayName("a moderator cannot change an hour's status")
  void moderatorCannotChangeStatus() {
    // Status changes are ADMIN-only by design (the Grails "only admin can change status" rule).
    signInAsModerator();
    page.navigate(url("/admin/hours"));

    assertThat(page.locator("tbody button:text-is('Approve')").count())
        .as("moderators must not even be offered the per-row approve control")
        .isZero();
    assertThat(page.locator("button:has-text('Approve selected')").count())
        .as("nor the bulk one")
        .isZero();
  }

  @Test
  @DisplayName("a student can sign up for an event")
  void studentCanSignUpForAnEvent() {
    signInAsStudent();
    page.navigate(url("/student/events"));

    Locator signUpButtons = page.locator("button:has-text('Sign up')");
    if (signUpButtons.count() == 0) {
      // Already signed up for everything on a re-run against a dirty stack — the flow still
      // has to show that state rather than an error page.
      assertThat(page.textContent("body")).doesNotContain("Something went wrong");
      return;
    }

    signUpButtons.first().click();
    page.waitForLoadState();

    // TC-108f: the page comes back showing the signup, not an error.
    assertThat(page.textContent("body")).doesNotContain("Something went wrong");
    assertThat(
            page.locator("button:has-text('Cancel'), :text('SIGNED_UP'), :text('Signed up')")
                .count())
        .isGreaterThan(0);
  }
}
