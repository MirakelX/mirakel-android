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

 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.settings.model_settings.account;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.Html;

import de.azapps.mirakel.adapter.SimpleModelAdapter;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelDetailFragment;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelListActivity;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSetupActivity;

public class AccountSettingsActivity extends GenericModelListActivity<AccountMirakel> {

    // Helper stuff
    private void showAlert(final int titleId, final int messageId,
                           final android.content.DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(this).setTitle(titleId).setMessage(messageId)
        .setPositiveButton(android.R.string.ok, listener).show();
    }

    protected void handleCalDAV() {
        new AlertDialog.Builder(this)
        .setTitle(R.string.sync_caldav)
        .setMessage(
            Html.fromHtml(this
                          .getString(R.string.sync_caldav_howto)))
        .setNegativeButton(R.string.download,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                Helpers.openURL(getBaseContext(),
                                "http://mirakel.azapps.de/releases.html#davdroid");
            }
        })
        .setPositiveButton(R.string.sync_add_account,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                startActivity(new Intent(
                                  Settings.ACTION_ADD_ACCOUNT));
            }
        }).show();
    }

    @NonNull
    @Override
    protected GenericModelDetailFragment<AccountMirakel> getDetailFragment() {
        return new AccountDetailFragment();
    }

    @NonNull
    @Override
    protected AccountMirakel getDefaultItem() {
        return AccountMirakel.getLocal();
    }

    @NonNull
    @Override
    protected void createItem(@NonNull Context ctx) {
        final CharSequence[] items = getResources().getTextArray(
                                         R.array.sync_types);
        new AlertDialog.Builder(this).setTitle(R.string.sync_add)
        .setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                switch (which) {
                case 0:
                    showAlert(
                        R.string.alert_caldav_title,
                        R.string.alert_caldav,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            final DialogInterface dialog,
                            final int which) {
                            handleCalDAV();
                        }
                    });
                    break;
                case 1:
                    showAlert(
                        R.string.alert_taskwarrior_title,
                        R.string.alert_taskwarrior,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            final DialogInterface dialog,
                            final int which) {
                            startActivity(new Intent(
                                              AccountSettingsActivity.this,
                                              TaskWarriorSetupActivity.class));
                        }
                    });
                    break;
                default:
                    break;
                }
                dialog.dismiss();
            }
        }).show();

    }


    @Override
    public SimpleModelAdapter<AccountMirakel> getAdapter() {
        return new SimpleModelAdapter<>(this, new MirakelQueryBuilder(this).query(AccountMirakel.URI), 0,
                                        AccountMirakel.class);
    }
}
