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

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.widget.TextView;

import com.google.common.base.Optional;

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
     * @param cls
     * @return
     */
    public static ListMirakel handleSortBy(final Context ctx,
                                           final ListMirakel list, final Helpers.ExecInterface cls,
                                           final Preference res) {
        final CharSequence[] SortingItems = ctx.getResources().getStringArray(
                                                R.array.task_sorting_items);
        final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(ctx.getString(R.string.task_sorting_title));
        builder.setSingleChoiceItems(SortingItems, list.getSortBy().getShort(),
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int item) {
                list.setSortBy(ListMirakel.SORT_BY.fromShort((short) item));
                list.save();
                if (res != null) {
                    res.setSummary(SortingItems[item]);
                }
                if (cls != null) {
                    cls.exec();
                }
                alert.dismiss(); // Ugly
            }
        });
        alert = builder.create();
        alert.show();
        return list;
    }



}
