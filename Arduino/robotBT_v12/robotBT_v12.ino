#include <Servo.h>
//#include <SoftwareSerial.h>


/* ------------- D E K L A R A C J A  P I N O W  A R D U I N O------------------------------------------- */

const byte LED_Pin    = 2;  // Wł/wy światełek 

//Motor A
const byte Motor_PWM1 = 3;  //Ustawienia prędkości lewego silnikiem
const byte Motor_IN1  = 4;  //Pin sterujący lewym silnikiem
const byte Motor_IN2  = 5;  //Pin sterujący lewym silnikiem

//Motor B
const byte Motor_PWM2 = 6;  //Ustawienia prędkości prawym silnikiem
const byte Motor_IN3  = 7;  //Pin sterujący prawym silnikiem
const byte Motor_IN4  = 8;  //Pin sterujący prawym silnikiem

const byte SG90_Pin   = 9;  //Pin sterujący servo mechanizmem SG90


const byte echo_Pin   = 12; //Pin sterującym czujnikiem odległości SR-04
const byte trig_Pin   = 13; //Pin sterującym czujnikiem odległości SR-04






/* ------------- D E K L A R A C J A  P O L E C E N  I  P A R A M E T R O W------------------------------ */

/*POLECENIA STEROWANIA PROBOTE
 Przed przesłaniem jakiegokolwiek polecująca należy zalogować się do urządzenia podając jego kod
 polecenie CMD_INIT 

 
 PANEL STERUJĄCY MASTER                            ROBOT -SLAVE
 ----------------------------------               ----------------------------
  CMD_INIT    byte[0]=CMD_INIT       -------->    ODPOWIEDŹ
              byte[1]=PARAM_CODE                  CMD_INIT + CMD_RESPONDE 
                                     <--------    byte[0] = CMD_INIT + CMD_RESPONDE
                                                  byte[1] = param speed //Ustawienie prędkości silników

                                                  
//Do przodu
 CMD_SET_MOTOR  byte[0]=CMD_SET_MOTOR -------->    ODPOWIEDŹ
                byte[1]=PARAM_FORWARD              CMD_SET_MOTOR + CMD_RESPONDE 
                                     <--------    byte[0] = CMD_SET_MOTOR + CMD_RESPONDE
                                                  byte[1] = PARAM_FORWARD
                                                  
              
 */

const byte CMD_RESPONDE     = 100; //Do odpowiedzi stosujemy responde + kod CMD

/*
 * Polecenie CMD_INIT sluzy do uzyskania kontroli nad pojazdem poprzez wprowadzenie kodu
 * aktywacyjnego, dzieki temu software master moze sterowac kilkoma robotami jednoczesnie
*/
const byte CMD_INIT         = 10;  //Odblokowanie poleceń - wysyłane przy połaczeniu parameter KOD 101

/*
 * Funkcja kontrolujace prace rota np.:
 * PARAM_OFF - zatrzymuje i wylacza wszystkie komponenty i rozlacza klienta
 * 
 * Klient musi ponownie podlaczyc sie, podajac kod aktywacyjny aby ponownie sterowac robotem
*/
const byte CMD_CTRL        = 11;  //Funkcje kontrolujące Wylaczenie robota PARAM_OFF ()

const byte CMD_SET_MOTOR   = 20;  //Sterowanie silnikami parametr LEFT, RIGHT, FORWARD, BACK, STOP
const byte CMD_SET_AUTO    = 21; //Właczenie trybu automatycznego parametr ON - wł OFF wył
const byte CMD_SET_SPEED   = 22; //Ustawienia obrótów silnika parametr SPEED1, SPEED2, SPEED3, SPEED4, SPEED5


const byte CMD_GET_BATTERY = 23; //Wysyłanie do Mastera odczytu onapięcia na baterii
const byte CMD_SET_LIGHT   = 24; //Sterowanie oświetleniem parametr ON - wł OFF wył 


