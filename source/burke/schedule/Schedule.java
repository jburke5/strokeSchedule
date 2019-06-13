package burke.schedule;

import java.util.*;
import java.io.*;

public class Schedule extends GregorianCalendar	{
	private static int maxShifts = -1;	//31 days in a month, and 2 shifts per day...
	private static int YEAR = -1;
	private static int MONTH = -1;
	
	private static int SHIFT_COUNT_FLOOR = 2;
	
	private Shift[] allShifts;
	
	public static Schedule MasterSchedule = null;
	
	private boolean master;
	

	public Schedule (int year, int month, boolean master)	{
		super(year, month, 1);
		this.YEAR = year;
		this.MONTH = month;
		initShifts();
		set(DAY_OF_MONTH, 1);	//reset to one after initializing shifts

		if (master)
			MasterSchedule = this;
		this.master = master;
	}
	
	public static Schedule getMasterSchedule()	{
		return MasterSchedule;
	}
	
	public Schedule(int year, int month)	{
		this(year, month, false);
	}
	
	public int countUnfilledShifts()	{
		int total = 0;
		for (int i = 0; i < allShifts.length; i++)	
			if (allShifts[i].isUnfilled())
				total++;
		return total;
	}
	
	public int countFilledShifts()	{
		int total = 0;
		for (int i = 0; i < allShifts.length; i++)	
			if (!allShifts[i].isUnfilled())
				total++;
		return total;
	}
	
	public int totalShiftCount()	{
		return allShifts.length;
	}
	
	public Schedule()	{
		this(Schedule.YEAR, Schedule.MONTH);
	}
	
	public void applyShifts()	{
		if (!master)
			copyShiftsFromMasterSchedule();
		
		
		if (PersonDirectory.getStaffedFellows().size() > 0)	{
			applyStaffedFellowPreferences();		//give the staffed fellow their preference shifts
		}
System.out.println("$$$$$$$$$		Print Schedule");
System.out.println(this);
		
		applyInvincibleShifts();		//first take the invincibles	
		applyRemainingShifts();			//then fill in the schedule for everybody else

		if (PersonDirectory.hasUnstaffedFellows() )	{
			applyUnstaffedFellowGaps();				//then have the unstaffed fellow fill in any gaps
			applyUnstaffedFellowPreferences();		//give the unstaffed fellow their preference shifts
		}
/*		Swap swap = findASwap(PersonDirectory.getNonInvinciblePeople());
		while (swap != null)	{
System.out.println("**Swap: " + swap.toString());
			swap.executeSwap();
			swap = findASwap(PersonDirectory.getNonInvinciblePeople());
		}
*/		Arrays.sort(allShifts);
	}
	
	private void copyShiftsFromMasterSchedule()	{
		Shift[] allMasterShifts = Schedule.MasterSchedule.getAllShifts();
		for (Shift masterShift : allMasterShifts)	{
			if (masterShift.getPerson() != null  && masterShift.getPerson().getTelestrokePreference().equals(TelestrokePreference.WITH))	{
				Shift newShift = getShift(masterShift.getDate(), masterShift.getAMPM());
				Person newPerson = PersonDirectory.getPerson(masterShift.getPerson().getFirstName(), masterShift.getPerson().getLastName());
				if (newPerson.isAvailableForShift(newShift) )
					newShift.assignPerson(newPerson);
			}
		}

	}
	
	private void applyStaffedFellowPreferences()	{		
		ArrayList<Person> fellows = PersonDirectory.getStaffedFellows();

		for (Person fellow : fellows)	{		
			//first give them all their preferred shifts if they didn't hit their cap doing mandatories
			for (int i = 0; i <  allShifts.length; i++)	{
				if (fellow.isPreferredForShift(allShifts[i]) )
					allShifts[i].assignStaffedFellow(fellow);
			}
		}

		ArrayList<Shift> shiftsToAssign = new ArrayList<Shift>(Arrays.asList(allShifts));
		Collections.shuffle(shiftsToAssign);
		
				
		for (Shift shift : shiftsToAssign)	{
			if (shift.hasStaffedFellowAssigned())
				continue;
			int weekInYear = Schedule.getMasterSchedule().getWeekYear() + shift.getWeekOfMonth();			
			int fellowForWeek = weekInYear % fellows.size();
			
			if (shift.isWeekdayDayShift())	{		//for weekday day shifts...assign one fellow to all shifts if available...if not try other fellow
				if (fellows.get(fellowForWeek).isAvailableForShift(shift))	{
					boolean fellowAssigned = shift.assignStaffedFellow(fellows.get(fellowForWeek));
					if (!fellowAssigned)
						shift.assignStaffedFellow(getFirstAvailablePerson(fellows, shift));
				}
			} else	{		//for all other shifts...assign fellows randomly based on availability.
				int availablePeople = countAvailablePeople(fellows, shift);
				if (availablePeople > 1)	{		//randomly assign
					int index = new Random().nextInt(fellows.size());	
					boolean fellowAssigned = shift.assignStaffedFellow(fellows.get(index));	
					
				} else if(availablePeople == 1)	
					shift.assignStaffedFellow(getFirstAvailablePerson(fellows, shift));
			}
		}
	}
	
