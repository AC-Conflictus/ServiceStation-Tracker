package sstation

import sstation.AcStudent;
import sstation.CampusOrg;
import sstation.Status;
import grails.transaction.Transactional
/**
 * This service class provides methods for reporting student service hours.
 * @author lenovo
 *
 */

class ReportService {

	/**
	 * Divide a student's service hours list in seperate sub-lists by semester
	 * @param ac
	 * @return
	 */
	def semesterReport(AcStudent ac) {
		//Get all approved service hours
		def list=[]
		if(ac.serviceHours!=null){
			if(ac.serviceHours.size()!=0){
				list=ac.serviceHours.toList().findAll{
					it.status==Status.APPROVED
				}
			}
		}
		//Get current year
		def currentYear=new Date().getAt(Calendar.YEAR)


		/*
		 * The for loop will go through 'list' to find all semesters that contains service hours
		 * The name of the found semester and the sub-list of service hours in that semester will be
		 * stored in the new list "SHlist"
		 */
		def SHlist=[]
		for(int i=currentYear;i>currentYear-5;i--){//In five years
			//find all service hours in i year
			def yearList=[]
			if(list.size()!=0){
				yearList=list.findAll{
					it.starttime.getAt(Calendar.YEAR)==i
				}

				if(yearList.size!=0){
					//find service hours in each semester in i year
					def lspring=yearList.findAll{
						it.starttime.getAt(Calendar.MONTH)>=1&&it.starttime.getAt(Calendar.MONTH)<=4
					}
					def lsummer=yearList.findAll{
						it.starttime.getAt(Calendar.MONTH)>=5&&it.starttime.getAt(Calendar.MONTH)<=7
					}
					def lfall=yearList.findAll{
						it.starttime.getAt(Calendar.MONTH)>=8&&it.starttime.getAt(Calendar.MONTH)<=11
					}
					def lwinter=yearList.findAll{
						it.starttime.getAt(Calendar.MONTH)==0
					}

					//add found semester's name and service hour sublist to the SHlist
					if(lspring.size()!=0){
						SHlist.add("${i}SP")
						SHlist.add(lspring)
					}
					if(lsummer.size()!=0){
						SHlist.add("${i}SU")
						SHlist.add(lsummer)
					}
					if(lfall.size()!=0){
						SHlist.add("${i}FA")
						SHlist.add(lfall)
					}
					if(lwinter.size()!=0){
						SHlist.add("${i}JA")
						SHlist.add(lwinter)
					}

				}



			}
		}
		return SHlist
	}

	def orgReport(AcStudent ac){
		//Find all approved service hours
		def list=ac.serviceHours.toList().findAll{
			it.status==Status.APPROVED
		}

		def SHList=[]
		def orgList=CampusOrg.list()

		for(CampusOrg org:orgList){
			def oneOrg=list.findAll{
				it.campusOrg==org
			}
			if(oneOrg.size()!=0){
				SHList.add(org)
				SHList.add(oneOrg)
			}
		}
		return SHList

	}
}
