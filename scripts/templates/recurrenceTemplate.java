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
package de.azapps.mirakel.sync.taskwarrior.model.test;

import android.test.suitebuilder.annotation.SmallTest;

import com.google.common.base.Optional;
import com.google.gson.JsonObject;

import junit.framework.TestCase;

import java.util.Calendar;

import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorRecurrence;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorTaskSerializer;

public class RecurrenceTest extends TestCase {

    private void performBackCheck(String rec,Recurring r) {
        final JsonObject e=new JsonObject();
        TaskWarriorTaskSerializer.handleRecurrence(e, r);
        String recur = e.get("recur").getAsString();
        assertEquals("Recurring does not match: was <"+recur+"> expected <"+rec+">",r.getInterval(),parseRecurring(recur).getInterval());
    }

    private Recurring parseRecurring(String rec) {
        final Recurring r;
        try{
           r = new TaskWarriorRecurrence(rec, Optional.<Calendar>absent());
        }catch(Exception e){
            fail("Parsing of " + rec + " failed: "+e.getMessage());
            //this can not be reached stupid compiler
            return null;
        }
        return r;
    }

	#foreach ($F in $FUNCTIONS)
	@SmallTest
	public void test_${F.get('name')}() {
		final String rec = "${F.get('name')}";
        final Recurring r = parseRecurring(rec);
		assertEquals(
				"Recurring does not match:  ${F.get('function')}" + r.${F.get('function')}(),
				${F.get('value')}, r.${F.get('function')}());
        performBackCheck(rec, r);
	}
	#end
	
	@SmallTest
	public void test_weekdays() {
		final String rec = "weekdays";
		final Recurring r = parseRecurring(rec);
		for (Integer i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
			if (i == Calendar.SATURDAY || i == Calendar.SUNDAY) {
				assertTrue("Recurring weekday contains  saturday or sunday", !r
						.getWeekdays().contains(i));
			} else {
				assertTrue("Recurring weekday don't contain weekday " + i, r
						.getWeekdays().contains(i));
			}
		}
		performBackCheck(rec, r);
	}
}
