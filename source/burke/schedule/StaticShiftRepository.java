package burke.schedule;

import java.io.*;
import java.util.*;

public class StaticShiftRepository	{
	private static String fileName = "staticShifts.csv";
	//a little messy weeks in the month go from 1-5
	//days in a week go from 1-7, thus the need for 8 entries (the zero is in there, but should be empty)
	private Person[][][] repository = new Person[6][8][2];	
	private HashMap<String,Integer> calDaysForStringDays = new HashMap<String,Integer>();

	
	public StaticShiftRepository(String reposPath) throws Exception	{
		setupMap();
		loadShiftFile(reposPath);
	}
	
	private void printShiftRepository()	{
		System.out.println("Sunday: " + Calendar.SUNDAY);
		System.out.println("Monday: " + Calendar.MONDAY);
		for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++)	{
			System.out.println("dayOfWeek: " + dayOfWeek);
			for (int week = 1; week <= 5; week++)	{
				System.out.println("week: " + week + " ");
				for (int ampm = 0; ampm < 2; ampm++)	{
					System.out.print(AMPM.get(ampm).toString() + " " );
					if (hasStaticShift(week, dayOfWeek, AMPM.get(ampm)))
						System.out.print(repository[week][dayOfWeek][ampm].getLastName() + " ");
					else
						System.out.print(" ");

				}
				System.out.println();
			}
		}
	}
	
	public boolean hasStaticShift(int weekOfMonth, int dayOfWeek, AMPM ampm)	{
		return repository[weekOfMonth][dayOfWeek][ampm.getIndex()] != null;
	}
	
	public Person getPerson(int weekOfMonth, int dayOfWeek, AMPM ampm)	{
		return repository[weekOfMonth][dayOfWeek][ampm.getIndex()];
	}
	
	private void setupMap()	{
		calDaysForStringDays.put("monday", Calendar.MONDAY);
		calDaysForStringDays.put("tuesday", Calendar.TUESDAY);
		calDaysForStringDays.put("wednesday", Calendar.WEDNESDAY);
		calDaysForStringDays.put("thursday", Calendar.THURSDAY);
		calDaysForStringDays.put("friday", Calendar.FRIDAY);
		calDaysForStringDays.put("saturday", Calendar.SATURDAY);
		calDaysForStringDays.put("sunday", Calendar.SUNDAY);
	}
	
	private AMPM getAMPM(String val)	{
		return val.toLowerCase().equals("am") ? AMPM.AM : AMPM.PM;
	}

	private void loadShiftFile(String reposPath) throws Exception	{
		String path = reposPath + File.separator + fileName;
		BufferedReader reader = new BufferedReader(new FileReader(path));
		
		String nextLine = reader.readLine();	//the header line...throw away
		nextLine = reader.readLine();	//the first line with content	
		while (nextLine != null){
			nextLine = nextLine.trim();
			String[] lineElements = nextLine.split(",");
			int dayOfWeek = calDaysForStringDays.get(lineElements[0].toLowerCase().trim());
			AMPM ampm = getAMPM(lineElements[1]);
			
			int maxWeeks = lineElements.length - 2;
			for(int week = 1; week <= maxWeeks; week++)	{
				String name = lineElements[1+week];
				repository[week][dayOfWeek][ampm.getIndex()] = PersonDirectory.getPerson(name, name);
			}
			
			nextLine = reader.readLine();
		}
		reader.close();
	}
}