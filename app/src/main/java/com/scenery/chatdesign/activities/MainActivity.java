package com.scenery.chatdesign.activities;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.scenery.chatdesign.R;
import com.scenery.chatdesign.adapters.MsgItemAdapter;
import com.scenery.chatdesign.models.UserMsg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.PopupWindow;
import android.widget.Toast;


import java.io.IOException;
import java.util.List;
import java.util.UUID;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    //````````````````````````````````
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String exitCode = "!@$#$#%$^%&%^*((";
    private static final String codeType = "GBK";
    private BluetoothSocket socket;
    private Thread AcceptPushThread;
    private List<String> devices;
    private List<BluetoothDevice> deviceList;
    private ArrayAdapter<String> mAdaptor;
    private ListView listview;
    //想调用蓝牙模块，就必须获得下面的adapter实例。
    private BluetoothAdapter bluetoothAdapter;
    private BlueToothReceiver blueToothReceiver;

    private  Object obj = new Object();
    private boolean search_mutex;
    private Handler subhandler;
    private Handler UIhandler;

    // popup
    PopupWindow popupWindow;
    View popupView;
    ProgressDialog pDialog;

    private void showDevicePopup() {
        if (popupWindow == null) {
            popupWindow = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow.setFocusable(true);

            ColorDrawable dw = new ColorDrawable(0xeedddddd);
            popupWindow.setBackgroundDrawable(dw);
            popupWindow.setAnimationStyle(android.R.style.Animation_InputMethod);
        }

        popupWindow.showAtLocation(MainActivity.this.findViewById(R.id.main), Gravity.BOTTOM, 0, 0);
    }

    //宠物
    private PetInfo pet;



    EditText text;
    ImageButton sendButton;

    ArrayList<UserMsg> list = new ArrayList<>();
    MsgItemAdapter simpleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (EditText) findViewById(R.id.MsgText);
        sendButton = (ImageButton) findViewById(R.id.sendBtn);

        UserMsg msg = new UserMsg("Hello", UserMsg.SYSTEM_MSG);
        list.add(msg);

        ListView listView = (ListView) findViewById(R.id.MsgView);
        simpleAdapter = new MsgItemAdapter(this, list);
        listView.setAdapter(simpleAdapter);

        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (text.getText().toString().length() != 0) {
                    sendButton.setEnabled(true);
                } else {
                    sendButton.setEnabled(false);
                }
            }
        });


        //````````````````````````````````
        devices = new ArrayList<String>();
        deviceList = new ArrayList<BluetoothDevice>();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,1);
        }
        Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        enable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600); //3600为蓝牙设备可见时间
        startActivity(enable);

        // popup
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        popupView = inflater.inflate(R.layout.device_popup, null);
        pDialog = new ProgressDialog(this);
        pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pDialog.setMessage("Waiting to connect...");
        pDialog.setIndeterminate(true);
        pDialog.setCancelable(true);
        pDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                try {
                    subhandler.sendEmptyMessage(1);
                }catch (Exception e) {}
            }
        });

        listview = (ListView) popupView.findViewById(R.id.deviceList);

        //开启搜索蓝牙

        //listview = (ListView) findViewById(R.id.deviceList);
        mAdaptor = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,devices);
        listview.setAdapter(mAdaptor);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (popupWindow != null)
                    popupWindow.dismiss();
                new ConnectThread(deviceList.get(position)).start();
            }
        });


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        blueToothReceiver = new BlueToothReceiver();
        registerReceiver(blueToothReceiver, filter);


        //search




        search_mutex = false;

        //宠物
        pet = new PetInfo("halo");

        UIhandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case -2:
                        Toast.makeText(getBaseContext(), "Failed, plz try again", Toast.LENGTH_SHORT).show();
                        break;
                    case -1:
                        simpleAdapter.notifyDataSetChanged();
                        break;
                    case 0:
                        getSupportActionBar().setTitle(msg.obj.toString());
                        list.add(new UserMsg("Connection established.", UserMsg.SYSTEM_MSG));
                        simpleAdapter.notifyDataSetChanged();
                        if (popupWindow != null)
                            popupWindow.dismiss();
