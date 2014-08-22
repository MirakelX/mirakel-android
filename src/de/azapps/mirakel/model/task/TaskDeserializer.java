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

import android.content.Context;
import android.util.Pair;
import android.util.SparseBooleanArray;

import com.google.common.base.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

public class TaskDeserializer implements JsonDeserializer<Task> {

    private static final String TAG = "TaskDeserializer";
    private final boolean isTW;
    private final AccountMirakel account;
    private final Context context;

    public TaskDeserializer(final boolean isTW, final AccountMirakel account,
                            final Context ctx) {
        this.isTW = isTW;
        this.account = account;
        this.context = ctx;
    }

    @Override
    public Task deserialize(final JsonElement json, final Type type,
                            final JsonDeserializationContext ctx) throws JsonParseException {
        final JsonObject el = json.getAsJsonObject();
        Task t = null;
        JsonElement id = el.get("id");
        if (id != null && !this.isTW) {// use uuid for tw-sync
            t = Task.get(id.getAsLong());
        } else {
            id = el.get("uuid");
            if (id != null) {
                t = Task.getByUUID(id.getAsString());
            }
        }
        if (t == null) {
            t = new Task();
        }
        if (this.isTW) {
            t.setDue(Optional.<Calendar>absent());
            t.setDone(false);
            t.setContent("");
            t.setPriority(0);
            t.setProgress(0);
            t.setList(null, false);
            t.clearAdditionalEntries();
            t.setIsRecurringShown(true);
        }
        // Name
        final Set<Entry<String, JsonElement>> entries = el.entrySet();
        boolean setPrioFromNumber = false;
        Calendar end = null;
        for (final Entry<String, JsonElement> entry : entries) {
            String key = entry.getKey();
            final JsonElement val = entry.getValue();
            if (key == null || key.equalsIgnoreCase("id")) {
                continue;
            }
            key = key.toLowerCase();
            switch (key) {
            case "uuid":
                t.setUUID(val.getAsString());
                break;
            case "name":
            case "description":
                t.setName(val.getAsString());
                break;
            case "content":
                String content = val.getAsString();
                if (content == null) {
                    content = "";
                }
                t.setContent(content);
                break;
            case "priority":
                if (setPrioFromNumber) {
                    break;
                }
            //$FALL-THROUGH$
            case "priorityNumber":
                final String prioString = val.getAsString().trim();
                if (prioString.equalsIgnoreCase("L") && t.getPriority() != -1) {
                    t.setPriority(-2);
                } else if (prioString.equalsIgnoreCase("M")) {
                    t.setPriority(1);
                } else if (prioString.equalsIgnoreCase("H")) {
                    t.setPriority(2);
                } else if (!prioString.equalsIgnoreCase("L")) {
                    t.setPriority((int) val.getAsFloat());
                    setPrioFromNumber = true;
                }
                break;
            case "progress":
                final int progress = (int) val.getAsDouble();
                t.setProgress(progress);
                break;
            case "list_id": {
                Optional<ListMirakel> list = ListMirakel.get(val.getAsInt());
                if (!list.isPresent()) {
                    list = Optional.fromNullable(SpecialList.firstSpecialSafe().getDefaultList());
                }
                t.setList(list.get(), true);
                break;
            }
            case "project": {
                Optional<ListMirakel> list = ListMirakel.findByName(val.getAsString(),
                                             this.account);
                if (list == null
                    || list.get().getAccount().getId() != this.account.getId()) {
                    try {
                        list = Optional.fromNullable(ListMirakel.newList(val.getAsString(),
                                                     ListMirakel.SORT_BY.OPT, this.account));
                    } catch (ListMirakel.ListAlreadyExistsException e) {
                        // This can not happen!
                        throw new RuntimeException("ListAlreadyExist while syncing from taskd. Thats impossible", e);
                    }
                }
                t.setList(list.get(), true);
                break;
            }
            case "created_at":
                t.setCreatedAt(val.getAsString().replace(":", ""));
                break;
            case "updated_at":
                t.setUpdatedAt(val.getAsString().replace(":", ""));
                break;
            case "entry":
                t.setCreatedAt(handleDate(val));
                break;
            case "modification":
            case "modified":
                t.setUpdatedAt(handleDate(val));
                break;
            case "done":
                t.setDone(val.getAsBoolean());
                break;
            case "status":
                final String status = val.getAsString();
                if ("completed".equalsIgnoreCase(status)) {
                    t.setDone(true);
                } else if ("deleted".equalsIgnoreCase(status)) {
                    t.setSyncState(SYNC_STATE.DELETE);
                } else {
                    t.setDone(false);
                    if (!"recurring".equals(status)) {
                        t.addAdditionalEntry(key, "\"" + val.getAsString()
                                             + "\"");
                    }
                    // TODO don't ignore waiting !!!
                }
                break;
            case "due":
                Calendar due = parseDate(val.getAsString(), "yyyy-MM-dd");
                if (due == null) {
                    due = parseDate(val.getAsString(),
                                    this.context.getString(R.string.TWDateFormat));
                    // try to workaround timezone-bug
                    if (due != null) {
                        due.setTimeInMillis(due.getTimeInMillis()
                                            + DateTimeHelper.getTimeZoneOffset(true, due));
                    }
                }
                t.setDue(fromNullable(due));
                break;
            case "reminder":
                Calendar reminder = parseDate(val.getAsString(), "yyyy-MM-dd");
                if (reminder == null) {
                    reminder = parseDate(val.getAsString(),
                                         this.context.getString(R.string.TWDateFormat));
                }
                t.setReminder(fromNullable(reminder));
                break;
            case "annotations":
                t.setContent(handleContent(val));
                break;
            case "sync_state":
                if (isTW) {
                    handleAdditionalEnties(t, key, val);
                }
                break;
            case "depends":
                t.setDependencies(val.getAsString().split(","));
                break;
            case "tags":
                handleTags(t, val);
                break;
            case "recur":
                final Recurring r = parseTaskWarriorRecurrence(val
                                    .getAsString());
                if (r != null) {
                    t.setRecurrence(r.getId());
                }
                break;
            case "imask":
                t.addRecurringChild(new Pair<>(el.get("parent").getAsString(),
                                               (int) val.getAsFloat()));
                break;
            case "parent":
            case "mask":
                // ignore this
                break;
            case "until":
                end = parseDate(val.getAsString(),
                                this.context.getString(R.string.TWDateFormat));
                break;
            default:
                handleAdditionalEnties(t, key, val);
                break;
            }
        }
        if (t.getRecurring() != null) {
            final Recurring r = t.getRecurring();
            r.setEndDate(Optional.fromNullable(end));
            r.save();
        }
        return t;
    }

