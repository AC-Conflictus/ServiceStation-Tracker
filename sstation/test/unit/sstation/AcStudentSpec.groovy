package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification
import sstation.AcStudent;

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 * References: 
 * 1.Testing Domain Constraints Using Grails 2.x & Spock 0.7
 * Received from: http://www.christianoestreich.com/2012/11/domain-constraints-grails-spock-updated/
 */
@TestFor(AcStudent)
class AcStudentSpec extends Specification {
    def "test AcStudent all constraints #field are #error"() {	
		when:
		def s=new AcStudent("$field":error)
		then:
		!s.validate()
		where:	
		
		field       | error
		'firstname' | ""
		'firstname' | null
		'lastname'  | ""
		'lastname'  | null
		'acid'      | null
		'acid'      | ""
		'acBox'     | null
		'acBox'     | ""
		'acEmail'   | null
		'acEmail'   | ""
		'acEmail'   | "not valid email form"
		'classification'  | ""
		'classification'  |null
		
    }
	

}
