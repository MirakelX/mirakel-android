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
package de.azapps.mirakel.custom_views;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.larswerkman.colorpicker.ColorPicker;
import com.larswerkman.colorpicker.SVBar;

import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;

public class TagListView extends View {
	private Task task;
	/**
	 * This is a List of pairs (point, tag) which maps the bottom right point of
	 * the tag-rect to a tag. The list is ordered by the position of the tags
	 */
	private final List<Pair<Point, Tag>> pointMap = new ArrayList<Pair<Point, Tag>>();
	private final Paint paintBackground;
	private final Paint paintText;
	private final Rect rect;
	private final RectF rectF;
	private final int black;
	private final int white;
	private final float TAG_SIZE;

	public TagListView(final Context context) {
		super(context);
		this.TAG_SIZE = scale(20);
		this.paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.rect = new Rect();
		this.rectF = new RectF();
		this.black = getContext().getResources()
				.getColor(android.R.color.black);
		this.white = getContext().getResources()
				.getColor(android.R.color.white);

		setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(final View v, final MotionEvent event) {
				if (event.getAction() != MotionEvent.ACTION_UP) {
					return true;
				}
				final int x = (int) event.getX();
				final int y = (int) event.getY();
				for (final Pair<Point, Tag> pair : TagListView.this.pointMap) {
					final int px = pair.first.x;
					final int py = pair.first.y;
					if (y < py && x < px) {
						handleTagTouch(pair.second);
						break;
					}
				}
				return false;
			}
		});

		this.paintText.setTextSize(this.TAG_SIZE);
	}

	private void handleTagTouch(final Tag tag) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(
				getContext());
		builder.setTitle(tag.getName());
		final String items[] = getResources().getStringArray(R.array.tag_menu);
		builder.setItems(items, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				switch (which) {
				case 0:
					handleTagEdit(tag);
					break;
				case 1:
					TagListView.this.task.removeTag(tag);
					invalidate();
					break;

				default:
					break;
				}
			}
		});
		builder.show();

	}

	private void handleTagEdit(final Tag tag) {
		final LinearLayout layout = (LinearLayout) inflate(getContext(),
				R.layout.tag_edit_dialog, null);
		final EditText editName = (EditText) layout
				.findViewById(R.id.tag_edit_name);
		editName.setText(tag.getName());
		final ColorPicker picker = (ColorPicker) layout
				.findViewById(R.id.color_picker);
		final SVBar op = (SVBar) layout.findViewById(R.id.svbar_color_picker);
		picker.addSVBar(op);
		picker.setColor(tag.getBackgroundColor());
		picker.setOldCenterColor(tag.getBackgroundColor());
		final AlertDialog.Builder builder = new AlertDialog.Builder(
				getContext());
		builder.setView(layout);
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						tag.setBackgroundColor(picker.getColor());
						tag.setName(editName.getText().toString());
						tag.save();
						invalidate();
					}
				});
		builder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						// TODO Auto-generated method stub

					}
				});
		builder.show();
	}

	public void init(final Task task) {
		this.task = task;
		invalidate();
	}

	private float scale(final float number) {
		return getContext().getResources().getDisplayMetrics().density * number;
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		float begin_x = scale(20);
		float begin_y = 0;
		float textLength = 0;
		final List<Tag> tags = this.task.getTags();
		this.pointMap.clear();
		if (tags.size() == 0) {
			this.paintText.setColor(getContext().getResources().getColor(
					android.R.color.darker_gray));
			canvas.drawText(getContext().getString(R.string.add_tags),
					scale(10), scale(21), this.paintText);
			setMinimumHeight((int) scale(25));
			return;
		}
		for (int i = 0; i < tags.size(); i++) {
			final Tag t = tags.get(i);
			this.paintText.setColor(t.isDarkText() ? this.black : this.white);
			this.paintText.getTextBounds(t.getName(), 0, t.getName().length(),
					this.rect);

			textLength = this.paintText.measureText(t.getName());

			if (begin_y == 0) {
				begin_y = this.TAG_SIZE + this.rect.bottom;
			}
			this.rectF.bottom = begin_y + scale(7);
			this.rectF.left = this.rect.left - scale(5) + begin_x;
			this.rectF.right = this.rect.right + scale(5) + begin_x;
			this.rectF.top = begin_y - this.TAG_SIZE;

			this.pointMap.add(new Pair<Point, Tag>(new Point(
					(int) this.rectF.right, (int) this.rectF.bottom), t));

			this.paintBackground.setColor(t.getBackgroundColor());

			// draw all
			canvas.drawRoundRect(this.rectF, 0, 0, this.paintBackground);
			canvas.drawText(t.getName(), begin_x, begin_y, this.paintText);

			begin_x += textLength + scale(20);
			if (begin_x >= canvas.getWidth() - scale(100)
					&& i != tags.size() - 1) {
				begin_x = scale(20);
				begin_y += this.TAG_SIZE + scale(20);
			}
		}
		setMinimumHeight((int) (begin_y + scale(5)));

	}
}
