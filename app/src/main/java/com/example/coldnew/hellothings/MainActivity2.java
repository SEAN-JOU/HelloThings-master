package com.example.coldnew.hellothings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class MainActivity2 extends AppCompatActivity implements TtsSpeaker.Listener, PocketSphinx.Listener {

    private TtsSpeaker tts;
    private PocketSphinx pocketsphinx;
    private State state;
    FirebaseDatabase database;
    DatabaseReference dbAI;
    private ImageButton btn;
    private ImageButton playBtn;
    private TextView textInput;
    private TextToSpeech mTts;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private final int MY_DATA_CHECK_CODE = 150;
    private static final boolean USE_VOICEHAT_I2S_DAC = Build.DEVICE.equals("rpi3");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        tts = new TtsSpeaker(this, this);
        database = FirebaseDatabase.getInstance();
        dbAI = database.getReference("dbAI");


        btn = (ImageButton) findViewById(R.id.mic);
        playBtn = (ImageButton) findViewById(R.id.playButton);
        textInput = (TextView) findViewById(R.id.txtSpeechInput);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }});
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textToSpeechConverter();
                Log.v("Main", "play button");
            }});


//        initAudioRecorder();
//        AudioDeviceInfo audioInputDevice = null;
//        AudioDeviceInfo audioOutputDevice = null;
//        if (USE_VOICEHAT_I2S_DAC) {
//            audioInputDevice = findAudioDevice(AudioManager.GET_DEVICES_INPUTS, AudioDeviceInfo.TYPE_USB_DEVICE);
//            if (audioInputDevice == null) {
//
//            }
//            audioOutputDevice = findAudioDevice(AudioManager.GET_DEVICES_OUTPUTS, AudioDeviceInfo.TYPE_USB_DEVICE);
//            if (audioOutputDevice == null) {
//
//            }
//        }

    }


    private static final int SAMPLE_RATE = 44100;
    private static final int ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_FORMAT = AudioFormat.CHANNEL_IN_MONO;

    private AudioRecord mRecorder;
    private final int mBufferSize = AudioRecord
            .getMinBufferSize(SAMPLE_RATE, CHANNEL_FORMAT, ENCODING_FORMAT);

    public void initAudioRecorder() {
        if (mRecorder == null) {
            try {
                mRecorder = new AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.MIC)
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(ENCODING_FORMAT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(CHANNEL_FORMAT)
                                .build())
                        .setBufferSizeInBytes(2 * mBufferSize)
                        .build();
                mRecorder.startRecording();
            } catch (UnsupportedOperationException e) {

            }
        }
    }


    private AudioDeviceInfo findAudioDevice(int deviceFlag, int deviceType) {
        AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] adis = manager.getDevices(deviceFlag);
        for (AudioDeviceInfo adi : adis) {
            if (adi.getType() == deviceType) {
                return adi;
            }
        }
        return null;
    }

    public void onResume() {
        super.onResume();

        dbAI.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                if (value.equals("true")) {
                } else if (value.equals("false")) {

                    Intent intent = new Intent(MainActivity2.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void promptSpeechInput() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.speech_prompt);
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    R.string.speech_not_supported,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    textInput.setText(result.get(0));
                }
            }
            break;
            case MY_DATA_CHECK_CODE: {
                if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    Log.v("Main", "need intallation");
                    startActivity(installIntent);
                    mTts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {

                        }
                    });

                }
                mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            mTts.setLanguage(Locale.US);
                        }
                        String myText = textInput.getText().toString();
                        mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
                    }
                });
            }
        }
    }

    private void textToSpeechConverter() {
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
    }


    private enum State {
        INITALIZING,
        LISTENING_TO_KEYPHRASE,
        CONFIRMING_KEYPHRASE,
        LISTENING_TO_ACTION,
        CONFIRMING_ACTION,
        TIMEOUT
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        tts.onDestroy();
        pocketsphinx.onDestroy();
    }

    @Override
    public void onTtsInitialized() {
        // There's no runtime permissions on Android Things.
        // Otherwise, we would first have to ask for the Manifest.permission.RECORD_AUDIO
        pocketsphinx = new PocketSphinx(this, this);
    }

    @Override
    public void onTtsSpoken() {
        switch (state) {
            case INITALIZING:
            case CONFIRMING_ACTION:
            case TIMEOUT:
                state = State.LISTENING_TO_KEYPHRASE;
                pocketsphinx.startListeningToActivationPhrase();
                break;
            case CONFIRMING_KEYPHRASE:
                state = State.LISTENING_TO_ACTION;
                pocketsphinx.startListeningToAction();
                break;
        }
    }

    @Override
    public void onSpeechRecognizerReady() {
        state = State.INITALIZING;
        textInput.setText("I'm ready!");
    }

    @Override
    public void onActivationPhraseDetected() {
        state = State.CONFIRMING_KEYPHRASE;
        textInput.setText("Yup?");
    }

    @Override
    public void onTextRecognized(String recognizedText) {
        state = State.CONFIRMING_ACTION;

        String answer;
        String input = recognizedText == null ? "" : recognizedText;
        if (input.contains("tv")) {
            answer = "No, you need to work!";
        } else if (input.contains("time")) {
            DateFormat dateFormat = new SimpleDateFormat("HH mm", Locale.US);
            answer = "It is " + dateFormat.format(new Date());
        } else if (input.matches(".* joke")) {
            answer = "You are a joke.";
        } else if (input.contains("weather")) {
            answer = "Buy me some sensors, and I will tell you.";
        } else if (input.matches("how are you.*")) {
            answer = "Could not be worst with you.";
        } else {
            answer = "Sorry, I didn't understand your poor English.";
        }
        textInput.setText(answer);
    }

    @Override
    public void onTimeout() {
        state = State.TIMEOUT;
        textInput.setText("Timeout! You're too slow");
    }
}
