package com.flomio.smartcartlib.binary;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class BinaryUtils {
    public static void writeUint16BE(byte[] buf, int uint16, int ix) {
        buf[ix] = (byte) ((uint16 >>> 8) & 0xff);
        buf[ix + 1] = (byte) ((uint16) & 0xff);
    }
    static int readUint16BE(byte[] buffer, int offset) {
        return ((buffer[offset] & 0xff) << 8) | (buffer[offset + 1] & 0xff);
    }

    public static byte[] toByteArray(List<byte[]> bytesList)
    {
        int size = 0;

        for (byte[] bytes : bytesList)
        {
            size += bytes.length;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(size);

        for (byte[] bytes : bytesList)
        {
            byteBuffer.put(bytes);
        }

        return byteBuffer.array();
    }

    public static byte[] slice(byte[] data, int start, int stop) {
        // Else Arrays.copyOfRange will zero pad
        if (stop > data.length) {
            stop = data.length;
        }
        return Arrays.copyOfRange(data, start, stop);
    }
}
