package sstation
import sstation.*;
import sstation.AcStudentController;
import sstation.CampusOrg;
import sstation.Classification;
import sstation.ServiceHour;
import sstation.Status;
import grails.test.runtime.FreshRuntime;
import grails.test.mixin.*
import spock.lang.*
import grails.plugin.springsecurity.*

@TestFor(AcStudentController)
@Mock([AcStudent,ServiceHour,CampusOrg,Event,CommAg])
@FreshRuntime
class AcStudentControllerSpec extends Specification {
	def rand

	def setup(){
		rand=new Random()
	}
	/**
	 * Testing methods about student management
	 */
	void "studentlist with no students should be empty"(){
		when:"execute studentList with no students"
		controller.studentList()
		then:"No students will be in the list"
		view=="/acStudent/student/studentList"
		model.list.size()==0
	}


	void "test studentList with some students should not be empty"(){

		when: "There are 10 students"
		10.times{
			new AcStudent(firstname:"Renata",lastname:"Chai",acid:"AC3483"+it,acEmail:"rchai13@austincollege.edu",acBox:"11111",acYear:2013,classification:Classification.JR,phone:"1111111111", status:'A').save(flush:true,failOnError:true)
		}
		controller.studentList()
		then:"10 students will be shown in the list"
		view=="/acStudent/student/studentList"
		model.list.size()==10
		model.list[0].firstname=="Renata"
	}




	void "test sTable which leads to the student list"(){
		when:"execute _studentList with no students"
		controller._sTable()
		then:"No students will be in the list"
		view=="/acStudent/student/_sTable"
		model.list.size()==0

		when: "There are 10 students"
		10.times{
			new AcStudent(firstname:"Renata",lastname:"Chai",acid:"AC3483"+it,acEmail:"rchai13@austincollege.edu",acBox:"11111",acYear:2013,classification:Classification.JR,phone:"1111111111", status:'A').save(flush:true,failOnError:true)
		}
		controller._sTable()
		then:"10 students will be shown in the list"
		view=="/acStudent/student/_sTable"
		model.list.size()==10
		model.list[0].firstname=="Renata"
	}

	void "test student which leads the student main page"(){
		when:
		AcStudent ac=new AcStudent(firstname:"Renata",lastname:"Chai",acid:"AC3483",acEmail:"rchai13@austincollege.edu",acBox:"11111",acYear:2013,classification:Classification.JR,phone:"1111111111", status:'A').save(flush:true,failOnError:true)
		controller.student(ac)
		then:
		view=="/acStudent/student/student"
		model.student==ac

	}

	void "test _showStudent which leads to the information page about the current student"(){
		when:
		AcStudent ac=new AcStudent(firstname:"Renata",lastname:"Chai",acid:"AC3483",acEmail:"rchai13@austincollege.edu",acBox:"11111",acYear:2013,classification:Classification.JR,phone:"1111111111", status:'A').save(flush:true,failOnError:true)
		controller._showStudent(ac)
		then:
		view=="/acStudent/student/_showStudent"
		model.student==ac
	}
	void "test _createStudent which leads to the student creation form "(){
		when:
		controller._createStudent()
		then:
		view=="/acStudent/student/_createStudent"
	}
	void "test saveStudent which saves the newly created student and leads to the student infor. page"(){
		when:
		int count=AcStudent.count()
		AcStudent ac=new AcStudent(firstname:"Renata",lastname:"Chai",acid:"AC3483",acEmail:"rchai13@austincollege.edu",acBox:"11111",acYear:2013,classification:Classification.JR,phone:"1111111111", status:'A').save(flush:true,failOnError:true)
		controller.saveStudent(ac)
		then:
		view=="/acStudent/student/student"
		model.student==ac
		AcStudent.count()==count+1

	}
	void "test _editStudent which leads to the student edition form"(){
		when:
		AcStudent ac=new AcStudent(firstname:"Renata",lastname:"Chai",acid:"AC3483",acEmail:"rchai13@austincollege.edu",acBox:"11111",acYear:2013,classification:Classification.JR,phone:"1111111111", status:'A').save(flush:true,failOnError:true)
		controller._editStudent(ac)
		then:
		view=="/acStudent/student/_editStudent"
	}
	void "test updateStudent which update the edited student and leads to the student infor page"(){
		when:
		controller.springSecurityService=new SpringSecurityService()
		AcStudent ac=new AcStudent(firstname:"Renata",lastname:"Chai",acid:"AC3483",acEmail:"rchai13@austincollege.edu",acBox:"11111",acYear:2013,classification:Classification.JR,phone:"1111111111", status:'A').save(flush:true,failOnError:true)
		params.firstname="R"
		controller._updateStudent(ac)
		then:
		ac.firstname=="R"
		
	}

