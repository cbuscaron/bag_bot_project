package com.flomio.smartcartlib.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.flomio.smartcartlib.api.model.EPC;
import com.flomio.smartcartlib.api.model.EPCSResponse;
import com.flomio.smartcartlib.api.model.Product;
import com.flomio.smartcartlib.api.model.ProductsResponse;
import com.flomio.smartcartlib.json.GSON;
import com.google.gson.Gson;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static com.flomio.smartcartlib.util.Logging.logD;

public class ProductApi {
    public static final String API_BASE_URL = "https://bd8lfab109.execute-api" +
            ".us-east-1.amazonaws.com/dev/";
    public static final Gson gson = GSON.Configured;

    private final OnApiResponse onResponse;
    private boolean disposed = false;

    public ProductApi(RequestQueue queue, OnApiResponse onResponse) {
        this.onResponse = onResponse;
        this.queue = queue;
        apiCall("/products");
        apiCall("/epcs");
        this.queue.start();
    }

    private RequestQueue queue;
    private ProductsResponse productsResponse;
    private EPCSResponse epcsResponse;
    private ConcurrentMap<Integer, Product> bySku = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Product> byEPC = new ConcurrentHashMap<>();
    public ConcurrentMap<Integer, Bitmap> imageCache = new
            ConcurrentHashMap<>();
    private ConcurrentSkipListSet<String> queued = new ConcurrentSkipListSet<>();

    public boolean haveProductsAndEPCSResponses() {
        return productsResponse != null && epcsResponse != null;
    }

    private void apiCall(final String path) {
        final String url = API_BASE_URL + path;
        if (queued.contains(url)) {
            return;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        ProductApi.this.onResponse(path, response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                logD("api error: %s %s", path, error);
                if (!disposed) {
                    queued.remove(url);
                    apiCall(path);
                }
            }
        });
        queued.add(url);
        queue.add(stringRequest);
    }

    public Product productForEPC(String epc) {
        Product product = byEPC.get(epc);
        if (product != null) {
            if (product.image == null) {
                this.apiCall("/product/" + product.sku);
            }
        }
        return product;
    }

    public void dispose() {
        disposed = true;
        queued.clear();
        queue.stop();
    }

    private void onResponse(String path, String response) {
        if (disposed) {
            return;
        }
        if (path.equals("/products")) {
            productsResponse = gson.fromJson(response, ProductsResponse.class);
            updateLookups();
        } else if (path.equals("/epcs")) {
            epcsResponse = gson.fromJson(response, EPCSResponse.class);
            updateLookups();
        } else if (path.startsWith("/product/")) {
            Product product = gson.fromJson(response, Product.class);
            imageCache.put(product.sku, BitmapFactory.decodeByteArray(product.image, 0, product.image.length));
            bySku.put(product.sku, product);
            updateEPCsLookup();
        }
        this.onResponse.onAPIResponse(path, this);
    }

    private void updateLookups() {
        if (productsResponse != null) {
            for (Product product : productsResponse.products) {
                // Just set defaults
                bySku.put(product.sku, product);
            }
        }
        updateEPCsLookup();

    }

    private void updateEPCsLookup() {
        if (epcsResponse != null) {
            for (EPC epc : epcsResponse.epcs) {
                Product product = bySku.get(epc.sku);
                if (product != null) {
                    byEPC.put(epc.epc, product);
                }
            }
        }
    }

}
