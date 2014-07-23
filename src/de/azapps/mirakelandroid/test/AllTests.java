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
import de.azapps.mirakel.model.task.TaskDeserializerTest;

public class AllTests extends TestSuite {
	public static Test suite() {
		final TestSuite suite = new TestSuite("All");
		MirakelModelBaseTests.addTests(suite);
		MirakelModelTests.addTests(suite);
		suite.addTestSuite(TaskDeserializerTest.class);

		return suite;
	}
}
