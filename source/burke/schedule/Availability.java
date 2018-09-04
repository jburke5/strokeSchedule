package burke.schedule;

public enum Availability	{

	Unavailable(1, "Unavailable"), 
	Available(2, "Available"), 
	Possible(3, "Possible"), 
	Preferred(4, "Preferred"),		//state for fellow only
	Unknown(5, "Unknown");		
	
	private final int value;
	private final String label;
	
	Availability(int value, String label)	{
		this.value = value;
		this.label = label;
	}
	
	public static Availability get(String value)	{
		if (value.equals("1"))
			return Unavailable;
		else if (value.equals("2"))
			return Available;
		else if (value.equals("3"))
			return Possible;
		else if (value.equals("4"))
			return Preferred;
		else
			return Unknown;
	}
	
}