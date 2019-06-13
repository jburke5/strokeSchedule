package burke.schedule;

import java.util.*;

public class Main	{
	private String shiftDirectory = "/Users/burke/Documents/stroke schedules/automated";

	public Main()	{}

	public void buildSchedule(int year, int month) throws Exception	{
		Schedule schedule = new Schedule(year, month, true); //the one and true master schedule...
		
		ScheduleReader reader = new ScheduleReader(shiftDirectory);
		reader.load(year, month);
		//dump the list of all shifts...
		ArrayList<Person> people = PersonDirectory.getNonInvinciblePeople();
		PersonDirectory.printPeople();
		//for (Person person : people)	
		//	System.out.println(person.toString() + " \n" + person.getAllAvailableShifts());

		schedule.applyShifts();
		int duplicates = PersonDirectory.checkForDuplicateShifts();
		if (duplicates > 0)
			throw new RuntimeException("Duplciate Shifts exist — Fix this!!!");

		int totalPersonShiftCount = PersonDirectory.printWeights();
		if (totalPersonShiftCount != schedule.totalShiftCount())
			System.out.println("Unstaffed shifts: Shift count assigned to people (" +totalPersonShiftCount + ") toal different than total shift count (" + schedule.totalShiftCount() + ")");
		schedule.printCSV(shiftDirectory);
		
		
		Schedule telestrokeSchedule = new Schedule(year, month, false); //the telestroke schedule
		
		ScheduleReader tsReader = new ScheduleReader(shiftDirectory);
		tsReader.load(year, month, "ts");
		telestrokeSchedule.applyShifts();
		telestrokeSchedule.printCSV(shiftDirectory, "ts");



		int totalPersonShiftCountTS = PersonDirectory.printWeights();
		if (totalPersonShiftCountTS != schedule.totalShiftCount())
			System.out.println("Telestroke Unstaffed shifts: Shift count assigned to people (" +totalPersonShiftCount + ") toal different than total shift count (" + schedule.totalShiftCount() + ")");
		
	}	

	public static void main(String[] args)	{
		try	{
			int year = Integer.valueOf(args[0]);
			int month = Integer.valueOf(args[1]) - 1;		//calendar has zero indexed months...which is crazytime...
			new Main().buildSchedule(year, month);
		} catch(Exception ex)	{
			ex.printStackTrace();
		}
	}
}