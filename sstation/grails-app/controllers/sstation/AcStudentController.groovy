package sstation


/**
 * AcStudent controller management the student list section. It has several functions
 * 1. Editing, creating, deleting student
 * 2. Editing, creating, deleting service hours for a specific student
 * 
 *  Domain 1 is admin and domain 2 is student.
 *  Only admin can change the status of the servicehour
 *  
 */
import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

import java.text.SimpleDateFormat
import java.util.Date

import sstation.AcStudent;
import sstation.CampusOrg;
import sstation.CommAg;
import sstation.Event;
import sstation.ServiceHour;
import sstation.Status;

import grails.transaction.Transactional
import java.security.MessageDigest
import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_ADMIN', 'ROLE_STUDENT', 'ROLE_MODERATOR'])
@Transactional(readOnly = false)

class AcStudentController {
	def hourService//Calculate all statistics about service hour list
	def reportService//Used for report pages
	def studentService//for student uploads
	def mailService
	def springSecurityService


	/*
	 * Basic Pages
	 */
	/**
	 * Basic Pages:Log in and sign up
	 * @return
	 */	



	/*
	 * Methods about student management
	 */


	/**
	 * Main Page
	 * @return
	 */
	def studentList() {
		def list=AcStudent.list()   //Prepare the student list
		render view:"student/studentList",model:[list:list]//Map and return the student list
	}

	/**
	 * Student Table
	 */
	def _sTable(){
		def list=AcStudent.list()   //Prepare the student list
		render view:"student/_sTable", model:[list:list]//Map and return the student list
	}


	/**
	 * Not used yet
	 * Report overall statistics about all students and their service hours
	 * @return
	 */
	def _studentOverallKPI(){
		render view:"kpi/_studentOverallKPI"
	}


	/**
	 * This page is the main page for the current student.
	 * @param student  passed from the _studentList.gsp
	 */
	def home(AcStudent student) {

		def stat=hourService.studentStat(student)
		def list=student.serviceHours

		render view:"student/home",model:[student:student,stat:stat,list:list]
	}


	/**
	 * This page is the main page for the current student.
	 * @param student  passed from the _studentList.gsp
	 */
	def student(AcStudent student) {
		render view:"student/student",model:[student:student]
	}

	/**
	 * Show the details information about the current student
	 * @param student passed from the _student.gsp
	 * @return
	 */
	def _showStudent(AcStudent student){
		render view:"student/_showStudent",model:[student:student]
	}


	/**
	 * KPI about a specific student
	 * @param student
	 * @return
	 */
	def _studentKPI(AcStudent student){
		render view:"student/_studentKPI",model:[student:student]
	}


	/**
	 * Render the create student page. The newly created AcStudent object is decleared in the gsp page
	 * @return
	 */
	def _createStudent(){
		AcStudent student=new AcStudent()
		render view:"student/_createStudent",model:[student:student]
	}


	/**
	 * Edit the current student. 
	 * @param student  passed from the _student.gsp
	 * @return
	 */
	def _editStudent(AcStudent student){
		render view:"student/_editStudent",model:[student:student]
	}



	/**
	 * This method will save the change the user make on the create or edit dialog
	 * @param student
	 * @return
	 */

	def saveStudent(AcStudent student) {


		student.properties=params
		try{
			if(!student.save(flush:true,failOnError:true)){
				render view:"student/_createStudent",model:[student:student]
			}

			student.save(flush:true,failOnError:true)
			render view:"student/student",model:[student:student]
		}catch(Exception ex){
			render view:"student/_createStudent",model:[student:student]

		}


	}

	/**
	 * Update changed student after editing.
	 * @param student 
	 * @return
	 */
	def _updateStudent(AcStudent student) {
		student.properties=params
		/*if (student == null) {
		 println("null")
		 notFound()
		 return
		 }
		 */
		if (student.hasErrors()) {
			println("error")
			respond student.errors, view:'_editStudent'
			return
		}

		student.save flush:true
		//println springSecurityService.getAuthentication()
	
		
		if(springSecurityService.getCurrentUser()==null){
			println student.id
			redirect controller:"acStudent", action:"student",id:student.id
			return
		}
		
		
		println springSecurityService.getCurrentUser().getAuthorities().toString()
		if(springSecurityService.getCurrentUser().getAuthorities().toString()=="[ROLE_ADMIN]"){
			redirect controller:"acStudent", action:"student",id:student.id
		}else if(springSecurityService.getCurrentUser().getAuthorities().toString()=="[ROLE_STUDENT]"){
			redirect controller:"acStudent",action:"home",id:student.id		
		} else {
			redirect controller:"acStudent", action:"student",id:student.id
		}
	}


