package com.flomio.smartcartlib.activity.pairing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import com.flomio.smartcartlib.binary.Message;
import com.flomio.smartcartlib.consts.Key;
import com.flomio.smartcartlib.service.ble.BLEService;
import com.flomio.smartcartlib.util.Format;
import com.flomio.smartcartlib.util.Hex;
import com.flomio.smartcartlib.util.Toaster;

import java.util.List;

import static android.content.Context.WIFI_SERVICE;
import static com.flomio.smartcartlib.util.Logging.logD;

public class WifiHelper {
    public static final String WIFI_CONNECTED = "WIFI_CONNECTED";

    private static final int WIFI_IP_REQUEST = 1;
    private String configuredIP;
    private String configuredNetSSID = "";
    private Toaster toaster;


    public final Context context;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    logD("Connection change");
                    /*WifiInfo connectionInfo = getWifiManager().getConnectionInfo();
                    boolean netReady = connectionInfo.getSupplicantState() ==
                            SupplicantState
                                    .COMPLETED;
                    boolean correctNet =
                            configuredNetSSID != null &&
                                    configuredNetSSID
                                            .equals
                                                    (connectionInfo.getSSID());
                    String logText = "connected to: " + connectionInfo +
                            " :" + action + " connected=" + netReady + " " +
                            "correct network=" + correctNet +
                            " configuredNetSSID=" + configuredNetSSID +
                            " connectionInfo.getSSID()=" + connectionInfo
                            .getSSID();
                    logD(logText);
                    if (correctNet *//*&&
                            netReady*//* && configuredIP != null) {
                        onWifiConnected();
                    }
                    break;*/
                case BLEService.BLE_RECEIVED_MESSAGE:
                    Message message =
                            Message.fromIntent(intent);
                    if (message.type == WIFI_IP_REQUEST) {
                        logD("WIFI_IP_REQUEST=%s", Hex.encode(message.data));
                        onWifiSettings(message.getVLField(), message
                                .getVLField(), message.getVLField());
                    } else {
                        logD("found tagged data: tag %d, data: %s",
                                message.type, Hex.encode(message.data));
                    }
                    break;
            }
        }
    };

    public WifiHelper(Context context) {
        this.context = context;
        this.toaster = new Toaster(context);

    }

    public void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(BLEService.BLE_RECEIVED_MESSAGE);

        lbm().registerReceiver(receiver, intentFilter);
    }

    private LocalBroadcastManager lbm() {
        return LocalBroadcastManager.getInstance(context);
    }

    public void unregisterReceiver() {
        lbm().unregisterReceiver(receiver);
    }

    private void onWifiConnected() {
        Intent intent = new Intent(WIFI_CONNECTED);
        intent.putExtra(Key.ip, configuredIP);
        lbm().sendBroadcast(intent);
    }

    private void onWifiSettings(byte[] ssid, byte[] password, byte[] ip) {
        String networkName = Format.decodeUTF8(ssid);
        String passwordText = Format.decodeUTF8(password);

        configuredIP = Format.decodeUTF8(ip);
        logD("wifi settings: %s/%s/%s", networkName, passwordText,
                configuredIP);

        WifiConfiguration wifiConfig = getWifiConfiguration(networkName, passwordText);
        WifiManager wifiManager = getWifiManager();
        Integer netId = getNetID(wifiConfig);

        if (wifiManager.getConnectionInfo().getSSID().equals(configuredNetSSID)) {
            onWifiConnected();
        } else {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
            toaster.toast("Connecting to wifi: " + networkName + " " +
                    "ip=" + configuredIP);
            onWifiConnected();
        }

    }

    private Integer getNetID(WifiConfiguration wifiConfig) {
        WifiManager wifiManager = getWifiManager();
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        Integer netId = null;
        if (configuredNetworks != null) {
            for (WifiConfiguration configuredNetwork : configuredNetworks) {
                if (configuredNetwork.SSID.equals(configuredNetSSID)) {
                    netId = configuredNetwork.networkId;
                }
            }
        }
        if (netId == null) {
            netId = wifiManager.addNetwork(wifiConfig);
        }
        return netId;
    }

    private WifiConfiguration getWifiConfiguration(String networkName, String password) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", networkName);
        wifiConfig.preSharedKey = String.format("\"%s\"", password);
        configuredNetSSID = wifiConfig.SSID;
        return wifiConfig;
    }

    private WifiManager getWifiManager() {
        return (WifiManager) context.getApplicationContext()
                .getSystemService
                        (WIFI_SERVICE);
    }

}
