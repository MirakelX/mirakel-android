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
package de.azapps.mirakel.sync.taskwarrior.model;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.util.Pair;

import com.google.common.base.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.R;
import de.azapps.tools.Log;

public class TaskWarriorTaskSerializer implements JsonSerializer<Task> {

    private static final String TAG = "TaskWarriorTaskSerializer";
    private final Context mContext;

    public TaskWarriorTaskSerializer(final Context ctx) {
        this.mContext = ctx;
    }

    private String formatCal(final Calendar calendar) {
        final SimpleDateFormat df = new SimpleDateFormat(
            this.mContext.getString(R.string.TWDateFormat));
        if (calendar.getTimeInMillis() < 0) {
            calendar.setTimeInMillis(10);
        }
        return df.format(calendar.getTime());
    }

    private static String escape(final String string) {
        return string.replace("\"", "\\\"");
    }

    private static String cleanQuotes(String str) {
        // call this only if string starts and ands with "
        // additional keys has this
        if (str.startsWith("\"") || str.startsWith("'")) {
            str = str.substring(1);
        }
        if (str.endsWith("\"") || str.endsWith("'")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    @Override
    public JsonElement serialize(final Task task, final Type arg1,
                                 final JsonSerializationContext arg2) {
        final JsonObject json = new JsonObject();
        final Map<String, String> additionals = task.getAdditionalEntries();
        boolean isMaster = false;
        if (task.getRecurrence().isPresent()) {
            if (new MirakelQueryBuilder(mContext).and(Recurring.CHILD, Operation.EQ,
                    task).count(MirakelInternalContentProvider.RECURRING_TW_URI) == 0) {
                isMaster = true;
            }
        }
        final Pair<String, String> statusEnd = getStatus(task, additionals, isMaster);
        final String status = statusEnd.second;
        final String end = statusEnd.first;
        String priority = null;
        switch (task.getPriority()) {
        case -2:
        case -1:
            priority = "L";
            break;
        case 1:
            priority = "M";
            break;
        case 2:
            priority = "H";
            break;
        default:
            break;
        }
        String uuid = task.getUUID();
        if (uuid.trim().isEmpty()) {
            uuid = java.util.UUID.randomUUID().toString();
            task.setUUID(uuid);
            task.save(false);
        }
        json.addProperty("uuid", uuid);
        json.addProperty("status", status);
        json.addProperty("entry", formatCalUTC(task.getCreatedAt()));
        json.addProperty("description", escape(task.getName()));
        if (task.getDue().isPresent()) {
            json.addProperty("due", formatCalUTC(task.getDue().get()));
        }
        if (!additionals.containsKey(Task.NO_PROJECT)) {
            json.addProperty("project", task.getList().getName());
        }
        if (priority != null) {
            json.addProperty("priority", priority);
            if ("L".equals(priority) && task.getPriority() != -2) {
                json.addProperty("priorityNumber", task.getPriority());
            }
        }
        json.addProperty("modified", formatCalUTC(task.getUpdatedAt()));
        if (task.getReminder().isPresent()) {
            json.addProperty("reminder", formatCalUTC(task.getReminder().get()));
        }
        if (end != null) {
            json.addProperty("end", end);
        }
        if (task.getProgress() != 0) {
            json.addProperty("progress", task.getProgress());
        }
        // Tags
        if (!task.getTags().isEmpty()) {
            final JsonArray tags = new JsonArray();
            for (final Tag t : task.getTags()) {
                // taskwarrior does not like whitespaces
                tags.add(new JsonPrimitive(t.getName().trim().replace(" ", "_")));
            }
            json.add("tags", tags);
        }
        // End Tags
        // Annotations
        if (!task.getContent().isEmpty()) {
            final JsonArray annotations = new JsonArray();
            /*
             * An annotation in taskd is a line of content in Mirakel!
             */
            final String annotationsList[] = escape(task.getContent()).split(
                                                 "\n");
            final Calendar updatedAt = task.getUpdatedAt();
            for (final String a : annotationsList) {
                final JsonObject line = new JsonObject();
                line.addProperty("entry", formatCalUTC(task.getUpdatedAt()));
                line.addProperty("description", a.replace("\n", ""));
                annotations.add(line);
                updatedAt.add(Calendar.SECOND, 1);
            }
            json.add("annotations", annotations);
        }
        // Anotations end
        // TW.depends==Mirakel.subtasks!
        // Dependencies
        if (task.countSubtasks() > 0) {
            boolean first1 = true;
            final List<Task> subTasks = task.getSubtasks();
            final StringBuilder depends = new StringBuilder(subTasks.size() * 10);
            for (final Task subtask : subTasks) {
                if (first1) {
                    first1 = false;
                } else {
                    depends.append(',');
                }
                depends.append(subtask.getUUID());
            }
            json.addProperty("depends", depends.toString());
        }
        // recurring tasks must have a due
        if (task.getRecurrence().isPresent() && task.getDue().isPresent()) {
            handleRecurrence(json, task.getRecurrence().get());
            if (isMaster) {
                final Cursor cursor = mContext.getContentResolver()
                                      .query(MirakelInternalContentProvider.RECURRING_TW_URI,
                                             new String[] { "child", "offsetCount" },
                                             "parent=?", new String[] {String.valueOf(task.getId())},
                                             "offsetCount ASC");
                final StringBuilder mask = new StringBuilder(cursor.getCount());
                cursor.moveToFirst();
                if (cursor.getCount() > 0) {
                    int oldOffset = -1;
                    do {
                        final int currentOffset = cursor.getInt(1);
                        if (currentOffset <= oldOffset) {
                            final long childId = cursor.getLong(0);
                            // This should not happen – it means that one offset is twice in the DB
                            final Optional<Task> child = Task.get(childId, true);
                            if (child.isPresent()) {
                                child.get().destroy(true);
                            } else {
                                // Whoa there is some garbage which we should destroy!
                                Task.destroyRecurrenceGarbageForTask(childId);
                            }
                            continue;
                        }
                        while (++oldOffset < currentOffset) {
                            mask.append('X');
                        }
                        final Optional<Task> child = Task.get(cursor.getLong(0));
                        if (!child.isPresent()) {
                            Log.wtf(TAG, "childtask is null");
                            mask.append('X');
                        } else {
                            mask.append(getRecurrenceStatus(getStatus(child.get(),
                                                            child.get().getAdditionalEntries(), false).second));
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
                json.addProperty("mask", mask.toString());
            } else {
                final Cursor cursor = mContext.getContentResolver()
                                      .query(MirakelInternalContentProvider.RECURRING_TW_URI,
                                             new String[] { "parent", "offsetCount" },
                                             "child=?", new String[] {String.valueOf(task.getId())},
                                             null);
                cursor.moveToFirst();
                if (cursor.getCount() > 0) {
                    final Optional<Task> master = Task.get(cursor.getLong(0));
                    if (!master.isPresent()) {
                        // The parent is gone. This should not happen and we
                        // should delete the child then
                        task.destroy();
                    } else {
                        json.addProperty("parent", master.get().getUUID());
                        json.addProperty("imask", cursor.getInt(1));
                    }
                } else {
                    Log.wtf(TAG, "no master found, but there must be a master");
                }
                cursor.close();
            }
        }
        // end Dependencies
        // Additional Strings
        for (final String key : additionals.keySet()) {
            if (!key.equals(Task.NO_PROJECT)
                && !"status".equals(key)) {
                json.addProperty(key, cleanQuotes(additionals.get(key)));
            }
        }
        // end Additional Strings
        return json;
    }

    private static String getRecurrenceStatus(final String s) {
        switch (s) {
        case "recurring":
        case "pending":
            return "-";
        case "completed":
            return "+";
        case "deleted":
            return "X";
        case "waiting":
            return "W";
        default:
            break;
        }
        return "";
    }

    public static void handleRecurrence(final JsonObject json, final Recurring r) {
        if (r == null) {
            Log.wtf(TAG, "recurring is null");
            return;
        }
        if (!r.getWeekdays().isEmpty()) {
            switch (r.getWeekdays().size()) {
            case 1:
                json.addProperty("recur", "weekly");
                return;
            case 7:
                json.addProperty("recur", "daily");
                return;
            case 5:
                final List<Integer> weekdays = r.getWeekdays();
                for (Integer i = Calendar.MONDAY; i <= Calendar.FRIDAY; i++) {
                    if (!weekdays.contains(i)) {
                        Log.w(TAG, "unsupported recurrence");
                        return;
                    }
                }
                json.addProperty("recur", "weekdays");
                return;
            default:
                Log.w(TAG, "unsupported recurrence");
                return;
            }
        }
        long interval = r.getInterval() / (1000 * 60);
        if (interval > 0) {
            if (r.getMinutes() > 0) {
                json.addProperty("recur", interval + "mins");
            } else if (r.getHours() > 0) {
                interval /= 60;
                json.addProperty("recur", interval + "hours");
            } else if (r.getDays() > 0) {
                interval /= 60 * 24;
                json.addProperty("recur", interval + "days");
            } else if (r.getMonths() > 0) {
                interval /= 60 * 24 * 30;
                json.addProperty("recur", interval + "months");
            } else {
                json.addProperty("recur", r.getYears() + "years");
            }
        }
    }

    private Pair<String, String> getStatus(final Task task,
                                           final Map<String, String> additionals, final boolean isMaster) {
        final Calendar now = new GregorianCalendar();
        now.setTimeInMillis(now.getTimeInMillis()
                            - DateTimeHelper.getTimeZoneOffset(true, now));
        String end = null;
        String status = "pending";
        if (task.getSyncState() == SYNC_STATE.DELETE) {
            status = "deleted";
            end = formatCal(now);
        } else if (task.isDone()) {
            status = "completed";
            if (additionals.containsKey("end")) {
                end = cleanQuotes(additionals.get("end"));
            } else {
                end = formatCal(now);
            }
        } else if (task.getRecurrence().isPresent() && isMaster) {
            status = "recurring";
        } else if (task.containsAdditional("status")) {
            status = cleanQuotes(task.getAdditionalString("status"));
        }
        return new Pair<>(end, status);
    }

    private String formatCalUTC(final Calendar calendar) {
        return formatCal(DateTimeHelper.getUTCCalendar(calendar));
    }
}
