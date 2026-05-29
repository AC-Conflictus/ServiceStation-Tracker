package sstation

import grails.test.mixin.TestFor
import grails.test.mixin.Mock
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(HourService)
@Mock([ServiceHour, AcStudent])
class HourServiceSpec extends Specification {

	def setup() {
	}

	def cleanup() {
	}

	void "init derives currentYear from the system clock, not a hardcoded value"() {
		when:
		// init() is private but Groovy lets us drive it directly; with no
		// ServiceHour rows it sets currentYear without touching the rest of the graph.
		service.init()

		then:
		service.currentYear == Calendar.getInstance().get(Calendar.YEAR)
		service.currentYear != 2015
	}
}
