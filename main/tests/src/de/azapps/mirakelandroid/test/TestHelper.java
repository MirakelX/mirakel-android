package de.azapps.mirakelandroid.test;

import android.content.Context;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.model.semantic.Semantic;

public class TestHelper {
	public static void init(final Context ctx) {
		Mirakel.init(ctx);
		Semantic.init(ctx);
	}

	public static void terminate() {

	}
}
