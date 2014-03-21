package de.azapps.mirakel.model.account;

import android.content.ContentValues;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;

public class AccountBase {
	public final static String TYPE = "type";
	public final static String ENABLED = "enabled";

	private int _id;
	private String name;
	private int type;
	private boolean enabled;

	public AccountBase(final int id, final String name,
			final ACCOUNT_TYPES type, final boolean enabled) {
		this.setId(id);
		this.setName(name);
		this.setType(type.toInt());
		this.setEnabeld(enabled);

	}

	public int getId() {
		return this._id;
	}

	public void setId(final int _id) {
		this._id = _id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ACCOUNT_TYPES getType() {
		return ACCOUNT_TYPES.parseInt(this.type);
	}

	public void setType(final int type) {
		this.type = type;
	}

	public ContentValues getContentValues() {
		final ContentValues cv = new ContentValues();
		cv.put(DatabaseHelper.ID, this._id);
		cv.put(DatabaseHelper.NAME, this.name);
		cv.put(TYPE, this.type);
		cv.put(ENABLED, this.enabled);
		return cv;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public boolean isEnabeld() {
		return this.enabled;
	}

	public void setEnabeld(final boolean enabeld) {
		this.enabled = enabeld;
	}

}
