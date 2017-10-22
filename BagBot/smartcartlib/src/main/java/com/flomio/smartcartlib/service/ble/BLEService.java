package com.flomio.smartcartlib.service.ble;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import com.flomio.smartcartlib.BuildConfig;
import com.flomio.smartcartlib.activity.pairing.BLEPairingActivity;
import com.flomio.smartcartlib.consts.Key;
import com.flomio.smartcartlib.consts.Strings;
import com.flomio.smartcartlib.binary.ChecksumError;
import com.flomio.smartcartlib.binary.Message;
import com.flomio.smartcartlib.binary.MessageDecoder;
import com.flomio.smartcartlib.binary.MessageEncoder;
import com.flomio.smartcartlib.util.Hex;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;
import static com.flomio.smartcartlib.util.Logging.logD;

public class BLEService extends Service {
    private static final UUID NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString
            ("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID SERVICE_UUID = UUID.fromString
            ("ab0b8ddd-43c6-498b-93c1-7e08be2d8b82");
    private static final UUID WRITE_UUID = UUID.fromString
            ("220e563f-c88a-4dee-9d10-13c8662277cb");
    private static final UUID NOTIFY_UUID = UUID.fromString
            ("ec18b6bd-70e9-44e7-a2ce-a0ea0c0ba8a3");

    public static final String BLE_NEAR_DEVICE = ("BLE_NEAR_DEVICE");
    public static final String BLE_TAPPED_DEVICE = ("BLE_TAPPED_DEVICE");
    public static final String BLE_RECEIVED_MESSAGE = ("BLE_RECEIVED_MESSAGE");
    public static final String BLE_SUBSCRIBED = ("BLE_SUBSCRIBED");
    public static final String BLE_STARTED_SCANNING = ("BLE_STARTED_SCANNING");

    public static final int FOUND_TAGS_MESSAGE_TYPE = 0;
    public static final int REQUEST_GET_WIFI_CREDS_AND_IP = 1;
    public static final int REQUEST_SET_WIFI_CREDS = 2;
    public static final int REQUEST_GET_CART_SOFTWARE_VERSION = 3;
    public static final int REQUEST_SET_POWER_LEVEL = 4;

    // Intent configurable
    private boolean autoConnect = true;
    private int rssiTapThreshold = -40;

    private Context context;
    private LocalBroadcastManager lbm;
    private BluetoothManager btm;
    private BluetoothAdapter bta;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothLeScanner bleScanner;
    private ScanCallback scanBack;
    private BluetoothDevice device;
    private MessageDecoder messageDecoder = null;
    private int lastMessageId = 0;

    private HashMap<String, Integer> deviceRssiMap = new HashMap<>();
    private final Object lock = new Object();
    private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Runnable> onSubscribed = new
            ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<Integer, ResultReceiver> receivers = new ConcurrentHashMap<>();
    private boolean paired = false;
    private boolean subscribed = false;

    private void configureFrom(Intent intent) {
        autoConnect = intent.getBooleanExtra(Key.autoConnect, autoConnect);
        rssiTapThreshold = intent.getIntExtra(Key.rssiTapThreshold,
                rssiTapThreshold);
        logD("tapping threshold is %d", rssiTapThreshold);
    }

    // boolean destroyed = true;
    @Override
    public void onDestroy() {
        logD("BLEService.onDestroy");
        stopScanningIfScanning();
        if (gatt != null) {
            gatt.close();
            gatt.disconnect();
            gatt = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init(getApplicationContext());
    }

    private void init(Context context) {
        this.context = context;
        this.lbm = LocalBroadcastManager.getInstance(context);
        this.btm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bta = btm.getAdapter();
        this.bleScanner = bta.getBluetoothLeScanner();
        logD("init complete");
    }

    public void stopScanningIfScanning() {
        if (scanBack != null) {
            bleScanner.stopScan(scanBack);
            scanBack = null;
        }
    }

    void startScanning(final String deviceNamePrefix) {
//        this.onPaired = onPaired;
        logD("scanBack==null=%s", scanBack == null);
        scanBack = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                // shouldn't need this guard but seems we do :/
                if (paired) {
                    return;
                }

                BluetoothDevice device = result.getDevice();
                logD("Near device: " + device.getName() + ": " + result
                        .getRssi());
                boolean isDeviceClass = device.getName() != null && device
                        .getName().startsWith(deviceNamePrefix);
                if (isDeviceClass) {
                    notifyNearDevice(device, result);
                }
                if (isDeviceClass && result.getRssi() >= rssiTapThreshold) {
                    paired = true;
                    stopScanningIfScanning();
                    BLEService.this.device = device;
                    connectToGatt(device);
                    notifyOnTapped();
                }
            }
        };
        bleScanner.startScan(new ArrayList<ScanFilter>(), new ScanSettings.Builder()
                .setScanMode
                        (SCAN_MODE_LOW_LATENCY).build(), scanBack);
        notifyStartedScanning();
    }

    private void notifyStartedScanning() {
        lbm.sendBroadcast(new Intent(BLE_STARTED_SCANNING));
    }

