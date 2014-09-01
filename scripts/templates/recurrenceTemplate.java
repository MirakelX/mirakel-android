/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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
package de.azapps.mirakel.model.task;

import java.util.Calendar;

import junit.framework.TestCase;
import android.test.suitebuilder.annotation.SmallTest;
import de.azapps.mirakel.model.recurring.Recurring;

public class RecurrenceTest extends TestCase {

	#foreach ($F in $FUNCTIONS)
	@SmallTest
	public void test_${F.get('name')}() {
		final String recc = "${F.get('name')}";
		final Recurring r = TaskDeserializer.parseTaskWarriorRecurrence(recc);
		assertNotNull("Parsing " + recc + " failed", r);
		assertEquals(
				"Reccuring does not match:  ${F.get('function')}" + r.${F.get('function')}(),
				${F.get('value')}, r.${F.get('function')}());
	}
	#end
	
	@SmallTest
	public void test_weekdays() {
		final String recc = "weekdays";
		final Recurring r = TaskDeserializer.parseTaskWarriorRecurrence(recc);
		assertNotNull("Parsing " + recc + " failed", r);
		for (Integer i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
			if (i == Calendar.SATURDAY || i == Calendar.SUNDAY) {
				assertTrue("Reccuring weekday contains  saturday or sunday", !r
						.getWeekdays().contains(i));
			} else {
				assertTrue("Reccuring weekday don't contain weekday " + i, r
						.getWeekdays().contains(i));
			}
		}
	}
}
