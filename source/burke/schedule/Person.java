package burke.schedule;

import java.util.*;

//should have made this protected so that only PersonDirectory can access it...
public class Person	implements  Comparable<Person> {
	private static int WEEKNIGHT_FELLOW_CAP = 4;
	private static int WEEKEND_FELLOW_CAP = 6;


	private String firstName;
	private String lastName;
	private boolean invincible;
	private boolean staticOverride;
	private boolean fellow;
	private boolean staffed;
	private AvailabilityCalendar availability;
	private List<Shift> assignedShifts = new ArrayList<Shift>();
	private int fellowPriority;
	private double target;
	private TelestrokePreference telestrokePreference;
	private boolean weekdayTelestroke;
	
	public Person(String firstName, String lastName, boolean invincible, boolean staticOverride, boolean fellow, boolean staffed, int fellowPriority, double target, int telestrokePreference, boolean weekdayPreference)	{
		this.firstName = firstName;
		this.lastName = lastName;
		this.fellow = fellow;
		this.invincible = invincible;
		this.staticOverride = staticOverride;
		this.availability = new AvailabilityCalendar();
		this.staffed = staffed;
		this.fellowPriority = fellowPriority;
		this.target = target;
		this.telestrokePreference = TelestrokePreference.get(telestrokePreference);
		this.weekdayTelestroke = weekdayPreference;
		if (this.staffed & !this.fellow)
			throw new RuntimeException("Invalid Person Description for " + lastName); 
	}
	
	public boolean isWeekdayAMStaffer()	{
		return weekdayTelestroke;
	}
	
	public boolean isFellow()	{
		return fellow;
	}
	
	public boolean isStaffedFellow()	{
		return staffed;
	}
	
	public TelestrokePreference getTelestrokePreference()	{
		return this.telestrokePreference;
	}
	
	public void setAvailablility(Availability avail, int date, AMPM ampm)	{
		availability.setAvailability(avail, date, ampm);
	}
	
	public Set<Shift> getAllAvailableShifts()	{
		return availability.getAllAvailableShifts();
	}
	
	public boolean isAvailableForShift(Shift shift)	{
		return availability.isAvailableForShift(shift);
	}
	
	public boolean isPossibleForShift(Shift shift)	{
		return availability.isPossibleForShift(shift);
	}
	
	public boolean isPreferredForShift(Shift shift)	{
		return availability.isPreferredForShift(shift);
	}
	
	public boolean isInvincible()	{
		return invincible;
	}
	
	public boolean staticOverride()	{
		return staticOverride;
	}
	
	public String getFirstName()	{
		return firstName;
	}
	
	public String getLastName()	{
		return lastName;
	}
	
	public String toString()	{
		return firstName + " " + lastName ;
	}
	
	public double getDistanceFromTarget(Shift[] allShifts)	{
		int totalWeight = 0;
		for (Shift shift : allShifts)
			totalWeight += shift.getWeight();
		return getTarget() - getTotalAssignedWeight() / totalWeight;
	}
	
	public void assignShift(Shift shift)	{
		assignedShifts.add(shift);
		//totalAssignedWeight += shift.getWeight();
	}
	
	public void unassignShift(Shift shift)	{
		boolean removed = assignedShifts.remove(shift);
		if (!removed)
			throw new RuntimeException("Trying to remove shift that is not assigned to person");
	}
	
	protected double getRemainingWeight()	{
		double remaining = 0.0;
		Set<Shift> available = getAllAvailableShifts();
		
		int unfilled = Schedule.getMasterSchedule().countUnfilledShifts();
		int filled = Schedule.getMasterSchedule().countFilledShifts();

		int approxDaysChecked = unfilled/(unfilled + filled) * 31;
		for (Shift shift : available)
			if (shift.getDate() > approxDaysChecked)
				remaining += shift.getWeight();
		return remaining;
	}
	
	public List<Shift> getMyShifts()	{
		return this.assignedShifts;
	}
	
	public int getShiftCount()	{
		return getMyShifts().size();
	}
	
	public int getNightShiftCount()	{
		int count = 0;
		for (Shift shift : assignedShifts)
			if (shift.isWeekdayNightShift())
				count++;
		return count;
	}
	
