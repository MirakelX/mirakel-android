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

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Optional;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

abstract class TaskBase extends ModelBase {
    public static final String ADDITIONAL_ENTRIES = "additional_entries";
    public static final String CONTENT = "content";
    public static final String DONE = "done";
    public static final String DUE = "due";
    public static final String LIST_ID = "list_id";
    public static final String PRIORITY = "priority";
    public static final String PROGRESS = "progress";
    public static final String RECURRING = "recurring";
    public static final String RECURRING_REMINDER = "recurring_reminder";
    public static final String REMINDER = "reminder";
    public static final String RECURRING_SHOWN = "is_shown_recurring";
    private static final String TAG = "TaskBase";
    public static final String UUID = "uuid";

    private Map<String, String> additionalEntries = null;
    @NonNull
    protected String additionalEntriesString = "";
    @NonNull
    protected String content = "";
    @NonNull
    protected Calendar createdAt = new GregorianCalendar();
    protected boolean done;
    @NonNull
    protected Optional<Calendar> due = absent();
    @NonNull
    protected final Map<String, Boolean> edited = new HashMap<>();
    @NonNull
    protected ListMirakel list;
    protected int priority;
    protected int progress;
    protected long recurrence;
    protected long recurringReminder;
    protected boolean isRecurringShown;
    @NonNull
    protected Optional<Calendar> reminder = absent();
    @NonNull
    protected SYNC_STATE syncState = SYNC_STATE.NOTHING;
    @NonNull
    protected Calendar updatedAt = new GregorianCalendar();
    @NonNull
    protected String uuid = "";
    @NonNull
    protected Optional<List<Tag>> tags = absent();
    private boolean isStub = false;

    TaskBase() {
        // nothing
        super(0, "");
    }

    TaskBase(final long newId, @NonNull final String newUuid, @NonNull final ListMirakel newList,
             @NonNull final String newName, @NonNull final String newContent,
             final boolean newDone, final @NonNull Optional<Calendar> newDue,
             final @NonNull Optional<Calendar> newReminder, final int newPriority,
             @NonNull final Calendar newCreatedAt, @NonNull final Calendar newUpdatedAt,
             @NonNull final SYNC_STATE newSyncState,
             @NonNull final String newAdditionalEntriesString, final int recurring,
             final int newRecurringReminder, final int newProgress,
             final boolean shown) {
        super(newId, newName);
        this.uuid = newUuid;
        setList(newList, false);
        setContent(newContent);
        setDone(newDone);
        setDue(newDue);
        setReminder(newReminder);
        setPriority(newPriority);
        this.setCreatedAt(newCreatedAt);
        this.setUpdatedAt(newUpdatedAt);
        setSyncState(newSyncState);
        this.additionalEntriesString = newAdditionalEntriesString;
        this.recurrence = recurring;
        this.recurringReminder = newRecurringReminder;
        this.progress = newProgress;
        clearEdited();
        this.tags = absent();
        setIsRecurringShown(shown);
    }

    TaskBase(@NonNull final String newName, @NonNull final ListMirakel list) {
        super(0, newName);
        this.uuid = java.util.UUID.randomUUID().toString();
        setList(list, false);
        setContent("");
        setDone(false);
        setDue(Optional.<Calendar>absent());
        setReminder(Optional.<Calendar>absent());
        this.priority = 0;
        this.setCreatedAt(new GregorianCalendar());
        this.setUpdatedAt(new GregorianCalendar());
        setSyncState(SYNC_STATE.NOTHING);
        this.recurrence = -1;
        this.recurringReminder = -1;
        this.progress = 0;
        setIsRecurringShown(true);
        clearEdited();
    }

    public void addAdditionalEntry(@NonNull final String key, final String value) {
        initAdditionalEntries();
        Log.d(TaskBase.TAG, "add: " + key + ":" + value);
        this.additionalEntries.put(key, value);
    }

    void clearEdited() {
        this.edited.clear();
    }

