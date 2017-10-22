package com.flomio.smartcart.activity.admin;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import com.flomio.smartcart.R;
import com.flomio.smartcartlib.binary.BytesList;
import com.flomio.smartcartlib.service.ble.BLEClient;
import com.flomio.smartcartlib.service.ble.BLEService;
import com.flomio.smartcartlib.service.ble.requests.GetIPWifiCredsAndConfig;
import com.flomio.smartcartlib.service.ble.requests.GetSoftwareVersion;
import com.flomio.smartcartlib.service.ble.requests.SetPowerLevel;
import com.flomio.smartcartlib.service.ble.requests.SetWifiCreds;
import com.flomio.smartcartlib.util.Toaster;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import static com.flomio.smartcartlib.util.Logging.logD;

public class SmartCartAdmin extends AppCompatActivity {
    EditText ssid, password;
    TextView versionLabel;
    TextView powerLevelLabel;
    Button setWifi;
    Button getVersion;
    Button getWifi;
    Toaster toaster;
    SeekBar powerSlider;
    BLEClient client;
    boolean enabled=true;
    private Handler handler;

    @SuppressWarnings("unchecked")
    private <T> T getView(int viewId) {
        return (T) findViewById(viewId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smartcart_admin);
        handler = new Handler(getMainLooper());
        toaster = new Toaster(this);
        client = new BLEClient(this);
        ssid = getView(R.id.ssidEditText);
        password = getView(R.id.passwordEditText);
        powerSlider = getView(R.id.powerSliderSeekView);
        powerLevelLabel = getView(R.id.powerLevelLabel);
        setSingle(ssid);
        setSingle(password);
        getWifi = getView(R.id.getWifiCredsAndIp);
        setWifi = getView(R.id.setWifiButton);
        getVersion = getView(R.id.getVersion);

        powerSlider.setMax(17);
        powerSlider.setProgress(0);
        powerSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int powerLevel = 10 + progress;
                logD("setting power level: " + powerLevel);
                setPowerLevel(powerLevel);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        setDefaultWifi();
        setWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendWifi(ssid.getText().toString(), password.getText().toString());
            }
        });
        getVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getVersion();
            }
        });
        getWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getWifiCredsAndIP();
            }
        });
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                enabled = s.length() > 0;
                setWifi.setEnabled(enabled);
            }
        };
        ssid.addTextChangedListener(watcher);
        password.addTextChangedListener(watcher);
        watcher.afterTextChanged(ssid.getText());
        watcher.afterTextChanged(password.getText());

        requestBLE();
    }

    int mPowerLevel = 10;
    private void setPowerLevel(final int powerLevel) {
        powerLevelLabel.setText("Power Level: " + powerLevel);
        mPowerLevel = powerLevel;
        handler.removeCallbacks(setPowerLevelRunnable);
        handler.postDelayed(setPowerLevelRunnable, 300);
    }

    private Runnable setPowerLevelRunnable = new Runnable() {
        @Override
        public void run() {
            client.request(new SetPowerLevel() {
                @Override
                public int powerLevel() {
                    return mPowerLevel;
                }

                @Override
                public void onSet() {
                    logD("received power result: " + mPowerLevel);
                }
            });
        }
    };

    private void requestBLE() {
        startService(BLEService.requestPairing(this, SmartCartAdmin.class, new ResultReceiver
                (handler) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == 0) {
                    finish();
                }
            }
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    private void setWifiCreds() {
        client.request(new SetWifiCreds() {
            @Override
            public void onSet(String configure) {
                toaster.toast("set wifi creds:\n" + configure);
            }
            @Override
            public void requestData(BytesList data) {
                addVL(data, getUtf8Bytes(SmartCartAdmin.this.ssid));
                addVL(data, getUtf8Bytes(SmartCartAdmin.this.password));
            }
        });
    }

    private void getWifiCredsAndIP() {
        client.request(new GetIPWifiCredsAndConfig() {
            @Override
            public void onWifiCredsAndIP(String ssid, String password, String ip, JSONObject jsonObject) {
                toaster.toast(String.format("ssid =%s\npassword =%s\nip" +
                        " =%s\nconfig=%s", ssid, password, ip, jsonObject));
            }
        });
    }

    private void getVersion() {
        client.request(new GetSoftwareVersion() {
            @Override
            public void onVersion(String human) {
                toaster.toast("software version: " + human);
            }
        });
    }

    private byte[] getUtf8Bytes(EditText editText){
        byte[] utf8;
        try {
            utf8 = editText.getText().toString().getBytes("utf8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return utf8;
    }

    private void addVL(BytesList data, byte[] ssid) {
        if (ssid.length > 127) {
            throw new RuntimeException();
        }
        data.add((byte) ssid.length);
        data.add(ssid);
    }

    private void setSingle(EditText editText) {
        editText.setSingleLine();
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
    }

    private void sendWifi(String ssid, String password) {
        toaster.toast(String.format("Setting wifi " +
                "settings:%n" +
                "ssid=%s%npassword=%s", ssid, password));
//        if (getIntent().getBooleanExtra("ble", false)) {
            setWifiCreds();
//        }

    }

    private void setDefaultWifi() {
        WifiManager manager = getWifiManager();
        WifiInfo connectionInfo = manager.getConnectionInfo();
        String ssid = connectionInfo.getSSID();
        if (ssid != null) {
            if (ssid.startsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            this.ssid.setText(ssid);
        }

    }
    private WifiManager getWifiManager() {
        return (WifiManager) getApplicationContext()
                .getSystemService(WIFI_SERVICE);
    }

}