	public boolean isAtWeeknightCap()	{
		return getNightShiftCount() >= WEEKNIGHT_FELLOW_CAP;
	}
	
	public boolean isAtWeekendCap()	{
		return getWeekendShiftCount() >= WEEKEND_FELLOW_CAP;
	}
	
	public int getWeekendShiftCount()	{
		int count = 0;
		for (Shift shift : assignedShifts)
			if (shift.isWeekend())
				count++;
		return count;
	}
	
	private double getAverageTarget()	{
		List<Person> people = PersonDirectory.getNonInvinciblePeople();
		double totalNonZeroTarget = 0.0;
		int totalNonZeroCount = 0;
		for (Person person : people)
			if (!(new Double(person.target).equals(0.0)))	{
				totalNonZeroTarget += person.target;
				totalNonZeroCount++;
			}
		
		return (1.0 - totalNonZeroTarget) / (numPeopleEligibleForCall() - totalNonZeroCount);
	}
	
	private int numPeopleEligibleForCall()	{
		int count = 0;
		for (Person person : PersonDirectory.getNonInvinciblePeople())
			if (person.hasAnyAvailability())
				count++;
		return count;
	}
	
	private boolean hasAnyAvailability()	{
		return this.availability.getAllAvailableShifts().size() > 0;
	}
	
	public double getTarget()	{
		double returnVal = target;
		if (new Double(target).equals(0.0))
			returnVal = getAverageTarget();
		return returnVal;
	}
	
	public double getPriority(Shift[] allShifts)	{
		double howFarBehindGoal = getTarget() - getTotalAssignedWeight()/totalAssignedWeightSoFar(allShifts);
		double targetAvailabilityGap = getTarget() - getRemainaingAvailableWeight(allShifts)/totalUnassignedWeightSoFar(allShifts);
		
		double filledShifts = 0;
		double unfilledShifts = 0;
		for (int i = 0; i < allShifts.length; i++)	{
			if (allShifts[i].isUnfilled())
				unfilledShifts++;
			else
				filledShifts++;
		}
		
		return 	howFarBehindGoal*(filledShifts/(filledShifts+unfilledShifts)) + targetAvailabilityGap *(unfilledShifts/(unfilledShifts + filledShifts));
	}
	
	public double totalAssignedWeightSoFar(Shift[] allShifts)	{
		double total = 0.0;
		for (int i = 0; i < allShifts.length; i++)
			if (!allShifts[i].isUnfilled() & !allShifts[i].invincibleAssigned())
				total += allShifts[i].getWeight();
		return total;
	}
	
	public double totalUnassignedWeightSoFar(Shift[] allShifts)	{
		double total = 0.0;
		for (int i = 0; i < allShifts.length; i++)
			if (allShifts[i].isUnfilled())
				total += allShifts[i].getWeight();
		return total;
	}
	
	public double getRemainaingAvailableWeight(Shift[] allShifts)	{
		double total = 0.0;
		for (int i = 0; i < allShifts.length; i++)
			if (allShifts[i].isUnfilled() && availability.isAvailableForShift(allShifts[i]))
				total += allShifts[i].getWeight();
		return total;
	}
	
	public double getTotalAssignedWeight()	{
		double assignedWeight = 0.0;
		for (Shift shift : assignedShifts)	{
			if (shift.getAssigned() != null)
				if (shift.getAssigned().equals(this))	
					assignedWeight += shift.getWeight();
		}
		return assignedWeight;
	}

	protected double getAverageWeight()	{
		int unfilled = Schedule.getMasterSchedule().countUnfilledShifts();
		int filled = Schedule.getMasterSchedule().countFilledShifts();
		
		double percentUnfilled = (double) unfilled / (unfilled + filled);
		double percentFilled = (double) filled / (unfilled + filled);
		return getTotalAssignedWeight() * percentFilled + getRemainingWeight() * percentUnfilled;
	}
	
	public int getFellowPriority()	{
		return this.fellowPriority;
	}
	
	public int compareTo(Person other)	{
		if (other == null || !other.getClass().equals(Person.class))	{
			throw new RuntimeException("Invalid comparable");
		}
		
		return (int) ((this.getAverageWeight() - ((Person) other).getAverageWeight()) * 100);
	}

}