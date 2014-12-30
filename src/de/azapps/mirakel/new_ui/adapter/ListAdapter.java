/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *  Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.new_ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.mirakel.ThemeManager;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

public class ListAdapter extends CursorAdapter<ListAdapter.ListViewHolder> {
    private static final String TAG = "ListAdapter";
    final LayoutInflater mInflater;
    private final OnItemClickedListener<ListMirakel> itemClickListener;
    private boolean selectMode = false;
    private final MultiSelectCallbacks multiSelectCallbacks;
    private final SparseBooleanArray selectedItems = new SparseBooleanArray();

    public ListAdapter(final Context context, final Cursor cursor,
                       final OnItemClickedListener<ListMirakel> itemClickListener,
                       MultiSelectCallbacks multiSelectCallbacks) {
        super(context, cursor);
        this.multiSelectCallbacks = multiSelectCallbacks;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.itemClickListener = itemClickListener;
        setHasStableIds(true);
    }

    @Override
    public ListViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int i) {
        final View view = mInflater.inflate(R.layout.row_list, viewGroup, false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ListViewHolder holder, final Cursor cursor, int position) {
        final ListMirakel listMirakel = new ListMirakel(cursor);
        holder.list = listMirakel;
        holder.name.setText(listMirakel.getName());
        // First hide the icon and show it if it exists
        holder.icon.setVisibility(View.GONE);
        new UpdateIconTask(mContext).execute(holder);
        final long count = cursor.getLong(cursor.getColumnIndex("task_count"));
        if (count != -1) {
            holder.count.setText(String.valueOf(count));
        } else {
            new UpdateTaskCountTask().execute(holder);
        }

        holder.position = position;
        if (selectedItems.get(position)) {
            holder.itemView.setBackgroundColor(ThemeManager.getColor(R.attr.colorSelectedRow));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
        if (selectMode) {
            holder.dragImage.setVisibility(View.VISIBLE);
        } else {
            holder.dragImage.setVisibility(View.GONE);
        }
    }

    public class ListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
        View.OnLongClickListener {
        @InjectView(R.id.row_list_drag)
        ImageView dragImage;
        @InjectView(R.id.row_list_icon)
        ImageView icon;
        @InjectView(R.id.list_name)
        TextView name;
        @InjectView(R.id.list_count)
        TextView count;
        ListMirakel list;
        int position;

        public ListViewHolder(final View view) {
            super(view);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            ButterKnife.inject(this, view);
        }

        public ListMirakel getList() {
            return list;
        }

        @Override
        public void onClick(final View v) {

            if (selectMode) {
                toggleSelection(position);
            } else {
                itemClickListener.onItemSelected(list);
            }
        }

        @Override
        public boolean onLongClick(final View v) {
            setSelectMode(true);
            toggleSelection(position);
            return true;
        }
    }

    private static class UpdateTaskCountTask extends AsyncTask<ListViewHolder, Void, Long> {
        private ListViewHolder viewHolder;

        @Override
        protected Long doInBackground(final ListViewHolder... holders) {
            viewHolder = holders[0];

            return viewHolder.list.countTasks();
        }

        @Override
        protected void onPostExecute(final Long result) {
            viewHolder.count.setText(String.valueOf(result));
        }

    }

    private static class UpdateIconTask extends AsyncTask<ListViewHolder, Void, Drawable> {
        private ListViewHolder viewHolder;
        private Context mContext;

        private UpdateIconTask(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        @Nullable
        protected Drawable doInBackground(final ListViewHolder... holders) {
            viewHolder = holders[0];
            final ListMirakel listMirakel = viewHolder.list;

            if (listMirakel.getIconPath().isPresent()) {
                final Bitmap bitmap;
                final String path = listMirakel.getIconPath().get();
                if (path.startsWith("file:///android_asset/")) {
                    try {
                        bitmap = BitmapFactory.decodeStream(mContext.getAssets().open(path.replace("file:///android_asset/",
                                                            "")));
                    } catch (final IOException e) {
                        Log.w(TAG, "Image not found", e);
                        return null;
                    }
                } else {
                    bitmap = BitmapFactory.decodeFile(listMirakel.getIconPath().get());
                }
                final BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
                drawable.setColorFilter(ThemeManager.getColor(R.attr.colorTextGrey), PorterDuff.Mode.MULTIPLY);
                return drawable;
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(@Nullable final Drawable drawable) {
            if (drawable != null) {
                viewHolder.icon.setImageDrawable(drawable);
                viewHolder.icon.setVisibility(View.VISIBLE);
            }
        }
    }


    // For Multi-select

    public void setSelectMode(final boolean selectMode) {
        this.selectMode = selectMode;
        multiSelectCallbacks.onSelectModeChanged(selectMode);
        notifyDataSetChanged();
    }

    public void toggleSelection(final int pos) {
        final Cursor cursor = getCursor();
        cursor.moveToPosition(pos);
        final ListMirakel item = new ListMirakel(cursor);

        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
            multiSelectCallbacks.onRemoveSelectedItem(item);
        } else {
            // Check if it is allowed to select the item
            if (!multiSelectCallbacks.canAddItem(item)) {
                if (getSelectedItemCount() == 0) {
                    setSelectMode(false);
                }
                return;
            }
            selectedItems.put(pos, true);
            multiSelectCallbacks.onAddSelectedItem(item);
        }
        notifyItemChanged(pos);
    }

    public void clearSelections() {
        setSelectMode(false);
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public ArrayList<ListMirakel> getSelectedItems() {
        final ArrayList<ListMirakel> items =
            new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            final int pos = selectedItems.keyAt(i);
            getCursor().moveToPosition(pos);
            items.add(new ListMirakel(getCursor()));
        }
        return items;
    }

    public interface MultiSelectCallbacks {
        public void onSelectModeChanged(boolean selectMode);

        public boolean canAddItem(ListMirakel listMirakel);

        public void onAddSelectedItem(ListMirakel listMirakel);

        public void onRemoveSelectedItem(ListMirakel listMirakel);
    }

    @Override
    /**
     * We need to override this for the DragSortRecycler
     */
    public long getItemId(int position) {
        return position;
    }
}
