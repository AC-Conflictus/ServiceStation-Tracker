package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.austincollege.sstation.domain.CampusOrg;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Deleting a reference entity detaches it from its service hours (nulls the FK) but keeps the hours
 * (TC-106a decision), rather than cascade-deleting them like the Grails app.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReferenceCrudServiceTest {

  @Autowired private ServiceHourRepository serviceHours;
  @Autowired private StudentRepository students;
  @Autowired private EventRepository events;
  @Autowired private CampusOrgRepository campusOrgs;
  @Autowired private CommunityAgencyRepository agencies;

  private ReferenceCrudService service;

  @BeforeEach
  void setUp() {
    service = new ReferenceCrudService(events, campusOrgs, agencies, serviceHours);
  }

  @Test
  void deletingEventKeepsHoursButClearsTheFk() {
    Student sam = students.save(student());
    Event event = events.save(event());
    Long hourId = serviceHours.save(hour(sam, event, null, null)).getId();

    service.deleteEvent(event.getId());

    assertThat(events.findById(event.getId())).isEmpty();
    ServiceHour reloaded = serviceHours.findById(hourId).orElseThrow();
    assertThat(reloaded.getEvent()).isNull();
  }

  @Test
  void deletingCampusOrgKeepsHoursButClearsTheFk() {
    Student sam = students.save(student());
    CampusOrg org = campusOrgs.save(campusOrg());
    Long hourId = serviceHours.save(hour(sam, null, org, null)).getId();

    service.deleteCampusOrg(org.getId());

    assertThat(campusOrgs.findById(org.getId())).isEmpty();
    assertThat(serviceHours.findById(hourId).orElseThrow().getCampusOrg()).isNull();
  }

  @Test
  void deletingCommunityAgencyKeepsHoursButClearsTheFk() {
    Student sam = students.save(student());
    CommunityAgency agency = agencies.save(agency());
    Long hourId = serviceHours.save(hour(sam, null, null, agency)).getId();

    service.deleteCommunityAgency(agency.getId());

    assertThat(agencies.findById(agency.getId())).isEmpty();
    assertThat(serviceHours.findById(hourId).orElseThrow().getCommAg()).isNull();
  }

  // --- fixtures ---

  private static Student student() {
    Student s = new Student();
    s.setFirstname("Sam");
    s.setLastname("Student");
    s.setAcid("AC50000");
    s.setAcEmail("student@austincollege.edu");
    s.setStatus('A');
    s.setIsModerator(false);
    return s;
  }

  private static ServiceHour hour(Student s, Event e, CampusOrg o, CommunityAgency a) {
    ServiceHour h = new ServiceHour();
    h.setDescription("service");
    h.setStatus(Status.APPROVED);
    h.setDuration(2.0);
    h.setStartTime(LocalDateTime.now());
    h.setLastModified(LocalDateTime.now());
    h.setStudent(s);
    h.setEvent(e);
    h.setCampusOrg(o);
    h.setCommAg(a);
    return h;
  }

  private static Event event() {
    Event e = new Event();
    e.setName("JanServe");
    e.setDescription("desc");
    e.setContact("Jane Doe");
    e.setContactPhone("9038132000");
    e.setContactEmail("jdoe@austincollege.edu");
    return e;
  }

  private static CampusOrg campusOrg() {
    CampusOrg o = new CampusOrg();
    o.setName("THINK");
    o.setDescription("desc");
    o.setContact("Jane Doe");
    o.setContactPhone("9038132000");
    o.setContactEmail("jdoe@austincollege.edu");
    return o;
  }

  private static CommunityAgency agency() {
    CommunityAgency a = new CommunityAgency();
    a.setName("Crisis Center");
    a.setAddress("900 N. Grand Ave.");
    a.setDescription("desc");
    a.setContact("Jane Doe");
    a.setContactPhone("9038132000");
    a.setContactEmail("jdoe@austincollege.edu");
    return a;
  }
}
