package com.flomio.smartcartlib.ws.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class FoundTagsEvent {
    @Expose
    @SerializedName("eventName")
    public String eventName;

    @Expose
    @SerializedName("tags")
    public ArrayList<Tag> tags;
}
