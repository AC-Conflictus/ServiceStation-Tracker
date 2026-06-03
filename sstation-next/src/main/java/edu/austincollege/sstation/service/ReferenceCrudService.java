package edu.austincollege.sstation.service;

import edu.austincollege.sstation.repository.CampusOrgRepository;
import edu.austincollege.sstation.repository.CommunityAgencyRepository;
import edu.austincollege.sstation.repository.EventRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional deletes for the reference entities (Event / CampusOrg / CommunityAgency). Each
 * deletion first <b>detaches</b> the entity from any service hours that reference it — nulling the
 * (nullable) FK — instead of cascade-deleting those hours as the Grails app did. That preserves the
 * students' logged hours; only the org/event association is cleared (TC-106 decision).
 */
@Service
public class ReferenceCrudService {

  private final EventRepository events;
  private final CampusOrgRepository campusOrgs;
  private final CommunityAgencyRepository agencies;
  private final ServiceHourRepository serviceHours;

  public ReferenceCrudService(
      EventRepository events,
      CampusOrgRepository campusOrgs,
      CommunityAgencyRepository agencies,
      ServiceHourRepository serviceHours) {
    this.events = events;
    this.campusOrgs = campusOrgs;
    this.agencies = agencies;
    this.serviceHours = serviceHours;
  }

  @Transactional
  public void deleteEvent(Long id) {
    events
        .findById(id)
        .ifPresent(
            event -> {
              serviceHours.detachEvent(event);
              events.delete(event);
            });
  }

  @Transactional
  public void deleteCampusOrg(Long id) {
    campusOrgs
        .findById(id)
        .ifPresent(
            org -> {
              serviceHours.detachCampusOrg(org);
              campusOrgs.delete(org);
            });
  }

  @Transactional
  public void deleteCommunityAgency(Long id) {
    agencies
        .findById(id)
        .ifPresent(
            agency -> {
              serviceHours.detachCommunityAgency(agency);
              agencies.delete(agency);
            });
  }
}
