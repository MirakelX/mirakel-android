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

package de.azapps.mirakelandroid.test;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;

public class TestHelper {
    public static void init(final Context ctx) {
        DefinitionsHelper.init(ctx, "");
        MirakelPreferences.init(ctx);
        ErrorReporter.init(ctx);
        ThemeManager.init(ctx, R.style.MirakelBaseTheme, R.style.MirakelDialogTheme);
        CursorGetter.init(ctx);
        ModelBase.init(ctx);
        final Locale locale = Helpers.getLocale(ctx);
        Locale.setDefault(locale);
        final Configuration config = new Configuration ();
        config.locale = locale;
        ctx.getResources().updateConfiguration(config,
                                               ctx.getResources().getDisplayMetrics());
        Semantic.init(ctx);
        try {
            ListMirakel.newList(RandomHelper.getRandomString(), RandomHelper.getRandomSORT_BY(),
                                RandomHelper.getRandomAccountMirakel());
        } catch (ListMirakel.ListAlreadyExistsException e) {
            //eat it
        }
        final Task t = Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel());
        Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(),
                     RandomHelper.getRandomOptional_DateTime(), RandomHelper.getRandomPriority());
        Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(),
                     RandomHelper.getRandomString(), RandomHelper.getRandomboolean(),
                     RandomHelper.getRandomOptional_DateTime(), RandomHelper.getRandomPriority());

        Tag.newTag(RandomHelper.getRandomString());
        FileMirakel.newFile(ctx, t, RandomHelper.getRandomUri());
        Recurring.newRecurring(RandomHelper.getRandomString(), RandomHelper.getRandomPeriod(),
                               RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomOptional_DateTime(), RandomHelper.getRandomOptional_DateTime(),
                               RandomHelper.getRandomboolean(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomSparseBooleanArray());
        Semantic.newSemantic(RandomHelper.getRandomString(), RandomHelper.getRandomint(),
                             RandomHelper.getRandomint(), RandomHelper.getRandomOptional_ListMirakel(),
                             RandomHelper.getRandomint());
    }

    public static void terminate() {
    }

}
