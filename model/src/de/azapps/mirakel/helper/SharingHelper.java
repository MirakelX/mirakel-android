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

package de.azapps.mirakel.helper;

import android.content.Context;
import android.content.Intent;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.ListMirakelInterface;
import de.azapps.mirakel.model.task.Task;

public class SharingHelper {

    /**
     * Share something
     *
     * @param context
     * @param subject
     * @param shareBody
     */
    static void share(final Context context, final String subject,
                      String shareBody) {
        shareBody += "\n\n" + context.getString(R.string.share_footer);
        final Intent sharingIntent = new Intent(
            android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        final Intent ci = Intent.createChooser(sharingIntent, context
                                               .getResources().getString(R.string.share_using));
        ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(ci);
    }

    /**
     * Share a list of Tasks from a List with other apps
     *
     * @param ctx
     * @param l
     */
    public static void share(final Context ctx, final ListMirakelInterface l) {
        final String subject = ctx.getString(R.string.share_list_title,
                                             l.getName(), l.countTasks());
        String body = "";
        for (final Task t : l.tasks()) {
            if (t.isDone()) {
                // body += "* ";
                continue;
            }
            body += "* ";
            body += TaskHelper.getTaskName(ctx, t) + "\n";
        }
        share(ctx, subject, body);
    }

    // Sharing
    /**
     * Share a Task as text with other apps
     *
     * @param ctx
     * @param t
     */
    public static void share(final Context ctx, final Task t) {
        final String subject = TaskHelper.getTaskName(ctx, t);
        share(ctx, subject, t.getContent());
    }

}
