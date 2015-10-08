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
package de.azapps.mirakel.model.list.meta;

import android.text.TextUtils;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.SpecialListsWhereDeserializer;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList.CONJUNCTION;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;


@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class SpecialListConditionTest extends MirakelDatabaseTestCase {

    @Test
    public void testGetAllSpecial() {
        List<SpecialList> all = SpecialList.allSpecial();
        for (SpecialList l : all) {
            assertThat(l.isSpecial()).isTrue();
        }
    }

    @Test
    public void testDoneCondition() {
        final boolean isDone = RandomHelper.getRandomboolean();

        final SpecialListsDoneProperty done = new SpecialListsDoneProperty(isDone);
        assertThat(done.isSet()).isEqualTo(isDone);

        final MirakelQueryBuilder qb = done.getWhereQueryBuilder(RuntimeEnvironment.application);
        assertThat(qb.getSelection()).isEqualTo("done = ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly(isDone ? "1" : "0");

        final Optional<SpecialListsBaseProperty> newDone = SpecialListsWhereDeserializer.deserializeWhere(
                    done.serialize(), "Test");
        if (newDone.isPresent() && (newDone.get() instanceof SpecialListsDoneProperty)) {
            final SpecialListsDoneProperty done2 = (SpecialListsDoneProperty)newDone.get();
            assertThat(done2.isSet()).isEqualTo(done.isSet());
        } else {
            fail("Could not parse done property: " + done.serialize());
        }
    }

    @Test
    public void testReminderCondition() {
        final boolean hasReminder = RandomHelper.getRandomboolean();

        final SpecialListsReminderProperty reminder = new SpecialListsReminderProperty(hasReminder);
        assertThat(reminder.isSet()).isEqualTo(hasReminder);

        final MirakelQueryBuilder qb = reminder.getWhereQueryBuilder(RuntimeEnvironment.application);
        assertThat(qb.getSelection()).isEqualTo(hasReminder ? "reminder IS NOT NULL " :
                                                "reminder IS NULL ");
        assertThat(qb.getSelectionArguments()).isEmpty();

        final Optional<SpecialListsBaseProperty> newReminder =
            SpecialListsWhereDeserializer.deserializeWhere(reminder.serialize(), "Test");
        if (newReminder.isPresent() && (newReminder.get() instanceof SpecialListsReminderProperty)) {
            final SpecialListsReminderProperty reminder2 = (SpecialListsReminderProperty)newReminder.get();
            assertThat(reminder2.isSet()).isEqualTo(reminder.isSet());
        } else {
            fail("Could not parse reminder property: " + reminder.serialize());
        }
    }

    @Test
    public void testFileCondition() {
        final boolean hasFile = RandomHelper.getRandomboolean();

        final SpecialListsFileProperty file = new SpecialListsFileProperty(hasFile);
        assertThat(file.isSet()).isEqualTo(hasFile);

        final MirakelQueryBuilder qb = file.getWhereQueryBuilder(RuntimeEnvironment.application);
        assertThat(qb.getSelection()).isEqualTo((hasFile ? "" : " NOT ") +
                                                "_id IN (SELECT task_id FROM files)");
        assertThat(qb.getSelectionArguments()).isEmpty();
        final Optional<SpecialListsBaseProperty> newFile = SpecialListsWhereDeserializer.deserializeWhere(
                    file.serialize(), "Test");
        if (newFile.isPresent() && (newFile.get() instanceof SpecialListsFileProperty)) {
            final SpecialListsFileProperty file2 = (SpecialListsFileProperty)newFile.get();
            assertThat(file2.isSet()).isEqualTo(file.isSet());
        } else {
            fail("Could not parse reminder property: " + file.serialize());
        }
    }

    @Test
    public void testDueExistsCondition() {
        final boolean hasDue = RandomHelper.getRandomboolean();

        final SpecialListsDueExistsProperty due = new SpecialListsDueExistsProperty(hasDue);
        assertThat(due.isSet()).isEqualTo(hasDue);

        final MirakelQueryBuilder qb = due.getWhereQueryBuilder(RuntimeEnvironment.application);
        assertThat(qb.getSelection()).isEqualTo(hasDue ? "due IS NULL " : "due IS NOT NULL ");
        assertThat(qb.getSelectionArguments()).isEmpty();

        final Optional<SpecialListsBaseProperty> newDue = SpecialListsWhereDeserializer.deserializeWhere(
                    due.serialize(), "Test");
        if (newDue.isPresent() && (newDue.get() instanceof SpecialListsDueExistsProperty)) {
            final SpecialListsDueExistsProperty due2 = (SpecialListsDueExistsProperty)newDue.get();
            assertThat(due2.isSet()).isEqualTo(due.isSet());
        } else {
            fail("Could not parse due exists property: " + due.serialize());
        }
    }

    @Test
    public void testContentCondition() {
        final String searchString = RandomHelper.getRandomString();
        final boolean negated = RandomHelper.getRandomboolean();
        final SpecialListsStringProperty.Type type = RandomHelper.getRandomStringPropertyType();

        final SpecialListsContentProperty content = new SpecialListsContentProperty(negated, searchString,
                type);
        assertThat(content.getSearchString()).isEqualTo(searchString);
        assertThat(content.getType()).isEqualTo(type);
        assertThat(content.isSet()).isEqualTo(negated);

        final MirakelQueryBuilder qb = content.getWhereQueryBuilder(RuntimeEnvironment.application);
        assertThat(qb.getSelection()).isEqualTo((negated ? "NOT " : "") + "content LIKE ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        switch (type) {
        case BEGIN:
            assertThat(qb.getSelectionArguments()).containsExactly(searchString + '%');
            break;
        case END:
            assertThat(qb.getSelectionArguments()).containsExactly('%' + searchString);
            break;
        case CONTAINS:
            assertThat(qb.getSelectionArguments()).containsExactly('%' + searchString + '%');
            break;
        }

        final Optional<SpecialListsBaseProperty> newContent =
            SpecialListsWhereDeserializer.deserializeWhere(content.serialize(), "Test");
        if (newContent.isPresent() && (newContent.get() instanceof SpecialListsContentProperty)) {
            final SpecialListsContentProperty content2 = (SpecialListsContentProperty)newContent.get();
            assertThat(content2.getSearchString()).isEqualTo(content.getSearchString());
            assertThat(content2.getType()).isEqualTo(content.getType());
            assertThat(content2.isSet()).isEqualTo(content.isSet());
        } else {
            fail("Could not parse content property: " + content.serialize());
        }
    }

    @Test
    public void testNameCondition() {
        final String searchString = RandomHelper.getRandomString();
        final boolean negated = RandomHelper.getRandomboolean();
        final SpecialListsStringProperty.Type type = RandomHelper.getRandomStringPropertyType();

        final SpecialListsNameProperty name = new SpecialListsNameProperty(negated, searchString, type);
        assertThat(name.getSearchString()).isEqualTo(searchString);
        assertThat(name.getType()).isEqualTo(type);
        assertThat(name.isSet()).isEqualTo(negated);

        final MirakelQueryBuilder qb = name.getWhereQueryBuilder(RuntimeEnvironment.application);
        assertThat(qb.getSelection()).isEqualTo((negated ? "NOT " : "") + "name LIKE ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        switch (type) {
        case BEGIN:
            assertThat(qb.getSelectionArguments()).containsExactly(searchString + '%');
            break;
        case END:
            assertThat(qb.getSelectionArguments()).containsExactly('%' + searchString);
            break;
        case CONTAINS:
            assertThat(qb.getSelectionArguments()).containsExactly('%' + searchString + '%');
            break;
        }

        final Optional<SpecialListsBaseProperty> newName = SpecialListsWhereDeserializer.deserializeWhere(
                    name.serialize(), "Test");
        if (newName.isPresent() && (newName.get() instanceof SpecialListsNameProperty)) {
            final SpecialListsNameProperty name2 = (SpecialListsNameProperty)newName.get();
            assertThat(name2.getSearchString()).isEqualTo(name.getSearchString());
            assertThat(name2.getType()).isEqualTo(name.getType());
            assertThat(name2.isSet()).isEqualTo(name.isSet());
        } else {
            fail("Could not parse name property: " + name.serialize());
        }
    }

    @Test
    public void testListNameCondition() {
        final String searchString = RandomHelper.getRandomString();
        final boolean negated = RandomHelper.getRandomboolean();
        final SpecialListsStringProperty.Type type = RandomHelper.getRandomStringPropertyType();

        final SpecialListsListNameProperty listName = new SpecialListsListNameProperty(negated,
                searchString, type);
        assertThat(listName.getSearchString()).isEqualTo(searchString);
        assertThat(listName.getType()).isEqualTo(type);
        assertThat(listName.isSet()).isEqualTo(negated);

        final MirakelQueryBuilder qb = listName.getWhereQueryBuilder(RuntimeEnvironment.application);
        assertThat(qb.getSelection()).isEqualTo("list_id IN (SELECT _id FROM lists WHERE " +
                                                (negated ? "NOT " : "") + "lists.name LIKE ?)");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        switch (type) {
        case BEGIN:
            assertThat(qb.getSelectionArguments()).containsExactly(searchString + '%');
            break;
        case END:
            assertThat(qb.getSelectionArguments()).containsExactly('%' + searchString);
            break;
        case CONTAINS:
            assertThat(qb.getSelectionArguments()).containsExactly('%' + searchString + '%');
            break;
        }

        final Optional<SpecialListsBaseProperty> newListName =
            SpecialListsWhereDeserializer.deserializeWhere(listName.serialize(), "Test");
        if (newListName.isPresent() && (newListName.get() instanceof SpecialListsListNameProperty)) {
            final SpecialListsListNameProperty listName2 = (SpecialListsListNameProperty)newListName.get();
            assertThat(listName2.getSearchString()).isEqualTo(listName.getSearchString());
            assertThat(listName2.getType()).isEqualTo(listName.getType());
            assertThat(listName2.isSet()).isEqualTo(listName.isSet());
        } else {
            fail("Could not parse list name property: " + listName.serialize());
        }
    }

    @Test
    public void testDueCondition() {
        final boolean negated = RandomHelper.getRandomboolean();
        final SpecialListsDueProperty.Unit unit = RandomHelper.getRandomUnit();
        final int length = Math.abs(RandomHelper.getRandomint(10));

        final SpecialListsDueProperty due = new SpecialListsDueProperty(unit, length, negated);
        assertThat(due.getUnit()).isEqualTo(unit);
        assertThat(due.isSet()).isEqualTo(negated);
        assertThat(due.getLength()).isEqualTo(length);

        final MirakelQueryBuilder qb = due.getWhereQueryBuilder(RuntimeEnvironment.application);
        DateTime date = new LocalDate().toDateTimeAtStartOfDay().plusDays(1).minusSeconds(10);
        switch (unit) {
        case DAY:
            date = date.plusDays(length);
            break;
        case MONTH:
            date = date.plusMonths(length);
            break;
        case YEAR:
            date = date.plusYears(length);
            break;
        }
        assertThat(qb.getSelection()).isEqualTo("due IS NOT NULL  AND due " + (negated ? '>' : '<') + " ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly(String.valueOf(
                    date.getMillis()));
        final Optional<SpecialListsBaseProperty> newDue = SpecialListsWhereDeserializer.deserializeWhere(
                    due.serialize(), "Test");
        if (newDue.isPresent() && (newDue.get() instanceof SpecialListsDueProperty)) {
            final SpecialListsDueProperty due2 = (SpecialListsDueProperty)newDue.get();
            assertThat(due2.getUnit()).isEqualTo(due.getUnit());
            assertThat(due2.isSet()).isEqualTo(due.isSet());
            assertThat(due2.getLength()).isEqualTo(due.getLength());
        } else {
            fail("Could not parse due property: " + due.serialize());
        }
    }

    //TODO check if its passing
    @Test
    public void testListCondition() {
        final boolean negated = RandomHelper.getRandomboolean();
        final List<Integer> lists = RandomHelper.getRandomList_ListMirakel_ID();
        final List<SpecialList> special = RandomHelper.getRandomList_SpecialList();
        final List<Integer> allIds = new ArrayList<>(lists);
        allIds.addAll(Collections2.transform(special, new Function<SpecialList, Integer>() {
            @Override
            public Integer apply(final SpecialList input) {
                return (int) input.getId();
            }
        }));

        final SpecialListsListProperty list = new SpecialListsListProperty(negated, allIds);
        assertThat(list.isSet()).isEqualTo(negated);
        assertThat(list.getContent()).containsExactlyElementsIn(allIds);

        final MirakelQueryBuilder qb = list.getWhereQueryBuilder(RuntimeEnvironment.application);

        final List<MirakelQueryBuilder> specialQBs = new ArrayList<>(Collections2.transform(special,
        new Function<SpecialList, MirakelQueryBuilder>() {
            @Override
            public MirakelQueryBuilder apply(final SpecialList input) {
                return input.getWhereQueryForTasks();
            }
        }));

        final String specialWhere = TextUtils.join(" OR ", Collections2.transform(specialQBs,
        new Function<MirakelQueryBuilder, String>() {
            @Override
            public String apply(final MirakelQueryBuilder input) {
                return '(' + input.getSelection() + ')';
            }
        }));

        final List<String> arguments = new ArrayList<>(Collections2.transform(lists,
        new Function<Integer, String>() {
            @Override
            public String apply(final Integer input) {
                return String.valueOf(input);
            }
        }));
        for (final MirakelQueryBuilder builder : specialQBs) {
            arguments.addAll(builder.getSelectionArguments());
        }
        String query = "";
        if (!special.isEmpty()) {
            query +=  specialWhere;
        }
        if (!lists.isEmpty()) {
            if (!query.isEmpty()) {
                query += " OR ";
            }
            query += "list_id IN(" + TextUtils.join(",", Collections2.transform(lists,
            new Function<Integer, String>() {
                @Override
                public String apply(final Integer input) {
                    return "?";
                }
            })) + ')';
        }

        if (negated && !query.trim().isEmpty()) {
            query = " NOT (" + query + ')';
        }
        assertThat(qb.getSelection()).isEqualTo(query);
        assertThat(qb.getSelectionArguments()).hasSize(arguments.size());
        assertThat(qb.getSelectionArguments()).containsExactlyElementsIn(arguments);

        final Optional<SpecialListsBaseProperty> newList = SpecialListsWhereDeserializer.deserializeWhere(
                    list.serialize(), "Test");
        if (newList.isPresent() && (newList.get() instanceof SpecialListsListProperty)) {
            final SpecialListsListProperty list2 = (SpecialListsListProperty)newList.get();
            assertThat(list2.isSet()).isEqualTo(list.isSet());
            assertThat(list2.getContent()).containsExactlyElementsIn(list.getContent());
        } else {
            fail("Could not parse list property: " + list.serialize());
        }
    }

    @Test
    public void testPriorityCondition() {
        final boolean negated = RandomHelper.getRandomboolean();
        final List<Integer> prios = new ArrayList<>(5);
        for (int i = -2; i < 3; i++) {
            if (RandomHelper.getRandomboolean()) {
                prios.add(i);
            }
        }

        final SpecialListsPriorityProperty prio = new SpecialListsPriorityProperty(negated, prios);
        assertThat(prio.isSet()).isEqualTo(negated);
        assertThat(prio.getContent()).containsExactlyElementsIn(prios);

        final MirakelQueryBuilder qb = prio.getWhereQueryBuilder(RuntimeEnvironment.application);
        String query;
        if (prios.isEmpty()) {
            query = "";
        } else {
            query = "priority IN(" + TextUtils.join(",", Collections2.transform(prios,
            new Function<Integer, String>() {
                @Override
                public String apply(final Integer input) {
                    return "?";
                }
            })) + ')';
            if (negated) {
                query = "NOT " + query;
            }
        }
        assertThat(qb.getSelection()).isEqualTo(query);
        assertThat(qb.getSelectionArguments()).hasSize(prios.size());
        assertThat(qb.getSelectionArguments()).containsExactlyElementsIn(Collections2.transform(prios,
        new Function<Integer, String>() {
            @Override
            public String apply(final Integer input) {
                return String.valueOf(input);
            }
        }));

        final Optional<SpecialListsBaseProperty> newPrio = SpecialListsWhereDeserializer.deserializeWhere(
                    prio.serialize(), "Test");
        if (newPrio.isPresent() && (newPrio.get() instanceof SpecialListsPriorityProperty)) {
            final SpecialListsPriorityProperty prio2 = (SpecialListsPriorityProperty)newPrio.get();
            assertThat(prio2.isSet()).isEqualTo(prio.isSet());
            assertThat(prio2.getContent()).containsExactlyElementsIn(prio.getContent());
        } else {
            fail("Could not parse priority property: " + prio.serialize());
        }
    }

    @Test
    public void testProgressCondition() {
        final int value = RandomHelper.getRandomint(100);
        final SpecialListsProgressProperty.OPERATION op = RandomHelper.getRandomOperation();

        final SpecialListsProgressProperty progress = new SpecialListsProgressProperty(value, op);
        assertThat(progress.getValue()).isEqualTo(value);
        assertThat(progress.getOperation()).isEqualTo(op);

        final MirakelQueryBuilder qb = progress.getWhereQueryBuilder(RuntimeEnvironment.application);
        String operation;
        switch (op) {
        case GREATER_THAN:
            operation = ">";
            break;
        case EQUAL:
            operation = "=";
            break;
        case LESS_THAN:
            operation = "<";
            break;
        default:
            fail("Someone add a new operation without updating the test");
            return;
        }
        assertThat(qb.getSelection()).isEqualTo("progress " + operation + " ?");
        assertThat(qb.getSelectionArguments()).hasSize(1);
        assertThat(qb.getSelectionArguments()).containsExactly(String.valueOf(value));

        final Optional<SpecialListsBaseProperty> newProgress =
            SpecialListsWhereDeserializer.deserializeWhere(progress.serialize(), "Test");
        if (newProgress.isPresent() && (newProgress.get() instanceof SpecialListsProgressProperty)) {
            final SpecialListsProgressProperty progress2 = (SpecialListsProgressProperty)newProgress.get();
            assertThat(progress2.getValue()).isEqualTo(progress.getValue());
            assertThat(progress2.getOperation()).isEqualTo(progress.getOperation());
        } else {
            fail("Could not parse progress property: " + progress.serialize());
        }
    }

    @Test
    public void testSubtaskCondition() {
        final boolean negated = RandomHelper.getRandomboolean();
        final boolean isParent = RandomHelper.getRandomboolean();

        final SpecialListsSubtaskProperty subtask = new SpecialListsSubtaskProperty(negated, isParent);
        assertThat(subtask.isSet()).isEqualTo(negated);
        assertThat(subtask.isParent()).isEqualTo(isParent);

        final MirakelQueryBuilder qb = subtask.getWhereQueryBuilder(RuntimeEnvironment.application);
        assertThat(qb.getSelection()).isEqualTo((negated ? " NOT " : "") + "_id IN (SELECT DISTINCT " +
                                                (isParent ? "parent_id" : "child_id") +
                                                " FROM subtasks)");
        assertThat(qb.getSelectionArguments()).isEmpty();

        final Optional<SpecialListsBaseProperty> newSubtask =
            SpecialListsWhereDeserializer.deserializeWhere(subtask.serialize(), "Test");
        if (newSubtask.isPresent() && (newSubtask.get() instanceof SpecialListsSubtaskProperty)) {
            final SpecialListsSubtaskProperty subtask2 = (SpecialListsSubtaskProperty)newSubtask.get();
            assertThat(subtask2.isSet()).isEqualTo(subtask.isSet());
            assertThat(subtask2.isParent()).isEqualTo(subtask2.isParent());
        } else {
            fail("Could not parse due exists property: " + subtask.serialize());
        }
    }


    @Test
    public void testTagCondition() {
        final int tagCount = RandomHelper.getRandomint(10);
        final List<Integer> tagIds = new ArrayList<>(tagCount);

        for (int i = 0; i < tagCount; i++) {
            tagIds.add(RandomHelper.getRandomint());
        }

        final boolean negated = RandomHelper.getRandomboolean();

        final SpecialListsTagProperty tag = new SpecialListsTagProperty(negated, tagIds);
        assertThat(tag.isSet()).isEqualTo(negated);
        assertThat(tag.getContent()).containsExactlyElementsIn(tagIds);

        final MirakelQueryBuilder qb = tag.getWhereQueryBuilder(RuntimeEnvironment.application);
        String query = "";
        if (!tagIds.isEmpty()) {
            query = (negated ? " NOT " : "") +
                    "_id IN (SELECT DISTINCT task_id FROM task_tag WHERE tag_id IN(";
            query += TextUtils.join(",", Collections2.transform(tagIds, new Function<Integer, String>() {
                @Override
                public String apply(final Integer input) {
                    return "?";
                }
            }));
            query += "))";
        }
        assertThat(qb.getSelection()).isEqualTo(query);
        assertThat(qb.getSelectionArguments()).hasSize(tagIds.size());
        assertThat(qb.getSelectionArguments()).containsExactlyElementsIn(Collections2.transform(tagIds,
        new Function<Integer, String>() {
            @Override
            public String apply(final Integer input) {
                return String.valueOf(input);
            }
        }));

        final Optional<SpecialListsBaseProperty> newTag = SpecialListsWhereDeserializer.deserializeWhere(
                    tag.serialize(), "Test");
        if (newTag.isPresent() && (newTag.get() instanceof SpecialListsTagProperty)) {
            final SpecialListsTagProperty tag2 = (SpecialListsTagProperty)newTag.get();
            assertThat(tag2.isSet()).isEqualTo(tag.isSet());
            assertThat(tag2.getContent()).containsExactlyElementsIn(tag.getContent());
        } else {
            fail("Could not parse tag property: " + tag.serialize());
        }
    }

    @Test
    public void testConjunctionCondition() {

        final boolean negated = RandomHelper.getRandomboolean();

        final SpecialListsDoneProperty dummyChild = new SpecialListsDoneProperty(negated);
        final SpecialListsConjunctionList conjunctionRoot = new SpecialListsConjunctionList(dummyChild,
                CONJUNCTION.AND);
        final SpecialListsConjunctionList conjunctionChild = new SpecialListsConjunctionList(dummyChild,
                CONJUNCTION.OR);
        final SpecialListsConjunctionList conjunctionLeaf = new SpecialListsConjunctionList(dummyChild,
                CONJUNCTION.AND);

        conjunctionLeaf.addChild(dummyChild);
        conjunctionChild.addChild(conjunctionLeaf);
        conjunctionRoot.addChild(conjunctionChild);
        assertThat(conjunctionRoot.getOperation()).isEqualTo(CONJUNCTION.AND);
        assertThat(conjunctionChild.getOperation()).isEqualTo(CONJUNCTION.OR);
        assertThat(conjunctionLeaf.getOperation()).isEqualTo(CONJUNCTION.AND);


        assertThat(conjunctionRoot.getChilds()).hasSize(2);
        assertThat(conjunctionRoot.getChilds().get(0)).isInstanceOf(SpecialListsDoneProperty.class);
        assertThat(((SpecialListsDoneProperty) conjunctionRoot.getChilds().get(0)).isSet()).isEqualTo(
            negated);
        assertThat(conjunctionRoot.getChilds().get(1)).isInstanceOf(SpecialListsConjunctionList.class);

        final SpecialListsConjunctionList child = (SpecialListsConjunctionList)
                conjunctionRoot.getChilds().get(1);
        assertThat(child.getChilds()).hasSize(2);
        assertThat(child.getOperation()).isEqualTo(CONJUNCTION.OR);

        assertThat(child.getChilds().get(0)).isInstanceOf(SpecialListsDoneProperty.class);
        assertThat(((SpecialListsDoneProperty) child.getChilds().get(0)).isSet()).isEqualTo(negated);
        assertThat(child.getChilds().get(1)).isInstanceOf(SpecialListsConjunctionList.class);

        final SpecialListsConjunctionList leaf = (SpecialListsConjunctionList) child.getChilds().get(1);
        assertThat(leaf.getChilds()).hasSize(2);
        assertThat(leaf.getOperation()).isEqualTo(CONJUNCTION.AND);

        assertThat(leaf.getChilds().get(0)).isInstanceOf(SpecialListsDoneProperty.class);
        assertThat(((SpecialListsDoneProperty) leaf.getChilds().get(0)).isSet()).isEqualTo(negated);
        assertThat(leaf.getChilds().get(1)).isInstanceOf(SpecialListsDoneProperty.class);
        assertThat(((SpecialListsDoneProperty) leaf.getChilds().get(1)).isSet()).isEqualTo(negated);



        assertThat(conjunctionChild.getChilds()).hasSize(2);
        assertThat(conjunctionChild.getOperation()).isEqualTo(CONJUNCTION.OR);

        assertThat(conjunctionChild.getChilds().get(0)).isInstanceOf(SpecialListsDoneProperty.class);
        assertThat(((SpecialListsDoneProperty) conjunctionChild.getChilds().get(0)).isSet()).isEqualTo(
            negated);
        assertThat(conjunctionChild.getChilds().get(1)).isInstanceOf(SpecialListsConjunctionList.class);

        SpecialListsConjunctionList childLeaf = (SpecialListsConjunctionList)
                                                conjunctionChild.getChilds().get(1);
        assertThat(childLeaf.getChilds()).hasSize(2);
        assertThat(childLeaf.getOperation()).isEqualTo(CONJUNCTION.AND);

        assertThat(childLeaf.getChilds().get(0)).isInstanceOf(SpecialListsDoneProperty.class);
        assertThat(((SpecialListsDoneProperty) childLeaf.getChilds().get(0)).isSet()).isEqualTo(negated);
        assertThat(childLeaf.getChilds().get(1)).isInstanceOf(SpecialListsDoneProperty.class);
        assertThat(((SpecialListsDoneProperty) childLeaf.getChilds().get(1)).isSet()).isEqualTo(negated);

        assertThat(conjunctionLeaf.getChilds()).hasSize(2);
        assertThat(conjunctionLeaf.getOperation()).isEqualTo(CONJUNCTION.AND);

        assertThat(conjunctionLeaf.getChilds().get(0)).isInstanceOf(SpecialListsDoneProperty.class);
        assertThat(((SpecialListsDoneProperty) conjunctionLeaf.getChilds().get(0)).isSet()).isEqualTo(
            negated);
        assertThat(conjunctionLeaf.getChilds().get(1)).isInstanceOf(SpecialListsDoneProperty.class);
        assertThat(((SpecialListsDoneProperty) conjunctionLeaf.getChilds().get(1)).isSet()).isEqualTo(
            negated);



        final MirakelQueryBuilder qb = conjunctionRoot.getWhereQueryBuilder(RuntimeEnvironment.application);
        String dummyQuery = '(' + dummyChild.getWhereQueryBuilder(
                                RuntimeEnvironment.application).getSelection() +
                            ')';
        final String query = dummyQuery + " AND (" + dummyQuery + " OR (" + dummyQuery + " AND " +
                             dummyQuery + "))";
        assertThat(qb.getSelection()).isEqualTo(query);
        assertThat(qb.getSelectionArguments()).hasSize(4);
        assertThat(qb.getSelectionArguments()).containsExactly(negated ? "1" : "0", negated ? "1" : "0",
                negated ? "1" : "0", negated ? "1" : "0");

        final Optional<SpecialListsBaseProperty> newRoot = SpecialListsWhereDeserializer.deserializeWhere(
                    conjunctionRoot.serialize(), "Test");
        if (newRoot.isPresent() && (newRoot.get() instanceof SpecialListsConjunctionList)) {
            final SpecialListsConjunctionList root2 = (SpecialListsConjunctionList)newRoot.get();
            assertThat(conjunctionRoot.getOperation()).isEqualTo(CONJUNCTION.AND);

            assertThat(root2.getChilds()).hasSize(2);
            assertThat(root2.getChilds().get(0)).isInstanceOf(SpecialListsDoneProperty.class);
            assertThat(((SpecialListsDoneProperty) root2.getChilds().get(0)).isSet()).isEqualTo(negated);
            assertThat(root2.getChilds().get(1)).isInstanceOf(SpecialListsConjunctionList.class);


            final SpecialListsConjunctionList child1 = (SpecialListsConjunctionList) root2.getChilds().get(1);
            assertThat(child1.getOperation()).isEqualTo(CONJUNCTION.OR);

            assertThat(child1.getChilds()).hasSize(2);
            assertThat(child1.getOperation()).isEqualTo(CONJUNCTION.OR);

            assertThat(child1.getChilds().get(0)).isInstanceOf(SpecialListsDoneProperty.class);
            assertThat(((SpecialListsDoneProperty) child1.getChilds().get(0)).isSet()).isEqualTo(negated);
            assertThat(child1.getChilds().get(1)).isInstanceOf(SpecialListsConjunctionList.class);

            final SpecialListsConjunctionList leaf1 = (SpecialListsConjunctionList) child1.getChilds().get(1);
            assertThat(leaf1.getOperation()).isEqualTo(CONJUNCTION.AND);

            assertThat(leaf1.getChilds().get(0)).isInstanceOf(SpecialListsDoneProperty.class);
            assertThat(((SpecialListsDoneProperty) leaf1.getChilds().get(0)).isSet()).isEqualTo(negated);
            assertThat(leaf1.getChilds().get(1)).isInstanceOf(SpecialListsDoneProperty.class);
            assertThat(((SpecialListsDoneProperty) leaf1.getChilds().get(1)).isSet()).isEqualTo(negated);

            assertThat(leaf1.getChilds()).hasSize(2);
            assertThat(leaf1.getOperation()).isEqualTo(CONJUNCTION.AND);

            assertThat(leaf1.getChilds().get(0)).isInstanceOf(SpecialListsDoneProperty.class);
            assertThat(((SpecialListsDoneProperty) leaf1.getChilds().get(0)).isSet()).isEqualTo(negated);
            assertThat(leaf1.getChilds().get(1)).isInstanceOf(SpecialListsDoneProperty.class);
            assertThat(((SpecialListsDoneProperty) leaf1.getChilds().get(1)).isSet()).isEqualTo(negated);


        } else {
            fail("Could not parse tag property: " + conjunctionRoot.serialize());
        }
    }
}
