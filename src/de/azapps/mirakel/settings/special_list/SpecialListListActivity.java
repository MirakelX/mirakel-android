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

package de.azapps.mirakel.settings.special_list;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.google.common.base.Optional;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.settings.R;

/**
 * An activity representing a list of SpecialLists. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link SpecialListDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link SpecialListListFragment} and the item details
 * (if present) is a {@link SpecialListDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link SpecialListListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class SpecialListListActivity extends Activity
    implements SpecialListListFragment.Callbacks {

    private static final int RESULT_SPECIAL = 0;
    private static final String TAG = "SpecialListListActivity";

    private FrameLayout mDetailContainer;
    private boolean mTwoPane = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MirakelCommonPreferences.isDark()) {
            setTheme(R.style.AppBaseThemeDARK);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speciallist_twopane);
        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mDetailContainer = (FrameLayout)findViewById(R.id.speciallist_detail_container);

        mTwoPane = MirakelCommonPreferences.isTablet();
        mDetailContainer.setVisibility(mTwoPane ? View.VISIBLE : View.GONE);
        FragmentTransaction transaction = getFragmentManager().beginTransaction().replace(
                                              R.id.speciallist_list_container, new SpecialListListFragment());
        if (mTwoPane) {
            transaction.replace(R.id.speciallist_detail_container,
                                getSpecialListDetailFragment(SpecialList.firstSpecialSafe()));
        }
        transaction.commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        SpecialList oldList = null;
        SpecialListDetailFragment frag = ((SpecialListDetailFragment)
                                          getFragmentManager().findFragmentById(
                                              R.id.speciallist_detail_container));
        if (frag != null) {
            oldList = frag.getList();
        }
        super.onConfigurationChanged(newConfig);
        if (mTwoPane != MirakelCommonPreferences.isTablet()) {
            mTwoPane = MirakelCommonPreferences.isTablet();
            mDetailContainer.setVisibility(mTwoPane ? View.VISIBLE : View.GONE);
            invalidateOptionsMenu();
            if (mTwoPane) {
                getFragmentManager().beginTransaction().replace(R.id.speciallist_detail_container,
                        getSpecialListDetailFragment(SpecialList.firstSpecialSafe())).commitAllowingStateLoss();
            } else if (oldList != null) {
                onItemSelected(oldList);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_SPECIAL) {
            switch (resultCode) {
            case SpecialListDetailActivity.NEED_UPDATE:
                updateList();
                break;
            case SpecialListDetailActivity.SWITCH_LAYOUT:
                if (mTwoPane && data != null) {
                    getFragmentManager().beginTransaction().replace(R.id.speciallist_detail_container,
                            getSpecialListDetailFragment((SpecialList) data.getParcelableExtra(
                                    SpecialListDetailFragment.ARG_ITEM))).commitAllowingStateLoss();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void updateList() {
        ((SpecialListListFragment)getFragmentManager().findFragmentById(
             R.id.speciallist_list_container)).reload();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (id == R.id.menu_add) {
            SpecialList newList = SpecialList.newSpecialList(getString(R.string.special_lists_new),
                                  Optional.<SpecialListsBaseProperty>absent(), true);
            onItemSelected(newList);
            if (mTwoPane) {
                updateList();
            }
        } else if (id == R.id.menu_delete && mTwoPane) {
            ((SpecialListDetailFragment)getFragmentManager().findFragmentById(
                 R.id.speciallist_detail_container)).getList().destroy();
            updateList();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.generic_list_settings, menu);
        menu.findItem(R.id.menu_delete).setVisible(mTwoPane);
        return true;
    }

    /**
     * Callback method from {@link SpecialListListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(final SpecialList item) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            getFragmentManager().beginTransaction()
            .replace(R.id.speciallist_detail_container, getSpecialListDetailFragment(item))
            .commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, SpecialListDetailActivity.class);
            detailIntent.putExtra(SpecialListDetailFragment.ARG_ITEM, item);
            startActivityForResult(detailIntent, RESULT_SPECIAL);
        }
    }

    private SpecialListDetailFragment getSpecialListDetailFragment(SpecialList item) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(SpecialListDetailFragment.ARG_ITEM, item);
        SpecialListDetailFragment fragment = new SpecialListDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }
}
