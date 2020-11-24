package com.example.my.bpi_test003;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;
    private static final String LOG_TAG_EXTERNAL_STORAGE = "EXTERNAL_STORAGE";

    private static String TAG = "PC";
    String networkSSID = "Scanner_RB";
    String welcome = "ยินดีต้อนรับสู่เครื่องช่วยอ่านสำหรับผู้พิการทางสายตาและผู้สูงอายุ";

    //View
    TextView txt;
    TextView text[] = new TextView[5];
    TextView motor[] = new TextView[4];
    ImageView im;

    //file + OCR + img
    private static final String FILE1 = "file1.txt";
    private static final String FILE2 = "file2.txt";
    private static final String FILE3 = "file3.txt";
    private static final String FILE4 = "file4.txt";
    private static final String FILE5 = "file5.txt";

    Bitmap image;
    public static String fname = "";
    public static String fname2 = "imgForOCR.jpg";

    private TessBaseAPI mTess;
    String datapath = "";

    //TextToSpeech
    public TextToSpeech mTTS;

    //GPIO
    String port[] = {"PB2", "PL8", "PL9", "PC3", "PH10","PE4"};
    String port2[] = {"PE19", "PE18", "PE5"};
    IO myio = new IO();
    int count = 0;

    //Check State
    int sel = 1; // sel = 1: tha+eng , 2 = kor+eng
    boolean wifistate = false;
    boolean imgstate = false;
    boolean btnBusy = false;
    int btnState = 0;
    int savestate = 0;
    int selbtn = 0;
    int currentbtn = 0;
    int img_ok = 0;

    //Audio manager
    AudioManager audioManager;
    int maxVolume;
    MediaPlayer mediaPlayer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,maxVolume,0);

        // initialize TextToSpeech
        mTTS = new TextToSpeech(this, this);
        // initialize Tesseract API
        String language = "eng+tha";
        datapath = getFilesDir() + "/tesseract/";
        mTess = new TessBaseAPI();
        checkFile(new File(datapath + "tessdata/"));
        mTess.init(datapath, language);
        mTess.setDebug(true);

        myio.init();
        text[0] = findViewById(R.id.Bt1);
        text[1] = findViewById(R.id.Bt2);
        text[2] = findViewById(R.id.Bt3);
        text[3] = findViewById(R.id.Bt4);
        text[4] = findViewById(R.id.Bt5);

        motor[0] = findViewById(R.id.l1);
        motor[1] = findViewById(R.id.l2);
        motor[2] = findViewById(R.id.l3);
        motor[3] = findViewById(R.id.l4);
        im = findViewById(R.id.imageView);
        txt = findViewById(R.id.text);
        txt.setMovementMethod(new ScrollingMovementMethod());
        final int[] portState = new int[port.length];
        Thread t = new Thread() {
            public void run() {
                while (true) {
                    if(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != maxVolume) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,maxVolume,0);
                    }
                    for (int i = 0; i < port.length; i++) {
                        portState[i] = myio.getLevel(port[i]);
                        //Log.d(TAG, "portStart: "+i+" = "+portState[i]);
                    }
                    if (count == 0) {
                        count = 1;
                        if (portState[0] == 0 && !btnBusy) dojob1();
                        else if (portState[1] == 0 && !btnBusy) dojob2();
                        else if (portState[2] == 0 && !btnBusy) dojob3();
                        else if (portState[3] == 0 && !btnBusy) dojob4();
                        else if (portState[4] == 0 && !btnBusy) dojob5();
                        else if (portState[5] == 0 && !btnBusy) dojob6();
                        count = 0;
                    }
                }
            }
        };
        t.start();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString();
        String musicName = "doo white.mp3";
        try {
            mediaPlayer.setDataSource(path+File.separator+musicName);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //for button PB2
    private void dojob1() {
        Log.i(TAG, "dojob1");
        selbtn = 1;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (savestate == 1 || savestate == 2) {
                    savefile(FILE1);
                } else {
                    loadfile(FILE1);
                }
            }
        });
    }
