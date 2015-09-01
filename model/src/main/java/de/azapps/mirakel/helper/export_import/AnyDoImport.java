/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.helper.export_import;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Environment;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.google.common.base.Optional;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
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
            taskOptional.get().setContent("".equals(oldContent) ? pair.second : (oldContent + "\n" +
                                          pair.second));
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
            new AlertDialogWrapper.Builder(activity)
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
            new AlertDialogWrapper.Builder(activity)
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
        final ListMirakel l = ListMirakel.safeNewList(name);
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
            final long dueMs = jsonTask.get("dueDate").getAsLong();
            if (dueMs > 0L) {
                t.setDue(of(new DateTime(dueMs)));
            }
        }
        if (jsonTask.has("priority")) {
            int prio = 0;
            if ("High".equals(jsonTask.get("priority").getAsString())) {
                prio = 2;
            }
            t.setPriority(prio);
        }
        if (jsonTask.has("status")) {
            boolean done = false;
            final String status = jsonTask.get("status").getAsString();
            if ("DONE".equals(status) || "CHECKED".equals(status)) {
                done = true;
            } else if (status.equals("UNCHECKED")) {
                done = false;
            }
            t.setDone(done);
        }
        if (jsonTask.has("repeatMethod")) {
            final String repeat = jsonTask.get("repeatMethod").getAsString();
            if (!repeat.equals("TASK_REPEAT_OFF")) {
                Optional<Recurring> recurringOptional = absent();
                switch (repeat) {
                case "TASK_REPEAT_DAY":
                    recurringOptional = Recurring.get(1, 0, 0);
                    if (!recurringOptional.isPresent()) {
                        recurringOptional = of(Recurring.newRecurring(
                                                   ctx.getString(R.string.daily), new Period(0, 0, 0, 1, 0, 0, 0, 0),
                                                   true, Optional.<DateTime>absent(), Optional.<DateTime>absent(), false, false,
                                                   new SparseBooleanArray()));
                    }
                    break;
                case "TASK_REPEAT_WEEK":
                    recurringOptional = Recurring.get(7, 0, 0);
                    if (!recurringOptional.isPresent()) {
                        recurringOptional = of(Recurring.newRecurring(
                                                   ctx.getString(R.string.weekly), new Period(0, 0, 1, 0, 0, 0, 0, 0),
                                                   true, Optional.<DateTime>absent(), Optional.<DateTime>absent(), false, false,
                                                   new SparseBooleanArray()));
                    }
                    break;
                case "TASK_REPEAT_MONTH":
                    recurringOptional = Recurring.get(0, 1, 0);
                    if (!recurringOptional.isPresent()) {
                        recurringOptional = of(Recurring.newRecurring(
                                                   ctx.getString(R.string.monthly), new Period(0, 1, 0, 0, 0, 0, 0, 0),
                                                   true, Optional.<DateTime>absent(), Optional.<DateTime>absent(), false, false,
                                                   new SparseBooleanArray()));
                    }
                    break;
                case "TASK_REPEAT_YEAR":
                    recurringOptional = Recurring.get(0, 0, 1);
                    if (!recurringOptional.isPresent()) {
                        recurringOptional = of(Recurring.newRecurring(
                                                   ctx.getString(R.string.yearly), new Period(1, 0, 0, 0, 0, 0, 0, 0),
                                                   true, Optional.<DateTime>absent(), Optional.<DateTime>absent(), false, false,
                                                   new SparseBooleanArray()));
                    }
                    break;
                }
                t.setRecurrence(recurringOptional);
            }
        }
        t.save(false);
        return contents;
    }

}
