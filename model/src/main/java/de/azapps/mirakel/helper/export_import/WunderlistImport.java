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

package de.azapps.mirakel.helper.export_import;

import android.support.annotation.NonNull;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import com.google.common.base.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.of;

public class WunderlistImport {
    private static final String TAG = "WunderlistImport";

    private static SparseArray<Task> taskMapping;
    private static SparseArray<ListMirakel> listMapping;

    public static boolean exec(final FileInputStream stream) {
        final JsonObject i;
        try {
            i = new JsonParser().parse(new InputStreamReader(stream))
            .getAsJsonObject();
        } catch (final JsonSyntaxException e) {
            Log.e(TAG, "malformed backup", e);
            return false;
        }
        final Set<Entry<String, JsonElement>> f = i.entrySet();
        listMapping = new SparseArray<>();
        taskMapping = new SparseArray<>();
        if (!parseLoop(f)) {
            return false;
        }
        for (final Pair<Task, Integer> pair : subtasks) {
            try {
                taskMapping.get(pair.second).addSubtask(pair.first);
            } catch (final RuntimeException e) {
                Log.e(TAG, "Blame yourself… ", e);
                // Blame yourself…
            }
        }
        return true;
    }

    private static boolean parseLoop(@NonNull final Set<Entry<String, JsonElement>> f) {
        for (final Entry<String, JsonElement> e : f) {
            switch (e.getKey().toLowerCase()) {
            case "data":
                if (e.getValue().isJsonObject()) {
                    parseLoop(e.getValue().getAsJsonObject().entrySet());
                } else {
                    throw new JsonParseException("data is no jsonobject");
                }
                break;
            case "lists":
                if (e.getValue().isJsonArray()) {
                    for (final JsonElement jsonElement : e.getValue()
                         .getAsJsonArray()) {
                        listMapping = parseList(jsonElement.getAsJsonObject());
                    }
                } else {
                    throw new JsonParseException("lists is no jsonarray");
                }
                break;
            case "tasks":
                if (e.getValue().isJsonArray()) {
                    for (JsonElement jsonElement : e.getValue()
                         .getAsJsonArray()) {
                        parseTask(jsonElement.getAsJsonObject());
                    }
                } else {
                    throw new JsonParseException("tasks is no jsonarry");
                }
                break;
            case "reminders":
                handleReminder(e);
                break;
            case "notes":
                handleNote(e);
                break;
            case "subtasks":
                handleSubtask(e);
                break;
            default:
                Log.d(TAG, e.getKey());
                ErrorReporter.report(ErrorType.IMPORT_WUNDERLIST);
                return false;
            }
        }
        return true;
    }

    private static void handleReminder(final @NonNull Entry<String, JsonElement> e) {
        if (e.getValue().isJsonArray()) {
            final JsonArray reminders = e.getValue().getAsJsonArray();
            for (final JsonElement reminder : reminders) {
                if (reminder.isJsonObject()) {
                    final int taskID = reminder.getAsJsonObject().get("task_id").getAsInt();
                    final String time = reminder.getAsJsonObject().get("date").getAsString();
                    final DateTime reminderDate;
                    try {
                        reminderDate = new DateTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.Sz").parse(time));
                    } catch (final ParseException e1) {
                        Log.wtf(TAG, "invalid timeformat", e1);
                        continue;
                    }
                    final Task t = taskMapping.get(taskID);
                    if (t != null) {
                        t.setReminder(of(reminderDate));
                        t.save();
                    }
                } else {
                    throw new JsonParseException("reminder is no jsonobject");
                }
            }
        }
    }

    private static void handleNote(final @NonNull Entry<String, JsonElement> e) {
        if (e.getValue().isJsonArray()) {
            final JsonArray notes = e.getValue().getAsJsonArray();
            for (final JsonElement note : notes) {
                if (note.isJsonObject()) {
                    final int taskID = note.getAsJsonObject().get("task_id").getAsInt();
                    final String noteText = note.getAsJsonObject().get("content").getAsString();
                    final Task t = taskMapping.get(taskID);
                    if (t != null) {
                        t.setContent(noteText);
                        t.save();
                    }
                } else {
                    throw new JsonParseException("note is no jsonobject");
                }
            }
        }
    }

