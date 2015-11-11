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
import android.support.annotation.NonNull;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.truth.Truth.assertThat;


@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class QueryBuilderTest extends MirakelDatabaseTestCase {

    @Test
    public void testBasicQuery() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        assertThat(qb.getSelection()).isEqualTo("");
    }

    @Test
    public void testSingleAndArgument() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.EQ, 1);
        assertThat(qb.getSelection()).isEqualTo(ModelBase.ID + " = ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly("1");
    }

    @Test
    public void testSingleOrArgument() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.or(ModelBase.ID, Operation.EQ, 1);
        assertThat(qb.getSelection()).isEqualTo(ModelBase.ID + " = ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly("1");
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
        assertThat(qb.getSelection()).isEqualTo(ModelBase.ID + " IN(?)");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly("1");
    }

    private static void checkOperation(final MirakelQueryBuilder qb,
                                       final Operation op, final String o) {
        qb.and(ModelBase.ID, op, 1);
        assertThat(qb.getSelection()).isEqualTo(ModelBase.ID + ' ' + o + " ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly("1");
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
        assertThat(qb.getSelection()).isEqualTo("NOT " + ModelBase.ID + " IN(?)");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly("1");
    }

    private static void checkNotOperation(final MirakelQueryBuilder qb,
                                          final Operation op, final String o) {
        qb.and(ModelBase.ID, op, 1);
        assertThat(qb.getSelection()).isEqualTo("NOT " + ModelBase.ID + ' ' + o + " ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly("1");
    }

    @Test
    public void testTwoAndConditions() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.EQ, 1).and(ModelBase.NAME, Operation.EQ,
                "foo");
        assertThat(qb.getSelection()).isEqualTo(ModelBase.ID + " = ? AND " + ModelBase.NAME + " = ?");
        assertThat(qb.getSelectionArguments()).hasSize(2);
        assertThat(qb.getSelectionArguments()).containsExactly("1", "foo");
    }

    @Test
    public void testTwoOrConditions() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.EQ, 1).or(ModelBase.NAME, Operation.EQ,
                "foo");
        assertThat(qb.getSelection()).isEqualTo(ModelBase.ID + " = ? OR " + ModelBase.NAME + " = ?");
        assertThat(qb.getSelectionArguments()).hasSize(2);
        assertThat(qb.getSelectionArguments()).containsExactly("1", "foo");
    }

    @Test
    public void testAndSubcondition() {
        final MirakelQueryBuilder qbInner = new MirakelQueryBuilder(
            RuntimeEnvironment.application);
        qbInner.and(ModelBase.ID, Operation.EQ, 1).or(ModelBase.NAME,
                Operation.EQ, "foo");
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(Task.DONE, Operation.EQ, false).and(qbInner);
        assertThat(qb.getSelection()).isEqualTo(Task.DONE + " = ? AND (" + ModelBase.ID + " = ? OR "
                                                + ModelBase.NAME + " = ?)");
        assertThat(qb.getSelectionArguments()).hasSize(3);
        assertThat(qb.getSelectionArguments()).containsExactly("0", "1", "foo");
    }

    @Test
    public void testOrSubcondition() {
        final MirakelQueryBuilder qbInner = new MirakelQueryBuilder(
            RuntimeEnvironment.application);
        qbInner.and(ModelBase.ID, Operation.EQ, 1).or(ModelBase.NAME,
                Operation.EQ, "foo");
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.or(Task.DONE, Operation.EQ, false).or(qbInner);
        assertThat(qb.getSelection()).isEqualTo(Task.DONE + " = ? OR (" + ModelBase.ID + " = ? OR "
                                                + ModelBase.NAME + " = ?)");
        assertThat(qb.getSelectionArguments()).hasSize(3);
        assertThat(qb.getSelectionArguments()).containsExactly("0", "1", "foo");
    }

    @Test
    public void testSubquery() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        final MirakelQueryBuilder qbInner = new MirakelQueryBuilder(
            RuntimeEnvironment.application).select(ModelBase.ID);
        qb.and(ModelBase.ID, Operation.IN, qbInner, Task.URI);
        assertThat(qb.getSelection()).isEqualTo(ModelBase.ID + " IN (SELECT " + ModelBase.ID + " FROM "
                                                + Task.TABLE + ')');
        assertThat(qb.getSelectionArguments()).isEmpty();
    }

    @Test
    public void testBooleanFilterTrue() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(Task.DONE, Operation.EQ, true);
        assertThat(qb.getSelection()).isEqualTo(Task.DONE + " = ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly("1");
    }

    @Test
    public void testBooleanFilterFalse() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(Task.DONE, Operation.EQ, false);
        assertThat(qb.getSelection()).isEqualTo(Task.DONE + " = ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly("0");
    }

    @Test
    public void testIntFilter() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.EQ, 1);
        assertThat(qb.getSelection()).isEqualTo(ModelBase.ID + " = ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly("1");
    }

    @Test
    public void testDoubleFilter() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(Task.PROGRESS, Operation.EQ, 1.0);
        assertThat(qb.getSelection()).isEqualTo(Task.PROGRESS + " = ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly("1.0");
    }

    @Test
    public void testStringFilter() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.NAME, Operation.EQ, "foo");
        assertThat(qb.getSelection()).isEqualTo(ModelBase.NAME + " = ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly("foo");
    }

    @Test
    public void testModelBaseFilter() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        final Task t = RandomHelper.getRandomTask();
        qb.and(ModelBase.ID, Operation.EQ, t);
        assertThat(qb.getSelection()).isEqualTo(ModelBase.ID + " = ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly(String.valueOf(t.getId()));
    }

    @Test
    public void testListIntegerFilter() {
        final List<Integer> filter = Arrays
                                     .asList(1, 2, 3, 42);
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.ID, Operation.IN, filter);
        assertThat(qb.getSelection()).isEqualTo(ModelBase.ID + " IN(?,?,?,?)");
        assertThat(qb.getSelectionArguments()).hasSize(filter.size());
        assertThat(qb.getSelectionArguments())
        .containsExactlyElementsIn(Collections2.transform(filter, new Function<Integer, String>() {
            @Override
            public String apply(final Integer input) {
                return String.valueOf(input);
            }
        }));
    }

    @Test
    public void testListStringFilter() {
        final List<String> filter = Arrays.asList("foo", "bar",
                                    "don't do this");
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ModelBase.NAME, Operation.IN, filter);
        assertThat(qb.getSelection()).isEqualTo(ModelBase.NAME + " IN(?,?,?)");
        assertThat(qb.getSelectionArguments()).hasSize(filter.size());
        assertThat(qb.getSelectionArguments()).containsExactlyElementsIn(filter);
    }

    //test get
    @Test
    public void testGetTask() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        final Optional<Task> res_qb = qb.get(Task.class);
        assertThat(res_qb).isPresent();
        final Cursor c = RuntimeEnvironment.application.getContentResolver().query(Task.URI,
                         Task.allColumns,
                         null,
                         null, null);
        assertThat(c.moveToFirst()).isTrue();
        Task res_raw = new Task(CursorGetter.unsafeGetter(c));
        c.close();
        assertThat(res_qb.get()).isEqualTo(res_raw);
    }
    @Test
    public void testGetAccount() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<AccountMirakel> res_qb = qb.get(AccountMirakel.class);
        assertThat(res_qb).isPresent();
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(
                       AccountMirakel.URI, AccountMirakel.allColumns, null, null, null);
        assertThat(c.moveToFirst()).isTrue();
        AccountMirakel res_raw = new AccountMirakel(CursorGetter.unsafeGetter(c));
        c.close();
        assertThat(res_qb.get()).isEqualTo(res_raw);
    }
    @Test
    public void testGetFile() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<FileMirakel> res_qb = qb.get(FileMirakel.class);
        assertThat(res_qb).isPresent();
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(FileMirakel.URI,
                   FileMirakel.allColumns, null, null, null);
        assertThat(c.moveToFirst()).isTrue();
        FileMirakel res_raw = new FileMirakel(CursorGetter.unsafeGetter(c));
        c.close();
        assertThat(res_qb.get()).isEqualTo(res_raw);
    }
    @Test
    public void testGetRecurring() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<Recurring> res_qb = qb.get(Recurring.class);
        assertThat(res_qb).isPresent();
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(Recurring.URI,
                   Recurring.allColumns, null, null, null);
        assertThat(c.moveToFirst()).isTrue();
        Recurring res_raw = new Recurring(CursorGetter.unsafeGetter(c));
        c.close();
        assertThat(res_qb.get()).isEqualTo(res_raw);
    }
    @Test
    public void testGetSemantic() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<Semantic> res_qb = qb.get(Semantic.class);
        assertThat(res_qb).isPresent();
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(Semantic.URI,
                   Semantic.allColumns, null, null, null);
        assertThat(c.moveToFirst()).isTrue();
        Semantic res_raw = new Semantic(CursorGetter.unsafeGetter(c));
        c.close();
        assertThat(res_qb.get()).isEqualTo(res_raw);
    }
    @Test
    public void testGetList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        Optional<ListMirakel> res_qb = qb.get(ListMirakel.class);
        assertThat(res_qb).isPresent();
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(ListMirakel.URI,
                   ListMirakel.allColumns, null, null, null);
        assertThat(c.moveToFirst()).isTrue();
        ListMirakel res_raw = new ListMirakel(CursorGetter.unsafeGetter(c));
        c.close();
        assertThat(res_qb.get()).isEqualTo(res_raw);
    }
    @Test
    public void testGetMetaList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        qb.and(ListMirakel.IS_SPECIAL, Operation.EQ, true);
        final Optional<SpecialList> res_qb = qb.get(SpecialList.class);
        assertThat(res_qb).isPresent();
        Cursor c = RuntimeEnvironment.application.getContentResolver().query(SpecialList.URI,
                   SpecialList.allColumns, "_id=?", new String[] {String.valueOf(res_qb.get().getId())}, null);
        assertThat(c.moveToFirst()).isTrue();
        final SpecialList res_raw = new SpecialList(CursorGetter.unsafeGetter(c));
        c.close();
        assertThat(res_qb.get()).isEqualTo(res_raw);
    }


    @Test
    public void testGetTaskList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List<Task> res_qb = qb.getList(Task.class);
        List<Task> res_raw = new Cursor2List<>(new CursorWrapper.CursorConverter<Task>() {
            @Override
            public Task convert(@NonNull CursorGetter getter) {
                return new Task(getter);
            }
        }).convert(CursorGetter.unsafeGetter(RuntimeEnvironment.application.getContentResolver().query(
                Task.URI,
                Task.allColumns, null, null, null)));
        assertThat(res_qb).hasSize(res_raw.size());
        assertThat(res_qb).containsExactlyElementsIn(res_raw);
    }



    @Test
    public void testGetAccountList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        final List<AccountMirakel> res_qb = qb.getList(AccountMirakel.class);
        final List<AccountMirakel> res_raw = new Cursor2List<>(new
        CursorWrapper.CursorConverter<AccountMirakel>() {
            @Override
            public AccountMirakel convert(@NonNull CursorGetter getter) {
                return new AccountMirakel(getter);
            }
        }).convert(CursorGetter.unsafeGetter(RuntimeEnvironment.application.getContentResolver().query(
                AccountMirakel.URI,
                AccountMirakel.allColumns, null, null, null)));
        assertThat(res_qb).hasSize(res_raw.size());
        assertThat(res_qb).containsExactlyElementsIn(res_raw);
    }
    @Test
    public void testGetFileList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List<FileMirakel> res_qb = qb.getList(FileMirakel.class);
        List<FileMirakel> res_raw = new Cursor2List<>(new CursorWrapper.CursorConverter<FileMirakel>() {
            @Override
            public FileMirakel convert(@NonNull CursorGetter getter) {
                return new FileMirakel(getter);
            }
        }).convert(CursorGetter.unsafeGetter(RuntimeEnvironment.application.getContentResolver().query(
                FileMirakel.URI,
                FileMirakel.allColumns, null, null, null)));
        assertThat(res_qb).hasSize(res_raw.size());
        assertThat(res_qb).containsExactlyElementsIn(res_raw);
    }
    @Test
    public void testGetRecurringList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List<Recurring> res_qb = qb.getList(Recurring.class);
        List<Recurring> res_raw = new Cursor2List<>(new CursorWrapper.CursorConverter<Recurring>() {
            @Override
            public Recurring convert(@NonNull CursorGetter getter) {
                return new Recurring(getter);
            }
        }).convert(CursorGetter.unsafeGetter(RuntimeEnvironment.application.getContentResolver().query(
                Recurring.URI,
                Recurring.allColumns, null, null, null)));
        assertThat(res_qb).hasSize(res_raw.size());
        assertThat(res_qb).containsExactlyElementsIn(res_raw);
    }
    @Test
    public void testGetSemanticList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List<Semantic> res_qb = qb.getList(Semantic.class);
        List<Semantic> res_raw = new Cursor2List<>(new CursorWrapper.CursorConverter<Semantic>() {
            @Override
            public Semantic convert(@NonNull CursorGetter getter) {
                return new Semantic(getter);
            }
        }).convert(CursorGetter.unsafeGetter(RuntimeEnvironment.application.getContentResolver().query(
                Semantic.URI,
                Semantic.allColumns, null, null, null)));
        assertThat(res_qb).hasSize(res_raw.size());
        assertThat(res_qb).containsExactlyElementsIn(res_raw);
    }
    @Test
    public void testGetListList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List<ListMirakel> res_qb = qb.getList(ListMirakel.class);
        List<ListMirakel> res_raw = new Cursor2List<>(new CursorWrapper.CursorConverter<ListMirakel>() {
            @Override
            public ListMirakel convert(@NonNull CursorGetter getter) {
                return new ListMirakel(getter);
            }
        }).convert(CursorGetter.unsafeGetter(RuntimeEnvironment.application.getContentResolver().query(
                ListMirakel.URI,
                ListMirakel.allColumns, null, null, null)));
        assertThat(res_qb).hasSize(res_raw.size());
        assertThat(res_qb).containsExactlyElementsIn(res_raw);
    }
    @Test
    public void testGetMetaListList() {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(RuntimeEnvironment.application);
        List<SpecialList> res_qb = qb.getList(SpecialList.class);
        List<SpecialList> res_raw = new Cursor2List<>(new CursorWrapper.CursorConverter<SpecialList>() {
            @Override
            public SpecialList convert(@NonNull CursorGetter getter) {
                return new SpecialList(getter);
            }
        }).convert(CursorGetter.unsafeGetter(RuntimeEnvironment.application.getContentResolver().query(
                SpecialList.URI,
                SpecialList.allColumns, null, null, null)));
        assertThat(res_qb).hasSize(res_raw.size());
        assertThat(res_qb).containsExactlyElementsIn(res_raw);
    }

}