//                        ViewGroup.LayoutParams params = listview.getLayoutParams();
//                        params.height = 0;
//                        listview.setLayoutParams(params);
                        break;
                    case 1:
//                        ViewGroup.LayoutParams params2 = listview.getLayoutParams();
//                        params2.height = 100;
//                        listview.setLayoutParams(params2);
                        getSupportActionBar().setTitle(R.string.app_name);
                        list.add(new UserMsg("Connection closed.", UserMsg.SYSTEM_MSG));
                        simpleAdapter.notifyDataSetChanged();
                        break;
                    case 2:
                        pDialog.show();
                        break;
                    case 3:
                        pDialog.hide();
                        break;
                    case 4:
                        list.add(new UserMsg("Connection failed", UserMsg.SYSTEM_MSG));
                        simpleAdapter.notifyDataSetChanged();
                        break;
                }
            }
        };


    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                SearchDevices();
                showDevicePopup();
                return true;
            case R.id.accpet:
                if (AcceptPushThread == null || AcceptPushThread.isAlive()==false) {
                    AcceptPushThread = new AcceptThread();
                    AcceptPushThread.start();
                    synchronized (MY_UUID) {
                        try {
                            MY_UUID.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    subhandler.sendEmptyMessage(2);
                }
                return true;
            case R.id.cancel:
                try {
                    subhandler.sendEmptyMessage(1);
                }catch (Exception e) {}
                return true;
            //                 return super.onOptionsItemSelected(item);
        }
        return  true;
    }

    public void SendMessage_Click(View view) {
        Message newmsg = Message.obtain();
        newmsg.obj = text.getText();
        newmsg.what = 0;
        try {
            subhandler.sendMessage(newmsg);
        }catch (Exception e) {
            e.printStackTrace();
        }
        text.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar,menu);
        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public void onDestroy() {
        unregisterReceiver(blueToothReceiver);
        super.onDestroy();
    }

    //开始查找，发送广播
    private void SearchDevices() {
        if (!search_mutex)
        {
            search_mutex = true;
            bluetoothAdapter.startDiscovery();
        }
    }


    public class PetInfo {
        public PetInfo(){
            this.PetName = "Pat";
        }
        public PetInfo(String name){
            this.PetName = name;
        }
        public void SetName(String name) {
            this.PetName = name;
        }
        public String getPetName() {
            return PetName;
        }
        private String PetName;
    }


    public class BlueToothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (devices.indexOf(device.getName()) == -1) {
                    devices.add(device.getName());
                    deviceList.add(device);
                }
            }
            mAdaptor.notifyDataSetChanged();
            Toast.makeText(context,"search finished", Toast.LENGTH_SHORT).show();
            search_mutex = false;
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private String sendMessage = "";
        private DataOutputStream out;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            socket = tmp;
        }


        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.i("TAG", "thread start");
                socket.connect();

                if (socket == null) {
                    UIhandler.sendEmptyMessage(-2);
                } else {
                    out = new DataOutputStream(socket.getOutputStream());
                    Message Titlemsg = Message.obtain();
                    Titlemsg.obj = mmDevice.getName();
                    Titlemsg.what = 0;
                    UIhandler.sendMessage(Titlemsg);
                    //接受线程的开启
                    new ManageSocket().start();

                    Looper.prepare();
                    subhandler = new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            try {
                                switch (msg.what) {
                                    //msg send
                                    case 0:
                                        sendMessage = msg.obj.toString();
                                        out.writeUTF(sendMessage);
                                        UserMsg umsg = new UserMsg(sendMessage, UserMsg.SEND_MSG);
                                        list.add(umsg);
                                        UIhandler.sendEmptyMessage(-1);
                                        break;
                                    case 1:
                                        UIhandler.sendEmptyMessage(1);
                                        getLooper().quit();
                                        break;
                                    case 3:
                                        UIhandler.sendEmptyMessage(1);
                                        getLooper().quit();
                                        break;

                                }
                            } catch (Exception e) {
                                getLooper().quit();
                            }
                        }
                    };
                    Looper.loop();
                }
            } catch (IOException connectException) {
                UIhandler.sendEmptyMessage(4);
                // Unable to connect; close the socket and get out
                //return;
            } finally {
                try {
                    out.close();
                    socket.close();
                    sleep(1000);
                    socket = null;
                    out = null;
                    subhandler.removeCallbacksAndMessages(null);
                    Log.i("out", "close socket in Con");
                } catch (Exception e)  {

                }
            }


        }


        /** Will cancel an in-progress connection, and close the socket */


    }


    private class ManageSocket extends Thread {
        private DataInputStream in;
        private String tmp;
        public ManageSocket() {
            super();
            try {
                in = new DataInputStream(socket.getInputStream());
                tmp = "";
            }catch (Exception e) {}
        }

        @Override
        public void run() {
            while (true) {
                try {
                    tmp = in.readUTF();
                    UserMsg rmsg = new UserMsg(tmp, UserMsg.RECEIVE_MSG);
                    list.add(rmsg);
                    UIhandler.sendEmptyMessage(-1);
                } catch (IOException e) {
                    subhandler.sendEmptyMessage(3);
                    try {
                        sleep(1);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    break;
                }
            }
            try {
                in.close();
                in = null;

            } catch (IOException e) {Log.i("Man", "close in");}
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String sendMessage = "";
        private DataOutputStream out;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("TestServer", MY_UUID);
            } catch (IOException e) {
                Log.i("RFComm", "create");
            }
            mmServerSocket = tmp;

        }

        public void run() {
            //BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                if (Looper.myLooper() == null) {
                    Looper.prepare();
                    subhandler = new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            try {
                                switch (msg.what) {
                                    //msg send
                                    case 0:
                                        sendMessage = msg.obj.toString();
                                        out.writeUTF(sendMessage);

                                        UserMsg umsg = new UserMsg(sendMessage, UserMsg.SEND_MSG);
                                        list.add(umsg);
                                        UIhandler.sendEmptyMessage(-1);
                                        break;
                                    case 1:
                                        Log.i("out", "close socket in AC");
                                        out.close();
                                        out = null;
                                        UIhandler.sendEmptyMessage(1);
                                        socket.close();
                                        socket = null;
                                        getLooper().quit();
                                        break;
                                    case 3:
                                        UIhandler.sendEmptyMessage(1);
                                        subhandler.sendEmptyMessage(2);
                                        break;
                                    case 2:
                                        UIhandler.sendEmptyMessage(2);
                                        socket = mmServerSocket.accept();
                                        UIhandler.sendEmptyMessage(3);
                                        if (socket != null) {
                                            out = new DataOutputStream(socket.getOutputStream());
                                            Message Titlemsg = Message.obtain();
                                            Titlemsg.obj = socket.getRemoteDevice().getName();
                                            Titlemsg.what = 0;
                                            UIhandler.sendMessage(Titlemsg);
                                            new ManageSocket().start();
                                        }
                                        break;
                                }
                            } catch (Exception e) {
                                getLooper().quit();
                            }
                        }
                    };
                    synchronized (MY_UUID) {
                        MY_UUID.notify();
                    }
                    Looper.loop();
                    break;
                }

            }
            try {
                if (mmServerSocket != null) {
                    mmServerSocket.close();
                }
            } catch (Exception e) {
                Log.i("Conerror", "close socket");
            }
            subhandler.removeCallbacksAndMessages(null);
            //接受线程的开启

        }

    }
}