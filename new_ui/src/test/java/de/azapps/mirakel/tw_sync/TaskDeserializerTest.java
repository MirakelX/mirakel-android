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
package de.azapps.mirakel.tw_sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorTask;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorTaskDeserializer;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorTaskSerializer;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.base.Optional.of;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * This test cases tests if the Deserializer is working properly.
 *
 * @author az
 *
 */
@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class TaskDeserializerTest extends MirakelDatabaseTestCase {

    private boolean compare(final String taskString, final TaskWarriorTask task) {
        final JsonParser parser = new JsonParser();
        final JsonObject o = (JsonObject) parser.parse(taskString);
        Task t = new Task();
        task.setToTask(t);

        final String outputTask = new GsonBuilder()
        .registerTypeAdapter(Task.class,
                             new TaskWarriorTaskSerializer(RuntimeEnvironment.application)).create()
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
                if (!diff.isEmpty()) {
                    ret = false;
                    diff += "\n";
                }
            }
            if (checkedElements.size() != s.entrySet().size()) {
                diff = diff(s, checkedElements, diff, s.entrySet());
                if (!diff.isEmpty()) {
                    ret = false;
                    diff += "\n";
                }
            }
            if (!ret && !"+modified".equals(diff)) {
                fail("Json object elements does not match: \n" + diff + "\n" + a.toString() + "\n" + b.toString());
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
            if (!checkedElements.contains(el.getKey()) && !"modified".equals(el.getKey())) {
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


    @Test
    public void testTaskJSON1() throws Exception {
        final String inputTask =
            "{\"description\":\"4: Basic graphing configuration\",\"entry\":\"20140520T231023Z\",\"githubbody\":\"Currently the graphs in Pathomx are very unconfigurable. Simple things like being able to change scales\\/etc. and add error bars would be very helpful.\\n\\nThe task is to define a set of basic options that are supported on multiple matplotlib graph types and create a ConfigPanel for them and add the config hooks to the views. In order to support the config by default as long as the panel is in place, the views should either function without the settings (preferable?) or set defaults on the config (bit hacky).\\n\\nThe config handler has hooks into the QtWidgets so you can define a config option 'show_error_bars' and set the widget as a handler and everything is automated - updating the widget refreshes the graph without intervention so this should be fairly straightforward to implement. One option here is to have the config panels add default settings options for any setting they can control (assuming it's not already set).\\n\\nThis would mean a tool can simple add the panel and forget.\\n\\n\\n\",\"githubcreatedon\":\"20140226T230414Z\",\"githubnumber\":\"4\",\"githubtitle\":\"Basic graphing configuration\",\"githubtype\":\"issue\",\"githubupdatedat\":\"20140226T230446Z\",\"githuburl\":\"https:\\/\\/github.com\\/pathomx\\/pathomx\\/issues\\/4\",\"priority\":\"M\",\"project\":\"pathomx\",\"status\":\"pending\",\"tags\":[\"bitesize\",\"enhancement\"],\"uuid\":\"4ed2ddcc-a98f-4d88-a8cc-da332ebcef44\"}";
        testString("Tasks are not equal", inputTask);
    }

    @Test
    public void testTaskJSON2() throws Exception {
        final String inputTask =
            "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed790b\",\"status\":\"pending\",\"entry\":\"20140312T123933Z\",\"description\":\"foordnd\",\"due\":\"20140501T000000Z\",\"project\":\"ni\", \"priority\":\"M\",\"modified\":\"20140502T130929Z\",\"tags\":[\"New_Tag\",\"bin\"]}";
        testString("Tasks are not equal", inputTask);
    }

    @Test
    public void testTaskJSON3() throws Exception {
        final String inputTask =
            "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed790b\",\"status\":\"pending\",\"entry\":\"20140312T123933Z\",\"description\":\"Ban\",\"due\":\"20140501T000000Z\",\"project\":\"ni\", \"priority\":\"M\",\"modified\":\"20140502T130929Z\",\"tags\":[\"New_Tag\",\"bin\"],\"annotations\":[{\"entry\":\"20140529T113842Z\",\"description\":\"htoo\"}]}";
        testString("Tasks are not equal", inputTask);
    }

    @Test
    public void testSerialize1() {
        Task t = new Task();
        t.setUUID(java.util.UUID.randomUUID().toString());
        t.setPriority(2);
        t.setName(RandomHelper.getRandomString());
        ListMirakel l = ListMirakel.getInboxList(AccountMirakel.getLocal());
        t.setList(l);

        DateTime created = new DateTime();
        t.setCreatedAt(created);

        DateTime updated = new DateTime().plusDays(1);
        t.setUpdatedAt(updated);

        DateTime due = new DateTime().plusDays(1);
        t.setDue(of(due));

        String serialized = new GsonBuilder()
        .registerTypeAdapter(Task.class,
                             new TaskWarriorTaskSerializer(RuntimeEnvironment.application)).create()
        .toJson(t);
        String json = "{\"uuid\":\"" + t.getUUID() + "\"," +
                      "\"status\":\"pending\"," +
                      "\"project\":\"" + l.getName() + "\"," +
                      "\"entry\":\"" + created.toString(ISODateTimeFormat.basicDateTimeNoMillis().withZoneUTC()) + "\"," +
                      "\"description\":\"" + t.getName() + "\"," +
                      "\"due\":\"" + due.toString(ISODateTimeFormat.basicDateTimeNoMillis().withZoneUTC()) + "\"," +
                      "\"modified\":\"" + updated.toString(ISODateTimeFormat.basicDateTimeNoMillis().withZoneUTC()) +
                      "\"," +
                      "\"priority\":\"H\"}";

        JsonParser parser = new JsonParser();

        assertTrue(equalJson(parser.parse(serialized), parser.parse(json)));
    }


}
