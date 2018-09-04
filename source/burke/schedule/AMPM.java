package burke.schedule;

public enum AMPM	{
	AM(0), PM(1);
	
	private final int index;
	
	private AMPM(int index)	{
		this.index = index;
	}
	
	public int getIndex()	{
		return index;
	}
	
/*	public static AMPM valueOf(String name)	{
		return name.toLowerCase().equals("am") ? AM : PM;
	}
*/	
	public static AMPM get(int val)	{
		return val==0 ? AM : PM;
	}
}