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
package de.azapps.mirakel.model.file;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;

import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.truth.Truth.assertThat;


@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class FileBaseTest extends MirakelDatabaseTestCase {



    // Test for getting and setting Task
    @Test
    public void testTask1() {
        final List<FileMirakel> all = FileMirakel.all();
        final FileMirakel obj = RandomHelper.getRandomElem(all);
        final Task t = RandomHelper.getRandomTask();
        obj.setTask(t);
        assertThat(obj.getTask()).isEqualTo(t);
    }

    // Test for getting and setting FileUri
    @Test
    public void testFileUri2() {
        final List<FileMirakel> all = FileMirakel.all();
        final FileMirakel obj = RandomHelper.getRandomElem(all);
        final Uri t = RandomHelper.getRandomUri();
        obj.setFileUri(t);
        assertThat(obj.getFileUri()).isEqualTo(t);
    }

}
