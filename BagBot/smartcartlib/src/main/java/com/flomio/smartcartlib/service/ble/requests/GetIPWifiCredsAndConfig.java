package com.flomio.smartcartlib.service.ble.requests;

import android.util.Log;
import com.flomio.smartcartlib.binary.BytesList;
import com.flomio.smartcartlib.binary.OERReader;
import com.flomio.smartcartlib.service.ble.BLERequest;
import com.flomio.smartcartlib.service.ble.BLEService;
import com.flomio.smartcartlib.util.Format;
import org.json.JSONException;
import org.json.JSONObject;

import static com.flomio.smartcartlib.util.Logging.logD;

public abstract class GetIPWifiCredsAndConfig extends BLERequest {
    @Override
    public int requestId() {
        return BLEService.REQUEST_GET_WIFI_CREDS_AND_IP;
    }
    @Override
    public void requestData(BytesList bytes) {

    }
    @Override
    public void onResult(OERReader reader) {
        byte[] ssidBytes = reader.readVarOctetString();
        byte[] passwordBytes = reader.readVarOctetString();
        byte[] ipBytes = reader.readVarOctetString();
        byte[] config = null;
        if (reader.available() > 0) {
            config = reader.readVarOctetString();
        }

        JSONObject jsonObject = getJsonObject(config);
        onWifiCredsAndIP(Format.decodeUTF8(ssidBytes),
                         Format.decodeUTF8(passwordBytes),
                         Format.decodeUTF8(ipBytes),
                jsonObject);
    }

    private JSONObject getJsonObject(byte[] config){
        if (config == null) {
            return new JSONObject();
        }
        try {
            String json = Format.decodeUTF8(config);
            return new JSONObject(json);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void onWifiCredsAndIP(String ssid, String password, String ip, JSONObject jsonObject);
}
