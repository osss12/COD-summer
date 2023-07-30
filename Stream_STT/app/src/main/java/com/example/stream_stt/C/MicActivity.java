package com.example.stream_stt.C;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.stream_stt.D.SpeechService;
import com.example.stream_stt.D.VoiceRecorder;
import com.example.stream_stt.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

public class MicActivity extends AppCompatActivity {

    private ListView listView;
    private List<String> list;
    private TextView textView;
    private ArrayAdapter adapter;
    private static final int MICROPHONE_PERMISSION_CODE = 200;
    private static final int RECORD_REQUEST_CODE = 101;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private SpeechService mSpeechService;
    private VoiceRecorder voiceRecorder;
    private final VoiceRecorder.Callback callback = new VoiceRecorder.Callback() {
        @Override
        public void onVoiceStart() {
            if(mSpeechService != null){
                mSpeechService.startRecognizing(voiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if(mSpeechService != null){
                mSpeechService.recognize(data,size);
            }
        }

        @Override
        public void onVoiceEnd() {
            if(mSpeechService != null){
                mSpeechService.finishRecognizing();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.c_mic_activity);

        textView = findViewById(R.id.textView7);
        listView = findViewById(R.id.listView);
        list = new ArrayList<>();
        list.add("Google Api");
        list.add("Experiment 1");
        adapter = new ArrayAdapter(this , android.R.layout.simple_list_item_1 , list);
        listView.setAdapter(adapter);
        listView.setVerticalScrollBarEnabled(true);

        mSpeechService = new SpeechService(this);
        ButterKnife.bind(this);
    }

    private boolean isMicrophonePresent(){
        if(this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)){ return true;
        }else{ return false;}
    }

    private void getMicrophonePermission(){
        if(ContextCompat.checkSelfPermission(this , Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this , new String[] {Manifest.permission.RECORD_AUDIO} ,MICROPHONE_PERMISSION_CODE );
        }
    }
    private int GrantedPermission(String permission){
        return ContextCompat.checkSelfPermission(this , permission);
    }
    private void makeRequest(String permission){
        ActivityCompat.requestPermissions(this , new String[] {permission} ,MICROPHONE_PERMISSION_CODE );
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//
//        if (requestCode == RECORD_REQUEST_CODE) {
//            if (grantResults.length == 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                finish();
//            } else {
//                startVoiceRecorder();
//            }
//        }
//    }

    private final SpeechService.Listener listener = new SpeechService.Listener() {
        @Override
        public void onSpeechRecognized(String text, boolean isFinal) {
            if(isFinal){
                voiceRecorder.dismiss();
            }
            if(textView!= null && !TextUtils.isEmpty(text)){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(isFinal){
                            textView.setText(null);
                            list.add(0,text);
                            adapter.notifyDataSetChanged();
                            listView.smoothScrollToPosition(0);
                            textView.setVisibility(View.GONE);
                        }else{
                            textView.setText(text);
                            textView.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }
    };
    private void startVoiceRecorder() {
        if (voiceRecorder != null) {
            voiceRecorder.stop();
        }
        voiceRecorder = new VoiceRecorder(callback, this);
        voiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        if (voiceRecorder != null) {
            voiceRecorder.stop();
            voiceRecorder = null;
        }
    }


    public void btnStart( View v) {
        if(GrantedPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_DENIED){
            textView.setText("GOT PERMISSION");
            startVoiceRecorder();
        }else {
            makeRequest(Manifest.permission.RECORD_AUDIO);
        }
        mSpeechService.addListener(listener);
    }

    @Override
    protected void onStop() {
        stopVoiceRecorder();

        mSpeechService.removeListener(listener);
        mSpeechService.Destroy();
        mSpeechService=null;
        super.onStop();
    }


}