	private Person getFirstAvailablePerson(List<Person> people, Shift shift)	{
		for (Person person : people)
			if (person.isAvailableForShift(shift))
				return person;
		throw new RuntimeException("No available person for shift: " + shift.toString() + " amongst people: " + people.toString());
	}
	
	private int countAvailablePeople(List<Person> people, Shift shift)	{
		int availableCount = 0;
		for (Person person : people)	{
			if (person.isAvailableForShift(shift))
				availableCount++;
		}
		return availableCount;
	}
	
	private void applyUnstaffedFellowPreferences()	{
		List<Person> fellows = PersonDirectory.getUnstaffedFellows();
		for (Person fellow : fellows)	{		
			//first give them all their preferred shifts if they didn't hit their cap doing mandatories
			//System.out.println("Starting preferred");
			for (int i = 0; i <  allShifts.length; i++)	{
				if (fellow.isPreferredForShift(allShifts[i]) )
					allShifts[i].assignPersonIfNotCapped(fellow);
			}
			//System.out.println("Ending preferred");
		
			//then give them available shifts until they hit their caps...which will assign weekday ams
			for (int i = 0; i <  allShifts.length; i++)	{
				if (fellow.isAvailableForShift(allShifts[i])  )
					allShifts[i].assignPersonIfNotCapped(fellow);
			}	
		}	
		
	}

	
	private class ShiftWeightComparator implements Comparator<Shift>	{
		public int compare(Shift a, Shift b) {
			return a.getWeight() < b.getWeight() ? 1 : a.getWeight() == b.getWeight() ? 0 : -1;
    	}
	}

		
	private void applyUnstaffedFellowGaps()	{
		List<Person> fellows = PersonDirectory.getUnstaffedFellows();
		for (Person fellow : fellows)	{
			for (int i = 0; i <  allShifts.length; i++)	
				if (allShifts[i].isUnfilled())	{
					if (fellow.isAvailableForShift(allShifts[i]))
						allShifts[i].assignPersonIfNotCapped(fellow); 
				}	
		}
	}
		
	private void applyRemainingShifts()	{
		//we're going to batch this into weekday ams vs. all other shifts.
		ArrayList<Shift> weekdayAMShifts = getWeekdayAMShifts();
		assignGroupOfShifts(weekdayAMShifts, PersonDirectory.getWeekdayAMStaffers());
		//if nobody in the weekday am list can cover it...try anybody...
		assignGroupOfShifts(weekdayAMShifts, PersonDirectory.getNonFellows());

		assignGroupOfShifts(getNonWeekdayAMShifts(), PersonDirectory.getNonFellows());
	}
	
	private ArrayList<Shift> getWeekdayAMShifts()	{
		Shift[] shifts = getAllShifts();
		ArrayList<Shift> weekdayAMs = new ArrayList<Shift>();
		for (Shift shift : shifts)	
			if (shift.isWeekdayDayShift())
				weekdayAMs.add(shift);
		return weekdayAMs;	
	}
	
	private ArrayList<Shift> getNonWeekdayAMShifts()	{
		ArrayList<Shift> allOtherShifts = new ArrayList<Shift>(Arrays.asList(getAllShifts()));
		allOtherShifts.removeAll(getWeekdayAMShifts());
		return allOtherShifts;
	}
	
