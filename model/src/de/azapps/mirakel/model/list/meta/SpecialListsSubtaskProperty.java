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
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsSubtaskProperty extends SpecialListsBaseProperty {
	boolean isParent;
	boolean isNegated;

	public SpecialListsSubtaskProperty(boolean isNegated, boolean isParent) {
		this.isParent = isParent;
		this.isNegated = isNegated;
	}

	@Override
	public String getSummary(Context ctx) {
		if (isParent && isNegated) {
			return ctx.getString(R.string.special_list_subtask_parent_not);
		} else if (isParent && !isNegated) {
			return ctx.getString(R.string.special_list_subtask_parent);
		} else if (!isParent && isNegated) {
			return ctx.getString(R.string.special_list_subtask_child_not);
		} else {
			return ctx.getString(R.string.special_list_subtask_child);
		}
	}

	@Override
	public String getWhereQuery() {
		return (isNegated ? "NOT " : "") + DatabaseHelper.ID
				+ " IN ( SELECT DISTINCT "
				+ (isParent ? "parent_id" : "child_id") + " FROM "
				+ Task.SUBTASK_TABLE + ")";
	}

	@Override
	public String serialize() {
		String ret = "\"" + Task.SUBTASK_TABLE + "\":{";
		ret += "\"isNegated\":" + isNegated;
		return ret + ",\"isParent\":" + isParent + "}";
	}

	public boolean isNegated() {
		return isNegated;
	}

	public boolean isParent() {
		return isParent;
	}

	public void setParent(boolean parent) {
		isParent = parent;
	}

	public void setNegated(boolean negated) {
		isNegated = negated;
	}
}