////////////////////////////////////////////////////////////////////////////////////////////////////
    // ocr process
    private void dojob2() {
        Log.i(TAG, "dojob2");
        selbtn = 2;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (savestate == 1 || savestate == 2) {
                    savefile(FILE2);
                } else {
                    loadfile(FILE2);
                }
            }
        });
    }

    private void dojob3() {
        Log.i(TAG, "dojob3");
        selbtn = 3;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (savestate == 1 || savestate == 2) {
                    savefile(FILE3);
                } else {
                    loadfile(FILE3);
                }
            }
        });
    }

    private void dojob4() {
        Log.i(TAG, "dojob4");
        selbtn = 4;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (savestate == 1 || savestate == 2) {
                    savefile(FILE4);
                } else {
                    loadfile(FILE4);
                }
            }
        });
    }
////////////////////////////////////////////////////////////////////////////////////////////////////
    private void dojob5() {
        Log.i(TAG, "dojob5");
        selbtn = 5;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (savestate == 1 || savestate == 2) {
                    savefile(FILE5);
                } else {
                    loadfile(FILE5);
                }
            }
        });

        /*if(mediaPlayer.isPlaying())
            mediaPlayer.pause();
        else
            mediaPlayer.start();*/
    }
    private void dojob6() {
        Log.i(TAG, "dojob6");
        btnBusy = true;
        if(mTTS.isSpeaking()) {
            mTTS.stop();
            myTalk("หยุดเล่น",2);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    im.setImageResource(R.drawable.year60);
                    txt.setText(welcome);
                }
            });
            btnBusy = false;
        }
        else {
            myTalk("เริ่มการสแกน กรุณาใส่กระดาษด้านหน้า",1);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    im.setImageResource(R.drawable.year60);
                    txt.setText("Scanning...");
                }
            });
            btnState = 1;
            wifistate = false;
            imgstate = false;
            img_ok = 0;

            //Turn on scanner
            if (myio.getLevel(port2[0]) == 1)
                myio.setlevel(port2[0], 0);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (myio.getLevel(port2[0]) == 0)
                myio.setlevel(port2[0], 1);

            try {
                Thread.sleep(3600);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Start scan
            if (myio.getLevel(port2[0]) == 1)
                myio.setlevel(port2[0], 0);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (myio.getLevel(port2[0]) == 0)
                myio.setlevel(port2[0], 1);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Play feed
            if (myio.getLevel(port2[1]) == 0)
                myio.setlevel(port2[1], 1);
            try {
                Thread.sleep(6700);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (myio.getLevel(port2[1]) == 1)
                myio.setlevel(port2[1], 0);

            //Stop scan
            if (myio.getLevel(port2[0]) == 1)
                myio.setlevel(port2[0], 0);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (myio.getLevel(port2[0]) == 0)
                myio.setlevel(port2[0], 1);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Stop feed
            if (myio.getLevel(port2[1]) == 1)
                myio.setlevel(port2[1], 0);

            myTalk("สแกนเสร็จสิ้น",1);
            myTalk("กำลังเชื่อมต่อ",1);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    img_ok = 0;
                    txt.setText("Connecting wifi");
                    if (!isOnline()) {
                        txt.setText(connectToKnownWifi(getApplicationContext(), networkSSID));
                        txt.setText("Test Connect");
                        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        txt.append("SSID :" + wifiInfo.getSSID() + "\n");

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    flashairChkpic();
                }
            });
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void del_pic(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "/101MEDIA/" + fname);
                if (file.exists()) {
                    file.delete();
                }
            }
        });
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onInit(int status) {
        mTTS.setLanguage(new Locale("th", "TH"));
        mTTS.setSpeechRate(1);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mTTS.speak(welcome, TextToSpeech.QUEUE_FLUSH, null, null);
        txt.setText(welcome);
        String instruction = "\nกดปุ่ม 6 เพื่อข้ามคำแนะนำ \nหมายเลข 1 ถึง 5 ปุ่มสำหรับเล่นไฟล์ที่บันทึกไว้หรือบันทึกไฟล์ที่ประมวลผลเสร็จสิ้น \nหมายเลข 6 ปุ่มสำหรับเริ่มการสแกนเอกสารหรือหยุดอ่านข้อความ \nหมายเลข 7 กดปุ่มค้างประมาณ 3 วินาทีเพื่อเปิดหรือปิดเครื่อง ";
        instruction += "\nหมายเลข 8 หมุนเพื่อปรับระดับเสียง \nหมายเลข 9 ช่องสำหรับต่อหูฟังหรือลำโพง \nหมายเลขสิบ สวิตช์เปิดปิดระบบไฟ \nหมายเลขสิบเอ็ด ช่อง Micro usb สำหรับชาร์จแบตเตอรี่";
        myTalk(instruction,1);
        txt.append(instruction);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void myTalk(final String txt,final int mode) {
        if(!txt.isEmpty()) {
            String s = txt.substring(0,1);
            if (!s.matches(".*[a-zA-Z].*")) {
                sel = 2;
            }
        }

        if(mode == 1) {
            if (sel == 1) {
                while (mTTS.isSpeaking()) {
                }
                mTTS.setLanguage(Locale.ENGLISH);
                mTTS.setSpeechRate(0.75f);
                mTTS.speak(txt, TextToSpeech.QUEUE_ADD, null, null);
            } else {
                mTTS.setLanguage(new Locale("th", "TH"));
                mTTS.setSpeechRate(1);
                mTTS.speak(txt, TextToSpeech.QUEUE_ADD, null, null);
                sel = 1;
            }
        }
        else if(mode == 2) {
            if (sel == 1) {
                mTTS.setLanguage(Locale.ENGLISH);
                mTTS.setSpeechRate(0.75f);
                mTTS.speak(txt, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                mTTS.setLanguage(new Locale("th", "TH"));
                mTTS.setSpeechRate(1);
                mTTS.speak(txt, TextToSpeech.QUEUE_FLUSH, null, null);
                sel = 1;
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/tha.traineddata";
            String filepath1 = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();
            InputStream instream = assetManager.open("tessdata/tha.traineddata");
            InputStream instream1 = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);
            OutputStream outstream1 = new FileOutputStream(filepath1);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

            while ((read = instream1.read(buffer)) != -1) {
                outstream1.write(buffer, 0, read);
            }
            outstream1.flush();
            outstream1.close();
            instream1.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
            File file1 = new File(filepath1);
            if (!file1.exists()) {
                throw new FileNotFoundException();
            }
            File file2 = new File(filepath1);
            if (!file2.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void processImage(){
        if(img_ok==1) {
            final ProgressDialog waitDialog;
            // Setting ProgressDialog
            waitDialog = new ProgressDialog(this);
            waitDialog.setMessage("Processing...");
            waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            waitDialog.show();
            new AsyncTask<String, Integer, String>(){
                @Override
                protected String doInBackground(String... params) {
                    String text = "";
                    if (image != null) {
                        Log.d(TAG, "doInBackground: Processing2...");
                        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

                        if (!textRecognizer.isOperational()) {
                            // Note: The first time that an app using a Vision API is installed on a
                            // device, GMS will download a native libraries to the device in order to do detection.
                            // Usually this completes before the app is run for the first time.  But if that
                            // download has not yet completed, then the above call will not detect any text,
                            // barcodes, or faces.
                            // isOperational() can be used to check if the required native libraries are currently
                            // available.  The detectors will automatically become operational once the library
                            // downloads complete on device.
                            Log.w(TAG, "Detector dependencies are not yet available.");

                            // Check for low storage.  If there is low storage, the native library will not be
                            // downloaded, so detection will not become operational.
                            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

                            if (hasLowStorage) {
                                //Toast.makeText(this, "Low Storage", Toast.LENGTH_LONG).show();
                                Log.w(TAG, "Low Storage");
                            }
                        }

                        Frame imageFrame = new Frame.Builder()
                                .setBitmap(image)
                                .build();

                        SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);
                        for (int i = 0; i < textBlocks.size(); i++) {
                            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                            text += textBlock.getValue();
                            publishProgress(i);
                        }
                    }
                    return text;
                }

                @Override
                protected void onPostExecute(String text) {
                    waitDialog.dismiss();
                    //ocrResult = text.split(" ");

                    /*for(int i=0;i<ocrResult.length;i++) {
                        //Log.d(TAG, "corResult: " + ocrResult[i]);
                        //txt.setHighlightColor(ContextCompat.getColor(getApplicationContext(), R.color.highlight));
                        txt.append(ocrResult[i]+" ");
                    }*/
                    myTalk("ประมวลผลเสร็จสิ้น",1);
                    txt.setText(text);

                    myTalk("กรุณาเลือกปุ่มที่ต้องการบันทึก",1);
                    savestate = 1;

                    btnBusy = false;
                }
            }.execute();
        } else {
            txt.append("image not found [OCR Process Abort]\n");
            btnBusy = false;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void processImage2(){
        if(img_ok==1) {
            final ProgressDialog waitDialog;
            // Setting ProgressDialog
            waitDialog = new ProgressDialog(this);
            waitDialog.setMessage("Processing...");
            waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            waitDialog.show();
            new AsyncTask<String, Integer, String>(){
                @Override
                protected String doInBackground(String... params) {
                    String OCRresult = "";
                    mTess.setImage(image);
                    OCRresult = mTess.getUTF8Text();
                    return OCRresult;
                }

                @Override
                protected void onPostExecute(String text) {
                    waitDialog.dismiss();
                    myTalk("ประมวลผลเสร็จสิ้น",1);
                    if(text.isEmpty()) {
                        myTalk("ไม่พบข้อความ",1);
                    } else {
                        txt.setText(text);
                        myTalk("กรุณาเลือกปุ่มที่ต้องการบันทึก", 1);
                        savestate = 1;
                    }
                    btnBusy = false;
                }
            }.execute();
        } else {
            txt.append("image not found [OCR Process Abort]\n");
            btnBusy = false;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/tha.traineddata";
            String datafilepath1 = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);
            File datafile1 = new File(datafilepath1);
            if (!datafile.exists()) {
                copyFiles();
            }
            if (!datafile1.exists()){
                copyFiles();
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void displayOneImage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txt.append("\nprepare image");
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "100MEDIA/" + fname2);
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    image = bitmap;
                    int nh = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
                    //image = scaled;
                    im.setImageBitmap(scaled);
                    img_ok = 1;
                    Log.d(TAG, "run: img_ok = 1");
                    deleteOldPic();
                } else {
                    img_ok = 0;
                    txt.append("\nfile :" + fname2 + " notfound");
                }
            }
        });
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION)
        {
            int grantResultsLength = grantResults.length;
            if(grantResultsLength > 0 && grantResults[0]== PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(getApplicationContext(), "You grant write external storage permission. Please click original button again to continue.", Toast.LENGTH_LONG).show();
            }else
            {
                Toast.makeText(getApplicationContext(), "You denied write external storage permission.", Toast.LENGTH_LONG).show();
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String connectToKnownWifi(Context context, String ssid) {
        String out ="";
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            out += i.SSID + "\n";
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
            }
        }
        return out;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void flashairChkpic() {
        String dir = "DIR=/DCIM/100MEDIA";
        final ProgressDialog waitDialog;
        // Setting ProgressDialog
        waitDialog = new ProgressDialog(this);
        waitDialog.setMessage("Connecting...");
        waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waitDialog.show();
        new AsyncTask<String, Void, String>(){
            @Override
            protected String doInBackground(String... params) {
                String dir = params[0];
                String fileCount = null;
                for(int i=1;i<=3;i++) {
                    fileCount = FlashAirRequest.getString("http://flashair/command.cgi?op=101&" + dir);
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return fileCount;
            }
            @Override
            protected void onPostExecute(String fileCount) {
                waitDialog.dismiss();
                if(fileCount.isEmpty()) {
                    wifistate = false;
                    txt.setText("Connect Failed");
                    myTalk("การเชื่อมต่อล้มเหลว",1);
                } else {
                    wifistate = true;
                    txt.setText("Connect Success");
                    txt.append("\nItems Found: " + fileCount);
                }
            }
        }.execute(dir);

        new AsyncTask<String, Void, String>(){
            @Override
            protected String doInBackground(String... params) {
                String dir = params[0];
                String fileNames = "";
                String files = FlashAirRequest.getString("http://flashair/command.cgi?op=100&" + dir);
                String[] allFiles = files.split("([,\n])"); // split by newline or comma
                for(int i = 2; i < allFiles.length; i= i + 6) {
                    if(allFiles[i].contains(".")) {
                        // File
                        fileNames += allFiles[i] + "\n";
                    }
                    else { // Directory, append "/"
                        fileNames += allFiles[i] + "/" + "\n";
                    }
                    fname = allFiles[i];
                }
                return fileNames;
            }
            @Override
            protected void onPostExecute(String file) {
                Log.d(TAG, "wifistate: " + wifistate);
                if (wifistate) {
                    txt.append("\n"+fname);
                    downloadFile(fname, "DCIM/100MEDIA");
                } else {
                    btnBusy = false;
                }
            }
        }.execute(dir);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void downloadFile(final String downloadFile, String directory) {
        final ProgressDialog waitDialog;
        // Setting ProgressDialog
        waitDialog = new ProgressDialog(this);
        waitDialog.setMessage("Downloading...");
        waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waitDialog.show();
        // Download file
        new AsyncTask<String, Void, Bitmap>(){
            @Override
            protected Bitmap doInBackground(String... params) {
                String fileName = params[0];
                return FlashAirRequest.getBitmap(fileName);
            }
            @Override
            protected void onPostExecute(Bitmap resultBitmap) {
                saveImage(resultBitmap, fname2);
                txt.append("\nLoad image success");
                Log.d(TAG, "imgstate: " + imgstate);
                if (imgstate) {
                    displayOneImage();
                    if(btnState == 1) {
                        myTalk("กำลังประมวลผล",1);
                        Log.i(TAG, "onPostExecute: Processing...");
                        processImage2();
                    }
                } else {
                    btnBusy = false;
                }
                btnState = 0;
                waitDialog.dismiss();
            }
        }.execute("http://flashair/" + directory + "/" + downloadFile);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void saveImage(Bitmap finalBitmap, String image_name) {

        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/100MEDIA/";
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = image_name;
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            imgstate = true;
        } catch (Exception e) {
            e.printStackTrace();
            imgstate = false;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void deleteOldPic() {
        String dir = "/DCIM/100MEDIA/";
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String dir = params[0];
                String result;
                Log.d(TAG, "http://flashair/upload.cgi?DEL=" + dir + fname);
                result = FlashAirRequest.getString("http://flashair/upload.cgi?DEL=" + dir + fname);
                return result;
            }

            @Override
            protected void onPostExecute(String result) {
                Log.i(TAG, "Delete old pic : " + result);
            }
        }.execute(dir);
    }
    //////////////////////////////////////////////////////////////////////////////////////////////
    public void savefile(String file) {
        loadfile(file);
        if(savestate == 1 || savestate == 3) {
            String text = txt.getText().toString();
            FileOutputStream fos = null;

            try {
                fos = openFileOutput(file, MODE_PRIVATE);
                fos.write(text.getBytes());

                Log.i(TAG, "Saved to " + getFilesDir() + "/" + file);
                savestate = 4;
                myTalk("บันทึกเสร็จสิ้น",2);
                loadfile(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    public void loadfile(String file) {
        FileInputStream fis = null;

        try {
            fis = openFileInput(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String text;

            if(savestate == 1) {
                currentbtn = selbtn;
                myTalk("ปุ่ม " + selbtn,1);
                String examtext = br.readLine();
                myTalk("พบไฟล์ที่บันทึกไว้แล้ว",1);
                myTalk(examtext,1);
                myTalk("ต้องการบันทึกทับไฟล์เดิมหรือไม่",1);
                savestate = 2;
            }
            else if(savestate == 2) {
                if(currentbtn == selbtn) {
                    savestate = 3;
                }
                else {
                    savestate = 0;
                    myTalk("ยกเลิกการบันทึกแล้ว",1);
                    myTalk("ข้อความของคุณคือ",1);
                    myTalk(txt.getText().toString(),1);
                    myTalk("สิ้นสุดข้อความ",1);
                }
            }
            else if(savestate == 4 || (savestate == 0 && !mTTS.isSpeaking())) {
                savestate = 0;
                while ((text = br.readLine()) != null) {
                    sb.append(text).append("\n");
                }
                txt.setText(sb.toString());
                String toSpeak = txt.getText().toString();
                myTalk("ปุ่ม " + selbtn + "ข้อความของคุณคือ",1);
                myTalk(toSpeak,1);
                myTalk("สิ้นสุดข้อความ",1);
            }
        } catch (FileNotFoundException e) {
            if(savestate == 2) {
                savestate = 3;
                return;
            }
            else {
                e.printStackTrace();
                myTalk("ไม่พบไฟล์ที่บันทึก",1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    public class StartMyServiceAtBootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Intent serviceIntent = new Intent(context, MainActivity.class);
                context.startService(serviceIntent);
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
}
////////////////////////////////////////////////////////////////////////////////////////////////////