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

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import de.azapps.mirakel.custom_views.BaseTaskDetailRow.OnTaskChangedListner;
import de.azapps.mirakel.custom_views.TaskDetailDueReminder.Type;
import de.azapps.mirakel.custom_views.TaskDetailFilePart.OnFileClickListener;
import de.azapps.mirakel.custom_views.TaskDetailFilePart.OnFileMarkedListener;
import de.azapps.mirakel.custom_views.TaskDetailHeader.OnDoneChangedListner;
import de.azapps.mirakel.custom_views.TaskDetailTagView.NeedFragmentManager;
import de.azapps.mirakel.custom_views.TaskSummary.OnTaskClickListener;
import de.azapps.mirakel.custom_views.TaskSummary.OnTaskMarkedListener;
import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.helper.MirakelViewPreferences;
import de.azapps.mirakel.model.task.Task;

public class TaskDetailView extends BaseTaskDetailRow implements
    OnTaskChangedListner {

    public static class TYPE {
        public static class NoSuchItemException extends Exception {
            private static final long serialVersionUID = 4952441280983309615L;

            public NoSuchItemException() {
                super();
            }
        }

        public final static int CONTENT = 4;
        public final static int DUE = 2;
        public final static int FILE = 1;
        public final static int HEADER = 0;
        public final static int PROGRESS = 7;
        public final static int REMINDER = 3;
        public final static int SUBTASK = 6;
        public final static int TAGS = 8;

        public final static int SUBTITLE = 5;

        public static String getName(final int item) throws NoSuchItemException {
            switch (item) {
            case HEADER:
                return "header";
            case FILE:
                return "file";
            case DUE:
                return "due";
            case REMINDER:
                return "reminder";
            case CONTENT:
                return "content";
            case SUBTASK:
                return "subtask";
            case PROGRESS:
                return "progress";
            case TAGS:
                return "tags";
            default:
                throw new NoSuchItemException(); // Throw exception;
            }
        }

        public static String getTranslatedName(final Context ctx, final int item)
        throws NoSuchItemException {
            switch (item) {
            case HEADER:
                return ctx.getString(R.string.task_fragment_header);
            case FILE:
                return ctx.getString(R.string.task_fragment_file);
            case DUE:
                return ctx.getString(R.string.task_fragment_due);
            case REMINDER:
                return ctx.getString(R.string.task_fragment_reminder);
            case CONTENT:
                return ctx.getString(R.string.task_fragment_content);
            case SUBTASK:
                return ctx.getString(R.string.task_fragment_subtask);
            case PROGRESS:
                return ctx.getString(R.string.task_fragment_progress);
            case TAGS:
                return ctx.getString(R.string.task_fragment_tags);
            default:
                throw new NoSuchItemException(); // Throw exception;
            }
        }
    }

    private final Context context;
    private List<Integer> items;
    protected final SparseArray<BaseTaskDetailRow> views;

    public TaskDetailView(final Context ctx) {
        super(ctx);
        this.context = ctx;
        this.items = MirakelViewPreferences.getTaskFragmentLayout();
        this.views = new SparseArray<BaseTaskDetailRow>();
        setupView();
    }

    public TaskDetailView(final Context ctx, final AttributeSet a) {
        super(ctx, a);
        this.context = ctx;
        this.items = MirakelViewPreferences.getTaskFragmentLayout();
        this.views = new SparseArray<BaseTaskDetailRow>();
        setupView();
    }

    public TaskDetailView(final Context ctx, final AttributeSet attrs,
                          final int defStyle) {
        super(ctx, attrs, defStyle);
        this.context = ctx;
        this.items = MirakelViewPreferences.getTaskFragmentLayout();
        this.views = new SparseArray<BaseTaskDetailRow>();
        setupView();
    }

    public void cancelContent() {
        if (this.views == null) {
            return;
        }
        final BaseTaskDetailRow v = this.views.get(TYPE.CONTENT);
        if (v != null) {
            ((TaskDetailContent) v).cancelContent();
        }
    }

    @Override
    public void onTaskChanged(final Task newTask) {
        if (this.taskChangedListner != null) {
            this.taskChangedListner.onTaskChanged(newTask);
        }
        if (this.task.isDone() != newTask.isDone()) {
            updateView();
        }
        this.task = newTask;
    }

    public void saveContent() {
        final BaseTaskDetailRow v = this.views.get(TYPE.CONTENT);
        if (v != null) {
            ((TaskDetailContent) v).saveContent();
        }
    }

    public void setAudioButtonClick(final OnClickListener l) {
        final BaseTaskDetailRow v = this.views.get(TYPE.FILE);
        if (v != null) {
            ((TaskDetailFile) v).setAudioClick(l);
        }
    }

    public void setCameraButtonClick(final OnClickListener l) {
        final BaseTaskDetailRow v = this.views.get(TYPE.FILE);
        if (v != null) {
            ((TaskDetailFile) v).setCameraClick(l);
        }
    }

    public void setOnFileClicked(final OnFileClickListener l) {
        final BaseTaskDetailRow v = this.views.get(TYPE.FILE);
        if (v != null) {
            ((TaskDetailFile) v).setOnFileClicked(l);
        }
    }

    public void setOnFileMarked(final OnFileMarkedListener l) {
        final BaseTaskDetailRow v = this.views.get(TYPE.FILE);
        if (v != null) {
            ((TaskDetailFile) v).setOnFileMarked(l);
        }
    }

    public void setOnSubtaskClick(final OnTaskClickListener l) {
        final BaseTaskDetailRow v = this.views.get(TYPE.SUBTASK);
        if (v != null) {
            ((TaskDetailSubtask) v).setOnClick(l);
        }
    }

    public void setOnSubtaskMarked(final OnTaskMarkedListener l) {
        final BaseTaskDetailRow v = this.views.get(TYPE.SUBTASK);
        if (v != null) {
            ((TaskDetailSubtask) v).setOnTaskMarked(l);
        }
    }

    private void setupView() {
        removeAllViews();
        setOrientation(VERTICAL);
        for (int position = 0; position < this.items.size(); position++) {
            final int i = this.items.get(position);
            BaseTaskDetailRow item;
            switch (i) {
            case TYPE.HEADER:
                final TaskDetailHeader h = new TaskDetailHeader(this.context);
                h.setOnDoneChangedListner(new OnDoneChangedListner() {
                    @Override
                    public void onDoneChanged(final Task newTask) {
                        if (task != null && newTask.getId() != TaskDetailView.this.task.getId()) {
                            update(newTask);
                        } else if (TaskDetailView.this.views.get(TYPE.DUE) != null) {
                            TaskDetailView.this.views.get(TYPE.DUE).update(
                                newTask);
                        }
                    }
                });
                item = h;
                break;
            case TYPE.SUBTASK:
                item = new TaskDetailSubtask(this.context);
                break;
            case TYPE.PROGRESS:
                item = new TaskDetailProgress(this.context);
                break;
            case TYPE.CONTENT:
                item = new TaskDetailContent(this.context);
                break;
            case TYPE.REMINDER:
                if (position > 0 && this.items.get(position - 1) != TYPE.DUE
                    && position < this.items.size() - 1
                    && this.items.get(position + 1) != TYPE.DUE
                    || position == 0 && this.items.size() > 1
                    && this.items.get(1) != TYPE.DUE
                    || position == this.items.size() - 1 && position != 0
                    && this.items.get(this.items.size() - 2) != TYPE.DUE) {
                    final TaskDetailDueReminder t = new TaskDetailDueReminder(
                        this.context);
                    t.setType(Type.REMINDER);
                    item = t;
                    break;
                }
                continue;
            case TYPE.DUE:
                final TaskDetailDueReminder t = new TaskDetailDueReminder(
                    this.context);
                if (position > 1
                    && this.items.get(position - 1) == TYPE.REMINDER
                    || position < this.items.size() - 1
                    && this.items.get(position + 1) == TYPE.REMINDER) {
                    t.setType(Type.COMBINED);
                } else {
                    t.setType(Type.DUE);
                }
                item = t;
                break;
            case TYPE.FILE:
                item = new TaskDetailFile(this.context);
                break;
            case TYPE.TAGS:
                item = new TaskDetailTagView(this.context);
                break;
            //$FALL-THROUGH$
            case TYPE.SUBTITLE:
            default:
                // noting
                continue;
            }
            item.setOnTaskChangedListner(this);
            addView(item);
            this.views.put(i, item);
        }
    }

    public void setFragmentManager(final NeedFragmentManager fm) {
        final BaseTaskDetailRow v = this.views.get(TYPE.TAGS);
        if (v != null) {
            ((TaskDetailTagView) v).setNeedFragmentManager(fm);
        }
    }

    public void unmark(final boolean saveContent) {
        // Handle Subtasks
        BaseTaskDetailRow v = this.views.get(TYPE.SUBTASK);
        if (v != null) {
            v.update(this.task);
            ((TaskDetailSubtask) v).disableMarked();
        }
        // Handle Files
        v = this.views.get(TYPE.FILE);
        if (v != null) {
            v.update(this.task);
            ((TaskDetailFile) v).disableMarked();
        }
        // Handle Content
    }

    public void updateLayout() {
        this.items = MirakelViewPreferences.getTaskFragmentLayout();
        setupView();
    }

    @Override
    protected void updateView() {
        int key = 0;
        for (int i = 0; i < this.views.size(); i++) {
            key = this.views.keyAt(i);
            this.views.get(key).update(this.task);
        }
    }

}
