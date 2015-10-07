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

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
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
    protected DateTime createdAt = new DateTime();
    @NonNull
    protected DateTime updatedAt = new DateTime();
    protected boolean done;
    @NonNull
    protected Optional<DateTime> due = absent();
    @NonNull
    protected final Map<String, Boolean> edited = new HashMap<>();
    protected ListMirakel list;
    protected int priority;
    protected int progress;
    protected long recurrence;
    protected long recurringReminder;
    protected boolean isRecurringShown;
    @NonNull
    protected Optional<DateTime> reminder = absent();
    @NonNull
    protected SYNC_STATE syncState = SYNC_STATE.NOTHING;
    @NonNull
    protected String uuid = "";
    @NonNull
    protected Optional<List<Tag>> tags = absent();
    protected boolean isStub;

    TaskBase() {
        // nothing
        super(INVALID_ID, "");
    }

    TaskBase(final long newId, @NonNull final String newUuid, @NonNull final ListMirakel newList,
             @NonNull final String newName, @NonNull final String newContent,
             final boolean newDone, final @NonNull Optional<DateTime> newDue,
             final @NonNull Optional<DateTime> newReminder, final int newPriority,
             @NonNull final DateTime newCreatedAt, @NonNull final DateTime newUpdatedAt,
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
        this.createdAt = newCreatedAt;
        this.updatedAt = newUpdatedAt;
        syncState = newSyncState;
        this.additionalEntriesString = newAdditionalEntriesString;
        this.recurrence = recurring;
        this.recurringReminder = newRecurringReminder;
        this.progress = newProgress;
        clearEdited();
        this.tags = absent();
        isRecurringShown = shown;
    }


    TaskBase(@NonNull final String newName, @NonNull final ListMirakel list) {
        super(INVALID_ID, newName);
        this.uuid = java.util.UUID.randomUUID().toString();
        setList(list, false);
        setContent("");
        setDone(false);
        setDue(Optional.<DateTime>absent());
        setReminder(Optional.<DateTime>absent());
        this.priority = 0;
        this.createdAt = new DateTime();
        this.updatedAt = createdAt;
        syncState = SYNC_STATE.NOTHING;
        this.recurrence = -1L;
        this.recurringReminder = -1L;
        this.progress = 0;
        isRecurringShown = true;
        clearEdited();
    }

    public void addAdditionalEntry(@NonNull final String key, final String value) {
        initAdditionalEntries();
        Log.d(TaskBase.TAG, "add: " + key + ':' + value);
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


    @NonNull
    public static String serializeAdditionalEntries(
        @NonNull final Map<String, String> additionalEntries) {
        final StringBuilder additionalEntriesStr = new StringBuilder("{");
        boolean first = true;
        for (final Entry<String, String> p : additionalEntries.entrySet()) {
            additionalEntriesStr.append(first ? "" : ",").append('"').append(p.getKey()).append("\":").append(
                p.getValue());
            first = false;
        }
        return additionalEntriesStr.toString() + '}';
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
            cv.put(DUE, due.get().getMillis());
        } else {
            cv.put(TaskBase.DUE, (Long) null);
        }
        if (this.reminder.isPresent()) {
            cv.put(TaskBase.REMINDER, reminder.get().getMillis());
        } else {
            cv.put(REMINDER, (Long)null);
        }
        cv.put(TaskBase.PRIORITY, this.priority);
        cv.put(DatabaseHelper.CREATED_AT,
               this.createdAt.getMillis());
        cv.put(DatabaseHelper.UPDATED_AT,
               this.updatedAt.getMillis());
        cv.put(DatabaseHelper.SYNC_STATE_FIELD, this.syncState.toInt());
        cv.put(TaskBase.RECURRING, this.recurrence);
        cv.put(TaskBase.RECURRING_REMINDER, this.recurringReminder);
        cv.put(TaskBase.PROGRESS, this.progress);
        cv.put(TaskBase.RECURRING_SHOWN, this.isRecurringShown);
        cv.put("additional_entries", getAdditionalEntriesString());
        return cv;
    }

    @NonNull
    public DateTime getCreatedAt() {
        return this.createdAt;
    }

    @NonNull
    public Optional<DateTime> getDue() {
        return this.due;
    }

    @NonNull
    public ListMirakel getList() {
        if (this.list == null) {
            throw new RuntimeException("The task is not properly initialized. List is null!");
        }
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

    @NonNull
    public Optional<Recurring> getRecurrence() {
        return Recurring.get(this.recurrence);
    }

    @NonNull
    public Optional<Recurring> getRecurringReminder() {
        return Recurring.get(this.recurringReminder);
    }

    public long getRecurringReminderId() {
        return this.recurringReminder;
    }

    @NonNull
    public Optional<DateTime> getReminder() {
        return this.reminder;
    }

    @NonNull
    public SYNC_STATE getSyncState() {
        return this.syncState;
    }

    @NonNull
    public DateTime getUpdatedAt() {
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
        if ((additionalEntriesString != null)
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
                } else if ((keyValue.length == 1) && (key != null)) {
                    ret.put(key, ret.get(key) + ',' + keyValue[0]);
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


    /**
     * @param newDone is the task marked as done?
     * @return The new task if one was created
     */
    @NonNull
    public Optional<Task> setDone(final boolean newDone) {
        if (this.done == newDone) {
            return absent();
        }
        this.done = newDone;
        this.edited.put(TaskBase.DONE, true);
        if (newDone && (this.recurrence != -1) && this.due.isPresent()) {
            if (getRecurrence().isPresent()) {
                final Optional<Task> oldTask = Task.get(getId());
                if (!oldTask.isPresent()) {
                    return absent();
                }
                final Task newTask = getRecurrence().get().incrementRecurringDue(
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

    public void setDue(final @NonNull Optional<DateTime> newDue) {
        if (DateTimeHelper.equalsCalendar(this.due, newDue)) {
            return;
        }
        this.due = newDue;
        this.edited.put(TaskBase.DUE, true);
        if (!newDue.isPresent()) {
            setRecurrence(-1L);
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
        if ((this.list != null) && (this.list.getId() == newList.getId())) {
            return;
        }
        if (newList.isSpecial()) {
            Optional<SpecialList> specialListOptional = newList.toSpecial();
            if (specialListOptional.isPresent()) {
                this.list = specialListOptional.get().getDefaultList();
            } else {
                ErrorReporter.report(ErrorType.LIST_VANISHED);
                return;
            }
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
        // This can not happen (it's final!! – but we are in a constructor), but hey, that's java
        if (edited != null) {
            this.edited.put(ModelBase.NAME, true);
        }
    }

    public void setPriority(final int priority) {
        if (this.priority == priority) {
            return;
        }
        if ((priority > 2) || (priority < -2)) {
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

    protected void setRecurrence(final long newRecurrence) {
        if (this.recurrence == newRecurrence) {
            return;
        }
        this.recurrence = newRecurrence;
        this.edited.put(TaskBase.RECURRING, true);
    }

    public void setRecurrence(final @NonNull Optional<Recurring> newRecurrence) {
        if (newRecurrence.isPresent()) {
            setRecurrence(newRecurrence.get().getId());
        } else {
            setRecurrence(-1L);
        }
    }

    public void setRecurringReminder(final long newRecurrence) {
        if (this.recurringReminder == newRecurrence) {
            return;
        }
        this.recurringReminder = newRecurrence;
        this.edited.put(TaskBase.RECURRING_REMINDER, true);
    }

    public void setRecurringReminder(final @NonNull Optional<Recurring> newRecurrence) {
        if (newRecurrence.isPresent()) {
            setRecurringReminder(newRecurrence.get().getId());
        } else {
            setRecurringReminder(-1L);
        }
    }

    public void setReminder(final @NonNull Optional<DateTime> newReminder) {
        setReminder(newReminder, false);
    }

    public void setReminder(final @NonNull Optional<DateTime> newReminder, final boolean force) {
        if (this.reminder.or(new DateTime()).equals(newReminder.or(new DateTime()))
            && !force) {
            return;
        }
        this.reminder = newReminder;
        this.edited.put(TaskBase.REMINDER, true);
        if (!newReminder.isPresent()) {
            setRecurringReminder(-1L);
        }
    }

    public void setSyncState(@NonNull final SYNC_STATE sync_state) {
        this.syncState = sync_state;
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
                this.tags = of((List<Tag>) new ArrayList<Tag>(0));
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

    @Override
    public boolean isStub() {
        return isStub || super.isStub();
    }

    public void setIsStub(boolean stub) {
        this.isStub = stub;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                 * result
                 + ((this.additionalEntries == null) ? 0 : this.additionalEntries
                    .hashCode());
        result = prime * result + (this.additionalEntriesString.hashCode());
        result = prime * result + this.content.hashCode();
        result = prime * result + (this.createdAt.hashCode());
        result = prime * result + (this.done ? 1231 : 1237);
        result = prime * result + (this.due.isPresent() ? this.due.get().hashCode() : 0);
        result = prime * result + (this.edited.hashCode());
        result = prime * result + (int) (this.getId() ^ (this.getId() >>> 32));
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
        result = prime * result + (this.isStub ? 1249 : 1259);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof TaskBase)) {
            return false;
        }
        final TaskBase other = (TaskBase) o;
        if (this.additionalEntries == null) {
            setAdditionalEntries(additionalEntriesString);
        }
        if (other.additionalEntries == null) {
            other.setAdditionalEntries(other.additionalEntriesString);
        }
        if (!Objects.equal(this.additionalEntries, other.additionalEntries)) {
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
        if (!Objects.equal(list, other.list)) {
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
        if (getRecurrence().isPresent() && other.getRecurrence().isPresent()) {
            if (!this.getRecurrence().get().equals(other.getRecurrence().get())) {
                return false;
            }
        } else if (getRecurrence().isPresent() != other.getRecurrence().isPresent()) {
            return false;
        }

        if (getRecurringReminder().isPresent() && other.getRecurringReminder().isPresent()) {
            if (!this.getRecurringReminder().get().equals(other.getRecurringReminder().get())) {
                return false;
            }
        } else if (getRecurringReminder().isPresent() != other.getRecurringReminder().isPresent()) {
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

    public void setUpdatedAt(@NonNull DateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDue(final @Nullable DateTime dateTime) {
        setDue(fromNullable(dateTime));
    }

    @VisibleForTesting
    public void setCreatedAt(final @NonNull DateTime created) {
        createdAt = created;
    }
}
