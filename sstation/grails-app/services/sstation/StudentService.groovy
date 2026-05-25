package sstation

import java.util.List;

import sstation.AcStudent;
import sstation.Classification;
import sstation.ServiceHour;
import sstation.Status;
import grails.transaction.Transactional

@Transactional
class StudentService {

	int currentYear//current year  i.e. 2015
	List<ServiceHour> currentList//stores all approved service hours in the current year.
	List<ServiceHour> totalList//stores all approved service hours
	double totalDuration//stores the sum of duration in totalList
	def studentList;
	def totalStudent;

	/**
	 * Initialize all values
	 */
	private void init(){
		currentYear=2015

		totalList=ServiceHour.findAllByStatus(Status.APPROVED)//grails method

		currentList=totalList.findAll{
			//groovy method
			it.starttime.getAt(Calendar.YEAR)==currentYear
		}
		totalDuration=totalList.duration.sum()
		studentList=AcStudent.list()
		totalStudent=AcStudent.count()
	}

	def overallStat() {

		int totalStudent=AcStudent.count()	//the amount of students

		double totalYear=currentList.duration.sum();//the sum of duration of service hours in the current year

		double averagePerStudent=totalDuration/totalStudent//average service hour per student

		double averageCurrent=totalYear/totalStudent//average service hour per student in the current year

		return [totalSt:totalStudent,totalSH:totalDuration,average:averagePerStudent,totalYear:totalYear]
	}

	def studentByClas(){
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

	def importStudents(inStream){

		int i = 0
		int numberAdded = 0
		int numberUpdated = 0
		inStream.toCsvReader(['charset':'UTF-8']).eachLine { tokens ->
			def allStudents = AcStudent.list()
			def ids = allStudents.collect{it.acid}
			if (i > 0) {
			
				def s
				
				if (ids.contains(tokens[0])) //student exists
				{
					s = AcStudent.findByAcid(tokens[0])
					s.firstname = tokens[2]
					s.lastname = tokens[3]
					s.status = tokens[4]
					s.acBox=tokens[5]
					try{
						s.classification = tokens[6] as Classification
					}
					catch(Exception e){
						s.classification = sstation.Classification.OTHER
					}
					s.acEmail = tokens[8]
					if (s.validate()) {
						//print(s.acid +","+ s.firstname+","+ s.lastname+","+ s.status+","+ s.acBox+","+ s.classification+","+ s.acEmail)
						numberUpdated++
						s.save(flush:true);
					}
				}
				else //student does not exist
				{
					s = new AcStudent();
					s.acid = tokens[0]
					s.firstname = tokens[2]
					s.lastname = tokens[3]
					s.status = tokens[4]
					s.acBox=tokens[5]
					try{
						s.classification = tokens[6] as Classification
					}
					catch(Exception e){
						s.classification = sstation.Classification.OTHER
					}
					s.acEmail = tokens[8]
					if (s.validate()) {
						//print(s.acid +","+ s.firstname+","+ s.lastname+","+ s.status+","+ s.acBox+","+ s.classification+","+ s.acEmail)
						numberAdded++
						s.save(flush:true);
					}
				}
			}
			i++
		}
		inStream.close()
		return [numberAdded, numberUpdated]
	}
}
