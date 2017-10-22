package com.flomio.smartcartlib.json;

import com.flomio.smartcartlib.util.CustomBase64;
import com.google.gson.*;

import java.lang.reflect.Type;

public class Base64Adapter implements JsonSerializer<byte[]>,
        JsonDeserializer<byte[]> {

    @Override
    public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return CustomBase64.decode(json.getAsString());
    }

    @Override
    public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(CustomBase64.encodeToString(src, false));
    }
}
