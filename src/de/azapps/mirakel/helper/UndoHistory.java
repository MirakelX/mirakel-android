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
package de.azapps.mirakel.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.model.MirakelContentProvider;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class UndoHistory {
	private static final short LIST = 1;
	private static String TAG = "UndoHistory";
	private static final short TASK = 0;
	public static String UNDO = "OLD";

	public static void logCreate(final ListMirakel newList, final Context ctx) {
		updateLog(LIST, newList.getId() + "", ctx);
	}

	public static void logCreate(final Task newTask, final Context ctx) {
		updateLog(TASK, newTask.getId() + "", ctx);
	}

	public static void undoLast() {
		final String last = MirakelCommonPreferences.getFromLog(0);
		if (last != null && !last.equals("")) {
			final short type = Short.parseShort(last.charAt(0) + "");
			if (last.charAt(1) != '{') {
				try {
					final long id = Long.parseLong(last.substring(1));
					switch (type) {
					case TASK:
						Task.get(id).destroy(true);
						break;
					case LIST:
						ListMirakel.get((int) id).destroy(true);
						break;
					default:
						Log.wtf(TAG, "unkown Type");
						break;
					}
				} catch (final Exception e) {
					Log.e(TAG, "cannot parse String");
				}

			} else {
				final JsonObject json = new JsonParser().parse(
						last.substring(1)).getAsJsonObject();
				switch (type) {
				case TASK:
					final Task t = Task.parse_json(json,
							AccountMirakel.getLocal(), false);
					if (Task.get(t.getId()) != null) {
						t.safeSave(false);
					} else {
						try {
							MirakelContentProvider.getWritableDatabase()
									.insert(Task.TABLE, null,
											t.getContentValues());
						} catch (final Exception e) {
							Log.e(TAG, "cannot restore Task");
						}
					}
					break;
				case LIST:
					final ListMirakel l = ListMirakel.parseJson(json);
					if (ListMirakel.get(l.getId()) != null) {
						l.save(false);
					} else {
						try {
							MirakelContentProvider.getWritableDatabase()
									.insert(ListMirakel.TABLE, null,
											l.getContentValues());
						} catch (final Exception e) {
							Log.e(TAG, "cannot restore List");
						}
					}
					break;
				default:
					Log.wtf(TAG, "unkown Type");
					break;
				}
			}
		}
		final SharedPreferences.Editor editor = MirakelPreferences.getEditor();
		for (int i = 0; i < MirakelCommonPreferences.getUndoNumber(); i++) {
			final String old = MirakelCommonPreferences.getFromLog(i + 1);
			editor.putString(UNDO + i, old);
		}
		editor.putString(UNDO + 10, "");
		editor.commit();
	}

	public static void updateLog(final ListMirakel listMirakel,
			final Context ctx) {
		if (listMirakel != null) {
			updateLog(LIST, listMirakel.toJson(), ctx);
		}

	}

	private static void updateLog(final short type, final String json,
			final Context ctx) {
		if (ctx == null) {
			Log.e(TAG, "context is null");
			return;
		}
		// Log.d(TAG, json);
		final SharedPreferences.Editor editor = MirakelPreferences.getEditor();
		for (int i = MirakelCommonPreferences.getUndoNumber(); i > 0; i--) {
			final String old = MirakelCommonPreferences.getFromLog(i - 1);
			editor.putString(UNDO + i, old);
		}
		editor.putString(UNDO + 0, type + json);
		editor.commit();
	}

	public static void updateLog(final Task task, final Context ctx) {
		if (task != null) {
			updateLog(TASK, task.toJson(), ctx);
		}

	}

}
