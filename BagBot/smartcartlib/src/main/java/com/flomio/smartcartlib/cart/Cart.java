package com.flomio.smartcartlib.cart;

import com.flomio.smartcartlib.api.ProductApi;
import com.flomio.smartcartlib.api.model.Product;
import com.flomio.smartcartlib.util.Format;
import com.flomio.smartcartlib.util.Hex;

import java.math.BigDecimal;
import java.util.*;

import static com.flomio.smartcartlib.util.Logging.logD;

public class Cart {
    private HashSet<String> epcs;
    private HashSet<String> newRound;
    private TreeMap<Integer, Integer> lastSeenProducts = new TreeMap<>();
    private int lastSeenCounter = 0;
    public HashMap<String, Product> products = new HashMap<>();

    public Cart(ProductApi api, CartListener listener) {
        this.api = api;
        this.listener = listener;
    }

    private ProductApi api;
    private CartListener listener;
    private boolean doRecalc = false;

    public int numProducts() {
        return epcs == null ? 0 : epcs.size();
    }

    public int compareLastSeen(Product product, Product product2) {
        int cmp = lastSeenProducts.get(product.sku) - lastSeenProducts.get
                (product2.sku);
        if (cmp == 0) {
            // This way we have stability
            cmp = product.name.toLowerCase().compareTo(product2.name.toLowerCase());
        }
        return cmp;
    }

    public void clear() {
        epcs = null;
        newRound = null;
        lastSeenProducts.clear();
        products.clear();
    }

    public void recalculate() {
        doRecalc = true;
    }

    public String getTotal() {
        BigDecimal bd = new BigDecimal(0);
        for (Product product : products.values()) {
            bd = bd.add(product.price);
        }
        return Format.money(bd);
    }

    public void onEpcNotification(byte[] epcBytes,
                                  int foundTagsRound,
                                  int nth,
                                  int totalTags) {
        String epc = Hex.encode(epcBytes);
        if (nth == 0 || newRound == null) {
            newRound = new HashSet<>();
        }
        newRound.add(epc);

        boolean refresh = false;
        if (nth + 1 == totalTags) {
            Set<String> added = null, removed = null;
            if (epcs != null) {
                added = new HashSet<>(newRound);
                added.removeAll(epcs);

                for (String addedEpc : added) {
                    refresh = true;
                    logD("Added epc: " + addedEpc);
                    Product product = api.productForEPC(addedEpc);
                    if (product != null) {
                        products.put(addedEpc, product);
                        trackLastSeen(product);
                        listener.onAddedProduct(addedEpc, product,
                                foundTagsRound);
                    }
                }

                epcs.removeAll(newRound);
                removed = epcs;
                for (String removedEPC : epcs) {
                    refresh = true;
                    logD("Removed epc: " + removedEPC);
                    products.remove(removedEPC);
                    Product product = api.productForEPC(removedEPC);
                    if (product != null) {
                        trackLastSeen(product);
                        listener.onRemovedProduct(removedEPC, product,
                                foundTagsRound);
                    }
                }
            } else {
                refresh = true;
                added = newRound;

                for (String newEpc : newRound) {
                    Product product = api.productForEPC(newEpc);
                    if (product != null) {
                        trackLastSeen(product);
                        products.put(newEpc, product);
                    }
                }
            }
            epcs = newRound;
            if (doRecalc) {
                refresh = true;
                doRecalc = false;

                for (String _epc : epcs) {
                    Product product = api.productForEPC(_epc);
                    if (product != null) {
                        trackLastSeen(product);
                        products.put(_epc, product);
                    }
                }

            }
            listener.onFinishedRound(foundTagsRound, refresh, epcs, added, removed);
        }
    }

    private void trackLastSeen(Product product) {
        int value = lastSeenCounter++;
        // java ints are signed, so at some point, ++ will overflow and
        // become negative
        if (value < 0) {
            lastSeenCounter = 0;
        }
        lastSeenProducts.put(product.sku, value);
    }
}
