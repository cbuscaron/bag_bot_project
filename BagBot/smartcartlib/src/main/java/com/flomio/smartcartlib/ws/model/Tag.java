package com.flomio.smartcartlib.ws.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Tag {
    @Expose
    @SerializedName("epc")
    public String epc;

    @Expose
    @SerializedName("tagType")
    public String type;
}
