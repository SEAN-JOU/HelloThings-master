package com.example.coldnew.hellothings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity2 extends AppCompatActivity {


    DatabaseReference myRef;
    FirebaseDatabase database;
    TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        textView= (TextView) findViewById(R.id.textView);
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("message");
        textView.setText("");
    }

    public void onResume() {
        super.onResume();

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                textView.setText(value);
                Log.d("aaaa", "Value is: " + value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                textView.setText("error");
                Log.d("aaaa", "Value is: " + "error");
            }
        });
    }
}
