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

import android.content.Context;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.model.task.Task;

public class TaskDetailProgressBar extends TaskDetailSubListBase<Integer> {

    private final SeekBar progress;

    public TaskDetailProgressBar(final Context ctx) {
        super(ctx);
        inflate(ctx, R.layout.task_progress, this);
        this.progress = (SeekBar) findViewById(R.id.task_progress_seekbar);
        this.progress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar,
                                          final int progressLocal, final boolean fromUser) {
                // nothing
            }
            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
                // nothing
            }
            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                if (TaskDetailProgressBar.this.task != null) {
                    TaskDetailProgressBar.this.task.setProgress(seekBar
                            .getProgress());
                    save();
                }
            }
        });
        this.progress.setMax(100);
    }

    public void setTask(final Task t) {
        this.task = t;
    }

    @Override
    public void updatePart(final Integer newValue) {
        this.progress.setProgress(newValue);
    }

}