    /**
     * Use getAdditional[Type] instead
     *
     * @return
     */
    @Deprecated
    public Map<String, String> getAdditionalEntries() {
        initAdditionalEntries();
        return this.additionalEntries;
    }

    public boolean containsAdditional(final String key) {
        initAdditionalEntries();
        return additionalEntries.containsKey(key);
    }

    @Nullable
    public String getAdditionalRaw(final String key) {
        initAdditionalEntries();
        return this.additionalEntries.get(key);
    }

    @Nullable
    public String getAdditionalString(final String key) {
        final String str = getAdditionalRaw(key);
        if (str == null) {
            return null;
        }
        return str.substring(1, str.length() - 1);
    }

    @Nullable
    public Integer getAdditionalInt(final String key) {
        final String str = getAdditionalRaw(key);
        if (str == null) {
            return null;
        }
        return Integer.valueOf(str);
    }

    public boolean existAdditional(final String key) {
        initAdditionalEntries();
        return this.additionalEntries.containsKey(key);
    }

    void setIsRecurringShown(final boolean shown) {
        this.isRecurringShown = shown;
    }

    boolean isRecurringShown() {
        return this.isRecurringShown;
    }

    @NonNull
    protected String getAdditionalEntriesString() {
        initAdditionalEntries();
        this.additionalEntriesString = serializeAdditionalEntries(this.additionalEntries);
        return this.additionalEntriesString;
    }

    public void clearAdditionalEntries() {
        this.additionalEntries = new HashMap<>();
        this.additionalEntriesString = "";
    }

    @NonNull
    public static String serializeAdditionalEntries(
        @NonNull final Map<String, String> additionalEntries) {
        String additionalEntriesStr = "{";
        boolean first = true;
        for (final Entry<String, String> p : additionalEntries.entrySet()) {
            additionalEntriesStr += (first ? "" : ",") + "\"" + p.getKey()
                                    + "\":" + p.getValue();
            first = false;
        }
        return additionalEntriesStr + "}";
    }

    @NonNull
    public String getContent() {
        return this.content;
    }

    @Override
    @NonNull
    public ContentValues getContentValues() throws NoSuchListException {
        final ContentValues cv = super.getContentValues();
        cv.put(TaskBase.UUID, this.uuid);
        cv.put(TaskBase.LIST_ID, this.list.getId());
        cv.put(TaskBase.CONTENT, this.content);
        cv.put(TaskBase.DONE, this.done);
        if (this.due.isPresent()) {
            if (this.due.get().get(Calendar.HOUR) == 0
                && this.due.get().get(Calendar.MINUTE) == 0
                && this.due.get().get(Calendar.SECOND) == 0) {
                cv.put(TaskBase.DUE, this.due.get().getTimeInMillis() / 1000);
            } else {
                cv.put(TaskBase.DUE, DateTimeHelper.getUTCTime(this.due));
            }
        } else {
            cv.put(TaskBase.DUE, (Integer) null);
        }
        cv.put(TaskBase.REMINDER, DateTimeHelper.getUTCTime(this.reminder));
        cv.put(TaskBase.PRIORITY, this.priority);
        cv.put(DatabaseHelper.CREATED_AT,
               this.createdAt.getTimeInMillis() / 1000);
        cv.put(DatabaseHelper.UPDATED_AT,
               this.updatedAt.getTimeInMillis() / 1000);
        cv.put(DatabaseHelper.SYNC_STATE_FIELD, this.syncState.toInt());
        cv.put(TaskBase.RECURRING, this.recurrence);
        cv.put(TaskBase.RECURRING_REMINDER, this.recurringReminder);
        cv.put(TaskBase.PROGRESS, this.progress);
        cv.put(TaskBase.RECURRING_SHOWN, this.isRecurringShown);
        cv.put("additional_entries", getAdditionalEntriesString());
        return cv;
    }

