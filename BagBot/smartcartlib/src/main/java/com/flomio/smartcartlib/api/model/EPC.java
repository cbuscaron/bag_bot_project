package com.flomio.smartcartlib.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EPC {
    @Expose
    @SerializedName("epc")
    public String epc;

    @Expose
    @SerializedName("sku")
    public int sku;
}
