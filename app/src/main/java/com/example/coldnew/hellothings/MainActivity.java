package com.example.coldnew.hellothings;

import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "HelloThings";
    private static final String LED = "BCM21";
    public static final String MOTOR_PIN_PLUS = "BCM15"; //physical pin #12
    public static final String MOTOR_PIN_REDUCE = "BCM17"; //physical pin #11
    private static final String PWM_PIN = "PWM1"; //physical pin 35 or 33
    private static final int INTERVAL_BETWEEN_BLINKS_MS = 1000;
    private Handler mHandler = new Handler();
    private Gpio mLedGpio;
    PeripheralManager service;
    DatabaseReference dbLED, dbMOTOR, dbSERVO,dbAI;
    FirebaseDatabase database;
    private Gpio mMotorGpio_plus, mMotorGpio_reduce;
    private Pwm mServo;
    double aDouble =70;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{
                "com.google.android.things.permission.MANAGE_INPUT_DRIVERS",
                "com.google.android.things.permission.USE_PERIPHERAL_IO"}, 0);


        service = PeripheralManager.getInstance();
        database = FirebaseDatabase.getInstance();
        dbLED = database.getReference("dbLED");
        dbMOTOR = database.getReference("dbMOTOR");
        dbSERVO = database.getReference("dbSERVO");
        dbAI= database.getReference("dbAI");
        try {
            mServo = service.openPwm(PWM_PIN);

        } catch (Exception es) {
            Log.d("aaaa", "bad");
        }
        //  http://myandroidthings.com/post/tutorial-10   非常重要 方向控制器
    }


    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLedGpio == null) {
                return;
            }
            try {
                mLedGpio.setValue(!mLedGpio.getValue());  // Toggle the GPIO state
                Log.d(TAG, "GPIO21 set to " + mLedGpio.getValue());
                mHandler.postDelayed(mBlinkRunnable, INTERVAL_BETWEEN_BLINKS_MS);
            } catch (IOException e) {
                Log.d(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    public void onResume() {
        super.onResume();

        dbAI.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                if (value.equals("true")) {
                  Intent intent = new Intent(MainActivity.this,MainActivity2.class);
                  startActivity(intent);
                  finish();
                }
                 else if (value.equals("false")) {

                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }});

        dbLED.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);

                Log.d(TAG, "Value is: " + value);

//                try {
//                mLedGpio = service.openGpio(LED);
//                mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }


                if (value.equals("true")) {
                    try {
                        mLedGpio = service.openGpio(LED);
                        mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                        mLedGpio.setValue(true);
                        Log.d(TAG, "Start blinking LED by GPIO21");
                        mHandler.post(mBlinkRunnable);
                    } catch (Exception e) {
                        Log.d(TAG, "Error on PeripheralIO API", e);
                    }
                } else if (value.equals("false")) {
                    mHandler.removeCallbacks(mBlinkRunnable); // <---- Add this
                    Log.d(TAG, "Closing LED GPIO21 pin");
                    try {
                        mLedGpio.setValue(false);
                        mLedGpio.close();
                    } catch (Exception e) {
                        Log.d(TAG, "Error on PeripheralIO API", e);
                    } finally {
                        mLedGpio = null;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d(TAG, "Value is: " + "error");
            }
        });
        dbMOTOR.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                try {
                    mMotorGpio_plus = service.openGpio(MOTOR_PIN_PLUS);
                    mMotorGpio_reduce = service.openGpio(MOTOR_PIN_REDUCE);
                    mMotorGpio_plus.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                    mMotorGpio_reduce.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (value.equals("go")) {
                    try {
                        setPinValues(false, true);//go
                    } catch (Exception e) {
                    }
                } else if (value.equals("stop")) {
                    try {
                        setPinValues(false, false);//stop
                    } catch (Exception e) {
                    }
                } else if (value.equals("back")) {
                    try {
                        setPinValues(true, false);//back
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        dbSERVO.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);

                if (value.equals("right")) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() { try {
                                mServo.setPwmFrequencyHz(aDouble);
                                mServo.setPwmDutyCycle(5);
                                mServo.setEnabled(true);
                                Log.d("aaaa", "right");
                            } catch (Exception e) {
                                Log.d("aaaa", "badright");
                            }
                            }}).start();


                } else if (value.equals("front")) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mServo.setPwmFrequencyHz(aDouble);
                                mServo.setPwmDutyCycle(10);
                                Log.d("aaaa", "front");
                            } catch (Exception e) {
                                Log.d("aaaa", "badfront");
                            }
                        }
                    }).start();

                } else if (value.equals("left")) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mServo.setPwmFrequencyHz(aDouble);
                                mServo.setPwmDutyCycle(15);
                                Log.d("aaaa", "left");
                            } catch (Exception e) {
                                Log.d("aaaa", "badleft");
                            }
                        }}).start();

                }}
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }});
    }

    private void setPinValues(boolean plus, boolean reduce) {

        try {
            mMotorGpio_plus.setValue(plus);
            mMotorGpio_reduce.setValue(reduce);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }




}
