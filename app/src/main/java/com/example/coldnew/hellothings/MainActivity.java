package com.example.coldnew.hellothings;

import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.contrib.driver.pwmservo.Servo;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HelloThings";
    private static final String LED = "BCM21";
    public static final String MOTOR_PIN_PLUS = "BCM18"; //physical pin #12
    public static final String MOTOR_PIN_REDUCE = "BCM17"; //physical pin #11
    private static final int INTERVAL_BETWEEN_BLINKS_MS = 1000;
    private Handler mHandler = new Handler();
    private Gpio mLedGpio;
    PeripheralManager service;
    DatabaseReference dbLED, dbMOTOR;
    FirebaseDatabase database;
    private Gpio mMotorGpio_plus, mMotorGpio_reduce;
    Servo mServo;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
        mServo = new Servo("PWM0");
        mServo.setPulseDurationRange(0.6, 2.5);
        mServo.setAngleRange(0, 180);
        mServo.setEnabled(true);
        mServo.setAngle(90);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ActivityCompat.requestPermissions(this, new String[]{
                "com.google.android.things.permission.MANAGE_INPUT_DRIVERS",
                "com.google.android.things.permission.USE_PERIPHERAL_IO"}, 0);

        database = FirebaseDatabase.getInstance();
        dbLED = database.getReference("dbLED");
        dbMOTOR = database.getReference("dbMOTOR");
//        try {
//            Max98357A dac = VoiceHat.openDac();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    public void onResume() {
        super.onResume();

        dbLED.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);

                Log.d("aaaa", "Value is: " + value);

                if (value.equals("true")) {
                    try {
                        service = PeripheralManager.getInstance();
                        mLedGpio = service.openGpio(LED);
                        mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                        mLedGpio.setValue(true);
                        Log.i(TAG, "Start blinking LED by GPIO21");
                        mHandler.post(mBlinkRunnable);
                    } catch (Exception e) {
                        Log.e(TAG, "Error on PeripheralIO API", e);
                    }
                } else if (value.equals("false")) {
                    mHandler.removeCallbacks(mBlinkRunnable); // <---- Add this
                    Log.i(TAG, "Closing LED GPIO21 pin");
                    try {
                        mLedGpio.setValue(false);
                        mLedGpio.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Error on PeripheralIO API", e);
                    } finally {
                        mLedGpio = null;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d("aaaa", "Value is: " + "error");
            }
        });
        dbMOTOR.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                try {
                    mMotorGpio_plus = service.openGpio(MOTOR_PIN_PLUS);
                    mMotorGpio_reduce = service.openGpio(MOTOR_PIN_REDUCE);
                    mMotorGpio_plus.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                    mMotorGpio_reduce.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
//                    mBtnGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
//                    mMotorGpio.registerGpioCallback(mMotorCallback);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (value.equals("right")) {
                    try {
                        turnRight();
                    } catch (Exception e) {
                        Log.e(TAG, "Error on PeripheralIO API", e);
                    }
                }
                else if (value.equals("stop")) {
                    try {
                        stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else if (value.equals("left")) {
                    try {
                        turnLeft();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }});}
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void stop() {
        setPinValues(false, false);
    }

    public void turnLeft() {
        setPinValues(false, true);
    }

    public void turnRight() {
        setPinValues(true, false);
    }

    private void setPinValues(boolean plus, boolean reduce) {

        try {
            mMotorGpio_plus.setValue(plus);
            mMotorGpio_reduce.setValue(reduce);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    private GpioCallback mMotorCallback = new GpioCallback() {
//        @Override
//        public boolean onGpioEdge(Gpio gpio) {
//            Log.i(TAG, "GPIO callback ------------");
//
//
//            if (mMotorGpio == null) {
//                return true;
//            }
//
//            try {
//                if (gpio.getValue()) {
//                    // stop the motor. set output LOW.
//                    Log.d(TAG, "Stop Motor");
//                    mMotorGpio.setValue(false);
//                } else {
//                    // start the motor. set output HIGH.
//                    Log.d(TAG, "Start Motor");
//                    mMotorGpio.setValue(true);
//                }
//
//            } catch (IOException e) {
//                Log.e(TAG, "Error on PeripheralIO API", e);
//            }
//            // Return true to keep callback active.
//            return true;
//        }
//    };
}
