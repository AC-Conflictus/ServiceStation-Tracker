package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification
import sstation.Event;

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(Event)
class EventSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test ServiceHour all constraints #field are #error"(){
		when:
		def s=new Event("$field":error)
		then:
		!s.validate()
		where:
		
		field       | error
		'name'    | null
		'name'    | ""
		'description'   | ""
		'description'  | null
		
	}
}
