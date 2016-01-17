package linix.example.com.robotcontrol;


public class RobotControl {

    protected byte CMD_RESPONDE = 100;
    protected byte CMD_INIT     =   10;
    protected byte CMD_SET_MOTOR=   20;
    protected byte CMD_SET_LIGHT=   21;
    protected byte CMD_SET_AUTO =   22;
    protected byte CMD_SET_EYES =   23;
    protected byte CMD_GET_EYES =   24;
    protected byte CMD_SET_SPEED=   25;

    protected byte PARAM_CODE    = 101;
    protected byte PARAM_ON      = 1;
    protected byte PARAM_OFF     = 0;

    protected byte PARAM_FORWARD = 1;
    protected byte PARAM_STOP    = 2;
    protected byte PARAM_BACK    = 3;
    protected byte PARAM_LEFT    = 4;
    protected byte PARAM_RIGHT   = 5;

    protected byte PARAM_SPEED1  = 1;
    protected byte PARAM_SPEED2  = 2;
    protected byte PARAM_SPEED3  = 3;
    protected byte PARAM_SPEED4  = 4;
    protected byte PARAM_SPEED5  = 5;

    private BluetoothClient BTClient;
    private int speed = 5;

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
        TX[0] = CMD_SET_AUTO;
        if (value)
            TX[1] = PARAM_ON;
        else
            TX[1] = PARAM_OFF;

        this.BTClient.write(TX);
    }

    public void setSpeed(int number)
    {
        byte[] TX;

        if (this.BTClient.getState() != BluetoothClient.STATE_CONNECTED)
            return;
        if ((number > 0) && (number < 6))
        {
            this.speed = (byte)number;

            TX = new byte[2];
            TX[0] = CMD_SET_SPEED;
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
        TX[0] = CMD_INIT;
        TX[1] = PARAM_CODE;
        this.BTClient.write(TX);
    }

    public void goForward()
    {
        byte[] TX;

        if (this.BTClient.getState() != BluetoothClient.STATE_CONNECTED)
            return;

        TX = new byte[2];
        TX[0] = CMD_SET_MOTOR;
        TX[1] = PARAM_FORWARD;
        this.BTClient.write(TX);
    }

    public void goBack()
    {
        byte[] TX;

        if (this.BTClient.getState() != BluetoothClient.STATE_CONNECTED)
            return;

        TX = new byte[2];
        TX[0] = CMD_SET_MOTOR;
        TX[1] = PARAM_BACK;
        this.BTClient.write(TX);
    }

    public void goStop()
    {
        byte[] TX;

        if (this.BTClient.getState() != BluetoothClient.STATE_CONNECTED)
            return;

        TX = new byte[2];
        TX[0] = CMD_SET_MOTOR;
        TX[1] = PARAM_STOP;
        this.BTClient.write(TX);
    }

    public void goLeft()
    {
        byte[] TX;

        if (this.BTClient.getState() != BluetoothClient.STATE_CONNECTED)
            return;

        TX = new byte[2];
        TX[0] = CMD_SET_MOTOR;
        TX[1] = PARAM_LEFT;
        this.BTClient.write(TX);
    }

    public void goRight()
    {
        byte[] TX;

        if (this.BTClient.getState() != BluetoothClient.STATE_CONNECTED)
            return;

        TX = new byte[2];
        TX[0] = CMD_SET_MOTOR;
        TX[1] = PARAM_RIGHT;
        this.BTClient.write(TX);
    }


}