const byte CMD_SET_EYES       = 25; //Ustawienie kierunków oczów parametr kąt 1-180

const byte CMD_GET_EYES      = 26; //Wysyłanie do Mastera odczytu odległości 0 -255 cm


const byte PARAM_CODE        = 101;   //Kod dostępu uruchamiający procedurę kontrolowania pojazdu
const byte PARAM_ON          = 1;     //Parametr właczenia np. AutoPilot światel
const byte PARAM_OFF         = 0;     //Parametr wyłaczenia np. Autopilot, światel


//CMD_CTRL
const byte PARAM_TEST        = 2;    //Testowanie komponentow SG90, SR04, LED
//PARAM_OFF

/*CMD_SET_MOTOR */
const byte PARAM_FORWARD     = 1;     //Paramter do przedu
const byte PARAM_STOP        = 2;     //Paramter zatrzymaj
const byte PARAM_BACK        = 3;     //Paramter do tyłu
const byte PARAM_LEFT        = 4;     //Paramter w lewo
const byte PARAM_RIGHT       = 5;     //Paramter w prawo

/*Parametry ustawiania prędkości CMD_SET_SPEED*/
const byte PARAM_SPEED1      = 1;  
const byte PARAM_SPEED2      = 2;
const byte PARAM_SPEED3      = 3;
const byte PARAM_SPEED4      = 4;
const byte PARAM_SPEED5      = 5;

/*Parametry napięcia baterji CMD_SET_BATTERY*/
const byte PARAM_BATTERY_LOW = 1;
const byte PARAM_BATTERY_HALF= 2;
const byte PARAM_BATTERY_FULL= 3;

/*Wartości  PWM dla silników*/
const byte MOTOR_SPEED1      = 100;
const byte MOTOR_SPEED2      = 125;
const byte MOTOR_SPEED3      = 150;
const byte MOTOR_SPEED4      = 200;
const byte MOTOR_SPEED5      = 250;


/* ------------- D E K L A R A C J A  F U N K C J I------------------------------------------------------ */


/* FUNKCCJE STERUJĄCE SILNIKAMI */
void motor_set_speed(byte param);
byte motor_get_speed();
void motor_control(byte param);
void motor_control_from_robot(byte param);


/* FUNKCJA STERUJĄCE SERWO MECHANIZMEM SG90 */
void SG90_set(byte param);

/*Funkcja sprawdza napiecie na baterii*/
void get_battery_life();

//Funkcjas wl/wylacz LED 
void led_set(byte param);

/* FUNKCJA CZYTAJĄCA DYSTANS HC-SR04 WARTOŚĆ 0-255 CM GDZIE 255 BARDZO DALEKO */
long distance_read();

/* PRZETWARZANIE KOMEND ODEBRANYCH */
void bluetooth_parse_command(byte cmd, byte param);
void bluetooth_responde(byte buf[], byte len);

/* FUNKCJE ROBOTA */
void robot_cache_direction(byte param);
void robot_control();
void robot_start();
void robot_stop();

void robot_off();   //Uruchomienie procedukry na odebrane polecenie CMD_CTRL PARAM_OFF
void robot_test();  //Uruchomienie procedukry na odebrane polecenie CMD_CTRL PARAM_TEST
void robot_stop_low_battery(); //Wylaczenie wszystkich ukladow wl w celu uszczedzania energii

/* ------------- Z M I E N N E  G L O B A L N E --------------------------------------------------------- */


const int interval_time = 50;
const int interval_time_battery = 15000;

/* Zmiene globaln */
Servo SG90;
byte MotorSpeed;    //Aktualna prędkość pojazdu
byte MotorState;    //Stan silników 
byte BatteryLife;   //Stan baterii
boolean Connected;  //Czy jest połączenie
boolean AutoRobot;  //Czy włączony jest autopilot


