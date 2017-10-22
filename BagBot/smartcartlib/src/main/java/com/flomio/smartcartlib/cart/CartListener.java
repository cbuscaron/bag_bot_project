package com.flomio.smartcartlib.cart;

import com.flomio.smartcartlib.api.model.Product;

import java.util.Set;

public interface CartListener {
    void onAddedProduct(String epc, Product product, int round);
    void onRemovedProduct(String epc, Product product, int round);
    void onFinishedRound(int round, boolean refresh, Set<String> all, Set<String> added, Set<String> removed);
}
