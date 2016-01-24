package linix.example.com.robotcontrol;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.provider.SyncStateContract;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static android.support.v4.app.ActivityCompat.startActivityForResult;


interface Constants{
    public static final int MESSAGE_STATE_CHANGE = 1;

    public static final int MESSAGE_READ  = 2;
    public static final int MESSAGE_WRITE = 3;

    public static final int MESSAGE_TOAST = 10;

    public static final String TOAST = "toast";
}

public class BluetoothClient {

    public static final int STATE_NONE        = 0;
    public static final int STATE_CONNECTING  = 1;
    public static final int STATE_CONNECTED   = 2;

    private Handler BTHandler = null;
    private BluetoothDevice BTDevice = null;
    protected BluetoothAdapter BTAdapter = null;

    protected boolean FoundDevice = false;
    private Context BTContext;
    private int BTState;
    BluetoothConnectingThread BTConnectingThread = null;
    BluetoothConnectedThread BTConnectedThread = null;


    BluetoothClient(Context context, Handler handler, BluetoothAdapter adapter){
        FoundDevice     = false;

        this.BTContext  = context;
        this.BTHandler  = handler;
        this.BTState    = STATE_NONE;

        BTAdapter= adapter;
 }

    private synchronized void setState(int state){
        this.BTState = state;
        BTHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    private void sendToast(String text){
        Message msg = BTHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, text);
        msg.setData(bundle);
        BTHandler.sendMessage(msg);
    }

    public synchronized int getState(){
        return BTState;
    }


    public synchronized boolean searchDevice(String deviceName){
        if (BTAdapter == null) {
            sendToast("Brak odbiornika bluetooth.");
            return false;
        }


        FoundDevice = false;

        Set<BluetoothDevice> pairedDevices = BTAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(deviceName)) {
                    BTDevice = device;
                    FoundDevice = true;
                    break;
                  }
            }
        }

       if (!FoundDevice){
           String msg = String.format("Nie znaleziono urządzenia: %s", deviceName);
           sendToast(msg);
       }

        return FoundDevice;
    }

    public void write(byte[] buffer){
        if (BTState != STATE_CONNECTED)
            return;
        BluetoothConnectedThread r;
        synchronized (this){
            r = BTConnectedThread;
        }

        r.write(buffer);
    }

    public synchronized void connect(){
        if (BTAdapter == null) {
            sendToast("Brak odbiornika bluetooth.");
            setState(STATE_NONE);
            return;
        }

        if (this.FoundDevice == false) {
            sendToast("Brak zdefiniowanego urządzenia bluetooth.");
            setState(STATE_NONE);
            return;
        }

        if (BTState == STATE_CONNECTING){
            if (BTConnectingThread != null){
                BTConnectingThread.cancel();
                BTConnectingThread = null;
            }
        }


        BTConnectingThread = new BluetoothConnectingThread(BTDevice);
        BTConnectingThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized  void disconnect(){
        if (BTConnectingThread != null){
            BTConnectingThread.cancel();
            BTConnectingThread = null;
        }

        if (BTConnectedThread != null){
            BTConnectedThread.cancel();
            BTConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    public synchronized void onSocketConnected(BluetoothSocket socket, BluetoothDevice device){
        if (BTConnectingThread != null){
            BTConnectingThread.cancel();
            BTConnectingThread = null;
        }

        if (BTConnectedThread != null){
            BTConnectedThread.cancel();
            BTConnectedThread = null;
        }


        BTConnectedThread = new BluetoothConnectedThread(socket);
        BTConnectedThread.start();

        sendToast("Ustanowiono połączenie bluetooth.");
        setState(STATE_CONNECTED);
    }

    public synchronized  void onSocketConnectFailed(){
        sendToast("Błąd: Nie można połaczyć się z urządzeniem.");
        setState(STATE_NONE);
    }

    public synchronized void onSocketConnectionLost(){
        sendToast("Połącznie zostało zerwane.");
        setState(STATE_NONE);
    }



/*----[ BEGIN CLASS: BluetoothConnectingThread ]------------
*
* */
private class BluetoothConnectingThread extends Thread{
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothSocket btSocket = null;
    private BluetoothDevice btDevice = null;

    BluetoothConnectingThread(BluetoothDevice device){
        btDevice = device;
        BluetoothSocket tmp = null;
        try{
            tmp = btDevice.createRfcommSocketToServiceRecord(uuid);
        } catch(IOException e){
        }

        btSocket = tmp;
    }

    public void run(){

         try{
            btSocket.connect();
        } catch (IOException e){
            try {
                if (btSocket != null)
                    btSocket.close();

            } catch (IOException err) {
                Log.d("IOException2", err.getMessage());

            }

             Log.d("IOException1", e.getMessage());
            onSocketConnectFailed();
            return;
         }


        synchronized (BluetoothClient.this){
            BTConnectingThread = null;
        }

        if (btSocket.isConnected())
            onSocketConnected(btSocket, btDevice);
    }

    public void cancel(){
        try {
            if (btSocket != null)
                btSocket.close();
        } catch (IOException e){}

        btSocket = null;
    }
}
/*----[ END CLASS: BluetoothConnectingThread ]------------
*
* */


    /*----[ BEGIN CLASS: BluetoothConnectedThread ]------------
    *
    * */
    private class BluetoothConnectedThread extends Thread{

        private final BluetoothSocket   mSocket;
        private final InputStream       mInStream;
        private final OutputStream      mOutStream;


        public BluetoothConnectedThread(BluetoothSocket socket){
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try{
                tmpIn = mSocket.getInputStream();
                tmpOut= mSocket.getOutputStream();
            }catch (IOException e){}

            mInStream = tmpIn;
            mOutStream= tmpOut;
         }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes = 0;

            while (true){
                try{
                    if (mInStream.available() > 1) {
                        bytes = mInStream.read(buffer);
                        Log.e("RECV DATA",String.format("Size : %d",bytes));
                        if (bytes == 2) {
                            BTHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                        }
                    }

                } catch (IOException e){
                    onSocketConnectionLost();
                    Log.e("EXCEPTION threadConnected: ", e.getMessage());
                    break;
                }

            }
        }

        public void write(byte[] buffer){
            try{
                mOutStream.write(buffer);
                Log.e("BLUETOOTH","Send data");
                BTHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e){

            }
        }

        public void cancel(){
            try{
                mSocket.close();
            }catch (IOException e){}

        }
    }
/*----[ END CLASS: BluetoothConnectingThread ]------------
*
* */

}

