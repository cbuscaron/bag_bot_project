package com.flomio.smartcartlib.activity.pairing;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import com.flomio.smartcartlib.R;
import com.flomio.smartcartlib.consts.Key;
import com.flomio.smartcartlib.consts.Strings;
import com.flomio.smartcartlib.service.ble.BLEService;
import com.flomio.smartcartlib.util.Toaster;

import static com.flomio.smartcartlib.util.Logging.logD;

public class BLEPairingActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 100;
    private final int REQUEST_LOCATION_PERMS = 101;
    private Toaster toaster;
    private WifiHelper helper;

    private SignalStrengthColor signalColor;
    private LocalBroadcastManager lbm;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BLEService.BLE_TAPPED_DEVICE:
                    signalColor.finish();
                    vibrate();
                    onTapped();
                    break;
                case WifiHelper.WIFI_CONNECTED:
                    onWifiConnected(intent);
                    break;
                case BLEService.BLE_STARTED_SCANNING:
                    toast("Tap the cart!");
                    break;
                case BLEService.BLE_SUBSCRIBED:
                    onSubscribed();
                    break;
                case BLEService.BLE_NEAR_DEVICE:
                    signalColor.onRssi(
                            intent.getIntExtra("rssi", 0));

                    break;
            }
        }
    };
    private String callAfterWifi;

    private void onSubscribed() {
        logD("onSubscribed");
        Intent intent = getIntent();
        String launchActivity = intent.getStringExtra(Key.callingActivity);
        if (launchActivity == null) {
            startService(BLEService.requestWifi(this));
            toast("Connected! Requesting wifi settings!");
        } else {
            Intent launchIntent = new Intent();
            launchIntent.setClassName(this, launchActivity);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(launchIntent);
            finish();
        }
    }

    private void onWifiConnected(Intent intent) {
        logD("onWifiConnected");
        Intent starting = new Intent();
        starting.setClassName(this, callAfterWifi);
        starting.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        starting.putExtra(Key.ip, intent.getStringExtra(Key.ip));
        startActivity(starting);
        finish();
    }

    private void onTapped() {
        logD("paired");
        ImageView imageView = (ImageView) findViewById(R.id.bluetoothLogo);
//        imageView.setVisibility(View.INVISIBLE);
        findViewById(R.id.image_load_progress).setVisibility(View.VISIBLE);
        toast("Connecting via Bluetooth LE!");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blepairing);
        callAfterWifi = getIntent().getStringExtra(Key.callAfterWifi);

        signalColor = new SignalStrengthColor(findViewById(R.id.bleBackground));
        lbm = LocalBroadcastManager.getInstance(this);
        toaster = new Toaster(this);
        helper = new WifiHelper(this);
        stopService(new Intent(this, BLEService.class));

        if (!requestPermissionsIfNeeded()) {
            checkAndStartBLE();
        }
    }

    private void toast(String text) {
         toaster.toast(text);
    }

    private void checkAndStartBLE() {
        if (checkForBLE()) {
            startBLE();
            logD("starting intent: " + getIntent());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        logD("requestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMS) {
            checkAndStartBLE();
        }
    }


    private boolean requestPermissionsIfNeeded() {
        String[] permissions = {
                "android.permission.ACCESS_COARSE_LOCATION"
        };
        if (ContextCompat.checkSelfPermission(this, Manifest.permission
                .ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            logD("requestPermissions");
            requestPermissions(permissions, REQUEST_LOCATION_PERMS);
            return true;
        }
        return false;
    }

    private boolean checkForBLE() {
        if (bleDisabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        } else {
            return true;
        }
    }

    private boolean bleDisabled() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter == null || !adapter.isEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            checkAndStartBLE();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();
    }

    private void startBLE() {
        Intent service = new Intent(
                BLEPairingActivity.this,
                BLEService.class);
        service.putExtra(Key.deviceNamePrefix, Strings.cartPeripheralName);
//        service.putExtra("rssiTapThreshold", -100);
        startService(service);
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(600);
    }

    private void unregisterReceiver() {
        lbm.unregisterReceiver(receiver);
        unregisterReceiver(receiver);
        helper.unregisterReceiver();
    }

    private void registerReceiver() {
        helper.registerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiHelper.WIFI_CONNECTED);
        filter.addAction(BLEService.BLE_NEAR_DEVICE);
        filter.addAction(BLEService.BLE_SUBSCRIBED);
        filter.addAction(BLEService.BLE_TAPPED_DEVICE);
        lbm.registerReceiver(receiver, filter);
        registerReceiver(receiver, filter);
    }
}
