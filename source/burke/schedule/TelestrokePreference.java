package burke.schedule;

public enum TelestrokePreference	{
	NONE(0), WITH(1), SEPARATE(2);
	
	private final int index;
	
	private TelestrokePreference(int index)	{
		this.index = index;
	}
	
	public int getIndex()	{
		return index;
	}
	
/*	public static AMPM valueOf(String name)	{
		return name.toLowerCase().equals("am") ? AM : PM;
	}
*/	
	public static TelestrokePreference get(int val)	{
		if (val == 0)
			return NONE;
		else if (val == 1)
			return WITH;
		else 
			return SEPARATE;
	}
}