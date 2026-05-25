package sstation

class CampusOrg {
	String name
	String description
	String contact
	String contactPhone
	String contactEmail
	
	static mapping = {
		description type: "text"
	}
	
    static constraints = {
		contactEmail(email:true,blank:false,nullable:false)
		description(email:false,nullable:false)
		contact(blank:false,nullable:false)
		contactPhone(blank:false,nullable:false)
		name(blank:false,nullable:false)
    }
}
