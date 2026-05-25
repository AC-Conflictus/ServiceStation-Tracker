package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification
import sstation.*

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(CommOrgController)
@Mock([CommAg,ServiceHour])
class CommOrgControllerSpec extends Specification {

	def setup() {
	}

	def cleanup() {
	}

	void "test that if an agency is saved(_saveOnCard) successfully the save view will be rendered"() {
		when:
		def ag=Mock(CommAg)
		controller._saveOnCard(ag)

		then:
		view=="/commOrg/agForm"
	}

	void "test that if an agency is not saved(_saveOnCard) successfully the form view will be rendered"(){
		when:
		def ag=Mock(CommAg)
		ag.contactEmail="aaaa"
		controller._saveOnCard(ag)

		then:
		view=="/commOrg/agForm"
		model.form==1
		model.card==1
		model.agency==ag
	}

	void "test that if an agency is saved(_saveOnTable) successfully the save view will be rendered"() {
		when:
		def ag=Mock(CommAg)
		controller._saveOnCard(ag)

		then:
		view=="/commOrg/agForm"
	}

	void "test that if an agency is not saved(_saveOnTable) successfully the form view will be rendered"(){
		when:
		def ag=Mock(CommAg)
		ag.contactEmail="aaaa"
		controller._saveOnCard(ag)

		then:
		view=="/commOrg/agForm"
		model.form==1
	}

	void "delete an agency on table"(){
		when:
		def ag=Mock(CommAg)
		ag.save(flush:true)
		controller._deleteOnTable(ag)

		then:
		CommAg.count()==0
		view=="/commOrg/_agTable"
	}

	void "delete an agency on Card"(){
		when:
		def ag=Mock(CommAg)
		ag.save(flush:true)
		controller._deleteOnCard(ag)

		then:
		CommAg.count()==0
		view=="/commOrg/_agCard"
	}
}
