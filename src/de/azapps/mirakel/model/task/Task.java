/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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
package de.azapps.mirakel.model.task;

import java.util.GregorianCalendar;

import android.content.Context;
import de.azapps.mirakel.model.list.ListMirakel;


public class Task extends TaskBase {

	public Task(long id, ListMirakel list, String name, String content,
			boolean done, GregorianCalendar due, int priority,
			String created_at, String updated_at, int sync_state) {
		super(id, list, name, content, done, due, priority, created_at, updated_at, sync_state);
	}


	public Task() {
		super();
	}

	public Task(Context ctx) {
		super(ctx);
	}
	public void save() {
		
	}
}