    @NonNull
    public Calendar getCreatedAt() {
        return this.createdAt;
    }

    @NonNull
    public Optional<Calendar> getDue() {
        return this.due;
    }

    @NonNull
    public ListMirakel getList() {
        return this.list;
    }

    public int getPriority() {
        return this.priority;
    }

    public int getProgress() {
        return this.progress;
    }

    public long getRecurrenceId() {
        return this.recurrence;
    }

    public Recurring getRecurring() {
        return Recurring.get(this.recurrence);
    }

    public Recurring getRecurringReminder() {
        return Recurring.get(this.recurringReminder);
    }

    public long getRecurringReminderId() {
        return this.recurringReminder;
    }

    @NonNull
    public Optional<Calendar> getReminder() {
        return this.reminder;
    }

    @NonNull
    public SYNC_STATE getSyncState() {
        return this.syncState;
    }

    @NonNull
    public Calendar getUpdatedAt() {
        return this.updatedAt;
    }

    @NonNull
    public String getUUID() {
        return this.uuid;
    }

    public boolean hasRecurringReminder() {
        return this.recurringReminder > 0;
    }

    /**
     * This function parses the additional fields only if it is necessary
     */
    private void initAdditionalEntries() {
        if (this.additionalEntries == null) {
            this.additionalEntries = parseAdditionalEntries(this.additionalEntriesString);
        }
    }

    protected void setAdditionalEntries(@NonNull final String additional) {
        this.additionalEntriesString = additional;
        initAdditionalEntries();
    }

    @NonNull
    public static Map<String, String> parseAdditionalEntries(
        @NonNull final String additionalEntriesString) {
        final Map<String, String> ret = new HashMap<>();
        if (additionalEntriesString != null
            && !additionalEntriesString.trim().equals("")
            && !additionalEntriesString.trim().equals("null")) {
            final String t = additionalEntriesString.substring(1,
                             additionalEntriesString.length() - 1);// remove {
            // and }
            final String[] parts = t.split(",");
            String key = null;
            for (final String p : parts) {
                final String[] keyValue = p.split(":");
                if (keyValue.length == 2) {
                    key = keyValue[0];
                    key = key.replaceAll("\"", "");
                    ret.put(key, keyValue[1]);
                } else if (keyValue.length == 1 && key != null) {
                    ret.put(key, ret.get(key) + "," + keyValue[0]);
                }
            }
        }
        return ret;
    }

    public boolean isDone() {
        return this.done;
    }

    boolean isEdited() {
        return this.edited.size() > 0;
    }

    boolean isEdited(final String key) {
        return this.edited.containsKey(key);
    }

    public void setContent(@NonNull String newContent) {
        if (this.content.equals(newContent)) {
            return;
        }
        newContent = newContent.trim().replace("\\n", "\n");
        newContent = newContent.replace("\\\"", "\"");
        this.content = newContent.replace("\b", "");
        this.edited.put(TaskBase.CONTENT, true);
    }

    public void setCreatedAt(@NonNull final Calendar created_at) {
        this.createdAt = created_at;
    }

    public void setCreatedAt(@NonNull final String created_at) {
        try {
            setCreatedAt(DateTimeHelper.parseDateTime(created_at));
        } catch (final ParseException e) {
            setCreatedAt(new GregorianCalendar());
        }
    }

    /**
     * @param newDone is the task marked as done?
     * @return The id of the new task, created by recurring or -1
     */
    @NonNull
    public Optional<Task> setDone(final boolean newDone) {
        if (this.done == newDone) {
            return absent();
        }
        this.done = newDone;
        this.edited.put(TaskBase.DONE, true);
        if (newDone && this.recurrence != -1 && this.due.isPresent()) {
            if (getRecurring() != null) {
                final Optional<Task> oldTask = Task.get(getId());
                if (!oldTask.isPresent()) {
                    return absent();
                }
                final Task newTask = getRecurring().incrementRecurringDue(
                                         oldTask.get());
                // set the sync state of the old task to recurring, only show
                // the new one
                this.done = true;
                return of(newTask);
            }
            Log.wtf(TaskBase.TAG, "Recurring vanish");
        } else if (newDone) {
            this.setProgress(100);
        } else {
            this.setProgress(0);
        }
        return absent();
    }

