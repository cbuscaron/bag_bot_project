package com.flomio.smartcart.productrecycler;

import android.graphics.Bitmap;
import com.flomio.smartcartlib.api.model.Product;

import java.util.TreeSet;

public class Products {
    public Products(Product product, Bitmap bitmap) {
        this.product = product;
        this.bitmap = bitmap;
    }

    Product product;
    Bitmap bitmap;
    TreeSet<String> epcs = new TreeSet<>();
}
