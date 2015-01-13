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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import de.azapps.mirakel.model.task.Task;

public abstract class BaseTaskDetailRow extends LinearLayout {

    public interface OnTaskChangedListner {
        abstract void onTaskChanged(final Task newTask);
    }

    protected static final Integer inactive_color = android.R.color.darker_gray;
    protected Context context;
    protected Task task;
    protected OnTaskChangedListner taskChangedListner;

    public BaseTaskDetailRow(final Context ctx) {
        super(ctx);
        this.context = ctx;
    }

    public BaseTaskDetailRow(final Context ctx, final AttributeSet a) {
        super(ctx, a);
        this.context = ctx;
    }

    @SuppressLint("NewApi")
    public BaseTaskDetailRow(final Context ctx, final AttributeSet attrs,
                             final int defStyle) {
        super(ctx, attrs, defStyle);
        this.context = ctx;
    }

    protected void save() {
        if (this.task != null) {
            this.task.save();
            if (this.taskChangedListner != null) {
                this.taskChangedListner.onTaskChanged(this.task);
            }
        }
    }

    public void setOnTaskChangedListner(final OnTaskChangedListner l) {
        this.taskChangedListner = l;
    }

    public void update(final Task t) {
        this.task = t;
        if (t != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    updateView();
                }
            });
        }
    }

    abstract protected void updateView();

}
