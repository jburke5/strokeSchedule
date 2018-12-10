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

	//logically, this seems like it should be a singleton...there is only one schedule...that's what the app makes — clean this up...
	public Schedule (int year, int month, boolean master)	{
		this(year, month);
		MasterSchedule = this;
	}
	
	public static Schedule getMasterSchedule()	{
		return MasterSchedule;
	}
	
	public Schedule(int year, int month)	{
		super(year, month, 1);
		this.YEAR = year;
		this.MONTH = month;
		initShifts();
		set(DAY_OF_MONTH, 1);	//reset to one after initializing shifts
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
		if (PersonDirectory.getStaffedFellows().size() > 0)	{
			applyStaffedFellowPreferences();		//give the staffed fellow their preference shifts
		}
//System.out.println("$$$$$$$$$		Print Schedule");
//System.out.println(this);
		
		applyInvincibleShifts();		//first take the invincibles
		applyRemainingShifts();			//then fill in the schedule for everybody else
		
		if (PersonDirectory.hasUnstaffedFellows() )	{
			applyUnstaffedFellowGaps();				//then have the unstaffed fellow fill in any gaps
			applyUnstaffedFellowPreferences();		//give the unstaffed fellow their preference shifts
		}
		fillUpMinimumShifts();		//for non-invincibles...try to get them up to 2 shifts per month — mainly this will mean taking from fellows...
//System.out.println("about to equalize shifts");
		equalizeShifts();		
	}
	
	private void applyStaffedFellowPreferences()	{		//this is easy...just give them all of their availabl.es...
		ArrayList<Person> fellows = PersonDirectory.getStaffedFellows();
		for (int i = 0; i < allShifts.length; i++)	{
			int availablePeople = countAvailablePeople(fellows, allShifts[i]);
			if (availablePeople > 1)	{		//randomly assign
				int index = new Random().nextInt(fellows.size());	
				allShifts[i].assignStaffedFellow(fellows.get(index));	
			} else if(availablePeople == 1)	
				allShifts[i].assignStaffedFellow(getFirstAvailablePerson(fellows, allShifts[i]));
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
		for (Person person : people)	
			if (person.isAvailableForShift(shift))
				availableCount++;
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
	
	private void fillShiftsToMinimum(Person slacker)	{
//System.out.println("filling minimum shifts for : " + slacker.getLastName());
		ArrayList<Shift> shifts = new ArrayList<Shift>(slacker.getAllAvailableShifts());
		Collections.sort(shifts, new ShiftWeightComparator());
		for (Shift shift : shifts)
			if (slacker.getShiftCount() < SHIFT_COUNT_FLOOR)
				getShift(shift.getDate(),  shift.getAMPM()).assignPerson(slacker);		//need to get the shift from the schedule not from the person...kind of ugly...
//System.out.println("done filling minimum shifts for : " + slacker.getLastName());
	}
	
	private class ShiftWeightComparator implements Comparator<Shift>	{
		public int compare(Shift a, Shift b) {
			return a.getWeight() < b.getWeight() ? 1 : a.getWeight() == b.getWeight() ? 0 : -1;
    	}
	}
	
	private void fillUpMinimumShifts()	{
//System.out.println("Filling shifts to minimum...");
		ArrayList<Person> people = 	PersonDirectory.getNonFellows();
		for (Person person : people)	
			if (person.getShiftCount() <= SHIFT_COUNT_FLOOR)
				fillShiftsToMinimum(person);		
//System.out.println("Done filling shifts to minimum...");
	}
	
	//remember to take the fellow out of the equalization lists...
	private void equalizeShifts()	{
		//then apply an equalization algorithm
		ArrayList<Person> people = 	PersonDirectory.getNonFellows();
		double currentSD = meanSDWeights();
		double lastSD = 100;
//System.out.println("currentSD: " + currentSD + " lastSD: " + lastSD);
		while (currentSD < lastSD)	{
			Collections.sort(people);
			Swap swap = lookForASwap(people);
			if (swap != null)	{
				swap.executeSwap();
			}
			if (meanSDWeights() > currentSD)
				swap.reverseSwap();
			
			lastSD = currentSD;
			currentSD = meanSDWeights();
//System.out.println("currentSD: " + currentSD + " lastSD: " + lastSD);
		}

		//then apply an equalization algorithm including possible shifts..
//System.out.println("**********Now considering possibles...");
//System.out.println("currentSD: " + currentSD + " lastSD: " + lastSD);
		lastSD = 100; 	//reset this so that we loop at least once...
		while (currentSD < lastSD * .9)	{		//only do a possible if it makes it much more fair...
			Collections.sort(people);
			Swap swap = lookForASwap(people, true);
			if (swap != null)	
				swap.executeSwap();
	
			if (meanSDWeights() > currentSD)
				swap.reverseSwap();
			lastSD = currentSD;
			currentSD = meanSDWeights();
//System.out.println("currentSD: " + currentSD + " lastSD: " + lastSD);
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
		//first fill in the availables
		for (int i = 0; i <  allShifts.length; i++)	{
			if (allShifts[i].isUnfilled())
				fillShift(allShifts[i]);
		}
		
		//then assign possibles...
		for (int i = 0; i <  allShifts.length; i++)	{
			if (allShifts[i].isUnfilled())
				fillShift(allShifts[i], true);
		}	
		
		//System.out.println("*******		Before Equalization");
		//PersonDirectory.printWeights();		
	}
	
	private Swap lookForASwap(ArrayList<Person> people)	{
		return lookForASwap(people, false);
	}
	
	private Swap lookForASwap(ArrayList<Person> people, boolean possibleOK)	{
		for (int tradeFromCount = people.size()-1; tradeFromCount >= 1; tradeFromCount--)	{	//count down from the person with the highest weight 
			for (int tradeToCount = 0; tradeToCount < tradeFromCount; tradeToCount++)	{		//count up from the person with teh lowest weight
				Person tradeFrom = people.get(tradeFromCount);
				Person tradeTo = people.get(tradeToCount);
				
				List<Shift> tradeFromShifts = tradeFrom.getMyShifts();
				for (Shift possibleTradeShift : tradeFromShifts)	
					if (possibleOK)	
						if (tradeTo.isAvailableForShift(possibleTradeShift) || tradeTo.isPossibleForShift(possibleTradeShift))	
							return new Swap(possibleTradeShift, tradeFrom, tradeTo);
											
					 else	
						if (tradeTo.isAvailableForShift(possibleTradeShift))	
							return new Swap(possibleTradeShift, tradeFrom, tradeTo);		
			}
		}
		return null;
	}
	
	private double meanSDShifts()	{
		ArrayList<Person> people = 	PersonDirectory.getAllPeople();
		double total = 0.0;
		for (Person person : people)	
			total += person.getShiftCount();
		
		double mean = total / people.size();
		double totalSD = 0.0;
		for (Person person : people)	{
			double sd = Math.sqrt(Math.pow((person.getShiftCount() - mean),2));
			totalSD += sd;
		}
		
		return totalSD / people.size();
		
	}
	
	private double meanSDWeights()	{
		ArrayList<Person> people = 	PersonDirectory.getAllPeople();
		double total = 0.0;
		for (Person person : people)	
			total += person.getAverageWeight();
		
		double mean = total / people.size();
		double totalSD = 0.0;
		for (Person person : people)	{
			double sd = Math.sqrt(Math.pow((person.getAverageWeight() - mean),2));
			totalSD += sd;
		}
		
		return totalSD / people.size();

	}
	
	private void fillShift(Shift shift)	{
		fillShift(shift, false);	//default to just using available shfits
	}
	
	private void fillShift(Shift shift, boolean possibleOK)	{
//System.out.println("##########		trying to fill shift: " + shift);
		ArrayList<Person> people = PersonDirectory.getNonInvinciblePeople();
		Collections.sort(people);	//put the people with the lowest weight first...					
		
		for (Person person : people)	{
//System.out.println(person.getLastName() + ": " + person.getAverageWeight());
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
	
	private void applyInvincibleShifts()	{
		List<Person> invincibles = PersonDirectory.getInvinciblePeople();
		for (Person person : invincibles)	{
			for (Shift shift : person.getAllAvailableShifts())	{
				getShift(shift).assignPerson(person);	//overwrite the static shifts with invincibles...
			}
		}
	
//System.out.println(this.toString());
	}

	protected Shift getShift(Shift shift)	{
		return getShift(shift.getDate(), shift.getAMPM());
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
		String finalPath = path + File.separator + "finalSchedule.csv";
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