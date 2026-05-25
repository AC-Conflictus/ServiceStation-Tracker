package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification
import sstation.CommAg;

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(CommAg)
class CommAgSpec extends Specification {

	def setup() {
	}

	def cleanup() {
	}

	def "test CampusOrg all constraints #field are #error"() {
		when:
		def s=new CommAg("$field":error)
		then:
		!s.validate()
		where:

		field            | error
		'name'           | ""
		'name'           | null
		'description'    | ""
		'description'    | null
		'contact'        | null
		'contact'        | ""
		'contactPhone'   | null
		'contactPhone'   | ""
		'contactEmail'   | null
		'contactEmail'   | ""
		'contactEmail'   | "not valid email form"
		'address'        | ""
		'address'        | null


	}
}
