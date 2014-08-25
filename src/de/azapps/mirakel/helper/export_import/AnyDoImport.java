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

package de.azapps.mirakel.helper.export_import;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Environment;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import com.google.common.base.Optional;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.of;

import static de.azapps.mirakel.DefinitionsHelper.NoSuchListException;

public class AnyDoImport {
    private static final String TAG = "AnyDoImport";
    private static SparseIntArray taskMapping;

    public static boolean exec(final Context ctx,
                               final FileInputStream stream) throws NoSuchListException {
        JsonObject i;
        try {
            i = new JsonParser().parse(new InputStreamReader(stream))
            .getAsJsonObject();
        } catch (final JsonSyntaxException e) {
            Log.e(TAG, "malformed backup", e);
            return false;
        }
        final Set<Entry<String, JsonElement>> f = i.entrySet();
        SparseIntArray listMapping = new SparseIntArray();
        List<Pair<Integer, String>> contents = new ArrayList<>();
        taskMapping = new SparseIntArray();
        for (final Entry<String, JsonElement> e : f) {
            if (e.getKey().equals("categorys")) {
                final Iterator<JsonElement> iter = e.getValue()
                                                   .getAsJsonArray().iterator();
                while (iter.hasNext()) {
                    listMapping = parseList(iter.next().getAsJsonObject(),
                                            listMapping);
                }
            } else if (e.getKey().equals("tasks")) {
                final Iterator<JsonElement> iter = e.getValue()
                                                   .getAsJsonArray().iterator();
                while (iter.hasNext()) {
                    contents = parseTask(iter.next().getAsJsonObject(),
                                         listMapping, contents, ctx);
                }
            } else {
                Log.d(TAG, e.getKey());
            }
        }
        for (final Pair<Integer, String> pair : contents) {
            final Optional<Task> taskOptional = Task.get(taskMapping.get(pair.first));
            if (!taskOptional.isPresent()) {
                Log.d(TAG, "Task not found");
                continue;
            }
            final String oldContent = taskOptional.get().getContent();
            taskOptional.get().setContent(oldContent.equals("") ? pair.second
                                          : oldContent + "\n" + pair.second);
            taskOptional.get().save(false);
        }
        return true;
    }

