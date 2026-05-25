package sstation

import grails.test.mixin.TestFor
import spock.lang.Specification
import sstation.HourController;


/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(HourController)
@Mock([ServiceHour,CommAg,CampusOrg,Event,AcStudent,HourService])
class HourControllerSpec extends Specification {

    def setup() {
    }

    void "list with no service hours should be empty in overall method"(){
		when:"executing list with no service hours"
		controller.overall()
		then:"No service hours will be in the list"
		view=="/hour/overall"
		model.list.size()==0
	}
	
	void "list with 10 service hours should have 10 things in it in overall method"(){
		when:"There are 10 ServiceHours"
		AcStudent s = new AcStudent(isModerator:false,firstname:"a",lastname:"b",acid:"id010",acEmail:"a@g.com",acBox:"20",status:"a",acYear:2013,classification:Classification.FR,phone:"903820784")
		10.times{
			def party = new Event(name:"party",description:"party",contact:"Patty Plainer",contactPhone:"4242424242",contactEmail:"parties@partyplace.com").save(flush:true,failOnError:true)
			def agency = new CommAg(name:"Bond",address:"Baker St.",description:"agency",contact:"James",contactPhone:"classified",contactEmail:"agency@mail.com").save(flush:true,failOnError:true)
			def org = new CampusOrg(name:"People",description:"living",contact:"Dude",contactPhone:"number",contactEmail:"person@place.com").save(flush:true,failOnError:true)
			
			Date d=new Date()
			def sh=new ServiceHour(lastmodified:d,starttime:d,description:"a",eventContactName:"Scrooge",eventContactPhone:"7777777777",eventContactEmail:"mcduck@money.com",event:party,commAg:agency,campusOrg:org,status:Status.PENDING, duration:77)
			s.addToServiceHours(sh).save(flush:true,failOnError:true)
			s.save(flush:true,failOnError:true)
			}
		controller.overall()
		then:"10 ServiceHours should be in the list"
		view=="/hour/overall"
		model.list.size()==10
	}
	 
	void "test for pending action with no service hour in the list"(){
		when:"excuting pending() with no service hour in the list"
		controller.pending()
		
		then:"No service hours will be in the list"
		view == "/hour/pending"
		model.list.size()==0
	}
	
	
	void "test for pending action with 10 service hour in the list"(){
		when:"excuting pending() with 10 service hour in the list"
		AcStudent s = new AcStudent(isModerator:false,firstname:"a",lastname:"b",acid:"id010",acEmail:"a@g.com",acBox:"20",status:"a",acYear:2013,classification:Classification.FR,phone:"903820784")
		10.times{
			def party = new Event(name:"party",description:"party",contact:"Patty Plainer",contactPhone:"4242424242",contactEmail:"parties@partyplace.com").save(flush:true,failOnError:true)
			def agency = new CommAg(name:"Bond",address:"Baker St.",description:"agency",contact:"James",contactPhone:"classified",contactEmail:"agency@mail.com").save(flush:true,failOnError:true)
			def org = new CampusOrg(name:"People",description:"living",contact:"Dude",contactPhone:"number",contactEmail:"person@place.com").save(flush:true,failOnError:true)
			
			Date d=new Date()
			def sh=new ServiceHour(lastmodified:d,starttime:d,description:"a",eventContactName:"Scrooge",eventContactPhone:"7777777777",eventContactEmail:"mcduck@money.com",event:party,commAg:agency,campusOrg:org,status:Status.PENDING, duration:77)
			s.addToServiceHours(sh).save(flush:true,failOnError:true)
			s.save(flush:true,failOnError:true)
			}
		controller.pending()
		then:"10 ServiceHours should be in the list"
		view=="/hour/pending"
		model.list.size()==10
	}
	
	void "test sTable which leads to the hour list with no service hours in it"(){
		when:"executing _totalHList with no Hours"
		controller._totalHList()
		then:"No hour will be in the list"
		view=="/hour/_totalHList"
		model.list.size()==0
	}
	
	void "test sTable which leads to the hour list with 10 service hours in it"(){
		when:"executing _totalHlist with 10 hours"
		AcStudent s = new AcStudent(isModerator:false,firstname:"a",lastname:"b",acid:"id000",acEmail:"a@g.com",acBox:"20",status:"a", acYear:2013,classification:Classification.FR,phone:"903820784")
		10.times{
			def party = new Event(name:"party",description:"party",contact:"Patty Plainer",contactPhone:"4242424242",contactEmail:"parties@partyplace.com").save(flush:true,failOnError:true)
			def agency = new CommAg(name:"Bond",address:"Baker St.",description:"agency",contact:"James",contactPhone:"classified",contactEmail:"agency@mail.com").save(flush:true,failOnError:true)
			def org = new CampusOrg(name:"People",description:"living",contact:"Dude",contactPhone:"number",contactEmail:"person@place.com").save(flush:true,failOnError:true)
			
			Date d=new Date()
			def sh=new ServiceHour(lastmodified:d,starttime:d,description:"a",eventContactName:"Scrooge",eventContactPhone:"7777777777",eventContactEmail:"mcduck@money.com",event:party,commAg:agency,campusOrg:org,status:Status.PENDING, duration:77)
			s.addToServiceHours(sh).save(flush:true,failOnError:true)
			s.save(flush:true,failOnError:true)
			}
		controller._totalHList()
		then:"10 ServiceHours will be shown in the list"
		view=="/hour/_totalHList"
		model.list.size()==10
		model.list[0].description == "a"
	}
	
