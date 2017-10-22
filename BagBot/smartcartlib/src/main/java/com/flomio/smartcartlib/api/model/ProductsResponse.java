package com.flomio.smartcartlib.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class ProductsResponse {
    @Expose
    @SerializedName("items")
    public ArrayList<Product> products;
}
