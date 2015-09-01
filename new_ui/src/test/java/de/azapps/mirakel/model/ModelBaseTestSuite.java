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
package de.azapps.mirakel.model;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.azapps.mirakel.model.account.AccountBaseTest;
import de.azapps.mirakel.model.file.FileBaseTest;
import de.azapps.mirakel.model.list.ListBaseTest;
import de.azapps.mirakel.model.recurring.RecurringBaseTest;
import de.azapps.mirakel.model.semantic.SemanticBaseTest;
import de.azapps.mirakel.model.tags.TagBaseTest;
import de.azapps.mirakel.model.task.TaskBaseTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    AccountBaseTest.class,
    FileBaseTest.class,
    ListBaseTest.class,
    RecurringBaseTest.class,
    SemanticBaseTest.class,
    TagBaseTest.class,
    TaskBaseTest.class
})
public class ModelBaseTestSuite {
}