    private void notifyNearDevice(BluetoothDevice device, ScanResult result) {
        Intent intent = new Intent(BLE_NEAR_DEVICE);
        String name = device.getName();
        int rssi = result.getRssi();
        deviceRssiMap.put(name, rssi);

        int maxRssi=-1000;
        String closest = name;
        for (String deviceName : deviceRssiMap.keySet()) {
            if (deviceRssiMap.get(deviceName) > maxRssi) {
                maxRssi = deviceRssiMap.get(deviceName);
                closest = deviceName;
            }
        }

        intent.putExtra(Key.deviceName, closest);
        intent.putExtra(Key.rssi, maxRssi);
        lbm.sendBroadcast(intent);
    }

    private void notifyOnTapped() {
        Intent intent = new Intent(BLE_TAPPED_DEVICE);
        intent.putExtra(Key.deviceName, device.getName());
        lbm.sendBroadcast(intent);
    }

    private void notifyOnSubscribed() {
        lbm.sendBroadcast(new Intent(BLE_SUBSCRIBED));
    }

    private void pump() {
        if (queue.size() > 0) {
            synchronized (lock) {
                logD("pumping");
                queue.poll().run();
            }
        }
    }

    private void dispose() {
        synchronized (lock) {
            stopScanningIfScanning();
            if (gatt != null) {
                gatt.disconnect();
                gatt.close();
            }
        }
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            logD("onConnectionStateChange");
            logD("context.getMainLooper().getThread()=" + context
                    .getMainLooper().getThread().getId());
            logD("currentThread=" + Thread.currentThread().getId());
            logD("gatt = [%s], status = [%d], newState = [%d]", gatt, status, newState);
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                logD("Connected");
                onConnected(gatt);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                logD("disconnected");
                onDisconnected();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int
                status) {
            super.onServicesDiscovered(gatt, status);
            logD("onServicesDiscovered gatt = [" + gatt +
                    "], status = [" +
                    status + "]");


            List<BluetoothGattService> services = gatt.getServices();
            logD("services found: %d", services.size());
            for (BluetoothGattService service : services) {
                if (service.getUuid().toString().endsWith
                        ("00805f9b34fb")) {
                    continue;
                }
                for (BluetoothGattCharacteristic charac : service.getCharacteristics()) {
                    logD("Found characteristic: " +
                            service.getUuid() + " " + charac.getUuid());

                    for (final BluetoothGattDescriptor desc : charac
                            .getDescriptors()) {
                        queue.add(new Runnable() {
                            @Override
                            public void run() {
                                gatt.readDescriptor(desc);
                            }
                        });
                    }
                }
            }
            final BluetoothGattService servicea = gatt.getService
                    (SERVICE_UUID);
            final BluetoothGattCharacteristic notifyCharacteristic = servicea
                    .getCharacteristic(NOTIFY_UUID);
            writeCharacteristic = servicea
                    .getCharacteristic(WRITE_UUID);

            queue.add(new Runnable() {
                @Override
                public void run() {
                    logD("Logged all the characteristics");
                    gatt.setCharacteristicNotification(notifyCharacteristic, true);
                    BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(NOTIFICATION_DESCRIPTOR_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
            });
            queue.add(new Runnable() {
                @Override
                public void run() {
                    logD("subscribed, now writing characteristic");
                    subscribed = true;
                    notifyOnSubscribed();
                    while (onSubscribed.size() > 0) {
                        queue.add(onSubscribed.poll());
                    }
                    pump();
                }
            });
            pump();

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            pump();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            logD("onCharacteristicWrite");
            pump();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (!characteristic.getUuid().equals(NOTIFY_UUID)) {
                return;
            }

            byte[] value = characteristic.getValue();
            if (messageDecoder == null) {
                messageDecoder = new MessageDecoder();
            }

            Message message;
            try {
                message = messageDecoder.push(value);
            } catch (ChecksumError e) {
                logD("checksum failed");
                messageDecoder = null;
                throw e;
            }
            if (message != null) {
                logD("found tagged data");
                messageDecoder = null;
                Intent intent = new Intent(BLE_RECEIVED_MESSAGE);
                int requestId = message.id;
                lastMessageId = requestId;
                if (receivers.containsKey(requestId)) {
                    ResultReceiver receiver = receivers.remove(requestId);
                    Bundle resultData = new Bundle();
                    resultData.putByteArray(Key.data, message.data);
                    receiver.send(0, resultData);
                }
                intent.putExtra(Key.data, message.data);
                intent.putExtra(Key.type, message.type);
                lbm.sendBroadcast(intent);
            } else {
                logD("waiting more chunks");
            }
            pump();
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);

            BluetoothGattCharacteristic charac = descriptor
                    .getCharacteristic();
            BluetoothGattService service = charac.getService();
            byte[] value = descriptor.getValue();
            logD("Found descriptor: status=" + status + " " +
                    service.getUuid() + " " + charac.getUuid() +
                    " desc: " + new String(value) + " byte[].length " +
                    value.length + " desc.uuid " + descriptor.getUuid());
            pump();

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            pump();

        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            pump();

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            pump();

        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            pump();
        }
    };

