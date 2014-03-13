package de.azapps.mirakel.model.file;

import java.io.File;

import android.content.ContentValues;
import de.azapps.mirakel.model.task.Task;

public class FileBase {
	private int id;
	private Task task;
	private String name;
	private String path;
	private File file;

	public FileBase(final int id, final Task task, final String name,
			final String path) {
		super();
		this.id = id;
		this.task = task;
		this.name = name;
		this.path = path;
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

	public String getPath() {
		return this.path;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	public File getFile() {
		if (this.file == null) {
			this.file = new File(this.path);
		}
		return this.file;
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
		cv.put("path", this.path);
		return cv;
	}
}
