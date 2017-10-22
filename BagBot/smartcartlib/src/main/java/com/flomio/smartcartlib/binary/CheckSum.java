package com.flomio.smartcartlib.binary;

public class CheckSum {
    public static int xor(byte[] buf, int start, int stop) {
        int xord = 0;
        // xord = (xord ^ buf.readUInt8(i)) & 0xFF
        for (int i = start; i < stop; i++) {
            int bufi = buf[i] & 0xff;
            xord = xord ^ (bufi);
        }
        return xord & 0xFF;
    }
}
