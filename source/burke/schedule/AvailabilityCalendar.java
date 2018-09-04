package burke.schedule;

import java.util.*;

public class AvailabilityCalendar extends Schedule	{
	
	Map<Shift, Availability> calendar = new HashMap<Shift, Availability>();
	
	public AvailabilityCalendar()	{
		super();
	}
	
	public void setAvailability(Availability avail, int date, AMPM ampm)	{
		calendar.put(getShift(date, ampm), avail);
	}
	
	public boolean isAvailableForShift(Shift shift)	{
		Availability avail = calendar.get(shift);
		return avail != null && ( avail.equals(Availability.Available) || avail.equals(Availability.Preferred));
	}
	
	public boolean isPreferredForShift(Shift shift)	{
		Availability avail = calendar.get(shift);
		return avail != null && avail.equals(Availability.Preferred);
	}

	public boolean isPossibleForShift(Shift shift)	{
		Availability avail = calendar.get(shift);
		return avail != null && avail.equals(Availability.Possible);
	}
	
	public Set<Shift> getAllAvailableShifts()	{
		Set<Shift> availableShifts = new TreeSet<Shift>();
		
		Set<Shift> allShifts =  calendar.keySet();
		for (Shift shift : allShifts)	
			if (calendar.get(shift).equals(Availability.Available))	
				availableShifts.add(shift);
		return availableShifts;	
	}
	
	public String toString()	{
		StringBuffer buf = new StringBuffer();
		Set<Shift> keys = calendar.keySet();
		
		TreeSet<Shift> sortedKeys = new TreeSet<Shift>(keys);
		for (Shift shift : sortedKeys)	{
			buf.append(shift.toString());
			buf.append(" - ");
			buf.append(calendar.get(shift).toString());
			buf.append(" ");
		}
		return buf.toString();
		
	}

}