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
package de.azapps.mirakel.model.query_builder;


import android.database.Cursor;

import com.google.common.base.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

import android.database.Cursor;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.test.MirakelTestCase;
import de.azapps.mirakelandroid.test.MirakelTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;

@RunWith(MirakelTestRunner.class)
public class QueryBuilderTest extends MirakelTestCase {

    @Test
    public void testBasicQuery() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        assertEquals("", qb.getSelection());
    }

    @Test
    public void testSingleAndArgument() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.EQ, 1);
        assertEquals(ModelBase.ID + " = ?", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals("1", args.get(0));
    }

    @Test
    public void testSingleOrArgument() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.or(ModelBase.ID, Operation.EQ, 1);
        assertEquals(ModelBase.ID + " = ?", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals("1", args.get(0));
    }

    @Test
    public void testEqual() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkOperation(qb, Operation.EQ, "=");
    }

    @Test
    public void testGreaterEqual() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkOperation(qb, Operation.GE, ">=");
    }

    @Test
    public void testLesserEqual() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkOperation(qb, Operation.LE, "<=");
    }

    @Test
    public void testGreater() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkOperation(qb, Operation.GT, ">");
    }

    @Test
    public void testLesser() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkOperation(qb, Operation.LT, "<");
    }

    @Test
    public void testLike() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkOperation(qb, Operation.LIKE, "LIKE");
    }

    @Test
    public void testIn() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.IN, 1);
        assertEquals(ModelBase.ID + " IN(?)", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals("1", args.get(0));
    }

    private static void checkOperation(final MirakelQueryBuilder qb,
                                       final Operation op, final String o) {
        qb.and(ModelBase.ID, op, 1);
        assertEquals(ModelBase.ID + " " + o + " ?", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals("1", args.get(0));
    }

    @Test
    public void testNotEqual() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkNotOperation(qb, Operation.NOT_EQ, "=");
    }

    @Test
    public void testNotGreaterEqual() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkNotOperation(qb, Operation.NOT_GE, ">=");
    }

    @Test
    public void testNotLesserEqual() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkNotOperation(qb, Operation.NOT_LE, "<=");
    }

    @Test
    public void testNotGreater() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkNotOperation(qb, Operation.NOT_GT, ">");
    }

    @Test
    public void testNotLesser() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkNotOperation(qb, Operation.NOT_LT, "<");
    }

    @Test
    public void testNotLike() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        checkNotOperation(qb, Operation.NOT_LIKE, "LIKE");
    }

    @Test
    public void testNotIn() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.NOT_IN, 1);
        assertEquals("NOT " + ModelBase.ID + " IN(?)", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals("1", args.get(0));
    }

    private static void checkNotOperation(final MirakelQueryBuilder qb,
                                          final Operation op, final String o) {
        qb.and(ModelBase.ID, op, 1);
        assertEquals("NOT " + ModelBase.ID + " " + o + " ?", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals("1", args.get(0));
    }

    @Test
    public void testTwoAndConditions() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.EQ, 1).and(ModelBase.NAME, Operation.EQ,
                "foo");
        assertEquals(ModelBase.ID + " = ? AND " + ModelBase.NAME + " = ?",
                     qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(2, args.size());
        assertEquals("1", args.get(0));
        assertEquals("foo", args.get(1));
    }

    @Test
    public void testTwoOrConditions() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.EQ, 1).or(ModelBase.NAME, Operation.EQ,
                "foo");
        assertEquals(ModelBase.ID + " = ? OR " + ModelBase.NAME + " = ?",
                     qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(2, args.size());
        assertEquals("1", args.get(0));
        assertEquals("foo", args.get(1));
    }

    @Test
    public void testAndSubcondition() {
        final MirakelQueryBuilder qbInner = new MirakelQueryBuilder(
            RuntimeEnvironment.application);
        qbInner.and(ModelBase.ID, Operation.EQ, 1).or(ModelBase.NAME,
                Operation.EQ, "foo");
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(Task.DONE, Operation.EQ, false).and(qbInner);
        assertEquals(Task.DONE + " = ? AND (" + ModelBase.ID + " = ? OR "
                     + ModelBase.NAME + " = ?)", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(3, args.size());
        assertEquals("0", args.get(0));
        assertEquals("1", args.get(1));
        assertEquals("foo", args.get(2));
    }

    @Test
    public void testOrSubcondition() {
        final MirakelQueryBuilder qbInner = new MirakelQueryBuilder(
            RuntimeEnvironment.application);
        qbInner.and(ModelBase.ID, Operation.EQ, 1).or(ModelBase.NAME,
                Operation.EQ, "foo");
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.or(Task.DONE, Operation.EQ, false).or(qbInner);
        assertEquals(Task.DONE + " = ? OR (" + ModelBase.ID + " = ? OR "
                     + ModelBase.NAME + " = ?)", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(3, args.size());
        assertEquals("0", args.get(0));
        assertEquals("1", args.get(1));
        assertEquals("foo", args.get(2));
    }

    @Test
    public void testSubquery() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        final MirakelQueryBuilder qbInner = new MirakelQueryBuilder(
            RuntimeEnvironment.application).select(ModelBase.ID);
        qb.and(ModelBase.ID, Operation.IN, qbInner, Task.URI);
        assertEquals(ModelBase.ID + " IN (SELECT " + ModelBase.ID + " FROM "
                     + Task.TABLE + ")", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(0, args.size());
    }

    @Test
    public void testBooleanFilterTrue() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(Task.DONE, Operation.EQ, true);
        assertEquals(Task.DONE + " = ?", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals("1", args.get(0));
    }

    @Test
    public void testBooleanFilterFalse() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(Task.DONE, Operation.EQ, false);
        assertEquals(Task.DONE + " = ?", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals("0", args.get(0));
    }

    @Test
    public void testIntFilter() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.EQ, 1);
        assertEquals(ModelBase.ID + " = ?", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals("1", args.get(0));
    }

    @Test
    public void testDoubleFilter() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(Task.PROGRESS, Operation.EQ, 1.0);
        assertEquals(Task.PROGRESS + " = ?", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals("1.0", args.get(0));
    }

    @Test
    public void testStringFilter() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.NAME, Operation.EQ, "foo");
        assertEquals(ModelBase.NAME + " = ?", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals("foo", args.get(0));
    }

    @Test
    public void testModelBaseFilter() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        final Task t = RandomHelper.getRandomTask();
        qb.and(ModelBase.ID, Operation.EQ, t);
        assertEquals(ModelBase.ID + " = ?", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(1, args.size());
        assertEquals(t.getId() + "", args.get(0));
    }

    @Test
    public void testListIntegerFilter() {
        final List<Integer> filter = Arrays
                                     .asList(new Integer[] { 1, 2, 3, 42 });
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.IN, filter);
        assertEquals(ModelBase.ID + " IN(?,?,?,?)", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(filter.size(), args.size());
        for (int i = 0; i < filter.size(); i++) {
            assertEquals(filter.get(i) + "", args.get(i));
        }
    }

    @Test
    public void testListStringFilter() {
        final List<String> filter = Arrays.asList(new String[] { "foo", "bar",
                                    "don't do this"
                                                               });
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.NAME, Operation.IN, filter);
        assertEquals(ModelBase.NAME + " IN(?,?,?)", qb.getSelection());
        final List<String> args = qb.getSelectionArguments();
        assertEquals(filter.size(), args.size());
        for (int i = 0; i < filter.size(); i++) {
            assertEquals(filter.get(i), args.get(i));
        }
    }

    //test get
    @Test
    public void testGetTask() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<Task> res_qb = qb.get(Task.class);
        if (!res_qb.isPresent()) {
            fail("Querybuilder returned empty result");
        }
        Cursor c = Robolectric.application.getContentResolver().query(Task.URI, Task.allColumns, null,
                   null, null);
        c.moveToFirst();
        Task res_raw = new Task(c);
        c.close();
        assertEquals(res_raw, res_qb.get());
    }
    @Test
    public void testGetAccount() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<AccountMirakel> res_qb = qb.get(AccountMirakel.class);
        if (!res_qb.isPresent()) {
            fail("Querybuilder returned empty result");
        }
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(
                       AccountMirakel.URI, AccountMirakel.allColumns, null, null, null);
        c.moveToFirst();
        AccountMirakel res_raw = new AccountMirakel(c);
        c.close();
        assertEquals(res_raw, res_qb.get());
    }
    @Test
    public void testGetFile() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<FileMirakel> res_qb = qb.get(FileMirakel.class);
        if (!res_qb.isPresent()) {
            fail("Querybuilder returned empty result");
        }
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(FileMirakel.URI,
                   FileMirakel.allColumns, null, null, null);
        c.moveToFirst();
        FileMirakel res_raw = new FileMirakel(c);
        c.close();
        assertEquals(res_raw, res_qb.get());
    }
    @Test
    public void testGetRecurring() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<Recurring> res_qb = qb.get(Recurring.class);
        if (!res_qb.isPresent()) {
            fail("Querybuilder returned empty result");
        }
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(Recurring.URI,
                   Recurring.allColumns, null, null, null);
        c.moveToFirst();
        Recurring res_raw = new Recurring(c);
        c.close();
        assertEquals(res_raw, res_qb.get());
    }
    @Test
    public void testGetSemantic() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<Semantic> res_qb = qb.get(Semantic.class);
        if (!res_qb.isPresent()) {
            fail("Querybuilder returned empty result");
        }
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(Semantic.URI,
                   Semantic.allColumns, null, null, null);
        c.moveToFirst();
        Semantic res_raw = new Semantic(c);
        c.close();
        assertEquals(res_raw, res_qb.get());
    }
    @Test
    public void testGetList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<ListMirakel> res_qb = qb.get(ListMirakel.class);
        if (!res_qb.isPresent()) {
            fail("Querybuilder returned empty result");
        }
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(ListMirakel.URI,
                   ListMirakel.allColumns, null, null, null);
        c.moveToFirst();
        ListMirakel res_raw = new ListMirakel(c);
        c.close();
        assertEquals(res_raw, res_qb.get());
    }
    @Test
    public void testGetMetaList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<SpecialList> res_qb = qb.get(SpecialList.class);
        if (!res_qb.isPresent()) {
            fail("Querybuilder returned empty result");
        }
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(SpecialList.URI,
                   SpecialList.allColumns, null, null, null);
        c.moveToFirst();
        SpecialList res_raw = new SpecialList(c);
        c.close();
        assertEquals(res_raw, res_qb.get());
    }

    //test getList
    private void compareLists(List res_qb, List res_raw) {
        assertEquals(res_raw.size(), res_qb.size());
        for (int i = 0; i < res_raw.size(); i++) {
            assertEquals(res_raw.get(i), res_qb.get(i));
        }
    }
    @Test
    public void testGetTaskList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List res_qb = qb.getList(Task.class);
        List res_raw = Task.cursorToTaskList(Robolectric.application.getContentResolver().query(Task.URI,
                                             Task.allColumns, null, null, null));
        compareLists(res_qb, res_raw);
    }



    @Test
    public void testGetAccountList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List res_qb = qb.getList(AccountMirakel.class);
        List res_raw = AccountMirakel.cursorToAccountList(
                           RuntimeEnvironment.application.getContentResolver().query(
                               AccountMirakel.URI, AccountMirakel.allColumns, null, null, null));
        compareLists(res_qb, res_raw);
    }
    @Test
    public void testGetFileList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List res_qb = qb.getList(FileMirakel.class);
        List res_raw = FileMirakel.cursorToFileList(Robolectric.application.getContentResolver().query(
                           FileMirakel.URI,
                           FileMirakel.allColumns, null, null, null));
        compareLists(res_qb, res_raw);
    }
    @Test
    public void testGetRecurringList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List res_qb = qb.getList(Recurring.class);
        List res_raw = Recurring.cursorToList(RuntimeEnvironment.application.getContentResolver().query(
                Recurring.URI, Recurring.allColumns, null, null, null));
        compareLists(res_qb, res_raw);
    }
    @Test
    public void testGetSemanticList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List res_qb = qb.getList(Semantic.class);
        List res_raw = Semantic.cursorToSemanticList(Robolectric.application.getContentResolver().query(
                           Semantic.URI,
                           Semantic.allColumns, null, null, null));
        compareLists(res_qb, res_raw);
    }
    @Test
    public void testGetListList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List res_qb = qb.getList(ListMirakel.class);
        List res_raw = ListMirakel.cursorToList(RuntimeEnvironment.application.getContentResolver().query(
                ListMirakel.URI,
                ListMirakel.allColumns, null, null, null));
        compareLists(res_qb, res_raw);
    }
    @Test
    public void testGetMetaListList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List res_qb = qb.getList(SpecialList.class);
        List res_raw = SpecialList.cursorToSpecialLists(Robolectric.application.getContentResolver().query(
                           SpecialList.URI, SpecialList.allColumns, null, null, null));
        compareLists(res_qb, res_raw);
    }

}
