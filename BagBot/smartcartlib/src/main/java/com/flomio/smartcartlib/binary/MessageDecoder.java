package com.flomio.smartcartlib.binary;


import com.flomio.smartcartlib.util.Hex;

import java.util.ArrayList;
import java.util.Arrays;

import static com.flomio.smartcartlib.util.Logging.logD;

public class MessageDecoder {
    private ArrayList<byte[]> dataChunks = new ArrayList<>();
    private int type;
    private int len;
    private int id;
    private int expectedChunks = 0;

    public Message push(byte[] chunk) {
        logD("pushing chunk: %d, %s", chunk.length, Hex.encode(chunk));
        if (dataChunks.size() == 0) {
            type = BinaryUtils.readUint16BE(chunk, 0);
            id = BinaryUtils.readUint16BE(chunk, 2);
            len = BinaryUtils.readUint16BE(chunk, 4);
            logD("decoded chunk header type=%d len=%d", type, len);
            byte[] data = Arrays.copyOfRange(chunk, 6, Math.min(
                    Message.MIN_MESSAGE_SIZE + len,
                    chunk.length));
            addChunk(data);
            expectedChunks = (Message.MIN_MESSAGE_SIZE + len) / 20;
            if ((len + Message.MIN_MESSAGE_SIZE) % 20 == 0) {
                expectedChunks -= 1;
            }
            logD("expectedChunks=%d", expectedChunks);
        } else {
            expectedChunks --;
            addChunk(chunk);
        }
        if (expectedChunks == 0) {
            byte[] pop = dataChunks.remove(dataChunks.size() - 1);
            int check = pop[pop.length - 1] & 0xFF;
            addChunk(Arrays.copyOfRange(pop, 0, pop.length - 1));
            byte[] data = BinaryUtils.toByteArray(dataChunks);
            int calculated = CheckSum.xor(data, 0, data.length);
            if (check != calculated) {
                logD("data: " + Hex.encode(data));
                logD("checksum failed: expected %d bytes vs %d" +
                        " data=%s check=%s calculated=%s", len, data
                        .length, Hex.encode(data), check, calculated);
                 throw new ChecksumError("check sum failed");
            }
            logD("found tagged data, creating new Message");
            return new Message(type, id, data);
        }
        return  null;
    }

    private void addChunk(byte[] data) {
        logD("adding chunk %d: len=%d, %s", dataChunks.size(),  data.length,
                Hex
                .encode
                (data));
        dataChunks.add(data);
    }

}
