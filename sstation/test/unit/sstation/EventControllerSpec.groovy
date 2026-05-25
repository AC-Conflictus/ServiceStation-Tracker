package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification
import sstation.EventController;

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(EventController)
@Mock([Event,ServiceHour])
class EventControllerSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }
//
//    void "test that if an event is saved(_saveEvent) successfully the table view will be rendered"() {
//		when:
//		def ev = new Event(name: "festival", description: "something", contact: "person", contactPhone: "666", contactEmail: "here@ac.edu")
//		controller._saveEvent(ev)
//		
//		then:
//		view=="/event/eventMain"
//    }
//	
//	
//	void "test that if an event is not saved(_saveEvent) successfully the form view will be rendered"(){
//		when:
//		def ev=Mock(Event)
//		controller._saveEvent(ev)
//		
//		then:
//		view=="/event/_eventForm"
//	}
//	
//	void "delete an event on table"(){
//		when:
//		def ev = new Event(name: "festival", description: "something", contact: "person", contactPhone: "666", contactEmail: "here@ac.edu")
//		controller._saveEvent(ev)
//		controller._deleteEventTable(ev)
//		
//		then:
//		Event.count()==0
//		view=="/event/_eventTableView"
//	}
}
