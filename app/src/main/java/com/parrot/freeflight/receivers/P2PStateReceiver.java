package com.parrot.freeflight.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;

import com.parrot.freeflight.activities.ControlDroneActivity;

/**
 * Created by yizheng on 7/27/17.
 */

public class P2PStateReceiver extends BroadcastReceiver {
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;
    private ControlDroneActivity mActivity;

    public P2PStateReceiver(WifiP2pManager.Channel channel, WifiP2pManager manager, ControlDroneActivity activity){
        mChannel=channel;
        mManager=mManager;
        mActivity=activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action=intent.getAction();
        switch(action){
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                int state=intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
                //WifiP2p is enabled
                if(state==WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                    mActivity.displayWifiStatus("WifiP2P is enabled.");
                }else{
                    mActivity.displayWifiStatus("WifiP2P is disabled");
                }
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                if (mManager != null) {
                    mManager.requestPeers(mChannel, (WifiP2pManager.PeerListListener)mActivity);
                }
                break;

            case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                break;


        }

    }
}
