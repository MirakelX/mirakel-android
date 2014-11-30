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

package de.azapps.mirakel.new_ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.OptionalUtils;

public class ProgressView extends LinearLayout {
    private int progress;

    @InjectView(R.id.progress_bar)
    SeekBar progressBar;

    public ProgressView(final Context context) {
        this(context, null);
    }

    public ProgressView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_progress, this);
        ButterKnife.inject(this, this);
    }

    public void setOnProgressChangeListener(final OptionalUtils.Procedure<Integer>
                                            onProgressChangeListener) {
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                // Do nothing
            }
            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
                // Do nothing
            }
            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                onProgressChangeListener.apply(seekBar.getProgress());
            }
        });
    }

    private void rebuildLayout() {
        progressBar.setProgress(progress);
        invalidate();
        requestLayout();
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(final int progress) {
        this.progress = progress;
        rebuildLayout();
    }
}