    private static void handleAdditionalEnties(final Task t, final String key,
            final JsonElement val) {
        if (val.isJsonPrimitive()) {
            final JsonPrimitive p = (JsonPrimitive) val;
            if (p.isBoolean()) {
                t.addAdditionalEntry(key, val.getAsBoolean() + "");
            } else if (p.isNumber()) {
                t.addAdditionalEntry(key, val.getAsInt() + "");
            } else if (p.isJsonNull()) {
                t.addAdditionalEntry(key, "null");
            } else if (p.isString()) {
                t.addAdditionalEntry(key, "\"" + val.getAsString() + "\"");
            } else {
                Log.w(TAG, "unkown json-type");
            }
        } else if (val.isJsonArray()) {
            final JsonArray a = (JsonArray) val;
            String s = "[";
            boolean first = true;
            for (final JsonElement e : a) {
                if (e.isJsonPrimitive()) {
                    final JsonPrimitive p = (JsonPrimitive) e;
                    String add;
                    if (p.isBoolean()) {
                        add = p.getAsBoolean() + "";
                    } else if (p.isNumber()) {
                        add = p.getAsInt() + "";
                    } else if (p.isString()) {
                        add = "\"" + p.getAsString() + "\"";
                    } else if (p.isJsonNull()) {
                        add = "null";
                    } else {
                        Log.w(TAG, "unkown json-type");
                        break;
                    }
                    s += (first ? "" : ",") + add;
                    first = false;
                } else {
                    Log.w(TAG, "unkown json-type");
                }
            }
            t.addAdditionalEntry(key, s + "]");
        } else {
            Log.w(TAG, "unkown json-type");
        }
    }

    private static void handleTags(final Task t, final JsonElement val) {
        final JsonArray tags = val.getAsJsonArray();
        final List<Tag> currentTags = new ArrayList<>(t.getTags());
        for (final JsonElement tag : tags) {
            if (tag.isJsonPrimitive()) {
                String tagName = tag.getAsString();
                tagName = tagName.replace("_", " ");
                final Tag newTag = Tag.newTag(tagName);
                if (!currentTags.remove(newTag)) {
                    // tag is not linked with this task
                    t.addTag(newTag, false, true);
                }
            }
        }
        for (final Tag tag : currentTags) {
            // remove unused tags
            t.removeTag(tag, false, true);
        }
    }

