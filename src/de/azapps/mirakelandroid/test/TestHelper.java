package de.azapps.mirakelandroid.test;

import android.content.Context;

import java.util.Collection;
import java.util.List;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.model.semantic.Semantic;

public class TestHelper {
	public static void init(final Context ctx) {
		Mirakel.init(ctx);
		Semantic.init(ctx);
	}

	public static void terminate() {
	}

	/**
	 * 'cause Java is to dumb to do the simplest things…
	 * @param a
	 * @param b
	 * @param <T>
	 * @return
	 */
	public static <T> boolean listEquals(List<T> a, List<T> b) {
		if(a==null || b==null) {
			if (a != b) {
				return false;
			}
		} else if (a.size() != b.size()) {
			return false;
		} else {
			for(int i=0; i<a.size(); i++) {
				T ia = a.get(i); // a donkey!
				T ib = b.get(i);
				if (ia == null) {
					if (ib != null) {
						return false;
					}
				} else if(!ia.equals(ib)) {
					return false;
				}
			}
		}
		return true;
	}
}
