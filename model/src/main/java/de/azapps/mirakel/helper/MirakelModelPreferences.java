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

package de.azapps.mirakel.helper;

import android.content.SharedPreferences.Editor;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import java.util.List;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;

public class MirakelModelPreferences extends MirakelPreferences {
    private static final String TAG = "MirakelModelPreferences";

    public static void setDefaultAccount(final AccountMirakel a) {
        settings.edit().putLong("defaultAccountID", a.getId()).commit();
    }

    @NonNull
    public static AccountMirakel getDefaultAccount() {
        final long id = settings.getLong("defaultAccountID", AccountMirakel
                                         .getLocal().getId());
        final Optional<AccountMirakel> a = AccountMirakel.get(id);
        if (a.isPresent()) {
            return a.get();
        }
        return AccountMirakel.getLocal();
    }

    public static Optional<ListMirakel> getImportDefaultList() {
        if (settings.getBoolean("importDefaultList", false)) {
            long listId;
            try {
                listId = (long) settings.getInt("defaultImportList", 0);
            } catch (final ClassCastException e) {
                listId = settings.getLong("defaultImportList", 0L);
            }
            return ListMirakel.get(listId);
        }
        return absent();
    }
    public static ListMirakel getSafeImportDefaultList() {
        return getImportDefaultList().or(ListMirakel.safeFirst());
    }

    public static ListMirakel getListForSubtask(final Task parent) {
        Optional<ListMirakel> list;
        if (MirakelCommonPreferences.addSubtaskToSameList()) {
            list = fromNullable(parent.getList());
        } else {
            list = subtaskAddToList();
        }
        // Create a new list and set this list as the default list for future
        // subtasks
        if (!list.isPresent()) {
            String listName = context.getString(R.string.subtask_list_name);
            list = ListMirakel.findByName(listName);
            if (!list.isPresent()) {
                list = of(ListMirakel.safeNewList(listName));
            }
            setSubtaskAddToList(list.get());
        }
        return list.get();
    }

    private static ListMirakel getListFromIdString(final int preference) {
        Optional<ListMirakel> list;
        try {
            list = ListMirakel.get(preference);
        } catch (final NumberFormatException e) {
            list = fromNullable((ListMirakel) SpecialList.firstSpecial().orNull());
        }
        if (!list.isPresent()) {
            return ListMirakel.safeFirst();
        } else {
            return list.get();
        }
    }

    public static ListMirakel getNotificationsList() {
        return getListFromIdString(MirakelCommonPreferences
                                   .getNotificationsListId());
    }

    public static ListMirakel getStartupList() {
        try {
            return ListMirakel.safeGet(Integer.parseInt(settings.getString(
                                           "startupList", "-1")));
        } catch (final NumberFormatException E) {
            return ListMirakel.safeFirst();
        }
    }

    public static int getSyncFrequency(final AccountMirakel account) {
        try {
            return Integer.parseInt(settings.getString("syncFrequency"
                                    + account.getName(), "-1"));
        } catch (final NumberFormatException E) {
            return -1;
        }
    }

    public static boolean setSyncFrequency(final AccountMirakel account,
                                           final int minutes) {
        final Editor editor = getEditor();
        editor.putString("syncFrequency" + account.getName(), minutes + "");
        return editor.commit();
    }

    public static Optional<ListMirakel> subtaskAddToList() {
        try {
            if (settings.contains("subtaskAddToList")) {
                return ListMirakel.get(settings.getLong("subtaskAddToList", -1));
            } else {
                return absent();
            }
        } catch (final Exception e) {
            // let old as fallback
            try {
                return ListMirakel.get(Integer.parseInt(settings.getString(
                        "subtaskAddToList", "-1")));
            } catch (final NumberFormatException e1) {
                Log.e(TAG, "Numberformat exception", e1);
                return absent();
            }
        }
    }

    public static void setSubtaskAddToList(final ListMirakel list) {
        final Editor editor = getEditor();
        editor.putLong("subtaskAddToList", list.getId());
        editor.commit();
    }

    public static String getDBName() {
        String db_name = "mirakel.db";
        if (MirakelCommonPreferences.isDemoMode()) {
            db_name = "demo_" + MirakelCommonPreferences.getLanguage() + ".db";
        }
        return db_name;
    }

    public static int getDividerPosition() {
        return settings.getInt("dividerPosition", 4);
    }

    public static void setDividerPosition(final int dividerPosition) {
        getEditor().putInt("dividerPosition", dividerPosition).apply();
    }
}