unsigned long prevTime  = 0; 
unsigned long prevTime2 = 0; 
//SoftwareSerial BT(10, 11); //rx,tx


/* ------------- S E T U P------------------------------------------------------------------------------- */
void setup() {
  MotorState = PARAM_STOP;
  MotorSpeed = MOTOR_SPEED3;
  BatteryLife= PARAM_BATTERY_FULL;
  Connected  = false;
  AutoRobot  = false;

//  BT.begin(9600);
  Serial.begin(9600);
  
  SG90.attach(SG90_Pin);

  pinMode(A0, INPUT);
  
  pinMode(Motor_PWM1, OUTPUT);
  pinMode(Motor_PWM2, OUTPUT);
 
  pinMode(Motor_IN1, OUTPUT);
  pinMode(Motor_IN2, OUTPUT);
  pinMode(Motor_IN3, OUTPUT);
  pinMode(Motor_IN4, OUTPUT);

  digitalWrite(Motor_IN1, LOW);
  digitalWrite(Motor_IN2, LOW);
  digitalWrite(Motor_IN2, LOW);
  digitalWrite(Motor_IN4, LOW);

  
  analogWrite(Motor_PWM1, MotorSpeed);
  analogWrite(Motor_PWM2, MotorSpeed);

  pinMode(SG90_Pin, OUTPUT);

  pinMode(echo_Pin, INPUT);
  pinMode(trig_Pin, OUTPUT);

  pinMode(LED_Pin, OUTPUT);
  digitalWrite(LED_Pin, LOW);

  /* Procedura diagnostyczna informująca operatora 
   *że urządzenie jest sprawne i gotowe do działania
   *
   *poruszamy czujnik z oczami kąt 180 - 0 - 90
   */
   SG90_set(180);
   delay(500);
   SG90_set(0);
   delay(500);
   SG90_set(90);
}


/* Gdy operator połączy się z urządzeniem
 *  zaświecamy LED na czas 1 sekundy
 */

/* ------------- L O O P--------------------------------------------------------------------------------- */

void loop() {
    
    unsigned long currTime = millis();  
    byte RX[2];
    byte TX[2];
    byte prevState;
    
    byte readBytes;
    
      //Sprawdzam czy są dane w buforze
    if (Serial.available())
    {
        readBytes = Serial.readBytes(RX, 2);
        if (readBytes == 2) 
        {
            bluetooth_parse_command(RX[0], RX[1]);
        }
  
    }

    //Sprawdzy napiecie na baterii co 15 sek.
    if (currTime - prevTime2 > interval_time_battery)
    {
        prevState = BatteryLife;
        get_battery_life();
        if (prevState != BatteryLife)
        {
             if (BatteryLife != PARAM_BATTERY_LOW)
              {
                TX[0] = CMD_RESPONDE + CMD_GET_BATTERY;
                TX[1] = BatteryLife;
                bluetooth_responde(TX, 2);
              }
                else
              robot_stop_low_battery(); 

        }
    }
       

    if (AutoRobot)
    {
        if (currTime - prevTime > interval_time)
            robot_control();
    }

    
}






/* ------------- F U N K C J E  S T E R O W A N I E S I L N I K A M I------------------------------------ */

