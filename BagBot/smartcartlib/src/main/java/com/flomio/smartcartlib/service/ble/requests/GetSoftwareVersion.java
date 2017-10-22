package com.flomio.smartcartlib.service.ble.requests;

import com.flomio.smartcartlib.binary.BytesList;
import com.flomio.smartcartlib.binary.OERReader;
import com.flomio.smartcartlib.service.ble.BLERequest;
import com.flomio.smartcartlib.service.ble.BLEService;

import java.nio.charset.Charset;

public abstract class GetSoftwareVersion extends BLERequest {
    @Override
    public int requestId() {
        return BLEService.REQUEST_GET_CART_SOFTWARE_VERSION;
    }

    @Override
    public void requestData(BytesList bytes) {

    }

    @Override
    public void onResult(OERReader reader) {
        byte[] version = reader.readVarOctetString();
        String utf8 = new String(version, Charset.forName("utf8"));
        String human = utf8.substring(0, 7) + "-" +
                utf8.substring(7);
        onVersion(human);
    }

    public abstract void onVersion(String human);
}
