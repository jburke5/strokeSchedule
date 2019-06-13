package burke.schedule;

import java.util.*;

public class PersonDirectory	{	
	private static List<Person> allPeople = new ArrayList<Person>();
		
	public static Person addPerson(Person newPerson)	{
		allPeople.add(newPerson);
		return newPerson;
	}
	
	public static void clearDirectory()	{
		allPeople  = new ArrayList<Person>();
	}
	
	public static Person getPerson(String firstName, String lastName)	{
		Person bestMatch = null;
		int bestMatchCount = 0;
		for (Person next : allPeople)	{
			boolean firstMatches = next.getFirstName().toLowerCase().equals(firstName.toLowerCase());
			boolean lastMatches = next.getLastName().toLowerCase().equals(lastName.toLowerCase());
			int matchCount =  (int) (firstMatches ? 1 : 0) + (int) (lastMatches ? 1 : 0);
			if (matchCount > bestMatchCount)	{
				bestMatch = next;
				bestMatchCount = matchCount;
			} 
		}
		return bestMatch;
	}
	
	public static ArrayList<Person> getNonInvinciblePeople()	{
		ArrayList<Person> nonInvincibles = new ArrayList<Person>();
		for (Person person : allPeople)	{
			if (!person.isInvincible() & !person.isFellow())	
				nonInvincibles.add(person);
		} 
		return nonInvincibles;		
	}
	
	public static List<Person> getInvinciblePeople()	{
		List<Person> invincibles = new ArrayList<Person>();
		for (Person person : allPeople)	{
			if (person.isInvincible())	
				invincibles.add(person);
		} 
		return invincibles;
	} 
	
	public static ArrayList<Person> getAllPeople()	{
		ArrayList<Person> all = new ArrayList<Person>(allPeople);
		return all;
	} 
	
	public static ArrayList<Person> getNonFellows()	{
		ArrayList<Person> nonFellows = new ArrayList<Person>();
		for (Person person : getAllPeople())
			if (!person.isFellow())
				nonFellows.add(person);
		return nonFellows;		
	}
	
	public static ArrayList<Person> getStaffedFellows()	{
		ArrayList<Person> staffedFellows = new ArrayList<Person>();
		for (Person person : getAllPeople())
			if (person.isStaffedFellow())
				staffedFellows.add(person);
		return staffedFellows;		
	}
	
	public static boolean hasUnstaffedFellows()	{
		return getUnstaffedFellows().size() > 0;
	}

	public static List<Person> getUnstaffedFellows()	{
		Person fellow = null;
		List<Person> fellows = new ArrayList<Person>();
		for (Person person : getAllPeople())
			if (person.isFellow() & !person.isStaffedFellow())
				fellows.add(person);
		Collections.sort(fellows, new Comparator<Person>()	{
			public int	compare(Person o1, Person o2)	{
				return (o1.getFellowPriority() - o2.getFellowPriority());
			}

			public boolean	equals(Person obj)	{
				return false;
			}
		});
		return fellows;
	}
	
	public static ArrayList<Person> getWeekdayAMStaffers()	{
		ArrayList<Person> weekdayAMStaffers = new ArrayList<Person>();
		for (Person person : getAllPeople())
			if (person.isWeekdayAMStaffer())
				weekdayAMStaffers.add(person);
		return weekdayAMStaffers;		

	}
		
	public static void printDirectory()	{
		for (Person next : allPeople)	{
			System.out.println(next.toString());
			System.out.println();
		}
	}
	
	public static int checkForDuplicateShifts()	{
		int duplicateShiftCount = 0;
		List<Shift> allShifts = new ArrayList<Shift>();
		Set<Shift> uniqueShifts = new HashSet<Shift>();
		for (Person person : allPeople)	{
			if (!person.isStaffedFellow())	{
				allShifts.addAll(person.getMyShifts());
				uniqueShifts.addAll(person.getMyShifts());
			}
		}
		Collections.sort(allShifts);
		Shift nextShift = null;
		for (Shift shift : allShifts)	{
			if (shift.equals(nextShift))	{
				System.out.println("Duplicate Shift: " + shift + " AND " + nextShift);
				duplicateShiftCount++;
			}
			nextShift = shift;
		}
		return duplicateShiftCount;
	}
	
	public static void printPeople()	{
		for (Person newPerson : allPeople)
			System.out.println("adding person: " + newPerson.getLastName() + " invincible: " + newPerson.isInvincible() + " fellow: " + newPerson.isFellow() + " staffed: " + newPerson.isStaffedFellow() + " target: " + newPerson.getTarget() );

	}
	
	public static int printWeights(Schedule schedule)	{
		System.out.println("\n\n###Final Weights###\n\n");
		int totalShiftCount = 0;
		int totalWeight = 0;
		Map<String, Integer> shiftsForName = new HashMap<String, Integer>();
		Map<String, Double> weightsForName = new HashMap<String, Double>();
		
		for (Person person : allPeople)	{
			if (!person.isStaffedFellow())	{
				double priorWeight = weightsForName.get(person.getLastName()) == null ? 0 : weightsForName.get(person.getLastName());
				int priorShifts = shiftsForName.get(person.getLastName()) == null ? 0 : shiftsForName.get(person.getLastName());
				
				totalShiftCount += person.getShiftCount();
				totalWeight += person.getTotalAssignedWeight(schedule.getAllShifts());
				
				weightsForName.put(person.getLastName(), priorWeight + person.getTotalAssignedWeight(schedule.getAllShifts()));
				shiftsForName.put(person.getLastName(), priorShifts + person.getShiftCount());
			}
		}
		for (String name : shiftsForName.keySet())	{
			double weightForPerson = weightsForName.get(name);
			int shiftsForPerson = shiftsForName.get(name);
			double percentShiftsForPreson = shiftsForPerson*100.0 /totalShiftCount;
			double percentWeightForPerson = weightForPerson / totalWeight * 100;
		
			System.out.printf("%s  : %.2f  ( %.2f%% )  shift count: %d ( %.2f%% ) %n", name, weightForPerson, percentWeightForPerson, shiftsForPerson, percentShiftsForPreson); 
		}
		System.out.println("Total Shift Count: " + totalShiftCount);
		System.out.println("Total  Weight: " + totalWeight);
		return totalShiftCount;
	}
	
}
