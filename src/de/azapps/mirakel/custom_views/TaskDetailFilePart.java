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

import java.io.FileNotFoundException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Optional;

import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

public class TaskDetailFilePart extends TaskDetailSubListBase<FileMirakel> {

    public interface OnFileClickListener {
        abstract public void clickOnFile(final FileMirakel f);
    }

    public interface OnFileMarkedListener {
        abstract public void markFile(final View v, final FileMirakel e,
                                      final boolean marked);
    }

    private static final String TAG = "TaskDetailFilePart";

    protected OnFileClickListener clickListner;
    private FileMirakel file;

    private final ImageView fileImage;
    private final TextView fileName;
    private final TextView filePath;
    private boolean marked;
    private OnFileMarkedListener markedListner;

    public TaskDetailFilePart(final Context context) {
        super(context);
        inflate(context, R.layout.files_row, this);
        this.fileImage = (ImageView) findViewById(R.id.file_image);
        this.fileName = (TextView) findViewById(R.id.file_name);
        this.filePath = (TextView) findViewById(R.id.file_path);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (TaskDetailFilePart.this.markedEnabled) {
                    handleMark();
                } else if (TaskDetailFilePart.this.clickListner != null) {
                    TaskDetailFilePart.this.clickListner
                    .clickOnFile(TaskDetailFilePart.this.file);
                }
            }
        });
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                handleMark();
                return true;
            }
        });
    }

    private void handleMark() {
        if (this.markedListner != null) {
            TaskDetailFilePart.this.marked = !TaskDetailFilePart.this.marked;
            this.markedListner.markFile(this, TaskDetailFilePart.this.file,
                                        TaskDetailFilePart.this.marked);
        }
    }

    public void setOnFileClickListner(final OnFileClickListener l) {
        this.clickListner = l;
    }

    public void setOnFileMarked(final OnFileMarkedListener l) {
        this.markedListner = l;
    }

    public void setShortMark(final boolean shortMark) {
        this.markedEnabled = shortMark;
    }

    @Override
    public void updatePart(final FileMirakel f) {
        setBackgroundColor(this.context.getResources().getColor(
                               android.R.color.transparent));
        Log.d(TAG, "update");
        this.file = f;
        new Thread(new Runnable() {
            private Optional<Bitmap> preview = absent();
            @Override
            public void run() {
                if (FileUtils.isAudio(TaskDetailFilePart.this.file.getFileUri())) {
                    final int resourceId = MirakelCommonPreferences.isDark() ? R.drawable.ic_action_play_dark
                                           : R.drawable.ic_action_play;
                    this.preview = fromNullable(BitmapFactory.decodeResource(
                                                    TaskDetailFilePart.this.context.getResources(),
                                                    resourceId));
                } else {
                    this.preview = TaskDetailFilePart.this.file.getPreview();
                }
                if (this.preview.isPresent()) {
                    Log.i(TAG, "preview not null");
                    TaskDetailFilePart.this.fileImage.post(new Runnable() {
                        @Override
                        public void run() {
                            TaskDetailFilePart.this.fileImage
                            .setImageBitmap(preview.get());
                            final LayoutParams params = (LayoutParams) TaskDetailFilePart.this.fileImage
                                                        .getLayoutParams();
                            params.height = preview.get().getHeight();
                            TaskDetailFilePart.this.fileImage
                            .setLayoutParams(params);
                        }
                    });
                } else {
                    Log.i(TAG, "preview null");
                    TaskDetailFilePart.this.fileImage.post(new Runnable() {
                        @Override
                        public void run() {
                            final LayoutParams params = (LayoutParams) TaskDetailFilePart.this.fileImage
                                                        .getLayoutParams();
                            params.height = 0;
                            TaskDetailFilePart.this.fileImage
                            .setLayoutParams(params);
                        }
                    });
                }
            }
        }).start();
        if (FileUtils.isAudio(this.file.getFileUri())) {
            this.fileName.setText(R.string.audio_record_file);
        } else if (FileUtils.isImage(this.file.getFileUri())) {
            this.fileName.setText(R.string.image_file);
        } else {
            this.fileName.setText(this.file.getName());
        }
        try {
            this.file.getFileStream(this.context);
            final String name = FileUtils.getNameFromUri(this.context,
                                this.file.getFileUri());
            this.filePath.setText(name.length() == 0 ? this.file.getFileUri()
                                  .toString() : name);
        } catch (final FileNotFoundException e) {
            this.filePath.setText(R.string.error_FILE_NOT_FOUND);
        }
    }
}