    public void setDue(final @NonNull Optional<Calendar> newDue) {
        if (DateTimeHelper.equalsCalendar(this.due, newDue)) {
            return;
        }
        this.due = newDue;
        this.edited.put(TaskBase.DUE, true);
        if (!newDue.isPresent()) {
            setRecurrence(-1);
        }
    }

    /**
     * Replaces the id of the current task by the foreign task. This is needed
     * if we want to override the current task by a remote task.
     *
     * @param t other task
     */
    public void takeIdFrom(@NonNull final Task t) {
        this.setId(t.getId());
    }

    public void setList(@NonNull final ListMirakel newList) {
        setList(newList, false);
    }

    public void setList(@NonNull final ListMirakel newList,
                        final boolean removeNoListFlag) {
        if (this.list != null && this.list.getId() == newList.getId()) {
            return;
        }
        if (newList.isSpecial()) {
            this.list = ((SpecialList) newList).getDefaultList();
        } else {
            this.list = newList;
        }
        this.edited.put(TaskBase.LIST_ID, true);
        if (removeNoListFlag) {
            if (this.additionalEntries == null) {
                initAdditionalEntries();
            }
            this.additionalEntries.remove(DefinitionsHelper.TW_NO_PROJECT);
        }
    }

    @Override
    public void setName(@NonNull final String newName) {
        if (getName().equals(newName)) {
            return;
        }
        super.setName(newName);
        this.edited.put(ModelBase.NAME, true);
    }

    public void setPriority(final int priority) {
        if (this.priority == priority) {
            return;
        }
        if (priority > 2 || priority < -2) {
            throw new IllegalArgumentException(
                "Priority is not in Range [-2,2]");
        }
        this.priority = priority;
        this.edited.put(TaskBase.PRIORITY, true);
    }

    public void setProgress(final int newProgress) {
        if (this.progress == newProgress) {
            return;
        }
        this.edited.put("progress", true);
        this.progress = newProgress;
    }

    public void setRecurrence(final long newRecurrence) {
        if (this.recurrence == newRecurrence) {
            return;
        }
        this.recurrence = newRecurrence;
        this.edited.put(TaskBase.RECURRING, true);
    }

    public void setRecurringReminder(final long newRecurrence) {
        if (this.recurringReminder == newRecurrence) {
            return;
        }
        this.recurringReminder = newRecurrence;
        this.edited.put(TaskBase.RECURRING_REMINDER, true);
    }

    public void setReminder(final @NonNull Optional<Calendar> newReminder) {
        setReminder(newReminder, false);
    }

    public void setReminder(final @NonNull Optional<Calendar> newReminder, final boolean force) {
        if (this.reminder.or(new GregorianCalendar()).equals(newReminder.or(new GregorianCalendar()))
            && !force) {
            return;
        }
        this.reminder = newReminder;
        this.edited.put(TaskBase.REMINDER, true);
        if (!newReminder.isPresent()) {
            setRecurringReminder(-1);
        }
    }

    public void setSyncState(@NonNull final SYNC_STATE sync_state) {
        this.syncState = sync_state;
    }

    public void setUpdatedAt(@NonNull final Calendar updated_at) {
        this.updatedAt = updated_at;
    }

    public void setUpdatedAt(@NonNull final String updated_at) {
        try {
            setUpdatedAt(DateTimeHelper.parseDateTime(updated_at));
        } catch (final ParseException e) {
            setUpdatedAt((Calendar) null);
        }
    }

    public void setUUID(@NonNull final String newUuid) {
        this.uuid = newUuid;
    }

    @NonNull
    public Optional<Task> toggleDone() {
        return setDone(!this.done);
    }

