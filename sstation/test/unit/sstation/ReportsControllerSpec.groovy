package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ReportsController)
@Mock([ServiceHour,StationReportService,Event])
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
