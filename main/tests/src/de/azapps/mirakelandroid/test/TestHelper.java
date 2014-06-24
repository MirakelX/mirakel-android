package de.azapps.mirakelandroid.test;

import android.content.Context;
import de.azapps.mirakel.Mirakel;

public class TestHelper {
	public static void init(final Context ctx) {
		Mirakel.init(ctx);
	}

	public static void terminate() {
		Mirakel.terminate();
	}
}
