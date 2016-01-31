package linix.example.com.robotcontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

interface IPCameraConsts{
    public static final int MESSAGE_CAMERA_FRAME = 100;
}

public class IPCamera{

    private static final String TAG = "MjpegActivity";

    String url;
    String usr;
    String pwd;
    MjpegInputStream is;
    boolean running = false;
    boolean enabled = false;
    Handler h;
    Context ctx;

    IPCamera(String url, String usr, String pwd, Context ctx){
        this.url = url;
        this.usr = usr;
        this.pwd = pwd;
        this.ctx = ctx;
    }

    public void setConfig(String url, String usr, String pwd)
    {
        this.url = url;
        this.usr = usr;
        this.pwd = pwd;
    }

    public void startStream(Handler h){
        this.h = h;
        is = new MjpegInputStream(httpRequest(url,usr,pwd));
        running = true;
    }

    public void startStream(){
        running = true;
    }

    public void stopStream(){
        running = false;
    }

    public boolean getRunningStream(){
        return running;
    }



    public void getFrame(){
        while (running) {
                Bitmap b;
                try {
                    b = is.readMjpegFrame();
                    Message m = h.obtainMessage(IPCameraConsts.MESSAGE_CAMERA_FRAME, b);
                    m.sendToTarget();
                } catch (IOException e) {
                    Log.e("ERROR", e.getMessage());
                }
        }
    }

    public InputStream httpRequest(String url, String usr, String pwd){
        HttpResponse res = null;
        DefaultHttpClient httpclient = new DefaultHttpClient();
        CredentialsProvider credProvider = new BasicCredentialsProvider();
        credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(usr, pwd));
        httpclient.setCredentialsProvider(credProvider);
        Log.e(TAG, "1. Sending http request");
        try {
            res = httpclient.execute(new HttpGet(URI.create(url)));
            Log.e(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
            if(res.getStatusLine().getStatusCode()==401){
                //You must turn off camera User Access Control before this will work
                return null;
            }
            return res.getEntity().getContent();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            Log.e(TAG, "Request failed-ClientProtocolException", e);
            //Error connecting to camera
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Request failed-IOException", e);
            //Error connecting to camera
        }

        return null;

    }
}