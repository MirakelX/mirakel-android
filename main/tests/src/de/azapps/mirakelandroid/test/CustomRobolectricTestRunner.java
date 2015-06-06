package de.azapps.mirakelandroid.test;


import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.SdkConfig;
import org.robolectric.internal.SdkEnvironment;
import org.robolectric.internal.bytecode.ClassInfo;
import org.robolectric.internal.bytecode.Setup;

import java.util.Properties;

import de.azapps.mirakel.model.MirakelInternalContentProvider;

/** Enable shadows for our own application classes. */
public class CustomRobolectricTestRunner extends RobolectricTestRunner {

    public CustomRobolectricTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected ClassLoader createRobolectricClassLoader(Setup setup, SdkConfig sdkConfig) {
        return super.createRobolectricClassLoader(new ExtraShadows(setup), sdkConfig);
    }

    //protected SdkConfig pickSdkVersion(AndroidManifest appManifest, Config config) {
    //Properties properties = new Properties();
    //properties.setProperty("emulateSdk", "18");
    //Config.Implementation implementation = new Config.Implementation(config, Config.Implementation.fromProperties(properties));
//        return super.pickSdkVersion(appManifest, config);
    //  }

    protected void configureShadows(SdkEnvironment sdkEnvironment, Config config) {
        Properties properties = new Properties();
        properties.setProperty("shadows", CustomShadowApplication.class.getName());
        Config.Implementation implementation = new Config.Implementation(config,
                Config.Implementation.fromProperties(properties));
        super.configureShadows(sdkEnvironment, implementation);
    }

    class ExtraShadows extends Setup {
        private Setup setup;

        public ExtraShadows(Setup setup) {
            this.setup = setup;
        }

        public boolean shouldInstrument(ClassInfo classInfo) {
            boolean shoudInstrument = setup.shouldInstrument(classInfo);
            return shoudInstrument ||
                   classInfo.getName().equals(MirakelInternalContentProvider.class.getName());
        }
    }
}