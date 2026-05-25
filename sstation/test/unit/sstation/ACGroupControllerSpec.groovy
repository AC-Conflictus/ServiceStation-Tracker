package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ACGroupController)
@Mock([CampusOrg,ServiceHour])
class ACGroupControllerSpec extends Specification {

	void "test when successful save and save view shown for the card"() {

		when:
		def co=Mock(CampusOrg)
		controller._saveOnCard(co)

		then:
		view=="/ACGroup/orgForm"
		model.card==1
	}

	void "test when not saved successfully and form view will be shown for the card"(){
		when:
		def co=Mock(CampusOrg)
		co.contactPhone="aaaa"
		controller._saveOnCard(co)

		then:
		view=="/ACGroup/orgForm"
		model.form==1
		model.card==1
	}

	void "test when successful save and save view shown for the table"() {

		when:
		def co=Mock(CampusOrg)
		controller._saveOnTable(co)

		then:
		view=="/ACGroup/orgForm"
		model.table==1
	}

	void "test when not saved successfully and form view will be shown for the table" (){
		when:
		def co=Mock(CampusOrg)
		co.contactEmail="aaaa"
		controller._saveOnTable(co)

		then:
		view=="/ACGroup/orgForm"
		model.form==1
		model.table==1
	}
	
	void "delete an agency on table"(){
		when:
		def org=Mock(CampusOrg)
		org.save(flush:true)
		controller._deleteOnTable(org)

		then:
		CampusOrg.count()==0
		view=="/ACGroup/_orgTable"
	}

	void "delete an agency on Card"(){
		when:
		def org=Mock(CampusOrg)
		org.save(flush:true)
		controller._deleteOnCard(org)

		then:
		CampusOrg.count()==0
		view=="/ACGroup/_orgCardMain"
	}
}
