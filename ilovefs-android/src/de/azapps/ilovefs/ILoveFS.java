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

package de.azapps.ilovefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ILoveFS {
    /**
     * The address where to send the mail to
     */
    private String email = null;
    /**
     * The package name of the app (to show the app store)
     */
    private String package_name = null;
    /**
     * Context where to open the alertdialog
     */
    private final Context context;

    /**
     * Simple Constructor.
     *
     * @param context
     */
    public ILoveFS(final Context context) {
        this.context = context;
    }

    /**
     * Show only the email-button
     *
     * @param context
     * @param email
     */
    public ILoveFS(final Context context, final String email) {
        this.email = email;
        this.context = context;
    }

    /**
     * Show the email and donate button
     *
     * @param context
     * @param email
     * @param package_name
     */
    public ILoveFS(final Context context, final String email,
                   final String package_name) {
        this.email = email;
        this.package_name = package_name;
        this.context = context;
    }

    public boolean isILFSDay() {
        final Calendar today = new GregorianCalendar();
        return today.get(Calendar.MONTH) == Calendar.FEBRUARY
               && today.get(Calendar.DAY_OF_MONTH) == 14;
    }

    /**
     * Show the AlertDialog if today is I love Free Software day
     */
    public void show() {
        final TextView message = new TextView(this.context);
        message.setTextAppearance(this.context,
                                  android.R.style.TextAppearance_Medium);
        message.setText(Html.fromHtml(this.context
                                      .getString(R.string.ilovefs_message)));
        if (Build.VERSION.SDK_INT < 11) {
            message.setTextColor(this.context.getResources().getColor(
                                     R.color.ilovefs_white));
        }
        final int padding = 10;
        message.setPadding(padding, padding, padding, padding);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        final AlertDialog.Builder builder = new AlertDialog.Builder(
            this.context);
        builder.setTitle(R.string.ilovefs_title).setView(message);
        if (this.rateListener != null && this.package_name != null) {
            builder.setPositiveButton(R.string.ilovefs_rate, this.rateListener);
        }
        if (this.emailListener != null && this.email != null) {
            builder.setNegativeButton(R.string.ilovefs_email,
                                      this.emailListener);
        }
        if (this.donateListener != null) {
            builder.setNeutralButton(R.string.ilovefs_donate,
                                     this.donateListener);
        }
        builder.show();
    }

    /**
     * Set the Listener for the rating-button
     */
    public DialogInterface.OnClickListener rateListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            try {
                ILoveFS.this.context.startActivity(new Intent(
                                                       Intent.ACTION_VIEW, Uri.parse("market://details?id="
                                                               + ILoveFS.this.package_name)));
            } catch (final android.content.ActivityNotFoundException anfe) {
                ILoveFS.this.context
                .startActivity(new Intent(
                                   Intent.ACTION_VIEW,
                                   Uri.parse("http://play.google.com/store/apps/details?id="
                                             + ILoveFS.this.package_name)));
            }
        }
    };
    /**
     * Set the Listener for the email-button
     */
    public DialogInterface.OnClickListener emailListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            final Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[] { ILoveFS.this.email });
            i.putExtra(Intent.EXTRA_SUBJECT, ILoveFS.this.context
                       .getString(R.string.ilovefs_email_title));
            try {
                final Intent ci = Intent.createChooser(i,
                                                       ILoveFS.this.context.getString(R.string.ilovefs_email));
                ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ILoveFS.this.context.startActivity(ci);
            } catch (final android.content.ActivityNotFoundException ex) {
                Toast.makeText(
                    ILoveFS.this.context,
                    ILoveFS.this.context
                    .getString(R.string.ilovefs_email_no_client),
                    Toast.LENGTH_SHORT).show();
            }
        }
    };
    /**
     * Set the Listener for the donate-button
     */
    public DialogInterface.OnClickListener donateListener = null;
}
