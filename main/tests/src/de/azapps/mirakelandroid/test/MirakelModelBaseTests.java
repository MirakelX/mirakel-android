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
package de.azapps.mirakelandroid.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import de.azapps.mirakel.model.account.AccountBaseTest;
import de.azapps.mirakel.model.file.FileBaseTest;
import de.azapps.mirakel.model.list.ListBaseTest;
import de.azapps.mirakel.model.recurring.RecurringBaseTest;
import de.azapps.mirakel.model.semantic.SemanticBaseTest;
import de.azapps.mirakel.model.tags.TagBaseTest;
import de.azapps.mirakel.model.task.TaskBaseTest;

public class MirakelModelBaseTests extends TestSuite {
	public static Test suite() {
		final TestSuite suite = new TestSuite("ModelBase");
		addTests(suite);

		return suite;
	}

	public static void addTests(final TestSuite suite) {
		suite.addTestSuite(AccountBaseTest.class);
		suite.addTestSuite(FileBaseTest.class);
		suite.addTestSuite(ListBaseTest.class);
		suite.addTestSuite(RecurringBaseTest.class);
		suite.addTestSuite(SemanticBaseTest.class);
		suite.addTestSuite(TagBaseTest.class);
		suite.addTestSuite(TaskBaseTest.class);
	}
}
