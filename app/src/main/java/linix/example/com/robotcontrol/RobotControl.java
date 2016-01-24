package linix.example.com.robotcontrol;

interface RobotProtocolConsts{
    public static final byte CMD_RESPONDE = 100;


    public  static final byte CMD_INIT   =   10;
    public byte CMD_SET_MOTOR=   20;
    public byte CMD_SET_AUTO =   21;
    public byte CMD_SET_SPEED=   22;
    public byte CMD_GET_BATTERY = 23;
    public byte CMD_SET_LIGHT=   24;
    public byte CMD_SET_EYES =   25;
    public byte CMD_GET_EYES =   26;



    public byte PARAM_CODE    = 101;
    public byte PARAM_ON      = 1;
    public byte PARAM_OFF     = 0;

    public byte PARAM_FORWARD = 1;
    public byte PARAM_STOP    = 2;
    public byte PARAM_BACK    = 3;
    public byte PARAM_LEFT    = 4;
    public byte PARAM_RIGHT   = 5;

    public byte PARAM_SPEED1  = 1;
    public byte PARAM_SPEED2  = 2;
    public byte PARAM_SPEED3  = 3;
    public byte PARAM_SPEED4  = 4;
    public byte PARAM_SPEED5  = 5;

    public byte PARAM_BATTERY_LOW   = 1;
    public byte PARAM_BATTERY_HALF  = 2;
    public byte PARAM_BATTERY_FULL  = 3;
}


public class RobotControl {

    private BluetoothClient BTClient;
    public int speed = 5;
    public boolean pilot = false;

    public RobotControl(BluetoothClient client)
    {
        this.BTClient = client;
    }

    public void autoPilot(boolean value)
    {
        byte[] TX;

        if (this.BTClient.getState() != BluetoothClient.STATE_CONNECTED)
            return;

        TX = new byte[2];
        TX[0] = RobotProtocolConsts.CMD_SET_AUTO;
        if (value)
            TX[1] = RobotProtocolConsts.PARAM_ON;
        else
            TX[1] = RobotProtocolConsts.PARAM_OFF;

        this.BTClient.write(TX);
    }

    public void setSpeed(int number)
    {
        byte[] TX;

        if (this.BTClient.getState() != BluetoothClient.STATE_CONNECTED)
            return;
        if ((number > 0) && (number < 6))
        {
            TX = new byte[2];
            TX[0] = RobotProtocolConsts.CMD_SET_SPEED;
            TX[1] = (byte) number;
            this.BTClient.write(TX);
        }
    }

    public int getSpeed()
    {
        return this.speed;
    }

    public void Init()
    {
        byte[] TX;

        if (this.BTClient.getState() != BluetoothClient.STATE_CONNECTED)
            return;

        TX = new byte[2];
        TX[0] = RobotProtocolConsts.CMD_INIT;
        TX[1] = RobotProtocolConsts.PARAM_CODE;
        this.BTClient.write(TX);
    }

    public void goForward()
    {
        byte[] TX;

        if (this.BTClient.getState() != BluetoothClient.STATE_CONNECTED)
            return;

        TX = new byte[2];
        TX[0] = RobotProtocolConsts.CMD_SET_MOTOR;
        TX[1] = RobotProtocolConsts.PARAM_FORWARD;
        this.BTClient.write(TX);
    }

    public void goBack()
    {
        byte[] TX;

        TX = new byte[2];
        TX[0] = RobotProtocolConsts.CMD_SET_MOTOR;
        TX[1] = RobotProtocolConsts.PARAM_BACK;
        this.BTClient.write(TX);
    }

    public void goStop()
    {
        byte[] TX;

        TX = new byte[2];
        TX[0] = RobotProtocolConsts.CMD_SET_MOTOR;
        TX[1] = RobotProtocolConsts.PARAM_STOP;
        this.BTClient.write(TX);
    }

    public void goLeft()
    {
        byte[] TX;

        TX = new byte[2];
        TX[0] = RobotProtocolConsts.CMD_SET_MOTOR;
        TX[1] = RobotProtocolConsts.PARAM_LEFT;
        this.BTClient.write(TX);
    }

    public void goRight()
    {
        byte[] TX;

        TX = new byte[2];
        TX[0] = RobotProtocolConsts.CMD_SET_MOTOR;
        TX[1] = RobotProtocolConsts.PARAM_RIGHT;
        this.BTClient.write(TX);
    }


}
