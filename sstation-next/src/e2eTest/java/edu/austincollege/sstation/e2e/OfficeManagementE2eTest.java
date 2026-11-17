package edu.austincollege.sstation.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.SelectOption;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Office CRUD and moderator flags, using disposable students rather than shared accounts. */
class OfficeManagementE2eTest extends E2eTest {
  @Test
  void studentAndHourEditsPersistAndModeratorFlagCanBeReversed() {
    String id = "E2E" + UUID.randomUUID().toString().substring(0, 8);
    String last = "Office-" + id;
    signInAsAdmin();
    page.navigate(url("/admin/students/new"));
    page.fill("[name=acid]", id);
    page.fill("[name=firstname]", "Browser");
    page.fill("[name=lastname]", last);
    page.fill("[name=acEmail]", id + "@example.invalid");
    page.fill("[name=status]", "A");
    page.selectOption("[name=classification]", "JR");
    submit();
    searchStudent(id);
    Locator row = page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(id));
    row.getByText("Edit", new Locator.GetByTextOptions().setExact(true)).click();
    page.fill("[name=acBox]", "4242");
    page.fill("[name=lastname]", last + "-edited");
    submit();
    searchStudent(id);
    row.getByText("Edit", new Locator.GetByTextOptions().setExact(true)).click();
    assertThat(page.inputValue("[name=acBox]")).isEqualTo("4242");
    assertThat(page.inputValue("[name=lastname]")).isEqualTo(last + "-edited");

    page.navigate(url("/admin/moderators"));
    Locator candidate = page.locator("tr").filter(new Locator.FilterOptions().setHasText(id));
    candidate.getByText("Promote", new Locator.GetByTextOptions().setExact(true)).click();
    assertThat(candidate.getByText("Demote").count()).isEqualTo(1);
    page.reload();
    candidate.getByText("Demote").click();
    page.reload();
    assertThat(candidate.getByText("Promote").count()).isEqualTo(1);

    page.navigate(url("/admin/hours/new"));
    page.selectOption(
        "[name=studentId]", new SelectOption().setLabel("Browser " + last + "-edited"));
    page.fill("[name=description]", "Hour-" + id);
    page.fill("[name=duration]", "1.25");
    page.fill("[name=startTime]", "2026-01-15T12:00");
    page.selectOption("[name=status]", "PENDING");
    submit();
    Locator hour =
        page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(last + "-edited"));
    hour.getByText("Edit", new Locator.GetByTextOptions().setExact(true)).click();
    page.fill("[name=duration]", "2.5");
    page.fill("[name=description]", "Hour-edited-" + id);
    submit();
    hour =
        page.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(last + "-edited"));
    hour.getByText("Edit", new Locator.GetByTextOptions().setExact(true)).click();
    assertThat(page.inputValue("[name=duration]")).isEqualTo("2.5");
    assertThat(page.inputValue("[name=description]")).isEqualTo("Hour-edited-" + id);
    page.navigate(url("/admin/hours"));
    hour.getByText("Delete", new Locator.GetByTextOptions().setExact(true)).click();
    assertThat(page.locator("tbody").textContent()).doesNotContain(last + "-edited");
    page.navigate(url("/admin/students"));
    searchStudent(id);
    row.getByText("Delete", new Locator.GetByTextOptions().setExact(true)).click();
    searchStudent(id);
    assertThat(page.locator("tbody").textContent()).doesNotContain(id);
  }

  private void searchStudent(String id) {
    page.waitForSelector("#studentsTable_wrapper input[type=search]");
    page.fill("#studentsTable_wrapper input[type=search]", id);
  }

  @Test
  void moderatorCannotManageModeratorFlags() {
    signInAsModerator();
    page.navigate(url("/admin/moderators"));
    assertThat(page.textContent("main")).contains("You don't have access to that page");
  }
}
