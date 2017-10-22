package com.flomio.smartcartlib.service.ble.requests;

import com.flomio.smartcartlib.binary.BytesList;
import com.flomio.smartcartlib.binary.OERReader;
import com.flomio.smartcartlib.service.ble.BLERequest;
import com.flomio.smartcartlib.service.ble.BLEService;

public abstract class SetPowerLevel extends BLERequest{
    public abstract int powerLevel();
    public abstract void onSet();

    @Override
    public int requestId() {
        return BLEService.REQUEST_SET_POWER_LEVEL;
    }

    @Override
    public void requestData(BytesList bytes) {
        bytes.add((byte) powerLevel());
    }

    @Override
    public void onResult(OERReader reader) {
        onSet();
    }
}
