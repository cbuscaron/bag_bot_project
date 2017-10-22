package com.flomio.smartcartlib.service.ble;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import com.flomio.smartcartlib.binary.BytesList;
import com.flomio.smartcartlib.binary.OERReader;
import com.flomio.smartcartlib.consts.Key;

abstract public class BLERequest {
    public abstract int requestId();
    public abstract void requestData(BytesList bytes);
    public abstract void onResult(OERReader reader);
    public void request(Context context) {
        BytesList bl = new BytesList();
        requestData(bl);
        Intent requestIntent = BLEService.getRequestIntent(context, requestId
                (), bl.bytes());
        requestIntent.putExtra(Key.resultReceiver, new ResultReceiver(new Handler(context
                .getMainLooper())
                ) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                byte[] data = resultData.getByteArray(Key.data);
                OERReader reader = new OERReader(data);
                onResult(reader);
            }
        });
        context.startService(requestIntent);
    }
}
