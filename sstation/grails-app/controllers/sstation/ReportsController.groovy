package sstation

import grails.transaction.Transactional
import java.security.MessageDigest
import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_ADMIN','ROLE_MODERATOR'])
@Transactional(readOnly = false)
class ReportsController {
	def stationReportService
	def constant = 5

    def index() { 
		def orgList=CommAg.list()
		def campusList =CampusOrg.list()
		def semesterList = ["Fall", "Janterm", "Spring", "Summer"]
		
		int currentYear=new Date().getAt(Calendar.YEAR)
		def yearList = []
		for(int a=currentYear; a > currentYear-5; a--){
			yearList.add(a)
		}
		[orgList:orgList, semesterList:semesterList, yearList:yearList,campusList:campusList]
	}

	
	def eventReport() {
		def listEvent = Event.list()
		def li = [:]
		listEvent.each{
			def keyName=stationReportService.hourKPIbyEvent(it)['name']
			li.put(keyName,stationReportService.hourKPIbyEvent(it)['total'])
		}
		println li.size
		render view:"eventReport", model:[li:li]
	}

	
	def _hoursByEvent() {
		int eventId = Integer.parseInt(params.event)
		def aList=ServiceHour.findAllByStatus(Status.APPROVED);
		def list=aList.findAll{
			it.event.id==eventId
		}
		render view:"_hoursByEvent",model:[list:list]
	}
	
	def summaryReport(){
		// Default to the current year; allow ?year=YYYY to view past years (TC-002).
		def year = params.year ? (params.year as Integer) : new Date().getAt(Calendar.YEAR)
		def totalHours = 0;


		def allHours = ServiceHour.list();
		def allAgs = CommAg.list();
		def allOrgs = CampusOrg.list();
		def allEvs = Event.list();

		// Bound the top-N loop by the shortest list so allAgs/allOrgs/allEvs.get(i)
		// never runs past the end of any of them when seed/real data has < 5 (TC-003).
		constant = [5, allAgs.size(), allOrgs.size(), allEvs.size()].min();

		def topAgs = [];
		def topEvs = [];
		def topOrgs = [];

		def agHours = [];
		def evHours = [];
		def orgHours = [];
		for (int i = 0; i<constant; i++){
			
			topAgs.add(allAgs.get(i));
			topEvs.add(allEvs.get(i));
			topOrgs.add(allOrgs.get(i));
			
			CommAgReportService agService = new CommAgReportService(allAgs.get(i), allHours);
			CampusOrgReportService orgService = new CampusOrgReportService(allOrgs.get(i), allHours);
			EventReportService evService = new EventReportService(allEvs.get(i), allHours);
			
			agHours.add(agService.getTotalHourByYear(year))
			evHours.add(evService.getTotalHourByYear(year))
			orgHours.add(orgService.getTotalHourByYear(year))
		}
		for (int i = 0; i<allHours.size;i++){
			def Calendar cal = Calendar.getInstance()
			cal.setTime(allHours.get(i).starttime)
			if(cal.get(Calendar.YEAR) == year){
				totalHours += allHours.get(i).duration
			}
		}

		// Year options for the selector: current year and the previous four.
		int currentYear = new Date().getAt(Calendar.YEAR)
		def yearList = []
		for (int a = currentYear; a > currentYear - 5; a--){
			yearList.add(a)
		}

		render view:"summaryReport", model: [totalHours:totalHours ,constant:constant, topAgs:topAgs, allHours:allHours, topOrgs:topOrgs, topEvs:topEvs, agHours:agHours, evHours:evHours, orgHours:orgHours, year:year, yearList:yearList]



	}
	