/*
  Funkcja steruje silnikami
*/
void motor_control(byte param)
{
    switch(param)
    {  
      case PARAM_FORWARD:
          MotorState = param;
          //Motor 1
          digitalWrite(Motor_IN1, HIGH);
          digitalWrite(Motor_IN2, LOW);

          //Motor 2
          digitalWrite(Motor_IN3, HIGH);
          digitalWrite(Motor_IN4, LOW);       
          break;

    case PARAM_STOP:
          MotorState = param;
          //Motor 1
          digitalWrite(Motor_IN1, LOW);
          digitalWrite(Motor_IN2, LOW);

          //Motor 2
          digitalWrite(Motor_IN3, LOW);
          digitalWrite(Motor_IN4, LOW);  
          
          delay(50);     
          break;

     case PARAM_BACK:
          MotorState = param;
          //Motor 1
          digitalWrite(Motor_IN1, LOW);
          digitalWrite(Motor_IN2, HIGH);
        
          //Motor 2
          digitalWrite(Motor_IN3, LOW);
          digitalWrite(Motor_IN4, HIGH);  
          break;      

     case PARAM_LEFT:
          MotorState = param;
          //Motor 1
          digitalWrite(Motor_IN1, HIGH);
          digitalWrite(Motor_IN2, LOW);
        
          //Motor 2
          digitalWrite(Motor_IN3, LOW);
          digitalWrite(Motor_IN4, HIGH);  
          break; 

     case PARAM_RIGHT:
          MotorState = param;
            //Motor 1
          digitalWrite(Motor_IN1, LOW);
          digitalWrite(Motor_IN2, HIGH);
        
          //Motor 2
          digitalWrite(Motor_IN3, HIGH);
          digitalWrite(Motor_IN4, LOW);  
          break;

     default:
          MotorState = PARAM_STOP;
          //Motor 1
          digitalWrite(Motor_IN1, LOW);
          digitalWrite(Motor_IN2, LOW);

          //Motor 2
          digitalWrite(Motor_IN3, LOW);
          digitalWrite(Motor_IN4, LOW);       
          break;     
    }
}


/*
  Funkcja steruje silnikami
*/
void motor_control_from_robot(byte param)
{
    byte TX[2];
    
    switch(param)
    {  
              case PARAM_FORWARD:
                  MotorState = param;
                  //Motor 1
                  digitalWrite(Motor_IN1, HIGH);
                  digitalWrite(Motor_IN2, LOW);
        
                  //Motor 2
                  digitalWrite(Motor_IN3, HIGH);
                  digitalWrite(Motor_IN4, LOW);       
                  break;
        
            case PARAM_STOP:
                  MotorState = param;
                  //Motor 1
                  digitalWrite(Motor_IN1, LOW);
                  digitalWrite(Motor_IN2, LOW);
        
                  //Motor 2
                  digitalWrite(Motor_IN3, LOW);
                  digitalWrite(Motor_IN4, LOW);   
        
                  delay(50);
                  break;
        
             case PARAM_BACK:
                  MotorState = param;
                  //Motor 1
                  digitalWrite(Motor_IN1, LOW);
                  digitalWrite(Motor_IN2, HIGH);
                
                  //Motor 2
                  digitalWrite(Motor_IN3, LOW);
                  digitalWrite(Motor_IN4, HIGH);  
                  break;      
        
             case PARAM_LEFT:
                  MotorState = param;
                  //Motor 1
                  digitalWrite(Motor_IN1, HIGH);
                  digitalWrite(Motor_IN2, LOW);
                
                  //Motor 2
                  digitalWrite(Motor_IN3, LOW);
                  digitalWrite(Motor_IN4, HIGH);  
                  break; 
        
             case PARAM_RIGHT:
                  MotorState = param;
                    //Motor 1
                  digitalWrite(Motor_IN1, LOW);
                  digitalWrite(Motor_IN2, HIGH);
                
                  //Motor 2
                  digitalWrite(Motor_IN3, HIGH);
                  digitalWrite(Motor_IN4, LOW);
                  break;
        
             default:
                  MotorState = PARAM_STOP;
                  //Motor 1
                  digitalWrite(Motor_IN1, LOW);
                  digitalWrite(Motor_IN2, LOW);
        
                  //Motor 2
                  digitalWrite(Motor_IN3, LOW);
                  digitalWrite(Motor_IN4, LOW);   
                  delay(20);    
                  break;     
     }
     
     TX[0] = CMD_RESPONDE + CMD_SET_MOTOR;
     TX[1] = MotorState;
     bluetooth_responde(TX,2);
}

