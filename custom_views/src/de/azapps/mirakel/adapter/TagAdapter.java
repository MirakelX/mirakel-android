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
package de.azapps.mirakel.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.model.tags.Tag;

public class TagAdapter extends CursorAdapter {

	private OnClickListener onClick;

	public TagAdapter(final Context context) {
		super(context, null, false);
	}

	@Override
	public void bindView(final View v, final Context ctx, final Cursor c) {
		final Tag tag = Tag.cursorToTag(c);
		final TextView t = (TextView) v.findViewById(R.id.tag_list_name);
		t.setText(tag.getName());
		t.setOnClickListener(this.onClick);
		t.setTag(tag);
	}

	@Override
	public View newView(final Context ctx, final Cursor c,
			final ViewGroup parent) {
		if (c != null && c.getCount() > 0) {
			final Tag tag = Tag.cursorToTag(c);
			final LayoutInflater inflater = LayoutInflater.from(ctx);
			final View v = inflater.inflate(R.layout.tag_list_row, null);

			final TextView t = (TextView) v.findViewById(R.id.tag_list_name);
			t.setText(tag.toString());
			t.setOnClickListener(this.onClick);
			return v;
		}
		return null;
	}

	public void setOnClickListerner(final OnClickListener l) {
		this.onClick = l;
	}
}
