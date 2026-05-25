package sstation

import java.util.Date;

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(StationReportService)
@Mock([AcStudent,ServiceHour,CampusOrg,Event,CommAg])
class StationReportServiceSpec extends Specification {

	AcStudent s
	def orgList=[]
	def eventList=[]
	def agList=[]
	def setup() {

		agNameList.each{
			agList.add(randomAg(it))
		}


		orgList.add(randomOrg("Service Station"))
		orgList.add(randomOrg("Habitat of Humanity"))
		orgList.add(randomOrg("AC Church"))
		orgList.add(randomOrg("THINK"))
		orgList.add(randomOrg("BIG"))
		orgList.add(randomOrg("APO"))
		orgList.add(randomOrg("N/A"))


		eventList.add(randomEvent("N/A"))
		eventList.add(randomEvent("Great Day of Service"))
		eventList.add(randomEvent("First We Serve"))
		eventList.add(randomEvent("GreenServe"))
		eventList.add(randomEvent("RooBound"))
		eventList.add(randomEvent("JanServe"))
		eventList.add(randomEvent("Austin College Tutoring(ACT"))
		eventList.add(randomEvent("Alternative Spring Break"))
		eventList.add(randomEvent("Other"))

		s=randomStudent()
		//Generate random service hours for the current student

	}

	def cleanup() {
	}

	void "test hourKPIbyYear(int year) with one ServiceHour stored in the database"() {
		when:
		ServiceHour sh=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:3.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh.starttime=new Date()
		sh.lastmodified=new Date()
		s.addToServiceHours(sh).save(failOnError:true,flush:true)
		def map=service.hourKPIbyYear(2016)
		then:
		map.year==2016
		map.total==3.0
	}

	void "test hourKPIbyYear(int year) with more data in 2016"(){
		when:
		ServiceHour sh=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:3.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh.starttime=new Date(2015,2,3,3,3,0)
		sh.lastmodified=new Date()
		s.addToServiceHours(sh).save(failOnError:true,flush:true)
		ServiceHour sh2=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.PENDING,duration:3.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh2.starttime=new Date()
		sh2.lastmodified=new Date()
		s.addToServiceHours(sh2).save(failOnError:true,flush:true)
		ServiceHour sh3=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:4.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh3.starttime=new Date()
		sh3.lastmodified=new Date()
		s.addToServiceHours(sh3).save(failOnError:true,flush:true)
		ServiceHour sh4=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.REJECTED,duration:3.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh4.starttime=new Date()
		sh4.lastmodified=new Date()
		s.addToServiceHours(sh4).save(failOnError:true,flush:true)
		ServiceHour sh5=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:5.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh5.starttime=new Date()
		sh5.lastmodified=new Date()
		s.addToServiceHours(sh5).save(failOnError:true,flush:true)

		def map=service.hourKPIbyYear(2016)

		then:
		map.year==2016
		map.total==9.0
		map.avgByStudent==9.0
		map.avgByEvent==((double)(9.0/9)).round(2)
		map.avgByCommOrg==((double)(9.0)/7).round(2)
		map.avgByGroup==((double)(9.0/7.0)).round(2)
	}

	void "test hourKPIbyEvent with one ServiceHour stored in the database"() {
		when:
		ServiceHour sh=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:3.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh.starttime=new Date()
		sh.lastmodified=new Date()
		s.addToServiceHours(sh).save(failOnError:true,flush:true)
		def map=service.hourKPIbyEvent(eventList[3])
		then:
		map.event==eventList[3].id
		map.total==3.0
	}

	void "test hourKPIbyEvent with more data, including non-approved service hours"(){
		when:
		// doesn't count, event ins't what we're looking for
		ServiceHour sh=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:3.0,campusOrg:orgList[2],event:eventList[5],commAg:agList[3])
		sh.starttime=new Date()
		sh.lastmodified=new Date()
		s.addToServiceHours(sh).save(failOnError:true,flush:true)
		// doesn't count, pending
		ServiceHour sh2=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.PENDING,duration:3.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh2.starttime=new Date()
		sh2.lastmodified=new Date()
		s.addToServiceHours(sh2).save(failOnError:true,flush:true)
		ServiceHour sh3=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:4.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh3.starttime=new Date()
		sh3.lastmodified=new Date()
		s.addToServiceHours(sh3).save(failOnError:true,flush:true)
		// doesn't count, rejected
		ServiceHour sh4=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.REJECTED,duration:3.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh4.starttime=new Date()
		sh4.lastmodified=new Date()
		s.addToServiceHours(sh4).save(failOnError:true,flush:true)
		ServiceHour sh5=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:5.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh5.starttime=new Date()
		sh5.lastmodified=new Date()
		s.addToServiceHours(sh5).save(failOnError:true,flush:true)

