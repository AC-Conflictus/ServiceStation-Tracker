package sstation

import grails.transaction.Transactional
import java.security.MessageDigest
import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_ADMIN'])
@Transactional(readOnly = false)
class ModeratorController {

    def index() { 
		def list=AcStudent.list()   //Prepare the student list
		def rs = []
		for (int i = 0; i < list.size(); i++){
			if (list.get(i).isModerator){
				rs.add(list.get(i))
			}
		}
		render view:"index",model:[list:rs]//Map and return the student list
	}
	
	def _presentStudentTableAdd(){
		def li=AcStudent.list()
		render view:'_tableStudentAdd', model:[list: li]
	}
	
	def _presentStudentTableDelete(){
		def li=AcStudent.list()
		render view:'_tableStudentDelete', model:[list: li]
	}
	
	def _addModerator(AcStudent acStudent){
		acStudent.isModerator = true
		acStudent.save(flush:true)
		def list=AcStudent.list()   //Prepare the student list
		def rs = []
		for (int i = 0; i < list.size(); i++){
			if (list.get(i).isModerator){
				rs.add(list.get(i))
			}
		}
	
		render view:"_sTable",model:[list:rs]//Map and return the student list
	}
	def _deleteModerator(AcStudent acStudent){
		acStudent.isModerator = false
		acStudent.save(flush:true)
		def list=AcStudent.list()   //Prepare the student list
		def rs = []
		for (int i = 0; i < list.size(); i++){
			if (list.get(i).isModerator){
				rs.add(list.get(i))
			}
		}
		
		render view:"_sTable",model:[list:rs]//Map and return the student list
	}
	
}
