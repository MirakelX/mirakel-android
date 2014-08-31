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

package de.azapps.mirakel.settings.generic_list;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import com.google.common.base.Optional;

import java.util.Locale;

import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.settings.R;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public abstract class GenericListSettingActivity<T extends ModelBase> extends ActionBarActivity
    implements
    GenericListSettingFragment.Callbacks<T>, GenericSettingsFragment.Callbacks<T> {

    private boolean isInList = true;
    private boolean isTablet = false;
    private Optional<T> currentModel = absent();

    protected abstract void createModel();

    protected GenericListSettingFragment getNewListFragment() {
        return new GenericListSettingFragment();
    }

    private Fragment getListFragment() {
        return getFragmentManager().findFragmentById(R.id.list_fragment);
    }

    private PreferenceFragment getSettingsFragment() {
        return (PreferenceFragment) getFragmentManager().findFragmentById(R.id.settings_fragment);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MirakelCommonPreferences.isDark()) {
            setTheme(R.style.AppBaseThemeDARK);
        } else {
            setTheme(R.style.AppBaseTheme);
        }
        Locale.setDefault(Helpers.getLocal(this));
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_generic_list_setting);
        isTablet = findViewById(R.id.settings_fragment) != null;
        GenericListSettingFragment listFragment = getNewListFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.list_fragment, listFragment);
        if (isTablet) {
            T first =  new MirakelQueryBuilder(this).get(getMyClass());
            GenericSettingsFragment settingsFragment = GenericSettingsFragment.newInstance(first);
            transaction.replace(R.id.settings_fragment, settingsFragment);
        }
        transaction.commit();
    }


    @Override
    public void selectItem(T model) {
        isInList = false;
        supportInvalidateOptionsMenu();
        currentModel = of(model);
        if (getSettingsFragment() == null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.list_fragment, GenericSettingsFragment.newInstance(model));
            transaction.addToBackStack(null);
            transaction.commit();
        } else {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.settings_fragment, GenericSettingsFragment.newInstance(model));
            transaction.commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.generic_list_settings, menu);
        if (isTablet) {
            menu.findItem(R.id.menu_add).setVisible(true);
            menu.findItem(R.id.menu_delete).setVisible(true);
        } else if (isInList) {
            menu.findItem(R.id.menu_add).setVisible(true);
            menu.findItem(R.id.menu_delete).setVisible(false);
        } else {
            menu.findItem(R.id.menu_add).setVisible(false);
            menu.findItem(R.id.menu_delete).setVisible(true);
        }
        return true;
    }

    public void onBackPressed() {
        super.onBackPressed();
        isInList = true;
        supportInvalidateOptionsMenu();
        currentModel = absent();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_add) {
            createModel();
        } else if (id == R.id.menu_delete) {
            OptionalUtils.withOptional(currentModel, new OptionalUtils.Procedure<T>() {
                @Override
                public void apply(T input) {
                    handleDelete(input);
                    if (!isTablet) {
                        getFragmentManager().popBackStack();
                    }
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleDelete(final T model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GenericListSettingActivity.this);
        builder.setTitle(R.string.delete);
        builder.setMessage(getString(R.string.delete_model_title, model.getName()));
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                model.destroy();
            }
        });
        builder.show();
    }

}
