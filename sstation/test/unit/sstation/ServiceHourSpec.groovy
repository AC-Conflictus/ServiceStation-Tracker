
package sstation;
import grails.test.mixin.TestFor
import spock.lang.Specification
import sstation.ServiceHour;

@TestFor(ServiceHour)
class ServiceHourSpec extends Specification{
	
	void "test ServiceHour all constraints #field are #error"(){
		when:
		def s=new ServiceHour("$field":error)
		then:
		!s.validate()
		where:
		
		field       | error
		'status'    | null
		'status'    | ""
		'eventContactEmail'   | "not valid email form"
		'duration'  | null

	}

}