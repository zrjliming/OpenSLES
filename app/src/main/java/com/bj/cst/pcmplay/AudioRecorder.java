package com.bj.cst.pcmplay;


public class AudioRecorder {
    static {
        System.loadLibrary("PlayJni");
    }

    public native void startRecord();

    public native void stopRecord();

    public native void release();
}
