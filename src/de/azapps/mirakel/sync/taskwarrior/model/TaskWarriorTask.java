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

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class TaskWarriorTask {

    private enum Priority {
        H, M, L;

        public static Priority fromString(String prio) {
            if (prio == null) {
                prio = "";
            }
            switch (prio.trim().toUpperCase()) {
            case "L":
                return L;
            case "M":
                return M;
            case "H":
                return H;
            default:
                throw new IllegalArgumentException("Unknown priority: " + prio);
            }
        }
    }
    private enum Status {
        PENDING, DELETED, COMPLETED, WAITING, RECURRING;

        public static Status fromString(String status) {
            if (status == null) {
                status = "";
            }
            switch (status.trim().toLowerCase()) {
            case "pending":
                return PENDING;
            case "deleted":
                return DELETED;
            case "completed":
                return COMPLETED;
            case "waiting":
                return WAITING;
            case "recurring":
                return RECURRING;
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
            }
        }
    }

    private class Annotation {
        public String description;
        public long entry;

        public Annotation(@NonNull final String description, final long entry) {
            this.description = description;
            this.entry = entry;
        }
    }

    //requried fields
    @NonNull
    private String UUID;
    @NonNull
    private Status status = Status.PENDING;
    @NonNull
    private long entry;
    @NonNull
    private String description;



    //optional fields
    @NonNull
    private Optional<Priority> priority = absent();
    @NonNull
    private Optional<String> project = absent();
    @NonNull
    private List<String> tags = new ArrayList<>();
    @NonNull
    private List<String> depends = new ArrayList<>();
    @NonNull
    private Optional<Long> due = absent();
    @NonNull
    private Optional<Long> start = absent();
    @NonNull
    private Optional<Long> end = absent();
    @NonNull
    private Optional<Long> until = absent();
    @NonNull
    private Optional<Long> wait = absent();
    @NonNull
    private Optional<Long> scheduled = absent();
    @NonNull
    private Optional<Long> modified = absent();
    @NonNull
    private List<Annotation> annotations = new ArrayList<>();

    //recurring
    @NonNull
    private Optional<String> mask = absent();
    @NonNull
    private Optional<String> recur = absent();
    @NonNull
    private Optional<Integer> imask = absent();
    @NonNull
    private Optional<String> parent = absent();

    //special mirakel fields
    @NonNull
    private Optional<Integer> priorityNumber = absent();
    @NonNull
    private Optional<Long> reminder = absent();
    @NonNull
    private Optional<Integer> progress = absent();

    //udas
    @NonNull
    private Map<String, String> uda = new HashMap<>();

    public TaskWarriorTask(@NonNull final String uuid, @NonNull final String status,
                           final @NonNull Calendar entry,
                           @NonNull final String description) {
        this.UUID = uuid;
        this.status = Status.fromString(status);
        this.entry = entry.getTimeInMillis() / 1000;
        this.description = description;
    }

    public void setPriority(@NonNull final String priority) {
        this.priority = of(Priority.fromString(priority));
    }

    public void setProject(@NonNull final String project) {
        this.project = of(project);
    }

    public void addTags(@NonNull final String tag) {
        this.tags.add(tag.replace("_", " "));
    }

    public void addDepends(@NonNull final String depends) {
        this.depends.add(depends);
    }

    public void setDue(@NonNull final Calendar due) {
        this.due = of(due.getTimeInMillis() / 1000);
    }

    public void setStart(@NonNull final Calendar start) {
        this.start = of(start.getTimeInMillis() / 1000);
    }

    public void setEnd(@NonNull final Calendar end) {
        this.end = of(end.getTimeInMillis() / 1000);
    }

    public void setUntil(@NonNull final Calendar until) {
        this.until = of(until.getTimeInMillis() / 1000);
    }

    public void setWait(@NonNull final Calendar wait) {
        this.wait = of(wait.getTimeInMillis());
    }

    public void setScheduled(@NonNull final Calendar scheduled) {
        this.scheduled = of(scheduled.getTimeInMillis() / 1000);
    }

    public void setModified(@NonNull final Calendar modified) {
        this.modified = of(modified.getTimeInMillis() / 1000);
    }

    public void setMask(@NonNull final String mask) {
        this.mask = of(mask);
    }

    public void setRecur(@NonNull final String recur) {
        this.recur = of(recur);
    }

    public void setImask(final int imask) {
        this.imask = of(imask);
    }

    public void setParent(@NonNull final String parent) {
        this.parent = of(parent);
    }

    public void addUDA(@NonNull final String key, @NonNull final String value) {
        this.uda.put(key, value);
    }

    public void setPriorityNumber(final int priority) {
        this.priorityNumber = of(priority);
    }

    public void setReminder(@NonNull final Calendar reminder) {
        this.reminder = of(reminder.getTimeInMillis());
    }

    public void addAnnotation(@NonNull final String description, @NonNull final Calendar entry) {
        annotations.add(new Annotation(description, entry.getTimeInMillis()));
    }

    public void setProgress(int progress) {
        this.progress = of(progress);
    }

    public void addDepends(final @NonNull String[] split) {
        depends.addAll(Arrays.asList(split));
    }

    public void addTags(final @NonNull String[] split) {
        tags.addAll(Arrays.asList(split));
    }

    @NonNull
    public String getUUID() {
        return UUID;
    }

    @NonNull
    public ContentProviderOperation getUpdate(final long local_id,
            final @NonNull String additional_column, final @NonNull Map<String, Long> projectMapping,
            final long inboxID) {
        return ContentProviderOperation.newUpdate(Task.URI).withValues(getContentValues(projectMapping,
                of(additional_column), inboxID)).withSelection(Task.ID + "=?", new String[] {String.valueOf(local_id)}).build();
    }

    @NonNull
    public ContentProviderOperation getInsert(final long inboxID,
            @NonNull final Map<String, Long> projectMapping) {
        return ContentProviderOperation.newInsert(Task.URI).withValues(getContentValues(projectMapping,
                Optional.<String>absent(), inboxID)).build();
    }

    @NonNull
    private ContentValues getContentValues(final @NonNull Map<String, Long> projectMapping,
                                           final @NonNull Optional<String> additional_string, final long inboxID) {
        Map<String, String> additionalEntries = OptionalUtils.withOptional(additional_string,
        new Function<String, Map<String, String>>() {
            @Override
            public Map<String, String> apply(String input) {
                return Task.parseAdditionalEntries(input);
            }
        }, new HashMap<String, String>());

        ContentValues cv = new ContentValues();
        cv.put(Task.NAME, description);
        cv.put(DatabaseHelper.CREATED_AT, entry);
        cv.put(Task.UUID, UUID);
        switch (status) {
        case PENDING:
            cv.put(Task.DONE, false);
            break;
        case DELETED:
            // must be handled elsewhere
            throw new IllegalStateException("Task is deleted, cannot get the contetvalues");
        case COMPLETED:
            cv.put(Task.DONE, true);
            break;
        case WAITING:
            additionalEntries.put("status", "waiting");
            break;
        case RECURRING:
            cv.put(Task.RECURRING_SHOWN, false);
            break;
        }
        if (priority.isPresent()) {
            switch (priority.get()) {
            case H:
                cv.put(Task.PRIORITY, 2);
                break;
            case M:
                cv.put(Task.PRIORITY, 1);
                break;
            case L:
                if (priorityNumber.isPresent() && (priorityNumber.get() == -1 || priorityNumber.get() == -2)) {
                    cv.put(Task.PRIORITY, priorityNumber.get());
                } else {
                    cv.put(Task.PRIORITY, -2);
                }
                break;
            }
        } else {
            cv.put(Task.PRIORITY, 0);
        }
        if (project.isPresent()) {
            cv.put(Task.LIST_ID, projectMapping.get(project.get()));
        } else {
            additionalEntries.put(Task.NO_PROJECT, "true");
            cv.put(Task.LIST_ID, inboxID);
        }
        if (due.isPresent()) {
            cv.put(Task.DUE, due.get());
        } else {
            cv.put(Task.DUE, (Integer)null);
        }

        if (reminder.isPresent()) {
            cv.put(Task.REMINDER, reminder.get());
        } else {
            cv.put(Task.REMINDER, (Integer)null);
        }

        if (progress.isPresent()) {
            cv.put(Task.PROGRESS, progress.get());
        } else {
            cv.put(Task.PROGRESS, 0);
        }

        if (modified.isPresent()) {
            cv.put(DatabaseHelper.UPDATED_AT, modified.get());
        } else {
            cv.put(DatabaseHelper.UPDATED_AT, new GregorianCalendar().getTimeInMillis());
        }

        cv.put(Task.CONTENT, TextUtils.join("\n", Collections2.transform(annotations,
        new Function<Annotation, String>() {
            @Override
            public String apply(Annotation input) {
                return input.description;
            }
        })));

        handleAdditionalEntries(additionalEntries);

        cv.put(Task.ADDITIONAL_ENTRIES, Task.serializeAdditionalEntries(additionalEntries));
        return cv;
    }

    private void handleAdditionalEntries(Map<String, String> additionalEntries) {
        // add missing fields as uda/additional entry
        if (start.isPresent()) {
            additionalEntries.put("start", String.valueOf(start.get()));
        } else {
            additionalEntries.remove("start");
        }

        if (end.isPresent()) {
            additionalEntries.put("end", String.valueOf(end.get()));
        } else {
            additionalEntries.remove("end");
        }

        if (scheduled.isPresent()) {
            additionalEntries.put("scheduled", String.valueOf(scheduled.get()));
        } else {
            additionalEntries.remove("scheduled");
        }

        if (until.isPresent()) {
            additionalEntries.put("until", String.valueOf(until.get()));
        } else {
            additionalEntries.remove("until");
        }

        if (wait.isPresent()) {
            additionalEntries.put("wait", String.valueOf(wait.get()));
        } else {
            additionalEntries.remove("wait");
        }

        additionalEntries.putAll(uda);
    }

    public boolean isNotDeleted() {
        return status != Status.DELETED;
    }

    public boolean hasProject() {
        return project.isPresent();
    }

    public boolean isRecurringMaster() {
        return mask.isPresent();
    }

    public boolean isRecurringChild() {
        return imask.isPresent() && parent.isPresent();
    }

    public int getImask() {
        if (imask.isPresent()) {
            return imask.get();
        }
        throw new IllegalStateException("There is no imask");
    }

    @NonNull
    public String getParent() {
        if (parent.isPresent()) {
            return parent.get();
        }
        throw new IllegalStateException("There is no parent");
    }


    @NonNull
    public String getProject() {
        if (project.isPresent()) {
            return project.get();
        } else {
            throw new IllegalStateException("No project set");
        }
    }

    @NonNull
    public List<String> getTags() {
        return tags;
    }

    @NonNull
    public List<String> getDependencies() {
        return depends;
    }

    public TaskWarriorRecurrence getRecurrence() throws
        TaskWarriorRecurrence.NotSupportedRecurrenceExeption {
        if (recur.isPresent()) {
            Optional<Calendar> until = OptionalUtils.withOptional(this.until,
            new Function<Long, Optional<Calendar>>() {
                @Override
                public Optional<Calendar> apply(Long input) {
                    Calendar c = new GregorianCalendar();
                    c.setTimeInMillis(input);
                    return of(c);
                }
            }, Optional.<Calendar>absent());
            return new TaskWarriorRecurrence(recur.get(), until);
        }
        throw new IllegalStateException("There is no recurrence");
    }
}
