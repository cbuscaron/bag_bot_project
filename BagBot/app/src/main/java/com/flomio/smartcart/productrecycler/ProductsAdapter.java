package com.flomio.smartcart.productrecycler;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import com.flomio.smartcartlib.api.model.Product;

import java.util.ArrayList;
import java.util.Map;

public class ProductsAdapter extends RecyclerView.Adapter<ProductsHolder>{
    public ArrayList<Products> products = new ArrayList<>();
    Map<Integer, Bitmap> imageCache;

    public ProductsAdapter(Map<Integer, Bitmap> imageCache) {
        super();
        this.imageCache = imageCache;
        setHasStableIds(true);
    }

    @Override
    public ProductsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return ProductsHolder.make(parent);
    }

    @Override
    public void onBindViewHolder(ProductsHolder holder, int position) {
        holder.setModel(products.get(position));
    }

    @Override
    public long getItemId(int position) {
        Product product = products.get(position).product;
        return product.sku; // | ((product.image == null ? 0 : 1) << 32);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void clear() {
        this.products.clear();
    }
    public void add(String epc, Product product) {
        boolean found = false;
        for (Products group : products) {
            if (group.product.sku == product.sku) {
                found = true;
                group.epcs.add(epc);
                if (group.bitmap == null) {
                    group.bitmap = imageCache.get(product.sku);
                }
            }
        }
        if (!found) {
            Products group = new Products(product, imageCache.get(product.sku));
            group.epcs.add(epc);
            products.add(0, group);
        }
    }
}
