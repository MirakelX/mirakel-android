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

package de.azapps.mirakel.new_ui.activities;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.common.base.Optional;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.listeners.EventListener;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.material_elements.utils.AnimationHelper;
import de.azapps.material_elements.utils.MenuHelper;
import de.azapps.material_elements.views.FloatingActionButton;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.adapter.SimpleModelListAdapter;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.ListDialogHelpers;
import de.azapps.mirakel.helper.SharingHelper;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.ListMirakelInterface;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.model.task.TaskOverview;
import de.azapps.mirakel.new_ui.fragments.ListsFragment;
import de.azapps.mirakel.new_ui.fragments.TaskFragment;
import de.azapps.mirakel.new_ui.fragments.TasksFragment;
import de.azapps.mirakel.new_ui.views.SearchView;
import de.azapps.mirakel.settings.SettingsActivity;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static de.azapps.tools.OptionalUtils.Procedure;
import static de.azapps.tools.OptionalUtils.withOptional;

public class MirakelActivity extends AppCompatActivity implements OnItemClickedListener<ModelBase>,
    LockableDrawer {

    private static final String TAG = "MirakelActivity";
    private Optional<DrawerLayout> mDrawerLayout = absent();
    private Optional<ActionBarDrawerToggle> mDrawerToggle = absent();
    private TaskFragment newFragment;



    class ActionBarViewHolder {
        @butterknife.Optional
        @InjectView(R.id.actionbar_switcher)
        ViewFlipper actionbarSwitcher;
        @InjectView(R.id.actionbar_spinner)
        @NonNull
        Spinner actionbarSpinner;
        @butterknife.Optional
        @InjectView(R.id.actionbar_title)
        @Nullable
        TextView actionbarTitle;
        @InjectView(R.id.search_view)
        SearchView searchView;

        private ActionBarViewHolder(final View v) {
            ButterKnife.inject(this, v);
        }
    }

    private ActionBarViewHolder actionBarViewHolder;

    @NonNull
    @InjectView(R.id.actionbar)
    Toolbar actionbar;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getter / Setter
    private TasksFragment getTasksFragment() {
        return (TasksFragment) getSupportFragmentManager().findFragmentById(R.id.tasks_fragment);
    }

    private ListsFragment getListsFragment() {
        return (ListsFragment) getSupportFragmentManager().findFragmentById(R.id.lists_fragment);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Override functions

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirakel);
        ButterKnife.inject(this);
        initDrawer();
        handleIntent(getIntent());
        if ((getTasksFragment() != null) && (getTasksFragment().getList() != null)) {
            setSupportActionBar(actionbar);
            setupActionbar();
        }
    }


    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        withOptional(mDrawerToggle, new Procedure<ActionBarDrawerToggle>() {
            @Override
            public void apply(final ActionBarDrawerToggle input) {
                input.syncState();
            }
        });
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        withOptional(mDrawerToggle, new Procedure<ActionBarDrawerToggle>() {
            @Override
            public void apply(final ActionBarDrawerToggle input) {
                input.onConfigurationChanged(newConfig);
            }
        });
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        if (mDrawerLayout.isPresent()) {
            // For phones
            final boolean drawerOpen = mDrawerLayout.get().isDrawerOpen(Gravity.START);
            if (drawerOpen) {
                getMenuInflater().inflate(R.menu.lists_menu, menu);
            } else {
                getMenuInflater().inflate(R.menu.tasks_menu, menu);
            }
        } else {
            getMenuInflater().inflate(R.menu.tablet_menu, menu);
        }

        MenuHelper.showMenuIcons(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (mDrawerToggle.isPresent()) {
            // Phone
            if (mDrawerToggle.get().onOptionsItemSelected(item)) {
                return true;
            }
        }
        final int id = item.getItemId();
        if (id == R.id.action_settings) {
            final Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_create_list) {
            getListsFragment().editList(ListMirakel.getStub());
            return true;
        } else if (id == R.id.menu_share) {
            SharingHelper.share(this, getTasksFragment().getList());
        } else if (id == R.id.menu_search) {
            getTasksFragment().handleShowSearch();
        } else if (id == R.id.menu_sort) {
            ListMirakelInterface listMirakelInterface = getTasksFragment().getList();
            if (listMirakelInterface instanceof ListMirakel) {
                ListDialogHelpers.handleSortBy(this, (ListMirakel) listMirakelInterface,
                new Helpers.ExecInterface() {
                    @Override
                    public void exec() {
                        getTasksFragment().resetList();
                    }
                }, null);
            } else {
                throw new IllegalArgumentException("It's not a proper List");
            }
        }
        return super.onOptionsItemSelected(item);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Other functions

    private void setList(final ListMirakel listMirakel) {
        final FragmentManager fragmentManager = getFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        final TasksFragment tasksFragment = getTasksFragment();
        tasksFragment.setList(listMirakel);
        fragmentTransaction.commit();
    }

    private void handleIntent(final Intent intent) {
        if (intent == null) {
            return;
        }
        switch (intent.getAction()) {
        case DefinitionsHelper.SHOW_TASK:
        case DefinitionsHelper.SHOW_TASK_FROM_WIDGET:
        case DefinitionsHelper.SHOW_TASK_REMINDER:
            final Optional<Task> task = TaskHelper.getTaskFromIntent(intent);
            if (task.isPresent()) {
                setList(task.get().getList());
                selectTask(task.get());
            }
            break;
        case Intent.ACTION_SEND:
        case Intent.ACTION_SEND_MULTIPLE:
            // TODO
            break;
        case DefinitionsHelper.SHOW_LIST:
        case DefinitionsHelper.SHOW_LIST_FROM_WIDGET:
            if (intent.hasExtra(DefinitionsHelper.EXTRA_LIST)) {
                setList((ListMirakel) intent.getParcelableExtra(DefinitionsHelper.EXTRA_LIST));
            } else {
                Log.d(TAG, "show_list does not pass list, so ignore this");
            }
            break;
        case Intent.ACTION_SEARCH:
            // TODO
            break;
        case DefinitionsHelper.ADD_TASK_FROM_WIDGET:
            if (intent.hasExtra(DefinitionsHelper.EXTRA_LIST)) {
                setList((ListMirakel) intent.getParcelableExtra(DefinitionsHelper.EXTRA_LIST));
                getTasksFragment().addTask();
            } else {
                Log.d(TAG, "show_list does not pass list, so ignore this");
            }
            break;
        case DefinitionsHelper.SHOW_MESSAGE:
            // TODO
            break;
        }
    }

    private void initDrawer() {
        // Nav drawer
        mDrawerLayout = fromNullable((DrawerLayout) findViewById(R.id.drawer_layout));
        withOptional(mDrawerLayout, new Procedure<DrawerLayout>() {
            @Override
            public void apply(final DrawerLayout mDrawerLayout) {
                final ActionBarDrawerToggle mDrawerToggle = new DrawerToggle(mDrawerLayout);
                mDrawerLayout.setDrawerListener(mDrawerToggle);
                mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
                MirakelActivity.this.mDrawerToggle = of(mDrawerToggle);
            }
        });
    }

    @Override
    public void lockDrawer() {
        withOptional(mDrawerLayout, new Procedure<DrawerLayout>() {
            @Override
            public void apply(final DrawerLayout drawerLayout) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
            }
        });
    }

    @Override
    public void unlockDrawer() {
        withOptional(mDrawerLayout, new Procedure<DrawerLayout>() {
            @Override
            public void apply(final DrawerLayout drawerLayout) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        });
    }

    public enum ActionBarState {
        NORMAL(0), SWITCHER(1);

        private final int position;
        ActionBarState(final int position) {
            this.position = position;
        }

        public int getPosition() {
            return position;
        }
    }

    public void updateToolbar(final ActionBarState actionBarState) {
        if (actionBarViewHolder != null) {
            actionBarViewHolder.actionbarSwitcher.setDisplayedChild(actionBarState.getPosition());
            if (actionBarState == ActionBarState.NORMAL && actionBarViewHolder.actionbarTitle != null) {
                actionBarViewHolder.actionbarTitle.setText(getTasksFragment().getList().getName(),
                        TextView.BufferType.SPANNABLE);
            }
        }
    }

    private void setupActionbar() {
        setTitle(null);
        final View actionbarLayout = LayoutInflater.from(this).inflate(R.layout.actionbar_layout,
                                     actionbar, false);
        actionBarViewHolder = new ActionBarViewHolder(actionbarLayout);
        actionbar.addView(actionbarLayout);


        final Cursor cursor = AccountMirakel.allCursorWithAllAccounts();
        final SimpleModelListAdapter<AccountMirakel> adapter = new SimpleModelListAdapter<>(this, cursor, 0,
                AccountMirakel.class, R.layout.simple_list_row_with_bold_header);
        actionBarViewHolder.actionbarSpinner.setAdapter(adapter);
        actionBarViewHolder.actionbarSpinner.setOnItemSelectedListener(new
        AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position,
                                       final long id) {

                final AccountMirakel accountMirakel = adapter.getItem(position);
                getListsFragment().setAccount(of(accountMirakel));
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // do nothing
            }
        });
        if (actionBarViewHolder.actionbarTitle != null) {
            actionBarViewHolder.actionbarTitle.setText(getTasksFragment().getList().getName(),
                    TextView.BufferType.SPANNABLE);
        }
    }

    @Override
    public void onItemSelected(final @NonNull ModelBase item) {
        if (item instanceof Task) {
            selectTask((Task) item);
        } else if (item instanceof TaskOverview) {
            final Optional<Task> taskOptional = ((TaskOverview) item).getTask();
            if (taskOptional.isPresent()) {
                selectTask(taskOptional.get());
            } else {
                ErrorReporter.report(ErrorType.TASK_VANISHED);
            }
        } else if (item instanceof ListMirakel) {
            selectList((ListMirakel) item);
        } else {
            throw new IllegalArgumentException("No handler for" + item.getClass().toString());
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if ((requestCode == TaskFragment.REQUEST_IMAGE_CAPTURE ||
             requestCode == TaskFragment.FILE_SELECT_CODE) && (resultCode == Activity.RESULT_OK)) {
            newFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void selectList(ListMirakel item) {
        setList(item);
        withOptional(mDrawerLayout, new Procedure<DrawerLayout>() {
            @Override
            public void apply(final DrawerLayout input) {
                input.closeDrawer(Gravity.START);
            }
        });
    }

    private void selectTask(final Task item) {
        newFragment = TaskFragment.newInstance((Task) item);
        newFragment.show(getSupportFragmentManager(), "dialog");
    }

    private boolean shouldShowSpinner() {
        return !mDrawerLayout.isPresent() || mDrawerLayout.get().isDrawerOpen(Gravity.START);
    }

    public void moveFABUp(final int height) {
        final FloatingActionButton fab = getTasksFragment().floatingActionButton;
        AnimationHelper.moveViewUp(this, fab, height);

    }
    public void moveFabDown(final int height) {
        final FloatingActionButton fab = getTasksFragment().floatingActionButton;
        AnimationHelper.moveViewDown(this, fab, height);
    }

    private class DrawerToggle extends ActionBarDrawerToggle {
        public DrawerToggle(DrawerLayout mDrawerLayout) {
            super(MirakelActivity.this, mDrawerLayout, MirakelActivity.this.actionbar, R.string.list_title,
                  R.string.list_title);
        }

        /**
         * Called when a drawer has settled in a completely closed state.
         */
        @Override
        public void onDrawerClosed(final View drawerView) {
            super.onDrawerClosed(drawerView);
            invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            getListsFragment().onCloseNavDrawer();
            updateToolbar(ActionBarState.NORMAL);
            invalidateOptionsMenu();
        }

        /**
         * Called when a drawer has settled in a completely open state.
         */
        @Override
        public void onDrawerOpened(final View drawerView) {
            super.onDrawerOpened(drawerView);
            invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            updateToolbar(ActionBarState.SWITCHER);
            invalidateOptionsMenu();
        }
    }
}
