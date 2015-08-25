package com.goqual.blueswitch001;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.ref.WeakReference;


public class SwitchActivity extends Activity implements View.OnClickListener{


    Button bt_switch1,bt_switch2;
    TextView tv_state,tv_message;

    String mac_address = "F4:B8:5E:94:5C:30";

    private BluetoothService bluetoothService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch);

        bt_switch1 = (Button)findViewById(R.id.bt_switch_1);
        bt_switch1.setOnClickListener(this);
        bt_switch2 = (Button)findViewById(R.id.bt_switch_2);
        bt_switch2.setOnClickListener(this);
        tv_state = (TextView)findViewById(R.id.tv_state);
        tv_message = (TextView)findViewById(R.id.tv_message);




    }

    @Override
    protected void onStart(){
        super.onStart();
        Intent intent = new Intent(this,BluetoothService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        registerReceiver(mBroadcastReceiver, getIntentFilter());
        Util.log("SwitchActivity - onStart - registerReceiver & bindService");
    }
    @Override
    protected void onStop(){
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
        bluetoothService.finalize();
        unbindService(mConnection);
        Util.log("SwitchActivity - onStop - unregisterReceiver & unbindService");
    }




    private ServiceConnection mConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder)service;
            bluetoothService = binder.getService();
            Util.log("SwitchActivity  - service connected");
            if(bluetoothService.initialize(mac_address)) {
                Util.log("SwitchActivity - bluetoothService is initialized");
                bluetoothService.connect();
            }else{

                Util.log("SwitchActivity - bluetoothService isn't initialized");
            }
        }

        //onServiceDisconnected is only called in extreme situations (crashed / killed).
        //which is very unlikely to happen for a local service...
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Util.log("SwitchActivity  - service disconnected");
        }
    };

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.bt_switch_1:
                sendMessage("on");
                break;
            case R.id.bt_switch_2:
                sendMessage("off");
                break;
            default:

                break;
        }
    }

    private void sendMessage(String message){

    }

    private static IntentFilter getIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothService.ACTION_BLUETOOTH_STATE_CHANGE);
        return intentFilter;
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
            Util.log("broadcast - action  : " +action);
            if(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){

            }else if(BluetoothService.ACTION_GATT_CONNECTED.equals(action)){

            }else if(BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)){

            }else if(BluetoothService.ACTION_DATA_AVAILABLE.equals(action)){

            }else if(BluetoothService.ACTION_BLUETOOTH_STATE_CHANGE.equals(action)){
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1)== BluetoothAdapter.STATE_OFF){
                    Util.log("bluetooth off");
                    // additional work is needed;
                    finish();
                }else if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1)== BluetoothAdapter.STATE_ON){
                    Util.log("bluetooth on");


                }else{
                    Util.log("state = " +intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1));
                }
            }

        }
    };







}