		def map=service.hourKPIbyEvent(eventList[3])

		then:
		map.name.equals(eventList[3].name)
		map.event==eventList[3].id
		map.total==9.0
		map.avgByStudent==9.0
		map.avgByCommOrg==((double)(9.0)/7).round(2)
		map.avgByGroup==((double)(9.0/7.0)).round(2)
	}


	void "test hourKPIinFiveYears"(){
		when:
		ServiceHour sh=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:2.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh.starttime=new Date(2015,2,3,3,3,0)
		sh.lastmodified=new Date()
		s.addToServiceHours(sh).save(failOnError:true,flush:true)
		ServiceHour sh2=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED, duration:3.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh2.starttime=new Date(2014,2,3,3,3,0)
		sh2.lastmodified=new Date()
		s.addToServiceHours(sh2).save(failOnError:true,flush:true)
		ServiceHour sh3=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:4.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh3.starttime=new Date()
		sh3.lastmodified=new Date()
		s.addToServiceHours(sh3).save(failOnError:true,flush:true)
		ServiceHour sh4=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:3.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh4.starttime=new Date(2013,2,3,3,3,0)
		sh4.lastmodified=new Date()
		s.addToServiceHours(sh4).save(failOnError:true,flush:true)
		ServiceHour sh5=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:10.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh5.lastmodified=new Date()
		sh5.starttime=new Date(2012,2,3,3,3,0)
		s.addToServiceHours(sh5).save(failOnError:true,flush:true)

		def list=service.hourKPIInFiveYear()
		then:
		list.size()==5
		list[0]['year']==2016
		list[0]['total']==4.0
		list[0]['avgByEvent']==((double)(4.0/9)).round(2)
	}

	void "test hourKPIinFiveYears with pending case"(){
		when:
		ServiceHour sh=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:2.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh.starttime=new Date(2015,2,3,3,3,0)
		sh.lastmodified=new Date()
		s.addToServiceHours(sh).save(failOnError:true,flush:true)
		ServiceHour sh2=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED, duration:3.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh2.starttime=new Date(2014,2,3,3,3,0)
		sh2.lastmodified=new Date()
		s.addToServiceHours(sh2).save(failOnError:true,flush:true)
		ServiceHour sh3=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:4.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh3.starttime=new Date()
		sh3.lastmodified=new Date()
		s.addToServiceHours(sh3).save(failOnError:true,flush:true)
		ServiceHour sh4=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:3.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh4.starttime=new Date(2013,2,3,3,3,0)
		sh4.lastmodified=new Date()
		s.addToServiceHours(sh4).save(failOnError:true,flush:true)
		ServiceHour sh5=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.APPROVED,duration:10.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh5.lastmodified=new Date()
		sh5.starttime=new Date(2012,2,3,3,3,0)
		s.addToServiceHours(sh5).save(failOnError:true,flush:true)
		ServiceHour sh6=new ServiceHour(description:"a",eventContactName:"a",eventContactPhone:"a",eventContactEmail:"a@g.com", status:Status.PENDING,duration:10.0,campusOrg:orgList[2],event:eventList[3],commAg:agList[3])
		sh6.lastmodified=new Date()
		sh6.starttime=new Date()
		s.addToServiceHours(sh6).save(failOnError:true,flush:true)

		def list=service.hourKPIInFiveYear()
		then:
		list.size()==5
		list[0]['year']==2016
		list[0]['total']==4.0
		list[0]['avgByEvent']==((double)(4.0/9)).round(2)
	}






	/*
	 * Create testing data
	 */
	def random=new Random()
	//lists used to generate random names
	def fn=[
		'John',
		'Mary',
		'Mike',
		'Jenny',
		'Aaron',
		'Priya',
		'Marrisa',
		'Brittany'
	]
	def ln=("Higgs,Block,Gorman,Countryman,Bortan,Mattew,Frindly").split(",")

	//community agency names list
	def agNameList=[
		'N/A',
		'Texas Community Center',
		'Crisis Center',
		'Big Brothers and Sisters',
		'Sherman Elementary School',
		'Charity Group',
		"Sherman Church"
	]

	/**
	 * Generate a random AcStudent
	 * @return
	 */
	private AcStudent randomStudent(){
		String fname=fn[random.nextInt(fn.size())]
		String lname=ln[random.nextInt(ln.size())]

		String id="AC${random.nextInt(10000)+50000}";
		int year=random.nextInt(4)+2012
		Classification clas;
		switch(year-2011){
			case 1:
				clas=Classification.FR
				break
			case 2:
				clas=Classification.SO
				break
			case 3:
				clas=Classification.JR
				break
			case 4:
				clas=Classification.SR
				break
			default:
				clas=Classification.OTHER
		}
		char stat = 'A'

		String box="${random.nextInt(30000)+30000}"
		String email=fname.substring(0,1)+lname+(year-2000)+"@austincollege.edu"

		return new AcStudent(isModerator:false,firstname:fname,lastname:lname,status:stat,acid:id,acEmail:email,acBox:box,acYear:year,classification:clas,phone:"903820784")
	}

	//	/**
	//	 * Generate a random service hour
	//	 * @return
	//	 */
	//	private ServiceHour randomSH(CampusOrg org,Event event,CommAg ag){
	//
	//		String status
	//		switch(random.nextInt(3)){
	//			case 0:
	//				status=Status.PENDING
	//				break
	//			case 1:
	//				status=Status.APPROVED
	//				break
	//			case 2:
	//				status=Status.REJECTED
	//				break
	//			default:
	//				status=Status.PENDING
	//		}
	//
	//		double duration=(double)random.nextInt(4)+(double)(1+random.nextInt(4))/4.0
	//		def first=fn[random.nextInt(fn.size())]
	//		def last=ln[random.nextInt(ln.size())]
	//		def name=first+" "+last
	//		def email=first.substring(0,1)+last+"@austincollege.edu"
	//
	//
	//		def year=111+random.nextInt(5)
	//		def month=1+random.nextInt(12)
	//		def date=1+random.nextInt(28)
	//		def hour=8+random.nextInt(10)
	//		def min=random.nextInt(60)
	//		def starttime=new Date(year,month,date,hour,min,0)
	//		def mod=new Date()
	//
	//
	//		ServiceHour sh=new ServiceHour(event:event,campusOrg:org,description:"service",status:status,commAg:ag,eventContactName:name,eventContactPhone:"9048937894",eventContactEmail:email,duration:duration,starttime:starttime,lastmodified:mod)
	//		return sh
	//	}


	/**
	 * Generate a random organization
	 * @param orgName  the name of the organization
	 * @return
	 */
	private CampusOrg randomOrg(String orgName){
		def first=fn[random.nextInt(fn.size())]
		def last=ln[random.nextInt(ln.size())]
		def name=first+" "+last
		def email=first.substring(0,1)+last+"@austincollege.edu"
		String contactPhone="9089038898"
		CampusOrg org=new CampusOrg(name:orgName,description:orgName,contact:name,contactPhone:contactPhone,contactEmail:email)
		org.save(failOnError:true,flush:true)
		return org
	}

	/**
	 * Generate a random event
	 * @param eventName the name of the event
	 * @return
	 */
	private Event randomEvent(String eventName){
		Event e=new Event(name:eventName)
		e.description="Contact the Service Station office to sign up! "

		int count=3

		def first=fn[random.nextInt(fn.size())]
		def last=ln[random.nextInt(ln.size())]
		def name=first+" "+last

		e.contact=name
		e.contactPhone="9089038898"
		e.contactEmail=first.substring(0,1)+last+"@austincollege.edu"

		e.save(flush:true,failOnError:true)
		return e;
	}

	/**
	 * Generate a random community agency
	 * @param agName  the name of the agency
	 * @return
	 */
	private CommAg randomAg(String agName){
		def first=fn[random.nextInt(fn.size())]
		def last=ln[random.nextInt(ln.size())]
		def name=first+" "+last
		def email=first.substring(0,1)+last+"@austincollege.edu"
		String contactPhone="9089038898"
		String address="900N. Grand Ave., Sherman, TX"
		CommAg ag=new CommAg(address:address,name:agName,description:agName,contact:name,contactPhone:contactPhone,contactEmail:email)
		ag.save(failOnError:true,flush:true)
		return ag
	}
}