	void "test for hourService is not null in _hourkpi"(){
		when:"executing _hourkpi"
		controller.hourService = Mock(HourService)
		controller._hourkpi()
		then:"hourService is not null"
		view=="/hour/_hourkpi"
		controller.hourService != null		
	}
	
	void "test sh not null in shour method"(){
		when:"executing shour"
		def sh = Mock(ServiceHour)
		controller.shour(sh)
		then:"sh is not null"
		view=="/hour/shour"
		model.shour != null
	}
	
	void "test for _editShours to confirm lists are empty"(){
		when:"executing..."
		def sh = Mock(ServiceHour)
		controller._editShour(sh)
		
		then:"lists should be empty"
		model.shour == sh
		model.eventList.size() == 0
		model.orgList.size() == 0
		model.agList.size() == 0
		view == "/hour/_editShour"
	}
	
	void "test for _createShour to confirm lists are empty"(){
		when:"executing..."
		controller._createShour()
		
		then:"lists should be empty"
		model.eventList.size() == 0
		model.orgList.size() == 0
		model.fullOrgList.size() == 0
		model.agList.size() == 0
		model.list.size() == 0
		model.nameList.size() == 0
		view == "/hour/_createShour"
	}
	
	void "saveShour works test"(){
		when:"executing..."
		def s = new AcStudent(isModerator:false,firstname:"a",lastname:"b",acid:"id007",status:"a",acEmail:"a@g.com",acBox:"20",acYear:2013,classification:Classification.FR,phone:"903820784").save(flush:true,failOnError:true)
		def party = new Event(name:"party",description:"party",contact:"Patty Plainer",contactPhone:"4242424242",contactEmail:"parties@partyplace.com").save(flush:true,failOnError:true)
		def agency = new CommAg(name:"Bond",address:"Baker St.",description:"agency",contact:"James",contactPhone:"classified",contactEmail:"agency@mail.com").save(flush:true,failOnError:true)
		def org = new CampusOrg(name:"People",description:"living",contact:"Dude",contactPhone:"number",contactEmail:"person@place.com").save(flush:true,failOnError:true)
		def sh=new ServiceHour()
		params.startdate = "07/07/2016"
		params.starthour = "07"
		params.startmin = "07"
		params.description = "a"
		params.eventContactName = "Scrooge"
		params.eventContactPhone = "7777777777"
		params.eventContactEmail = "mcduck@money.com"
		params.duration = "77"
		params.shOrg = "People"
		params.shEvent = "party"
		params.shCommAg = "Bond"
		params.status = "PENDING"
		params.studentIdHolder = "1"
		
		controller.saveShour(sh)
		
		then:"it should return sh value"
		view == "/hour/shour"
		model.shour.description == "a"
	}
	
	void "updateShour works test"(){
		when:"executing..."
		def s = new AcStudent(isModerator:false,firstname:"a",lastname:"b",acid:"id007",status:"a",acEmail:"a@g.com",acBox:"20",acYear:2013,classification:Classification.FR,phone:"903820784").save(flush:true,failOnError:true)
		def party = new Event(name:"party",description:"party",contact:"Patty Plainer",contactPhone:"4242424242",contactEmail:"parties@partyplace.com").save(flush:true,failOnError:true)
		def agency = new CommAg(name:"Bond",address:"Baker St.",description:"agency",contact:"James",contactPhone:"classified",contactEmail:"agency@mail.com").save(flush:true,failOnError:true)
		def org = new CampusOrg(name:"People",description:"living",contact:"Dude",contactPhone:"number",contactEmail:"person@place.com").save(flush:true,failOnError:true)
		def sh=new ServiceHour()
		s.addToServiceHours(sh)
		params.startdate = "07/07/2016"
		params.starthour = "07"
		params.startmin = "07"
		params.description = "a"
		params.eventContactName = "Scrooge"
		params.eventContactPhone = "7777777777"
		params.eventContactEmail = "mcduck@money.com"
		params.duration = "77"
		params.shOrg = "People"
		params.shEvent = "party"
		params.shCommAg = "Bond"
		params.status = "PENDING"
		
		controller.updateShour(sh)
		
		then:"it should return sh value"
		view == "/hour/shour"
		model.shour.description == "a"
	}
	
	void "test for deleteShour"(){
		when:"There is 1 ServiceHours"
		AcStudent s = new AcStudent(isModerator:false,firstname:"a",lastname:"b",acid:"id010",acEmail:"a@g.com",acBox:"20",status:"a",acYear:2013,classification:Classification.FR,phone:"903820784")
		def party = new Event(name:"party",description:"party",contact:"Patty Plainer",contactPhone:"4242424242",contactEmail:"parties@partyplace.com").save(flush:true,failOnError:true)
		def agency = new CommAg(name:"Bond",address:"Baker St.",description:"agency",contact:"James",contactPhone:"classified",contactEmail:"agency@mail.com").save(flush:true,failOnError:true)
		def org = new CampusOrg(name:"People",description:"living",contact:"Dude",contactPhone:"number",contactEmail:"person@place.com").save(flush:true,failOnError:true)
		Date d=new Date()
		def sh=new ServiceHour(lastmodified:d,starttime:d,description:"a",eventContactName:"Scrooge",eventContactPhone:"7777777777",eventContactEmail:"mcduck@money.com",event:party,commAg:agency,campusOrg:org,status:Status.PENDING, duration:77)
		s.addToServiceHours(sh).save(flush:true,failOnError:true)
		controller.deleteShour(sh)
		
		then:"0 ServiceHours should be in the list"
		view=="/hour/_totalHList"
		model.list.size()==0
	}
	
}
