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

	public FileBase(int id, Task task, String name, String path) {
		super();
		this.id = id;
		this.task = task;
		this.name = name;
		this.path = path;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Task getTask() {
		return task;
	}

	public void setTask_id(Task task) {
		this.task = task;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public File getFile() {
		if (file == null)
			file = new File(path);
		return file;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put("_id", id);
		cv.put("task_id", task.getId());
		cv.put("name", name);
		cv.put("path", path);
		return cv;
	}
}
