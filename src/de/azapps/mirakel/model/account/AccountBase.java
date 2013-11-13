package de.azapps.mirakel.model.account;

import android.content.ContentValues;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_TYPES;

public class AccountBase {
	public final static String TYPE="type";
	public final static String ENABLED="enabled";
	
	private int _id;
	private String name;
	private int type;
	private boolean enabled;
	
	public AccountBase(int id, String name, SYNC_TYPES type, boolean enabled){
		this.setId(id);
		this.setName(name);
		this.setType(type.toInt());
		this.setEnabeld(enabled);
		
	}

	public int getId() {
		return _id;
	}

	public void setId(int _id) {
		this._id = _id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	public SYNC_TYPES getType() {
		return SYNC_TYPES.parseInt(type);
	}

	public void setType(int type) {
		this.type = type;
	}
	
	public ContentValues getContentValues(){
		ContentValues cv= new ContentValues();
		cv.put(DatabaseHelper.ID, _id);
		cv.put(DatabaseHelper.NAME, name);
		cv.put(TYPE, type);
		cv.put(ENABLED, enabled);
		return cv;
	}
	
	@Override
	public String toString() {
		return name;
	}

	public boolean isEnabeld() {
		return enabled;
	}

	public void setEnabeld(boolean enabeld) {
		this.enabled = enabeld;
	}

}
