package com.goqual.blueswitch001;

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
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by pc on 2015-08-24.
 */
public class BluetoothService extends Service {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothGatt mBluetoothGatt;
    private String mac_address;


    private int mConnectionState = STATE_CONNECTING;



    public static final int STATE_DISCONNECTED= 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;


    private ArrayList<BluetoothGattService> gattServices = new ArrayList<BluetoothGattService>();
    private ArrayList<BluetoothGattCharacteristic> writableCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
    private ArrayList<BluetoothGattCharacteristic> readableCharacteristics = new ArrayList<BluetoothGattCharacteristic>();





    public final static String ACTION_BLUETOOTH_STATE_CHANGE ="android.bluetooth.adapter.action.STATE_CHANGED";

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public boolean initialize(String mac_address) {
        this.mac_address = mac_address;
        Util.log("bluetooth service - initialize  - mac : " + this.mac_address);
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Util.log("bluetooth service - initialize  - BluetoothManager ==null");

                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Util.log("bluetooth service - initialize  - BluetoothAdapter is null");
            return false;
        }
        Util.log("bluetooth service is initialized successfully");

        return true;
    }

    public void finalize() {

    }

    public boolean connect() {
        if(mBluetoothAdapter ==null || mac_address ==null){
            Util.log("bluetooth service - connect - BluetoothAdapter or mac_address is null");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac_address);
        if(device ==null){
            Util.log("bluetooth service - connect - device is null");
            return false;
        }

        synchronized(this){
            mBluetoothGatt = device.connectGatt(this,false,mGattCallback);
        }

        mConnectionState = STATE_CONNECTING;
        return true;

    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;

        }
    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback(){

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
           if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Util.log("bluetooth service - gattcallback -  connected to GATT server");


               //discoverSercvies - 서버에있는 서비스들을 모르기때문에 처음에 확인함함
               if(mBluetoothGatt.discoverServices())
                {
                    Util.log("bluetooth service - gattcallback -  start service discovery");
                }
                else{
                    Util.log("bluetooth service - gattcallback -  start service discovery - failed");
                }


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Util.log("bluetooth service - gattcallback -  disconnected from GATT server");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                processServices(gatt.getServices());
            } else {
               Util.log("bluetooth service - onServicesDiscovered received: " + status);
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("onCharacteristicRead  " + characteristic.getUuid().toString());
               // broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
        @Override
        public void  onDescriptorWrite(BluetoothGatt gatt,
                                       BluetoothGattDescriptor characteristic,
                                       int status){
            System.out.println("onDescriptorWrite  "+characteristic.getUuid().toString()+" "+status);
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            System.out.println("onCharacteristicChanged  " + new String(characteristic.getValue()));
           // broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

    };


    private void processServices(List<BluetoothGattService> services){


        for( BluetoothGattService service : services){
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

            for(BluetoothGattCharacteristic characteristic : characteristics){
                UUID uuid = characteristic.getUuid();
                mBluetoothGatt.setCharacteristicNotification(characteristic,true);
                mBluetoothGatt.readCharacteristic(characteristic);

            }







        }


    }


}
