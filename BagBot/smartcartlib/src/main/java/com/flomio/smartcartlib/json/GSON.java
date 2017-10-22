package com.flomio.smartcartlib.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GSON {
    public static final Gson Configured = new GsonBuilder()
            .registerTypeHierarchyAdapter
            (byte[].class, new Base64Adapter()).create();
}