/*
  Funkcja steruje prędkością obrotową silników
*/
void motor_set_speed(byte param)
{
    //PARAM_SPEED
    switch(param)
    {
          case PARAM_SPEED1:
                  MotorSpeed = MOTOR_SPEED1;
                  break;
          case PARAM_SPEED2:
                  MotorSpeed = MOTOR_SPEED2;
                  break;                  
          case PARAM_SPEED3:
                  MotorSpeed = MOTOR_SPEED3;
                  break;                  
          case PARAM_SPEED4:
                  MotorSpeed = MOTOR_SPEED4;
                  break;                  
          case PARAM_SPEED5:
                  MotorSpeed = MOTOR_SPEED5;
                  break;                  
          default:
                  MotorSpeed = MOTOR_SPEED5;
    }
    
    analogWrite(Motor_PWM1, MotorSpeed);
    analogWrite(Motor_PWM2, MotorSpeed);
}

/*
  Funkcja zwraca wprędkośc w numerze biegów
*/
byte motor_get_speed()
{
    switch(MotorSpeed)
    {
          case MOTOR_SPEED1:
                  return PARAM_SPEED1;
                  break;
          case MOTOR_SPEED2:
                  return PARAM_SPEED2;
                  break;                  
          case MOTOR_SPEED3:
                  return PARAM_SPEED3;
                  break;                  
          case MOTOR_SPEED4:
                  return PARAM_SPEED4;
                  break;                  
          case MOTOR_SPEED5:
                  return PARAM_SPEED5;
                  break;                  
          default:
                  return 0;
    }  
}

/*
  Funkcja przetwarza odebrane dane z odbiornika Bluetooth
*/

void bluetooth_responde(byte buf[], byte len)
{
  
  int sendBytes = Serial.write(buf, len);  
  Serial.flush();
  if (sendBytes < len)
  {
      if (MotorState != PARAM_STOP)
        motor_control(PARAM_STOP);
      if (AutoRobot)
        AutoRobot = false;

      Connected = false;
  }
}

void bluetooth_parse_command(byte cmd, byte param)
{
      byte TX[2];
        
      if ((!Connected) && (cmd != CMD_INIT))
          return;
    
      TX[0] = CMD_RESPONDE+cmd;
      
      switch(cmd)
      {
          case CMD_INIT: 
            
              if (param == PARAM_CODE) 
              {
                  led_set(PARAM_ON);
                  TX[1] = motor_get_speed();
                  Connected = true;
                  bluetooth_responde(TX, 2);
                  delay(1000);
                  led_set(PARAM_OFF);
              }
              break;

         case CMD_CTRL:
              if (param == PARAM_OFF)
                 robot_off();
              else
                if (param == PARAM_TEST && AutoRobot == false)
                  robot_test();

              TX[1] = PARAM_TEST;
              bluetooth_responde(TX, 2);                  
              break;
              
         case CMD_SET_MOTOR:
              if (!AutoRobot)
              {
                    if (BatteryLife == PARAM_BATTERY_LOW)
                    {
                         TX[0] = CMD_RESPONDE + CMD_GET_BATTERY;
                         TX[1] = BatteryLife;
                         bluetooth_responde(TX, 2);
                         if (MotorState != PARAM_STOP)
                            motor_control(PARAM_STOP);
                    }
                      else
                    {
                        TX[1] = param;
                        bluetooth_responde(TX, 2);
                        motor_control(param);
                    }
              }
              break;


        case CMD_GET_BATTERY:
              TX[1] = BatteryLife;
              bluetooth_responde(TX, 2);
              break;
             
        case CMD_SET_AUTO: //CMD_SET_AUTO
              if (param == PARAM_ON && BatteryLife != PARAM_BATTERY_LOW)
              {
                 if (AutoRobot)
                    AutoRobot = false;
                 else
                    AutoRobot = true;

              }
              else
              {
                 AutoRobot = false;
                 robot_stop();
              }
             
             TX[1] = AutoRobot;
             bluetooth_responde(TX, 2);     

              if  (AutoRobot)
                  robot_start();
             break;
               

        case CMD_SET_SPEED: 
              if (!AutoRobot)
                  motor_set_speed(param);
             
             TX[1] = motor_get_speed();
             bluetooth_responde(TX, 2);                  
             break;

         case CMD_SET_LIGHT:
            if (param == PARAM_ON && BatteryLife != PARAM_BATTERY_LOW)
            {
                led_set(param);
                TX[1] = param;
            }
              else
            TX[1] = PARAM_OFF;
            bluetooth_responde(TX, 2);  
            break;

          case CMD_SET_EYES:
              byte pos;
              if (BatteryLife != PARAM_BATTERY_LOW)
                 SG90_set(param);
               
              TX[1] = digitalRead(SG90_Pin);
              bluetooth_responde(TX, 2);  
              break; 

           case CMD_GET_EYES:
              int dis = distance_read();
              if (dis > 250)
                TX[1] = 251;
              else
               if (dis < 1)
                  TX[1] = 0;
               else
                 TX[1] = (byte)dis;
              bluetooth_responde(TX, 2);               
              break;
         
            
      }
}

