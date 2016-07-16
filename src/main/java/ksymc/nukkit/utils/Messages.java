package ksymc.nukkit.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Messages {
	private int version;
	
	private String defaultLanguage;
	
	private Map<String, Object> messages;
	
	public Messages(Map<String, Object> config) {
		version = config.containsKey("version") ? (int) config.get("version") : 0;
		defaultLanguage = config.containsKey("default-language") ? (String) config.get("default-language") : "en";
		messages = config.containsKey("messages") ? (Map<String, Object>) config.get("messages") : null;
	}
	
	public int getVersion() {
		return version;
	}
	
	public String getDefaultLanguage() {
		return defaultLanguage;
	}
	
	public Map<String, Object> getMessages() {
		return messages;
	}
	
	public String getMessage(String key) {
		return getMessage(key, new HashMap<>(), null);
	}
	
	public String getMessage(String key, Map<String, String> format) {
		return getMessage(key, format, null);
	}
	
	public String getMessage(String key, Map<String, String> format, String language) {
		if (language == null)
			language = getDefaultLanguage();
		
		Map<String, String> message = (Map<String, String>) getMessages().get(key);
		if (message == null)
			return null;
		
		String string = message.get(language);
		if (string != null && !language.equals(getDefaultLanguage()))
			string = message.get(getDefaultLanguage());
		
		if (string != null) {
			for (Entry<String, String> e : format.entrySet())
				string = string.replace("{%" + e.getKey() + "}", e.getValue());
			return string;
		}
		return null;
	}
}
