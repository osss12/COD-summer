package com.example.voicerecorder;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final int MICROPHONE_PERMISSION_CODE = 200;
    MediaRecorder mediaRecorder;
    TextView textView;
    MediaPlayer mediaPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isMicrophonePresent()) {
            getMicrophonePermission();
        }
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        file = new File(musicDirectory , "test"+ ".wav");
    }
    private static final int SAMPLING_RATE_IN_HZ = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2;

    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;
    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    File file;
    public void btnRecord(View v) {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        file = new File(musicDirectory , "test"+ ".wav");
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(file.getPath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setAudioChannels(2);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.prepare();
            mediaRecorder.start();

            Toast.makeText(this , "Recoding has started" , Toast.LENGTH_LONG).show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void btnStop(View v){
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        Toast.makeText(this , "Recoding has stopped" , Toast.LENGTH_LONG).show();
    }
    public void btnPlay(View v){
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            Toast.makeText(this , "Recoding has started Playing" , Toast.LENGTH_LONG).show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static final int SPEECH_TO_TEXT_REQUEST_CODE = 1;
    Translate translate = null;
    public void getTranslateService() {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try (InputStream is = getResources().openRawResource(R.raw.credential)) {

            //Get credentials:
            final GoogleCredentials myCredentials = GoogleCredentials.fromStream(is);

            //Set credentials and get translate service:
            TranslateOptions translateOptions = TranslateOptions.newBuilder().setCredentials(myCredentials).build();
            translate = translateOptions.getService();

        } catch (IOException ioe) {
            ioe.printStackTrace();

        }
    }
    public String translate(String textToTranslate, String sourceLanguage, String targetLanguage) {

        /*
        Get input text to be translated:
        'Translate' has a 'TranslateOption' field too and there are configuration settings so explore.
         */

        Translation translation = translate.translate(textToTranslate, Translate.TranslateOption.targetLanguage(targetLanguage), Translate.TranslateOption.sourceLanguage(sourceLanguage) , Translate.TranslateOption.model("base"));
        String translatedText = translation.getTranslatedText();

        //Translated text and original text are set to TextViews:
        return translatedText;

    }
    /*
    It's value is set to 'best transcription'.
    It will used as an input for 'google translate API'.
     */
    String pares;
    String lang;
    public void btnConvert2(View v) throws IOException, InterruptedException, ExecutionException {
        textView = findViewById(R.id.textView);
        /*
        get the valid credentials.
         */
        getTranslateService();

        String S = pares;
        /*
        translate the text from a source language to the targeted language.
         */
        if (lang.equals("en-in")) {
            S = translate(S, Language.ENGLISH, Language.HINDI);
        }
        S = translate(S, Language.HINDI, Language.ENGLISH);
        textView.setText(S);

    }
    public void btnConvert(View v) throws IOException, InterruptedException {
        // Here we try to get relevant credentials from 'R.raw.credential' file.
        GoogleCredentials credentials;
        try {
            InputStream stream = this.getResources().openRawResource(R.raw.credential);
            credentials = GoogleCredentials.fromStream(stream);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading credentials", Toast.LENGTH_LONG).show();
            return;
        }

        // Create Speech-to-Text client with credentials generated above.
        SpeechClient speechClient;
        try {
            speechClient = SpeechClient.create(SpeechSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating SpeechClient", Toast.LENGTH_LONG).show();
            return;
        }
        /*
        All preferred languages(lang. code supported by google speech to text api.) of the user are added to the list.
        We can add at most 3 languages here.

         */
        ArrayList<String> languageList = new ArrayList<>();
        languageList.add("en-IN");
        languageList.add("hi-IN");

        /*
            Here , we specify the configuration of the loacal file, list of preferred languages to the  'RecognitionConfig'.
            setEncoding(RecognitionConfig.AudioEncoding.AMR) - encoding has been set to AMR instead to LINEAR16
            because local audio file had this configuration.
            Similarly, audio file had 'SampleRateHertz = 8000'.
            Specify then accordingly.
            You can read more about configuration settings on google.
        */
        RecognitionConfig recognitionConfig;
        try {
            recognitionConfig =
                    RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.AMR)
                            //.setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)

                            .setSampleRateHertz(8000)
                            .setLanguageCode("en-IN")
                            .addAllAlternativeLanguageCodes(languageList)
                            .setEnableSeparateRecognitionPerChannel(true)
                            .build();
        }catch (Exception e){
            Toast.makeText(this , "Could not RecognitionConfig" , Toast.LENGTH_LONG).show();
            return;
        }

        /*
        String PP = "/storage/emulated/0/Android/data/com.example.voicerecorder/files/Music/audio.wav";
        Here the local file is read and stored in 'audioBytes'.
        'file.getPath()' will fetch you the address of the local audio file.
         */
        String PP = file.getPath();
        ByteString audioBytes;
        try {
            Path path = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                path = Paths.get(PP);
            }
            byte[] data = new byte[0];
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                data = Files.readAllBytes(path);
            }
            audioBytes = ByteString.copyFrom(data);
        }catch (Exception e){
            Toast.makeText(this , "Problem in reading file" , Toast.LENGTH_LONG).show();
            return;
        }

        // Perform speech recognition
        RecognitionAudio recognitionAudio;
        try {
            recognitionAudio = RecognitionAudio.newBuilder().setContent(audioBytes).build();
        }catch (Exception e){
            Toast.makeText(this , "Could not recognitionAudio" , Toast.LENGTH_LONG).show();
            return;
        }

        /*
        We get the response from api in 'response' variable.
         */
        RecognizeResponse response;
        try {
            response = speechClient.recognize(recognitionConfig, recognitionAudio);
        } catch (ApiException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error during speech response", Toast.LENGTH_LONG).show();
            return;
        }

        textView = findViewById(R.id.textView);
        String Ss = "";
        List<SpeechRecognitionResult> results = response.getResultsList();
        for (SpeechRecognitionResult result : results) {
            lang = result.getLanguageCode();
            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
            System.out.printf("Transcription: %s%n", alternative.getTranscript());
            Ss = alternative.getTranscript() +" " + lang;
            pares = alternative.getTranscript();

        }
        /*
        In this version , Ss = best transcription + " " + it's language code.
        String pares is defined outside of this function.
        It's value is set to 'best transcription'.
        It will used as an input for 'google translate API'.
         */
        textView.setText(Ss);
    }
    private boolean isMicrophonePresent(){
        if(this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)){
            return true;
        }else{
            return false;
        }
    }
    private void getMicrophonePermission(){
        if(ContextCompat.checkSelfPermission(this , Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this , new String[] {Manifest.permission.RECORD_AUDIO} ,MICROPHONE_PERMISSION_CODE );
        }
    }
}