package com.flomio.smartcartlib.service.ble.requests;

import com.flomio.smartcartlib.binary.OERReader;
import com.flomio.smartcartlib.service.ble.BLERequest;
import com.flomio.smartcartlib.service.ble.BLEService;
import com.flomio.smartcartlib.util.Format;

abstract public class SetWifiCreds extends BLERequest {
    @Override
    public int requestId() {
        return BLEService.REQUEST_SET_WIFI_CREDS;
    }

    @Override
    public void onResult(OERReader reader) {
        onSet(Format.decodeUTF8(reader.readVarOctetString()));
    }

    public abstract void onSet(String output);
}
