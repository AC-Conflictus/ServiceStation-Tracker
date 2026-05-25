package sstation

class Event {
	String name,description,contact,contactPhone, contactEmail
	
	static mapping = {
		description type: "text"
	}
	
    static constraints = {
		name nullable:false,blank:false
		description nullable:false,blank:false
		contact nullable:false,blank:false
		contactPhone nullable:false,blank:false
		contactEmail nullable:false,blank:false
    }
}
