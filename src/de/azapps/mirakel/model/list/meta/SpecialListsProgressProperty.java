/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.model.list.meta;

import android.content.Context;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsProgressProperty extends SpecialListsBaseProperty {

	public enum OPERATION {
		GREATER_THAN, EQUAL, LESS_THAN;
	}

	int value;
	OPERATION op;

	public SpecialListsProgressProperty(int value, OPERATION op) {
		this.value = value;
		this.op = op;
	}

	@Override
	public String getWhereQuery() {
		String ret = Task.PROGRESS;
		switch (op) {
		case EQUAL:
			ret += " = ";
			break;
		case GREATER_THAN:
			ret += " >= ";
			break;
		case LESS_THAN:
			ret += " <= ";
			break;
		}
		return ret + value;
	}

	@Override
	public String serialize() {
		String ret = "\"" + Task.PROGRESS + "\":{";
		ret += "\"value\":" + value;
		ret += ",\"op\":" + op.ordinal();
		return ret + "}";
	}

	@Override
	public String getSummary(Context ctx) {
		switch (op) {
		case EQUAL:
			return ctx.getString(R.string.special_list_progress_equal, value);
		case GREATER_THAN:
			return ctx.getString(R.string.special_list_progress_greater_than,
					value);
		case LESS_THAN:
			return ctx.getString(R.string.special_list_progress_less_than,
					value);
		}
		return "";
	}

	public OPERATION getOperation() {
		return op;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public void setOperation(OPERATION op) {
		this.op = op;
	}

}
