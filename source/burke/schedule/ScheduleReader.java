package burke.schedule;

import java.io.*;
import java.text.*;
import java.util.*;

//now this is a crappy class name...this shoudl be InputFileReader or AvailabiltityReader...
public class ScheduleReader	{
	private String path;
	private Map<Integer, Person> peopleForIndices = new HashMap<Integer, Person>();

	public ScheduleReader(String path)	{
		this.path = path;
	}
	
	public void load(int year, int month, String scheduleNameSuffix) throws Exception	{
		String filePath = path + File.separator +  new DateFormatSymbols().getMonths()[month] + Integer.toString(year) + scheduleNameSuffix +  ".csv";
		System.out.println("file path: " + filePath);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		loadPeople(reader);
		//reader.readLine(); 	//burn the blank line
		loadAvailability(reader, year, month);
	}
	
	public void load(int year, int month) throws Exception	{
		load(year, month, "");
	}
	
	private void loadPeople(BufferedReader reader) throws Exception	{
		PersonDirectory.clearDirectory();
		String firstNameLine = reader.readLine();
		String lastNameLine = reader.readLine();
		String invincibleLine = reader.readLine();
		String staticOverrideLine = reader.readLine();
		String fellowLine = reader.readLine();
		String staffedLine = reader.readLine();
		String targetLine = reader.readLine(); 	//the target percentage to aim for
		String tsLine = reader.readLine(); 	//the target percentage to aim for
		String weekdayLine = reader.readLine(); 	
		String invincibleTsWeekdayLine = reader.readLine();
		//System.out.println(invincibleTsWeekdayLine);
		//String fellowPriorityLine = reader.readLine();
		
		String[] firstNames = firstNameLine.split(",");
		String[] lastNames = lastNameLine.split(",");
		String[] invincibles = invincibleLine.split(",");
		String[] staticOverrides = staticOverrideLine.split(",");
		String[] fellows = fellowLine.split(",");
		String[] staffedFields = staffedLine.split(",");
		String[] targets = targetLine.split(",");
		String[] telestrokePreferences = tsLine.split(",");
		String[] weekdayPreferences = weekdayLine.split(",");
		String[] weekdayInvincibleTs = invincibleTsWeekdayLine.split(",");
		//String[] fellowPriorityFields = fellowPriorityLine.split(",");
		
		//skip the first two columns - those are the title  + holiday columns
		System.out.println(firstNameLine);
		System.out.println(Arrays.asList(firstNames));
		System.out.println(invincibleLine);
		System.out.println(Arrays.asList(invincibles));
		for (int i = 2; i < firstNames.length; i++)	{
			boolean invincible = invincibles[i].equals("1") ? true : false;
			boolean staticOverride = staticOverrides[i].equals("1") ? true : false;
			boolean fellow = fellows[i].equals("1") ? true : false;
			boolean staffed = staffedFields[i].equals("1") ? true : false;
			int fellowPriority = new Integer(staffedFields[i]);
			double target = new Double(targets[i]);
			int telestrokePreference = new Integer(telestrokePreferences[i]);
			boolean weekdayTelestroke = weekdayPreferences[i].equals("1") ? true : false;
			
			boolean invicibleTsWeekday = weekdayInvincibleTs[i].equals("1") ? true : false;
			Person newPerson = PersonDirectory.addPerson(new Person(firstNames[i], lastNames[i], invincible, staticOverride, fellow, staffed, fellowPriority, target, telestrokePreference, weekdayTelestroke, invicibleTsWeekday));
			peopleForIndices.put(i, newPerson);
		}
	}
	
	private Person lookupPerson(int index)	{
		return peopleForIndices.get(index);
	}
	
	private Availability parseAvailability(String[] lineElems, int index)	{
		return Availability.get(lineElems[index].trim());
	}
	
	private int parseDate(String[] lineElems)	{
		String[] split = lineElems[0].split("-");
		return new Integer(split[0]);
	}
	
	private AMPM parseAMPM(String[] lineElems)	{
		String[] split = lineElems[0].split("-");
		return AMPM.valueOf(split[1]);
	}

	private void loadAvailability(BufferedReader reader, int year, int month ) throws Exception	{
		String nextLine = reader.readLine();
		while (nextLine != null)	{
			System.out.println("Next line before trim: " + nextLine);
			String[] lineElems = nextLine.trim().split(",");
			System.out.println("Next line: " + nextLine);
			for (int i = 2; i < lineElems.length; i++)	{
				lookupPerson(i).setAvailablility(parseAvailability(lineElems, i), parseDate(lineElems), parseAMPM(lineElems));
			}
			nextLine = reader.readLine();
		}
	}

}