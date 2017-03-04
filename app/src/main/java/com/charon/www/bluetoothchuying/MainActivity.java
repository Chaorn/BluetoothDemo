package com.charon.www.bluetoothchuying;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 530;
    private static final int REQUEST_ENABLE_BT = 1;
    private List<View> list_view;
    private ViewpageAdapter adpter;
    private SimpleAdapter mSimpleAdapter;
    private ListView mListView;
    private Toolbar mToolbar;
    private String mListName[] = new String[]{"LED闪动", "提示音+LED闪动", "防丢功能", "尝试连接"};
    public static int bleId = 0;
    SharedPreferences spre;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spre = getSharedPreferences("myPref", MODE_PRIVATE);
        mListView = (ListView) findViewById(R.id.main_list);
        mSimpleAdapter = new SimpleAdapter(this, getData(), R.layout.main_list, new String[]{"name"}, new int[]{R.id.list_name});
        mListView.setAdapter(mSimpleAdapter);
        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        mToolbar.setTitle("");

        setSupportActionBar(mToolbar);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentId = mViewPager.getCurrentItem()+1;
                showNormalDialog(spre.getString("Name" + currentId, "none"));
            }
        });

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);


        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "您的设备不支持蓝牙BLE，将关闭", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                showMessageOKCancel("你必须允许这个权限", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                    }
                });
                return;
            }
            requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            return;
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ScanActivity.class);
                startActivity(intent);
            }
        });


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        boolean bll = bindService(gattServiceIntent, mServiceConnection,//2
                BIND_AUTO_CREATE);
        if (bll) {
            Log.d("123", "绑定成功");
        } else {
            Log.d("123", "绑定失败");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled() || mBluetoothAdapter == null) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        bleId = spre.getInt("Id", 0);
        if (bleId != 0) {
            list_view = new ArrayList<>();
            mToolbar.setNavigationIcon(R.drawable.delete);
            for (int i = 1; i <= bleId; i++) {
                View view = LayoutInflater.from(this).inflate(R.layout.fragment_main, null);
                TextView txt_num = (TextView) view.findViewById(R.id.fragment_text_name);
                txt_num.setText(spre.getString("Name" + i, "none"));
                list_view.add(view);
            }
            adpter = new ViewpageAdapter(list_view);
            mViewPager.setAdapter(adpter);
            int currentItem = list_view.size() / 2;     // 让第一个当前页是 0
            mViewPager.setCurrentItem(currentItem);
            connectDevice();
        }
    }

    private void deleteView() {
        int currentNum = mViewPager.getCurrentItem();
        int size = list_view.size();
        int oldId = currentNum + 2;
        int newId = currentNum + 1;
        SharedPreferences.Editor editor = spre.edit();
        if (size > 0) {
            list_view.remove(currentNum);
            adpter.notifyDataSetChanged();
            bleId--;
            if (bleId == 0){
                mToolbar.setNavigationIcon(null);
            }
            Log.d("123", bleId + "delete");
            editor.putInt("Id", bleId);
            editor.commit();
            Log.d("123", spre.getString("Name" + currentNum, "error" + currentNum));
        }
        for (; newId < size; newId++, oldId++) {
            editor.putString("Name" + newId, spre.getString("Name" + oldId, "error" + oldId));
            editor.putString("Address" + newId, spre.getString("Address" + oldId, "error" + oldId));
            editor.commit();
        }
    }
    private void showNormalDialog(String name){
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setIcon(R.drawable.logo);
        normalDialog.setTitle("删除设备");
        normalDialog.setMessage("是否删除"+name);
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteView();
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
    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < 4; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("name", mListName[i]);
            list.add(map);
        }
        return list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            int currentNum = mViewPager.getCurrentItem();
            Log.d("123", "Num" + currentNum);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void connectDevice() {

        //7
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());//8

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mAddress);
            Log.d("123", "连接结果" + result);
        } else Log.d("123", "mBluetoothLeService为空");
    }
    private final ServiceConnection mServiceConnection = new ServiceConnection() {//2

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {//3
                Log.e("123", "Unable to initialize Bluetooth");
                finish();
            } else Log.d("123", "能初始化");
            // 自动连接to the device upon successful start-up
            // 初始化.
            mBluetoothLeService.connect(mAddress);//4
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

}
