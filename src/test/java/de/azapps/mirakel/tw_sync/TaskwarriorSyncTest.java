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
package de.azapps.mirakel.tw_sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.nio.charset.MalformedInputException;
import java.util.List;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSync;
import de.azapps.mirakel.sync.taskwarrior.network_helper.Msg;
import de.azapps.mirakel.sync.taskwarrior.services.SyncAdapter;
import de.azapps.mirakel.sync.taskwarrior.utilities.TaskWarriorAccount;
import de.azapps.mirakel.sync.taskwarrior.utilities.TaskWarriorSyncFailedException;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class TaskwarriorSyncTest extends MirakelDatabaseTestCase {

    private AccountMirakel account;
    private ListMirakel list;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Bundle b = new Bundle();
        b.putString(SyncAdapter.BUNDLE_SERVER_TYPE, TaskWarriorSync.TYPE);
        final Account a = new Account("test",
                                      AccountMirakel.ACCOUNT_TYPE_MIRAKEL);
        b.putString(SyncAdapter.BUNDLE_ORG, "org");
        b.putString(SyncAdapter.BUNDLE_SERVER_URL, "server");
        b.putString(DefinitionsHelper.BUNDLE_CERT, "cert");
        b.putString(DefinitionsHelper.BUNDLE_CERT_CLIENT, "clientkey");
        AccountManager.get(RuntimeEnvironment.application).addAccountExplicitly(a,
                "foo\n:barnentantcltbatcntalvinrecilceitrcp", b);
        account = AccountMirakel.newAccount("test", AccountMirakel.ACCOUNT_TYPES.TASKWARRIOR, true);
        list = ListMirakel.newList("foo", ListMirakel.SORT_BY.DUE, account);

    }

    @Test
    public void fullInsertTest1() {
        final Msg m = new Msg();
        try {
            final String input = "type: sync\n" +
                                 "org: <organization>\n" +
                                 "user: <user>\n" +
                                 "key: <key>\n" +
                                 "client: task 2.3.0\n" +
                                 "protocol: v1\n" +
                                 "\n" +
                                 "2e4685f8-34bc-4f9b-b7ed-399388e182e1\n" +
                                 "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed790b\"," +
                                 "\"status\":\"pending\",\"entry\":\"20140312T123933Z\"," +
                                 "\"description\":\"foordnd\"," +
                                 "\"due\":\"20140501T000000Z\"," +
                                 "\"project\":\"ni\", " +
                                 "\"priority\":\"M\"," +
                                 "\"modified\":\"20140502T130929Z\"," +
                                 "\"tags\":[\"New_Tag\",\"bin\"]}";
            m.parse(input);
        } catch (MalformedInputException e) {
            fail(e.getMessage());
        }

        assertThat(m.getHeader("type")).hasValue("sync");
        assertThat(m.getHeader("org")).hasValue("<organization>");
        assertThat(m.getHeader("user")).hasValue("<user>");
        assertThat(m.getHeader("key")).hasValue("<key>");
        assertThat(m.getHeader("client")).hasValue("task 2.3.0");
        assertThat(m.getHeader("protocol")).hasValue("v1");
        assertThat(m.getPayload()).isEqualTo("2e4685f8-34bc-4f9b-b7ed-399388e182e1\n" +
                                             "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed790b\"," +
                                             "\"status\":\"pending\",\"entry\":\"20140312T123933Z\"," +
                                             "\"description\":\"foordnd\"," +
                                             "\"due\":\"20140501T000000Z\"," +
                                             "\"project\":\"ni\", " +
                                             "\"priority\":\"M\"," +
                                             "\"modified\":\"20140502T130929Z\"," +
                                             "\"tags\":[\"New_Tag\",\"bin\"]}");
        final TaskWarriorSync sync = new TaskWarriorSync(RuntimeEnvironment.application);
        final TaskWarriorAccount tw_account = new TaskWarriorAccount(account,
                RuntimeEnvironment.application);
        try {
            sync.parseTasks(tw_account, m);
        } catch (TaskWarriorSyncFailedException e) {
            fail(e.getMessage());
        }
        final Optional<Task> t = Task.getByUUID("5e9682e0-4e1a-491a-a7cb-8097c9ed790b");
        assertThat(t).isPresent();
        assertThat(t.get().getName()).isEqualTo("foordnd");
        assertThat(t.get().getList().getName()).isEqualTo("ni");
        assertThat(t.get().getPriority()).isEqualTo(1L);
        assertThat(t.get().getTags()).hasSize(2);
        assertThat(t.get().getTags()).containsNoDuplicates();
        for (final Tag tag : t.get().getTags()) {
            assertThat(tag.getName()).isAnyOf("New Tag", "bin");
        }
        assertThat(t.get().getDue()).isPresent();
        assertThat(t.get().getDue().get()).isEquivalentAccordingToCompareTo(new DateTime(2014, 5, 1, 0, 0,
                0, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));
        assertThat(t.get().getCreatedAt()).isEquivalentAccordingToCompareTo(new DateTime(2014, 3, 12, 12,
                39, 33, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));
        assertThat(t.get().getUpdatedAt()).isEquivalentAccordingToCompareTo(new DateTime(2014, 5, 2, 13, 9,
                29, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));
    }

    @Test
    public void fullInsertTest2() {
        final Msg m = new Msg();
        try {
            final String input = "type: sync\n" +
                                 "org: <organization>\n" +
                                 "user: <user>\n" +
                                 "key: <key>\n" +
                                 "client: task 2.3.0\n" +
                                 "protocol: v1\n" +
                                 '\n' +
                                 "2e4685f8-34bc-4f9b-b7ed-399388e182e1\n" +
                                 "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed790b\"," +
                                 "\"status\":\"pending\",\"entry\":\"20140312T123933Z\"," +
                                 "\"description\":\"foordnd\"," +
                                 "\"due\":\"20140501T000000Z\"," +
                                 "\"project\":\"ni\", " +
                                 "\"priority\":\"M\"," +
                                 "\"modified\":\"20140502T130929Z\"," +
                                 "\"tags\":[\"New_Tag\",\"bin\"]}\n" +
                                 "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed7902\"," +
                                 "\"status\":\"pending\",\"entry\":\"20140312T123933Z\"," +
                                 "\"description\":\"foordnd\"," +
                                 "\"due\":\"20140501T000000Z\"," +
                                 "\"project\":\"ni\", " +
                                 "\"priority\":\"M\"," +
                                 "\"modified\":\"20140502T130929Z\"," +
                                 "\"tags\":[\"New_Tag\",\"bin\"]}";
            m.parse(input);
        } catch (MalformedInputException e) {
            fail(e.getMessage());
        }

        assertThat(m.getHeader("type")).hasValue("sync");
        assertThat(m.getHeader("org")).hasValue("<organization>");
        assertThat(m.getHeader("user")).hasValue("<user>");
        assertThat(m.getHeader("key")).hasValue("<key>");
        assertThat(m.getHeader("client")).hasValue("task 2.3.0");
        assertThat(m.getHeader("protocol")).hasValue("v1");
        assertThat(m.getPayload()).isEqualTo("2e4685f8-34bc-4f9b-b7ed-399388e182e1\n" +
                                             "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed790b\"," +
                                             "\"status\":\"pending\",\"entry\":\"20140312T123933Z\"," +
                                             "\"description\":\"foordnd\"," +
                                             "\"due\":\"20140501T000000Z\"," +
                                             "\"project\":\"ni\", " +
                                             "\"priority\":\"M\"," +
                                             "\"modified\":\"20140502T130929Z\"," +
                                             "\"tags\":[\"New_Tag\",\"bin\"]}\n" +
                                             "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed7902\"," +
                                             "\"status\":\"pending\",\"entry\":\"20140312T123933Z\"," +
                                             "\"description\":\"foordnd\"," +
                                             "\"due\":\"20140501T000000Z\"," +
                                             "\"project\":\"ni\", " +
                                             "\"priority\":\"M\"," +
                                             "\"modified\":\"20140502T130929Z\"," +
                                             "\"tags\":[\"New_Tag\",\"bin\"]}");
        final TaskWarriorSync sync = new TaskWarriorSync(RuntimeEnvironment.application);
        final TaskWarriorAccount tw_account = new TaskWarriorAccount(account,
                RuntimeEnvironment.application);
        try {
            sync.parseTasks(tw_account, m);
        } catch (TaskWarriorSyncFailedException e) {
            fail(e.getMessage());
        }
        final Optional<Task> t1 = Task.getByUUID("5e9682e0-4e1a-491a-a7cb-8097c9ed790b");
        assertThat(t1).isPresent();
        assertThat(t1.get().getName()).isEqualTo("foordnd");
        assertThat(t1.get().getList().getName()).isEqualTo("ni");
        assertThat(t1.get().getPriority()).isEqualTo(1L);
        assertThat(t1.get().getTags()).hasSize(2);
        assertThat(t1.get().getTags()).containsNoDuplicates();
        for (final Tag tag : t1.get().getTags()) {
            assertThat(tag.getName()).isAnyOf("New Tag", "bin");
        }
        assertThat(t1.get().getDue()).isPresent();
        assertThat(t1.get().getDue().get()).isEquivalentAccordingToCompareTo(new DateTime(2014, 5, 1, 0, 0,
                0, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));
        assertThat(t1.get().getCreatedAt()).isEquivalentAccordingToCompareTo(new DateTime(2014, 3, 12, 12,
                39, 33, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));
        assertThat(t1.get().getUpdatedAt()).isEquivalentAccordingToCompareTo(new DateTime(2014, 5, 2, 13, 9,
                29, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));

        final Optional<Task> t2 = Task.getByUUID("5e9682e0-4e1a-491a-a7cb-8097c9ed7902");
        assertThat(t2).isPresent();
        assertThat(t2.get().getName()).isEqualTo("foordnd");
        assertThat(t2.get().getList().getName()).isEqualTo("ni");
        assertThat(t2.get().getPriority()).isEqualTo(1L);
        assertThat(t2.get().getTags()).hasSize(2);
        assertThat(t2.get().getTags()).containsNoDuplicates();
        for (final Tag tag : t2.get().getTags()) {
            assertThat(tag.getName()).isAnyOf("New Tag", "bin");
        }
        assertThat(t2.get().getDue()).isPresent();
        assertThat(t2.get().getDue().get()).isEquivalentAccordingToCompareTo(new DateTime(2014, 5, 1, 0, 0,
                0, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));
        assertThat(t2.get().getCreatedAt()).isEquivalentAccordingToCompareTo(new DateTime(2014, 3, 12, 12,
                39, 33, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));
        assertThat(t2.get().getUpdatedAt()).isEquivalentAccordingToCompareTo(new DateTime(2014, 5, 2, 13, 9,
                29, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));
    }

    @Test
    public void fullUpdateTest1() {
        final Task task = Task.newTask("foo", RandomHelper.getRandomListMirakel());
        task.setUUID("5e9682e0-4e1a-491a-a7cb-8097c9ed790b");
        task.save();
        final Msg m = new Msg();
        try {
            final String input = "type: sync\n" +
                                 "org: <organization>\n" +
                                 "user: <user>\n" +
                                 "key: <key>\n" +
                                 "client: task 2.3.0\n" +
                                 "protocol: v1\n" +
                                 '\n' +
                                 "2e4685f8-34bc-4f9b-b7ed-399388e182e1\n" +
                                 "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed790b\"," +
                                 "\"status\":\"pending\",\"entry\":\"20140312T123933Z\"," +
                                 "\"description\":\"foordnd\"," +
                                 "\"due\":\"20140501T000000Z\"," +
                                 "\"project\":\"ni\", " +
                                 "\"priority\":\"M\"," +
                                 "\"modified\":\"20140502T130929Z\"," +
                                 "\"tags\":[\"New_Tag\",\"bin\"]}";
            m.parse(input);
        } catch (MalformedInputException e) {
            fail(e.getMessage());
        }

        assertThat(m.getHeader("type")).hasValue("sync");
        assertThat(m.getHeader("org")).hasValue("<organization>");
        assertThat(m.getHeader("user")).hasValue("<user>");
        assertThat(m.getHeader("key")).hasValue("<key>");
        assertThat(m.getHeader("client")).hasValue("task 2.3.0");
        assertThat(m.getHeader("protocol")).hasValue("v1");
        assertThat(m.getPayload()).isEqualTo("2e4685f8-34bc-4f9b-b7ed-399388e182e1\n" +
                                             "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed790b\"," +
                                             "\"status\":\"pending\",\"entry\":\"20140312T123933Z\"," +
                                             "\"description\":\"foordnd\"," +
                                             "\"due\":\"20140501T000000Z\"," +
                                             "\"project\":\"ni\", " +
                                             "\"priority\":\"M\"," +
                                             "\"modified\":\"20140502T130929Z\"," +
                                             "\"tags\":[\"New_Tag\",\"bin\"]}");
        final TaskWarriorSync sync = new TaskWarriorSync(RuntimeEnvironment.application);
        final TaskWarriorAccount tw_account = new TaskWarriorAccount(account,
                RuntimeEnvironment.application);
        try {
            sync.parseTasks(tw_account, m);
        } catch (TaskWarriorSyncFailedException e) {
            fail(e.getMessage());
        }
        final Optional<Task> t = Task.getByUUID("5e9682e0-4e1a-491a-a7cb-8097c9ed790b");
        assertThat(t).isPresent();
        assertThat(t.get().getName()).isEqualTo("foordnd");
        assertThat(t.get().getList().getName()).isEqualTo("ni");
        assertThat(t.get().getPriority()).isEqualTo(1L);
        assertThat(t.get().getTags()).hasSize(2);
        assertThat(t.get().getTags()).containsNoDuplicates();
        for (final Tag tag : t.get().getTags()) {
            assertThat(tag.getName()).isAnyOf("New Tag", "bin");
        }
        assertThat(t.get().getDue()).isPresent();
        assertThat(t.get().getDue().get()).isEquivalentAccordingToCompareTo(new DateTime(2014, 5, 1, 0, 0,
                0, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));
        assertThat(t.get().getCreatedAt()).isEquivalentAccordingToCompareTo(new DateTime(2014, 3, 12, 12,
                39, 33, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));
        assertThat(t.get().getUpdatedAt()).isEquivalentAccordingToCompareTo(new DateTime(2014, 5, 2, 13, 9,
                29, DateTimeZone.UTC).toDateTime(DateTimeZone.getDefault()));
    }

    @Test
    public void fullDeleteTest1() {
        final Task task = Task.newTask("foo", list);
        task.setUUID("5e9682e0-4e1a-491a-a7cb-8097c9ed790b");
        task.save();
        final Msg m = new Msg();
        try {
            final String input = "type: sync\n" +
                                 "org: <organization>\n" +
                                 "user: <user>\n" +
                                 "key: <key>\n" +
                                 "client: task 2.3.0\n" +
                                 "protocol: v1\n" +
                                 '\n' +
                                 "2e4685f8-34bc-4f9b-b7ed-399388e182e1\n" +
                                 "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed790b\"," +
                                 "\"status\":\"deleted\",\"entry\":\"20140312T123933Z\"," +
                                 "\"description\":\"foordnd\"," +
                                 "\"due\":\"20140501T000000Z\"," +
                                 "\"project\":\"ni\", " +
                                 "\"priority\":\"M\"," +
                                 "\"modified\":\"20140502T130929Z\"," +
                                 "\"tags\":[\"New_Tag\",\"bin\"]}";
            m.parse(input);
        } catch (MalformedInputException e) {
            fail(e.getMessage());
        }

        assertThat(m.getHeader("type")).hasValue("sync");
        assertThat(m.getHeader("org")).hasValue("<organization>");
        assertThat(m.getHeader("user")).hasValue("<user>");
        assertThat(m.getHeader("key")).hasValue("<key>");
        assertThat(m.getHeader("client")).hasValue("task 2.3.0");
        assertThat(m.getHeader("protocol")).hasValue("v1");
        assertThat(m.getPayload()).isEqualTo("2e4685f8-34bc-4f9b-b7ed-399388e182e1\n" +
                                             "{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed790b\"," +
                                             "\"status\":\"deleted\",\"entry\":\"20140312T123933Z\"," +
                                             "\"description\":\"foordnd\"," +
                                             "\"due\":\"20140501T000000Z\"," +
                                             "\"project\":\"ni\", " +
                                             "\"priority\":\"M\"," +
                                             "\"modified\":\"20140502T130929Z\"," +
                                             "\"tags\":[\"New_Tag\",\"bin\"]}");
        final TaskWarriorSync sync = new TaskWarriorSync(RuntimeEnvironment.application);
        final TaskWarriorAccount tw_account = new TaskWarriorAccount(account,
                RuntimeEnvironment.application);
        try {
            sync.parseTasks(tw_account, m);
        } catch (TaskWarriorSyncFailedException e) {
            fail(e.getMessage());
        }
        final Optional<Task> t = Task.getByUUID("5e9682e0-4e1a-491a-a7cb-8097c9ed790b");
        assertThat(t).isAbsent();
    }

    @Test
    public void getAddedTasksTest1() {
        final Task task = Task.newTask("foo", list);
        task.setUUID("5e9682e0-4e1a-491a-a7cb-8097c9ed790b");
        task.save();
        List<Task> tasks = Task.getTasksToSync(account);
        assertThat(tasks).hasSize(1);
        assertThat(tasks).containsExactly(task);
    }

    @Test
    public void getMessageTest1() {
        final Task task = Task.newTask("foo", list);
        task.setUUID("5e9682e0-4e1a-491a-a7cb-8097c9ed790b");
        task.save();
        List<Task> tasks = Task.getTasksToSync(account);
        final TaskWarriorSync sync = new TaskWarriorSync(RuntimeEnvironment.application);
        final TaskWarriorAccount tw_account = new TaskWarriorAccount(account,
                RuntimeEnvironment.application);
        try {
            Msg m = sync.getMsg(tw_account, tasks);
            assertThat(m.getHeader("protocol")).hasValue(TaskWarriorSync.TW_PROTOCOL_VERSION);
            assertThat(m.getHeader("type")).hasValue("sync");
            assertThat(m.getHeader("org")).hasValue(tw_account.getOrg());
            assertThat(m.getHeader("user")).hasValue(tw_account.getUser());
            assertThat(m.getHeader("key")).hasValue(tw_account.getUserPassword());

            String now = ISODateTimeFormat.basicDateTimeNoMillis().withZoneUTC().print(task.getCreatedAt());

            assertThat(m.getPayload().trim())
            .isEqualTo("{\"uuid\":\"5e9682e0-4e1a-491a-a7cb-8097c9ed790b\"," +
                       "\"status\":\"pending\"," +
                       "\"entry\":\"" + now + "\"," +
                       "\"description\":\"foo\"," +
                       "\"project\":\"foo\"," +
                       "\"modified\":\"" + now + "\"}");
        } catch (TaskWarriorSyncFailedException e) {
            throw new RuntimeException(e);
        }
    }
}
