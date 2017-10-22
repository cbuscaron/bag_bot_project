package com.flomio.smartcart.activity.cart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.flomio.smartcart.R;
import com.flomio.smartcart.Settings;
import com.flomio.smartcartlib.cart.Cart;
import com.flomio.smartcartlib.cart.CartListener;
import com.flomio.smartcartlib.activity.pairing.BLEPairingActivity;
import com.flomio.smartcart.activity.admin.SmartCartAdmin;
import com.flomio.smartcartlib.api.OnApiResponse;
import com.flomio.smartcartlib.api.ProductApi;
import com.flomio.smartcartlib.api.model.Product;
import com.flomio.smartcartlib.binary.Message;
import com.flomio.smartcartlib.consts.Key;
import com.flomio.smartcart.productrecycler.ProductsAdapter;
import com.flomio.smartcartlib.service.ble.BLEService;
import com.flomio.smartcartlib.util.Format;
import com.flomio.smartcartlib.util.Toaster;
import com.flomio.smartcartlib.ws.RetryingSocketConnection;

import java.text.MessageFormat;
import java.util.*;

import static com.flomio.smartcartlib.util.Logging.logD;
import static java.text.MessageFormat.format;


public class SmartCartActivity extends AppCompatActivity
        implements
        OnApiResponse,
        CartListener,
        SwipeRefreshLayout.OnRefreshListener
{
    private Settings settings;
    private RetryingSocketConnection ws;
    private ProductApi api;
    private Cart cart;
    private RecyclerView productsRecycler;
    private ProductsAdapter productsAdapter;
    private LocalBroadcastManager lbm;
    private SwipeRefreshLayout refreshLayout;
    private Handler handler = new Handler();
    private Toaster toaster;
    private int failedWsConnectionAttempts = 0;

    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BLEService.BLE_RECEIVED_MESSAGE:
                    Message message = Message.fromIntent(intent);
                    if (message.type == BLEService.FOUND_TAGS_MESSAGE_TYPE) {
                        if (refreshLayout.isRefreshing()) {
                            refreshLayout.setRefreshing(false);
                        }
                        int round = message.getUint16();
                        int numTags = message.getUint16();
                        int nth = 0;
                        while (message.hasMoreData()) {
                            byte[] epc = message.getVLField();
                            cart.onEpcNotification(epc, round, nth, numTags);
                            nth++;
                        }
                        logD("numTags=%d", numTags);
                        if (numTags == 0 && cart.products.size() > 0) {
                            cart.clear();
                            refreshProductsRecyclerAndSetTotal();
                        }
                    }
                    break;
                case RetryingSocketConnection.SOCKET_VERSION_RESPONSE:
//                    setTitle("SmartCart \u2014 v" + intent.getStringExtra(Key
//                            .version));
                    break;
                case RetryingSocketConnection.SOCKET_DISCONNECTED:
                    onWsDisconnected();
                    break;
                case RetryingSocketConnection.SOCKET_CONNECTED:
                    onWsConnected();
                    break;
                case RetryingSocketConnection.SOCKET_CONNECT_FAILED:
                    onWsConnectFailed();
                    break;
            }
        }
    };

    private void onWsConnectFailed() {
        failedWsConnectionAttempts++;
        if ((failedWsConnectionAttempts % 10) == 9) {
            toast("Can't connect to cart. Repair!");
        }
    }

    private void onWsConnected() {
        failedWsConnectionAttempts--;
        ws.sendGetVersionMessage();
        ImageView image = getView(R.id.headerCartIcon);
        image.setImageResource(R.drawable.ic_shopping_cart_green);
    }
    private void onWsDisconnected() {
        ImageView image = getView(R.id.headerCartIcon);
        image.setImageResource(R.drawable.ic_shopping_cart_grey);
    }

    private boolean launchingPairing = false;


    private void toast(String text) {
        toaster.toast(text);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_smart_cart);
        refreshLayout = getView(R.id.swipeRefresh);
        refreshLayout.setOnRefreshListener(this);
        Toolbar toolbar = getView(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        toaster = new Toaster(this);
        settings = new Settings(this);

        lbm = LocalBroadcastManager.getInstance(this);
        String ip = getCartWsUrl();
        ws = new RetryingSocketConnection(ip,
                LocalBroadcastManager.getInstance(this));
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        queue.getCache().clear();
        api = new ProductApi(queue,
                this);
        cart = new Cart(api, this);

        productsRecycler = getView(R.id.productsListView);
        productsAdapter = new ProductsAdapter(api.imageCache);
        productsRecycler.setAdapter(productsAdapter);
        productsRecycler.setLayoutManager(new LinearLayoutManager(this));
        refreshProductsRecyclerAndSetTotal();
    }

    private String getCartWsUrl() {
        String ip = getIntent()
                .getStringExtra(Key.ip);
        logD("got ip from intent: " + ip);
        if ((ip == null) || (ip == "")) {
            ip = settings.getString(Key.cartIp);
        } else {
            settings.setString(Key.cartIp, ip);
        }
        String url;
        if (!(ip == null) && !(ip == "")) {
            logD("host part is: " + ip);
            url = "ws://" + ip + ":8010";
        } else {
            toast("Cart ip unknown! Pair with cart first");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    launchPairingActivityAndClose();

                }
            }, 500);
            url = "ws://willnotconnect";

        }
        return url;
    }


    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter
                (BLEService.BLE_RECEIVED_MESSAGE);
        filter.addAction(RetryingSocketConnection.SOCKET_VERSION_RESPONSE);
        filter.addAction(RetryingSocketConnection.SOCKET_DISCONNECTED);
        filter.addAction(RetryingSocketConnection.SOCKET_CONNECTED);
        filter.addAction(RetryingSocketConnection.SOCKET_CONNECT_FAILED);
        lbm.registerReceiver(notificationReceiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        lbm.unregisterReceiver(notificationReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        logD("onDestroy");
        if (!launchingPairing) {
            stopService(new Intent(this, BLEService.class));
        }
        ws.disconnect();
        api.dispose();
        super.onDestroy();
    }

    @Override
    public void onAPIResponse(String path, ProductApi api) {
        logD("onApiResponse: %s", path);
        if (api.haveProductsAndEPCSResponses()) {
            ProgressBar pb = getView(R.id.image_load_progress);
            pb.setVisibility(View.GONE);
        }
        cart.recalculate();
        refreshProductsRecyclerAndSetTotal();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.smart_cart_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_admin:
                Intent intent = new Intent(this, SmartCartAdmin.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            case R.id.action_pair:
                launchPairingActivityAndClose();
                return true;
            case R.id.action_fun:
                onRefresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchPairingActivityAndClose() {
        launchingPairing=true;
        Intent intent = new Intent(this, BLEPairingActivity.class);
        intent.putExtra(Key.callAfterWifi, getClass().getCanonicalName());
        startActivity(intent);
        finish();
    }

    @Override
    public void onAddedProduct(String epc, Product product, int round) {
        toast(format("Added {0}: ${1}", product.name, Format.money(product
                .price)));
    }

    @Override
    public void onRemovedProduct(String epc, Product product, int round) {
        toast(format("Removed {0}: ${1}", product.name, Format.money
                (product
                .price)));
    }

    @Override
    public void onFinishedRound(int round, boolean changed, Set<String> all, Set<String> added, Set<String> removed) {
        if (changed) {
            logD("onFinishedRound: %s, len(cart)=%d, cart.numProducts()=%d",
                    round, cart.products
                    .size(), cart.numProducts());
            refreshProductsRecyclerAndSetTotal();
        }
    }

    private void refreshProductsRecyclerAndSetTotal() {
        logD("refreshProductsRecyclerAndSetTotal");
        productsAdapter.clear();
        ArrayList<String> strings = new ArrayList<>(cart.products.keySet());
        Collections.sort(strings, new Comparator<String>() {
            @Override
            public int compare(String epc1, String epc2) {
                Product product = api.productForEPC(epc1);
                Product product2 = api.productForEPC(epc2);
                return cart.compareLastSeen(product, product2);
            }
        });
        for (String epc : strings) {
            productsAdapter.add(epc, cart.products.get(epc));
        }
        logD("notify change");
        productsAdapter.notifyDataSetChanged();
        logD("after change");
        setTotal();
    }

    private void setTotal() {

//        String.format(Locale.US,"%d items: $%s", cart
//                .products
//                .size(), cart
//                .getTotal())
        TextView line1View = getView(R.id.totalLine1);
        TextView line2View = getView(R.id.totalLine2);

        line1View.setText(MessageFormat.format("${0}", cart.getTotal()));
        line2View.setText(String.format(Locale.US, "%d items", cart.products
                .size
                ()));

    }

    @SuppressWarnings("unchecked")
    private <T extends View> T getView(int viewId) {
        return (T) findViewById(viewId);
    }

    @Override
    public void onRefresh() {
        logD("onRefresh");
        if (refreshLayout.isRefreshing()) {
            return;
        }
        refreshLayout.setRefreshing(true);

        ws.sendRefreshMessage();
        cart.clear();
        refreshProductsRecyclerAndSetTotal();
        // In case we don't get a foundTags event (no reader for testing ... )
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                refreshLayout.setRefreshing(false);
//            }
//        }, 2000);
    }
}
