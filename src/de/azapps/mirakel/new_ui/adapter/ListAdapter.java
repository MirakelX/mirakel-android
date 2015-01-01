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
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.mirakel.ThemeManager;
import de.azapps.mirakel.adapter.MultiSelectCursorAdapter;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.model.list.ListMirakel;
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
        return new ListMirakel(cursor);
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

    public class ListViewHolder extends MultiSelectCursorAdapter.MultiSelectViewHolder {
        @InjectView(R.id.row_list_drag)
        ImageView dragImage;
        @InjectView(R.id.row_list_icon)
        ImageView icon;
        @InjectView(R.id.list_name)
        TextView name;
        @InjectView(R.id.list_count)
        TextView count;
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
                final Uri iconUri = listMirakel.getIconPath().get();
                final String path = iconUri.toString();
                try {
                    if (path.startsWith("file:///android_asset/")) {
                        bitmap = BitmapFactory.decodeStream(mContext.getAssets().open(path.replace("file:///android_asset/",
                                                            "")));
                    } else {
                        final InputStream inputStream = mContext.getContentResolver().openInputStream(iconUri);
                        bitmap = BitmapFactory.decodeStream(inputStream);
                    }
                } catch (final FileNotFoundException e) {
                    Log.w(TAG, "Image not found", e);
                    return null;
                } catch (final IOException e) {
                    Log.w(TAG, "Other IO Error", e);
                    return null;
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


    @Override
    /**
     * We need to override this for the DragSortRecycler
     */
    public long getItemId(int position) {
        return position;
    }
}
