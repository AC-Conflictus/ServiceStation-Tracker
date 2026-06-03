package edu.austincollege.sstation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Relationship + mapping coverage for the ported entities (TC-103). Runs against the real
 * Flyway-built schema (H2 in PostgreSQL mode) with Hibernate {@code ddl-auto=validate}, so a green
 * run also proves the entities and {@code V1__initial_schema.sql} agree.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DomainRelationshipTest {

  @Autowired private TestEntityManager em;
  @Autowired private EntityManager entityManager;

  @Test
  void userHasRealFkToStudent() {
    Student student = em.persist(newStudent("AC50001"));
    User user = new User("student", "{bcrypt}hash");
    user.setStudent(student);
    Long id = em.persistAndGetId(user, Long.class);
    em.flush();
    em.clear();

    User reloaded = em.find(User.class, id);
    assertThat(reloaded.getStudent()).isNotNull();
    assertThat(reloaded.getStudent().getAcid()).isEqualTo("AC50001");
  }

  @Test
  void userRoleJoinUsesCompositeKey() {
    User user = em.persist(new User("admin", "{bcrypt}hash"));
    Role role = em.persist(new Role("ROLE_ADMIN"));
    em.persist(new UserRole(user, role));
    em.flush();
    em.clear();

    Long count =
        entityManager
            .createQuery(
                "select count(ur) from UserRole ur where ur.user.username = :u", Long.class)
            .setParameter("u", "admin")
            .getSingleResult();
    assertThat(count).isEqualTo(1L);
  }

  @Test
  void studentOwnsServiceHoursAndCascades() {
    Student student = newStudent("AC50002");
    ServiceHour hour = newHour(Status.APPROVED);
    student.addServiceHour(hour);
    Long studentId = em.persistAndGetId(student, Long.class);
    em.flush();
    em.clear();

    Student reloaded = em.find(Student.class, studentId);
    assertThat(reloaded.getServiceHours()).hasSize(1);
    assertThat(reloaded.getServiceHours().get(0).getStatus()).isEqualTo(Status.APPROVED);
  }

  @Test
  void serviceHourFksAreNullableAndPersistWhenSet() {
    Student student = em.persist(newStudent("AC50003"));
    Event event = em.persist(newEvent());
    CampusOrg org = em.persist(newCampusOrg());
    CommunityAgency agency = em.persist(newAgency());

    // All three FKs null — the nullable decision from TC-103.
    ServiceHour bare = newHour(Status.PENDING);
    bare.setStudent(student);
    bare.setOtherCamOrg("Some unlisted org");
    Long bareId = em.persistAndGetId(bare, Long.class);

    // All three FKs set.
    ServiceHour full = newHour(Status.APPROVED);
    full.setStudent(student);
    full.setEvent(event);
    full.setCampusOrg(org);
    full.setCommAg(agency);
    Long fullId = em.persistAndGetId(full, Long.class);

    em.flush();
    em.clear();

    ServiceHour reloadedBare = em.find(ServiceHour.class, bareId);
    assertThat(reloadedBare.getEvent()).isNull();
    assertThat(reloadedBare.getCampusOrg()).isNull();
    assertThat(reloadedBare.getCommAg()).isNull();
    assertThat(reloadedBare.getOtherCamOrg()).isEqualTo("Some unlisted org");

    ServiceHour reloadedFull = em.find(ServiceHour.class, fullId);
    assertThat(reloadedFull.getEvent()).isNotNull();
    assertThat(reloadedFull.getCampusOrg()).isNotNull();
    assertThat(reloadedFull.getCommAg()).isNotNull();
  }

  @Test
  void classificationAndStatusPersistAsStrings() {
    Student student = newStudent("AC50004");
    student.setClassification(Classification.SR);
    Long id = em.persistAndGetId(student, Long.class);
    em.flush();
    em.clear();

    // Read the raw column to prove @Enumerated(STRING).
    Object raw =
        entityManager
            .createNativeQuery("select classification from students where id = :id")
            .setParameter("id", id)
            .getSingleResult();
    assertThat(raw).isEqualTo("SR");
  }

  // --- fixtures ---

  private static Student newStudent(String acid) {
    Student s = new Student();
    s.setFirstname("Sam");
    s.setLastname("Student");
    s.setAcid(acid);
    s.setAcEmail("student@austincollege.edu");
    s.setStatus('A');
    s.setClassification(Classification.JR);
    s.setIsModerator(false);
    return s;
  }

  private static ServiceHour newHour(Status status) {
    ServiceHour h = new ServiceHour();
    h.setDescription("service");
    h.setStatus(status);
    h.setDuration(2.5);
    h.setStartTime(LocalDateTime.now().minusDays(3));
    h.setLastModified(LocalDateTime.now());
    return h;
  }

  private static Event newEvent() {
    Event e = new Event();
    e.setName("JanServe");
    e.setDescription("desc");
    e.setContact("Jane Doe");
    e.setContactPhone("9038132000");
    e.setContactEmail("jdoe@austincollege.edu");
    return e;
  }

  private static CampusOrg newCampusOrg() {
    CampusOrg o = new CampusOrg();
    o.setName("THINK");
    o.setDescription("desc");
    o.setContact("Jane Doe");
    o.setContactPhone("9038132000");
    o.setContactEmail("jdoe@austincollege.edu");
    return o;
  }

  private static CommunityAgency newAgency() {
    CommunityAgency a = new CommunityAgency();
    a.setName("Crisis Center");
    a.setAddress("900 N. Grand Ave., Sherman, TX");
    a.setDescription("desc");
    a.setContact("Jane Doe");
    a.setContactPhone("9038132000");
    a.setContactEmail("jdoe@austincollege.edu");
    return a;
  }
}
