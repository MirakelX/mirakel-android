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
package de.azapps.mirakel.sync.taskwarrior.model;

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

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Period;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class TaskWarriorTaskSerializer implements JsonSerializer<Task> {

    private static final String TAG = "TaskWarriorTaskSerializer";
    private final Context mContext;

    public TaskWarriorTaskSerializer(final Context ctx) {
        this.mContext = ctx;
    }

    private String formatCal(final DateTime calendar) {
        if (calendar.isBefore(0L)) {
            return ISODateTimeFormat.basicDateTimeNoMillis().print(10L);
        }
        return ISODateTimeFormat.basicDateTimeNoMillis().withZoneUTC().print(calendar);
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
    public JsonElement serialize(final Task src, final Type typeOfSrc,
                                 final JsonSerializationContext context) {
        final JsonObject json = new JsonObject();
        final Map<String, String> additionals = src.getAdditionalEntries();
        boolean isMaster = false;
        if (src.getRecurrence().isPresent()) {
            if (new MirakelQueryBuilder(mContext).and(Recurring.CHILD, Operation.EQ,
                    src).count(MirakelInternalContentProvider.RECURRING_TW_URI) == 0) {
                isMaster = true;
            }
        }
        final Pair<String, String> statusEnd = getStatus(src, isMaster);
        final String status = statusEnd.second;
        final String end = statusEnd.first;
        String priority = null;
        switch (src.getPriority()) {
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
        String uuid = src.getUUID();
        if (uuid.trim().isEmpty()) {
            uuid = java.util.UUID.randomUUID().toString();
            src.setUUID(uuid);
            src.save(false);
        }
        json.addProperty("uuid", uuid);
        json.addProperty("status", status);
        json.addProperty("entry", formatCal(src.getCreatedAt()));
        json.addProperty("description", src.getName());
        if (src.getDue().isPresent()) {
            json.addProperty("due", formatCal(src.getDue().get()));
        }
        if (!src.containsAdditional(Task.NO_PROJECT)) {
            json.addProperty("project", src.getList().getName());
        }
        if (priority != null) {
            json.addProperty("priority", priority);
            if ("L".equals(priority) && (src.getPriority() != -2)) {
                json.addProperty("priorityNumber", src.getPriority());
            }
        }
        json.addProperty("modified", formatCal(src.getUpdatedAt()));
        if (src.getReminder().isPresent()) {
            json.addProperty("reminder", formatCal(src.getReminder().get()));
        }
        if (end != null) {
            json.addProperty("end", end);
        }
        if (src.getProgress() != 0) {
            json.addProperty("progress", src.getProgress());
        }
        // Tags
        if (!src.getTags().isEmpty()) {
            final JsonArray tags = new JsonArray();
            for (final Tag t : src.getTags()) {
                // taskwarrior does not like whitespaces
                tags.add(new JsonPrimitive(t.getName().trim().replace(" ", "_")));
            }
            json.add("tags", tags);
        }
        // End Tags
        // Annotations
        if (!src.getContent().isEmpty()) {
            final JsonArray annotations = new JsonArray();
            /*
             * An annotation in taskd is a line of content in Mirakel!
             */
            final String annotationsList[] = src.getContent().split("\n");
            DateTime updatedAt = src.getUpdatedAt();
            for (final String a : annotationsList) {
                final JsonObject line = new JsonObject();
                line.addProperty("entry", formatCal(updatedAt));
                line.addProperty("description", a.replace("\n", ""));
                annotations.add(line);
                updatedAt = updatedAt.plusSeconds(1);
            }
            json.add("annotations", annotations);
        }
        // Anotations end
        // TW.depends==Mirakel.subtasks!
        // Dependencies
        if (src.countSubtasks() > 0L) {
            boolean first1 = true;
            final List<Task> subTasks = src.getSubtasks();
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
        if (src.getRecurrence().isPresent() && src.getDue().isPresent()) {
            handleRecurrence(json, src.getRecurrence().get());
            if (isMaster) {
                final Cursor cursor = mContext.getContentResolver()
                                      .query(MirakelInternalContentProvider.RECURRING_TW_URI,
                                             new String[] { "child", "offsetCount" },
                                             "parent=?", new String[] {String.valueOf(src.getId())},
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
                                                            false).second));
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
                json.addProperty("mask", mask.toString());
            } else {
                final Cursor cursor = mContext.getContentResolver()
                                      .query(MirakelInternalContentProvider.RECURRING_TW_URI,
                                             new String[] { "parent", "offsetCount" },
                                             "child=?", new String[] {String.valueOf(src.getId())},
                                             null);
                cursor.moveToFirst();
                if (cursor.getCount() > 0) {
                    final Optional<Task> master = Task.get(cursor.getLong(0));
                    if (!master.isPresent()) {
                        // The parent is gone. This should not happen and we
                        // should delete the child then
                        src.destroy();
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
        for (final Map.Entry<String, String> entry : additionals.entrySet()) {
            if (!entry.getKey().equals(Task.NO_PROJECT)
                && !"status".equals(entry.getKey())) {
                json.addProperty(entry.getKey(), cleanQuotes(entry.getValue()));
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
                for (Integer i = DateTimeConstants.MONDAY; i <= DateTimeConstants.FRIDAY; i++) {
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
        final long interval = r.getIntervalMs() / (1000L * 60L);
        if (interval > 0L) {
            Period p = r.getInterval();
            if (r.getInterval().getMinutes() > 0) {
                json.addProperty("recur", p.toStandardMinutes().getMinutes() + "mins");
            } else if (r.getInterval().getHours() > 0) {
                json.addProperty("recur", p.toStandardHours().getHours() + "hours");
            } else if (r.getInterval().getDays() > 0) {
                json.addProperty("recur", p.toStandardDays().getDays() + "days");
            } else if (r.getInterval().getWeeks() > 0) {
                json.addProperty("recur", p.toStandardWeeks().getWeeks() + "weeks");
            } else if (r.getInterval().getMonths() > 0) {
                json.addProperty("recur", p.getMonths() + (12 * p.getYears()) + "months");
            } else {
                json.addProperty("recur", p.getYears() + "years");
            }
        }
    }

    private Pair<String, String> getStatus(final Task task,
                                           final boolean isMaster) {

        final DateTime now = new DateTime();
        String end = null;
        String status = "pending";
        if (task.getSyncState() == SYNC_STATE.DELETE) {
            status = "deleted";
            end = formatCal(now);
        } else if (task.isDone()) {
            status = "completed";
            if (task.containsAdditional("end")) {
                end = cleanQuotes(task.getAdditionalRaw("end"));
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

}
