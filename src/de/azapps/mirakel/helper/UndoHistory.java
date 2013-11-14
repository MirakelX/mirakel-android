package de.azapps.mirakel.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;

public class UndoHistory {
	private static final short TASK = 0;
	private static final short LIST = 1;
	private static String TAG = "UndoHistory";
	public static String UNDO = "OLD";


	public static void updateLog(ListMirakel listMirakel, Context ctx) {
		if (listMirakel != null)
			updateLog(LIST, listMirakel.toJson(), ctx);

	}

	private static void updateLog(short type, String json, Context ctx) {
		if (ctx == null) {
			Log.e(TAG, "context is null");
			return;
		}
		// Log.d(TAG, json);
		SharedPreferences.Editor editor = Helpers.settings.edit();
		for (int i = Helpers.settings.getInt("UndoNumber", 10); i > 0; i--) {
			String old = Helpers.settings.getString(UNDO + (i - 1), "");
			editor.putString(UNDO + i, old);
		}
		editor.putString(UNDO + 0, type + json);
		editor.commit();
	}

	public static void updateLog(Task task, Context ctx) {
		if (task != null)
			updateLog(TASK, task.toJson(), ctx);

	}

	public static void undoLast(Context ctx) {
		String last = Helpers.settings.getString(UNDO + 0, "");
		if (last != null && !last.equals("")) {
			short type = Short.parseShort(last.charAt(0) + "");
			if (last.charAt(1) != '{') {
				try {
					Long id = Long.parseLong(last.substring(1));
					switch (type) {
					case TASK:
						Task.get(id).destroy(true);
						break;
					case LIST:
						ListMirakel.getList(id.intValue()).destroy(true);
						break;
					default:
						Log.wtf(TAG, "unkown Type");
						break;
					}
				} catch (Exception e) {
					Log.e(TAG, "cannot parse String");
				}

			} else {
				JsonObject json = new JsonParser().parse(last.substring(1))
						.getAsJsonObject();
				switch (type) {
				case TASK:
					try {
						Task t = Task.parse_json(json);
						if (Task.get(t.getId()) != null)
							t.save(false);
						else {
							try {
								Mirakel.getWritableDatabase().insert(
										Task.TABLE, null, t.getContentValues());
							} catch (Exception e) {
								Log.e(TAG, "cannot restore Task");
							}
						}
					} catch (NoSuchListException e) {
						Log.e(TAG, "List not found");
					}
					break;
				case LIST:
					ListMirakel l = ListMirakel.parseJson(json);
					if (ListMirakel.getList(l.getId()) != null)
						l.save(false);
					else {
						try {
							Mirakel.getWritableDatabase().insert(
									ListMirakel.TABLE, null,
									l.getContentValues());
						} catch (Exception e) {
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
		SharedPreferences.Editor editor = Helpers.settings.edit();
		for (int i = 0; i < Helpers.settings.getInt("UndoNumber", 10); i++) {
			String old = Helpers.settings.getString(UNDO + (i + 1), "");
			editor.putString(UNDO + i, old);
		}
		editor.putString(UNDO + 10, "");
		editor.commit();
	}

	public static void logCreate(Task newTask, Context ctx) {
		updateLog(TASK, newTask.getId() + "", ctx);
	}

	public static void logCreate(ListMirakel newList, Context ctx) {
		updateLog(LIST, newList.getId() + "", ctx);
	}

}
