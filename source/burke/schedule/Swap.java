package burke.schedule;

public class Swap	{
	private Person from;
	private Person to;
	private Shift shift;

	public Swap(Shift shift, Person from, Person to)	{
		this.from = from;
		this.to = to;
		this.shift = shift;
	}
	
	public void executeSwap()	{
		shift.assignPerson(to);
	}
	
	public void reverseSwap()	{
		shift.assignPerson(from);
	}
	
}