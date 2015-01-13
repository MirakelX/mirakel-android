/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.model.task;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorTask;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorTaskDeserializer;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorTaskSerializer;
import de.azapps.mirakelandroid.test.MirakelTestCase;
import de.azapps.mirakelandroid.test.RandomHelper;
import static com.google.common.base.Optional.of;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test cases tests if the Deserializer is working properly.
 *
 * @author az
 *
 */
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class TaskDeserializerTest extends MirakelTestCase{

	private boolean compare(final String taskString, final TaskWarriorTask task) {
		final JsonParser parser = new JsonParser();
		final JsonObject o = (JsonObject) parser.parse(taskString);
        Task t=new Task();
        task.setToTask(t);

		final String outputTask = new GsonBuilder()
				.registerTypeAdapter(Task.class,
						new TaskWarriorTaskSerializer(Robolectric.application)).create()
				.toJson(t);
		final JsonObject o2 = (JsonObject) parser.parse(outputTask);
		return equalJson(o2, o);
	}

	//the buildin equal method does not match if the fields have a other order
	private boolean equalJson(final JsonElement a, final JsonElement b) {
		if (a == b) {
			return true;
		}
		if (a.isJsonArray() && !b.isJsonArray()) {
			fail("json types does not match");
			return false;
		}
		if (a.isJsonNull() && !b.isJsonNull()) {
			fail("json types does not match");
			return false;
		}
		if (a.isJsonObject() && !b.isJsonObject()) {
			fail("json types does not match");
			return false;
		}
		if (a.isJsonPrimitive() && !b.isJsonPrimitive()) {
			fail("json types does not match");
			return false;
		}
		if (a.isJsonArray()) {
			final JsonArray f = a.getAsJsonArray();
			final JsonArray s = b.getAsJsonArray();
			if (s.size() != f.size()) {
				fail("json-array sizs does not match: array1 has " + f.size()
						+ " elements, array2 has " + s.size() + " elements");
				return false;
			}
			for (int i = 0; i < s.size(); i++) {
				final JsonElement first = f.get(i);
				int j;
				for (j = 0; j < s.size(); j++) {
					if (equalJson(first, s.get(j))) {
						break;
					}
				}
				if (j == s.size()) {
					fail("no matching arrayelement for<" + first.toString()
							+ "> in <" + s.toString() + "> found");
					return false;
				}
			}
			return true;
		} else if (a.isJsonPrimitive()) {
			// this part is only called recursive, so no failing here to get
			// more informations
			final JsonPrimitive f = a.getAsJsonPrimitive();
			final JsonPrimitive s = b.getAsJsonPrimitive();
			if (f.isBoolean() && !s.isBoolean()) {
				return false;
			} else if (f.isNumber() && !s.isNumber()) {
				return false;
			} else if (f.isString() && !s.isString()) {
				return false;
			}
			if (s.isBoolean()) {
				return s.getAsBoolean() == f.getAsBoolean();
			} else if (s.isNumber() && s.isNumber()) {
				return s.getAsNumber().equals(f.getAsNumber());
			} else if (s.isString()) {
				return s.getAsString().equals(f.getAsString());
			}
			return false;
		} else if (a.isJsonObject()) {
			final JsonObject f = a.getAsJsonObject();
			final JsonObject s = b.getAsJsonObject();
			final List<String> checkedElements = new ArrayList<>();
			for (final Entry<String, JsonElement> el : f.entrySet()) {
				for (final Entry<String, JsonElement> el2 : s.entrySet()) {
					if (el.getKey().equals(el2.getKey())) {
						checkedElements.add(el.getKey());
						if (!equalJson(el.getValue(), el2.getValue())) {
							fail("element " + el.toString()
									+ " does not match " + el2.toString());
							return false;
						}
						break;
					}
				}
			}
			boolean ret = true;
			String diff = "";
			if (checkedElements.size() != f.entrySet().size()) {
				diff = diff(s, checkedElements, diff, f.entrySet());
                if(!diff.isEmpty()) {
                    ret = false;
                    diff += "\n";
                }
			}
			if (checkedElements.size() != s.entrySet().size()) {
				diff = diff(s, checkedElements, diff, s.entrySet());
                if(!diff.isEmpty()) {
                    ret = false;
                    diff+="\n";
                }
			}
			if (!ret&&!"+modified".equals(diff)) {
				fail("Json object elements does not match: \n" + diff+"\n"+a.toString()+"\n"+b.toString());
				return false;
			}
			return true;
		}

		return false;
	}

	private static String diff(final JsonObject s,
			final List<String> checkedElements, final String diff,
			final Set<Entry<String, JsonElement>> set) {
		String retDiff = diff;
		for (final Entry<String, JsonElement> el : set) {
			if (!checkedElements.contains(el.getKey())&&!"modified".equals(el.getKey())) {
				retDiff += "+"
						+ (diff.length() == 0 ? el.getKey() : el.getKey() + ",");
			}
		}
		return retDiff;
	}

	private void testString(final String message, final String inputTask)
			throws Exception {
		final Gson gson = new GsonBuilder().registerTypeAdapter(
				TaskWarriorTask.class,
				new TaskWarriorTaskDeserializer()).create();

		final TaskWarriorTask task = gson.fromJson(inputTask, TaskWarriorTask.class);
		assertTrue(message, compare(inputTask, task));
	}

	#foreach ($JSON in $JSON_LIST)
	@Test
	public void testTaskJSON${foreach.count}() throws Exception {
		final String inputTask = "$JSON";
		testString("Tasks are not equal", inputTask);
	}
	#end

    @Test
    public void testSerialize1(){
        Task t=new Task();
        t.setUUID(java.util.UUID.randomUUID().toString());
        t.setPriority(2);
        t.setName(RandomHelper.getRandomString());
        ListMirakel l=ListMirakel.getInboxList(AccountMirakel.getLocal());
        t.setList(l);

        Calendar created=new GregorianCalendar();
        t.setCreatedAt(created);

        Calendar updated=new GregorianCalendar();
        updated.add(Calendar.DAY_OF_MONTH,1);
        t.setUpdatedAt(updated);

        Calendar due=new GregorianCalendar();
        due.add(Calendar.DAY_OF_MONTH, 7);
        t.setDue(of(due));

        String serialized=new GsonBuilder()
                .registerTypeAdapter(Task.class,
                        new TaskWarriorTaskSerializer(Robolectric.application)).create()
                .toJson(t);
        SimpleDateFormat format=DateTimeHelper.taskwarriorFormat;

        due.setTimeZone(TimeZone.getTimeZone("UTC"));
        created.setTimeZone(TimeZone.getTimeZone("UTC"));
        updated.setTimeZone(TimeZone.getTimeZone("UTC"));

        String json="{\"uuid\":\""+t.getUUID()+"\","+
                "\"status\":\"pending\","+
                "\"entry:\":\""+format.format(created.getTime())+"\","+
                "\"description\":\""+t.getName()+"\","+
                "\"due:\":\""+format.format(due.getTime())+"\","+
                "\"modified:\":\""+format.format(updated.getTime())+"\","+
                "\"priority\":\"H\"}";

        JsonParser parser=new JsonParser();

        assert(equalJson(parser.parse(serialized),parser.parse(json)));
    }

}
