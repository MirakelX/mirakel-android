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

import android.app.Activity;
import android.content.Context;
import android.view.View;
import de.azapps.mirakel.custom_views.TaskDetailFilePart.OnFileMarkedListner;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakelandroid.R;

public class TaskDetailFile extends TaskDetailSubtitleView<FileMirakel, TaskDetailFilePart> implements OnFileMarkedListner {
	private int					markCounter;
	private OnFileMarkedListner	onFileMarked;

	public TaskDetailFile(Context ctx) {
		super(ctx);
		this.markCounter = 0;
		this.title.setText(R.string.add_files);
		this.button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Helpers.showFileChooser(MainActivity.RESULT_ADD_FILE,
						TaskDetailFile.this.context
						.getString(R.string.file_select),
						(Activity) TaskDetailFile.this.context);
			}
		});
		this.button.setImageDrawable(this.context.getResources().getDrawable(
				android.R.drawable.ic_menu_add));
	}


	private void markFile(boolean markted) {
		this.markCounter += markted ? 1 : -1;
		for(TaskDetailFilePart v:this.viewList){
			v.setShortMark(this.markCounter > 0);
		}
	}

	@Override
	public void markFile(View v, FileMirakel e, boolean markted) {
		if (this.onFileMarked != null) {
			markFile(markted);
			this.onFileMarked.markFile(v, e, markted);
		}
	}

	@Override
	TaskDetailFilePart newElement() {
		TaskDetailFilePart t = new TaskDetailFilePart(this.context);
		t.setOnFileMarked(this);
		return t;
	}

	public void setAudioClick(OnClickListener onClick) {
		if (this.audioButton != null) {
			this.audioButton.setOnClickListener(onClick);
		}
	}


	public void setCameraClick(OnClickListener onClick){
		if(this.cameraButton!=null){
			this.cameraButton.setOnClickListener(onClick);
		}
	}

	public void setOnFileMarked(OnFileMarkedListner l) {
		this.onFileMarked = l;
	}

	@Override
	protected void updateView() {
		updateSubviews(this.task.getFiles());
	}

}