    public static void handleImportAnyDo(final Activity activity) {
        final File dir = new File(Environment.getExternalStorageDirectory()
                                  + "/data/anydo/backups");
        if (dir.isDirectory()) {
            File lastBackup = null;
            for (final File f : dir.listFiles()) {
                if (lastBackup == null) {
                    lastBackup = f;
                } else if (f.getAbsolutePath().compareTo(
                               lastBackup.getAbsolutePath()) > 0) {
                    lastBackup = f;
                }
            }
            final File backupFile = lastBackup;
            if (backupFile == null) {
                return;
            }
            new AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.import_any_do_click))
            .setMessage(
                activity.getString(R.string.any_do_this_file,
                                   backupFile.getAbsolutePath()))
            .setPositiveButton(activity.getString(android.R.string.ok),
            new OnClickListener() {
                @Override
                public void onClick(
                    final DialogInterface dialog,
                    final int which) {
                    try {
                        exec(activity, new FileInputStream(
                                 backupFile));
                    } catch (final FileNotFoundException e) {
                        ErrorReporter
                        .report(ErrorType.FILE_NOT_FOUND);
                        Log.wtf(TAG, "file vanished", e);
                        return;
                    } catch (final NoSuchListException e) {
                        ErrorReporter
                        .report(ErrorType.LIST_VANISHED);
                        Log.wtf(TAG, "list vanished", e);
                        return;
                    }
                    android.os.Process
                    .killProcess(android.os.Process
                                 .myPid());
                }
            })
            .setNegativeButton(
                activity.getString(R.string.select_file),
            new OnClickListener() {
                @Override
                public void onClick(
                    final DialogInterface dialog,
                    final int which) {
                    Helpers.showFileChooser(
                        DefinitionsHelper.REQUEST_FILE_ANY_DO,
                        activity.getString(R.string.any_do_import_title),
                        activity);
                }
            }).show();
        } else {
            new AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.import_any_do_click))
            .setMessage(activity.getString(R.string.any_do_how_to))
            .setPositiveButton(activity.getString(android.R.string.ok),
            new OnClickListener() {
                @Override
                public void onClick(
                    final DialogInterface dialog,
                    final int which) {
                    handleImportAnyDo(activity);
                }
            })
            .setNegativeButton(
                activity.getString(R.string.select_file),
            new OnClickListener() {
                @Override
                public void onClick(
                    final DialogInterface dialog,
                    final int which) {
                    Helpers.showFileChooser(
                        DefinitionsHelper.REQUEST_FILE_ANY_DO,
                        activity.getString(R.string.any_do_import_title),
                        activity);
                }
            }).show();
            // TODO show dialog with tutorial
        }
    }


    private static SparseIntArray parseList(final JsonObject jsonList,
                                            final SparseIntArray listMapping) {
        final String name = jsonList.get("name").getAsString();
        final int id = jsonList.get("id").getAsInt();
        final ListMirakel l = ListMirakel.saveNewList(name);
        listMapping.put(id, (int) l.getId());
        return listMapping;
    }

    private static List<Pair<Integer, String>> parseTask(
        final JsonObject jsonTask, final SparseIntArray listMapping,
        final List<Pair<Integer, String>> contents, final Context ctx) throws NoSuchListException {
        final String name = jsonTask.get("title").getAsString();
        if (jsonTask.has("parentId")) {
            contents.add(new Pair<Integer, String>(jsonTask.get("parentId")
                                                   .getAsInt(), name));
            return contents;
        }
        final int list_id = jsonTask.get("categoryId").getAsInt();
        final Optional<ListMirakel> listMirakel = ListMirakel.get(listMapping.get(list_id));
        if (!listMirakel.isPresent()) {
            throw new NoSuchListException("Task:" + jsonTask.get("id").getAsInt());
        }
        final Task t = Task.newTask(name, listMirakel.get());
        taskMapping.put(jsonTask.get("id").getAsInt(), (int) t.getId());
        if (jsonTask.has("dueDate")) {
            final Calendar due = new GregorianCalendar();
            final long dueMs = jsonTask.get("dueDate").getAsLong();
            if (dueMs > 0) {
                due.setTimeInMillis(dueMs);
                t.setDue(of(due));
            }
        }
        if (jsonTask.has("priority")) {
            int prio = 0;
            if (jsonTask.get("priority").getAsString().equals("High")) {
                prio = 2;
            }
            t.setPriority(prio);
        }
        if (jsonTask.has("status")) {
            boolean done = false;
            final String status = jsonTask.get("status").getAsString();
            if (status.equals("DONE") || status.equals("CHECKED")) {
                done = true;
            } else if (status.equals("UNCHECKED")) {
                done = false;
            }
            t.setDone(done);
        }
        if (jsonTask.has("repeatMethod")) {
            final String repeat = jsonTask.get("repeatMethod").getAsString();
            if (!repeat.equals("TASK_REPEAT_OFF")) {
                Recurring r = null;
                if (repeat.equals("TASK_REPEAT_DAY")) {
                    r = Recurring.get(1, 0, 0);
                    if (r == null) {
                        r = Recurring.newRecurring(
                                ctx.getString(R.string.daily), 0, 0, 1, 0, 0,
                                true, null, null, false, false,
                                new SparseBooleanArray());
                    }
                } else if (repeat.equals("TASK_REPEAT_WEEK")) {
                    r = Recurring.get(7, 0, 0);
                    if (r == null) {
                        r = Recurring.newRecurring(
                                ctx.getString(R.string.weekly), 0, 0, 7, 0, 0,
                                true, null, null, false, false,
                                new SparseBooleanArray());
                    }
                } else if (repeat.equals("TASK_REPEAT_MONTH")) {
                    r = Recurring.get(0, 1, 0);
                    if (r == null) {
                        r = Recurring.newRecurring(
                                ctx.getString(R.string.monthly), 0, 0, 0, 1, 0,
                                true, null, null, false, false,
                                new SparseBooleanArray());
                    }
                } else if (repeat.equals("TASK_REPEAT_YEAR")) {
                    r = Recurring.get(0, 0, 1);
                    if (r == null) {
                        r = Recurring.newRecurring(
                                ctx.getString(R.string.yearly), 0, 0, 0, 0, 1,
                                true, null, null, false, false,
                                new SparseBooleanArray());
                    }
                }
                if (r != null) {
                    t.setRecurrence(r.getId());
                } else {
                    Log.d(TAG, repeat);
                }
            }
        }
        t.save(false);
        return contents;
    }

}
