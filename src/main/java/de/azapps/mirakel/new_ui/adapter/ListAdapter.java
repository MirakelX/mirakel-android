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

package de.azapps.mirakel.new_ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.adapter.MultiSelectCursorAdapter;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.generic.ModelFactory;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

public class ListAdapter extends MultiSelectCursorAdapter<ListAdapter.ListViewHolder, ListMirakel> {
    private static final String TAG = "ListAdapter";
    final LayoutInflater mInflater;

    public ListAdapter(final Context context, final Cursor cursor,
                       final OnItemClickedListener<ListMirakel> itemClickListener,
                       final MultiSelectCallbacks<ListMirakel> multiSelectCallbacks) {
        super(context, cursor, itemClickListener, multiSelectCallbacks);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ListMirakel fromCursor(@NonNull final Cursor cursor) {
        return (ListMirakel) ModelFactory.createModel(CursorGetter.unsafeGetter(cursor), ListMirakel.class);
    }

    @Override
    public ListViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int i) {
        final View view = mInflater.inflate(R.layout.row_list, viewGroup, false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ListViewHolder holder, final Cursor cursor, final int position) {
        if (position == MirakelModelPreferences.getDividerPosition()) {
            holder.viewSwitcher.setDisplayedChild(1);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        } else {
            holder.viewSwitcher.setDisplayedChild(0);
            final ListMirakel listMirakel = getItemAt(position);
            holder.list = listMirakel;
            holder.name.setText(listMirakel.getName());
            // First hide the icon and show it if it exists
            holder.icon.setVisibility(View.GONE);

            updateIcon(listMirakel, holder);

            if (listMirakel.isSpecial()) {
                new UpdateTaskCountTask().execute(holder);
            } else {
                final long count = cursor.getLong(cursor.getColumnIndex("task_count"));
                holder.count.setText(String.valueOf(count));
            }

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
    }

    public class ListViewHolder extends MultiSelectCursorAdapter.MultiSelectViewHolder {
        @InjectView(R.id.row_list_drag)
        ImageView dragImage;
        @InjectView(R.id.row_list_icon)
        ImageView icon;
        @InjectView(R.id.list_name)
        TextView name;
        @InjectView(R.id.list_count)
        TextView count;
        @InjectView(R.id.view_switcher)
        ViewSwitcher viewSwitcher;
        ListMirakel list;

        public ListViewHolder(final View view) {
            super(view);
            ButterKnife.inject(this, view);
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

    private void updateIcon(final ListMirakel listMirakel, final ListViewHolder holder) {
        // This is much faster and less annoying than using an async task.
        if (listMirakel.getIconPath().isPresent()) {
            final Uri iconUri = listMirakel.getIconPath().get();
            final String path = iconUri.toString();
            try {
                final Bitmap bitmap;
                if (path.startsWith("file:///android_asset/")) {
                    bitmap = BitmapFactory.decodeStream(mContext.getAssets().open(path.replace("file:///android_asset/",
                                                        "")));
                } else {
                    final InputStream inputStream = mContext.getContentResolver().openInputStream(iconUri);
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }
                final BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
                drawable.setColorFilter(ThemeManager.getColor(R.attr.colorTextGrey), PorterDuff.Mode.MULTIPLY);
                holder.icon.setImageDrawable(drawable);
                holder.icon.setVisibility(View.VISIBLE);
            } catch (final FileNotFoundException e) {
                Log.w(TAG, "Image not found", e);
                holder.icon.setVisibility(View.GONE);
            } catch (final IOException e) {
                Log.w(TAG, "Other IO Error", e);
                holder.icon.setVisibility(View.GONE);
            }
        } else {
            holder.icon.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        final int count = super.getItemCount();
        if ((count == 0) || (count < MirakelModelPreferences.getDividerPosition())) {
            return count;
        } else {
            return count + 1;
        }
    }

    @Override
    public void onBindViewHolder(final ListViewHolder viewHolder, int position) {
        // do not move the cursor! We are moving it later on. This is needed 'cause we are using this divider.
        onBindViewHolder(viewHolder, getCursor(), position);
    }

    @Override
    protected boolean toggleSelection(final int pos) {
        if (pos != MirakelModelPreferences.getDividerPosition()) {
            return super.toggleSelection(pos);
        }
        return false;
    }

    // For Multi-select


    @Override
    /**
     * We need to override this for the DragSortRecycler
     */
    public long getItemId(final int position) {
        return position;
    }

    @NonNull
    protected ListMirakel getItemAt(int position) {
        final Cursor cursor = getCursor();
        if ((position > 0) && (position > MirakelModelPreferences.getDividerPosition())) {
            position--;
        }
        cursor.moveToPosition(position);
        return fromCursor(cursor);
    }

    public void onItemMoved(final int from, final int to) {
        if (from == to) {
            return;
        }
        final boolean tmpVal = selectedItems.get(from);
        selectedItems.delete(from);
        final int start = Math.min(from, to);
        final int end = Math.max(from, to);
        final int delta = (from > to) ? 1 : -1;
        for (int i = start; i <= end; i++) {
            final boolean tmp = selectedItems.get(i);
            selectedItems.delete(i);
            if (tmp) {
                selectedItems.put(i + delta, true);
            }
        }
        if (tmpVal) {
            selectedItems.put(to, true);
        }
    }
}
