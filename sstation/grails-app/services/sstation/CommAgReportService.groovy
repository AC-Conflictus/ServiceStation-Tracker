package sstation;

import java.util.ArrayList;

public class CommAgReportService {
	def CommAg ag
	def serviceList = []
	public CommAgReportService(CommAg a, serviceList){
		this.ag = a;
		this.serviceList = serviceList;
		
	}
	

	
	def getTotalHour(){
		def totalHour = 0;
		def li = [];
		
		for (ServiceHour s: serviceList){
			if (s.commAg.name.equals(this.ag.name)){
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
			if (s.commAg.name.equals(this.ag.name) && cal.get(Calendar.YEAR) == year){
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
	def getListHour(){
		def li = []
		for (ServiceHour s: serviceList){
			if (s.commAg.equals(this.ag)){
				li.add(s);
			}
		}
		return li
	}
	
	
}
