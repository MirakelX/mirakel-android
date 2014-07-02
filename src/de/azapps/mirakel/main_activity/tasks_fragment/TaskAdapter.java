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
package de.azapps.mirakel.main_activity.tasks_fragment;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import de.azapps.mirakel.custom_views.BaseTaskDetailRow.OnTaskChangedListner;
import de.azapps.mirakel.custom_views.TaskSummary;
import de.azapps.mirakel.model.task.Task;

public class TaskAdapter extends CursorAdapter {

    private final OnTaskChangedListner taskChange;
    protected int touchedPosition;

    public TaskAdapter (final Context context,
                        final OnTaskChangedListner taskChange) {
        super (context, null, false);
        this.taskChange = taskChange;
    }

    // update view here
    @Override
    public void bindView (View v, final Context ctx, final Cursor c) {
        if (v == null || ! (v instanceof TaskSummary)) {
            v = new TaskSummary (ctx);
        }
        final Task t = Task.cursorToTask (c);
        ((TaskSummary) v).updatePart (t);
        v.setTag (t.getId ());
    }

    @Override
    public View getView (final int position, final View convertView,
                         final ViewGroup parent) {
        final View v = super.getView (position, convertView, parent);
        v.setOnTouchListener (new OnTouchListener () {
            @Override
            public boolean onTouch (final View v, final MotionEvent event) {
                TaskAdapter.this.touchedPosition = position;
                return false;
            }
        });
        return v;
    }

    public int getLastTouched () {
        return this.touchedPosition;
    }

    // create new views
    @Override
    public View newView (final Context ctx, final Cursor c,
                         final ViewGroup parent) {
        final TaskSummary summary = new TaskSummary (ctx);
        summary.setOnTaskChangedListner (this.taskChange);
        return summary;
    }

}
