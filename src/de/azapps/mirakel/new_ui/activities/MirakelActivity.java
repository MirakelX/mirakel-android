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

package de.azapps.mirakel.new_ui.activities;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.common.base.Optional;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.new_ui.fragments.ListsFragment;
import de.azapps.mirakel.new_ui.fragments.TaskFragment;
import de.azapps.mirakel.new_ui.fragments.TasksFragment;
import de.azapps.mirakel.new_ui.interfaces.OnListSelectedListener;
import de.azapps.mirakel.new_ui.interfaces.OnTaskSelectedListener;
import de.azapps.mirakel.settings.SettingsActivity;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static de.azapps.tools.OptionalUtils.Procedure;
import static de.azapps.tools.OptionalUtils.withOptional;

public class MirakelActivity extends ActionBarActivity implements OnTaskSelectedListener,
    OnListSelectedListener {

    private static final String TAG = "MirakelActivity";
    private Optional<DrawerLayout> mDrawerLayout = absent();
    private Optional<ActionBarDrawerToggle> mDrawerToggle = absent();

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirakel);
        Toolbar actionbar = (Toolbar) findViewById(R.id.actionbar);
        initDrawer();
        handleIntent(getIntent());
        if ((getTasksFragment() != null) && (getTasksFragment().getList() != null)) {
            actionbar.setTitle(getTasksFragment().getList().getName());
            setSupportActionBar(actionbar);
        }
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        withOptional(mDrawerToggle, new Procedure<ActionBarDrawerToggle>() {
            @Override
            public void apply(ActionBarDrawerToggle input) {
                input.syncState();
            }
        });
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        withOptional(mDrawerToggle, new Procedure<ActionBarDrawerToggle>() {
            @Override
            public void apply(ActionBarDrawerToggle input) {
                input.onConfigurationChanged(newConfig);
            }
        });
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        if (mDrawerLayout.isPresent()) {
            // For phones
            final boolean drawerOpen = mDrawerLayout.get().isDrawerOpen(Gravity.START);
            if (drawerOpen) {
                // TODO list menu
            } else {
                // TODO tasks menu
            }
        } else {
            // For Tablets
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mirakel, menu);
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
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            final Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_toggle_ui) {
            MirakelCommonPreferences.setUseNewUI(false);
            Helpers.restartApp(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTaskSelected(final Task task) {
        final DialogFragment newFragment = TaskFragment.newInstance(task);
        newFragment.show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onListSelected(final ListMirakel list) {
        setList(list);
        withOptional(mDrawerLayout, new Procedure<DrawerLayout>() {
            @Override
            public void apply(DrawerLayout input) {
                input.closeDrawer(Gravity.START);
            }
        });
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
                onTaskSelected(task.get());
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
            // TODO
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
            public void apply(DrawerLayout mDrawerLayout) {
                final ActionBar actionBar = MirakelActivity.this.getActionBar();
                final ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(MirakelActivity.this,
                        mDrawerLayout,
                        R.drawable.ic_drawer,
                        R.string.list_title, /* "open drawer" description */
                R.string.list_title /* "close drawer" description */) {
                    /** Called when a drawer has settled in a completely closed state. */
                    public void onDrawerClosed(final View drawerView) {
                        super.onDrawerClosed(drawerView);
                        if (actionBar != null) {
                            actionBar.setTitle(getTasksFragment().getList().getName());
                        }
                        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                    }
                    /** Called when a drawer has settled in a completely open state. */
                    public void onDrawerOpened(final View drawerView) {
                        super.onDrawerOpened(drawerView);
                        if (actionBar != null) {
                            actionBar.setTitle(R.string.list_title);
                        }
                        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                    }
                };
                mDrawerLayout.setDrawerListener(mDrawerToggle);
                MirakelActivity.this.mDrawerToggle = Optional.of(mDrawerToggle);
                if (actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    actionBar.setHomeButtonEnabled(true);
                }
            }
        });
    }


}
