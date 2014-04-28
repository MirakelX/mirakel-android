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

	public void setId(final int id) {
		this.id = id;
	}

	public Task getTask() {
		return this.task;
	}

	public void setTask_id(final Task task) {
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
}