	//apply a group of shifts across a group of people...
	private void assignGroupOfShifts(ArrayList<Shift> shifts, ArrayList<Person> peopleForComparison)	{
System.out.println("peopelForComparison: " + peopleForComparison.toString());
		//so priority for receiving shift = distance from goal for month + goal/availability mismatch
		//award shifts to people that are a high distance from their goal OR who hav a high goal availability mismatch
		//distance from goal = goal - proportion of current shifts covered for the month
		//goal/availability mismatch = goal - prportion of shifts that we hvaent' gotten to yet that your'e available for.

		//sort the shifts by the amount of availability...fill shifts from teh least available to the most available
		
		Collections.sort(shifts, new ShiftAvailabilityComparator(peopleForComparison));
		for (Shift shift : shifts)	
			if (shift.isUnfilled())
				fillShift(shift, shifts, peopleForComparison);
		
		//then assign possibles...
		for (Shift shift : shifts)	
			if (shift.isUnfilled())
				fillShift(shift, shifts, peopleForComparison, true);
		
		//System.out.println("*******		Before Equalization");
		//PersonDirectory.printWeights();			
	}

	
	private class ShiftAvailabilityComparator  implements Comparator<Shift>	{
		private ArrayList<Person> peopleForComparison;
		
		public ShiftAvailabilityComparator(ArrayList<Person> peopleForComparison) {
			this.peopleForComparison = peopleForComparison;
		}
	
		public int compare(Shift a, Shift b) {
			int availableA = countAvailablePeople( peopleForComparison, a);
			int availableB = countAvailablePeople(peopleForComparison, b);
			return availableA < availableB ? -1 : availableA == availableB ? 0 : 1;
    	}
	}

	
	private Swap findASwap(ArrayList<Person> people)	{
		ArrayList<Person> sortedPeople = new ArrayList<Person>(people);
		Shift[] nonWeekdayAMShifts = (Shift[]) getNonWeekdayAMShifts().toArray(new Shift[getNonWeekdayAMShifts().size()]);
		Collections.sort(sortedPeople, new TargetComparator(nonWeekdayAMShifts));
		
		ArrayList<Person> reversedSortedPeople = new ArrayList<Person>(sortedPeople);
		Collections.reverse(reversedSortedPeople);
		for (Person tradeFrom : reversedSortedPeople)	{	//count down from the person with the highest priority
			for (Person tradeTo : sortedPeople)	{		//count up from the person with teh lowest weight
				//System.out.println("Try to trade from: " + tradeFrom.toString() + " gap: " + tradeFrom.getDistanceFromTarget(allShifts));
				//System.out.println("Try to trade to: " + tradeTo.toString() + " gap: " + tradeTo.getDistanceFromTarget(allShifts));
				if (tradeTo.equals(tradeFrom))
					break;
				//for shifts where we're tradign beteween a person more than 15% over goal ane somebody under goal...look through shifts and see if you can swap any...
				if ((tradeFrom.getDistanceFromTarget(allShifts) < tradeFrom.getTarget()*-0.15 ) && (tradeTo.getDistanceFromTarget(allShifts) > 0))	{
					//System.out.println("in if...");
					List<Shift> tradeFromShifts = tradeFrom.getMyShifts();
					for (Shift possibleTradeShift : tradeFromShifts)	{
							//System.out.println(possibleTradeShift.toString());
							if (tradeTo.isAvailableForShift(possibleTradeShift))	{
								//System.out.println("trade to is available...");
								return new Swap(possibleTradeShift, tradeFrom, tradeTo);
							}
					}
				}
			}
		}
		return null;
	}

	
	private void fillShift(Shift shift, ArrayList<Shift> shiftGroup, ArrayList<Person> peopleForComparison)	{
		fillShift(shift, shiftGroup, peopleForComparison, false);	//default to just using available shfits
	}
	
	private void fillShift(Shift shift, ArrayList<Shift> shiftGroup, ArrayList<Person> peopleForComparison, boolean possibleOK)	{
//System.out.println("##########		trying to fill shift: " + shift);

//for (Person person : PersonDirectory.getNonInvinciblePeople())
	//System.out.println(person.getLastName() + " target: " + person.getTarget() + " priority: " + person.getPriority(allShifts) + " far behind: " + (person.getTarget() - person.getTotalAssignedWeight()  / person.totalAssignedWeightSoFar(allShifts)) + " gap: " + (person.getTarget() -  person.getRemainaingAvailableWeight(allShifts)/person.totalUnassignedWeightSoFar(allShifts)));
		
		Collections.sort(peopleForComparison, new PersonPriorityComparator((Shift[]) shiftGroup.toArray(new Shift[shiftGroup.size()])));	//put the people with the lowest weight first...					
		
		for (Person person : peopleForComparison)	{
//System.out.println(person.getLastName() + ": " + person.getPriority(allShifts));
			if (possibleOK)	{
				if (person.isPossibleForShift(shift) || person.isAvailableForShift(shift))	{
					shift.assignPerson(person);	
					break;
				}	
			} else	{
				if (person.isAvailableForShift(shift) )	{
					shift.assignPerson(person);
					break;
				}
			}
		}
	}

