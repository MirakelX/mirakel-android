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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.azapps.mirakelandroid.R;

public abstract class TaskDetailSubtitleView<E, T extends TaskDetailSubListBase<E>> extends BaseTaskDetailRow {

	protected final ImageButton	audioButton;
	protected final ImageButton	button;
	protected final ImageButton	cameraButton;
	protected final View		divider;
	protected final View		subtitle;
	protected final TextView	title;
	protected List<T>			viewList;

	public TaskDetailSubtitleView(Context ctx) {
		super(ctx);
		setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		this.subtitle = LayoutInflater.from(this.context).inflate(
				R.layout.task_subtitle, null);
		this.title = (TextView) this.subtitle.findViewById(R.id.task_subtitle);
		this.button = (ImageButton) this.subtitle
				.findViewById(R.id.task_subtitle_button);
		this.audioButton = (ImageButton) this.subtitle
				.findViewById(R.id.task_subtitle_audio_button);
		this.cameraButton = (ImageButton) this.subtitle
				.findViewById(R.id.task_subtitle_camera_button);
		this.divider = this.subtitle.findViewById(R.id.item_separator);
		this.divider.setBackgroundColor(this.context.getResources().getColor(
				inactive_color));
		this.title.setTextColor(this.context.getResources().getColor(
				inactive_color));
		this.viewList = new ArrayList<T>();
		setOrientation(VERTICAL);
		addView(this.subtitle);
	}

	public void disableMarked(){
		for(T l:this.viewList){
			l.disableMark();
		}
	}

	abstract T newElement();

	protected void updateSubviews(List<E> elementList) {
		if (elementList.size() < this.viewList.size()) {
			// remove
			while (getChildCount() > elementList.size() + 1) {
				removeViewAt(getChildCount() - 1);
				this.viewList.remove(this.viewList.size() - 1);
			}
		} else if (elementList.size() > this.viewList.size()) {
			// add
			for (int i = this.viewList.size(); i < elementList.size(); i++) {
				T temp = newElement();
				this.viewList.add(temp);
				addView(temp);
			}
		}
		for (int i = 0; i < elementList.size(); i++) {
			this.viewList.get(i).updatePart(elementList.get(i));
		}
		invalidate();
	}


}
