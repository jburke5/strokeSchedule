package burke.schedule;

import java.io.*;
import java.util.*;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.*;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.*;
import net.fortuna.ical4j.data.*;


public class ConvertToIcal	{

	private static String directory = "/Users/burke/Documents/stroke schedules/automated/final schedules/";
	private String convertFromFile = "";
	private boolean rollForward = true;
	
	public ConvertToIcal(String name, int roll)	{
		this.convertFromFile = name;
		this.rollForward = roll == 1 ? true : false;
	}
	
	public void convert() throws Exception	{
		List<Shift> allShifts = readFile("");
		List<Shift> tsShifts = readFile("ts");
		net.fortuna.ical4j.model.Calendar ical = setupCalendar();
		java.util.Calendar today = getToday();
		ical = writeToIcal(allShifts, ical, today, "BIG");
		ical = writeToIcal(tsShifts, ical, today, "Tele");
		printCalendar(ical);
	}
	
	private List<Shift> readFile(String suffix) throws Exception	{
		String fileName = convertFromFile;
		if (!suffix.equals("")) 	{
			int dotIndex = convertFromFile.indexOf(".");
			fileName = convertFromFile.substring(0, dotIndex) + suffix + convertFromFile.substring(dotIndex, convertFromFile.length());
		}
		String filePath = directory + File.separator +  fileName; 
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		String line = reader.readLine();
		List<Shift> allShifts = new ArrayList<Shift>();
		while (line != null)	{
			String[] args = line.split(",");
			String numeralString = args[0].split("-")[0].trim();
			int date =  Integer.decode(numeralString);
			String ampmRaw = args[0].split("-")[1].trim();
			AMPM ampm = ampmRaw.equals("AM") ? AMPM.AM : AMPM.PM;
			String name = args[1];
			Shift shift = new Shift(date,  -1, ampm, false, -1);
			shift.assigned = new Person(null, name, false, false, false, false, 0, 0,0);
			allShifts.add(shift);
			line = reader.readLine();
		}
		return allShifts;
	
	}
	
	private java.util.Calendar getToday()	{
		java.util.Calendar today = java.util.Calendar.getInstance();
		today.setTime(new java.util.Date());
		if (this.rollForward)	{
			if (today.get(java.util.Calendar.MONTH) == java.util.Calendar.DECEMBER)
				today.roll(java.util.Calendar.YEAR, true);
			today.roll(java.util.Calendar.MONTH, true);	//roll the calendar forward a month
		}
		return today;

	}
	
	private net.fortuna.ical4j.model.Calendar setupCalendar()	{
		net.fortuna.ical4j.model.Calendar ical = new net.fortuna.ical4j.model.Calendar();
		ical.getProperties().add(new ProdId("Burke-Stroke"));
		ical.getProperties().add(Version.VERSION_2_0);
		ical.getProperties().add(CalScale.GREGORIAN);		
		
		return ical;	
	}
	
	private net.fortuna.ical4j.model.Calendar writeToIcal(List<Shift>  allShifts, net.fortuna.ical4j.model.Calendar ical, java.util.Calendar today, String suffix) throws Exception	{	
		UidGenerator ug = new UidGenerator("1");

		for (Shift next : allShifts)	{
			if (next.getPerson().getLastName().toLowerCase().contains("burke"))	{
System.out.println(next.toString());
				java.util.Calendar newDate = (java.util.Calendar) today.clone();
				newDate.set(java.util.Calendar.DAY_OF_MONTH, next.getDate());
System.out.println("** new date" + newDate.toString());
System.out.println("** new date/time" + new net.fortuna.ical4j.model.Date(newDate.getTime()));
				
				boolean hasFellow = next.getPerson().getLastName().toLowerCase().contains("/");
				String title = suffix + " Stroke Call - ";
				title += next.getAMPM() == AMPM.AM ? "AM" : "PM";	
				title += hasFellow ? " + Fellow" : "";		

				VEvent newEvent = new VEvent(new net.fortuna.ical4j.model.Date(newDate.getTime()), title);
				newEvent.getProperties().add(ug.generateUid());
				
				if (next.getAMPM() == AMPM.AM)	{
					newDate.set(java.util.Calendar.HOUR_OF_DAY, 7);
					newDate.set(java.util.Calendar.MINUTE, 30);
					
					VAlarm earlyReminder = new VAlarm(new DateTime(newDate.getTime()));
					earlyReminder.getProperties().add(Action.DISPLAY);
					earlyReminder.getProperties().add(new Description(suffix + " Stroke Call - AM"));
					
					newDate.set(java.util.Calendar.HOUR_OF_DAY, 8);
					VAlarm lateReminder = new VAlarm(new DateTime(newDate.getTime()));
					lateReminder.getProperties().add(Action.DISPLAY);
					lateReminder.getProperties().add(new Description(suffix+ " Stroke Call - AM "));
					
					newEvent.getAlarms().add(earlyReminder);
					newEvent.getAlarms().add(lateReminder);
				} else	{
					newDate.set(java.util.Calendar.HOUR_OF_DAY, 16);
					newDate.set(java.util.Calendar.MINUTE, 30);
					
					VAlarm earlyReminder = new VAlarm(new DateTime(newDate.getTime()));
					earlyReminder.getProperties().add(Action.DISPLAY);
					earlyReminder.getProperties().add(new Description(suffix + " Stroke Call - PM "));
					
					newDate.set(java.util.Calendar.HOUR_OF_DAY, 20);
					newDate.set(java.util.Calendar.MINUTE, 0);
					VAlarm lateReminder = new VAlarm(new DateTime(newDate.getTime()));
					lateReminder.getProperties().add(Action.DISPLAY);
					lateReminder.getProperties().add(new Description(suffix + " Stroke Call - PM"));
					
					newEvent.getAlarms().add(earlyReminder);
					newEvent.getAlarms().add(lateReminder);
				}

				ical.getComponents().add(newEvent);
			}	
		}
		return ical;
	}
	
	private void printCalendar(net.fortuna.ical4j.model.Calendar ical) throws Exception	{	
System.out.println(convertFromFile);
		String shortFileName = convertFromFile.substring(0, convertFromFile.indexOf("."));
System.out.println(shortFileName);

		FileOutputStream fout = new FileOutputStream(directory + File.separator + shortFileName + ".ics");

		CalendarOutputter outputter = new CalendarOutputter();
		outputter.output(ical, fout);
		fout.flush();
		fout.close();
	}


	public static void main(String[] args)	{
		String csvFileName = args[0];
		int month = new Integer(args[1]);
		try	{
			new ConvertToIcal(csvFileName, month).convert();
		} catch (Exception ex)	{
			ex.printStackTrace();
		}
	}
	
}