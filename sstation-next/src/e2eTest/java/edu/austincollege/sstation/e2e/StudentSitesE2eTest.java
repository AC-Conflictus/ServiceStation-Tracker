package edu.austincollege.sstation.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route.FulfillOptions;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StudentSitesE2eTest extends E2eTest {
  @Test
  void studentBrowsesSitesOnMobileAndOpensAnExternalFormWithoutJoiningTheRoster() {
    String name = "Great Day of Service — " + UUID.randomUUID();
    signInAsAdmin();
    page.navigate(url("/admin/events/new"));
    page.fill("[name=name]", name);
    page.fill("[name=description]", "Choose a Texoma nonprofit to volunteer with.");
    page.fill("[name=contact]", "Service Station Office");
    page.fill("[name=contactPhone]", "9030000000");
    page.fill("[name=contactEmail]", "office@example.invalid");
    page.fill("[name=startsAt]", "2027-04-10T09:00");
    page.fill("[name=endsAt]", "2027-04-10T12:00");
    submit();
    Locator eventRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText(name));
    eventRow.getByText("Sites", new Locator.GetByTextOptions().setExact(true)).click();
    String adminList = page.url();
    String eventId = adminList.replaceAll(".*/events/(\\d+)/sites", "$1");
    page.getByText("New site", new Page.GetByTextOptions().setExact(true)).click();
    String create = page.url();
    page.fill("[name=name]", "Community Pantry");
    page.fill("[name=location]", "Example location, Sherman, TX");
    page.fill("[name=description]", "Sort donations and prepare pantry boxes.");
    page.fill("[name=requirements]", "Closed-toe shoes. Bring water.");
    page.fill("[name=registrationUrl]", "https://forms.gle/site-browser-test");
    submit();
    page.navigate(create);
    page.fill("[name=name]", "Neighborhood Garden");
    page.fill("[name=location]", "Example location, Denison, TX");
    page.fill("[name=startsAt]", "2027-04-10T10:00");
    page.fill("[name=endsAt]", "2027-04-10T13:00");
    submit();
    page.selectOption("[name=registrationMode]", "SITE_FORMS");
    submit();
    page.click("button:has-text('Sign out')");
    signInAsStudent();
    page.navigate(url("/student/events"));
    eventRow.getByText("View sites", new Locator.GetByTextOptions().setExact(true)).click();
    assertThat(page.locator("article")).hasCount(2);
    assertThat(page.textContent("main"))
        .contains(
            "Community Pantry",
            "Neighborhood Garden",
            "9:00 AM",
            "10:00 AM",
            "Closed-toe shoes",
            "Availability unavailable");
    assertThat(page.getByText("Registration coming soon")).isDisabled();
    page.setViewportSize(390, 844);
    assertThat((Boolean) page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"))
        .isTrue();
    context.route(
        "https://forms.gle/site-browser-test",
        route ->
            route.fulfill(
                new FulfillOptions()
                    .setStatus(200)
                    .setContentType("text/html")
                    .setBody("<h1>Synthetic registration form</h1>")));
    Page form =
        page.waitForPopup(
            () ->
                page.getByText("Open registration form", new Page.GetByTextOptions().setExact(true))
                    .click());
    form.waitForLoadState();
    assertThat(form.textContent("body")).contains("Synthetic registration form");
    form.close();
    page.setViewportSize(1440, 1000);
    page.click("button:has-text('Sign out')");
    signInAsAdmin();
    page.navigate(url("/admin/events/" + eventId + "/roster"));
    assertThat(page.textContent("main")).contains("No sign-ups yet.");
    page.navigate(url("/admin/events"));
    eventRow.getByText("Delete", new Locator.GetByTextOptions().setExact(true)).click();
    assertThat(eventRow).hasCount(0);
  }
}
