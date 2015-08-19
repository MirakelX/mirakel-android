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

package de.azapps.mirakel.model.file;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.model.task.TaskVanishedException;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

abstract class FileBase extends ModelBase {

    public static final String TASK = "task_id";
    public static final String PATH = "path";

    private static final String TAG = "FileBase";
    @NonNull
    protected Task task;
    @NonNull
    protected Uri fileUri;

    public FileBase(final long id, final String name, final Task task,
                    @NonNull final Uri uri) throws TaskVanishedException {
        super(id, name);
        if (task == null) {
            throw new TaskVanishedException("While creating file");
        }
        this.task = task;
        this.fileUri = uri;
    }

    protected FileBase() {
        // Do nothing
    }


    public @NonNull Task getTask() {
        return this.task;
    }

    public void setTask(@NonNull final Task task) {
        this.task = task;
    }

    public @NonNull Uri getFileUri() {
        return this.fileUri;
    }

    public void setFileUri(@NonNull final Uri path) {
        this.fileUri = path;
    }

    public FileInputStream getFileStream(final Context ctx)
    throws FileNotFoundException {
        return FileUtils.getStreamFromUri(ctx, this.fileUri);
    }


    @NonNull
    public ContentValues getContentValues() {
        final ContentValues cv;
        try {
            cv = super.getContentValues();
        } catch (DefinitionsHelper.NoSuchListException e) {
            Log.wtf(TAG, "How can this happen?", e);
            return new ContentValues();
        }
        cv.put("task_id", this.task.getId());
        cv.put("path", this.fileUri.toString());
        return cv;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)this.getId();
        result = prime * result + (this.getName().hashCode());
        result = prime * result + (this.task.hashCode());
        result = prime * result + (this.fileUri.hashCode());
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
        if (!(obj instanceof FileBase)) {
            return false;
        }
        final FileBase other = (FileBase) obj;
        if (this.getId() != other.getId()) {
            return false;
        }
        if (!this.getName().equals(other.getName())) {
            return false;
        }
        if (!this.task.equals(other.task)) {
            return false;
        }
        if (!this.fileUri.equals(other.fileUri)) {
            return false;
        }
        return true;
    }

}
