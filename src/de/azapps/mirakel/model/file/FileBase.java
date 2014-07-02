package de.azapps.mirakel.model.file;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;

public class FileBase {
    private int id;
    private Task task;
    private String name;
    private Uri uri;

    public FileBase(final int id, final Task task, final String name,
                    final Uri uri) {
        super();
        this.id = id;
        this.task = task;
        this.name = name;
        this.uri = uri;
    }

    public int getId() {
        return this.id;
    }

    protected void setId(final int id) {
        this.id = id;
    }

    public Task getTask() {
        return this.task;
    }

    public void setTask(final Task task) {
        this.task = task;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Uri getUri() {
        return this.uri;
    }

    public void setUri(final Uri path) {
        this.uri = path;
    }

    public FileInputStream getFileStream(final Context ctx)
    throws FileNotFoundException {
        return FileUtils.getStreamFromUri(ctx, this.uri);
    }

    @Override
    public String toString() {
        return this.name;
    }

    public ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();
        cv.put("_id", this.id);
        cv.put("task_id", this.task.getId());
        cv.put("name", this.name);
        cv.put("path", this.uri != null ? this.uri.toString() : "");
        return cv;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id;
        result = prime * result
                 + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result
                 + (this.task == null ? 0 : this.task.hashCode());
        result = prime * result + (this.uri == null ? 0 : this.uri.hashCode());
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
        if (this.id != other.id) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.task == null) {
            if (other.task != null) {
                return false;
            }
        } else if (!this.task.equals(other.task)) {
            return false;
        }
        if (this.uri == null) {
            if (other.uri != null) {
                return false;
            }
        } else if (!this.uri.equals(other.uri)) {
            return false;
        }
        return true;
    }

}
