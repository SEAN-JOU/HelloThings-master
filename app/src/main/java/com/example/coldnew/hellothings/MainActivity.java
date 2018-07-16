package com.example.coldnew.hellothings;

import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
    private static final int INTERVAL_BETWEEN_BLINKS_MS = 1000;

    private Handler mHandler = new Handler();
    private Gpio mLedGpio;
    PeripheralManager service;
    DatabaseReference myRef;
    FirebaseDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{
                "com.google.android.things.permission.MANAGE_INPUT_DRIVERS",
                "com.google.android.things.permission.USE_PERIPHERAL_IO"}, 0);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("message");

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

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                Log.d("aaaa", "Value is: " + value);
                if(value.equals("true")){
                    try {
                        service = PeripheralManager.getInstance();
                        mLedGpio =service.openGpio(LED);
                        mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                        mLedGpio.setValue(true);
                        Log.i(TAG, "Start blinking LED by GPIO21");
                        mHandler.post(mBlinkRunnable);
                    } catch (IOException e) {
                        Log.e(TAG, "Error on PeripheralIO API", e);
                    }
                }
                else {
                    mHandler.removeCallbacks(mBlinkRunnable); // <---- Add this
                    Log.i(TAG, "Closing LED GPIO21 pin");
                    try {
                        mLedGpio.close();
                    } catch (IOException e) {
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
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
