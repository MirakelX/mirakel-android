/*******************************************************************************
 * Mirakel is an Android App for Managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.joda.time.LocalDate;

public class MirakelHelper {

	/**
	 * Format a Date for showing it in the app
	 * 
	 * @param date
	 *            Date
	 * @param format
	 *            Format–String (like dd.MM.YY)
	 * @return The formatted Date as String
	 */
	static String formatDate(GregorianCalendar date, String format) {
		if (date.compareTo(new GregorianCalendar(1970, 1, 1)) < 0)
			return "";
		else {
			return new SimpleDateFormat(format, Locale.getDefault())
					.format(date.getTime());
		}
	}

	/**
	 * Returns the ID of the Color–Resource for a Due–Date
	 * 
	 * @param origDue
	 *            The Due–Date
	 * @param isDone
	 *            Is the Task done?
	 * @return ID of the Color–Resource
	 */
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
