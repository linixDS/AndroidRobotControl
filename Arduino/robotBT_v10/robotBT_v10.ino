#include <Servo.h>

/*USTAWIENIA MIKROPROCESORA*/

#define Motor_PWM1     3  //Ustawienia prędkości lewego silnikiem

#define Motor_IN1      4  //Pin sterujący lewym silnikiem
#define Motor_IN2      5  //Pin sterujący lewym silnikiem

#define Motor_PWM2     6  //Ustawienia prędkości prawym silnikiem
#define Motor_IN3      7  //Pin sterujący prawym silnikiem
#define Motor_IN4      8  //Pin sterujący prawym silnikiem

#define SG90_Pin       9  //Pin sterujący servo mechanizmem SG90

#define echo_Pin       10 //Pin sterującym czujnikiem odległości SR-04
#define trig_Pin       11 //Pin sterującym czujnikiem odległości SR-04


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


#define CMD_RESPONDE    100 //Do odpowiedzi stosujemy responde + kod CMD


#define CMD_INIT        10  //Odblokowanie poleceń - wysyłane przy połaczeniu parameter KOD 101
#define CMD_SET_MOTOR   20  //Sterowanie silnikami parametr LEFT, RIGHT, FORWARD, BACK, STOP
#define CMD_SET_LIGHT   21 //Sterowanie oświetleniem parametr ON - wł OFF wył 
#define CMD_SET_AUTO    22 //Właczenie trybu automatycznego parametr ON - wł OFF wył

//Rezerwacja w wersji nr 2
#define CMD_SET_EYES    23 //Ustawienie kierunków oczów parametr kąt 1-180

//Rezerwacja w wersji nr 2
#define CMD_GET_EYES    24 //Wysyłanie do Mastera odczytu odległości 0 -255 cm

//Dostępna w wersji nr 1
#define CMD_SET_SPEED   25 //Ustawienia obrótów silnika parametr SPEED1, SPEED2, SPEED3, SPEED4, SPEED5

//Rezerwacja w wersji nr 2
#define CMD_GET_BATTERY    26 //Wysyłanie do Mastera odczytu onapięcia na baterii

#define PARAM_CODE    101   //Kod dostępu uruchamiający procedurę kontrolowania pojazdu
#define PARAM_ON      1     //Parametr właczenia np. AutoPilot świateł
#define PARAM_OFF     0     //Parametr wyłaczenia

/*CMD_SET_MOTOR */
#define PARAM_FORWARD 1     //Paramter do przedu
#define PARAM_STOP    2     //Paramter zatrzymaj
#define PARAM_BACK    3     //Paramter do tyłu
#define PARAM_LEFT    4     //Paramter w lewo
#define PARAM_RIGHT   5     //Paramter w prawo

/*Parametry ustawiania prędkości CMD_SET_SPEED*/
#define PARAM_SPEED1  1   
#define PARAM_SPEED2  2
#define PARAM_SPEED3  3
#define PARAM_SPEED4  4
#define PARAM_SPEED5  5

/*Parametry napięcia baterji CMD_SET_BATTERY*/
#define PARAM_BATTERY_LOW  1
#define PARAM_BATTERY_HALF 2
#define PARAM_BATTERY_FULL 3

/*Wartości  PWM dla silników*/
#define MOTOR_SPEED1  50
#define MOTOR_SPEED2  100
#define MOTOR_SPEED3  150
#define MOTOR_SPEED4  200
#define MOTOR_SPEED5  255


#define interval_time 250 

/* Zmiene globaln */
Servo SG90;
byte MotorSpeed;    //Aktualna prędkość pojazdu
byte MotorState;    //Stan silników 
boolean Connected;  //Czy jest połączenie
boolean AutoRobot;  //Czy włączony jest autopilot

unsigned long prevTime = 0; 


/* FUNKCCJE STERUJĄCE SILNIKAMI */
void motor_set_speed(byte param);
byte motor_get_speed();
void motor_control(byte param);

void motor_control_from_robot(byte param);

