package de.azapps.mirakelandroid.test;

import org.junit.After;
import org.junit.Before;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowSQLiteConnection;

import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;

public abstract class MirakelDatabaseTestCase extends MirakelTestCase {

    public MirakelDatabaseTestCase() {
        super();
    }

    @Before
    public void setUp() throws Exception {
        openDatabase();
        super.setUp();
    }

    private void openDatabase() {
        DatabaseHelper.getDatabaseHelper(RuntimeEnvironment.application);
    }


    @After
    public void tearDown() {
        super.tearDown();
        resetDatabase();
        ShadowSQLiteConnection.reset();
    }

    public void resetDatabase() {
        MirakelInternalContentProvider.reset();
        DatabaseHelper.resetDB();
    }
}
