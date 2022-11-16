package com.bj.cst.pcmplay;

import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.bj.gxz.pcmplay.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OpenSlEs";
    private OpenSlEsPlayer openSlEsPlayer;
    private static final int PERMISSION_REQUEST = 111;

    private AudioTrackPlayer audioTrackPlayer;
    private MediaPlayer mediaPlayer;
    private LinkedBlockingDeque<Data> deque1 = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<Data> deque2 = new LinkedBlockingDeque<>();

    private ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    private volatile boolean stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!checkPermissionAllGranted(PERMISSIONS_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    PERMISSIONS_STORAGE, PERMISSION_REQUEST);
        }

//        loadData();

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.reset();
            AssetManager mAssetManager = getAssets();

            AssetFileDescriptor mAssetFileDescriptor = mAssetManager.openFd("ding.wav");
            mediaPlayer.setDataSource(mAssetFileDescriptor.getFileDescriptor(),
                    mAssetFileDescriptor.getStartOffset(), mAssetFileDescriptor.getLength());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

        openSlEsPlayer = new OpenSlEsPlayer();
        openSlEsPlayer.init();

        audioTrackPlayer = new AudioTrackPlayer();
        audioTrackPlayer.initAudioTrack();
    }

    public void opensles(View view) {
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> play(1));
    }

    public void audioTrack(View view) {
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> play(2));
    }

    private static class Data implements Serializable {
        public byte[] data;
        public int size;

        public Data(byte[] data, int size) {
            this.data = data;
            this.size = size;
        }
    }


    private void loadData() {
        fixedThreadPool.submit(() -> {
            try {
                // mix.pcm是采样率44100 双声道 采样位数16位
//                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mix.pcm";
//                String path = Environment.getExternalStorageDirectory() + File.separator+"audio1.pcm";
                String path = "/data/user/0/com.bj.cst.pcmplay/files/audio1.pcm";
                InputStream in = new FileInputStream(path);

                int n = 0;
                while (true) {
                    byte[] buffer = new byte[44100 * 2 * 2];
                    n = in.read(buffer);
                    if (n == -1) {
                        break;
                    }
                    Data data = new Data(buffer, n);
                    deque1.add(data);
                    deque2.add(data);
                }
                in.close();
                Log.d(TAG, "loadData read done!");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "loadData Exception ", e);
            }
        });
    }


    private void play(int type) {
        if (type == 1) {
            fixedThreadPool.submit((Runnable) () -> {
                while (!stop && deque2.size() > 0) {
                    try {
                        Data data = deque2.poll(100, TimeUnit.MILLISECONDS);
                        if (data == null) {
                            continue;
                        }
                        openSlEsPlayer.sendPcmData(data.data, data.size);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "openSlEsPlayer Exception ", e);
                    }
                }
                Log.e(TAG, "openSlEsPlayer done");
            });
        } else {
            fixedThreadPool.submit(() -> {
                while (!stop && deque1.size() > 0) {
                    try {
                        Data data = deque1.poll(100, TimeUnit.MILLISECONDS);
                        if (data == null) {
                            continue;
                        }
                        audioTrackPlayer.write(data.data, 0, data.size);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "audioTrackPlayer Exception ", e);
                    }
                }
                Log.e(TAG, "audioTrackPlayer done");
            });
        }
    }


    private AudioRecorder audioRecorder = new AudioRecorder();

    public void startRecord(View view) {

//       String folderPath = Environment.getExternalStorageDirectory() + File.separator+"audio1.pcm";
       String folderPath = MainActivity.this.getFilesDir().getPath()+ File.separator+"audio1.pcm";
        Log.e(TAG,  " startRecord folderPath path "+ folderPath.toString());
        File fileDir = new File(folderPath);
        if(fileDir.exists()){
            fileDir.delete();
        }
        if (!fileDir.exists()) {
           try {
               fileDir.createNewFile();
               Log.e(TAG,  " startRecord path "+ fileDir.getPath()+" abs "+fileDir.getAbsolutePath());
           } catch (IOException e) {
               e.printStackTrace();
           }
       }

        audioRecorder.startRecord();
    }

    public void stopRecord(View view) {
        audioRecorder.stopRecord();
    }

    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                return false;
            }
        }
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop = true;
        fixedThreadPool.shutdown();
        audioTrackPlayer.release();
        openSlEsPlayer.release();
        deque1.clear();
        deque2.clear();
        mediaPlayer.stop();
        mediaPlayer.release();
        audioRecorder.release();
    }
}