/* FUNKCJA STERUJĄCE SERWO MECHANIZMEM SG90 */
boolean SG90_set(byte param);


/* FUNKCJA CZYTAJĄCA DYSTANS HC-SR04 WARTOŚĆ 0-255 CM GDZIE 255 BARDZO DALEKO */
byte distance_read();

/* PRZETWARZANIE KOMEND ODEBRANYCH */
void bluetooth_parse_command(byte cmd, byte param);
void bluetooth_responde(byte buf[], byte len);

/* FUNKCJE ROBOTA */
void robot_cache_direction(byte param);
void robot_control();
void robot_start();
void robot_stop();




void setup() {
  MotorState = PARAM_STOP;
  MotorSpeed = MOTOR_SPEED5;
  Connected  = false;
  AutoRobot  = false;

  Serial.begin(9600);
  
  //Ustawiamy 2 sek timeout na oczekiwanie danych i wysyłanie
  Serial.setTimeout(2000);
  SG90.attach(SG90_Pin);
  
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
}

void loop() {
  
    byte RX[1];
    byte readBytes;
    
    //Sprawdzam czy są dane w buforze
    if (Serial.available())
    {
        readBytes = Serial.readBytes(RX, 2);
        if (readBytes == 2) 
            bluetooth_parse_command(RX[0], RX[1]);
         else
            Serial.flush();
    }

    if (AutoRobot)
    {
        unsigned long currTime = millis();
        if (currTime - prevTime > interval_time)
            robot_control();
    }
  
}

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

     byte TX[2];
     TX[0] = CMD_SET_MOTOR;
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
      byte TX[3];
      if ((!Connected) && (cmd != CMD_INIT))
          return;
          
      TX[0] = CMD_RESPONDE+cmd;
      switch(cmd)
      {
          case CMD_INIT: 
              if (param == PARAM_CODE) 
              {
                  TX[1] = motor_get_speed();
                  TX[2] = PARAM_BATTERY_FULL; //Wersji nr 2 będzie odczyt i sprawdzanie baterii
                  Connected = true;
                  bluetooth_responde(TX, 3);
              }
              break;
              
         case CMD_SET_MOTOR:
              if (!AutoRobot)
              {
                motor_control(param);
                TX[1] = MotorState;
                bluetooth_responde(TX, 2);
              }
              break;
                 
             
        case CMD_SET_AUTO: //CMD_SET_AUTO
              if (param == PARAM_ON)
              {
                 AutoRobot = true;
                 robot_start();
              }
              else
              {
                 AutoRobot = false;
                 robot_stop();
              }
             
             TX[1] = AutoRobot;
             bluetooth_responde(TX, 2);                  
             break;
               

        case CMD_SET_SPEED: 
              if (!AutoRobot)
                  motor_set_speed(param);
             
             TX[1] = motor_get_speed();
             bluetooth_responde(TX, 2);                  
             break;
            
      }
}

boolean SG90_set(byte param)
{
    if ((param > 0) && (param < 181))
    {
        SG90.write(param);
        return true;
    }
      else
    return false;
}


byte distance_read()
{
  long time_t, distance_t;
  digitalWrite(trig_Pin, LOW);
  delayMicroseconds(2);
  digitalWrite(trig_Pin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trig_Pin, LOW);

  time_t = pulseIn(echo_Pin, HIGH);
  
  distance_t = time_t / 58;

  if (distance_t > 255)
      return 255;
  else
    return distance_t;
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
    
   if (digitalRead(SG90_Pin) != 90)
      SG90.write(90); 
            
    prevTime = millis();

   //Sprawdzam dysense do przeszkody, który jest największy
   byte left_dis, right_dis, top_dis; 
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
      SG90.write(90);       
}

void robot_control()
{
    if (MotorState == PARAM_BACK)
      motor_control_from_robot(PARAM_STOP);
    
    //Sprawdzam  czy jest przeszkoda 30 cm od pojazdu wartość 255 - więcej niż 2 metry
    byte top_dis = distance_read();
    
    if (top_dis < 30)
    {
        
        byte left_dis, right_dis;
        
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
}

