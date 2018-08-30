package com.example.coldnew.hellothings;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
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
    private static final String LED = "BCM26"; //physical pin #37
    public static final String RMOTOR_PIN_PLUS = "BCM14"; //physical pin #8
    public static final String RMOTOR_PIN_REDUCE = "BCM27"; //physical pin #11

    public static final String LMOTOR_PIN_PLUS = "BCM15"; //physical pin #10
    public static final String LMOTOR_PIN_REDUCE = "BCM17"; //physical pin #11
    private static final String PWM_PIN = "PWM1"; //physical pin 35 or 33
    private static final String ECHO_PIN_NAME = "BCM20";
    private static final String TRIGGER_PIN_NAME = "BCM21";
    private Gpio mEcho;
    private Gpio mTrigger;
    private static final int INTERVAL_BETWEEN_BLINKS_MS = 1000;
    private Handler mHandler = new Handler();
    private Gpio mLedGpio;
    PeripheralManager service;
    DatabaseReference dbLED, dbMOTOR, dbSERVO, dbAI, dbUltrasound, dbDistance;
    FirebaseDatabase database;
    private Gpio rmMotorGpio_plus, rmMotorGpio_reduce, lmMotorGpio_plus, lmMotorGpio_reduce;
    private Pwm mServo;
    double aDouble = 70;
    private Handler mCallbackHandler;
    private Handler ultrasonicTriggerHandler;
    private static final int INTERVAL_BETWEEN_TRIGGERS = 3000;
    long time1, time2;


    private Runnable triggerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                readDistanceAsnyc();
                ultrasonicTriggerHandler.postDelayed(triggerRunnable, INTERVAL_BETWEEN_TRIGGERS);
            } catch (Exception e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ActivityCompat.requestPermissions(this, new String[]{
                "com.google.android.things.permission.MANAGE_INPUT_DRIVERS",
                "android.permission.RECORD_AUDIO",
                "com.google.android.things.permission.USE_PERIPHERAL_IO"}, 0);

        service = PeripheralManager.getInstance();
        database = FirebaseDatabase.getInstance();
        dbLED = database.getReference("dbLED");
        dbMOTOR = database.getReference("dbMOTOR");
        dbSERVO = database.getReference("dbSERVO");
        dbAI = database.getReference("dbAI");
        dbUltrasound = database.getReference("dbULTRASOUND");
        dbDistance = database.getReference("dbDISTANCE");
        try {
            mServo = service.openPwm(PWM_PIN);

        } catch (Exception es) {
            Log.d("aaaa", "bad");
        }

        dbUltrasound.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);

                if (value.equals("true")) {

                    HandlerThread handlerThread = new HandlerThread("callbackHandlerThread");
                    handlerThread.start();
                    mCallbackHandler = new Handler(handlerThread.getLooper());

                    // Prepare handler to send triggers
                    HandlerThread triggerHandlerThread = new HandlerThread("triggerHandlerThread");
                    triggerHandlerThread.start();
                    ultrasonicTriggerHandler = new Handler(triggerHandlerThread.getLooper());


                    try {
                        // Step 1. Create GPIO connection.
                        mEcho = service.openGpio(ECHO_PIN_NAME);
                        // Step 2. Configure as an input.
                        mEcho.setDirection(Gpio.DIRECTION_IN);
                        // Step 3. Enable edge trigger events.
                        mEcho.setEdgeTriggerType(Gpio.EDGE_BOTH);
                        // Step 4. Set Active type to HIGH, then it will trigger TRUE (HIGH, active) events
                        mEcho.setActiveType(Gpio.ACTIVE_HIGH);
                        // Step 5. Register an event callback.
                        mEcho.registerGpioCallback(mCallbackHandler, mCallback);
                    } catch (IOException e) {
                        Log.e(TAG, "Error on PeripheralIO API", e);
                    }

                    try {
                        // Step 1. Create GPIO connection.
                        mTrigger = service.openGpio(TRIGGER_PIN_NAME);

                        // Step 2. Configure as an output with default LOW (false) value.
                        mTrigger.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

                    } catch (IOException e) {
                        Log.e(TAG, "Error on PeripheralIO API", e);
                    }


                    ultrasonicTriggerHandler.post(triggerRunnable);
                } else {
                    if (mEcho != null) {
                        try {
                            mEcho.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error on PeripheralIO API", e);
                        }
                    }
                    if (mTrigger != null) {
                        try {
                            mTrigger.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error on PeripheralIO API", e);
                        }
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    protected void readDistanceAsnyc() throws Exception {
        // Just to be sure, set the trigger first to false
        mTrigger.setValue(false);
        Thread.sleep(0, 2000);

        // Hold the trigger pin high for at least 10 us
        mTrigger.setValue(true);
        Thread.sleep(0, 10000); //10 microsec

        // Reset the trigger pin
        mTrigger.setValue(false);

    }

    private GpioCallback mCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                mTrigger.setValue(false);
                try {
                    Thread.sleep(0, 2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mTrigger.setValue(true);
                try {
                    Thread.sleep(0, 10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mTrigger.setValue(false);

                while (mEcho.getValue() == false) {

                }
                time1 = System.nanoTime();
                Log.i(TAG, "Echo ARRIVED!");

                while (mEcho.getValue() == true) {

                }
                time2 = System.nanoTime();
                Log.i(TAG, "Echo ENDED!");

                long pulseWidth = time2 - time1;
                double d = (pulseWidth / 1000.0) / 100; //cm
                int distance = (int) d;
                Log.i(TAG, "distance: " + distance + " cm");

                dbDistance.setValue(distance + "");

            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }


        @Override
        public void onGpioError(Gpio gpio, int error) {
            try {
                Log.e(TAG, "error: " + error);
//            mCallback.onGpioError(gpio,error);
            } catch (Exception e) {
            }

        }
    };


    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLedGpio == null) {
                return;
            }
            try {
                mLedGpio.setValue(!mLedGpio.getValue());
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
                    Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                    startActivity(intent);
                    finish();
                } else if (value.equals("false")) {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        dbLED.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);

                Log.d(TAG, "Value is: " + value);


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
                    lmMotorGpio_plus = service.openGpio(LMOTOR_PIN_PLUS);
                    lmMotorGpio_reduce = service.openGpio(LMOTOR_PIN_REDUCE);
                    rmMotorGpio_plus = service.openGpio(RMOTOR_PIN_PLUS);
                    rmMotorGpio_reduce = service.openGpio(RMOTOR_PIN_REDUCE);
                    rmMotorGpio_plus.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                    rmMotorGpio_reduce.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                    lmMotorGpio_plus.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                    lmMotorGpio_reduce.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (value.equals("go")) {
                    try {
                        setRightPinValues(true, false);//go
                        setLeftPinValues(true, false);//go
                    } catch (Exception e) {
                    }
                } else if (value.equals("right")) {
                    try {
                        setRightPinValues(true, false);//go
                        setLeftPinValues(false, false);//go
                    } catch (Exception e) {
                    }
                } else if (value.equals("left")) {
                    try {
                        setRightPinValues(false, false);//go
                        setLeftPinValues(true, false);//go
                    } catch (Exception e) {
                    }
                } else if (value.equals("stop")) {
                    try {
                        setRightPinValues(false, false);//stop
                        setLeftPinValues(false, false);//go
                    } catch (Exception e) {
                    }
                } else if (value.equals("back")) {
                    try {
                        setRightPinValues(false, true);
                        setLeftPinValues(false, true);
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
                        public void run() {
                            try {
                                mServo.setPwmFrequencyHz(aDouble);
                                mServo.setPwmDutyCycle(5);
                                mServo.setEnabled(true);
                                Log.d("aaaa", "right");
                            } catch (Exception e) {
                                Log.d("aaaa", "badright");
                            }
                        }
                    }).start();


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
                        }
                    }).start();

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void setRightPinValues(boolean plus, boolean reduce) {

        try {
            rmMotorGpio_plus.setValue(plus);
            rmMotorGpio_reduce.setValue(reduce);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setLeftPinValues(boolean plus, boolean reduce) {

        try {
            lmMotorGpio_plus.setValue(plus);
            lmMotorGpio_reduce.setValue(reduce);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
