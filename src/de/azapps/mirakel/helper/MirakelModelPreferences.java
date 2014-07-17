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

import java.util.List;

import android.content.SharedPreferences.Editor;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class MirakelModelPreferences extends MirakelPreferences {
    private static final String TAG = "MirakelModelPreferences";

    public static void setDefaultAccount(final AccountMirakel a) {
        settings.edit().putLong("defaultAccountID", a.getId()).commit();
    }

    public static AccountMirakel getDefaultAccount() {
        final long id = settings.getLong("defaultAccountID", AccountMirakel
                                         .getLocal().getId());
        final AccountMirakel a = AccountMirakel.get(id);
        if (a != null) {
            return a;
        }
        return AccountMirakel.getLocal();
    }

    public static ListMirakel getImportDefaultList(final boolean safe) {
        if (settings.getBoolean("importDefaultList", false)) {
            final int listId = settings.getInt("defaultImportList", 0);
            if (listId == 0) {
                return null;
            }
            return ListMirakel.get(listId);
        }
        if (!safe) {
            return null;
        }
        return ListMirakel.safeFirst(context);
    }

    public static ListMirakel getListForSubtask(final Task parent) {
        ListMirakel list = null;
        if (MirakelCommonPreferences.addSubtaskToSameList()) {
            list = parent.getList();
        } else {
            list = subtaskAddToList();
        }
        // Create a new list and set this list as the default list for future
        // subtasks
        if (list == null) {
            list = ListMirakel.newList(context
                                       .getString(R.string.subtask_list_name));
            setSubtaskAddToList(list);
        }
        return list;
    }

    private static ListMirakel getListFromIdString(final int preference) {
        ListMirakel list;
        try {
            list = ListMirakel.get(preference);
        } catch (final NumberFormatException e) {
            list = SpecialList.firstSpecial();
        }
        if (list == null) {
            list = ListMirakel.safeFirst(context);
        }
        return list;
    }

    public static ListMirakel getNotificationsList() {
        return getListFromIdString(MirakelCommonPreferences
                                   .getNotificationsListId());
    }

    public static ListMirakel getNotificationsListOpen() {
        return ListMirakel.get(MirakelCommonPreferences
                               .getNotificationsListOpenId());
    }

    public static ListMirakel getStartupList() {
        try {
            return ListMirakel.safeGet(Integer.parseInt(settings.getString(
                                           "startupList", "-1")));
        } catch (final NumberFormatException E) {
            return ListMirakel.safeFirst(context);
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

    public static ListMirakel subtaskAddToList() {
        try {
            if (settings.contains("subtaskAddToList")) {
                return ListMirakel.get(settings.getLong("subtaskAddToList", -1));
            } else {
                return null;
            }
        } catch (final Exception e) {
            // let old as fallback
            try {
                return ListMirakel.get(Integer.parseInt(settings.getString(
                        "subtaskAddToList", "-1")));
            } catch (final NumberFormatException e1) {
                Log.e(TAG, "Numberformat exception", e1);
                return null;
            }
        }
    }

    public static void setSubtaskAddToList(final ListMirakel list) {
        final Editor editor = getEditor();
        editor.putLong("subtaskAddToList", list.getId());
        editor.commit();
    }

    public static boolean useSync() {
        final List<AccountMirakel> all = AccountMirakel.all();
        for (final AccountMirakel a : all) {
            if (a.getType() != ACCOUNT_TYPES.LOCAL && a.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    public static String getDBName() {
        String db_name = "mirakel.db";
        if (MirakelCommonPreferences.isDemoMode()) {
            db_name = "demo_" + MirakelCommonPreferences.getLanguage() + ".db";
        }
        return db_name;
    }
}
