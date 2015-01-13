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
package de.azapps.mirakel.custom_views;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.custom_views.TaskDetailFilePart.OnFileClickListener;
import de.azapps.mirakel.custom_views.TaskDetailFilePart.OnFileMarkedListener;
import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.file.FileMirakel;

public class TaskDetailFile extends
    TaskDetailSubtitleView<FileMirakel, TaskDetailFilePart> implements
    OnFileMarkedListener, OnFileClickListener {
    private int markCounter;
    private OnFileClickListener onFileClicked;
    private OnFileMarkedListener onFileMarked;

    public TaskDetailFile(final Context ctx) {
        super(ctx);
        this.markCounter = 0;
        this.title.setText(R.string.add_files);
        this.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                Helpers.showFileChooser(DefinitionsHelper.RESULT_ADD_FILE,
                                        TaskDetailFile.this.context
                                        .getString(R.string.file_select),
                                        (Activity) TaskDetailFile.this.context);
            }
        });
        this.button.setImageDrawable(this.context.getResources().getDrawable(
                                         android.R.drawable.ic_menu_add));
    }

    @Override
    public void clickOnFile(final FileMirakel f) {
        if (this.onFileClicked != null) {
            this.onFileClicked.clickOnFile(f);
        }
    }

    private void markFile(final boolean markted) {
        this.markCounter += markted ? 1 : -1;
        for (final TaskDetailFilePart v : this.viewList) {
            v.setShortMark(this.markCounter > 0);
        }
    }

    @Override
    public void markFile(final View v, final FileMirakel e,
                         final boolean markted) {
        if (this.onFileMarked != null) {
            markFile(markted);
            this.onFileMarked.markFile(v, e, markted);
        }
    }

    @Override
    TaskDetailFilePart newElement() {
        final TaskDetailFilePart t = new TaskDetailFilePart(this.context);
        t.setOnFileMarked(this);
        t.setOnFileClickListner(this);
        return t;
    }

    public void setAudioClick(final OnClickListener onClick) {
        if (this.audioButton != null) {
            this.audioButton.setOnClickListener(onClick);
        }
    }

    public void setCameraClick(final OnClickListener onClick) {
        if (this.cameraButton != null) {
            this.cameraButton.setOnClickListener(onClick);
        }
    }

    public void setOnFileClicked(final OnFileClickListener l) {
        this.onFileClicked = l;
    }

    public void setOnFileMarked(final OnFileMarkedListener l) {
        this.onFileMarked = l;
    }

    @Override
    protected void updateView() {
        updateSubviews(this.task.getFiles());
    }

}