    private static String handleContent(final JsonElement val) {
        String content = "";
        try {
            final JsonArray annotations = val.getAsJsonArray();
            boolean first = true;
            for (final JsonElement a : annotations) {
                if (first) {
                    first = false;
                } else {
                    content += "\n";
                }
                content += a.getAsJsonObject().get("description").getAsString();
            }
        } catch (final Exception e) {
            Log.e(TAG, "cannot parse json", e);
        }
        return content;
    }

    private Calendar handleDate(final JsonElement val) {
        Calendar createdAt = parseDate(val.getAsString(),
                                       this.context.getString(R.string.TWDateFormat));
        if (createdAt == null) {
            createdAt = new GregorianCalendar();
        } else {
            createdAt.add(Calendar.SECOND,
                          DateTimeHelper.getTimeZoneOffset(false, createdAt));
        }
        return createdAt;
    }

    private static Calendar parseDate(final String date, final String format) {
        final GregorianCalendar temp = new GregorianCalendar();
        try {
            temp.setTime(new SimpleDateFormat(format, Locale.getDefault())
                         .parse(date));
            return temp;
        } catch (final ParseException e) {
            return null;
        }
    }

    public static Recurring parseTaskWarriorRecurrence(final String recur) {
        final Scanner in = new Scanner(recur);
        in.useDelimiter("[^0-9]+");
        int number = 1;
        if (in.hasNextInt()) {
            number = in.nextInt();
        }
        in.close();
        // remove number and possible sign(recurrence should be positive but who
        // knows)
        final Recurring r;
        switch (recur.replace("" + number, "").replace("-", "")) {
        case "yearly":
        case "annual":
            number = 1;
        //$FALL-THROUGH$
        case "years":
        case "year":
        case "yrs":
        case "yr":
        case "y":
            r = new Recurring(0, recur, 0, 0, 0, 0, number, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "semiannual":
            r = new Recurring(0, recur, 0, 0, 0, 6, 0, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(), true,
                              false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "biannual":
        case "biyearly":
            r = new Recurring(0, recur, 0, 0, 0, 0, 2, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(), true,
                              false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "bimonthly":
            r = new Recurring(0, recur, 0, 0, 0, 2, 0, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(), true,
                              true, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "biweekly":
        case "fortnight":
            r = new Recurring(0, recur, 0, 0, 14, 0, 0, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(), true,
                              false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "daily":
            number = 1;
        //$FALL-THROUGH$
        case "days":
        case "day":
        case "d":
            r = new Recurring(0, recur, 0, 0, number, 0, 0, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "hours":
        case "hour":
        case "hrs":
        case "hr":
        case "h":
            r = new Recurring(0, recur, 0, number, 0, 0, 0, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "minutes":
        case "mins":
        case "min":
            r = new Recurring(0, recur, number, 0, 0, 0, 0, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "monthly":
            number = 1;
        //$FALL-THROUGH$
        case "months":
        case "month":
        case "mnths":
        case "mths":
        case "mth":
        case "mos":
        case "mo":
            r = new Recurring(0, recur, 0, 0, 0, number, 0, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "quarterly":
            number = 1;
        //$FALL-THROUGH$
        case "quarters":
        case "qrtrs":
        case "qtrs":
        case "qtr":
        case "q":
            r = new Recurring(0, recur, 0, 0, 0, 3 * number, 0, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        default:
        case "seconds":
        case "secs":
        case "sec":
        case "s":
            Log.w(TAG, "mirakel des not support " + recur);
            r = null;
            break;
        case "weekdays":
            final SparseBooleanArray weekdays = new SparseBooleanArray(7);
            for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
                weekdays.put(i, i != Calendar.SATURDAY && i != Calendar.SUNDAY);
            }
            r = new Recurring(0, recur, 0, 0, 0, 0, 0, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(), true,
                              false, weekdays, Optional.<Long>absent());
            break;
        case "sennight":
        case "weekly":
            number = 1;
        //$FALL-THROUGH$
        case "weeks":
        case "week":
        case "wks":
        case "wk":
        case "w":
            r = new Recurring(0, recur, 0, 0, 7 * number, 0, 0, true, Optional.<Calendar>absent(),
                              Optional.<Calendar>absent(), true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        }
        return r.create();
    }

}
