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

import android.content.Context;
import android.text.util.Linkify;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.tools.Log;

public class TaskDetailContent extends BaseTaskDetailRow {

	public interface OnEditChanged {
		abstract public void handleCab(boolean startEdit);
	}

	protected static final String	TAG	= "TaskDetailContent";
	private String content;
	protected OnEditChanged			editChanged;
	private final ImageButton		editContent;
	protected boolean				isContentEdit;
	private final TextView			taskContent;

	protected final EditText		taskContentEdit;
	protected final ViewSwitcher	taskContentSwitcher;

	public TaskDetailContent(Context ctx) {
		super(ctx);
		inflate(ctx, R.layout.task_content, this);
		this.taskContent = (TextView) findViewById(R.id.task_content);
		this.taskContentSwitcher = (ViewSwitcher) findViewById(R.id.switcher_content);
		this.taskContentEdit = (EditText) findViewById(R.id.task_content_edit);
		this.isContentEdit = false;
		this.editContent = (ImageButton) findViewById(R.id.edit_content);
		this.editContent.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "edit content");
				TaskDetailContent.this.isContentEdit = !TaskDetailContent.this.isContentEdit;
				if (!TaskDetailContent.this.isContentEdit) {
					saveContentHelper();
				}
				TaskDetailContent.this.context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
				TaskDetailContent.this.taskContentSwitcher.showNext();
				v.setBackgroundResource(TaskDetailContent.this.isContentEdit ? android.R.drawable.ic_menu_save
						: android.R.drawable.ic_menu_edit);
				if (TaskDetailContent.this.isContentEdit
						&& TaskDetailContent.this.task != null) {
					TaskDetailContent.this.taskContentEdit
					.setOnFocusChangeListener(new OnFocusChangeListener() {

						@Override
						public void onFocusChange(View view, boolean hasFocus) {
							InputMethodManager imm = (InputMethodManager) TaskDetailContent.this.context
									.getSystemService(Context.INPUT_METHOD_SERVICE);
							if (hasFocus) {
								imm.showSoftInput(
										TaskDetailContent.this.taskContentEdit,
										InputMethodManager.SHOW_IMPLICIT);
							} else {
								imm.showSoftInput(
										TaskDetailContent.this.taskContentEdit,
										InputMethodManager.HIDE_IMPLICIT_ONLY);
								imm.hideSoftInputFromWindow(
										TaskDetailContent.this.taskContentEdit
										.getWindowToken(), 0);
							}
							if (TaskDetailContent.this.editChanged != null) {
								TaskDetailContent.this.editChanged
								.handleCab(hasFocus);
							}

						}
					});
					TaskDetailContent.this.taskContentEdit.requestFocus();
					TaskDetailContent.this.taskContentEdit
					.setSelection(TaskDetailContent.this.task
							.getContent().length());
				}

			}
		});
	}

	public void cancelContent() {
		if (this.taskContentSwitcher.getCurrentView().getId() != this.taskContent
				.getId()) {
			TaskDetailContent.this.taskContentSwitcher.showNext();
		}
		this.isContentEdit = false;
		this.editContent
		.setBackgroundResource(TaskDetailContent.this.isContentEdit ? android.R.drawable.ic_menu_save
				: android.R.drawable.ic_menu_edit);
		TaskDetailContent.this.taskContentEdit
		.setText(TaskDetailContent.this.task.getContent());
	}

	public void saveContent() {
		saveContentHelper();
		cancelContent();
	}

	public void saveContentHelper() {
		TaskDetailContent.this.task
		.setContent(TaskDetailContent.this.taskContentEdit.getText()
				.toString());
		save();
		this.content = this.task.getContent();
		if (this.task.getContent().length() > 0) {
			this.taskContent.setText(this.task.getContent());
			Linkify.addLinks(this.taskContent, Linkify.WEB_URLS);
			this.taskContent.setTextColor(this.context.getResources().getColor(
					MirakelCommonPreferences.isDark() ? android.R.color.white
							: android.R.color.black));
			this.taskContentEdit.setText(this.task.getContent());
			Linkify.addLinks(this.taskContentEdit, Linkify.WEB_URLS);
		} else {
			this.taskContent.setText(R.string.add_content);
			this.taskContent.setTextColor(this.context.getResources().getColor(
					inactive_color));
			this.taskContentEdit.setText("");
		}
	}

	public void setOnEditChanged(OnEditChanged l) {
		this.editChanged = l;
	}

	@Override
	protected void updateView() {
		if (this.task.getContent() == null && this.content == null
				|| this.task.getContent() != null
				&& this.task.getContent().equals(this.content)) return;
		this.content = this.task.getContent();
		this.editContent.setBackgroundResource(android.R.drawable.ic_menu_edit);
		if (this.task.getContent().length() > 0) {
			this.taskContent.setText(this.task.getContent());
			Linkify.addLinks(this.taskContent, Linkify.WEB_URLS);
			this.taskContent.setTextColor(this.context.getResources().getColor(
					MirakelCommonPreferences.isDark() ? android.R.color.white
							: android.R.color.black));
			this.taskContentEdit.setText(this.task.getContent());
			Linkify.addLinks(this.taskContentEdit, Linkify.WEB_URLS);
		} else {
			this.taskContent.setText(R.string.add_content);
			this.taskContent.setTextColor(this.context.getResources().getColor(
					inactive_color));
			this.taskContentEdit.setText("");
		}
	}

}