	private class PersonPriorityComparator implements Comparator<Person>	{
		private Shift[] shiftsForComparison;

		
		public PersonPriorityComparator(Shift[] shifts)	{
			this.shiftsForComparison = shifts;
		}
	
		//notSureTag: may need to reverse the sort order on this...i think this will get us high priority to low prioity
		public int compare(Person a, Person b) {	
			return a.getPriority(shiftsForComparison) < b.getPriority(shiftsForComparison) ? 1 : a.getPriority(shiftsForComparison) == b.getPriority(shiftsForComparison) ? 0 : -1;
    	}
	}

	private class TargetComparator implements Comparator<Person>	{
		private Shift[] shiftsForTargetComparison;
		
		public TargetComparator(Shift[] shiftsForTarget)	{
			this.shiftsForTargetComparison= shiftsForTarget;
		}
	
		public int compare(Person a, Person b) {	
			return a.getDistanceFromTarget(shiftsForTargetComparison) < b.getDistanceFromTarget(shiftsForTargetComparison) ? 1 : a.getDistanceFromTarget(shiftsForTargetComparison) == b.getDistanceFromTarget(shiftsForTargetComparison) ? 0 : -1;
    	}
	}
	
	private void applyInvincibleShifts()	{
		List<Person> invincibles = PersonDirectory.getInvinciblePeople();
		for (Person person : invincibles)	{
			for (Shift shift : person.getAllAvailableShifts())	{
				getShift(shift).assignPerson(person);	//overwrite the static shifts with invincibles...
			}
		}
	}

	protected Shift getShift(Shift shift)	{
		return getShift(shift.getDate(), shift.getAMPM());
	}

	protected Shift[] getAllShifts()	{
		return this.allShifts;
	}
	
	protected Shift getShift(int date, AMPM ampm)	{
		for(int i = 0; i < allShifts.length; i++)	
			if (allShifts[i].getDate() == date && allShifts[i].getAMPM().equals(ampm))
				return allShifts[i];		
		
		throw new RuntimeException("can't find shift for date: " + Integer.toString(date) + ampm.toString());	//if you dont' find anything default to null.
	}
	
	private boolean currentDayIsWeekend(AMPM ampm)	{
		return get(DAY_OF_WEEK) == SATURDAY || get(DAY_OF_WEEK) == SUNDAY || (get(DAY_OF_WEEK)==FRIDAY & ampm ==AMPM.PM) ? true : false;
	}
	
	public String toString()	{
		return Arrays.toString(allShifts);
	}
	
	public void printCSV(String path) throws Exception	{
		printCSV(path, "");
	}

	public void printCSV(String path, String suffix) throws Exception	{
		String finalPath = path + File.separator + "finalSchedule" + suffix + ".csv";
		FileWriter output = new FileWriter(finalPath);
		for (int i = 0; i < allShifts.length; i++)	{
			output.write(allShifts[i].getDate() + " - " + allShifts[i].getAMPM().toString());
			output.write(",");
			if (allShifts[i].getAssigned() == null)	{
				output.write("UNASSIGNED");
			}else	{
				if (!allShifts[i].noStaffedFellowAssigned())	{
					output.write(allShifts[i].getStaffedFellow().getLastName());
					output.write("/");
				}
				output.write(allShifts[i].getAssigned().getLastName());
			}
			output.write("\n");
		}
		output.flush();
		output.close();
	}
	
	private void initShifts()	{
		maxShifts = getActualMaximum(DAY_OF_MONTH) * 2; 	//am + pm
		allShifts = new Shift[maxShifts];
		
		for (int i = 0; i < getActualMaximum(DAY_OF_MONTH); i++)	{
			for (int time = 0; time < 2; time++)	{
				allShifts[i*2+time] = new Shift(get(DAY_OF_MONTH), get(DAY_OF_WEEK), AMPM.get(time),  currentDayIsWeekend(AMPM.get(time)), get(WEEK_OF_MONTH));
			}
			roll(DAY_OF_MONTH, true);
		}
	}
}