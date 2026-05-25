package sstation
import sstation.AcStudent;
import sstation.Classification;
import sstation.ServiceHour;
import sstation.Status;
import grails.transaction.Transactional

/**
 * Stores all methods used in statistics calculation
 * @author lenovo
 *
 */
class HourService {
	int currentYear//current year  i.e. 2015
	List<ServiceHour> currentList//stores all approved service hours in the current year.
	List<ServiceHour> totalList//stores all approved service hours
	double totalDuration//stores the sum of duration in totalList


	/*
	 * Hour List Stats Methods
	 */
	/**
	 * Initialize all values
	 */
	private void init(){
		currentYear=2015

		totalList=ServiceHour.findAllByStatus(Status.APPROVED)//grails method

		currentList=totalList.findAll{//groovy method
			it.starttime.getAt(Calendar.YEAR)==currentYear
		}
		if(totalList.size()!=0)
			totalDuration=totalList.duration.sum()
		else totalDuration=0
	}

	def hourCall(){
		init()
		def overallStat=overallStat()

		def tendency5y=overall5Year()
		def tendencyYear=overallCurrentYear()

		def chartClas=overallByClas()
		def chartStatus=overallByStatus()
		return [overallStat:overallStat,chartClas:chartClas,chartStatus:chartStatus,tendencyYear:tendencyYear,tendency5y:tendency5y]
	}

	/**
	 * Show the statistics about all students and all service hours
	 * Only Admin have access to these data
	 * @return
	 */
	def overallStat() {
		int totalStudent=AcStudent.count()	//the amount of students

		double totalYear=currentList.duration.sum();//the sum of duration of service hours in the current year

		double averagePerStudent=totalDuration/totalStudent//average service hour per student

		def shSubP=ServiceHour.findAllByStatus(Status.PENDING)//all pending service hours
		int pTotal=shSubP.size()

		return [totalSt:totalStudent,totalSH:totalDuration,average:averagePerStudent,totalYear:totalYear,pTotal:pTotal]
	}




	/*
	 * Tendency Chart Methods
	 */

	/**
	 * Calculate the total service hours in each of the last five years
	 * @return
	 */
	def overall5Year(){
		int last1=currentYear-1
		int last2=currentYear-2
		int last3=currentYear-3
		int last4=currentYear-4

		//Stores the sum of duration of service hours for the last five years
		double totalYear=0,totalLast1=0,totalLast2=0,totalLast3=0,totalLast4=0

		/*
		 * Find service hours in each year in the totalList(total approved service hour list)
		 * Add the duration of the found service hour to corresponding sum
		 */
		totalList.each{
			switch(it.starttime.getAt(Calendar.YEAR)){
				case currentYear:
					totalYear+=it.duration
					break

				case last1:
					totalLast1+=it.duration
					break

				case last2:
					totalLast2+=it.duration
					break
				case last3:
					totalLast3+=it.duration
					break
				case last4:
					totalLast4+=it.duration

			}
		}

		def average=totalDuration/5//Average service hours per year

		return[average:average,totalYear:totalYear,totalLast1:totalLast1,totalLast2:totalLast2,totalLast3:totalLast3,totalLast4:totalLast4]
	}


	/**
	 * Calculate the total service hours in each of 12 monthes in the current year
	 * @return
	 */
	def overallCurrentYear(){
		def monthSH=[]//Stores sublist of service hours in each month

		for(int i=0;i<12;i++){
			def currentM=currentList.findAll{it.starttime.getAt(Calendar.MONTH)==i}//Current month service hour list
			def num=currentM.duration.sum()
			if(num!=null)
				monthSH.add(num)
			else
				monthSH.add(0)
		}
		def average
		if(currentList.size()!=0)average=currentList.duration.sum()/12//Average service hours per month
		else average=0
		monthSH.add(average)

		return monthSH
	}




	/**
	 * Overall Current Month Tendency
	 */
	def overallMonth(){
		int m=new Date().getAt(Calendar.MONTH)
		def mList=currentList.findAll{
			it.starttime.getAt(Calendar.MONTH)==m
		}

	}






	/*
	 * Pie Chart Methods
	 */

	/**
	 * Group service hours by classification
	 */
	def overallByClas(){
		//Find service hour sublist by classification
		def frList=ServiceHour.list().findAll{
			if(it.acStudent!=null){
				it.acStudent.classification==Classification.FR
			}
		}
		def soList=ServiceHour.list().findAll{
			if(it.acStudent!=null){
				it.acStudent.classification==Classification.SO
			}
		}
		def juList=ServiceHour.list().findAll{
			if(it.acStudent!=null){
				it.acStudent.classification==Classification.JR
			}
		}
		def seList=ServiceHour.list().findAll{
			if(it.acStudent!=null){
				it.acStudent.classification==Classification.SR
			}
		}
		def gradList=ServiceHour.list().findAll{
			if(it.acStudent!=null){
				it.acStudent.classification==Classification.GRAD
			}
		}
		def otherList=ServiceHour.list().findAll{
			if(it.acStudent!=null){
				it.acStudent.classification==Classification.OTHER
			}
		}

		//Calculate percentage
		def frSum=0.0
		if(frList.size()!=0) frSum=frList.duration.sum()/totalDuration

		def soSum=0.0
		if(soList.size()!=0) soSum=soList.duration.sum()/totalDuration

		def juSum=0.0
		if(juList.size()!=0)juSum=juList.duration.sum()/totalDuration

		def seSum=0.0
		if(seList.size()!=0)seSum=seList.duration.sum()/totalDuration

		def gradSum=0
		def otherSum=0

		return[frSum:frSum,soSum:soSum,juSum:juSum,seSum:seSum,gradSum:gradSum,otherSum:otherSum]
	}


	/**
	 * Group service hours by status
	 * @return
	 */
	def overallByStatus(){
		//find service hour sublists by status
		def aList=ServiceHour.list().findAll{
			it.status==Status.APPROVED
		}
		def pList=ServiceHour.list().findAll{
			it.status==Status.PENDING
		}
		def rList=ServiceHour.list().findAll{
			it.status==Status.REJECTED
		}

		double aSum=0
		if(aList.size()!=0)aSum=aList.duration.sum()/totalDuration

		double pSum=0
		if(pList.size()!=0)pSum=pList.duration.sum()/totalDuration


		double rSum=0
		if(rList.size()!=0)rSum=rList.duration.sum()/totalDuration

		return[aSum:aSum,pSum:pSum,rSum:rSum]


	}















	/*
	 * Single Student Method. This method will be moved to StudentService 
	 */

	/**
	 * Show the statistics about a specific student 
	 */
	def studentStat(AcStudent ac){
		def acList=[]
		if(ac.serviceHours!=null){
			acList=ac.serviceHours.toList()
		}
		double grandSum=0
		double aSum=0
		double pSum=0
		double rSum=0
		def aList=[]
		def pList=[]
		def rList=[]

		if(acList!=null){
			if(acList.size()!=0){
				grandSum=acList.duration.sum()

				aList=acList.findAll{
					it.status==Status.APPROVED
				}
				pList=acList.findAll{
					it.status==Status.PENDING
				}
				rList=acList.findAll{
					it.status==Status.REJECTED
				}

				if(aList!=null){
				if(aList.size()!=0)aSum=aList.duration.sum()

				}
				
				if(pList!=null){
				if(pList.size()!=0)pSum=pList.duration.sum()
				}

				if(rList!=null){
				if(rList.size()!=0)rSum=rList.duration.sum()
				}
			}
		}

		return [grandSum:grandSum,aSum:aSum,pSum:pSum,rSum:rSum]
	}

}
