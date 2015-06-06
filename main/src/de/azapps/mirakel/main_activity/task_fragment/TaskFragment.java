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
package de.azapps.mirakel.main_activity.task_fragment;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LongSparseArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.DefinitionsModel.ExecInterfaceWithTask;
import de.azapps.mirakel.custom_views.BaseTaskDetailRow.OnTaskChangedListner;
import de.azapps.mirakel.custom_views.TaskDetailFilePart.OnFileClickListener;
import de.azapps.mirakel.custom_views.TaskDetailFilePart.OnFileMarkedListener;
import de.azapps.mirakel.custom_views.TaskDetailTagView.NeedFragmentManager;
import de.azapps.mirakel.custom_views.TaskDetailView;
import de.azapps.mirakel.custom_views.TaskSummary.OnTaskClickListener;
import de.azapps.mirakel.custom_views.TaskSummary.OnTaskMarkedListener;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.main_activity.R;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class TaskFragment extends Fragment {

    protected enum ActionbarState {
        CONTENT, FILE, SUBTASK;
    }

    public TaskFragment () {
        super ();
    }

    private static final String TAG = "TaskActivity";

    protected ActionbarState cabState;
    protected TaskDetailView detailView;

    protected MainActivity main;
    protected LongSparseArray<FileMirakel> markedFiles;

    protected LongSparseArray<Task> markedSubtasks;

    protected Menu mMenu;

    protected Task task;

    private Runnable updateThread;

    private boolean saveContent = true;

    private ActionMode mActionMode;
    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback () {
        // Called when the user selects a
        // contextual menu item
        @Override
        public boolean onActionItemClicked (final ActionMode mode,
                                            final MenuItem item) {
            final boolean b = handleActionBarClick (item);
            if (!b) {
                mode.finish ();
            }
            return b;
        }
        // Called when the action mode is
        // created; startActionMode() was
        // called
        @Override
        public boolean onCreateActionMode (final ActionMode mode, final Menu menu) {
            // Inflate a menu resource
            // providing context menu items
            final MenuInflater inflater = mode.getMenuInflater ();
            final boolean b = handleCabCreateMenu (inflater, menu);
            if (b) {
                TaskFragment.this.mActionMode = mode;
                TaskFragment.this.mMenu = menu;
            }
            return b;
        }
        // Called when the user exits the
        // action mode
        @Override
        public void onDestroyActionMode (final ActionMode mode) {
            handleCloseCab ();
            TaskFragment.this.mActionMode = null;
        }
        // Called each time the action mode
        // is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if
        // the mode is invalidated.
        @Override
        public boolean onPrepareActionMode (final ActionMode mode,
                                            final Menu menu) {
            return false; // Return false if
            // nothing is
            // done
        }
    };

    //abstract protected void changeVisiblity (final boolean visible,
    //        final MenuItem item);

    //abstract public void closeActionMode ();

    protected boolean handleActionBarClick (final MenuItem item) {
        this.saveContent = true;
        int i1 = item.getItemId();
        if (i1 == R.id.save) {
            if (TaskFragment.this.detailView != null) {
                TaskFragment.this.detailView.saveContent();
            }

        } else if (i1 == R.id.cancel) {
            Log.v(TAG, "cancel");
            if (TaskFragment.this.detailView != null) {
                this.saveContent = false;
                TaskFragment.this.detailView.cancelContent();
            }

        } else if (i1 == R.id.menu_delete) {
            if (TaskFragment.this.cabState == ActionbarState.FILE) {
                final List<FileMirakel> selectedItems = new ArrayList<>();
                for (int i = 0; i < this.markedFiles.size(); i++) {
                    selectedItems.add(this.markedFiles.valueAt(i));
                }
                TaskDialogHelpers.handleDeleteFile(selectedItems,
                                                   getActivity(), TaskFragment.this.task,
                new ExecInterfaceWithTask() {
                    @Override
                    public void exec(final Task task) {
                        update(task);
                    }
                });
            } else {// Subtask
                final List<Task> selectedItems = new ArrayList<>();
                for (int i = 0; i < this.markedSubtasks.size(); i++) {
                    selectedItems.add(this.markedSubtasks.valueAt(i));
                }
                TaskDialogHelpers.handleRemoveSubtask(selectedItems,
                                                      getActivity(), TaskFragment.this.task,
                new ExecInterfaceWithTask() {
                    @Override
                    public void exec(final Task task) {
                        update(task);
                    }
                });
            }

        } else if (i1 == R.id.edit_task) {
            if (TaskFragment.this.main != null) {
                TaskFragment.this.main
                .setCurrentTask(TaskFragment.this.markedSubtasks
                                .valueAt(0));
            }

        } else if (i1 == R.id.done_task) {
            for (int i = 0; i < this.markedSubtasks.size(); i++) {
                final Task t = this.markedSubtasks.valueAt(i);
                if (t != null) {
                    t.setDone(true);
                    t.save();
                }
            }
            update(TaskFragment.this.task);
            TaskFragment.this.main.getTasksFragment().updateList();
            TaskFragment.this.main.getListFragment().update();

        } else {
            return false;
        }
        Log.i (TaskFragment.TAG, "item clicked");
        closeActionMode();
        return true;
    }

    public void closeActionMode() {
        if (this.mActionMode != null) {
            this.mActionMode.finish ();
        }
    }

    protected boolean handleCabCreateMenu (final MenuInflater inflater,
                                           final Menu menu) {
        if (TaskFragment.this.cabState == null) {
            return false;
        }
        switch (TaskFragment.this.cabState) {
        case CONTENT:
            inflater.inflate (R.menu.save, menu);
            break;
        case FILE:
            inflater.inflate (R.menu.context_file, menu);
            break;
        case SUBTASK:
            inflater.inflate (R.menu.context_subtask, menu);
            break;
        default:
            Log.d (TaskFragment.TAG, "where are the dragons");
            return false;
        }
        return true;
    }

    protected void handleCloseCab () {
        TaskFragment.this.cabState = null;
        if (TaskFragment.this.detailView != null) {
            TaskFragment.this.detailView.unmark (this.saveContent);
        }
        TaskFragment.this.markedFiles = new LongSparseArray<>();
        TaskFragment.this.markedSubtasks = new LongSparseArray<>();
        Log.d(TaskFragment.TAG, "kill mode");
    }

    @Override
    public View onCreateView (final LayoutInflater inflater,
                              final ViewGroup container, final Bundle savedInstanceState) {
        this.cabState = null;
        this.markedFiles = new LongSparseArray<>();
        this.markedSubtasks = new LongSparseArray<>();
        this.main = (MainActivity) getActivity();
        View view;
        this.updateThread = new Runnable () {
            @Override
            public void run () {
                TaskFragment.this.detailView.update (TaskFragment.this.task);
            }
        };
        try {
            view = inflater.inflate (R.layout.task_fragment, container, false);
        } catch (final Exception e) {
            Log.w(TaskFragment.TAG, "Failed do inflate layout", e);
            return null;
        }
        this.detailView = (TaskDetailView) view
                          .findViewById (R.id.task_fragment_view);
        this.detailView.setOnTaskChangedListner (new OnTaskChangedListner () {
            @Override
            public void onTaskChanged (final Task newTask) {
                if (TaskFragment.this.main.getTasksFragment () != null
                    && TaskFragment.this.main.getListFragment () != null) {
                    TaskFragment.this.main.getTasksFragment ().updateList ();
                    TaskFragment.this.main.getListFragment ().update ();
                }
            }
        });
        this.detailView.setOnSubtaskClick(new OnTaskClickListener() {
            @Override
            public void onTaskClick (final Task t) {
                TaskFragment.this.main.setCurrentTask (t);
            }
        });
        this.detailView.setFragmentManager (new NeedFragmentManager () {
            @Override
            public FragmentManager getFragmentManager () {
                return TaskFragment.this.main.getSupportFragmentManager ();
            }
        });
        if (MirakelCommonPreferences.useBtnCamera ()
            && Helpers.isIntentAvailable (this.main,
                                          MediaStore.ACTION_IMAGE_CAPTURE)) {
            this.detailView.setAudioButtonClick (new View.OnClickListener () {
                @Override
                public void onClick (final View v) {
                    TaskDialogHelpers.handleAudioRecord (getActivity (),
                                                         TaskFragment.this.task,
                    new ExecInterfaceWithTask () {
                        @Override
                        public void exec (final Task t) {
                            update (t);
                        }
                    });
                }
            });
            this.detailView.setCameraButtonClick (new View.OnClickListener () {
                @Override
                public void onClick (final View v) {
                    try {
                        final Intent cameraIntent = new Intent (
                            MediaStore.ACTION_IMAGE_CAPTURE);
                        final Uri fileUri = FileUtils
                                            .getOutputMediaFileUri (FileUtils.MEDIA_TYPE_IMAGE);
                        if (fileUri == null) {
                            return;
                        }
                        TaskFragment.this.main.setFileUri (fileUri);
                        cameraIntent.putExtra (MediaStore.EXTRA_OUTPUT, fileUri);
                        getActivity ().startActivityForResult (cameraIntent,
                                                               MainActivity.RESULT_ADD_PICTURE);
                    } catch (final ActivityNotFoundException a) {
                        ErrorReporter.report (ErrorType.PHOTO_NO_CAMERA);
                    } catch (final IOException e) {
                        if (e.getMessage ().equals (FileUtils.ERROR_NO_MEDIA_DIR)) {
                            ErrorReporter
                            .report (ErrorType.PHOTO_NO_MEDIA_DIRECTORY);
                        }
                    }
                }
            });
        }
        if (this.task != null) {
            update (this.task);
        }
        this.detailView.setOnSubtaskMarked (new OnTaskMarkedListener() {
            @Override
            public void markTask (final View v, final Task t,
                                  final boolean marked) {
                if (t == null || TaskFragment.this.cabState != null
                    && TaskFragment.this.cabState != ActionbarState.SUBTASK) {
                    return;
                }
                if (marked) {
                    TaskFragment.this.cabState = ActionbarState.SUBTASK;
                    if (mActionMode == null) {
                        main.startActionMode (mActionModeCallback);
                    }
                    v.setBackgroundColor (Helpers
                                          .getHighlightedColor (getActivity ()));
                    TaskFragment.this.markedSubtasks.put (t.getId (), t);
                } else {
                    Log.d(TaskFragment.TAG, "not marked");
                    v.setBackgroundColor(getActivity().getResources().getColor(
                                             android.R.color.transparent));
                    TaskFragment.this.markedSubtasks.remove(t.getId());
                    if (TaskFragment.this.markedSubtasks.size() == 0 && mActionMode != null) {
                        mActionMode.finish ();
                    }
                }
                if (TaskFragment.this.mMenu != null) {
                    final MenuItem item = TaskFragment.this.mMenu
                                          .findItem (R.id.edit_task);
                    if (mActionMode != null && item != null) {
                        item.setVisible (TaskFragment.this.markedSubtasks.size () == 1);
                    }
                }
            }
        });
        this.detailView.setOnFileMarked (new OnFileMarkedListener() {
            @Override
            public void markFile (final View v, final FileMirakel e,
                                  final boolean marked) {
                if (e == null || TaskFragment.this.cabState != null
                    && TaskFragment.this.cabState != ActionbarState.FILE) {
                    return;
                }
                if (marked) {
                    TaskFragment.this.cabState = ActionbarState.FILE;
                    if (mActionMode == null) {
                        main.startActionMode (mActionModeCallback);
                    }
                    v.setBackgroundColor (Helpers
                                          .getHighlightedColor (getActivity ()));
                    TaskFragment.this.markedFiles.put (e.getId (), e);
                } else {
                    v.setBackgroundColor (getActivity ().getResources ().getColor (
                                              android.R.color.transparent));
                    TaskFragment.this.markedFiles.remove (e.getId ());
                    if (TaskFragment.this.markedFiles.size () == 0 && mActionMode != null) {
                        mActionMode.finish ();
                    }
                }
            }
        });
        this.detailView.setOnFileClicked (new OnFileClickListener() {
            @Override
            public void clickOnFile (final FileMirakel file) {
                final Context context = getActivity ();
                String[] items;
                if (FileUtils.isAudio(file.getFileUri())) {
                    items = context.getResources().getStringArray(
                                R.array.audio_playback_options);
                } else {
                    TaskDialogHelpers.openFile (context, file);
                    return;
                }
                new AlertDialog.Builder (context)
                .setTitle (R.string.audio_playback_select_title)
                .setItems (items, new DialogInterface.OnClickListener () {
                    @Override
                    public void onClick (final DialogInterface dialog,
                                         final int which) {
                        switch (which) {
                        case 0: // Open
                            TaskDialogHelpers.openFile (context, file);
                            break;
                        default: // playback
                            TaskDialogHelpers.playbackFile (
                                getActivity (), file, which == 1);
                            break;
                        }
                    }
                }).show ();
                return;
            }
        });
        this.detailView.update (this.main.getCurrentTask ());
        return view;
    }


    public void update (final Task t) {
        closeActionMode();
        this.task = t;
        if (this.detailView != null && this.updateThread != null) {
            new Thread (this.updateThread).start ();
        }
    }

    public Task getTask () {
        return this.task;
    }

    public void updateLayout () {
        if (this.detailView != null) {
            this.detailView.updateLayout ();
        }
    }

}
