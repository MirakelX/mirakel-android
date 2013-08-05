package de.azapps.mirakel.helper;

public class JsonHelper {
	public static String addToJsonString(String key, boolean value,
			String oldJson) {
		return addToJsonString(key, value?"true":"false", false, oldJson);
	}

	public static String addToJsonString(String key, String value,
			String oldJson) {
		return addToJsonString(key, value, true, oldJson);
	}

	public static String addToJsonString(String key, long value, String oldJson) {
		return addToJsonString(key, value+"", false, oldJson);
	}

	private static String addToJsonString(String key, String value,
			boolean isStringValue, String oldJson) {
		if(oldJson.toCharArray()[oldJson.length()-1]!='{')
			oldJson+=",";
		oldJson+="\""+key+"\":";
		if(isStringValue)
			value="\""+value+"\"";
		oldJson+=value;
		return oldJson;
	}
}
