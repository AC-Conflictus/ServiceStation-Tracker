package sstation

import sstation.CampusOrg;
import sstation.CommAg;
import sstation.Event;
import sstation.ServiceHour;
import grails.transaction.Transactional
import java.security.MessageDigest
import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_ADMIN','ROLE_MODERATOR'])
@Transactional(readOnly = false)

import grails.transaction.Transactional
import java.security.MessageDigest
import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_ADMIN', 'ROLE_MODERATOR'])
@Transactional(readOnly = false)

class EventController {
	/*
	 * Methods about events
	 */
	/**
	 * The event main page
	 * @return
	 */
	def index(){
		def list=Event.list()
		[list:list]
	}

	def eventMain(){
		def list=Event.list()
		render view:"eventMain",model:[list:list]
	}

	/**
	 * Event list page
	 * @return
	 */

	def _eventTable(){
		def list=Event.list()

		render view:"_eventTable",model:[list:list]
	}

	def _eventTableView(){
		def list=Event.list()

		render view:"_eventTableView",model:[list:list]
	}

	/**
	 * KPI page about all events
	 * @return
	 */
	def _eventOverallKPI(){
	}

	/**
	 * KPI page about a single event
	 * @param e
	 * @return
	 */

	def _eventKPI(){
	}

	/**
	 * Create a new event
	 * @return
	 */
	def _createEvent(){
		Event event=new Event()
		def heading="New Event"
		render view:"_eventForm",model:[event:event,heading:heading]
	}

	/**
	 * Edit a specific event
	 * @param e
	 * @return
	 */
	def _editEvent(Event e){
		render view:"_eventForm",model:[event:e,heading:e.name]
	}

	/**
	 * Save the edited/created event
	 * @param e
	 * @return
	 */

	def _saveOnCard(Event e){
		e.properties=params
		try{

			e.save(flush:true,failOnError:true)

			render view:'formSaved',model:[card:1]
		}catch(MissingPropertyException ex){
			redirect action:"index"
		}finally{
			if(!e.save(flush:true)){
				render view:'_eventForm', model:[event:e,form:1,card:1]
				return
			}
		}
	}
	/**
	 * Save or update an event and direct to the saved page
	 * from there it auto redirects to the index
	 * @param e
	 * @return
	 */
	def _saveOnTable(Event e){
		e.properties=params
		try{

			e.save(flush:true,failOnError:true)

			render view:'formSaved',model:[table:1]
		}catch(MissingPropertyException ex){
			redirect action:"index"
		}finally{
			if(!e.save(flush:true)){
				render view:'_eventForm', model:[event:e,form:1,table:1]
				return
			}
		}
	}

	/**
	 * Delete a specific event
	 * @param e
	 * @return
	 */
	def _deleteEventCard(Event e){
		ServiceHour.findAllByEvent(e).each{
			it.delete(flush:true,failOnError:true)
		}
		e.delete(flush:true,failOnError:true)
		_eventTable()
	}

	/**
	 * Delete a specific event
	 * @param e
	 * @return
	 */

	def _deleteEventTable(Event e){
		ServiceHour.findAllByEvent(e).each{
			it.delete(flush:true,failOnError:true)
		}
		e.delete(flush:true,failOnError:true)
		_eventTableView()
	}
}
