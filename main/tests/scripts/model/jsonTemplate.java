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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.test.suitebuilder.annotation.SmallTest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import de.azapps.mirakel.sync.taskwarrior.TaskWarriorTaskSerializer;
import de.azapps.mirakelandroid.test.MirakelTestCase;
import de.azapps.mirakelandroid.test.RandomHelper;

/**
 * This test cases tests if the Deserializer is working properly.
 * 
 * @author az
 * 
 */
public class TaskDeserializerTest extends MirakelTestCase {

	private boolean compare(final String taskString, final Task task) {
		final JsonParser parser = new JsonParser();
		final JsonObject o = (JsonObject) parser.parse(taskString);

		final String outputTask = new GsonBuilder()
				.registerTypeAdapter(Task.class,
						new TaskWarriorTaskSerializer(getContext())).create()
				.toJson(task);
		final JsonObject o2 = (JsonObject) parser.parse(outputTask);
		return equalJson(o, o2);
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
				ret = false;
				diff += "\n";
			}
			if (checkedElements.size() != s.entrySet().size()) {
				diff = diff(s, checkedElements, diff, s.entrySet());
				ret = false;
			}
			if (!ret) {
				fail("Json object elements does not match: " + diff);
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
			if (!checkedElements.contains(el.getKey())) {
				retDiff += "+"
						+ (diff.length() == 0 ? el.getKey() : el.getKey() + ",");
			}
		}
		return retDiff;
	}

	private void testString(final String message, final String inputTask)
			throws Exception {
		final Gson gson = new GsonBuilder().registerTypeAdapter(
				Task.class,
				new TaskDeserializer(true, RandomHelper
						.getRandomAccountMirakel(), this.mContext)).create();

		final Task task = gson.fromJson(inputTask, Task.class);
		assertTrue(message, compare(inputTask, task));
	}
	
	#foreach ($JSON in $JSON_LIST)
	@SmallTest
	public void testTaskJSON${foreach.count}() throws Exception {
		final String inputTask = "$JSON";
		testString("Tasks are not equal", inputTask);
	}
	#end

}
