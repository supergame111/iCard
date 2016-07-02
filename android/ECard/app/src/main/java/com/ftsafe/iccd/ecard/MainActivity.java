package com.ftsafe.iccd.ecard;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.feitian.readerdk.Tool.DK;
import com.ftreader.bluetooth.BlueToothReceiver;
import com.ftsafe.iccd.ecard.fragments.FirstFragment;
import com.ftsafe.iccd.ecard.fragments.SecondFragment;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnMenuTabClickListener;
import com.unionpay.minipay.device.UsbBrocastReceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ftsafe.sdk.BlueToothReader;
import ftsafe.sdk.MiniPay;

public class MainActivity extends FragmentActivity implements OnMenuTabClickListener
        , FirstFragment.OnFirstFragmentInteractionListener
        , SecondFragment.OnSecondFragmentInteractionListener {


    private static final int USB_VENDOR_ID = 2414;

    private BottomBar mBottomBar;
    private BluetoothAdapter mBluetoothAdapter;
    private BlueToothReceiver mBleReceiver;
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private ArrayList<BluetoothDevice> mBlueToothDeviceList;
    private List<String> mDeviceNameList;

    private ProgressDialog mProgressDialog;
    private Fragment mCurrentFragment;
    private FirstFragment mFirstFragment;
    private SecondFragment mSecondFragment;

    public static MiniPay MiniPay;
    public static BlueToothReader BtReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(Config.APP_ID, "onCreate");

        // 初始化全局变量
        initialization();

        // 配置BottomBar
        configBottomBar(savedInstanceState);

        // 注册BLE广播
        registerBLEBrocastReceiver();

        // 初始化Fragment
        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null)
                return;
            // First Fragment
            mFirstFragment = new FirstFragment();
            mFirstFragment.setArguments(getIntent().getExtras());
            // Second Fragment
            mSecondFragment = SecondFragment.newInstance(null, null);

            // Show FirstFragment
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, mFirstFragment)
                    .commit();
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Necessary to restore the BottomBar's state, otherwise we would
        // lose the current tab on orientation change.
        mBottomBar.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 注销广播
        unregisterReceiver(mBleReceiver);
    }

    @Override
    public void onMenuTabSelected(int menuItemId) {
        switch (menuItemId) {
            case R.id.bottomBarItemOne:
                switchFragment(mSecondFragment, mFirstFragment);
                break;
            case R.id.bottomBarItemTwo:
                switchFragment(mFirstFragment, mSecondFragment);
                break;
            default:
                break;
        }
    }

    private void switchFragment(Fragment from, Fragment to) {
        if (mCurrentFragment != to) {
            mCurrentFragment = to;
//            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().setCustomAnimations(
//                    android.R.anim.fade_in, android.R.anim.fade_out);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (to.isAdded()) { //  added
                Log.d(Config.APP_ID, "switch to");
                transaction.hide(from).show(to).commit();
            } else {
                Log.d(Config.APP_ID, "add to");
                transaction.hide(from).add(R.id.fragment_container,to).commit();
            }
        }
    }

    @Override
    public void onMenuTabReSelected(int menuItemId) {

    }

    private void initialization() {
        // USB Manager
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        // 蓝牙设别列表
        mBlueToothDeviceList = new ArrayList<>();
        // 设备名字列表
        mDeviceNameList = new ArrayList<>();
        // 初始化进度对话框
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("请等待...");
        mProgressDialog.setCancelable(false);

    }

    private void registerBLEBrocastReceiver() {
        //To monitor reader connection status
        //#1, register card status monitoring
        BlueToothReceiver.registerCardStatusMonitoring(mHandler);

        //MyBroadcastReceiver
        mBleReceiver = new BlueToothReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        filter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        filter.addAction("android.bluetooth.device.action.FOUND");
        filter.addAction("android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED");
        //register receiver
        registerReceiver(mBleReceiver, filter);
    }

    private void configBottomBar(Bundle savedInstanceState) {
        mBottomBar = BottomBar.attach(this, savedInstanceState);
        mBottomBar.setItems(R.menu.menu_bottombar);
        mBottomBar.setOnMenuTabClickListener(this);
//        mBottomBar.mapColorForTab(0, ContextCompat.getColor(this, R.color.colorAccent));
//        mBottomBar.mapColorForTab(1, "#5D4037");
//        mBottomBar.mapColorForTab(2, "#FF9800");
    }

    private UsbDevice discoverUSBDevice() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            if (device.getVendorId() == USB_VENDOR_ID) {
                return device;
            }
        }
        return null;
    }

    private void discoverBLEReader() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothAdapter.enable();

        // 清空蓝牙设备列表
        mBlueToothDeviceList.clear();
        // 清空设备名字列表
        mDeviceNameList.clear();

        Log.e(Config.APP_ID, "扫描蓝牙设备");
        // 开始扫描
        scanLeDevice(true);

    }

    private void openMiniPay() {
        if (mUsbDevice == null)
            mUsbDevice = discoverUSBDevice();
        if (mUsbDevice != null) {
            MiniPay = new MiniPay(MainActivity.this, mUsbManager);
            MiniPay.openMiniPay(mUsbDevice);
        } else {
            Toast.makeText(this, "请插入设备", Toast.LENGTH_SHORT).show();
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, 50000);

            boolean i = mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String str = device.getName();
                    if (str == null)
                        str = "UnknownDevice";
                    if (!mBlueToothDeviceList.contains(device) && (null != str && (-1 != str.indexOf("FT")))) {
                        Log.e(Config.APP_ID, "bluetooth device address " + device.getAddress());
                        mBlueToothDeviceList.add(device);
                        mDeviceNameList.add(str);

                        //openBleDeviceListFragment();
                    }
                }
            });
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case DK.CARD_STATUS:
                    switch (msg.arg1) {
                        case DK.CARD_ABSENT:
                            // card absent
                            Toast.makeText(MainActivity.this, "Card Absent", Toast.LENGTH_SHORT).show();
                            break;
                        case DK.CARD_PRESENT:
                            // card present
                            Toast.makeText(MainActivity.this, "Card Present", Toast.LENGTH_SHORT).show();
                            break;
                        case DK.CARD_UNKNOWN:
                            // unknown card
                            Toast.makeText(MainActivity.this, "Card Unknown", Toast.LENGTH_SHORT).show();
                            break;
                        case DK.IFD_COMMUNICATION_ERROR:
                            // communication error

                            break;
                    }
                    //To monitor reader bluetooth connection status
                    //#2, get the reader bluetooth status, add put your handle code here
                case BlueToothReceiver.BLETOOTH_STATUS:
                    switch (msg.arg1) {

                        //Once reader bluetooth connected, then do your operation here
                        case BlueToothReceiver.BLETOOTH_CONNECT:
                            // Toast.makeText(TerminalActivity.this, "Connectioned", Toast.LENGTH_SHORT).show();
                            break;
                        case BlueToothReceiver.BLETOOTH_DISCONNECT:
                            //Once bluetooth disconnection, change UI

                            break;
                    }
                case UsbBrocastReceiver.USB_STATUS:
                    switch (msg.arg1) {
                        case UsbBrocastReceiver.USB_PERMISSION:
                            // 打开设备
                            openMiniPay();
                            Log.e(Config.APP_ID, "get Permission");
                            break;
                        case UsbBrocastReceiver.USB_ATTACHED:
                            // 搜索设备
                            mUsbDevice = discoverUSBDevice();
                            // 获取控制权限
                            // mUsbManager.requestPermission(mUsbDevice, mPermissionIntet);
                            break;
                        case UsbBrocastReceiver.USB_DETACHED:
                            // 关闭设备
                            if (MiniPay != null)
                                MiniPay.closeMiniPay();
                            break;
                    }
                default:
                    break;
            }
        }
    };

    @Override
    public void onFirstFragmentInteraction(Uri uri) {

    }

    @Override
    public void onSecondFragmentInteraction(Uri uri) {

    }
}