    private void onDisconnected() {
        // we maintain the paired status
        paired = !!paired;
        subscribed = false;
    }

    private void queueRequest(final int requestId, int requestMethod, byte[] data) {
        if (data == null) {
            data = new byte[0];
        }
        ArrayList<byte[]> chunks = MessageEncoder.encodeMessage(requestMethod,
                requestId, data);
        final Queue<Runnable> workQueue = subscribed ? this.queue : this.onSubscribed;
        for (final byte[] chunk : chunks) {
            Runnable writeChunk = new Runnable() {
                @Override
                public void run() {
                    logD("writing chunk: %s", Hex.encode(chunk));
                    writeCharacteristic.setValue(chunk);
                    BLEService.this.gatt.writeCharacteristic
                            (writeCharacteristic);
                }
            };
            workQueue.add(writeChunk);
        }
//        workQueue.add(new Runnable() {
//            @Override
//            public void run() {
//                // This ought to cause a wait until the response has been
//                // processed ...
//                if (!(lastMessageId == requestId)) {
//                    workQueue.add(this);
//                }
//            }
//        });
    }

    private void connectToGatt(BluetoothDevice device) {
        logD("connectToGatt");
        gatt = device.connectGatt(context, autoConnect, gattCallback, BluetoothDevice
                .TRANSPORT_LE);

    }

    private void onConnected(BluetoothGatt gatt) {
        gatt.discoverServices();
    }

    static int requestIds = 0;

    public static Intent requestWifi(Context context) {
        return getRequestIntent(context, REQUEST_GET_WIFI_CREDS_AND_IP, null);
    }

    public static Intent getRequestIntent(Context context, int requestMethod,
                                          byte[] data) {
        Intent intent = new Intent();
        intent.setClassName(context, BLEService.class.getCanonicalName());
        intent.putExtra(Key.requestId, requestIds++);
        intent.putExtra(Key.requestMethod, requestMethod);
        if (data != null) {
            intent.putExtra(Key.data, data);
        }
        return intent;
    }

    public static Intent requestPairing(Context context, Class<? extends Activity> callingActivity,
                                        ResultReceiver resultReceiver) {
        Intent intent = new Intent();
        intent.setClassName(context,
                BLEService.class.getCanonicalName());
        intent.putExtra(Key.pairingRequested, true);
        intent.putExtra(Key.callingActivity, callingActivity.getCanonicalName());
        intent.putExtra(Key.pairingReceiver, resultReceiver);
        intent.putExtra(Key.deviceNamePrefix, Strings.cartPeripheralName);
        return intent;
    }


    private int starts=0;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        starts++;

        logD("intent: " + intent);
        if (intent != null && intent.hasExtra(Message.REQUEST_ID)) {
            handleRequest(intent);
        } else if (intent != null && intent.hasExtra(Key.pairingRequested)) {
            ResultReceiver receiver = intent.getParcelableExtra
                    (Key.pairingReceiver);

            if (starts == 1 || !paired) {
                // This should call `finish()`
                receiver.send(0, null);
                Intent launchPairing = new Intent(this,
                        BLEPairingActivity.class);
                launchPairing.putExtra(Key.callingActivity, intent
                        .getStringExtra(Key.callingActivity));
                // Needed for android < 7.0 (TODO: confirm theory)
                launchPairing.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchPairing);
                handleStart(intent);
            } else {
                // TODO: Is this actually necessary ?
                // also: using a ResultReceiver vs BroadcastReceiver ... the
                // latter is easily toggled by add/remove)Receiver in
                // onPause/onResume
                receiver.send(1, null);
            }

        } else if (intent != null && paired) {
            handleAlreadyPaired();
        } else if (intent != null) {
            handleStart(intent);

        }
        return START_STICKY;
    }

    private void handleAlreadyPaired() {
        notifyOnTapped();
    }

    private void handleStart(Intent intent) {
        configureFrom(intent);
        String prefix = intent.getStringExtra(Key.deviceNamePrefix);
        if (prefix != null) {
            dispose();
            startScanning(prefix);
        }
    }

    private void handleRequest(Intent intent) {
        int requestId = intent.getIntExtra(Message.REQUEST_ID,
                            -1);
        byte[] data = intent.getByteArrayExtra(Key.data);
        int  requestMethod = intent.getIntExtra(Key.requestMethod, 0);
        boolean wasEmpty = queue.size() == 0;
        logD("sending request with requestMethod=%d, requestId=%d, " +
                "data=%s, wasEmpty=%s subscribed=%s", requestMethod, requestId,
                data == null ?
                "null" : Hex
                .encode
                (data), wasEmpty, subscribed);
        if (intent.hasExtra(Key.resultReceiver)) {
            receivers.put(requestId, (ResultReceiver) intent
                    .getParcelableExtra
                    (Key.resultReceiver));
        }
        queueRequest(requestId, requestMethod, data);
        // We only need to pump if there was nothing in the queue
        if (wasEmpty) {
            // If we aren't connected, queueRequest will put the
            // characteristic writes into the `onSubscribed` queue and this
            // pump call will be a `noop`.
            if (subscribed) {
                pump();
            }
        }
    }
}
