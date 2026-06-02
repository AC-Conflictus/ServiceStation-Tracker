package edu.austincollege.sstation.config;

import edu.austincollege.sstation.domain.CampusOrg;
import edu.austincollege.sstation.domain.Classification;
import edu.austincollege.sstation.domain.CommunityAgency;
import edu.austincollege.sstation.domain.Event;
import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.CampusOrgRepository;
import edu.austincollege.sstation.repository.CommunityAgencyRepository;
import edu.austincollege.sstation.repository.EventRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds demo reference data and randomized students + service hours so the dashboards and reports
 * have something to show in dev (TC-105). The Spring port of the bulk of the Grails {@code
 * BootStrap}.
 *
 * <p>Dev-only ({@code @Profile("dev")}) and idempotent (skips if hours already exist). Start times
 * are spread across the last five years ending today, via {@link LocalDateTime} — no repeat of the
 * Grails legacy-{@code Date} bug that dumped everything into 2011–2015 (TC-004). Sam Student
 * (AC50000) gets five hours across all three statuses (TC-005).
 */
@Component
@Profile("dev")
@Order(2)
public class DemoDataSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
  private static final int YEARS_BACK = 5;

  private final StudentRepository students;
  private final ServiceHourRepository serviceHours;
  private final EventRepository events;
  private final CampusOrgRepository campusOrgs;
  private final CommunityAgencyRepository agencies;

  private final Random random = new Random(42); // fixed seed → stable demo data

  private final List<String> firstNames =
      List.of("John", "Mary", "Mike", "Jenny", "Aaron", "Priya", "Marrisa", "Brittany");
  private final List<String> lastNames =
      List.of("Higgs", "Block", "Gorman", "Countryman", "Bortan", "Mattew", "Frindly");

  public DemoDataSeeder(
      StudentRepository students,
      ServiceHourRepository serviceHours,
      EventRepository events,
      CampusOrgRepository campusOrgs,
      CommunityAgencyRepository agencies) {
    this.students = students;
    this.serviceHours = serviceHours;
    this.events = events;
    this.campusOrgs = campusOrgs;
    this.agencies = agencies;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (serviceHours.count() > 0) {
      return; // already seeded
    }
    log.info("[dev] seeding demo reference data, students and service hours");

    List<CampusOrg> orgs = seedCampusOrgs();
    List<Event> evs = seedEvents();
    List<CommunityAgency> ags = seedAgencies();

    // Sam Student (the seeded `student` login) gets a populated dashboard (TC-005).
    students
        .findByAcid("AC50000")
        .ifPresent(
            sam -> {
              Status[] samStatuses = {
                Status.APPROVED, Status.PENDING, Status.REJECTED, Status.APPROVED, Status.APPROVED
              };
              for (Status st : samStatuses) {
                ServiceHour h = randomHour(orgs, evs, ags);
                h.setStatus(st);
                sam.addServiceHour(h);
              }
              students.save(sam);
            });

    for (int i = 0; i < 10; i++) {
      Student s = randomStudent();
      for (int j = 0; j < 10; j++) {
        s.addServiceHour(randomHour(orgs, evs, ags));
      }
      students.save(s);
    }
  }

  private List<CampusOrg> seedCampusOrgs() {
    List<CampusOrg> out = new ArrayList<>();
    for (String name :
        List.of(
            "Service Station", "Habitat of Humanity", "AC Church", "THINK", "BIG", "APO", "N/A")) {
      CampusOrg o = new CampusOrg();
      o.setName(name);
      o.setDescription(name);
      o.setContact(randomName());
      o.setContactPhone("9089038898");
      o.setContactEmail(randomEmail());
      out.add(campusOrgs.save(o));
    }
    return out;
  }

  private List<Event> seedEvents() {
    List<Event> out = new ArrayList<>();
    for (String name :
        List.of(
            "N/A",
            "Great Day of Service",
            "First We Serve",
            "GreenServe",
            "RooBound",
            "JanServe",
            "Austin College Tutoring (ACT)",
            "Alternative Spring Break",
            "Other")) {
      Event e = new Event();
      e.setName(name);
      e.setDescription("Contact the Service Station office to sign up!");
      e.setContact(randomName());
      e.setContactPhone("9089038898");
      e.setContactEmail(randomEmail());
      out.add(events.save(e));
    }
    return out;
  }

  private List<CommunityAgency> seedAgencies() {
    List<CommunityAgency> out = new ArrayList<>();
    for (String name :
        List.of(
            "N/A",
            "Texas Community Center",
            "Crisis Center",
            "Big Brothers and Sisters",
            "Sherman Elementary School",
            "Charity Group",
            "Sherman Church",
            "Other")) {
      CommunityAgency a = new CommunityAgency();
      a.setName(name);
      a.setAddress("900 N. Grand Ave., Sherman, TX");
      a.setDescription(name);
      a.setContact(randomName());
      a.setContactPhone("9089038898");
      a.setContactEmail(randomEmail());
      out.add(agencies.save(a));
    }
    return out;
  }

  private Student randomStudent() {
    String first = pick(firstNames);
    String last = pick(lastNames);
    int year = random.nextInt(4) + 2012;
    Classification clas =
        switch (year - 2011) {
          case 1 -> Classification.FR;
          case 2 -> Classification.SO;
          case 3 -> Classification.JR;
          case 4 -> Classification.SR;
          default -> Classification.OTHER;
        };
    Student s = new Student();
    s.setFirstname(first);
    s.setLastname(last);
    s.setAcid("AC" + (random.nextInt(10000) + 50000));
    s.setAcEmail(first.charAt(0) + last + (year - 2000) + "@austincollege.edu");
    s.setAcBox(String.valueOf(random.nextInt(30000) + 30000));
    s.setAcYear(year);
    s.setStatus('A');
    s.setClassification(clas);
    s.setIsModerator(false);
    s.setPhone("9038207084");
    return s;
  }

  private ServiceHour randomHour(List<CampusOrg> orgs, List<Event> evs, List<CommunityAgency> ags) {
    ServiceHour h = new ServiceHour();
    h.setDescription("service");
    h.setStatus(Status.values()[random.nextInt(Status.values().length)]);
    h.setDuration(random.nextInt(4) + (1 + random.nextInt(4)) / 4.0);
    h.setCampusOrg(pick(orgs));
    h.setEvent(pick(evs));
    h.setCommAg(pick(ags));
    h.setEventContactName(randomName());
    h.setEventContactPhone("9048937894");
    h.setEventContactEmail(randomEmail());

    // Spread across the last five years ending today (TC-004).
    LocalDateTime start =
        LocalDateTime.now()
            .minusDays(random.nextInt(YEARS_BACK * 365))
            .withHour(8 + random.nextInt(10))
            .withMinute(random.nextInt(60))
            .withSecond(0)
            .withNano(0);
    h.setStartTime(start);
    h.setLastModified(LocalDateTime.now());
    return h;
  }

  private String randomName() {
    return pick(firstNames) + " " + pick(lastNames);
  }

  private String randomEmail() {
    String first = pick(firstNames);
    String last = pick(lastNames);
    return first.charAt(0) + last + "@austincollege.edu";
  }

  private <T> T pick(List<T> list) {
    return list.get(random.nextInt(list.size()));
  }
}