	/**
	 * Delete the current student
	 * @param student
	 * @return
	 */
	def deleteStudent(AcStudent student) {

		if (student == null) {
			notFound()
			return
		}

		student.delete flush:true
		redirect controller:"acStudent",action:"studentList"
	}

	/**
	 * Delete multiple selected students
	 * @return
	 */
	def deletemult(){
		def list=[];
		println params.checkstudent
		params.checkstudent.each{
			def id=Long.parseLong(it)
			AcStudent.get(id).delete flush:true
		}
		render view:"student/studentList", model:[list:AcStudent.list()]




	}

	/**
	 * When the specific acStudent object is not found, this method is used.
	 */
	protected void notFound() {
		request.withFormat {
			form multipartForm {
				flash.message = message(code: 'default.not.found.message', args: [message(code: 'acStudent.label', default: 'AcStudent'), params.id])
				redirect action: "student/_studentList", method: "GET"
			}
			'*'{ render status: NOT_FOUND }
		}
	}






	/*
	 * Methods about service hours in a specific student
	 */
	/**
	 * Display the service hour list of a specific student
	 * @param student
	 * @return
	 */
	def _hourList(AcStudent student){
		def stat=hourService.studentStat(student)
		def list=student.serviceHours
		render view:"shour/_hourList",model:[student:student,stat:stat,list:list]
	}

	/**
	 * Update the status of the current service hour. Only admin could access this function
	 */
	def updateStatus(){
		int shid=Integer.parseInt(params.shid)
		def sh=ServiceHour.get(shid)
		sh.status=params.statusChange;
		sh.save flush:true,failOnError:true

		def student=sh.acStudent;
		_hourList(student)
	}

	/**
	 * Show the current service hour
	 * @param sh
	 * @return
	 */
	def shour(ServiceHour sh){
		render view:"shour/shour",model:[shour:sh]
	}
	/**
	 * Create a new service hour
	 * @param ac:the AcStudent that the new service hour will belong to
	 * @return
	 */
	def _createShour(AcStudent ac){
		ServiceHour sh=new ServiceHour()

		def eventList=Event.list().collect{ it.name }
		def orgList=CampusOrg.list().collect{ it.name }
		def fullOrgList = CampusOrg.list()
		def fullAgList = CommAg.list()
		def agList=CommAg.list().collect{ it.name }
		render view: "shour/_createShour",
		model: [acid:ac.id,eventList:eventList,orgList:orgList,fullOrgList:fullOrgList,agList:agList,fullAgList:fullAgList,shour:sh]
	}
	/**
	 * Edit the current service hour
	 * @param sh
	 * @return
	 */
	def _editShour(ServiceHour sh){
		def eventList=Event.list().collect{ it.name }
		def orgList=CampusOrg.list().collect{ it.name }
		def fullOrgList = CampusOrg.list()
		def fullAgList = CommAg.list()
		def agList=CommAg.list().collect{ it.name }
		render view: "shour/_editShour",
		model: [eventList:eventList,orgList:orgList,fullOrgList:fullOrgList,agList:agList,fullAgList:fullAgList,shour:sh]
	}

	/**
	 * Save the current servicehour
	 * @param sh
	 * @return
	 */
	def saveShour(ServiceHour sh){
		//Set the lastModified time
		sh.properties=params
		//Set the lastModified time
		sh.lastmodified=new Date()

		//Get the start date from the input
		SimpleDateFormat format=new SimpleDateFormat("MM/dd/yyyy")
		String d=params.startdate+" "+params.starthour+":"+params.startmin
		Date date = new Date().parse("MM/dd/yyyy HH:mm",d)
		println "printing stuff"
		sh.starttime=date
		sh.campusOrg=CampusOrg.findByName(params.selected_CamOrg)
		if (sh.campusOrg == null){
			sh.otherCamOrg = params.selected_CamOrg
		}
		sh.event=Event.findByName(params.shEvent)
		sh.commAg=CommAg.findByName(params.selected_CommAg)
		if (sh.commAg == null){
			sh.otherCommAg = params.selected_CommAg
		}
		sh.status=params.status;

		//Parse the id in the idList param and get corresponding students
		def ac=AcStudent.get(params.acid)
		ac.addToServiceHours(sh).save(flush:true,failOnError:true)


		if( !sh.save(flush:true,failOnError:true) ) {

			sh.errors.each { println it }
		}

		render view:"shour/shour",model:[shour:sh]


	}

