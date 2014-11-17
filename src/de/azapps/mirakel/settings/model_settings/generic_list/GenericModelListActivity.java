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

package de.azapps.mirakel.settings.model_settings.generic_list;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.google.common.base.Optional;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.settings.R;

import static com.google.common.base.Optional.absent;

public abstract class GenericModelListActivity<T extends ModelBase> extends FragmentActivity
    implements
    GenericModelListFragment.Callbacks<T> {

    private static final int RESULT_ITEM = 0;

    private FrameLayout mDetailContainer;
    private boolean mTwoPane;

    protected abstract boolean isSupport();

    @NonNull
    protected Optional<android.app.Fragment> getDetailFragment() {
        return absent();
    }

    @NonNull
    protected  Optional<android.support.v4.app.Fragment> getSupportDetailFragment() {
        return absent();
    }

    @NonNull
    protected abstract Class<? extends GenericModelListActivity> getSelf();


    private android.app.Fragment instanceDetail(final @NonNull T item) {
        final Optional<android.app.Fragment> fragment = getDetailFragment();
        if (!fragment.isPresent()) {
            throw new IllegalStateException("Activity claims to have fragment, but do not have one. Please implement getDetailFragment().");
        }
        final Bundle arguments = new Bundle();
        arguments.putParcelable(GenericModelDetailFragment.ARG_ITEM, item);
        fragment.get().setArguments(arguments);
        return fragment.get();
    }

    private android.support.v4.app.Fragment instanceSupportDetail(final @NonNull T item) {
        final Optional<android.support.v4.app.Fragment> fragment = getSupportDetailFragment();
        if (!fragment.isPresent()) {
            throw new IllegalStateException("Activity claims to have fragment, but do not have one. Please implement getSupportDetailFragment().");
        }
        final Bundle arguments = new Bundle();
        arguments.putParcelable(GenericModelDetailFragment.ARG_ITEM, item);
        fragment.get().setArguments(arguments);
        return fragment.get();
    }

    @NonNull
    protected abstract T getDefaultItem();

    /**
     * Call selectItem in your implementation after creating
     * @param ctx
     */
    @NonNull
    protected abstract void createItem(final @NonNull Context ctx);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MirakelCommonPreferences.isDark()) {
            setTheme(R.style.AppBaseThemeDARK);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_model_twopane);
        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mDetailContainer = (FrameLayout)findViewById(R.id.generic_model_detail_container);

        mTwoPane = MirakelCommonPreferences.isTablet();
        mDetailContainer.setVisibility(mTwoPane ? View.VISIBLE : View.GONE);
        final FragmentTransaction transaction = getFragmentManager().beginTransaction().replace(
                R.id.generic_model_list_container, new GenericModelListFragment<T>());
        if (mTwoPane) {
            if (!isSupport()) {
                transaction.replace(R.id.generic_model_detail_container, instanceDetail(getDefaultItem()));
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                        instanceSupportDetail(getDefaultItem())).commit();
            }
        }
        transaction.commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        T oldItem = null;
        GenericModelDetailFragment<T> frag = (GenericModelDetailFragment<T>)
                                             getFragmentManager().findFragmentById(R.id.speciallist_detail_container);
        if (frag != null) {
            oldItem = frag.getItem();
        }
        super.onConfigurationChanged(newConfig);
        if (mTwoPane != MirakelCommonPreferences.isTablet()) {
            mTwoPane = MirakelCommonPreferences.isTablet();
            mDetailContainer.setVisibility(mTwoPane ? View.VISIBLE : View.GONE);
            invalidateOptionsMenu();
            if (mTwoPane) {
                if (!isSupport()) {
                    getFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                            instanceDetail(getDefaultItem())).commitAllowingStateLoss();
                } else {
                    getSupportFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                            instanceSupportDetail(getDefaultItem())).commitAllowingStateLoss();
                }
            } else if (oldItem != null) {
                onItemSelected(oldItem);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_ITEM) {
            switch (resultCode) {
            case GenericModelDetailActivity.NEED_UPDATE:
                updateList();
                break;
            case GenericModelDetailActivity.SWITCH_LAYOUT:
                if (mTwoPane && data != null) {
                    if (!isSupport()) {
                        getFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                                instanceDetail((T) data.getParcelableExtra(
                                                   GenericModelDetailFragment.ARG_ITEM))).commitAllowingStateLoss();
                    } else {
                        getSupportFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                                instanceSupportDetail((T) data.getParcelableExtra(
                                                          GenericModelDetailFragment.ARG_ITEM))).commitAllowingStateLoss();
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void updateList() {
        ((GenericModelListFragment)getFragmentManager().findFragmentById(
             R.id.generic_model_list_container)).reload();
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
            finish();
            return true;
        } else if (id == R.id.menu_add) {
            createItem(this);
            updateList();
        } else if ((id == R.id.menu_delete) && mTwoPane) {
            final T toDestroy;
            if (isSupport()) {
                toDestroy = ((IDetailFragment<T>) getSupportFragmentManager().findFragmentById(
                                 R.id.generic_model_detail_container)).getItem();
            } else {
                toDestroy = ((IDetailFragment<T>) getFragmentManager().findFragmentById(
                                 R.id.generic_model_detail_container)).getItem();
            }
            toDestroy.destroy();
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
     * Callback method from {@link GenericModelListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(final @NonNull T item) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (!isSupport()) {
                getFragmentManager().beginTransaction()
                .replace(R.id.generic_model_detail_container, instanceDetail(item)).commit();
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                        instanceSupportDetail(item)).commit();
            }
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, GenericModelDetailActivity.class);
            detailIntent.putExtra(GenericModelDetailFragment.ARG_ITEM, item);
            if (!isSupport()) {
                detailIntent.putExtra(GenericModelDetailActivity.FRAGMENT,
                                      ((Object) instanceDetail(item)).getClass());
            } else {
                detailIntent.putExtra(GenericModelDetailActivity.FRAGMENT,
                                      ((Object) instanceSupportDetail(item)).getClass());
            }
            detailIntent.putExtra(GenericModelDetailActivity.BACK_ACTIVITY, getSelf());
            startActivityForResult(detailIntent, RESULT_ITEM);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateList();
    }
}
