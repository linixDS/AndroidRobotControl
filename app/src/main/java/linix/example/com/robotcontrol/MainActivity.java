package linix.example.com.robotcontrol;

import android.app.Activity;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 3;

    //Warstawa 1
    ImageView leftImg;
    ImageView rightImg;
    ImageView forwardImg;
    ImageView backhtImg;
    ImageView cameraImg;
    ImageView speedImg;
    ImageView connectImg;
    ImageView animImg;
    ImageView batteryImg;

    TextView msgView;

    ViewSwitcher switcher;
    AnimationDrawable animation;
    Animation animBattery;

    boolean connected = false;

    RadioGroup biegi;
    RadioButton speed1, speed2, speed3, speed4, speed5;

    ToggleButton cameraSwitch;
    Button driverButton;
    ToggleButton autoSwitch;
    ToggleButton ledSwitch;


    //Warstwa 2
    ImageView   radarImg;
    SeekBar     posBar;
    Button      setSG90Btn, getSR04Btn, prevBtn, diagBtn;
    TextView    distanceText, valueText;

    BluetoothClient BTClient;
    RobotControl    robot;
    ProgressDialog connectionDialog;
    WifiManager wm;
    CountDownTimer timer;

    String deviceName;
    String addressIP;
    byte deviceAddress;

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

                            robot.Connect(deviceAddress);
                            timer = new CountDownTimer(5000, 2000) {
                                @Override
                                public void onTick(long l) {
                                }

                                @Override
                                public void onFinish() {
                                    if (!connected)
                                    {
                                        BTClient.disconnect();
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
                    byte readBytes = (byte) msg.arg1;

                    byte cmd    = readBuf[0];
                    byte param  = readBuf[1];

                    onReceiveData(cmd, param);
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
        wm.setWifiEnabled(true);

        switcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

        biegi     = (RadioGroup) findViewById(R.id.radioGroup);
        biegi.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
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
        batteryImg= (ImageView) findViewById(R.id.batteryView);

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
                if (BTClient.getState() == BluetoothClient.STATE_CONNECTED)
                    stopRobot();
                else
                    startRobot();
            }
        });


        autoSwitch = (ToggleButton) findViewById(R.id.autoSwitch);
        autoSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (autoSwitch.isChecked())
                    robot.autoPilot(true);
                else
                    robot.autoPilot(false);
            }
        });


        ledSwitch = (ToggleButton) findViewById(R.id.ledSwitch);
        ledSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ledSwitch.isChecked())
                    robot.onLED();
                else
                    robot.offLED();
            }
        });


        cameraSwitch = (ToggleButton) findViewById(R.id.cameraSwitch);
        cameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (wm.isWifiEnabled()) {

                    } else
                        cameraSwitch.setChecked(false);
                } else {
                    cameraImg.setImageResource(R.drawable.nocapture);
                }

            }
        });

        driverButton = (Button) findViewById(R.id.driverBtn);
        driverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switcher.showNext();
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



        //------------------------------------------------------------------------------------------------------------

        radarImg = (ImageView) findViewById(R.id.radarImg);
        radarImg.setRotation(90);

        posBar   = (SeekBar) findViewById(R.id.posBar);
        posBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                radarImg.setRotation(i);
                valueText.setText(String.format("%d st.", i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        distanceText = (TextView) findViewById(R.id.distanceText);
        valueText    = (TextView) findViewById(R.id.valueText);

        setSG90Btn   = (Button) findViewById(R.id.setSG90Btn);
        setSG90Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                robot.setSG90((byte) posBar.getProgress());
            }
        });

        getSR04Btn   = (Button) findViewById(R.id.getSR04Btn);
        getSR04Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                robot.getDistance();
            }
        });

        prevBtn      = (Button) findViewById(R.id.prevButton);
        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switcher.showPrevious();
            }
        });
        diagBtn      = (Button) findViewById(R.id.diagnosticBtn);
        diagBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                robot.runDiagnostic();
            }
        });


        //------------------------------------------------------------------------------------------------------------
        setDisconnectComponent();
        loadConfig();


        BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (BTAdapter == null) {
            Toast.makeText(getApplicationContext(), "Brak adaptera bluetooth !", Toast.LENGTH_LONG).show();
           finish();
        }


        if (!BTAdapter.isEnabled()) {
            connectImg.setVisibility(View.INVISIBLE);
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        } else
            connectImg.setVisibility(View.VISIBLE);

        BTClient = new BluetoothClient(getApplicationContext(), handler, BTAdapter);
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:

                if (resultCode == Activity.RESULT_OK) {
                    connectImg.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getApplicationContext(), "Błąd: Bluetooth nie został włączony",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }


    private void loadConfig()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        deviceName = prefs.getString("deviceName","Brak");
        addressIP  = prefs.getString("addressCamera","192.168.1.1");
        String code = prefs.getString("deviceAddress", "101");
        int codeInt = Integer.valueOf(code);
        if (codeInt >= 0 && codeInt <= 255)
            deviceAddress = (byte)codeInt;
        else
            deviceAddress = 101;
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
        ledSwitch.setEnabled(false);

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

        diagBtn.setEnabled(false);
        posBar.setEnabled(false);
        getSR04Btn.setEnabled(false);
        setSG90Btn.setEnabled(false);
        driverButton.setEnabled(false);
    }
    private void setDisconnectComponent_autoPilot()
    {
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

        diagBtn.setEnabled(false);
        posBar.setEnabled(false);
        getSR04Btn.setEnabled(false);
        setSG90Btn.setEnabled(false);
        driverButton.setEnabled(false);
    }

    private void setConnectedComponent_autoPilot()
    {
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

        autoSwitch.setEnabled(true);
        ledSwitch.setEnabled(true);

        cameraSwitch.setChecked(false);
        cameraSwitch.setEnabled(true);

        speedImg.setImageResource(R.drawable.speed0);
        connectImg.setImageResource(R.drawable.stop);
        cameraImg.setImageResource(R.drawable.nocapture);

        diagBtn.setEnabled(true);
        posBar.setEnabled(true);
        getSR04Btn.setEnabled(true);
        setSG90Btn.setEnabled(true);
    }

    private void setConnectedComponent()
    {
        connected = true;
        msgView.setText(String.format("Połączenie z %s", deviceName));

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
        ledSwitch.setEnabled(true);
        ledSwitch.setChecked(false);

        cameraSwitch.setChecked(false);
        cameraSwitch.setEnabled(true);

        speedImg.setImageResource(R.drawable.speed0);
        connectImg.setImageResource(R.drawable.stop);
        cameraImg.setImageResource(R.drawable.nocapture);

        diagBtn.setEnabled(true);
        posBar.setEnabled(true);
        getSR04Btn.setEnabled(true);
        setSG90Btn.setEnabled(true);
        driverButton.setEnabled(true);
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

    private void setBatteryState(byte state)
    {
        switch (state){
            case RobotProtocolConsts.PARAM_BATTERY_LOW:
                batteryImg.setImageResource(R.drawable.battery_low);
                animBattery = new AlphaAnimation(1, 0);
                animBattery.setDuration(500);
                animBattery.setInterpolator(new LinearInterpolator());
                animBattery.setRepeatCount(Animation.INFINITE);
                animBattery.setRepeatMode(Animation.REVERSE);
                batteryImg.startAnimation(animBattery);

                Toast.makeText(getApplicationContext(), "Niski poziom baterii", Toast.LENGTH_LONG).show();
                break;

            case RobotProtocolConsts.PARAM_BATTERY_HALF:
                batteryImg.clearAnimation();
                batteryImg.setImageResource(R.drawable.battery_half);
                break;

            case RobotProtocolConsts.PARAM_BATTERY_FULL:
                batteryImg.clearAnimation();
                batteryImg.setImageResource(R.drawable.battery_full);
                break;
        }
    }

    private void setSpeedImage(boolean stopped)
    {
        if (stopped) {
             speedImg.setImageResource(R.drawable.speed0);
        }
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
            connectionDialog = new ProgressDialog(MainActivity.this);
            connectionDialog.setMessage("Łączenie ...");
            connectionDialog.setIndeterminate(false);
            connectionDialog.setCancelable(false);
            connectionDialog.show();

            BTClient.connect();
        }
            else
            msgView.setText("Brak połączenia");
    }

    private void stopRobot()
    {
        if (BTClient.getState() == BluetoothClient.STATE_CONNECTED)
        {
            robot.Disconnect();
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
                robot.speed = param;
                break;


            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_SET_MOTOR:
                switch (param){
                    case RobotProtocolConsts.PARAM_FORWARD:
                        animImg.setBackgroundResource(R.drawable.anim_up);
                        break;
                    case RobotProtocolConsts.PARAM_BACK:
                        animImg.setBackgroundResource(R.drawable.anim_back);
                        break;
                    case RobotProtocolConsts.PARAM_LEFT:
                        animImg.setBackgroundResource(R.drawable.anim_left);
                        break;
                    case RobotProtocolConsts.PARAM_RIGHT:
                        animImg.setBackgroundResource(R.drawable.anim_right);
                        break;
                    case RobotProtocolConsts.PARAM_STOP:
                        animImg.setBackgroundResource(R.drawable.anim_empty);
                        break;

                }


                AnimationDrawable animation = (AnimationDrawable) animImg.getBackground();
                animation.start();
                 break;

            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_SET_SPEED:
                robot.speed = param;
                break;

            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_SET_AUTO:
                if (param == RobotProtocolConsts.PARAM_ON) {
                    setDisconnectComponent_autoPilot();
                    autoSwitch.setChecked(true);
                } else
                {
                    setConnectedComponent_autoPilot();
                    autoSwitch.setChecked(false);
                }
                break;

            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_GET_BATTERY:
                setBatteryState(param);
                break;

            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_SET_LIGHT:
                if (param == RobotProtocolConsts.PARAM_OFF)
                    ledSwitch.setChecked(false);
                else
                    ledSwitch.setChecked(true);
                break;

            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_CTRL:
                if (param == RobotProtocolConsts.PARAM_TEST)
                    Toast.makeText(getApplicationContext(), "Uruchomiono diagnostykę układu", Toast.LENGTH_LONG).show();
                break;

            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_SET_EYES:
                Toast.makeText(getApplicationContext(), String.format("Ustanowiono nową pozycję dla SG90 - kąt: %d",param+180), Toast.LENGTH_LONG).show();
                break;

            case RobotProtocolConsts.CMD_RESPONDE + RobotProtocolConsts.CMD_GET_EYES:
                if (param == 255)
                    distanceText.setText("> 250 cm");
                else
                    distanceText.setText(String.format("%d cm",param));
                Toast.makeText(getApplicationContext(), "Odebrano dane z czujnika odległości", Toast.LENGTH_LONG).show();
                break;

        }
    }
}