	/**
	 * Delete the current service hour
	 * @param sh
	 * @return
	 */
	def deleteShour(ServiceHour sh){
		println sh;
		AcStudent ac=sh.acStudent
		sh.delete flush:true
		student(ac)

	}



	/*
	 *Report Methods 
	 */

	/**
	 * Admin access only. Show three different ways to report the service hours of a specific student
	 * @param ac
	 * @return
	 */
	def reportAdmin(AcStudent ac){
		def stat=hourService.studentStat(ac)
		render view:"report/reportAdmin",model:[student:ac,stat:stat]


	}

	def reportStudent(AcStudent ac){
		def stat=hourService.studentStat(ac)
		render view:"report/reportStudent",model:[student:ac,stat:stat]
	}

	/**
	 * Show a report page about service hours of a specific student
	 */
	def studentReport(AcStudent ac){
		def list=[]
		def totalSH=0
		if(ac.serviceHours!=null){
			if(ac.serviceHours.toList().size()!=0){
				list=ac.serviceHours.toList().findAll{
					it.status==Status.APPROVED
				}
			}
			totalSH=hourService.studentStat(ac).get('aSum')
		}


		render view:"report/studentReport",model:[student:ac,list:list,totalSH:totalSH]
	}

	/**
	 * Show a report page about service hours by semester of a specific student 
	 */
	def semesterReport(AcStudent ac){
		def SHlist=reportService.semesterReport(ac)
		def totalSH=0.0
		if(hourService.studentStat(ac).get('aSum')!=null){
			totalSH=hourService.studentStat(ac).get('aSum')
		}
		render view:"report/semesterReport",model:[student:ac,SHlist:SHlist,totalSH:totalSH]
	}

	def orgReport(AcStudent ac){
		def stat=hourService.studentStat(ac)
		def totalSH=stat.get('aSum')

		def listSH = []
		ac.serviceHours.each {
			if (it.status == Status.APPROVED){
				listSH.add(it)
			}
		}
		println listSH.size()

		def listCO = CampusOrg.list()

		render view:"report/orgReport",model:[student:ac,listSH:listSH,listCO:listCO,totalSH:totalSH]

	}

	def requestSemester(AcStudent ac){
		mailService.sendMail{
			to "mhiggs@austincollege.edu"
			subject "Semester Report Request"
			body "Request semester report for ${springSecurityService.currentUser}"
		}

		def message = "Request semester report sent"
		def stat=hourService.studentStat(ac)
		redirect action:"reportStudent", id: "${ac.id}", controller:"acStudent", params:[message:"Semester Report Request Sent"]

	}


	def requestGeneral(AcStudent ac){
		mailService.sendMail{
			to "mhiggs@austincollege.edu"
			subject "General Report Request"
			body "Request general report for ${springSecurityService.currentUser}"
		}

		def message = "Request general report sent"
		def stat=hourService.studentStat(ac)
		redirect action:"reportStudent", id: "${ac.id}", controller:"acStudent", params:[message:"General Report Request Sent"]

	}

	def requestCampusGroup(AcStudent ac){
		mailService.sendMail{
			to "mhiggs@austincollege.edu"
			subject "Campus Group Report Request"
			body "Request campus group report for ${springSecurityService.currentUser}"
		}

		def message = "Request campus group report sent"
		def stat=hourService.studentStat(ac)
		redirect action:"reportStudent", id: "${ac.id}", controller:"acStudent", params:[message:"Campus Group Report Request Sent"]

	}






	/**
	 * Upload students
	 * @return
	 */
	def uploadPage(){
		render view:"uploadPage",model:[]
	}

	def upload(){
		def a

		println params

		def uploadedFile = request.getFile('CSV')

		if (uploadedFile == null) throw new Exception("cannot access course file")

		def invalidList = []

		if(!uploadedFile.empty){
			println "Class: ${uploadedFile.class}"
			println "Name: ${uploadedFile.name}"
			println "OriginalFileName: ${uploadedFile.originalFilename}"
			println "Size: ${uploadedFile.size}"
			println "ContentType: ${uploadedFile.contentType}"

			def inStream = uploadedFile.getInputStream()
			a = studentService.importStudents(inStream)
		}
		render view:"uploadSuccess",model:[added:a[0],updated:a[1]]
	}





}