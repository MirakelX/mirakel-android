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

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;

public class ListDialogHelpers {
    protected static final String TAG = "ListDialogHelpers";
    /**
     * Ugly helper variable
     */
    private static AlertDialog alert;

    /**
     * Handle the SortBy dialog
     *
     * @param ctx
     * @param list
     * @return
     */
    public static ListMirakel handleSortBy(final Context ctx,
                                           final ListMirakel list, final TextView res) {
        return handleSortBy(ctx, list, null, null);
    }

    public static ListMirakel handleSortBy(final Context ctx,
                                           final ListMirakel list, final Preference res) {
        return handleSortBy(ctx, list, null, res);
    }

    public static ListMirakel handleSortBy(final Context ctx,
                                           final ListMirakel list) {
        return handleSortBy(ctx, list, null, null);
    }

    /**
     * Handle the SortBy dialog
     *
     * @param ctx
     * @param list
     * @param callback
     * @return
     */
    public static ListMirakel handleSortBy(final Context ctx,
                                           final ListMirakel list, final Helpers.ExecInterface callback,
                                           final Preference preferences) {
        final CharSequence[] sortingItems = ctx.getResources().getStringArray(
                                                R.array.task_sorting_items);
        MaterialDialog.Builder builder = new MaterialDialog.Builder(ctx);

        builder.title(ctx.getString(R.string.task_sorting_title));
        builder.items(sortingItems);
        // Find item
        builder.itemsCallbackSingleChoice(list.getSortBy().getShort(), new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog materialDialog, View view, int item,
                                    CharSequence charSequence) {
                list.setSortBy(ListMirakel.SORT_BY.fromShort((short) item));
                list.save();
                if (preferences != null) {
                    preferences.setSummary(sortingItems[item]);
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
