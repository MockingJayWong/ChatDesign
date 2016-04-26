package com.scenery.chatdesign.activities;

import android.bluetooth.BluetoothAdapter;
import android.nfc.tech.IsoDep;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.scenery.chatdesign.R;
import com.scenery.chatdesign.adapters.MsgItemAdapter;
import com.scenery.chatdesign.models.UserMsg;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private volatile boolean IsOpen;
    private Handler subhandler;
    private Handler UIhandler;


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

        UserMsg msg = new UserMsg("Hello", false);
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

        //开启搜索蓝牙

        listview = (ListView) findViewById(R.id.deviceList);
        mAdaptor = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,devices);
        listview.setAdapter(mAdaptor);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
                        ViewGroup.LayoutParams params = listview.getLayoutParams();
                        params.height = 0;
                        listview.setLayoutParams(params);
                        break;
                    case 1:
                        ViewGroup.LayoutParams params2 = listview.getLayoutParams();
                        params2.height = 100;
                        listview.setLayoutParams(params2);
                        getSupportActionBar().setTitle(R.string.app_name);
                }
            }
        };

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                SearchDevices();
                return true;
            case R.id.accpet:
                if (AcceptPushThread == null || !AcceptPushThread.isAlive()) {
                    AcceptPushThread = new AcceptThread();
                    AcceptPushThread.start();
                    IsOpen = true;
                }
                return true;
            case R.id.cancel:
                if (IsOpen) {
                    IsOpen = false;
                    subhandler.sendEmptyMessage(1);
                }
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
                socket.getRemoteDevice();
                if (socket == null) {
                    UIhandler.sendEmptyMessage(-2);
                } else {
                    IsOpen = true;
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

                                        UserMsg umsg = new UserMsg(sendMessage, true);
                                        list.add(umsg);
                                        UIhandler.sendEmptyMessage(-1);
                                        break;
                                    case 1:
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
                } catch (Exception e)  {
                    Log.i("Con", "close socket");
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
            while (IsOpen) {
                try {
                    tmp = in.readUTF();
                    UserMsg rmsg = new UserMsg(tmp, false);
                    list.add(rmsg);
                    UIhandler.sendEmptyMessage(-1);
                    //byte[] buffer = new byte[1024];
//                    if (socket.getInputStream().read(buffer) != -1) {
//                        //在这里处理byte转string类型
//                        String conv = "";
//                        char c;
//                        for (int i = 0; i < 1024; i++) {
//                            if (buffer[i] == 36) {
//                                break;
//                            }
//                            c = (char) buffer[i];
//                            conv = conv + c;
//                        }
//                        UserMsg rmsg = new UserMsg(conv, false);
//                        list.add(rmsg);
//                        UIhandler.sendEmptyMessage(-1);
//                    }

                } catch (IOException e) {
                    subhandler.sendEmptyMessage(1);
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
            try {
                socket = mmServerSocket.accept();
                if (socket != null) {
                    out = new DataOutputStream(socket.getOutputStream());
                    Message Titlemsg = Message.obtain();
                    Titlemsg.obj = socket.getRemoteDevice().getName();
                    Titlemsg.what = 0;
                    UIhandler.sendMessage(Titlemsg);
                    new ManageSocket().start();
                }
            } catch (Exception e) {

            }
            while (IsOpen) {
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

                                        UserMsg umsg = new UserMsg(sendMessage, true);
                                        list.add(umsg);
                                        UIhandler.sendEmptyMessage(-1);
                                        break;
                                    case 1:
                                        out.close();
                                        out = null;
                                        UIhandler.sendEmptyMessage(1);
                                        if (IsOpen == false) {
                                            getLooper().quit();
                                        } else {
                                            socket = mmServerSocket.accept();
                                            if (socket != null) {
                                                out = new DataOutputStream(socket.getOutputStream());
                                                Message Titlemsg = Message.obtain();
                                                Titlemsg.obj = socket.getRemoteDevice().getName();
                                                Titlemsg.what = 0;
                                                UIhandler.sendMessage(Titlemsg);
                                                new ManageSocket().start();
                                            }
                                        }
                                        break;
                                }
                            } catch (Exception e) {
                                getLooper().quit();
                            }
                        }
                    };
                    Looper.loop();
                }

            }
            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
            } catch (Exception e) {
                Log.i("Conerror", "close socket");
            }
            subhandler.removeCallbacksAndMessages(null);
            //接受线程的开启

        }

    }
}

