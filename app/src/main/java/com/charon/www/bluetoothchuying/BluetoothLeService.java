package com.charon.www.bluetoothchuying;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2017/2/23.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLeService extends Service {
    private int currentNum;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;//蓝牙设备地址
    public ArrayList<BluetoothGatt> mBluetoothGattList = new ArrayList<BluetoothGatt>();
    private int mConnectionState = STATE_DISCONNECTED;
    public int mRssiArray[] = new int[7];

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.charon.www.bluetoothchuying.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.charon.www.bluetoothchuying.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.charon.www.bluetoothchuying.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.charon.www.bluetoothchuying.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.charon.www.bluetoothchuying.EXTRA_DATA";
    public final static String READ_RSSI = "com.charon.www.bluetoothchuying.READ_RSSI";
    SharedPreferences spre;
    /*public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID
            .fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);*/

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular
        // example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    public void close() {
        Log.d("123", "current" + currentNum);
        if (mBluetoothGattList.isEmpty()) {
            return;
        }
        listClose(null);
    }
    private synchronized void listClose(BluetoothGatt gatt) {
        if (!mBluetoothGattList.isEmpty()) {
            if (gatt != null) {
                for(final BluetoothGatt bluetoothGatt:mBluetoothGattList){
                    if(bluetoothGatt.equals(gatt)){
                        bluetoothGatt.close();

                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    Thread.sleep(250);
                                    mBluetoothGattList.remove(bluetoothGatt);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }
            }else{
                for (BluetoothGatt bluetoothGatt : mBluetoothGattList) {
                    bluetoothGatt.close();
                }
                mBluetoothGattList.clear();
            }
        }
    }
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    //3
    public boolean initialize() {//3
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.d("123", "不能初始化BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.d("123", "不能获取a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    //4
    public boolean connect(final String address,int currentNum) {//4
        Log.d("123", "连接" + mBluetoothDeviceAddress);
        if (mBluetoothAdapter == null || address == null) {
            Log.d("123",
                    "BluetoothAdapter不能初始化 or 未知 address.");
            return false;
        }

        /*// 以前连接过的设备，重新连接. (��ǰ���ӵ��豸�� ������������)
        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGattList.get(currentNum) != null) {
            Log.d("123",
                    "尝试使用现在的 mBluetoothGatt连接.");
            if (mBluetoothGattList.get(currentNum).connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }*/

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.d("123", "设备没找到，不能连接");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mBluetoothDeviceAddress = address;
        BluetoothGatt bluetoothGatt;
        bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        if(checkGatt(bluetoothGatt)){
            mBluetoothGattList.add(bluetoothGatt);
        }

        //这个方法需要三个参 数：一个Context对象，自动连接（boolean值,表示只要BLE设备可用是否自动连接到它），和BluetoothGattCallback调用。
        Log.d("123", "Trying to create a new connection.");
        mConnectionState = STATE_CONNECTING;
        //mBluetoothGatt.readRemoteRssi();
        return true;
    }
    private boolean checkGatt(BluetoothGatt bluetoothGatt) {
        if (!mBluetoothGattList.isEmpty()) {
            for(BluetoothGatt btg:mBluetoothGattList){
                if(btg.equals(bluetoothGatt)){
                    return false;
                }
            }
        }
        return true;
    }
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGattList.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        for(BluetoothGatt bluetoothGatt:mBluetoothGattList){
            bluetoothGatt.disconnect();
        }
        mBluetoothGattList.clear();
    }

    public void disconnectOne(int currentNum) {
        if (mBluetoothAdapter == null || mBluetoothGattList.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGattList.get(currentNum).disconnect();
        mBluetoothGattList.remove(currentNum);
        Log.d("123", "disconnectOne gattList" + mBluetoothGattList.size());
    }

    private int findWhichPage(String address) {
        int num = 0;
        spre = getSharedPreferences("myPref", MODE_PRIVATE);
        for (int i = 1; i <= MainActivity.bleId; i++) {
            if (address.equals(spre.getString("Address" + i, "none"))) {//遍历每个address
                num = --i;
                break;
            }
        }
        Log.d("123", "选择的页数：" + num);
        return num;
    }

    //通过BLE API的不同类型的回调方法
    // Implements callback methods for GATT events that the app cares about. For
    // example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {//5
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            String intentAction;
            Log.d("123", "广播address" + gatt.getDevice().getAddress());
            String address = gatt.getDevice().getAddress();

            if (newState == BluetoothProfile.STATE_CONNECTED) {//当连接状态发生改变
                intentAction = ACTION_GATT_CONNECTED+address;
                mConnectionState = STATE_CONNECTED;
                Log.d("123", "连接GATT server"+intentAction);
                // Attempts to discover services after successful connection.
                Log.i("123", "Attempting to start service discovery:"
                        + mBluetoothGattList.get(currentNum).discoverServices());
                broadcastUpdate(intentAction);//6
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//当设备无法连接
                intentAction = ACTION_GATT_DISCONNECTED+address;
                mConnectionState = STATE_DISCONNECTED;
                Log.i("123", "Disconnected from GATT server."+intentAction);
                broadcastUpdate(intentAction);   //发送广播
            }
        }

        @Override
        // 发现新服务端
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            String address = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED+address);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        // 读写特性
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("onCharacteristicRead");
            String address = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE+address, characteristic);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            Log.d("123","onDescriptorWriteonDescriptorWrite = " + status
                    + ", descriptor =" + descriptor.getUuid().toString());
        }

        //如果对一个特性启用通知,当远程蓝牙设备特性发送变化，回调函数onCharacteristicChanged( ))被触发。
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String address = gatt.getDevice().getAddress();
            broadcastUpdate(ACTION_DATA_AVAILABLE+address, characteristic);
            if (characteristic.getValue() != null) {
                byte[] arrayOfByte = characteristic.getValue();
                Log.d("123CharacteristicChanged", Bytes2HexString(arrayOfByte));
            }
        }


        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d("123", "LeRssi" + rssi);
            String address = gatt.getDevice().getAddress();
            final int findId = findWhichPage(address);
            Log.d("123", "page" + findId+"size"+mBluetoothGattList.size());
            broadcastUpdate(READ_RSSI+address);
            mRssiArray[findId] = rssi;//以首页的位置存
        }

        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            Log.d("123","--------write success----- status:" + status);
        }
    };

    private void broadcastUpdate(final String action) {//9发送广播
        final Intent intent = new Intent(action);
        Log.d("123", "发送了广播");
        sendBroadcast(intent);//广播
    }

    public List<BluetoothGattService> getSupportedGattServices() {//10
        if (mBluetoothGattList.get(currentNum) == null)
            return null;
        return mBluetoothGattList.get(currentNum).getServices();
    }

    private final static byte[] hex = "0123456789ABCDEF".getBytes();

    private static int parse(char c) {
        if (c >= 'a')
            return (c - 'a' + 10) & 0x0f;
        if (c >= 'A')
            return (c - 'A' + 10) & 0x0f;
        return (c - '0') & 0x0f;
    }

    // 从字节数组到十六进制字符串转换
    public static String Bytes2HexString(byte[] b) {
        byte[] buff = new byte[2 * b.length];
        for (int i = 0; i < b.length; i++) {
            buff[2 * i] = hex[(b[i] >> 4) & 0x0f];
            buff[2 * i + 1] = hex[b[i] & 0x0f];
        }
        return new String(buff);
    }

    // 从十六进制字符串到字节数组转换
    public static byte[] HexString2Bytes(String hexstr) {
        byte[] b = new byte[hexstr.length() / 2];
        int j = 0;
        for (int i = 0; i < b.length; i++) {
            char c0 = hexstr.charAt(j++);
            char c1 = hexstr.charAt(j++);
            b[i] = (byte) ((parse(c0) << 4) | parse(c1));
        }
        return b;
    }


    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        // 这是心率测量配置文件。

            // For all other profiles, writes the data formatted in HEX.
            // 对于所有其他的配置文件，用十六进制格式写数据
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(
                        data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                Log.d("123","发送数据广播ppp" + new String(data) + "\n"
                        + stringBuilder.toString());
                intent.putExtra(EXTRA_DATA, new String(data) + "\n"
                        + stringBuilder.toString());
            }

        sendBroadcast(intent);
    }
}
