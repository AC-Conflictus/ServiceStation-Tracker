package sstation
import grails.transaction.Transactional
import java.security.MessageDigest
import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_ADMIN','ROLE_MODERATOR'])
@Transactional(readOnly = false)
class ACGroupController {

	/*
	 * Methods about Campus Organizations
	 */

	/**
	 * The main page for organization and agencies. Display 10 of each
	 * @return
	 */

	def index(){
		def list=CampusOrg.list()
		[list:list]
	}
	def _orgCardMain(){
		def orgList=CampusOrg.list()
		render view:"_orgCardMain",model:[list:orgList]
	}

	def _orgTableMain(){
		def list=CampusOrg.list()
		render view:"_orgTable",model:[list:list]
	}

	def _createOrg(){
		redirect action:'orgForm'
	}

	def orgForm(){
		CampusOrg org=new CampusOrg();
		def heading="New Campus Organization"
		render view:"orgForm",model:[org:org,heading:heading]
	}

	def _editOrg(CampusOrg org){
		render view:"orgForm",model:[org:org,heading:org.name]
	}
	/**
	 * Save or update a org and direct to the saved page
	 * from there it auto redirects to the index
	 * @param org
	 * @return
	 */
	def _saveOnCard(CampusOrg org){
		org.properties=params
		try{
			if (!org.save(flush:true)) {
				render view:'orgForm', model:[org:org,form:1,card:1]
				return
			}
			org.save(flush:true,failOnError:true)

			render view:'formSaved',model:[card:1]
		}catch(Exception e){
			render view:'orgForm', model:[org:org,form:1,card:1]
		}
	}
	/**
	 * Save or update an org and direct to the saved page
	 * from there it auto redirects to the index
	 * @param org
	 * @return
	 */
	def _saveOnTable(CampusOrg org){
		org.properties=params
		try{
			if (!org.save(flush:true)) {
				render view:'orgForm', model:[org:org,form:1,table:1]
				return
			}
			org.save(flush:true,failOnError:true)

			render view:'formSaved',model:[table:1]
		}catch(Exception e){
			render view:'orgForm', model:[org:org,form:1,table:1]
		}
	}

	/**
	 * Delete an org and direct to the card view
	 * @param org
	 * @return
	 */
	def _deleteOnCard(CampusOrg org){
		ServiceHour.findAllByCampusOrg(org).each{
			it.delete(flush:true,failOnError:true)
		}
		org.delete(flush:true,failOnError:true)
		def list=CampusOrg.list()
		render view:"_orgCardMain",model:[list:list]
	}
	/**
	 * Delete an org and direct to the table view
	 * @param org
	 * @return
	 */
	def _deleteOnTable(CampusOrg org){
		ServiceHour.findAllByCampusOrg(org).each{
			it.delete(flush:true,failOnError:true)
		}
		org.delete(flush:true,failOnError:true)

		def list=CampusOrg.list()
		render view:"_orgTable",model:[list:list]
	}
}
