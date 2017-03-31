package com.charon.www.bluetoothchuying;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


@RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
public class MainActivity extends AppCompatActivity {
    public static boolean isPause = false;
    ConnectListener connectListener = new ConnectListener();
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 530;
    private static final int REQUEST_ENABLE_BT = 1;
    private List<View> list_view = new ArrayList<>();
    private List<TextView> list_text_connect = new ArrayList<>();
    private List<TextView> list_text_rssi = new ArrayList<>();
    private List<Integer> list_int_connect = new ArrayList<>();
    private ViewpageAdapter adpter;
    private SimpleAdapter mSimpleAdapter;
    private ListView mListView;
    private Toolbar mToolbar;
    private String mListName[] = new String[]{"LED闪动", "提示音+LED闪动", "防丢功能", "尝试连接"};
    public static int bleId = 0;
    SharedPreferences spre;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    private ViewPager mViewPager;
    private int mExit = 0;
    private Handler mHandler;
    private boolean isRegister = false;//是否注册
    public static boolean isScan = false;//是否点击搜索
    private boolean isClick = false;//是否连续点击
    private Timer timer = new Timer();
    private boolean isHandDis = false;//是否点击断开
    private boolean isDelete = false;//是否点击删除

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        init();
        initBle();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);//绑定服务
        Log.d("123", "Try to bindService=" + bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initBle() {
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
        }
    }

    private void init() {
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
                int currentId = mViewPager.getCurrentItem() + 1;
                showNormalDialog(spre.getString("Name" + currentId, "none"));
            }
        });

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ScanActivity.class);
                isScan = true;
                mBluetoothLeService.disconnect();
                startActivity(intent);
            }
        });
    }

    private void disconnectClearView() {
        list_text_connect.clear();
        list_text_rssi.clear();
        list_int_connect.clear();
    }//停止连接的状态

    @Override
    protected void onResume() {
        super.onResume();
        isDelete = false;
        isHandDis = false;
        initView();
    }

    private void initView() {
        if (!mBluetoothAdapter.isEnabled() || mBluetoothAdapter == null) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        bleId = spre.getInt("Id", 0);
        if (!isRegister) {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());//注册广播接受者
            isRegister = true;
        }
        if (bleId != 0 && !isPause) {
            disconnectClearView();
            list_view.clear();
            Log.d("123", "进入加载view");
            mToolbar.setNavigationIcon(R.drawable.delete);
            for (int i = 1; i <= bleId; i++) {
                View view = LayoutInflater.from(this).inflate(R.layout.fragment_main, null);
                TextView text_num = (TextView) view.findViewById(R.id.fragment_text_name);
                TextView text_conncet = (TextView) view.findViewById(R.id.fragment_text_connect);
                TextView text_rssi = (TextView) view.findViewById(R.id.fragment_text_rssi);

                text_num.setText(spre.getString("Name" + i, "none"));
                text_conncet.setText("未连接");
                text_conncet.setOnClickListener(connectListener);
                list_view.add(view);
                list_text_connect.add(text_conncet);
                list_text_rssi.add(text_rssi);
            }

            adpter = new ViewpageAdapter(list_view);
            mViewPager.setAdapter(adpter);   // 让第一个当前页是 0
            if (!isScan) {
                mViewPager.setCurrentItem(0);
            } else {
                int num = bleId - 1;
                mViewPager.setCurrentItem(num);
            }
        }
        isScan = false;
    }

    private class ConnectListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final int currentNum = mViewPager.getCurrentItem();//当前的页数
            Log.d("123", "第一页" + list_text_connect.get(0).getText());
            Log.d("123", "点击时的页数" + currentNum);
            if (!isClick) {
                isClick = false;
                TimerTask task = null;
                task = new TimerTask() {
                    @Override
                    public void run() {
                        isClick = false;

                    }
                };
                timer.schedule(task, 5000);
            } else {
                Toast.makeText(MainActivity.this, "稍等一下改变状态", Toast.LENGTH_SHORT).show();
            }
            if (list_text_connect.get(currentNum).getText().equals("未连接")) {
                Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
                Log.d("123", "Try to bindService=" + bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));
                int currentBleNum = currentNum + 1;
                String bleAddress = spre.getString("Address" + currentBleNum, "error");
                connectBle(bleAddress, currentNum);
                isHandDis = false;
            } else {
                isHandDis = true;
                mBluetoothLeService.disconnectOne(findGattNum(currentNum));
                //list_text_connect.get(currentNum).setText("未连接");
                //list_text_rssi.get(currentNum).setText("信号断开");
            }
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
            list_text_connect.remove(currentNum);
            list_text_rssi.remove(currentNum);
            list_int_connect.clear();
            mBluetoothLeService.disconnect();
            //mBluetoothLeService.mRssiArray[currentNum] = 10;
            adpter.notifyDataSetChanged();
            bleId--;
            if (bleId == 0) {
                mToolbar.setNavigationIcon(null);
            }
            Log.d("123", bleId + "delete");
            editor.putInt("Id", bleId);
            editor.commit();
            Log.d("123", spre.getString("Name" + currentNum, "error" + currentNum));

            for (int i = 0; i < MainActivity.bleId; i++) {
                list_text_connect.get(i).setText("未连接");
                list_text_rssi.get(i).setText("信号断开");
            }

        }
        for (; newId < size; newId++, oldId++) {
            editor.putString("Name" + newId, spre.getString("Name" + oldId, "error" + oldId));
            editor.putString("Address" + newId, spre.getString("Address" + oldId, "error" + oldId));
            editor.commit();
        }
    }

    private void showNormalDialog(String name) {
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setIcon(R.drawable.logo);
        normalDialog.setTitle("删除设备");
        normalDialog.setMessage("是否删除" + name + "，正在连接的设备将会断开");
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isDelete = true;
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
        int currentNum = mViewPager.getCurrentItem();
        /*if (mListConnect.get(currentNum)) {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        }*/
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_disconnect:
                /*Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                Log.d("123", "Try to bindService=" + bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));
                int currentNum = mViewPager.getCurrentItem();
                int currentBleNum = currentNum + 1;
                String bleAddress = spre.getString("Address"+currentBleNum,"error");
                connectBle(bleAddress);
                mListConnect.set(currentNum,true);
                invalidateOptionsMenu();*/
                break;
            case R.id.menu_connect:
                /*mBluetoothLeService.disconnect();
                mListConnect.set(mViewPager.getCurrentItem(),false);
                invalidateOptionsMenu();*/
                break;
        }
        int currentNum = mViewPager.getCurrentItem();
        Log.d("123", "Num" + currentNum);
        return true;
    }

    private int findGattNum(int currentNum) {//通过第几页得到gattList的添加顺序
        int choose = 0;
        for (int i = 0; i < list_int_connect.size(); i++) {
            Log.d("123", "gattNum" + list_int_connect.get(i));
            if (currentNum == list_int_connect.get(i)) {
                choose = i;
                break;
            } else choose = -1;
        }
        Log.d("123", "选择的第几个Gatt:" + choose);
        return choose;
    }

    private void connectBle(String address, int currentNum) {
        while (true) {
            Log.d("123", "进入了while" + mBluetoothLeService);
            if (mBluetoothLeService != null) {
                mBluetoothLeService.connect(address, currentNum);//gatt添加
                list_text_connect.get(currentNum).setText("已连接");
                list_text_rssi.get(currentNum).setText("正在连接");
                list_int_connect.add(currentNum);
                break;
            } else {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {//2

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {

            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            Log.d("123", "LeService" + mBluetoothLeService.toString());
            if (!mBluetoothLeService.initialize()) {//3
                Log.e("123", "Unable to initialize Bluetooth");
                finish();
            } else Log.d("123", "能初始化");
            // 自动连接to the device upon successful start-up
            // 初始化.
            /*int currentNum = mViewPager.getCurrentItem();
            int currentBleNum = currentNum + 1;
            mBluetoothLeService.connect(spre.getString("Address"+currentBleNum,"error"));//4*/
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("123", "没有连接");
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

    //接收广播进行处理
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String actionAddress = intent.getAction();//接收广播
            int actionLength = actionAddress.length();//接收的广播加了地址
//            Log.d("123", "actionaddress" + actionAddress + "length" + actionLength);

            int addressBegin = actionLength - 17;
            String address = actionAddress.substring(addressBegin, actionLength);//地址
            String action = actionAddress.substring(0, addressBegin);//
//            Log.d("123", "action: " + action + "    address: " + address);

            int currentNum = findWhichPage(address);//通过地址找到首页的第几页
            spre.edit().putInt(address, currentNum).apply();
            Log.d("123", currentNum + "current");
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d("123", "Connected");
                int current = spre.getInt(address, -1);
                //  mConnected = true;
                //  updateConnectionState(R.string.connected);//8
                //  mRadarView.setSearching(true);
                //  autoPoint(true);
                //int currentNum = mViewPager.getCurrentItem();
                //mBluetoothLeService.mBluetoothGattList.get(currentNum).readRemoteRssi();
                list_text_connect.get(current).setText("已连接");
                if (mBluetoothLeService.mBluetoothGattList.size() > findGattNum(current) && current != -1) {
                    mBluetoothLeService.mBluetoothGattList.get(findGattNum(current)).readRemoteRssi();//只会一次
                }
                getRssi(true);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED//断开连接后，不能再获取其他的rssi，进行循环判断，
                    .equals(action)) {
                int current = spre.getInt(address, -1);
                Log.d("123", "current" + current);
                int gattNum = findGattNum(current);
                Log.d("123", "DisConnected");
                if (current < list_text_connect.size() && isScan) {//搜索
                    list_text_connect.get(current).setText("未连接");
                    list_text_rssi.get(current).setText("信号断开");
                    Log.d("123", "搜索断开");
                }
                if (!isHandDis && !isScan && !isDelete) {//因设备问题，过远普通断开
                    Log.d("123", "自动断开");
                    list_text_connect.get(current).setText("未连接");
                    list_text_rssi.get(current).setText("信号断开");
                    if (gattNum >= 0) {
                        list_int_connect.remove(gattNum);
                        mBluetoothLeService.mBluetoothGattList.remove(gattNum);
                    }
                }
                if (isHandDis) {//手动断开,
                    Log.d("123", "手动断开");
                    list_text_connect.get(current).setText("未连接");
                    list_text_rssi.get(current).setText("信号断开");
                    if (gattNum >= 0)
                        list_int_connect.remove(gattNum);
                }
                if (isDelete) {
                    Log.d("123", "删除断开");
                    //删除
                    if (mBluetoothLeService.mBluetoothGattList.size() == 0) {
                        isDelete = true;
                    }
                }
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
//                Log.d("123", "发现新服务");
                // 搜索需要的uuid
                //displayGattServices(mBluetoothLeService
                // .getSupportedGattServices());//10
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // displayData(intent
                //  .getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.READ_RSSI.equals(action)) {
                //list_text_rssi.get(currentNum).setText(mBluetoothLeService.readRssi);
            }
        }
    };

    private int findWhichPage(String address) {
        int num = 0;
        for (int i = 1; i <= bleId; i++) {
            if (address.equals(spre.getString("Address" + i, "none"))) {//遍历每个address
                num = --i;
                break;
            } else
                num = -1;
        }
        Log.d("123", "选择的哪页：" + num);
        return num;
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        for (int i = 1; i <= bleId; i++) {
            String address = spre.getString("Address" + i, "none");
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED + address);
            Log.d("123", "address" + address + "i:" + i);
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED + address);
            intentFilter
                    .addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED + address);
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE + address);
            intentFilter.addAction(BluetoothLeService.READ_RSSI + address);
        }
        return intentFilter;
    }

    private void getRssi(final boolean connect) {
        final int currentNum = mViewPager.getCurrentItem();
        int gattId = findGattNum(currentNum);
        if (connect && mExit == 0 && gattId != -1) {
            Log.d("123", "getRssi gattId" + gattId);
            Log.d("123", "gattListSize" + mBluetoothLeService.mBluetoothGattList.size());
            if (mBluetoothLeService.mBluetoothGattList.size() > 0) {
                boolean read = mBluetoothLeService.mBluetoothGattList.get(gattId).readRemoteRssi();
                if (read) {
                    Log.d("123", "读取rssi成功");
                    checkRssi(currentNum);
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (list_text_connect.size() > currentNum){
                            if (list_text_connect.get(currentNum).getText().toString().equals("已连接")) {
                                getRssi(true);
                            }
                        }

                    }
                }, 3000);
            }
        } else {
            list_text_rssi.get(currentNum).setText("信号断开");
        }
    }

    private void checkRssi(final int pageId) {
        Log.d("123", "checkRssi的pageId" + pageId);
        final int rssi = mBluetoothLeService.mRssiArray[pageId];
        Log.d("123", "rssi:" + rssi);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (rssi < 0 && rssi > -30)
                    list_text_rssi.get(pageId).setText("信号很强");
                else if (rssi < 0 && rssi > -45)
                    list_text_rssi.get(pageId).setText("信号较强");
                else if (rssi > -75 && rssi <= -45)
                    list_text_rssi.get(pageId).setText("信号一般");
                else if (rssi <= -75)
                    list_text_rssi.get(pageId).setText("信号较弱");
                else if (rssi == 0)
                    list_text_rssi.get(pageId).setText("正在连接");
                else if (rssi >= 0)
                    list_text_rssi.get(pageId).setText("信号中断");
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRegister) {
            unregisterReceiver(mGattUpdateReceiver);
            isRegister = false;
        }
        isPause = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        if (mBluetoothLeService != null) {
            mBluetoothLeService.close();
            mBluetoothLeService = null;
        }
        mExit = 1;
        Log.i("123", "MainActivity closed!!!");
    }
}