	def semesterReport() {
		def semesterValue = params.semesterComboBox
		
		def yearValue = params.yearComboBox
		println yearValue +" "+ semesterValue
		def list = []
		def servList = ServiceHour.list()
		def yearList = servList.findAll{
			it.starttime.getAt(Calendar.YEAR)==Integer.parseInt(yearValue)
		}
		
		switch (semesterValue){
			case "Fall": list=yearList.findAll{ it.starttime.getAt(Calendar.MONTH)>=8&&it.starttime.getAt(Calendar.MONTH)<=11 }
			break;
			case "Janterm": list=yearList.findAll{ it.starttime.getAt(Calendar.MONTH)==0 }
			break;
			case "Spring": list=yearList.findAll{it.starttime.getAt(Calendar.MONTH)>=1&&it.starttime.getAt(Calendar.MONTH)<=4 }
			break;
			case "Summer": list=yearList.findAll{ it.starttime.getAt(Calendar.MONTH)>=5&&it.starttime.getAt(Calendar.MONTH)<=7 }
			break;
		}
		println list.size()
		def totalHours = 0
		for(int a = 0; a < list.size(); a++){
			totalHours += list.get(a).duration
		}
		
		def allHours = ServiceHour.list();
		def allAgs = CommAg.list();
		def allOrgs = CampusOrg.list();
		def allEvs = Event.list();
		
		def topAgs = [];
		def topEvs = [];
		def topOrgs = [];
		
		def agHours = [];
		def evHours = [];
		def orgHours = [];
		// Bound by the shortest list so .get(i) is always safe (TC-003).
		constant = [5, allAgs.size(), allOrgs.size(), allEvs.size()].min();
		for (int i = 0; i < constant; i++){

			topAgs.add(allAgs.get(i));
			topEvs.add(allEvs.get(i));
			topOrgs.add(allOrgs.get(i));

			def agli = 0
			def evli = 0
			def orgli = 0

			// commAg/event/campusOrg are nullable:true on ServiceHour — guard every
			// deref so a single hour with a null FK can't NPE the whole report (TC-006).
			for (ServiceHour s: list){
				if (s.commAg?.name?.equals(allAgs.get(i).name)){
					agli += s.duration
				}
				if (s.event?.name?.equals(allEvs.get(i).name)){
					evli += s.duration
				}
				if (s.campusOrg?.name?.equals(allOrgs.get(i).name)){
					orgli += s.duration
				}
			}
			agHours.add(agli)
			evHours.add(evli)
			orgHours.add(orgli)
			
		}

		render view:"semesterReport", model: [totalHours:totalHours, constant:constant, topAgs:topAgs, topOrgs:topOrgs, topEvs:topEvs, agHours:agHours, evHours:evHours, orgHours:orgHours]

	}
	
	def commOrgReport() {
		def orgList = CommAg.list()
		def hourList = []
		for(int a = 0; a < orgList.size(); a++){
			def total = 0
			for(ServiceHour s: ServiceHour.list()){
				if(s.commAg.name.equals(orgList.get(a).name)){
					total += s.duration
				}
			}
			hourList.add(total)
		}
		render view: "commOrgReport", model: [orgList:orgList, hourList:hourList]
	}
	
	
	def campusOrgReport() {
		def orgList = CampusOrg.list()
		def hourList = []
		for(int a = 0; a < orgList.size(); a++){
			def total = 0
			for(ServiceHour s: ServiceHour.list()){
				if(s.campusOrg.name.equals(orgList.get(a).name)){
					total += s.duration
				}
			}
			hourList.add(total)
		}
		render view: "campusOrgReport", model: [orgList:orgList, hourList:hourList]
		
	}
	
	def yearReport(){
		render view:"yearReport",model:[list:stationReportService.hourKPIInFiveYear()]
	}
	
	def _hoursByYear(){
		int year=Integer.parseInt(params.year)
		def aList=ServiceHour.findAllByStatus(Status.APPROVED);
		def list=aList.findAll{
			it.starttime.getAt(Calendar.YEAR)==year
		}
		render view:"_hoursByYear",model:[list:list]
	}
	
	
}
