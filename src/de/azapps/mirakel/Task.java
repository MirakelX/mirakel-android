package de.azapps.mirakel;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;

public class Task {
	private long id;
	private long list_id;
	private String name;
	private String content;
	private boolean done;
	private GregorianCalendar due;
	private int priority;
	private String created_at;
	private String updated_at;
	private Map<String, Boolean> edited = new HashMap<String, Boolean>();
	private int sync_state;

	public Task(long id, long list_id, String name, String content,
			boolean done, GregorianCalendar due, int priority, String created_at,
			String updated_at, int sync_state){
		this.id=id;
		this.setListId(list_id);
		this.setName(name);
		this.setContent(content);
		this.setDone(done);
		this.setDue(due);
		this.setPriority(priority);
		this.setCreated_at(created_at);
		this.setUpdated_at(updated_at);
		this.setSync_state(sync_state);
	}

	public Task(){}
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
		edited.put("id", true);
	}

	public long getListId() {
		return list_id;
	}

	public void setListId(long list_id) {
		this.list_id = list_id;
		edited.put("list_id", true);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		edited.put("name", true);
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
		edited.put("content", true);
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
		edited.put("done", true);
	}

	public void toggleDone() {
		this.done = !this.done;
		edited.put("done", true);
	}

	public GregorianCalendar getDue() {
		return due;
	}

	public void setDue(GregorianCalendar due) {
		this.due = due;
		edited.put("due", true);
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
		edited.put("priority", true);
	}

	public String getCreated_at() {
		return created_at;
	}

	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}

	public String getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(String updated_at) {
		this.updated_at = updated_at;
	}
	
	public int getSync_state() {
		return sync_state;
	}

	public void setSync_state(int sync_state) {
		this.sync_state = sync_state;
	}
	
	@Override
	public String toString() {
		return name;
	}

	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put("_id", id);
		cv.put("list_id", list_id);
		cv.put("name", name);
		cv.put("content", content);
		cv.put("done", done);
		cv.put("due", due.get(Calendar.YEAR)+"-"+(due.get(Calendar.MONTH)+1)+"-"+due.get(Calendar.DAY_OF_MONTH));   
		cv.put("priority", priority);
		cv.put("created_at", created_at);
		cv.put("updated_at", updated_at);
		cv.put("sync_state", sync_state);
		return cv;
	}

}
