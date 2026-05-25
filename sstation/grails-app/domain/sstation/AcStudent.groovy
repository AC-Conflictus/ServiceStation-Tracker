package sstation

/**
 * This class contains basic information about a student. Every AcStudent has many service hours
 * @author lenovo
 *
 */
class AcStudent {
	String firstname,lastname
	String acid,acEmail,acBox,phone
	Integer acYear
	Character status
	Classification classification
	Boolean isModerator

	static constraints = {
		firstname(nullable:false,blank:false)
		lastname(nullable:false,blank:false)
		status(nullable:false, blank:false)
		acid(unique:true,nullable:false,blank:false)
		acBox(nullable:true,blank:false)
		acEmail(email:true,nullable:false,blank:false)
		classification(nullable:true,blank:false)
		phone(nullable:true)
		acYear(nullable:true)
		isModerator(nullable:true)
	}
	static hasMany=[serviceHours:ServiceHour]


}
