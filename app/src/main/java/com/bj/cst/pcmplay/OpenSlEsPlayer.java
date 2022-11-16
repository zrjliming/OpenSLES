package com.bj.cst.pcmplay;


public class OpenSlEsPlayer {

    static {
        System.loadLibrary("PlayJni");
    }

    public native void init();

    public native void sendPcmData(byte[] data, int size);
    public native void release();

}
