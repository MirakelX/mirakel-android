package de.azapps.mirakel.model;

import android.database.MatrixCursor;
import android.os.Parcel;

import com.google.common.base.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.truth.Truth.assertThat;


@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class ParceableTest extends MirakelDatabaseTestCase {

    @Test
    public void testTaskParcelable() {
        final ListMirakel list = RandomHelper.getRandomListMirakel();
        final Task task = new Task(RandomHelper.getRandomString(), list, RandomHelper.getRandomString(),
                                   RandomHelper.getRandomboolean(), RandomHelper.getRandomOptional_DateTime(),
                                   RandomHelper.getRandomPriority());
        final Parcel parcel = Parcel.obtain();
        task.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Task parceledTask = Task.CREATOR.createFromParcel(parcel);
        assertThat(task).isEqualTo(parceledTask);
    }

    @Test
    public void testTagParcelable() {
        final Tag tag = new Tag(RandomHelper.getRandomint(), RandomHelper.getRandomString(),
                                RandomHelper.getRandomint(), RandomHelper.getRandomboolean());
        final Parcel parcel = Parcel.obtain();
        tag.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        final Tag parceledTag = Tag.CREATOR.createFromParcel(parcel);
        assertThat(tag).isEqualTo(parceledTag);
    }

    @Test
    public void testListParcelable() {
        final ListMirakel list = RandomHelper.getRandomListMirakel();
        final Parcel parcel = Parcel.obtain();
        list.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        final ListMirakel parceledList = ListMirakel.CREATOR.createFromParcel(parcel);
        assertThat(list).isEqualTo(parceledList);
    }
    @Test
    public void testSemanticParcelable() {
        final Semantic semantic = new Semantic(RandomHelper.getRandomint(), RandomHelper.getRandomString(),
                                               RandomHelper.getRandomPriority(), RandomHelper.getRandomInteger(), Optional.<ListMirakel>absent(),
                                               RandomHelper.getRandomInteger());
        final Parcel parcel = Parcel.obtain();
        semantic.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        final Semantic parceledSemantic = Semantic.CREATOR.createFromParcel(parcel);
        assertThat(semantic).isEqualTo(parceledSemantic);
    }

    @Test
    public void testRecurringParcelable() {
        final Recurring recurring = new Recurring(RandomHelper.getRandomlong(),
                RandomHelper.getRandomString(), RandomHelper.getRandomPeriod(),
                RandomHelper.getRandomboolean(), RandomHelper.getRandomOptional_DateTime(),
                RandomHelper.getRandomOptional_DateTime(), RandomHelper.getRandomboolean(),
                RandomHelper.getRandomboolean(), RandomHelper.getRandomSparseBooleanArray(),
                RandomHelper.getRandomOptional_Long());
        final Parcel parcel = Parcel.obtain();
        recurring.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        final Recurring parceledRecurring = Recurring.CREATOR.createFromParcel(parcel);
        assertThat(recurring).isEqualTo(parceledRecurring);
    }

    @Test
    public void testFileParcelable() {
        final Task t = RandomHelper.getRandomTask();
        final MatrixCursor c = new MatrixCursor(new String[] {ModelBase.ID, ModelBase.NAME, FileMirakel.TASK, FileMirakel.PATH});
        c.addRow(new Object[] {RandomHelper.getRandomInteger(), RandomHelper.getRandomString(), t.getId(), RandomHelper.getRandomUri()});
        c.moveToFirst();
        final FileMirakel fileMirakel = new FileMirakel(CursorGetter.unsafeGetter(c));
        final Parcel parcel = Parcel.obtain();
        fileMirakel.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        final FileMirakel parceledFile = FileMirakel.CREATOR.createFromParcel(parcel);
        assertThat(fileMirakel).isEqualTo(parceledFile);
    }

    @Test
    public void testAccountParcelable() {
        final AccountMirakel account = new AccountMirakel(RandomHelper.getRandomint(),
                RandomHelper.getRandomString(), RandomHelper.getRandomACCOUNT_TYPES(),
                RandomHelper.getRandomboolean(), RandomHelper.getRandomOptional_String());
        final Parcel parcel = Parcel.obtain();
        account.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        final AccountMirakel parceledAccount = AccountMirakel.CREATOR.createFromParcel(parcel);
        assertThat(account).isEqualTo(parceledAccount);
    }

    @Test
    public void testSpecialListParcelable() {
        MatrixCursor c = new MatrixCursor(new String[] {ModelBase.ID,
                                          ModelBase.NAME,
                                          SpecialList.WHERE_QUERY,
                                          SpecialList.ACTIVE,
                                          SpecialList.DEFAULT_LIST,
                                          SpecialList.DEFAULT_DUE,
                                          SpecialList.SORT_BY_FIELD,
                                          DatabaseHelper.SYNC_STATE_FIELD,
                                          SpecialList.COLOR,
                                          SpecialList.LFT,
                                          SpecialList.RGT,
                                          SpecialList.ICON_PATH ,
                                          DatabaseHelper.CREATED_AT,
                                          DatabaseHelper.UPDATED_AT,
                                          ListMirakel.ACCOUNT_ID,
                                          ListMirakel.IS_SPECIAL
                                                       });
        final int id = RandomHelper.getRandomint();
        c.addRow(new Object[] {id, //ModelBase.ID,
                               RandomHelper.getRandomString(),//ModelBase.NAME
                               " ",//SpecialList.WHERE_QUERY
                               RandomHelper.getRandomboolean() ? 0 : 1, //SpecialList.ACTIVE
                               RandomHelper.getRandomint(),//SpecialList.DEFAULT_LIST,
                               RandomHelper.getRandomOptional_Integer().orNull(),//SpecialList.DEFAULT_DUE,
                               RandomHelper.getRandomSORT_BY().getShort(),//SpecialList.SORT_BY_FIELD,
                               RandomHelper.getRandomSYNC_STATE().toInt(),//DatabaseHelper.SYNC_STATE_FIELD,
                               RandomHelper.getRandomint(),//SpecialList.COLOR,
                               RandomHelper.getRandomint(),//SpecialList.LFT,
                               RandomHelper.getRandomint(),//SpecialList.RGT,
                               RandomHelper.getRandomOptional_Uri().orNull(),//SpecialList.ICON_PATH
                               RandomHelper.getRandomDateTime().getMillis(),//CREATED_AT
                               RandomHelper.getRandomDateTime().getMillis(),//UPDATED_AT
                               RandomHelper.getRandomAccountMirakel().getId(),//ACCOUNT_ID
                               1
                              });
        c.moveToFirst();
        SpecialList special = new SpecialList(CursorGetter.unsafeGetter(c));

        final Parcel parcel = Parcel.obtain();
        special.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        final SpecialList parceledSpecial = SpecialList.CREATOR.createFromParcel(parcel);
        assertThat(special).isEqualTo(parceledSpecial);
    }

}
