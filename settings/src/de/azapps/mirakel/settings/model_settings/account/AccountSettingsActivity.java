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

package de.azapps.mirakel.settings.model_settings.account;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.Html;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.google.common.base.Optional;

import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.SettingsActivity;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelListActivity;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSetupActivity;

import static com.google.common.base.Optional.of;

public class AccountSettingsActivity extends GenericModelListActivity<AccountMirakel> {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
        builder.setTitle(R.string.settings_dev_sync)
        .setMessage(R.string.sync_message)
        .setPositiveButton(R.string.sync_contact_us, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Helpers.contact(AccountSettingsActivity.this, getString(R.string.sync_help_dev));
            }
        })
        .setNeutralButton(R.string.title_donations,
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final Intent intent = new Intent(AccountSettingsActivity.this,
                                                 SettingsActivity.class);
                intent.putExtra(SettingsActivity.SHOW_FRAGMENT,
                                de.azapps.mirakel.settings.custom_views.Settings.DONATE.ordinal());
                AccountSettingsActivity.this.startActivity(intent);
                dialog.dismiss();
            }
        })
        .setNegativeButton(R.string.sync_understand, null).show();
    }

    // Helper stuff
    private void showAlert(final int titleId, final int messageId,
                           final android.content.DialogInterface.OnClickListener listener) {
        new AlertDialogWrapper.Builder(this).setTitle(titleId).setMessage(messageId)
        .setPositiveButton(android.R.string.ok, listener).show();
    }

    protected void handleCalDAV() {
        new AlertDialogWrapper.Builder(this)
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

    @Override
    protected boolean isSupport() {
        return false;
    }

    @NonNull
    @Override
    protected Optional<android.app.Fragment> getDetailFragment(final @NonNull AccountMirakel item) {
        return of((Fragment)new AccountDetailFragment());
    }

    @NonNull
    @Override
    protected Class<? extends GenericModelListActivity> getSelf() {
        return AccountSettingsActivity.class;
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
        new AlertDialogWrapper.Builder(this).setTitle(R.string.sync_add)
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
    protected String getTextTitle() {
        return getString(R.string.sync_title);
    }

    @Override
    protected Class<AccountMirakel> getItemClass() {
        return AccountMirakel.class;
    }

    @Override
    protected Cursor getQuery() {
        return new MirakelQueryBuilder(this).query(AccountMirakel.URI).getRawCursor();
    }


}
