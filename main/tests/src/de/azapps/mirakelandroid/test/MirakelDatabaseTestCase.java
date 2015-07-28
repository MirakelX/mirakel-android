package de.azapps.mirakelandroid.test;

import android.content.Context;

import org.junit.Before;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.File;

import de.azapps.mirakel.model.MirakelInternalContentProvider;

public abstract class MirakelDatabaseTestCase extends MirakelTestCase {

    public Context context;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context = RuntimeEnvironment.application;

        resetDatabase();
        MirakelInternalContentProvider provider = new MirakelInternalContentProvider();
        provider.onCreate();
        ShadowContentResolver.registerProvider("de.azapps.mirakel.provider.internal", provider);
    }

    public void resetDatabase() {
        File dbFile = context.getDatabasePath(null);
        if (dbFile.exists()) {
            context.deleteDatabase(dbFile.getAbsolutePath());
        }
    }
}
