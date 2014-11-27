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
package de.azapps.mirakel.main_activity.list_fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.azapps.mirakel.adapter.MirakelArrayAdapter;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.ViewHelper;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountVanishedException;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.main_activity.R;

@SuppressLint ("UseSparseArrays")
public class ListAdapter extends MirakelArrayAdapter<ListMirakel> {
    static class ListHolder {
        TextView listAccount;
        ImageView listRowDrag;
        TextView listRowName;
        TextView listRowTaskNumber;
    }

    private boolean enableDrop;

    private final Map<Long, View> viewsForLists = new HashMap<> ();

    public ListAdapter (final Context c) {
        // do not call this, only for error-fixing there
        super (c, 0, new ArrayList<ListMirakel> ());
    }

    public ListAdapter (final Context context, final int layoutResourceId,
                        final List<ListMirakel> data, final boolean enable) {
        super (context, layoutResourceId, data);
        this.enableDrop = enable;
    }

    @Override
    public void changeData (final List<ListMirakel> lists) {
        this.viewsForLists.clear ();
        super.changeData (lists);
    }

    @Override
    public View getView (final int position, final View convertView,
                         final ViewGroup parent) {
        if (!getDataAt(position).isPresent()) {
            return new View(context);
        }
        View row = convertView;
        ListHolder holder;
        if (row == null) {
            final LayoutInflater inflater = ((Activity) this.context)
                                            .getLayoutInflater ();
            row = inflater.inflate (this.layoutResourceId, parent, false);
            holder = new ListHolder ();
            holder.listRowName = (TextView) row
                                 .findViewById (R.id.list_row_name);
            holder.listRowTaskNumber = (TextView) row
                                       .findViewById (R.id.list_row_task_number);
            holder.listRowDrag = (ImageView) row
                                 .findViewById (R.id.list_row_drag);
            holder.listAccount = (TextView) row
                                 .findViewById (R.id.list_row_account_name);
            row.setTag (holder);
        } else {
            holder = (ListHolder) row.getTag ();
        }
        final ListMirakel list = this.getDataAt(position).get();
        if (!this.enableDrop) {
            holder.listRowDrag.setVisibility (View.GONE);
        } else {
            holder.listRowDrag.setVisibility (View.VISIBLE);
        }
        holder.listRowName.setText (list.getName ());
        holder.listRowName.setTag (list);
        holder.listRowTaskNumber.setText (String.valueOf(list.countTasks()));
        if (list.isSpecial () || !MirakelCommonPreferences.isShowAccountName ()) {
            holder.listAccount.setVisibility (View.GONE);
        } else {
            holder.listAccount.setVisibility (View.VISIBLE);

            AccountMirakel a ;
            try {
                a = list.getAccount();
            } catch (AccountVanishedException ignored) {
                a = AccountMirakel.getLocal();
                list.setAccount(a);
                list.save(false);
            }
            holder.listAccount.setText(a.getName());
            holder.listAccount.setTextColor(this.context.getResources()
                                            .getColor(android.R.color.darker_gray));
        }
        this.viewsForLists.put(list.getId(), row);
        final int w = row.getWidth() != 0 ? row.getWidth() : parent.getWidth();
        ViewHelper.setListColorBackground (list, row, w);
        if (this.isSelectedAt (position)) {
            row.setBackgroundColor (this.context.getResources ().getColor (
                                        this.darkTheme ? R.color.highlighted_text_holo_dark
                                        : R.color.highlighted_text_holo_light));
        }
        return row;
    }

    public View getViewForList (final ListMirakel list) {
        return this.viewsForLists.get (list.getId ());
    }

    public boolean isDropEnabled () {
        return this.enableDrop;
    }

    public void onDrop (final int from, final int to) {
        if (getDataAt(from).isPresent() && getDataAt(to).isPresent()) {
            final ListMirakel t = this.getDataAt(from).get();
            String TABLE;
            if (t.getId() < 0) {
                TABLE = SpecialList.TABLE;
            } else {
                TABLE = ListMirakel.TABLE;
            }
            ContentValues cv = new ContentValues();
            ListMirakel lTo = this.getDataAt(to).get();
            ListMirakel lFrom = this.getDataAt(from).get();
            cv.put("TABLE", TABLE);
            if (to < from) {// move list up
                context.getContentResolver().update(MirakelInternalContentProvider.UPDATE_LIST_MOVE_UP_URI, cv,
                                                    "lft>="
                                                    + lTo.getLft() + " and lft<"
                                                    + lFrom.getLft(), null);
            } else if (to > from) {// move list down
                context.getContentResolver().update(MirakelInternalContentProvider.UPDATE_LIST_MOVE_DOWN_URI, cv,
                                                    "lft>"
                                                    + lFrom.getLft() + " and lft<="
                                                    + lTo.getLft(), null);
            } else {
                return;
            }
            t.setLft(lTo.getLft());
            t.save();
            context.getContentResolver().update(MirakelInternalContentProvider.UPDATE_LIST_FIX_RGT_URI, cv,
                                                null, null);
            this.remove(from);
            this.addToData(to, t);
            notifyDataSetChanged();
            final Thread load = new Thread(new Runnable() {
                @Override
                public void run() {
                    changeData(ListMirakel.all());
                }
            });
            load.start();
        }
    }

}
