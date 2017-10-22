package com.flomio.smartcartlib.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class Product {
    @Expose
    @SerializedName("sku")
    public int sku;

    @Expose
    @SerializedName("price")
    public BigDecimal price;

    @Expose
    @SerializedName("image")
    public byte[] image;

    @Expose
    @SerializedName("name")
    public String name;

    @Expose
    @SerializedName("description")
    public String description;

    @Override
    public String toString() {
        return "Product{" +
                "sku=" + sku +
                ", price=" + price +
                ", image=" + "byte["+ image.length +"]" +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}

