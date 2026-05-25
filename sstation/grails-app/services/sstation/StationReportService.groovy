package sstation

import grails.transaction.Transactional

@Transactional
class StationReportService {

	/**
	 * Calculate hour kpi in a specific year
	 */
	def hourKPIbyYear(int year){
		//All APPROVED hours
		def alist=ServiceHour.findAllByStatus(Status.APPROVED)
		
		//All APPROVED hours in "year"
		def totalList=alist.findAll{
			it.starttime.getAt(Calendar.YEAR)==year
		}
		
		//The sum of durations of all service hours in totalList
		double total=0
		if(totalList.size()!=0) total=totalList.duration.sum()
		
		//Diverse Averages
		double avgByStudent=0
		if(AcStudent.count()!=0) avgByStudent=(total/AcStudent.count()).round(2)
			
		double avgByGroup=0
		if(CampusOrg.count()!=0) avgByGroup=(total/CampusOrg.count()).round(2)
		
		double avgByCommOrg=0
		if(CommAg.count()!=0) avgByCommOrg=(total/CommAg.count()).round(2)
		
		double avgByEvent=0
		if(Event.count()!=0) avgByEvent=(total/Event.count()).round(2)
		
		
		[year:year,total:total,avgByStudent:avgByStudent,avgByGroup:avgByGroup,avgByCommOrg:avgByCommOrg,avgByEvent:avgByEvent]
	}
	
	
	/**
	 * Calculate hour KPI in five years
	 * @return
	 */
	def hourKPIInFiveYear(){
		def currentYear=new Date().getAt(Calendar.YEAR)
		
		def KPIlist=[]
		
		5.times{
			KPIlist.add(hourKPIbyYear(currentYear))
			currentYear--
		}
		return KPIlist
	}
	
	
	/**
	 * Calculate hour kpi for a specific event
	 */
	def hourKPIbyEvent(Event event){
		//All APPROVED hours
		def alist=ServiceHour.findAllByStatus(Status.APPROVED)
		
		//All APPROVED hours for an event
		def totalList=alist.findAll{
			it.event.equals(event);
		}
		
		//The sum of durations of all service hours in totalList
		double total=0
		if(totalList.size()!=0) total=totalList.duration.sum()
		
		//Diverse Averages
		double avgByStudent=0
		if(AcStudent.count()!=0) avgByStudent=(total/AcStudent.count()).round(2)
			
		double avgByGroup=0
		if(CampusOrg.count()!=0) avgByGroup=(total/CampusOrg.count()).round(2)
		
		double avgByCommOrg=0
		if(CommAg.count()!=0) avgByCommOrg=(total/CommAg.count()).round(2)
		
		
		[name:event.name,event:event.id,total:total,avgByStudent:avgByStudent,avgByGroup:avgByGroup,avgByCommOrg:avgByCommOrg]
	}
}
