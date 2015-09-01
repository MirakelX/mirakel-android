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

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import org.joda.time.DateTime;

import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.generic.IGenericElementInterface;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

/**
 * A subset of the Task class which implements only functions you need in the TasksFragment
 */
public class TaskOverview extends ModelBase implements IGenericElementInterface,
    android.os.Parcelable {
    @NonNull
    private Optional<Task> taskOptional = absent();
    private boolean done;
    private int priority;
    private int progress;
    @NonNull
    protected Optional<DateTime> due = absent();
    private long listId;
    @NonNull
    private Optional<ListMirakel> listMirakelOptional = absent();
    private String listName;
    private long accountId;
    @NonNull
    private Optional<AccountMirakel> accountMirakelOptional = absent();

    public TaskOverview(final Task task) {
        taskOptional = of(task);
        done = task.isDone();
        progress = task.getProgress();
        listMirakelOptional = of(task.getList());
        listId = listMirakelOptional.get().getId();
        listName = listMirakelOptional.get().getName();
        priority = task.getPriority();
    }

    public TaskOverview(final CursorGetter cursor) {
        setId(cursor.getLong(ModelBase.ID));
        setName(cursor.getString(ModelBase.NAME));
        done = cursor.getBoolean(Task.DONE);
        progress = cursor.getInt(Task.PROGRESS);
        due = cursor.getOptional(Task.DUE, DateTime.class);
        listId = cursor.getLong(Task.LIST_ID);
        listName = cursor.getString("list_name");
        accountId = cursor.getLong("account_id");
        priority = cursor.getInt(Task.PRIORITY);
    }

    public boolean isDone() {
        return done;
    }

    public int getProgress() {
        return progress;
    }

    @NonNull
    public Optional<DateTime> getDue() {
        return due;
    }

    public long getListId() {
        return listId;
    }

    public String getListName() {
        return listName;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    protected Uri getUri() {
        return Task.URI;
    }

    public void withTask(OptionalUtils.Procedure<Task> taskProcedure) {
        Optional<Task> taskOptional = getTask();
        OptionalUtils.withOptional(taskOptional, taskProcedure);
    }

    @NonNull
    public Optional<Task> getTask() {
        if (!taskOptional.isPresent()) {
            taskOptional = Task.get(getId());
        }
        return taskOptional;
    }

    @NonNull
    public Optional<ListMirakel> getList() {
        if (!listMirakelOptional.isPresent()) {
            listMirakelOptional = ListMirakel.get(listId);
        }
        return listMirakelOptional;
    }
    @NonNull
    public Optional<AccountMirakel> getAccountMirakel() {
        if (!accountMirakelOptional.isPresent()) {
            accountMirakelOptional = AccountMirakel.get(accountId);
        }
        return accountMirakelOptional;
    }

    // Parcelable stuff

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(done ? (byte) 1 : (byte) 0);
        dest.writeInt(this.priority);
        dest.writeInt(this.progress);
        dest.writeSerializable(this.due);
        dest.writeLong(this.listId);
        dest.writeString(this.listName);
        dest.writeLong(this.accountId);
        dest.writeLong(this.getId());
        dest.writeString(this.getName());
    }

    private TaskOverview(Parcel in) {
        this.done = in.readByte() != 0;
        this.priority = in.readInt();
        this.progress = in.readInt();
        this.due = (Optional<DateTime>) in.readSerializable();
        this.listId = in.readLong();
        this.listName = in.readString();
        this.accountId = in.readLong();
        setId(in.readLong());
        setName(in.readString());
    }

    public static final Creator<TaskOverview> CREATOR = new Creator<TaskOverview>() {
        public TaskOverview createFromParcel(Parcel source) {
            return new TaskOverview(source);
        }

        public TaskOverview[] newArray(int size) {
            return new TaskOverview[size];
        }
    };
}
