package sstation;

import java.util.ArrayList;

public class EventReportService {
	def Event event
	def serviceList = []
	public EventReportService(Event e, serviceList){
		this.event = e;
		this.serviceList = serviceList;
		
	}
	

	
	def getTotalHourByEvent(Event e){
		def totalHour = 0;
		def li = [];
		
		for (ServiceHour s: serviceList){
			if (s.event.name.equals(this.event.name)){
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
			if (s.event.name.equals(this.event.name) && cal.get(Calendar.YEAR) == year){
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
	def getListHourByEvent(Event e){
		def li = []
		
		for (ServiceHour s: serviceList){
			if (s.event.equals(this.event)){
				li.add(s);
			}
		}
		return li
	}
	
	
}
