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

package de.azapps.mirakel.new_ui.helper;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;

public class ListDialogHelpers {
    public static ListMirakel handleSortBy(final Context ctx,
                                           final ListMirakel list, final Helpers.ExecInterface callback,
                                           final Preference preferences) {
        final CharSequence[] sortingItems = ctx.getResources().getStringArray(
                                                R.array.task_sorting_items);
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(ctx);

        builder.setTitle(ctx.getString(R.string.task_sorting_title));
        builder.setItems(sortingItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                list.setSortBy(ListMirakel.SORT_BY.fromShort((short) which));
                list.save();
                if (preferences != null) {
                    preferences.setSummary(sortingItems[which]);
                }
                if (callback != null) {
                    callback.exec();
                }
            }
        });
        builder.show();
        return list;
    }
}
