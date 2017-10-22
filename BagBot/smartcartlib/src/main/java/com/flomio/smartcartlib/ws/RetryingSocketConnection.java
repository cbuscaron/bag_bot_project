package com.flomio.smartcartlib.ws;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import com.flomio.smartcartlib.binary.BytesList;
import com.flomio.smartcartlib.consts.Key;
import com.flomio.smartcartlib.json.GSON;
import com.flomio.smartcartlib.service.ble.BLEService;
import com.flomio.smartcartlib.util.Hex;
import com.flomio.smartcartlib.ws.model.FoundTagsEvent;
import com.flomio.smartcartlib.ws.model.Tag;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neovisionaries.ws.client.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.flomio.smartcartlib.util.Logging.logD;
import static com.flomio.smartcartlib.util.Logging.logDisabled;

public class RetryingSocketConnection implements WebSocketListener {
    public static final Gson gson = GSON.Configured;

    public static final String SOCKET_VERSION_RESPONSE = "SOCKET_VERSION_RESPONSE";
    public static final String SOCKET_DISCONNECTED = "SOCKET_DISCONNECTED";
    public static final String SOCKET_CONNECTED = "SOCKET_CONNECTED";
    public static final String SOCKET_CONNECT_FAILED = "SOCKET_CONNECT_FAILED";

    private WebSocket socket;
    private JsonParser parser;
    private WebSocketFactory factory;
    private LocalBroadcastManager lbm;
    private String uri;
    private boolean shouldBeConnected;

    public RetryingSocketConnection(String uri, LocalBroadcastManager manager) {
        this.uri = uri;
        this.lbm = manager;
        factory = new WebSocketFactory();
        parser = new JsonParser();
        connect();
    }

    public void disconnect() {
        shouldBeConnected = false;
        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
    }

    private void connect() {
        shouldBeConnected = true;
        if (socket != null) {
            socket.removeListener(this);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (shouldBeConnected) {
                    try {
                        try {
                            socket = factory.createSocket(uri);
                            socket.addListener(RetryingSocketConnection.this);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        socket.connect();
                        break;
                    } catch (WebSocketException e) {
                        logD("failed to connect: %s", e);
                        lbm.sendBroadcast(new Intent(SOCKET_CONNECT_FAILED));
                        if (socket != null) {
                            socket.removeListener(RetryingSocketConnection.this);
                        }
                        socket = null;
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e1) {
                            //
                        }
                    }
                }
            }
        }).start();
    }

    int round = 0;

    /**
     *
     * Convert the WebSocket messages into the same tagged message as would
     * be sent by the Rpi3 unit.
     */
    private void onFoundTags(FoundTagsEvent foundTags) {
        round = (round + 1) % (1 << 16);
        int ix = 0, numTags = foundTags.tags.size();
        BytesList bl = new BytesList();
        bl.add(encodeShort(round));
        bl.add(encodeShort(numTags));

        for (Tag tag : foundTags.tags) {
            logDisabled(
                    "found epc via ws: %s, %s/%s/%s", round, tag.epc, ix,
                    numTags);
            ix ++;
            byte[] tagEPC = Hex.decode(tag.epc);
            bl.add((byte) tagEPC.length);
            bl.add(tagEPC);
        }

        Intent intent = new Intent(BLEService.BLE_RECEIVED_MESSAGE);
        intent.putExtra("data", bl.bytes());
        intent.putExtra("type", BLEService.FOUND_TAGS_MESSAGE_TYPE);
        lbm.sendBroadcast(intent);
    }

    private void writeShort(byte[] buf, int uint16, int ix) {
        buf[ix] = (byte) ((uint16 >>> 8) & 0xff);
        buf[ix + 1] = (byte) ((uint16) & 0xff);
    }

    private byte[] encodeShort(int uint16) {
        byte[] shortBuf = new byte[2];
        writeShort(shortBuf, uint16, 0);
        return shortBuf;
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
        socket.removeListener(this);
        logD("onDisconnected");
        lbm.sendBroadcast(new Intent(SOCKET_DISCONNECTED));
        if (shouldBeConnected) {
            connect();
        }
    }

    @Override
    public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {

    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        logD("connected");
        lbm.sendBroadcast(new Intent(SOCKET_CONNECTED));
    }

    @Override
    public void onConnectError(WebSocket websocket, WebSocketException cause) throws Exception {

    }

    @Override
    public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }


    @Override
    public void onTextMessage(WebSocket websocket, String text) throws Exception {
        JsonObject event = null;
        event = parser.parse(text).getAsJsonObject();
        logDisabled("event=" + event);
        if (event.get(Key.eventName).getAsString().equals(Key.foundTags)) {
            FoundTagsEvent foundTags = gson.fromJson(event, FoundTagsEvent
                    .class);
            onFoundTags(foundTags);
        } else if (event.get(Key.eventName).getAsString().equals(Key.response)) {
            if (event.has(Key.version)) {
                Intent intent = new Intent(SOCKET_VERSION_RESPONSE);
                intent.putExtra(Key.version, event.get(Key.version).getAsString());
                lbm.sendBroadcast(intent);
            }
        }
    }


    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {

    }

    @Override
    public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onThreadCreated(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

    }

    @Override
    public void onThreadStarted(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

    }

    @Override
    public void onThreadStopping(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        logD("error: " + cause);
    }

    @Override
    public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {

    }

    @Override
    public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {

    }

    @Override
    public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {

    }

    @Override
    public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {

    }

    @Override
    public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {

    }

    @Override
    public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception {

    }

    public void sendRefreshMessage() {
        if (socket != null) {
            logD("send Refresh Message");
            socket.sendText("{\"cmd\" : \"clear_events\"}");
        }
    }
    public void sendGetVersionMessage() {
        if (socket != null) {
            socket.sendText("{\"cmd\" : \"get_version\",  \"id\": 0}");
        }
    }
}
