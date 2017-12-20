package utils;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONUtils {
	
	public static String writeObjectAsJSONString(Object object) {
		ObjectMapper objectMapper = new ObjectMapper();
		String json = null;

		try {
			json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return json;
	}

	public static <T> T parseJSONToObject(InputStream json, Class<T> clazz) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(json, clazz);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
	

}