void SG90_set(byte param)
{
    if ((param >= 0) && (param <= 180))
    {
        SG90.write(param);
        delay(50);
    }
}

void get_battery_life()
{
    int sensorV = analogRead(A0);
    if (sensorV == 0)
      BatteryLife = PARAM_BATTERY_FULL;
    else
    {
        float BatVolt = (sensorV * (13.6 / 1023.0))+0.3;
        if (BatVolt >= 12.5) 
          BatteryLife = PARAM_BATTERY_FULL;
        else
          if (BatVolt > 10.0 && BatVolt < 12.5)
            BatteryLife = PARAM_BATTERY_HALF;
          else
            BatteryLife = PARAM_BATTERY_LOW;
              
    }
    
   prevTime2 = millis();
}


long  distance_read()
{
  long time_t, distance_t;
  digitalWrite(trig_Pin, LOW);
  delayMicroseconds(2);
  digitalWrite(trig_Pin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trig_Pin, LOW);

  time_t = pulseIn(echo_Pin, HIGH);
  
  distance_t = time_t / 58;

  return distance_t;
}

void led_set(byte param)
{
  if (param == PARAM_ON)
    digitalWrite(LED_Pin, HIGH);
  else
    digitalWrite(LED_Pin, LOW);
}

void robot_cache_direction(byte param)
{
    if ((param == PARAM_LEFT) || (param == PARAM_RIGHT))
    {
        motor_control_from_robot(param);
        delay(1000);
        motor_control_from_robot(PARAM_STOP);
    }
}


/*Start robota w trybie auto */
void robot_start()
{
   //Zatrzymuje robota
   if (MotorState != PARAM_STOP)
       motor_control_from_robot(PARAM_STOP);

   if (motor_get_speed() != PARAM_SPEED3)
      motor_set_speed(PARAM_SPEED3); 
   
   if (digitalRead(SG90_Pin) != 90)
      SG90_set(90); 
            
    prevTime = millis();

   //Sprawdzam dysense do przeszkody, który jest największy
   long left_dis, right_dis, top_dis; 
   SG90_set(180);
   left_dis = distance_read();
   SG90_set(1);
   right_dis = distance_read();
   SG90_set(90);  
   top_dis = distance_read();

   //Jeśli któraś z 3 wartości jest wieksza od 30
   if ((top_dis > 30) || (left_dis > 30) || (right_dis > 30))
   {
        if ((top_dis >= left_dis) && (top_dis >= right_dis))
            motor_control_from_robot(PARAM_FORWARD);
        else
         if ((top_dis < left_dis) && (left_dis > right_dis))
            robot_cache_direction(PARAM_LEFT);
         else
            if ((top_dis < right_dis) && (left_dis < right_dis))
              robot_cache_direction(PARAM_RIGHT);
   }
    else
    //Cofam do tyłu
    motor_control_from_robot(PARAM_BACK);
}

