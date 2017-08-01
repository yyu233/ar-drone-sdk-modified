package com.parrot.freeflight.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.parrot.freeflight.VIONavData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by yizheng on 7/28/17.
 */

public class VIONavDataService extends IntentService {


    private static final String RETRIEVE_NAV_DATA="RETRIEVE_NAV_DATA";
    private static final String TAG="VIONavDataService";
    private ServerSocket serverSocket;
    private Socket pipeline;
    private ObjectInputStream serverInputStream;
    private VIONavData vioNavData;


    public VIONavDataService(){
        super("VIONavDataService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
            Log.d(TAG, "VIONavDataService started");
            try {
                serverSocket=new ServerSocket(5370);
                pipeline= serverSocket.accept();
                Log.d(TAG, "TCP connection status: "+pipeline.isConnected());
                Log.d(TAG, "Tango phone ip address: "+ pipeline.getRemoteSocketAddress().toString());
                while(true){
                    serverInputStream=new ObjectInputStream(pipeline.getInputStream());
                    //Retrieve VIO nav data from the pipeline
                    try {
                        vioNavData= (VIONavData) serverInputStream.readObject();
                        Log.d(TAG, "Position")
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }




}
