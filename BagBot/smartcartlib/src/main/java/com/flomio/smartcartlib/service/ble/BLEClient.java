package com.flomio.smartcartlib.service.ble;

import android.content.Context;

public class BLEClient {
    Context context;
    public BLEClient(Context context) {
        this.context = context;
    }

    public void request(BLERequest request) {
        request.request(context);
    }
}
