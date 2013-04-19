package de.azapps.mirakel;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.joda.time.LocalDate;


public class MirakelHelper {
	static String getTaskDueString(GregorianCalendar due, String format) {
		if (due.compareTo(new GregorianCalendar(1970, 1, 1)) < 0)
			return "";
		else {
			return new SimpleDateFormat(format, Locale.getDefault())
					.format(due.getTime());
		}
	}

	static int getTaskDueColor(GregorianCalendar origDue, boolean isDone) {
		LocalDate today = new LocalDate();
		LocalDate nextWeek = new LocalDate().plusDays(7);
		LocalDate due = new LocalDate(origDue);
		int cmpr = today.compareTo(due);
		int color;
		if (isDone) {
			color = R.color.Grey;
		} else if (cmpr > 0) {
			color = R.color.Red;
		} else if (cmpr == 0) {
			color = R.color.Orange;
		} else if (nextWeek.compareTo(due) >= 0) {
			color = R.color.Yellow;
		} else {
			color = R.color.Green;
		}
		return color;
	}
}
