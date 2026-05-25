package sstation

/**
 * This domain class contains basic information of a service hour. It belongs to a specific AcStudent.
 * @author lenovo
 *
 */

class ServiceHour {
	String description,eventContactName,eventContactPhone,eventContactEmail//Variables about the service
	Status status //The current status of the service hour. Possible values: Approved, Pending, or Reject
	double duration
	CampusOrg campusOrg
	CommAg commAg
	Event event
	Date starttime,lastmodified 
	static belongsTo=[acStudent:AcStudent]
	String otherCamOrg
	String otherCommAg
	
	static mapping = {
		description type: "text"
	}
	
    static constraints = {
		eventContactEmail(email:true,nullable:true,blank:true)
		event(nullable:true)
		eventContactName(nullable:true,blank:true)
		eventContactPhone(nullable:true,blank:true)
		commAg(nullable:true)
		otherCommAg(nullable:true)
		campusOrg(nullable:true)
		status(nullable:false,blank:false)
		duration(nullable:false)
		otherCamOrg(nullable:true)
		otherCommAg(nullable:true)
    }
	
}
