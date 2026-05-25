package sstation;

import java.util.ArrayList;

public class CampusOrgReportService {
	def CampusOrg org
	def serviceList = []
	public CampusOrgReportService(CampusOrg o, serviceList){
		this.org = o;
		this.serviceList = serviceList;
		
	}
	

	
	def getTotalHour(){
		def totalHour = 0;
		def li = [];
		
		for (ServiceHour s: serviceList){
			if (s.campusOrg.name.equals(this.org.name)){
				li.add(s);
			}
		}
		for (ServiceHour s: li){
			totalHour += s.duration;
		}
		
		return totalHour;
	}
	
	def getTotalHourByYear(int year){
		def totalHour = 0;
		def li = [];
		def Calendar cal = Calendar.getInstance()
		
		for (ServiceHour s: serviceList){
			cal.setTime(s.starttime)
			if (s.campusOrg.name.equals(this.org.name) && cal.get(Calendar.YEAR) == year){
				li.add(s);
			}
		}
		for (ServiceHour s: li){
			totalHour += s.duration;
		}
		
		return totalHour;
	}
	
	/*
	 * Get the list of all service hour
	 */
	def getListHour(CampusOrg o){
		def li = []
		for (ServiceHour s: serviceList){
			if (s.campusOrg.equals(this.org)){
				li.add(s);
			}
		}
		return li
	}
	
	
}
