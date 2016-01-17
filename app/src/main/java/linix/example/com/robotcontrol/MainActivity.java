package linix.example.com.robotcontrol;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    ImageView leftImg;
    ImageView rightImg;
    ImageView forwardImg;
    ImageView backhtImg;
    ImageView cameraImg;
    ImageView speedImg;
    ImageView connectImg;

    RadioGroup biegi;
    RadioButton speed1, speed2, speed3, speed4, speed5;

    Switch cameraSwitch;
    Switch autoSwitch;

    BluetoothClient BTClient;
    RobotControl    robot;
    ProgressDialog connectionDialog;

    private final Handler handler = new Handler(){

        public void  handleMessage(Message msg){
            switch (msg.what){
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1){
                        case BluetoothClient.STATE_CONNECTING:
                            break;

                        case BluetoothClient.STATE_CONNECTED:
                            if (connectionDialog != null)
                                connectionDialog.dismiss();
                            connectionDialog = null;

                            setConnectedComponent();
                            break;
                        case BluetoothClient.STATE_NONE:
                            if (connectionDialog != null)
                                connectionDialog.dismiss();
                            connectionDialog = null;
                            setDisconnectComponent();
                            break;
                    }
                    break;

                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    //readData(readBuf);
                    break;

                case Constants.MESSAGE_WRITE:
                    break;

                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    break;

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        //Aktywacja WIFI
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wm.setWifiEnabled (true);

        biegi     = (RadioGroup) findViewById(R.id.radioGroup);
        biegi.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.speed1:
                        robot.setSpeed(1);
                        break;
                    case R.id.speed2:
                        robot.setSpeed(2);
                        break;
                    case R.id.speed3:
                        robot.setSpeed(3);
                        break;
                    case R.id.speed4:
                        robot.setSpeed(4);
                        break;
                    case R.id.speed5:
                        robot.setSpeed(5);
                        break;

                }
            }
        });

        speed1    = (RadioButton) findViewById(R.id.speed1);
        speed2    = (RadioButton) findViewById(R.id.speed2);
        speed3    = (RadioButton) findViewById(R.id.speed3);
        speed4    = (RadioButton) findViewById(R.id.speed4);
        speed5    = (RadioButton) findViewById(R.id.speed5);

        cameraImg = (ImageView) findViewById(R.id.imageCamera);
        speedImg  = (ImageView) findViewById(R.id.imageSpeed);
        connectImg= (ImageView) findViewById(R.id.imageConnect);

        autoSwitch = (Switch) findViewById(R.id.switchAutoRobot);
        autoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    robot.autoPilot(true);
                    setDisconnectComponent();
                    cameraSwitch.setEnabled(true);
                    autoSwitch.setEnabled(true);
                }
                else
                {
                    robot.autoPilot(false);
                }

            }
        });

        cameraSwitch = (Switch) findViewById(R.id.switchCamera);
        cameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    //ON
                }
                    else
                {
                    //OFF
                }

            }
        });

        leftImg = (ImageView) findViewById(R.id.imageLeft);
        leftImg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        blockDirectionButtons(false, leftImg);
                        robot.goLeft();
                        break;
                    case MotionEvent.ACTION_UP:
                        blockDirectionButtons(true, leftImg);
                        robot.goStop();
                        break;
                }
                return false;
            }
        });

        rightImg = (ImageView) findViewById(R.id.imageRight);
        rightImg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        blockDirectionButtons(false, rightImg);
                        robot.goRight();
                        break;
                    case MotionEvent.ACTION_UP:
                        blockDirectionButtons(true, rightImg);
                        robot.goStop();
                        break;
                }
                return false;
            }
        });

        forwardImg = (ImageView) findViewById(R.id.imageForward);
        forwardImg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        blockDirectionButtons(false, forwardImg);
                        robot.goForward();
                        break;
                    case MotionEvent.ACTION_UP:
                        blockDirectionButtons(true, forwardImg);
                        robot.goStop();
                        break;
                }
                return false;
            }
        });

        backhtImg = (ImageView) findViewById(R.id.imageBack);
        backhtImg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        blockDirectionButtons(false, backhtImg);
                        robot.goBack();
                        break;
                    case MotionEvent.ACTION_UP:
                        blockDirectionButtons(true, backhtImg);
                        robot.goStop();
                        break;
                }
                return false;
            }
        });

        setDisconnectComponent();
        BTClient = new BluetoothClient(getApplicationContext(), handler, BluetoothAdapter.getDefaultAdapter());
        robot = new RobotControl(BTClient);
    }


    private void setDisconnectComponent()
    {
        biegi.setEnabled(false);
        forwardImg.setEnabled(false);
        backhtImg.setEnabled(false);
        leftImg.setEnabled(false);
        rightImg.setEnabled(false);

        autoSwitch.setChecked(false);
        autoSwitch.setEnabled(false);

        cameraSwitch.setChecked(false);
        cameraSwitch.setEnabled(false);

        speedImg.setImageResource(R.drawable.speed0);
        connectImg.setImageResource(R.drawable.start);
        cameraImg.setImageResource(R.drawable.nocapture);
    }

    private void setConnectedComponent()
    {
        biegi.setEnabled(true);
        forwardImg.setEnabled(true);
        backhtImg.setEnabled(true);
        leftImg.setEnabled(true);
        rightImg.setEnabled(true);

        autoSwitch.setChecked(false);
        autoSwitch.setEnabled(true);

        cameraSwitch.setChecked(false);
        cameraSwitch.setEnabled(true);

        speedImg.setImageResource(R.drawable.speed0);
        connectImg.setImageResource(R.drawable.stop);
        cameraImg.setImageResource(R.drawable.nocapture);
    }

    private void blockDirectionButtons(boolean enabled, ImageView clickButton)
    {
        if (enabled)
            setSpeedImage(false);
        else
            setSpeedImage(true);

        if (clickButton.getId() != forwardImg.getId())
            forwardImg.setEnabled(enabled);
        if (clickButton.getId() != backhtImg.getId())
            backhtImg.setEnabled(enabled);
        if (clickButton.getId() != leftImg.getId())
            leftImg.setEnabled(enabled);
        if (clickButton.getId() != rightImg.getId())
            rightImg.setEnabled(enabled);
    }

    private void setSpeedImage(boolean stopped)
    {
        if (stopped)
            speedImg.setImageResource(R.drawable.speed0);
        else
        {
            switch (robot.getSpeed())
            {
                case 1:
                    speedImg.setImageResource(R.drawable.speed1);
                    break;

                case 2:
                    speedImg.setImageResource(R.drawable.speed2);
                    break;

                case 3:
                    speedImg.setImageResource(R.drawable.speed3);
                    break;

                case 4:
                    speedImg.setImageResource(R.drawable.speed4);
                    break;

                case 5:
                    speedImg.setImageResource(R.drawable.speed5);
                    break;
            }
        }
    }
}
