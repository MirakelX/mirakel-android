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
	
	public static String getPart(String key, String json){
		String[] p=json.split(",");
		for(String e:p){
			if(e.contains(key)){
				if(e.charAt(0)=='{')
					e=e.substring(1);
				if(e.charAt(e.length()-1)=='}'||e.charAt(e.length()-1)=='}')
					e=e.substring(1, e.length()-2);
				return e.trim();
			}
		}
		return null;
	}
}
