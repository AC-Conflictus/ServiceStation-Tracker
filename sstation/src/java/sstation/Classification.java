package sstation;

public enum Classification {
	FR("Freshman"),
	SO("Sophomore"),
	JR("Junior"),
	SR("Senior"),
	GRAD("Graduate Student"),
	OTHER("Other");
	
	final String name;
	
	private Classification(String name){
		this.name=name;
	}
}
