package com.flomio.smartcartlib.binary;

import java.util.ArrayList;

public class MessageEncoder {
    private static ArrayList<byte[]> chunkBuffer(byte[] data) {
        ArrayList<byte[]> chunks = new ArrayList<>();
        for (int i = 0; i < data.length; i += 20) {
            chunks.add(BinaryUtils.slice(data, i, i + 20));
        }
        return chunks;
    }

    public static ArrayList<byte[]> encodeMessage(int messageType, int id, byte[] data) {
        byte[] message = new byte[data.length + Message.MIN_MESSAGE_SIZE];
        BinaryUtils.writeUint16BE(message, messageType, 0);
        BinaryUtils.writeUint16BE(message, id, 2);
        BinaryUtils.writeUint16BE(message, data.length, 4);
        System.arraycopy(data, 0, message, 6, data.length);
        message[message.length - 1] = (byte) (CheckSum.xor(data, 0, data
                .length) & 0xff);
        return chunkBuffer(message);
    }
}