    private static void handleSubtask(final @NonNull Entry<String, JsonElement> e) {
        if (e.getValue().isJsonArray()) {
            final JsonArray subtasks = e.getValue().getAsJsonArray();
            for (final JsonElement subtask : subtasks) {
                if (subtask.isJsonObject()) {
                    final int taskID = subtask.getAsJsonObject().get("task_id").getAsInt();
                    final String subtaskName = subtask.getAsJsonObject().get("title").getAsString();
                    final boolean done = subtask.getAsJsonObject().has("completed") &&
                                         subtask.getAsJsonObject().get("completed").getAsBoolean();
                    final Task t = taskMapping.get(taskID);
                    if (t != null) {
                        final ListMirakel list = MirakelModelPreferences
                                                 .getListForSubtask(t);
                        final Task subtaskTask = Task.newTask(subtaskName, list);
                        t.addSubtask(subtaskTask);
                        t.setDone(done);
                        t.save();
                    }
                } else {
                    throw new JsonParseException("subtask is no jsonobject");
                }
            }
        }
    }

    private static SparseArray<ListMirakel> parseList(final JsonObject jsonList) {
        final String name = jsonList.get("title").getAsString();
        final int id = jsonList.get("id").getAsInt();
        final ListMirakel l = ListMirakel.safeNewList(name);
        l.save(false);
        listMapping.put(id, l);
        return listMapping;
    }

    /**
     * <Subtask, id of parent>
     */
    private static List<Pair<Task, Integer>> subtasks = new ArrayList<>();

    private static void parseTask(final JsonObject jsonTask) {
        final String name = jsonTask.get("title").getAsString();
        final int list_id_string = jsonTask.get("list_id").getAsInt();
        final Long listId = listMapping.get(list_id_string).getId();
        Optional<ListMirakel> listMirakelOptional  = ListMirakel.get(listId);
        final ListMirakel list;
        if (listMirakelOptional.isPresent()) {
            list = listMirakelOptional.get();
        } else {
            list = ListMirakel.safeFirst();
        }
        final Task t = Task.newTask(name, list);
        taskMapping.put(jsonTask.get("id").getAsInt(), t);
        if (jsonTask.has("due_date")) {
            try {
                final DateTime due = new DateTime(DateTimeHelper.parseDate(jsonTask.get(
                                                      "due_date").getAsString()));
                t.setDue(of(due));
            } catch (final ParseException e) {
                Log.e(TAG, "cannot parse date", e);
            }
        }
        if (jsonTask.has("note")) {
            t.setContent(jsonTask.get("note").getAsString());
        }
        if (jsonTask.has("completed_at")) {
            t.setDone(true);
        }
        if (jsonTask.has("starred") && jsonTask.get("starred").getAsBoolean()) {
            t.setPriority(2);
        }
        if (jsonTask.has("parent_id")) {
            subtasks.add(new Pair<>(t, jsonTask.get("parent_id")
                                    .getAsInt()));
        }
        if (jsonTask.has("recurrence_type") && jsonTask.has("recurrence_count")) {
            final int rec_count = jsonTask.get("recurrence_count").getAsInt();
            final Recurring r;
            final String type = jsonTask.get("recurrence_type").getAsString();
            switch (type) {
            case "year":
                r = Recurring.newRecurring(type, new Period(rec_count, 0, 0, 0, 0, 0, 0, 0), true,
                                           Optional.<DateTime>absent(),
                                           Optional.<DateTime>absent(), true, true, new SparseBooleanArray());
                break;
            case "month":
                r = Recurring.newRecurring(type, new Period(0, rec_count, 0, 0, 0, 0, 0, 0), true,
                                           Optional.<DateTime>absent(),
                                           Optional.<DateTime>absent(), true, true, new SparseBooleanArray());
                break;
            case "week":
                r = Recurring.newRecurring(type, new Period(0, 0, rec_count, 0, 0, 0, 0, 0), true,
                                           Optional.<DateTime>absent(), Optional.<DateTime>absent(), true, true, new SparseBooleanArray());
                break;
            case "day":
                r = Recurring.newRecurring(type, new Period(0, 0, 0, rec_count, 0, 0, 0, 0), true,
                                           Optional.<DateTime>absent(),
                                           Optional.<DateTime>absent(), true, true, new SparseBooleanArray());
                break;
            default:
                throw new JsonParseException("Unknown recurring " + type);
            }
            t.setRecurrence(of(r));
        }
        t.save(false);
    }
}
