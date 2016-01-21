package linix.example.com.robotcontrol;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    ImageView leftImg;
    ImageView rightImg;
    ImageView forwardImg;
    ImageView backhtImg;
    ImageView cameraImg;
    ImageView speedImg;
    ImageView connectImg;
    ImageView animImg;
    AnimationDrawable animation;
    TextView msgView;

    boolean connected = false;

    RadioGroup biegi;
    RadioButton speed1, speed2, speed3, speed4, speed5;

    Switch cameraSwitch;
    Switch autoSwitch;

    BluetoothClient BTClient;
    RobotControl    robot;
    ProgressDialog connectionDialog;
    WifiManager wm;
    CountDownTimer timer;

    String deviceName;
    String addressIP;

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

                            robot.Init();
                            timer = new CountDownTimer(2000, 1000) {
                                @Override
                                public void onTick(long l) {
                                }

                                @Override
                                public void onFinish() {
                                    if (!connected)
                                    {
                                        msgView.setText("Brak odpowiedzi z urządzenia");
                                        connectImg.setImageResource(R.drawable.start);
                                    }
                                }
                            }.start();

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
                    if (readBuf.length == 2){
                        byte cmd    = readBuf[0];
                        byte param  = readBuf[1];
                        onReceiveData(cmd, param);
                    }

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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);


        wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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

        msgView   = (TextView) findViewById(R.id.msgView);

        speed1    = (RadioButton) findViewById(R.id.speed1);
        speed2    = (RadioButton) findViewById(R.id.speed2);
        speed3    = (RadioButton) findViewById(R.id.speed3);
        speed4    = (RadioButton) findViewById(R.id.speed4);
        speed5    = (RadioButton) findViewById(R.id.speed5);

        cameraImg = (ImageView) findViewById(R.id.imageCamera);
        speedImg  = (ImageView) findViewById(R.id.imageSpeed);
        animImg   = (ImageView) findViewById(R.id.animView);
        connectImg= (ImageView) findViewById(R.id.imageConnect);

        connectImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connected)
                    stopRobot();
                else
                    startRobot();
            }
        });

        autoSwitch = (Switch) findViewById(R.id.switchAutoRobot);
        autoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    robot.autoPilot(true);
                }
                else
                {
                    if (robot.pilot == true)
                        robot.autoPilot(false);
                }

            }
        });

        cameraSwitch = (Switch) findViewById(R.id.switchCamera);
        cameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    if (wm.isWifiEnabled()){

                    }
                        else
                        cameraSwitch.setChecked(false);
                }
                    else
                {
                    cameraImg.setImageResource(R.drawable.nocapture);
                }

            }
        });

        leftImg = (ImageView) findViewById(R.id.imageLeft);
        leftImg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        leftImg.setImageResource(R.drawable.button_left_select);

                        blockDirectionButtons(false, leftImg);
                        robot.goLeft();
                        break;
                    case MotionEvent.ACTION_UP:
                        leftImg.setImageResource(R.drawable.button_left);
                        blockDirectionButtons(true, leftImg);
                        robot.goStop();
                        break;
                }
                return true;
            }
        });

        rightImg = (ImageView) findViewById(R.id.imageRight);
        rightImg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        rightImg.setImageResource(R.drawable.button_right_select);
                        blockDirectionButtons(false, rightImg);
                        robot.goRight();
                        break;
                    case MotionEvent.ACTION_UP:
                        rightImg.setImageResource(R.drawable.button_right);
                        blockDirectionButtons(true, rightImg);
                        robot.goStop();
                        break;
                }
                return true;
            }
        });

        forwardImg = (ImageView) findViewById(R.id.imageForward);
        forwardImg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        forwardImg.setImageResource(R.drawable.button_up_select);
                        blockDirectionButtons(false, forwardImg);
                        robot.goForward();
                        break;
                    case MotionEvent.ACTION_UP:
                        forwardImg.setImageResource(R.drawable.button_up);
                        blockDirectionButtons(true, forwardImg);
                        robot.goStop();
                        break;
                }
                return true;
            }
        });

        backhtImg = (ImageView) findViewById(R.id.imageBack);
        backhtImg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        backhtImg.setImageResource(R.drawable.button_down_select);
                        blockDirectionButtons(false, backhtImg);
                        robot.goBack();
                        break;
                    case MotionEvent.ACTION_UP:
                        backhtImg.setImageResource(R.drawable.button_down);
                        blockDirectionButtons(true, backhtImg);
                        robot.goStop();
                        break;
                }
                return true;
            }
        });




        setDisconnectComponent();
        loadConfig();
        BTClient = new BluetoothClient(getApplicationContext(), handler, BluetoothAdapter.getDefaultAdapter());
        robot = new RobotControl(BTClient);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_settings:
                Intent i = new Intent(this, Prefs.class);
                startActivityForResult(i, 0);
                break;

        }

        return true;
    }

    private void loadConfig()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        deviceName = prefs.getString("deviceName","Brak");
        addressIP  = prefs.getString("addressCamera","192.168.1.1");
    }

    private void setDisconnectComponent()
    {
        connected = false;

        biegi.setEnabled(false);
        speed1.setEnabled(false);
        speed2.setEnabled(false);
        speed3.setEnabled(false);
        speed4.setEnabled(false);
        speed5.setEnabled(false);

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

        msgView.setText("");
    }


    private void setConnectedComponent()
    {
        connected = true;
        msgView.setText(String.format("Połączenie z %s",deviceName));

        biegi.setEnabled(true);
        speed1.setEnabled(true);
        speed2.setEnabled(true);
        speed3.setEnabled(true);
        speed4.setEnabled(true);
        speed5.setEnabled(true);

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
        msgView.setText("");
    }

    private void blockDirectionButtons(boolean enabled, ImageView clickButton)
    {
        setSpeedImage(enabled);

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

    private void startRobot()
    {
        loadConfig();

        if (BTClient.searchDevice(deviceName))
        {
            BTClient.connect();
        }
            else
            msgView.setText("Brak połączenia");
    }

    private void stopRobot()
    {
        if (BTClient.getState() == BluetoothClient.STATE_CONNECTED) {
            robot.goStop();
            BTClient.disconnect();
        }

        setDisconnectComponent();
    }

    private void onReceiveData(byte cmd, byte param)
    {
        switch (cmd){
            //Odpowiedź na dostęp do maszyny
            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_INIT:
                setConnectedComponent();
                break;


            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_SET_MOTOR:
                switch (param){
                    case RobotProtocolConsts.PARAM_FORWARD:
                        animImg.setBackgroundResource(R.drawable.anim_up);
                        break;
                    case RobotProtocolConsts.PARAM_BACK:
                        animImg.setBackgroundResource(R.drawable.anim_empty);
                        break;
                    case RobotProtocolConsts.PARAM_LEFT:
                        animImg.setBackgroundResource(R.drawable.anim_left);
                        break;
                    case RobotProtocolConsts.PARAM_RIGHT:
                        animImg.setBackgroundResource(R.drawable.anim_right);
                        break;
                    case RobotProtocolConsts.PARAM_STOP:
                        animation.stop();
                        animImg.setBackgroundResource(R.drawable.anim_empty);
                        break;

                }

                if (param != RobotProtocolConsts.PARAM_STOP) {
                    animation = (AnimationDrawable) animImg.getBackground();
                    animation.start();
                }
                break;

            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_SET_SPEED:
                robot.speed = param;
                break;

            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_SET_AUTO:

                if (param == RobotProtocolConsts.PARAM_ON) {
                    robot.pilot = true;
                    setDisconnectComponent();
                    autoSwitch.setChecked(true);
                    autoSwitch.setEnabled(true);
                }
                    else
                {
                    robot.pilot = false;
                    autoSwitch.setChecked(false);
                }
                break;
        }
    }
}
