package com.flomio.smartcartlib.binary;

import java.util.ArrayList;

public class BytesList {
    private ArrayList<byte[]> buffer = new ArrayList<>();

    private int len = 0;

    public void add(BytesList bl) {
        for (byte[] bytes : bl.rawList()) {
            add(bytes);
        }
    }

    public void add(byte aByte) {
        add(new byte[]{aByte});
    }

    public void add(byte[] bytes) {
        len += bytes.length;
        buffer.add(bytes);
    }

    public byte[] bytes() {
        int n = bytesLength();
        byte[] bytes = new byte[n];
        addBytes(bytes, 0);
        return bytes;
    }

    static private String[] hexLookup = new String[256];
    static {
        for (int i = 0; i < 256; i++) {
            String s = Integer.toHexString(i).toUpperCase();
            if (s.length() == 1) {
                s = "0" + s;
            }
            hexLookup[i] = s;
        }
    }

    public String bytesHex() {
        StringBuilder builder = new StringBuilder(len * 2);
        for (byte[] buf : buffer) {
            for (byte aBytes : buf) {
                builder.append(hexLookup[aBytes & 0xFF]);
            }
        }
        return builder.toString();
    }

    public int bytesLength() {
        return len;
    }

    private int addBytes(byte[] bytes, int destPos) {
        for (byte[] buf : buffer) {
            System.arraycopy(buf, 0, bytes, destPos, buf.length);
            destPos += buf.length;
        }
        return destPos;
    }

    public ArrayList<byte[]> rawList() {
        return buffer;
    }
}