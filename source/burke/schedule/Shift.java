package burke.schedule;

public class Shift implements Comparable<Shift>	{

	
	private int date;
	private int dayOfWeek;
	private int weekOfMonth;
	private AMPM ampm;
	private boolean weekend;
	protected Person assigned;
	protected Person staffedFellowAssigned;

	public Shift(int date, int dayOfWeek, AMPM ampm, boolean weekend, int weekOfMonth)	{
		this.date = date;
		this.dayOfWeek = dayOfWeek;
		this.ampm = ampm;
		this.weekend = weekend;
		this.weekOfMonth = weekOfMonth;
	}
		
	public int getDate()	{
		return date;
	}

	public AMPM getAMPM()	{
		return ampm;
	}
	
	public int getWeekOfMonth()	{
		return this.weekOfMonth;
	}
	
	public boolean isWeekdayDayShift()	{
		return !isWeekend() & getAMPM().equals(AMPM.AM);
	}
	
	public boolean isWeekdayNightShift()	{
		return !isWeekend() & getAMPM().equals(AMPM.PM);
	}
	
	public boolean isWeekend()	{
		return weekend;
	}
	
	public boolean isUnfilled()	{
		return assigned == null;
	}
	
	public boolean noStaffedFellowAssigned()	{
		return staffedFellowAssigned == null;
	}
	
	public Person getStaffedFellow()	{
		return staffedFellowAssigned;
	}
	
	public Person getAssigned()	{
		return assigned;
	}
	
	public boolean invincibleAssigned()	{
		boolean invincibleAssigned = false;
		if (getPerson() != null)
			if (getPerson().isInvincible())
				invincibleAssigned = true;
		
		return invincibleAssigned;
	}
	
	
	public void assignPerson(Person person)	{
		if ((person.isFellow() & noStaffedFellowAssigned() & !invincibleAssigned()) | (!person.isFellow() & !invincibleAssigned()))	{	//don't let an unstaffed fellow staff a staffed fellow
System.out.println("assigning person: " + person.getLastName() + " to shift: " + this);
			if (getPerson() != null)	{
System.out.println("Unassigning person: " + getPerson().getLastName() + " from shift: " + toString());
				getPerson().unassignShift(this);
			}
			this.assigned = person;
			person.assignShift(this);
		}
	}
	
	public boolean assignStaffedFellow(Person person)	{
		boolean fellowAssigned = false;

		if (!isCappedForThisShift(person))	{
			this.staffedFellowAssigned = person;
			person.assignShift(this);
			fellowAssigned = true;
		}
		return fellowAssigned;
	}
	
	public boolean hasStaffedFellowAssigned()	{
		return this.staffedFellowAssigned != null;
	}
	
	private boolean isCappedForThisShift(Person person)	{
		if (!isWeekend() && getAMPM().equals(AMPM.PM) && !person.isAtWeeknightCap() )	//assign to weeknight if weeknight and not at cap
			return false;
		else if (isWeekend() & !person.isAtWeekendCap())		//assign to weekend if not at cap
			return false; 
		else if (!isWeekend() & getAMPM().equals(AMPM.AM))		//assign to day if available
			return false;
		else
			return true;
	}
	
	public void assignPersonIfNotCapped(Person person)	{
		if (!isCappedForThisShift(person))
			assignPerson(person);
	}
	
	public Person getPerson()	{
		return assigned;
	}
	
	public double getWeight()	{
		if (isWeekend())	{
			return 2.5;
		} else if (getAMPM().equals(AMPM.PM))	{
			return 1.79;
		} else	{
			return 1.0;
		}
	}
	
	
	public int compareTo(Shift other)	{
		if (other == null)
			return -1;
				
		if (getDate() < other.getDate())
			return -1;
		else if (getDate() == other.getDate() && getAMPM() == AMPM.AM && other.getAMPM() == AMPM.PM)
			return -1;
		else if (getDate() == other.getDate() && getAMPM() == other.getAMPM())
			return 0;
		else
			return 1;
	}
	
	public String toString()	{
		StringBuffer buf = new StringBuffer();
		buf.append(new Integer(date).toString());
		buf.append(" ");
		buf.append(ampm.toString());
		buf.append(" ");
		if (staffedFellowAssigned != null)	{
			buf.append(staffedFellowAssigned.getLastName());
			buf.append("/");
		}
		if (assigned != null)
			buf.append(assigned.getLastName());
		//buf.append("\n");
		return  buf.toString();
	}
	
	public int hashCode()	{
		return date * 2 + ampm.getIndex(); 
	}
	
	public boolean equals(Object other)	{
		if (other == null)
			return false;
		
		Shift otherShift = (Shift) other;
		
		return date == otherShift.getDate() && ampm.equals(otherShift.getAMPM());
	}
	
}