	void "test delete Student"(){
		when:
		AcStudent ac=new AcStudent(firstname:"Renata",lastname:"Chai",acid:"AC3483",acEmail:"rchai13@austincollege.edu",acBox:"11111",acYear:2013,classification:Classification.JR,phone:"1111111111", status:'A').save(flush:true,failOnError:true)
		int count=AcStudent.count()
		controller.deleteStudent(ac)
		then:
		AcStudent.count()==count-1
	}

	void "test deleting multiple student"(){
		when:"There are 10 students and students 1,3,5 are deleted"
		10.times{
			new AcStudent(firstname:"Renata",lastname:"Chai",acid:"AC3483"+it,acEmail:"rchai13@austincollege.edu",acBox:"11111",acYear:2013,classification:Classification.JR,phone:"1111111111", status:'A').save(flush:true,failOnError:true)
		}
		params.checkstudent=["1","3","5"]
		controller.deletemult()
		then:"7 students left"
		view=="/acStudent/student/studentList"
		model.list.size()==7
		AcStudent.get(1)==null
		AcStudent.get(3)==null
		AcStudent.get(5)==null
	}





	/**
	 * Report Method
	 */
	void "test reportAdmin"(){
		def s=Mock(AcStudent)
		controller.hourService=Mock(HourService)
		when:
		controller.reportAdmin(s)
		then:
		view=="/acStudent/report/reportAdmin"
	}

	void "test semesterReport"(){
		def s=new AcStudent(firstname:"Renata",lastname:"Chai",acid:"AC3483",acEmail:"rchai13@austincollege.edu",acBox:"11111",acYear:2013,classification:Classification.JR,phone:"1111111111", status:'A').save(flush:true,failOnError:true)
		controller.reportService=new ReportService()
		controller.hourService=new HourService()
		when:
		controller.semesterReport(s)
		then:
		view=="/acStudent/report/semesterReport"
	}

	void "test studentReport"(){
		def s=new AcStudent(firstname:"Renata",lastname:"Chai",acid:"AC3483",acEmail:"rchai13@austincollege.edu",acBox:"11111",acYear:2013,classification:Classification.JR,phone:"1111111111", status:'A').save(flush:true,failOnError:true)
		controller.hourService=new HourService()
		when:
		controller.studentReport(s)
		then:
		view=="/acStudent/report/studentReport"
	}

	/**
	 * Service Hour Management
	 */

	void "test _shour which leads to the basic infor of the current service hour"(){
		10.times{
			Mock(ServiceHour).save(flush:true,failOnError:true)
		}
		when:
		controller.shour(ServiceHour.get(1))
		then:
		view=="/acStudent/shour/shour"
		model.shour==ServiceHour.get(1)
	}

	void "test creating a service hour"(){
		def s=Mock(AcStudent)
		when:
		controller._createShour(s)
		then:
		view=="/acStudent/shour/_createShour"
	}

	void "test editing a service hour"(){
		when:
		def sh=Mock(ServiceHour)
		controller._editShour(sh)
		then:
		view=="/acStudent/shour/_editShour"
	}



}
