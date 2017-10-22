package com.flomio.smartcartlib.binary;

public class OERReader {
    // Most significant bit in a byte
    static final int HIGH_BIT = 0x80;

    // Other bits in a byte
    static final int LOWER_SEVEN_BITS = 0x7F;

    final byte[] buf;
    private int cursor = 0;

    public OERReader(byte[] buf) {
        this.buf = buf;
    }

    public int readLength() {
        int length = readUint8();
        if ((length & HIGH_BIT) != 0) {
            int lengthPrefixLength = length & LOWER_SEVEN_BITS;
            return readUint(lengthPrefixLength);
        }
        return length;
    }

    private int readUint(int length) {
        int n = 0;
        for (int i = 0; i < length; i++) {
            n = (n << 8) | readUint8();
        }
        return n;
    }

    public int readUint16BE() {
        return BinaryUtils.readUint16BE(read(2), 0);
    }

    public int readUint8() {
        return read() & 0xff;
    }

    public byte[] readVarOctetString() {
        return read(readLength());
    }

    private byte read() {
        return buf[this.cursor++];
    }

    public int available() {
        return buf.length - cursor;
    }

    public byte[] read(int n) {
        if ((cursor + n) > buf.length) {
            throw new IllegalArgumentException();
        }
        return BinaryUtils.slice(buf, cursor, cursor += n);
    }
}
