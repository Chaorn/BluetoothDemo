package com.charon.www.bluetoothchuying.ui.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.charon.www.bluetoothchuying.ui.adapter.LeDeviceListAdapter;
import com.charon.www.bluetoothchuying.R;

import java.util.ArrayList;
import java.util.List;


public class ScanActivity extends AppCompatActivity {
    private final static String TAG = ScanActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 530;
    private BluetoothAdapter mBluetoothAdapter;
    //搜索BUTTON
    private TextView mChooseName,mChooseAddress;
    //搜索结果List
    private ListView resultList;
    //搜索状态的标示
    private boolean mScanning ;
    //扫描时长
    private static final long SCAN_PERIOD =10000;
    //请求启用蓝牙请求码
    private static final int REQUEST_ENABLE_BT = 1;
    //蓝牙适配器
    private LeDeviceListAdapter mBlueToothDeviceAdapter;
    //蓝牙适配器List
    private List<BluetoothDevice> mBlueList = new ArrayList<>();
    SharedPreferences spre;
    //MainActivity mainActivity = new MainActivity();
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanble);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar_scan1);
        setTitle(R.string.none);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationIcon(R.drawable.back32);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.isPause = true;
                finish();
            }
        });
        //for (int i = 0;i<address2.length;i++)
        //address2[i] = "0";
        spre= getSharedPreferences("myPref", MODE_PRIVATE);
        resultList = (ListView) findViewById(R.id.scanlist);
        Context context = this;
        //初始化蓝牙适配器
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "您的设备不支持蓝牙BLE，将关闭", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //初始化适配器
        mBlueToothDeviceAdapter = new LeDeviceListAdapter(mBlueList, context);


        //显示对话框
        resultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mChooseName = (TextView) view.findViewById(R.id.device_name);
                mChooseAddress = (TextView) view.findViewById(R.id.device_address);
                showNormalDialog(mChooseName,mChooseAddress);
            }
        });
        if (ContextCompat.checkSelfPermission(ScanActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            if (!ActivityCompat.shouldShowRequestPermissionRationale(ScanActivity.this,
                    Manifest.permission.READ_CONTACTS)){
                showMessageOKCancel("你必须允许这个权限", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(ScanActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                    }
                });
                return;
            }
            requestPermissions(new String[] {Manifest.permission.WRITE_CONTACTS},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(ScanActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    @Override

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item ) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mBlueToothDeviceAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }


    private void showNormalDialog(final TextView name, final TextView address){
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(ScanActivity.this);
        normalDialog.setIcon(R.mipmap.ic_launcher);
        normalDialog.setTitle("添加设备");
        normalDialog.setMessage("是否添加"+name.getText().toString());
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do传入数据库，建立连接，退出
                        if (MainActivity.bleId == 6){
                            Toast.makeText(ScanActivity.this, "最多添加6个设备", Toast.LENGTH_SHORT).show();
                        }else if (!checkAddress(address.getText().toString())){
                            MainActivity.bleId++;
                            Log.d("123", MainActivity.bleId+"scan");
                            SharedPreferences.Editor editor = spre.edit();
                            editor.putString("Name"+MainActivity.bleId, name.getText().toString());
                            editor.putString("Address"+MainActivity.bleId, address.getText().toString());
                            editor.putInt("Id", MainActivity.bleId);
                            editor.apply();
                            MainActivity.isPause = false;
                            MainActivity.isScan = true;
                            finish();
                        }

                    }
                });
        normalDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        // 显示
        normalDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 确保蓝牙在设备上可以开启
        if (!mBluetoothAdapter.isEnabled() || mBluetoothAdapter == null) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        scanLeDevice(true);
    }
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }
    /**
     * 设备搜索
     *
     * @param enable 是否正在搜索的标示
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Log.d("123","显示"+mScanning);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mScanning) {
                        //scanf.setVisibility(View.VISIBLE);
                        //stop.setVisibility(View.GONE);
                        Log.d("123","回到搜索");
                        mScanning = false;
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        invalidateOptionsMenu();
                    }
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mHandler.sendEmptyMessage(1);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }invalidateOptionsMenu();
    }

    private boolean checkAddress(String address){
        if (MainActivity.bleId != 0){
            Log.d("123", "CHECK");
            for (int i = 1;i<=MainActivity.bleId;i++){
                Log.d("123", address + spre.getString("Address" + i, "none"));
                if (address.equals(spre.getString("Address"+i,"none")) ){
                    Toast.makeText(this, "已经添加该设备", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        }
        return false;
    }

    // Handler
    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1: // Notify change
                    mBlueToothDeviceAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        MainActivity.isPause = true;
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            //Log.d("123", "mLeScanCallback 搜索结果" );
                            //获取到蓝牙设备
                            //可以判断是否添加
                            if (!mBlueList.contains(device)){
                                mBlueList.add(device);
                                //Log.d("123", "mLeScanCallback 搜索结果   " + device.getAddress());
                            }
                            //List加载适配器
                            if (mBlueToothDeviceAdapter.isEmpty()) {
                                //Log.d("123", "mLeDeviceListAdapter为空");
                            } else {
                                //Log.d("123", "mLeDeviceListAdapter设置");
                                resultList.setAdapter(mBlueToothDeviceAdapter);
                            }
                            mHandler.sendEmptyMessage(1);
                        }
                    });
                }
            };
}
