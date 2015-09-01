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

import de.azapps.mirakel.model.account.AccountMirakelTest;
import de.azapps.mirakel.model.file.FileMirakelTest;
import de.azapps.mirakel.model.list.ListMirakelTest;
import de.azapps.mirakel.model.recurring.RecurringTest;
import de.azapps.mirakel.model.semantic.SemanticTest;
import de.azapps.mirakel.model.tags.TagTest;
import de.azapps.mirakel.model.task.TaskTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    AccountMirakelTest.class,
    FileMirakelTest.class,
    ListMirakelTest.class,
    RecurringTest.class,
    SemanticTest.class,
    TagTest.class,
    TaskTest.class
})
public class ModelDatabaseTestSuite {
}
