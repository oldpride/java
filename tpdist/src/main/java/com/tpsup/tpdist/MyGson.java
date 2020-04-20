package com.tpsup.tpdist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

//we use a shared Gson object to avoid re-create it. Do we really need to do this?

public final class MyGson {
	// https://mvnrepository.com/artifact/com.google.code.gson/gson/2.8.6
	// https://stackoverflow.com/questions/8360836/gson-is-there-an-easier-way-to-serialize-a-map
	final public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	final public static String toJson (Object obj) {
		return gson.toJson(obj);
	}
} 