    @NonNull
    public List<Tag> getTags() {
        checkTags();
        return this.tags.get();
    }

    private void checkTags() {
        if (!this.tags.isPresent()) {
            if (this.getId() == 0) {
                this.tags = of((List<Tag>) new ArrayList<Tag>());
            } else {
                this.tags = of(Tag.getTagsForTask(this.getId()));
            }
        }
    }

    protected void addTag(@NonNull final Tag tag) {
        checkTags();
        this.tags.get().add(tag);
    }

    protected void removeTag(@NonNull final Tag tag) {
        checkTags();
        this.tags.get().remove(tag);
    }

    public boolean isStub() {
        return isStub;
    }

    public void setStub(boolean isStub) {
        this.isStub = isStub;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                 * result
                 + (this.additionalEntries == null ? 0 : this.additionalEntries
                    .hashCode());
        result = prime * result + (this.additionalEntriesString.hashCode());
        result = prime * result + this.content.hashCode();
        result = prime * result + (this.createdAt.hashCode());
        result = prime * result + (this.done ? 1231 : 1237);
        result = prime * result + (this.due.isPresent() ? this.due.get().hashCode() : 0);
        result = prime * result + (this.edited.hashCode());
        result = prime * result + (int) (this.getId() ^ this.getId() >>> 32);
        result = prime * result + (this.isRecurringShown ? 1231 : 1237);
        result = prime * result + (this.list.hashCode());
        result = prime * result + (getName().hashCode());
        result = prime * result + this.priority;
        result = prime * result + this.progress;
        result = prime * result + (int) this.recurrence;
        result = prime * result + (int) this.recurringReminder;
        result = prime * result
                 + (this.reminder.isPresent() ? this.reminder.get().hashCode() : 0);
        result = prime * result + (this.syncState.hashCode());
        result = prime * result
                 + (!this.tags.isPresent() ? 0 : this.tags.get().hashCode());
        result = prime * result + (this.updatedAt.hashCode());
        result = prime * result + (this.uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (((Object) this).getClass() != obj.getClass()) {
            return false;
        }
        final TaskBase other = (TaskBase) obj;
        if (this.additionalEntries == null) {
            if (other.additionalEntries != null) {
                return false;
            }
        } else if (!this.additionalEntries.equals(other.additionalEntries)) {
            return false;
        }
        if (!this.additionalEntriesString
            .equals(other.additionalEntriesString)) {
            return false;
        }
        if (!this.content.equals(other.content)) {
            return false;
        }
        // We should ignore the created_at date
        if (this.done != other.done) {
            return false;
        }
        if (!DateTimeHelper.equalsCalendar(this.due, other.due)) {
            return false;
        }
        if (this.getId() != other.getId()) {
            return false;
        }
        if (this.isRecurringShown != other.isRecurringShown) {
            return false;
        }
        if (!this.list.equals(other.list)) {
            return false;
        }
        if (!getName().equals(other.getName())) {
            return false;
        }
        if (this.priority != other.priority) {
            return false;
        }
        if (this.progress != other.progress) {
            return false;
        }
        if (this.getRecurring() == null) {
            if (other.getRecurring() != null) {
                return false;
            }
        } else if (!this.getRecurring().equals(other.getRecurring())) {
            return false;
        }
        if (this.getRecurringReminder() == null) {
            if (other.getRecurringReminder() != null) {
                return false;
            }
        } else if (!this.getRecurringReminder().equals(other.getRecurringReminder())) {
            return false;
        }
        if (!DateTimeHelper.equalsCalendar(this.reminder, other.reminder)) {
            return false;
        }
        if (this.syncState != other.syncState) {
            return false;
        }
        if (!this.getTags().equals(other.getTags())) {
            return false;
        }
        // Do not compare updatedAt because it is updated
        if (!this.uuid.equals(other.uuid)) {
            return false;
        }
        return true;
    }

}