void robot_stop()
{
   if (MotorState != PARAM_STOP)
       motor_control_from_robot(PARAM_STOP);
    
    if (digitalRead(SG90_Pin) != 90)
      SG90_set(90);       
}

void robot_control()
{
    if (MotorState == PARAM_BACK)
      motor_control_from_robot(PARAM_STOP);

    if (BatteryLife == PARAM_BATTERY_LOW)
    {
        AutoRobot = false;
        byte TX[2];

        TX[0] = CMD_RESPONDE + CMD_SET_AUTO;
        TX[1] = PARAM_OFF;
        bluetooth_responde(TX, 2);
        delay(50);
                
        TX[0] = CMD_RESPONDE + CMD_GET_BATTERY;
        TX[1] = BatteryLife;
        bluetooth_responde(TX, 2);
        
        return;
    }
    
    //Sprawdzam  czy jest przeszkoda 30 cm od pojazdu wartość 255 - więcej niż 2 metry
    long top_dis = distance_read();
    
    if (top_dis < 30)
    {
        
        long left_dis, right_dis;
        
        //Jest przeszkoda zatrzymuje pojazd
        if (MotorState != PARAM_STOP)
            motor_control_from_robot(PARAM_STOP);

        //Przesuwam czujnik w lewą stronę  sprawdzam odlełośc do najbliższej przeszkody
        SG90_set(180);
        left_dis = distance_read();
        
        //Przesuwam czujnik w prawą stronę  sprawdzam odlełośc do najbliższej przeszkody
        SG90_set(1);
        right_dis = distance_read();

        //Przesuwam czujnik na środek
        SG90_set(90);

        /*Jeżeli którakolwiek z wartości jest większa niż 30 cm
        */
        if ((left_dis > 30) || (right_dis > 30))
        {
            /*Wartość lewa jest większa zmieniam kierunek pojazdu
             * jeśli nie to w prawo
             */
            
            if (left_dis > right_dis)
              robot_cache_direction(PARAM_LEFT);
            else
              robot_cache_direction(PARAM_RIGHT);
        }
          else
         /* żadna z wartość nie  jest odpowiednia cofam pojazd*/
          motor_control_from_robot(PARAM_BACK);
    }
      else
    if (MotorState == PARAM_STOP)
      motor_control_from_robot(PARAM_FORWARD);

    prevTime = millis();  
}


void robot_off()
{
    AutoRobot = false;
    Connected = false;
    motor_control(PARAM_STOP);
    led_set(PARAM_OFF);
}

void robot_test()
{
    byte i;
    motor_control(PARAM_STOP);

    led_set(PARAM_ON);
    delay(500);
    led_set(PARAM_OFF);
 
    SG90_set(1);
    delay(500);
    SG90_set(180);
    delay(500);
    SG90_set(90);
    delay(500);
}

void robot_stop_low_battery()
{
    byte TX[2];
    
    if (Connected)
    {
        TX[0] = CMD_RESPONDE + CMD_GET_BATTERY;
        TX[1] = BatteryLife;
        bluetooth_responde(TX, 2);
    }
    
    led_set(PARAM_OFF);
    motor_control(PARAM_STOP);
    
    if ((Connected) && (digitalRead(LED_Pin) == HIGH))
    {
        TX[0] = CMD_RESPONDE + CMD_SET_LIGHT;
        TX[1] = PARAM_OFF;
        bluetooth_responde(TX, 2);
        delay(50);
    }

    
    if ((Connected) && (AutoRobot))
    {
        AutoRobot = false;
        TX[0] = CMD_RESPONDE + CMD_SET_AUTO;
        TX[1] = PARAM_OFF;
        bluetooth_responde(TX, 2);
    }
}

