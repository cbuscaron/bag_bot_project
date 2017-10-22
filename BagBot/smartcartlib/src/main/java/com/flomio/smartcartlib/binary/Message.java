package com.flomio.smartcartlib.binary;

import android.content.Intent;

public class Message {
    public static final int MIN_MESSAGE_SIZE = 7;
    public static final String REQUEST_ID = "requestId";
    public static final String TYPE = "type";
    public static final String ID = "id";
    public static final String DATA = "data";

    public final int id;
    public final int type;
    public final byte[] data;
    private final OERReader buf;

    public Message(int type, int id, byte[] data) {
        this.id = id;
        this.type = type;
        this.data = data;
        this.buf = new OERReader(data);
    }

    public boolean hasMoreData() {
        return buf.available() > 0;
    }

    public byte[] getVLField() {
        return buf.readVarOctetString();
    }

    public int getUint16() {
        return buf.readUint16BE();
    }

    public static Message fromIntent(Intent intent) {
        return new Message(
                intent.getIntExtra(TYPE, 0),
                intent.getIntExtra(ID, 0),
                intent.getByteArrayExtra(DATA)
        );
    }

}
