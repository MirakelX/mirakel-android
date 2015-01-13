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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.SpecialListsWhereDeserializer;
import de.azapps.mirakel.model.list.meta.SpecialListsConjunctionList.CONJUNCTION;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakelandroid.test.RandomHelper;
import de.azapps.mirakelandroid.test.TestHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class SpecialListConditionTest {

    @Test
    public void testDoneCondition() {
        final boolean isDone = RandomHelper.getRandomboolean();

        final SpecialListsDoneProperty done = new SpecialListsDoneProperty(isDone);
        assertEquals("Done status not set correctly", isDone, done.isSet());

        final MirakelQueryBuilder qb = done.getWhereQueryBuilder(Robolectric.application);
        assertEquals("Query does not match", "done = ?", qb.getSelection().trim());
        assertEquals("Argument count does not match", 1L, qb.getSelectionArguments().size());
        assertEquals("Argument does not match", isDone ? "1" : "0", qb.getSelectionArguments().get(0));

        final Optional<SpecialListsBaseProperty> newDone = SpecialListsWhereDeserializer.deserializeWhere(
                    done.serialize(), "Test");
        if (newDone.isPresent() && (newDone.get() instanceof SpecialListsDoneProperty)) {
            final SpecialListsDoneProperty done2 = (SpecialListsDoneProperty)newDone.get();
            assertEquals("Done state does not match after parsing", done.isSet(), done2.isSet());
        } else {
            fail("Could not parse done property: " + done.serialize());
        }
    }

    @Test
    public void testReminderCondition() {
        final boolean hasReminder = RandomHelper.getRandomboolean();

        final SpecialListsReminderProperty reminder = new SpecialListsReminderProperty(hasReminder);
        assertEquals("Reminder status not set correctly", hasReminder, reminder.isSet());

        final MirakelQueryBuilder qb = reminder.getWhereQueryBuilder(Robolectric.application);
        assertEquals("Query does not match", hasReminder ? "reminder IS NOT NULL" : "reminder IS NULL",
                     qb.getSelection().trim());
        assertEquals("Argument count does not match", 0L, qb.getSelectionArguments().size());

        final Optional<SpecialListsBaseProperty> newReminder =
            SpecialListsWhereDeserializer.deserializeWhere(reminder.serialize(), "Test");
        if (newReminder.isPresent() && (newReminder.get() instanceof SpecialListsReminderProperty)) {
            final SpecialListsReminderProperty reminder2 = (SpecialListsReminderProperty)newReminder.get();
            assertEquals("Reminder state does not match after parsing", reminder.isSet(), reminder2.isSet());
        } else {
            fail("Could not parse reminder property: " + reminder.serialize());
        }
    }

    @Test
    public void testFileCondition() {
        final boolean hasFile = RandomHelper.getRandomboolean();

        final SpecialListsFileProperty file = new SpecialListsFileProperty(hasFile);
        assertEquals("File status not set correctly", hasFile, file.isSet());

        final MirakelQueryBuilder qb = file.getWhereQueryBuilder(Robolectric.application);
        assertEquals("Query does not match", (hasFile ? "" : "NOT ") + "_id IN (SELECT task_id FROM files)",
                     qb.getSelection().trim());
        assertEquals("Argument count does not match", 0L, qb.getSelectionArguments().size());

        final Optional<SpecialListsBaseProperty> newFile = SpecialListsWhereDeserializer.deserializeWhere(
                    file.serialize(), "Test");
        if (newFile.isPresent() && (newFile.get() instanceof SpecialListsFileProperty)) {
            final SpecialListsFileProperty file2 = (SpecialListsFileProperty)newFile.get();
            assertEquals("File state does not match after parsing", file.isSet(), file2.isSet());
        } else {
            fail("Could not parse reminder property: " + file.serialize());
        }
    }

    @Test
    public void testDueExistsCondition() {
        final boolean hasDue = RandomHelper.getRandomboolean();

        final SpecialListsDueExistsProperty due = new SpecialListsDueExistsProperty(hasDue);
        assertEquals("Due status not set correctly", hasDue, due.isSet());

        final MirakelQueryBuilder qb = due.getWhereQueryBuilder(Robolectric.application);
        assertEquals("Query does not match", hasDue ? "due IS NULL" : "due IS NOT NULL",
                     qb.getSelection().trim());
        assertEquals("Argument count does not match", 0L, qb.getSelectionArguments().size());

        final Optional<SpecialListsBaseProperty> newDue = SpecialListsWhereDeserializer.deserializeWhere(
                    due.serialize(), "Test");
        if (newDue.isPresent() && (newDue.get() instanceof SpecialListsDueExistsProperty)) {
            final SpecialListsDueExistsProperty due2 = (SpecialListsDueExistsProperty)newDue.get();
            assertEquals("Due state does not match after parsing", due.isSet(), due2.isSet());
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
        assertEquals("Search string not set correctly", searchString, content.getSearchString());
        assertEquals("Search type not set correctly", type, content.getType());
        assertEquals("Search negated  not set correctly", negated, content.isSet());

        final MirakelQueryBuilder qb = content.getWhereQueryBuilder(Robolectric.application);
        assertEquals("Query does not match", (negated ? "NOT " : "") + "content LIKE ?",
                     qb.getSelection().trim());
        assertEquals("Argument count does not match", 1L, qb.getSelectionArguments().size());
        switch (type) {
        case BEGIN:
            assertEquals("Argument does not match", searchString + '%', qb.getSelectionArguments().get(0));
            break;
        case END:
            assertEquals("Argument does not match", '%' + searchString, qb.getSelectionArguments().get(0));
            break;
        case CONTAINS:
            assertEquals("Argument does not match", '%' + searchString + '%',
                         qb.getSelectionArguments().get(0));
            break;
        }

        final Optional<SpecialListsBaseProperty> newContent =
            SpecialListsWhereDeserializer.deserializeWhere(content.serialize(), "Test");
        if (newContent.isPresent() && (newContent.get() instanceof SpecialListsContentProperty)) {
            final SpecialListsContentProperty content2 = (SpecialListsContentProperty)newContent.get();
            assertEquals("Search string does not match after parsing", content.getSearchString(),
                         content2.getSearchString());
            assertEquals("Search type does not match after parsing", content.getType(), content2.getType());
            assertEquals("Search negated does not match after parsing", content.isSet(), content2.isSet());
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
        assertEquals("Search string not set correctly", searchString, name.getSearchString());
        assertEquals("Search type not set correctly", type, name.getType());
        assertEquals("Search negated  not set correctly", negated, name.isSet());

        final MirakelQueryBuilder qb = name.getWhereQueryBuilder(Robolectric.application);
        assertEquals("Query does not match", (negated ? "NOT " : "") + "name LIKE ?",
                     qb.getSelection().trim());
        assertEquals("Argument count does not match", 1L, qb.getSelectionArguments().size());
        switch (type) {
        case BEGIN:
            assertEquals("Argument does not match", searchString + '%', qb.getSelectionArguments().get(0));
            break;
        case END:
            assertEquals("Argument does not match", '%' + searchString, qb.getSelectionArguments().get(0));
            break;
        case CONTAINS:
            assertEquals("Argument does not match", '%' + searchString + '%',
                         qb.getSelectionArguments().get(0));
            break;
        }

        final Optional<SpecialListsBaseProperty> newName = SpecialListsWhereDeserializer.deserializeWhere(
                    name.serialize(), "Test");
        if (newName.isPresent() && (newName.get() instanceof SpecialListsNameProperty)) {
            final SpecialListsNameProperty name2 = (SpecialListsNameProperty)newName.get();
            assertEquals("Search string does not match after parsing", name.getSearchString(),
                         name2.getSearchString());
            assertEquals("Search type does not match after parsing", name.getType(), name2.getType());
            assertEquals("Search negated does not match after parsing", name.isSet(), name2.isSet());
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
        assertEquals("Search string not set correctly", searchString, listName.getSearchString());
        assertEquals("Search type not set correctly", type, listName.getType());
        assertEquals("Search negated  not set correctly", negated, listName.isSet());

        final MirakelQueryBuilder qb = listName.getWhereQueryBuilder(Robolectric.application);
        assertEquals("Query does not match",
                     "list_id IN (SELECT _id FROM lists WHERE " + (negated ? "NOT " : "") + "lists.name LIKE ?)",
                     qb.getSelection().trim());
        assertEquals("Argument count does not match", 1L, qb.getSelectionArguments().size());
        switch (type) {
        case BEGIN:
            assertEquals("Argument does not match", searchString + '%', qb.getSelectionArguments().get(0));
            break;
        case END:
            assertEquals("Argument does not match", '%' + searchString, qb.getSelectionArguments().get(0));
            break;
        case CONTAINS:
            assertEquals("Argument does not match", '%' + searchString + '%',
                         qb.getSelectionArguments().get(0));
            break;
        }

        final Optional<SpecialListsBaseProperty> newListName =
            SpecialListsWhereDeserializer.deserializeWhere(listName.serialize(), "Test");
        if (newListName.isPresent() && (newListName.get() instanceof SpecialListsListNameProperty)) {
            final SpecialListsListNameProperty listName2 = (SpecialListsListNameProperty)newListName.get();
            assertEquals("Search string does not match after parsing", listName.getSearchString(),
                         listName2.getSearchString());
            assertEquals("Search type does not match after parsing", listName.getType(), listName2.getType());
            assertEquals("Search negated does not match after parsing", listName.isSet(), listName2.isSet());
        } else {
            fail("Could not parse list name property: " + listName.serialize());
        }
    }

    @Test
    public void testDueCondition() {
        final boolean negated = RandomHelper.getRandomboolean();
        final SpecialListsDueProperty.Unit unit = RandomHelper.getRandomUnit();
        final int length = Math.abs(RandomHelper.getRandomint());

        final SpecialListsDueProperty due = new SpecialListsDueProperty(unit, length, negated);
        assertEquals("Unit not set correctly", unit, due.getUnit());
        assertEquals("Negated not set correctly", negated, due.isSet());
        assertEquals("Length not set correctly", length, due.getLength());

        final MirakelQueryBuilder qb = due.getWhereQueryBuilder(Robolectric.application);
        final Calendar date = new GregorianCalendar();
        date.setTimeZone(TimeZone.getTimeZone(TimeZone.getAvailableIDs(0)[0]));
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.HOUR, 0);
        switch (unit) {
        case DAY:
            date.add(Calendar.DAY_OF_MONTH, length);
            break;
        case MONTH:
            date.add(Calendar.MONTH, length);
            break;
        case YEAR:
            date.add(Calendar.YEAR, length);
            break;
        }
        assertEquals("Query does not match", "due IS NOT NULL  AND due " + (negated ? '>' : '<') + " ?",
                     qb.getSelection().trim());
        assertEquals("Argument count does not match", 1L, qb.getSelectionArguments().size());
        assertEquals("Argument does not match", String.valueOf(date.getTimeInMillis() / 1000L),
                     qb.getSelectionArguments().get(0));

        final Optional<SpecialListsBaseProperty> newDue = SpecialListsWhereDeserializer.deserializeWhere(
                    due.serialize(), "Test");
        if (newDue.isPresent() && (newDue.get() instanceof SpecialListsDueProperty)) {
            final SpecialListsDueProperty due2 = (SpecialListsDueProperty)newDue.get();
            assertEquals("Unit does not match", due.getUnit(), due2.getUnit());
            assertEquals("Negated does not match", due.isSet(), due2.isSet());
            assertEquals("Length does not match", due.getLength(), due2.getLength());
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
        assertEquals("Negation not set correctly", negated, list.isSet());
        assertTrue("List ids not set correctly", TestHelper.listEquals(allIds, list.getContent()));

        final MirakelQueryBuilder qb = list.getWhereQueryBuilder(Robolectric.application);

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

        String query = " list_id IN (" + TextUtils.join(",", Collections2.transform(lists,
        new Function<Integer, String>() {
            @Override
            public String apply(final Integer input) {
                return "?";
            }
        })) + ')';
        if (!special.isEmpty()) {
            query += " OR " + specialWhere;
        }

        if (negated) {
            query = "NOT (" + query + ')';
        }

        assertEquals("Query does not match", query, qb.getSelection());
        assertEquals("Argument count does not match", arguments.size(), qb.getSelectionArguments().size());
        assertTrue("Argument list does not match: <" + qb.getSelectionArguments() + "> expected <" +
                   arguments + '>', TestHelper.listEquals(arguments, qb.getSelectionArguments()));

        final Optional<SpecialListsBaseProperty> newList = SpecialListsWhereDeserializer.deserializeWhere(
                    list.serialize(), "Test");
        if (newList.isPresent() && (newList.get() instanceof SpecialListsListProperty)) {
            final SpecialListsListProperty list2 = (SpecialListsListProperty)newList.get();
            assertEquals("Negation does not match", list.isSet(), list2.isSet());
            assertTrue("List ids does not match", TestHelper.listEquals(list.getContent(), list2.getContent()));
        } else {
            fail("Could not parse list property: " + list.serialize());
        }
    }

    public void testPriotityCondition() {
        final boolean negated = RandomHelper.getRandomboolean();
        final List<Integer> prios = new ArrayList<>(5);
        for (int i = -2; i < 3; i++) {
            if (RandomHelper.getRandomboolean()) {
                prios.add(i);
            }
        }

        final SpecialListsPriorityProperty prio = new SpecialListsPriorityProperty(negated, prios);
        assertEquals("Negation not set correctly", negated, prio.isSet());
        assertTrue("Priorities not set correctly", TestHelper.listEquals(prios, prio.getContent()));

        final MirakelQueryBuilder qb = prio.getWhereQueryBuilder(Robolectric.application);
        String query = " priority IN (" + TextUtils.join(",", Collections2.transform(prios,
        new Function<Integer, String>() {
            @Override
            public String apply(final Integer input) {
                return "?";
            }
        })) + ')';
        assertEquals("Query does not match", query, qb.getSelection());
        assertEquals("Argument count does not match", prios.size(), qb.getSelectionArguments().size());
        assertTrue("Argument list does not match: <" + qb.getSelectionArguments() + "> expected <" + prios +
                   '>', TestHelper.listEquals(new ArrayList<String>(Collections2.transform(prios,
        new Function<Integer, String>() {
            @Override
            public String apply(final Integer input) {
                return String.valueOf(input);
            }
        })), qb.getSelectionArguments()));

        final Optional<SpecialListsBaseProperty> newPrio = SpecialListsWhereDeserializer.deserializeWhere(
                    prio.serialize(), "Test");
        if (newPrio.isPresent() && (newPrio.get() instanceof SpecialListsPriorityProperty)) {
            final SpecialListsPriorityProperty prio2 = (SpecialListsPriorityProperty)newPrio.get();
            assertEquals("Negation does not match", prio.isSet(), prio2.isSet());
            assertTrue("Priorities does not match", TestHelper.listEquals(prio.getContent(),
                       prio2.getContent()));
        } else {
            fail("Could not parse priority property: " + prio.serialize());
        }
    }

    @Test
    public void testProgressCondition() {
        final int value = RandomHelper.getRandomint(100);
        final SpecialListsProgressProperty.OPERATION op = RandomHelper.getRandomOperation();

        final SpecialListsProgressProperty progress = new SpecialListsProgressProperty(value, op);
        assertEquals("Value not set correctly", value, progress.getValue());
        assertEquals("Operation not set correctly", op, progress.getOperation());

        final MirakelQueryBuilder qb = progress.getWhereQueryBuilder(Robolectric.application);
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
        assertEquals("Query does not match", "progress " + operation + " ?", qb.getSelection().trim());
        assertEquals("Argument count does not match", 1L, qb.getSelectionArguments().size());
        assertEquals("Argument does not match", String.valueOf(value), qb.getSelectionArguments().get(0));

        final Optional<SpecialListsBaseProperty> newProgress =
            SpecialListsWhereDeserializer.deserializeWhere(progress.serialize(), "Test");
        if (newProgress.isPresent() && (newProgress.get() instanceof SpecialListsProgressProperty)) {
            final SpecialListsProgressProperty progress2 = (SpecialListsProgressProperty)newProgress.get();
            assertEquals("Value does not match after parsing", progress.getValue(), progress2.getValue());
            assertEquals("Operation does not match after parsing", progress.getOperation(),
                         progress2.getOperation());
        } else {
            fail("Could not parse progress property: " + progress.serialize());
        }
    }

    @Test
    public void testSubtaskCondition() {
        final boolean negated = RandomHelper.getRandomboolean();
        final boolean isParent = RandomHelper.getRandomboolean();

        final SpecialListsSubtaskProperty subtask = new SpecialListsSubtaskProperty(negated, isParent);
        assertEquals("Negated not set correctly", negated, subtask.isSet());
        assertEquals("Parent not set correctly", isParent, subtask.isParent());

        final MirakelQueryBuilder qb = subtask.getWhereQueryBuilder(Robolectric.application);
        assertEquals("Query does not match",
                     (negated ? "NOT " : "") + "_id IN (SELECT DISTINCT " + (isParent ? "parent_id" : "child_id") +
                     " FROM subtasks)" , qb.getSelection().trim());
        assertEquals("Argument count does not match", 0L, qb.getSelectionArguments().size());

        final Optional<SpecialListsBaseProperty> newSubtask =
            SpecialListsWhereDeserializer.deserializeWhere(subtask.serialize(), "Test");
        if (newSubtask.isPresent() && (newSubtask.get() instanceof SpecialListsSubtaskProperty)) {
            final SpecialListsSubtaskProperty subtask2 = (SpecialListsSubtaskProperty)newSubtask.get();
            assertEquals("Negated does not match after parsing", subtask.isSet(), subtask2.isSet());
            assertEquals("Parent does not match after parsing", subtask.isParent(), subtask2.isParent());
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
        assertEquals("Negated not set correctly", negated, tag.isSet());
        assertTrue("Tagids not set correctly", TestHelper.listEquals(tagIds, tag.getContent()));

        final MirakelQueryBuilder qb = tag.getWhereQueryBuilder(Robolectric.application);
        String query = (negated ? "NOT " : "") +
                       "_id IN (SELECT DISTINCT task_id FROM task_tag WHERE tag_id IN(";
        query += TextUtils.join(",", Collections2.transform(tagIds, new Function<Integer, String>() {
            @Override
            public String apply(final Integer input) {
                return "?";
            }
        }));
        query += "))";
        assertEquals("Query does not match", query, qb.getSelection().trim());
        assertEquals("Argument count does not match", tagCount, qb.getSelectionArguments().size());
        assertTrue("Arguments does not match",
                   TestHelper.listEquals(new ArrayList<String>(Collections2.transform(tagIds,
        new Function<Integer, String>() {
            @Override
            public String apply(final Integer input) {
                return String.valueOf(input);
            }
        })), qb.getSelectionArguments()));

        final Optional<SpecialListsBaseProperty> newTag = SpecialListsWhereDeserializer.deserializeWhere(
                    tag.serialize(), "Test");
        if (newTag.isPresent() && (newTag.get() instanceof SpecialListsTagProperty)) {
            final SpecialListsTagProperty tag2 = (SpecialListsTagProperty)newTag.get();
            assertEquals("Negated does not match after parsing", tag.isSet(), tag2.isSet());
            assertTrue("Tagids does not match after parsing", TestHelper.listEquals(tag.getContent(),
                       tag2.getContent()));
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

        assertEquals("Conjunction root operation not set correctly", CONJUNCTION.AND,
                     conjunctionRoot.getOperation());
        assertEquals("Conjunction child operation not set correctly", CONJUNCTION.OR,
                     conjunctionChild.getOperation());
        assertEquals("Conjunction leaf operation not set correctly", CONJUNCTION.AND,
                     conjunctionLeaf.getOperation());


        assertEquals("Conjunction root childcount does not match", 2L, conjunctionRoot.getChilds().size());
        assertTrue("First root child is no done property",
                   conjunctionRoot.getChilds().get(0) instanceof SpecialListsDoneProperty);
        assertEquals("First dummychild does not match in root", negated,
                     ((SpecialListsDoneProperty) conjunctionRoot.getChilds().get(0)).isSet());
        assertTrue("Second root child is no conjunction property",
                   conjunctionRoot.getChilds().get(1) instanceof SpecialListsConjunctionList);

        final SpecialListsConjunctionList child = (SpecialListsConjunctionList)
                conjunctionRoot.getChilds().get(1);
        assertEquals("Conjunction child childcount does not match (from root)", 2L,
                     child.getChilds().size());
        assertEquals("Conjunction child operation not set correctly (from root)", CONJUNCTION.OR,
                     child.getOperation());
        assertTrue("First child child is no done property (from root)",
                   child.getChilds().get(0) instanceof SpecialListsDoneProperty);
        assertEquals("First dummychild does not match in child (from root)", negated,
                     ((SpecialListsDoneProperty) child.getChilds().get(0)).isSet());
        assertTrue("Second child child is no conjunction property (from root)",
                   child.getChilds().get(1) instanceof SpecialListsConjunctionList);

        final SpecialListsConjunctionList leaf = (SpecialListsConjunctionList) child.getChilds().get(1);
        assertEquals("Conjunction leaf childcount does not match (from root)", 2L, leaf.getChilds().size());
        assertEquals("Conjunction leaf operation not set correctly (from root)", CONJUNCTION.AND,
                     leaf.getOperation());
        assertTrue("First leaf child is no done property (from root)",
                   leaf.getChilds().get(0) instanceof SpecialListsDoneProperty);
        assertEquals("First dummychild does not match in leaf (from root)", negated,
                     ((SpecialListsDoneProperty)leaf.getChilds().get(0)).isSet());
        assertTrue("Second leaf child is no done property (from root)",
                   leaf.getChilds().get(1) instanceof SpecialListsDoneProperty);
        assertEquals("Second dummychild does not match in leaf (from root)", negated,
                     ((SpecialListsDoneProperty)leaf.getChilds().get(1)).isSet());


        assertEquals("Conjunction child childcount does not match", 2L,
                     conjunctionChild.getChilds().size());
        assertTrue("First child child is no done property",
                   conjunctionChild.getChilds().get(0) instanceof SpecialListsDoneProperty);
        assertEquals("First dummychild does not match in child", negated,
                     ((SpecialListsDoneProperty) conjunctionChild.getChilds().get(0)).isSet());
        assertTrue("Second child child is no conjunction property",
                   conjunctionChild.getChilds().get(1) instanceof SpecialListsConjunctionList);

        SpecialListsConjunctionList childLeaf = (SpecialListsConjunctionList)
                                                conjunctionChild.getChilds().get(1);
        assertEquals("Conjunction leaf operation not set correctly (from child)", CONJUNCTION.AND,
                     childLeaf.getOperation());
        assertEquals("Conjunction leaf childcount does not match (from child)", 2L,
                     childLeaf.getChilds().size());
        assertTrue("First leaf child is no done property (from child)",
                   childLeaf.getChilds().get(0) instanceof SpecialListsDoneProperty);
        assertEquals("First dummychild does not match in leaf (from child)", negated,
                     ((SpecialListsDoneProperty)childLeaf.getChilds().get(0)).isSet());
        assertTrue("Second leaf child is no done property (from child)",
                   childLeaf.getChilds().get(1) instanceof SpecialListsDoneProperty);
        assertEquals("Second dummychild does not match in leaf (from child)", negated,
                     ((SpecialListsDoneProperty)childLeaf.getChilds().get(1)).isSet());


        assertEquals("Conjunction leaf childcount does not match", 2L, conjunctionLeaf.getChilds().size());
        assertTrue("First leaf child is no done property",
                   conjunctionLeaf.getChilds().get(0) instanceof SpecialListsDoneProperty);
        assertEquals("First dummychild does not match in leaf", negated,
                     ((SpecialListsDoneProperty) conjunctionLeaf.getChilds().get(0)).isSet());
        assertTrue("Second leaf child is no done property",
                   conjunctionLeaf.getChilds().get(1) instanceof SpecialListsDoneProperty);
        assertEquals("Second dummychild does not match in leaf", negated,
                     ((SpecialListsDoneProperty) conjunctionLeaf.getChilds().get(1)).isSet());


        final MirakelQueryBuilder qb = conjunctionRoot.getWhereQueryBuilder(Robolectric.application);
        String dummyQuery = '(' + dummyChild.getWhereQueryBuilder(Robolectric.application).getSelection() +
                            ')';
        final String query = dummyQuery + " AND (" + dummyQuery + " OR (" + dummyQuery + " AND " +
                             dummyQuery + "))";
        assertEquals("Query does not match", query, qb.getSelection().trim());
        assertEquals("Argument count does not match", 4L, qb.getSelectionArguments().size());
        for (String arg : qb.getSelectionArguments()) {
            assertEquals("Argument does not match", negated ? "1" : "0", arg);
        }

        final Optional<SpecialListsBaseProperty> newRoot = SpecialListsWhereDeserializer.deserializeWhere(
                    conjunctionRoot.serialize(), "Test");
        if (newRoot.isPresent() && (newRoot.get() instanceof SpecialListsConjunctionList)) {
            final SpecialListsConjunctionList root2 = (SpecialListsConjunctionList)newRoot.get();
            assertEquals("Conjunction root childcount does not match", conjunctionRoot.getChilds().size(),
                         root2.getChilds().size());
            assertTrue("First root child is no done property",
                       root2.getChilds().get(0) instanceof SpecialListsDoneProperty);
            assertEquals("First dummychild does not match in root",
                         ((SpecialListsDoneProperty) conjunctionRoot.getChilds().get(0)).isSet(),
                         ((SpecialListsDoneProperty) root2.getChilds().get(0)).isSet());
            assertTrue("Second root child is no conjunction property",
                       root2.getChilds().get(1) instanceof SpecialListsConjunctionList);

            final SpecialListsConjunctionList child1 = (SpecialListsConjunctionList) root2.getChilds().get(1);
            assertEquals("Conjunction child childcount does not match (from root)", child.getChilds().size(),
                         child1.getChilds().size());
            assertEquals("Conjunction child operation not set correctly (from root)", child.getOperation(),
                         child1.getOperation());
            assertTrue("First child child is no done property (from root)",
                       child1.getChilds().get(0) instanceof SpecialListsDoneProperty);
            assertEquals("First dummychild does not match in child (from root)",
                         ((SpecialListsDoneProperty) child.getChilds().get(0)).isSet(),
                         ((SpecialListsDoneProperty) child1.getChilds().get(0)).isSet());
            assertTrue("Second child child is no conjunction property (from root)",
                       child1.getChilds().get(1) instanceof SpecialListsConjunctionList);

            final SpecialListsConjunctionList leaf1 = (SpecialListsConjunctionList) child1.getChilds().get(1);
            assertEquals("Conjunction leaf childcount does not match (from root)", leaf.getChilds().size(),
                         leaf1.getChilds().size());
            assertEquals("Conjunction leaf operation not set correctly (from root)", leaf.getOperation(),
                         leaf1.getOperation());
            assertTrue("First leaf child is no done property (from root)",
                       leaf1.getChilds().get(0) instanceof SpecialListsDoneProperty);
            assertEquals("First dummychild does not match in leaf (from root)",
                         ((SpecialListsDoneProperty)leaf.getChilds().get(0)).isSet(),
                         ((SpecialListsDoneProperty)leaf1.getChilds().get(0)).isSet());
            assertTrue("Second leaf child is no done property (from root)",
                       leaf1.getChilds().get(1) instanceof SpecialListsDoneProperty);
            assertEquals("Second dummychild does not match in leaf (from root)",
                         ((SpecialListsDoneProperty)leaf.getChilds().get(1)).isSet(),
                         ((SpecialListsDoneProperty)leaf1.getChilds().get(1)).isSet());

        } else {
            fail("Could not parse tag property: " + conjunctionRoot.serialize());
        }
    }
}
