package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ReportsController)
@Mock([ServiceHour,StationReportService,Event,CommAg,CampusOrg,AcStudent])
class ReportsControllerSpec extends Specification {

	def setup() {
	}

	def cleanup() {
	}

	void "test yearReport"() {
		when:
		controller.stationReportService=Mock(StationReportService)
		controller.yearReport()
		then:
		view=="/reports/yearReport"

	}

	void "test _hoursByYear"() {
		when:
		params.year="2016"
		controller._hoursByYear()
		then:
		view=="/reports/_hoursByYear"
	}


	void "test eventReport"() {
		when:
		controller.stationReportService=Mock(StationReportService)
		controller.eventReport()
		
		then:
		view=="/reports/eventReport"
	}
	
	void "summaryReport is bounds-safe when fewer than 5 agencies/orgs/events exist (TC-003)"() {
		given: "only 2 of each entity, so the old constant=5 loop would have thrown"
		2.times { int i ->
			new CommAg(address:"a", name:"Ag$i", description:"d", contact:"c", contactPhone:"1", contactEmail:"c@a.edu").save(flush:true, failOnError:true)
			new CampusOrg(name:"Org$i", description:"d", contact:"c", contactPhone:"1", contactEmail:"c@a.edu").save(flush:true, failOnError:true)
			randomEvent("Ev$i")
		}

		when:
		controller.summaryReport()

		then:
		notThrown(IndexOutOfBoundsException)
		view == "/reports/summaryReport"
		model.constant == 2
	}

	void "semesterReport tolerates a ServiceHour with a null commAg (TC-006)"() {
		given: "one of each entity so the top-N loop runs, plus an hour with no community agency"
		def ag = new CommAg(address:"a", name:"Crisis Center", description:"d", contact:"c", contactPhone:"1", contactEmail:"c@a.edu").save(flush:true, failOnError:true)
		def org = new CampusOrg(name:"THINK", description:"d", contact:"c", contactPhone:"1", contactEmail:"c@a.edu").save(flush:true, failOnError:true)
		def ev = randomEvent("Great Day of Service")
		def student = new AcStudent(isModerator:false, firstname:"Test", lastname:"Student", status:('A' as char), acid:"AC99999", acEmail:"student@austincollege.edu", acBox:"1", acYear:2026, classification:Classification.SR, phone:"1").save(flush:true, failOnError:true)

		def cal = Calendar.getInstance()
		cal.set(Calendar.MONTH, Calendar.MARCH)
		def sh = new ServiceHour(event:ev, campusOrg:org, description:"s", status:Status.APPROVED, commAg:null, duration:2.0, starttime:cal.getTime(), lastmodified:new Date())
		student.addToServiceHours(sh).save(flush:true, failOnError:true)

		when:
		params.yearComboBox = String.valueOf(cal.get(Calendar.YEAR))
		params.semesterComboBox = "Spring"
		controller.semesterReport()

		then:
		notThrown(NullPointerException)
		view == "/reports/semesterReport"
	}

	private Event randomEvent(String eventName){
		Event e=new Event(name:eventName)
		e.description="Contact the Service Station office to sign up! "

		int count=3

		def first="qwer"
		def last="asdf"
		def name=first+" "+last

		e.contact=name
		e.contactPhone="9089038898"
		e.contactEmail=first.substring(0,1)+last+"@austincollege.edu"

		e.save(flush:true,failOnError:true)
		return e;
	}
	
}
