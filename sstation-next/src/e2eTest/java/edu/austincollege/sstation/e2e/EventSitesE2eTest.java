package edu.austincollege.sstation.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventSitesE2eTest extends E2eTest {
  @Test
  void adminConfiguresSiteWithBlankLinkThenAddsLinkAndScheduleOverride() {
    String name = "Sites-" + UUID.randomUUID();
    signInAsAdmin();
    page.navigate(url("/admin/events/new"));
    page.fill("[name=name]", name);
    page.fill("[name=description]", "Synthetic site event");
    page.fill("[name=contact]", "Office");
    page.fill("[name=contactPhone]", "9030000000");
    page.fill("[name=contactEmail]", "office@example.invalid");
    page.fill("[name=startsAt]", "2027-04-01T09:00");
    page.fill("[name=endsAt]", "2027-04-01T12:00");
    submit();
    var row =
        page.locator("tr")
            .filter(new com.microsoft.playwright.Locator.FilterOptions().setHasText(name));
    row.getByText("Edit", new com.microsoft.playwright.Locator.GetByTextOptions().setExact(true))
        .click();
    String updateAction = page.locator("main form").getAttribute("action");
    page.fill("[name=endsAt]", "2027-04-01T08:00");
    submit();
    assertThat(page.textContent("main")).contains("end after start");
    assertThat(page.locator("main form").getAttribute("action")).isEqualTo(updateAction);
    page.fill("[name=endsAt]", "2027-04-01T12:00");
    submit();
    com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(row).hasCount(1);
    row.getByText("Sites", new com.microsoft.playwright.Locator.GetByTextOptions().setExact(true))
        .click();
    String list = page.url();
    page.getByText("New site", new com.microsoft.playwright.Page.GetByTextOptions().setExact(true))
        .click();
    page.fill("[name=name]", "Example nonprofit");
    page.fill("[name=location]", "123 Example St");
    submit();
    assertThat(page.textContent("main")).contains("Example nonprofit", "Not configured");
    page.getByText("Edit", new com.microsoft.playwright.Page.GetByTextOptions().setExact(true))
        .click();
    assertThat(page.inputValue("[name=startsAt]")).isEmpty();
    page.fill("[name=location]", "456 New St");
    page.fill("[name=registrationUrl]", "https://forms.gle/synthetic");
    page.fill("[name=startsAt]", "2027-04-01T10:00");
    page.fill("[name=endsAt]", "2027-04-01T12:30");
    submit();
    page.getByText("Edit", new com.microsoft.playwright.Page.GetByTextOptions().setExact(true))
        .click();
    assertThat(page.inputValue("[name=location]")).isEqualTo("456 New St");
    assertThat(page.inputValue("[name=registrationUrl]")).isEqualTo("https://forms.gle/synthetic");
    assertThat(page.inputValue("[name=startsAt]")).startsWith("2027-04-01T10:00");
    page.navigate(list);
    page.getByText("Remove", new com.microsoft.playwright.Page.GetByTextOptions().setExact(true))
        .click();
    assertThat(page.textContent("main")).doesNotContain("Example nonprofit");
    page.navigate(url("/admin/events"));
    row.getByText("Delete", new com.microsoft.playwright.Locator.GetByTextOptions().setExact(true))
        .click();
    assertThat(page.textContent("main")).doesNotContain(name);
  }
}
