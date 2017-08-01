/*
 * ControlDroneActivity
 * 
 * Created on: May 5, 2011
 * Author: Dmytro Baryskyy
 */

package com.parrot.freeflight.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.parrot.freeflight.FreeFlightApplication;
import com.parrot.freeflight.R;
import com.parrot.freeflight.activities.base.ParrotActivity;
import com.parrot.freeflight.drone.DroneConfig;
import com.parrot.freeflight.drone.DroneConfig.EDroneVersion;
import com.parrot.freeflight.drone.NavData;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiver;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiverDelegate;
import com.parrot.freeflight.receivers.DroneCameraReadyActionReceiverDelegate;
import com.parrot.freeflight.receivers.DroneCameraReadyChangeReceiver;
import com.parrot.freeflight.receivers.DroneEmergencyChangeReceiver;
import com.parrot.freeflight.receivers.DroneEmergencyChangeReceiverDelegate;
import com.parrot.freeflight.receivers.DroneFlyingStateReceiver;
import com.parrot.freeflight.receivers.DroneFlyingStateReceiverDelegate;
import com.parrot.freeflight.receivers.DroneRecordReadyActionReceiverDelegate;
import com.parrot.freeflight.receivers.DroneRecordReadyChangeReceiver;
import com.parrot.freeflight.receivers.DroneVideoRecordStateReceiverDelegate;
import com.parrot.freeflight.receivers.DroneVideoRecordingStateReceiver;
import com.parrot.freeflight.receivers.P2PStateReceiver;
import com.parrot.freeflight.receivers.WifiSignalStrengthChangedReceiver;
import com.parrot.freeflight.receivers.WifiSignalStrengthReceiverDelegate;
import com.parrot.freeflight.remotecontrollers.ButtonController;
import com.parrot.freeflight.remotecontrollers.ButtonDoubleClickController;
import com.parrot.freeflight.remotecontrollers.ButtonPressedController;
import com.parrot.freeflight.remotecontrollers.ButtonValueController;
import com.parrot.freeflight.remotecontrollers.ControlButtons;
import com.parrot.freeflight.remotecontrollers.ControlButtonsFactory;
import com.parrot.freeflight.sensors.DeviceOrientationChangeDelegate;
import com.parrot.freeflight.sensors.DeviceOrientationManager;
import com.parrot.freeflight.sensors.DeviceSensorManagerWrapper;
import com.parrot.freeflight.sensors.RemoteSensorManagerWrapper;
import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.service.VIONavDataService;
import com.parrot.freeflight.settings.ApplicationSettings;
import com.parrot.freeflight.settings.ApplicationSettings.ControlMode;
import com.parrot.freeflight.settings.ApplicationSettings.EAppSettingProperty;
import com.parrot.freeflight.transcodeservice.TranscodingService;
import com.parrot.freeflight.ui.HudViewController;
import com.parrot.freeflight.ui.HudViewController.JoystickType;
import com.parrot.freeflight.ui.SettingsDialogDelegate;
import com.parrot.freeflight.ui.hud.AcceleroJoystick;
import com.parrot.freeflight.ui.hud.AnalogueJoystick;
import com.parrot.freeflight.ui.hud.JoystickBase;
import com.parrot.freeflight.ui.hud.JoystickFactory;
import com.parrot.freeflight.ui.hud.JoystickListener;
import com.parrot.freeflight.utils.NookUtils;
import com.parrot.freeflight.utils.SystemUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static com.parrot.freeflight.ui.HudViewController.DRONE_DST_ID;

@SuppressLint("NewApi")
public class ControlDroneActivity
        extends ParrotActivity
        implements DeviceOrientationChangeDelegate, WifiSignalStrengthReceiverDelegate, DroneVideoRecordStateReceiverDelegate, DroneEmergencyChangeReceiverDelegate,
        DroneBatteryChangedReceiverDelegate, DroneFlyingStateReceiverDelegate, DroneCameraReadyActionReceiverDelegate, DroneRecordReadyActionReceiverDelegate, SettingsDialogDelegate

{
    private static final int LOW_DISK_SPACE_BYTES_LEFT = 1048576 * 20; //20 mebabytes
    private static final int WARNING_MESSAGE_DISMISS_TIME = 5000; // 5 seconds
    
    private static final String TAG = "ControlDroneActivity";
    private static final float ACCELERO_TRESHOLD = (float) Math.PI / 180.0f * 2.0f;

    private static final int PITCH = 1;
    private static final int ROLL = 2;


    private DroneControlService droneControlService;
    private ApplicationSettings settings;
    private SettingsDialog settingsDialog;

    private JoystickListener rollPitchListener;
    private JoystickListener gazYawListener;

    private HudViewController view;

    private boolean useSoftwareRendering;
   // private boolean forceCombinedControlMode;

    private int screenRotationIndex;

    private WifiSignalStrengthChangedReceiver wifiSignalReceiver;
    private DroneVideoRecordingStateReceiver videoRecordingStateReceiver;
    private DroneEmergencyChangeReceiver droneEmergencyReceiver;
    private DroneBatteryChangedReceiver droneBatteryReceiver;
    private DroneFlyingStateReceiver droneFlyingStateReceiver;
    private DroneCameraReadyChangeReceiver droneCameraReadyChangedReceiver;
    private DroneRecordReadyChangeReceiver droneRecordReadyChangeReceiver;

    private P2PStateReceiver p2pStateReceiver;

    private SoundPool soundPool;
    private int batterySoundId;
    private int effectsStreamId;

    private boolean combinedYawEnabled;
    private boolean acceleroEnabled;
    private boolean magnetoEnabled;
    private boolean magnetoAvailable;
    private boolean controlLinkAvailable;

    private boolean pauseVideoWhenOnSettings;

    private DeviceOrientationManager deviceOrientationManager;

    private float pitchBase;
    private float rollBase;
    private boolean running;

    private boolean flying;
    private boolean recording;
    private boolean cameraReady;
    private boolean prevRecording;
    private boolean rightJoyPressed;
    private boolean leftJoyPressed;
    private boolean isGoogleTV;

    private List<ButtonController> buttonControllers;

    private boolean dstTapped;

    private static float tap_x;
    private static float tap_y;
    private static float fly_disp;
    private static float diff_x;
    private static float diff_y;

    private Thread goThread;
    private Handler timer;
    private Runnable runnable;
    private volatile boolean reached;
    private volatile boolean interrupted;

    /*private final IntentFilter p2pIntentFilter=new IntentFilter();
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;
    private List<WifiP2pDevice> peers=new ArrayList<>();

    private WifiP2pManager.PeerListListener peerListListener;*/


    private ServiceConnection mConnection = new ServiceConnection()
    {

        public void onServiceConnected(ComponentName name, IBinder service)
        {
            droneControlService = ((DroneControlService.LocalBinder) service).getService();
            onDroneServiceConnected();
        }

        public void onServiceDisconnected(ComponentName name)
        {
            droneControlService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        if (isFinishing()) {
            return;
        }
        
        settings = getSettings();

        this.isGoogleTV = SystemUtils.isGoogleTV(this);

        // TODO google TV requires specific sensor manager and device rotation
        if (this.isGoogleTV) {
            this.applyHandDependendTVControllers();
            deviceOrientationManager = new DeviceOrientationManager(new RemoteSensorManagerWrapper(this), this);
        } else {
            screenRotationIndex = getWindow().getWindowManager().getDefaultDisplay().getRotation();
            deviceOrientationManager = new DeviceOrientationManager(new DeviceSensorManagerWrapper(this), this);
        }
        
        deviceOrientationManager.onCreate();

        bindService(new Intent(this, DroneControlService.class), mConnection, Context.BIND_AUTO_CREATE);

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            useSoftwareRendering = bundle.getBoolean("USE_SOFTWARE_RENDERING");
//            forceCombinedControlMode = bundle.getBoolean("FORCE_COMBINED_CONTROL_MODE");
        } else {
            useSoftwareRendering = false;
//            forceCombinedControlMode = false;
        }

        pauseVideoWhenOnSettings = getResources().getBoolean(R.bool.settings_pause_video_when_opened);

        combinedYawEnabled = true;
        acceleroEnabled = false;
        running =true;

       // initRegularJoystics();

        view = new HudViewController(this, useSoftwareRendering);

        wifiSignalReceiver = new WifiSignalStrengthChangedReceiver(this);
        videoRecordingStateReceiver = new DroneVideoRecordingStateReceiver(this);
        droneEmergencyReceiver = new DroneEmergencyChangeReceiver(this);
        droneBatteryReceiver = new DroneBatteryChangedReceiver(this);
        droneFlyingStateReceiver = new DroneFlyingStateReceiver(this);
        droneCameraReadyChangedReceiver = new DroneCameraReadyChangeReceiver(this);
        droneRecordReadyChangeReceiver = new DroneRecordReadyChangeReceiver(this);



        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        batterySoundId = soundPool.load(this, R.raw.battery, 1);

        if (!deviceOrientationManager.isAcceleroAvailable()) {
            settings.setControlMode(ControlMode.NORMAL_MODE);
        }
        
        settings.setFirstLaunch(false);
        
        view.setCameraButtonEnabled(false);
        view.setRecordButtonEnabled(false);


        dstTapped=false;

       // initP2PIntentFilter();
       // initP2PReceiver();



    }

    public void startVIONavDataService(){
        Intent mService= new Intent(ControlDroneActivity.this, VIONavDataService.class);
        ControlDroneActivity.this.startService(mService);
    }

    private void applyHandDependendTVControllers()
    {
        if (settings.isLeftHanded()) {
            screenRotationIndex = Surface.ROTATION_90;          
            initGoogleTVControllers(ControlButtonsFactory.getLeftHandedControls());
        } else {
            screenRotationIndex = Surface.ROTATION_270;  
            initGoogleTVControllers(ControlButtonsFactory.getRightHandedControls());
        }
    }

    private void initRegularJoystics()
    {
        rollPitchListener = new JoystickListener()
        {

            public void onChanged(JoystickBase joy, float x, float y)
            {
                if (droneControlService != null && acceleroEnabled == false && running == true) {
                    droneControlService.setRoll(x);
                    droneControlService.setPitch(-y);
                }
            }

            @Override
            public void onPressed(JoystickBase joy)
            {
                leftJoyPressed = true;

                if (droneControlService != null) {
                    droneControlService.setProgressiveCommandEnabled(true);

                    if (combinedYawEnabled && rightJoyPressed) {
                        droneControlService.setProgressiveCommandCombinedYawEnabled(true);
                    } else {
                        droneControlService.setProgressiveCommandCombinedYawEnabled(false);
                    }
                }

                running = true;
            }

            @Override
            public void onReleased(JoystickBase joy)
            {
                leftJoyPressed = false;

                if (droneControlService != null) {
                    droneControlService.setProgressiveCommandEnabled(false);

                    if (combinedYawEnabled) {
                        droneControlService.setProgressiveCommandCombinedYawEnabled(false);
                    }
                }

                running = false;
            }
        };

        gazYawListener = new JoystickListener()
        {

            public void onChanged(JoystickBase joy, float x, float y)
            {
                if (droneControlService != null) {
                    droneControlService.setGaz(y);
                    droneControlService.setYaw(x);
                }
            }

            @Override
            public void onPressed(JoystickBase joy)
            {
                rightJoyPressed = true;

                if (droneControlService != null) {
                    if (combinedYawEnabled && leftJoyPressed) {
                        droneControlService.setProgressiveCommandCombinedYawEnabled(true);
                    } else {
                        droneControlService.setProgressiveCommandCombinedYawEnabled(false);
                    }
                }
            }

            @Override
            public void onReleased(JoystickBase joy)
            {
                rightJoyPressed = false;

                if (droneControlService != null && combinedYawEnabled) {
                    droneControlService.setProgressiveCommandCombinedYawEnabled(false);
                }
            }
        };
    }

    private void initGoogleTVControllers(final ControlButtons buttons)
    {

        this.buttonControllers = new LinkedList<ButtonController>();
        this.buttonControllers.add(new ButtonValueController(buttons.getButtonCode(ControlButtons.BUTTON_UP), buttons.getButtonCode(ControlButtons.BUTTON_DOWN))
        {

            @Override
            public void onValueChanged(float theCurrentValue)
            {
                if (droneControlService != null) {
                    droneControlService.setGaz(theCurrentValue);
                }
            }
        });

        this.buttonControllers.add(new ButtonValueController(buttons.getButtonCode(ControlButtons.BUTTON_TURN_RIGHT), buttons.getButtonCode(ControlButtons.BUTTON_TURN_LEFT))
        {
            @Override
            public void onValueChanged(float theCurrentValue)
            {
                if (droneControlService != null) {
                    droneControlService.setYaw(theCurrentValue);
                }
            }
        });

        this.buttonControllers.add(new ButtonValueController(buttons.getButtonCode(ControlButtons.BUTTON_PITCH_LEFT), buttons.getButtonCode(ControlButtons.BUTTON_PITCH_RIGHT))
        {
            @Override
            public void onValueChanged(float theCurrentValue)
            {
                if (droneControlService != null && acceleroEnabled == false && running) {
                    droneControlService.setPitch(theCurrentValue);
                }
            }
        });

        this.buttonControllers.add(new ButtonValueController(buttons.getButtonCode(ControlButtons.BUTTON_ROLL_FORWARD), buttons.getButtonCode(ControlButtons.BUTTON_ROLL_BACKWARD))
        {
            @Override
            public void onValueChanged(float theCurrentValue)
            {
                if (droneControlService != null && acceleroEnabled == false && running) {
                    droneControlService.setPitch(-theCurrentValue);
                }
            }
        });

        this.buttonControllers.add(new ButtonPressedController(buttons.getButtonCode(ControlButtons.BUTTON_ACCELEROMETR))
        {

            @Override
            public void onButtonReleased()
            {
                leftJoyPressed = false;

                if (droneControlService != null) {
                    droneControlService.setProgressiveCommandEnabled(false);

                    if (combinedYawEnabled) {
                        droneControlService.setProgressiveCommandCombinedYawEnabled(false);
                    }
                }

                running = false;
            }

            @Override
            public void onButtonPressed()
            {
                leftJoyPressed = true;

                if (droneControlService != null) {
                    droneControlService.setProgressiveCommandEnabled(true);

                    if (combinedYawEnabled && rightJoyPressed) {
                        droneControlService.setProgressiveCommandCombinedYawEnabled(true);
                    } else {
                        droneControlService.setProgressiveCommandCombinedYawEnabled(false);
                    }
                }

                running = true;
            }
        });

        this.buttonControllers.add(new ButtonDoubleClickController(buttons.getButtonCode(ControlButtons.BUTTON_SALTO))
        {

            @Override
            public void onButtonDoubleClicked()
            {
                if (settings.isFlipEnabled()) {
                    droneControlService.doLeftFlip();
                }
            }
        });

        this.buttonControllers.add(new ButtonPressedController(buttons.getButtonCode(ControlButtons.BUTTON_TAKE_OFF))
        {

            @Override
            public void onButtonReleased()
            {
                droneControlService.triggerTakeOff();
            }

            @Override
            public void onButtonPressed()
            {
            }
        });

        this.buttonControllers.add(new ButtonPressedController(buttons.getButtonCode(ControlButtons.BUTTON_EMERGENCY))
        {

            @Override
            public void onButtonReleased()
            {
                droneControlService.triggerEmergency();
            }

            @Override
            public void onButtonPressed()
            {
            }
        });

        this.buttonControllers.add(new ButtonPressedController(buttons.getButtonCode(ControlButtons.BUTTON_CAMERA))
        {

            @Override
            public void onButtonReleased()
            {
                droneControlService.switchCamera();
            }

            @Override
            public void onButtonPressed()
            {
            }
        });

        this.buttonControllers.add(new ButtonPressedController(buttons.getButtonCode(ControlButtons.BUTTON_SETTINGS))
        {

            @Override
            public void onButtonReleased()
            {
                showSettingsDialog();
            }

            @Override
            public void onButtonPressed()
            {
            }
        });

        this.buttonControllers.add(new ButtonPressedController(buttons.getButtonCode(ControlButtons.BUTTON_PHOTO))
        {

            @Override
            public void onButtonReleased()
            {
                onTakePhoto();
            }

            @Override
            public void onButtonPressed()
            {
            }
        });

        this.buttonControllers.add(new ButtonPressedController(buttons.getButtonCode(ControlButtons.BUTTON_RECORD))
        {

            @Override
            public void onButtonReleased()
            {
                onRecord();
            }

            @Override
            public void onButtonPressed()
            {
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (applyKeyEvent(event)) {
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if (applyKeyEvent(event)) {
            return true;
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }

    private boolean applyKeyEvent(KeyEvent theEvent)
    {
        boolean result = false;
        if (this.buttonControllers != null) {
            for (ButtonController controller : this.buttonControllers) {
                if (controller.onKeyEvent(theEvent)) {
                    result = true;
                }
            }
        }
        return result;
    }

    private void initListeners()
    {
        view.setSettingsButtonClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                showSettingsDialog();
            }
        });

        view.setBtnCameraSwitchClickListener(new OnClickListener()
        {

            public void onClick(View v)
            {
                if (droneControlService != null) {
                    droneControlService.switchCamera();
                    Log.d(TAG,"Camera Switched");
                }
            }
        });

        view.setBtnTakeOffClickListener(new OnClickListener()
        {

            public void onClick(View v)
            {
                if (droneControlService != null) {
                    droneControlService.triggerTakeOff();
                }
            }
        });

        view.setBtnEmergencyClickListener(new OnClickListener()
        {

            public void onClick(View v)
            {
                if (droneControlService != null) {
                    droneControlService.triggerEmergency();
                }
            }

        });

        view.setBtnPhotoClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                if (droneControlService != null) {
                    onTakePhoto();
                }
            }
        });

        view.setBtnRecordClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                onRecord();
            }
        });

        view.setBtnBackClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                finish();
            }
        });

        view.setDoubleTapClickListener(new OnDoubleTapListener()
        {

            public boolean onSingleTapConfirmed(MotionEvent e)
            {   //Either Go or Stop button is clickable
                Log.d(TAG, "SingleTap");
                if(droneControlService!=null && (view.getBtnGO().isEnabled() ||view.getBtnSTOP().isEnabled()))
                {

                  if(dstTapped){//user has previously placed any destination marker
                       view.getRenderer().removeSprite(DRONE_DST_ID);//remove the old destination marker
                   }
                    //find the tap location on the screen.
                   tap_x= e.getRawX();
                    tap_y=e.getY();
                    Log.d(TAG, "tap_x: " +tap_x);
                    Log.d(TAG, "tap_y: "+tap_y);
                    //draw destinatin marker
                    view.createDroneDst(tap_x,tap_y,getResources());

                    dstTapped=true;

                    //locate the central coordiante of the screen (which is also the centre of starting marker)
                    float centre_x=(float)(getResources().getDisplayMetrics().widthPixels)/2.0f;
                    float centre_y=(float)(getResources().getDisplayMetrics().heightPixels)/2.0f;

                    //difference between tap_x and centre_x, relative to centre_x
                    diff_x=tap_x-centre_x;
                    diff_y=tap_y-centre_y; //from top to bottom, y decreases, negative diff_y means to fly backwards

                    Log.d(TAG, "diff_x: "+diff_x);
                    Log.d(TAG, "diff_y: "+diff_y);
                    //displacement frome centre of screen to the tap location
                    fly_disp= (float)(Math.sqrt(Math.pow((double)diff_x,2) +Math.pow((double)diff_y,2)));

                    return true;
                }
                return false;
            }

            public boolean onDoubleTapEvent(MotionEvent e)
            {
                // Left unimplemented
                return false;
            }

            public boolean onDoubleTap(MotionEvent e)
            {
                if (settings.isFlipEnabled() && droneControlService != null) {
                    droneControlService.doLeftFlip();
                    return true;
                }

                return false;
            }
        });


         view.setBtnGoClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if(droneControlService!=null && dstTapped && view.getBtnGO().isEnabled()) {//GO button is only enabled when drone takes off

                    view.setGoButtonEnabled(false); //disable and hide go button
                    view.setGoButtonVisible(false);
                    view.setStopButtonEnabled(true);//enable and show stop button
                    view.setStopButtonVisible(true);
                    //determine
                    goThread = new Thread(new Runnable() {
                        @Override

                        public void run() {

                            while (!reached) {
                                Log.d(TAG,"Flying Thread Entered");
                                Log.d(TAG,"Roll value: "+diff_x / Math.abs(diff_x) );
                                Log.d(TAG,"Pitch value: "+diff_y / Math.abs(diff_y));

                                if(Thread.interrupted()){//Stop button is
                                    interrupted=true;
                                    break;
                                }
                                droneControlService.setProgressiveCommandEnabled(true);
                                droneControlService.setProgressiveCommandCombinedYawEnabled(true);
                                droneControlService.setRoll(diff_x / Math.abs(diff_x));//range from -1.0 to 1.0, I intentionally gives the maximum which is 1.0
                                droneControlService.setPitch(diff_y / Math.abs(diff_y));


                            }
                            Log.d(TAG,"Flying Thread END");
                            droneControlService.setProgressiveCommandEnabled(false);
                            droneControlService.setProgressiveCommandCombinedYawEnabled(false);

                            if(reached){//reached destination, Go button should be enabled to allow drone to start a new flight

                                view.setGoButtonEnabled(true); //enable and show go button
                                view.setGoButtonVisible(true);
                                view.setStopButtonEnabled(false);//disable and hide stop button
                                view.setStopButtonVisible(false);

                            }
                            reached=false;
                        }
                    });

                    goThread.start();

                    //*******Test for 3 seconds******
                    // Is not finalized.
                    timer = new Handler();

                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Timer works");
                            if(!interrupted){
                                reached=true;
                            }
                        }
                    };

                    timer.postDelayed(runnable, 1000);


                }
            }
        });

        view.setBtnStopClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

             view.setGoButtonEnabled(true); //enable and show go button
                view.setGoButtonVisible(true);
                view.setStopButtonEnabled(false);//disable and hide stop button
                view.setStopButtonVisible(false);

                //we are sure at this point that goThread must have been created. Then stop goThread
                goThread.interrupt();


            }
        });


    }

    
    private void initVirtualJoysticks(JoystickType leftType, JoystickType rightType, boolean isLeftHanded)
    {
        JoystickBase joystickLeft = (!isLeftHanded ? view.getJoystickLeft() : view.getJoystickRight());
        JoystickBase joystickRight = (!isLeftHanded ? view.getJoystickRight() : view.getJoystickLeft());

        ApplicationSettings settings = getSettings();

        if (leftType == JoystickType.ANALOGUE) {
            if (joystickLeft == null || !(joystickLeft instanceof AnalogueJoystick) || joystickLeft.isAbsoluteControl() != settings.isAbsoluteControlEnabled()) {
                joystickLeft = JoystickFactory.createAnalogueJoystick(this, settings.isAbsoluteControlEnabled(), rollPitchListener);
            } else {
                joystickLeft.setOnAnalogueChangedListener(rollPitchListener);
                joystickRight.setAbsolute(settings.isAbsoluteControlEnabled());
            }
        } else if (leftType == JoystickType.ACCELERO) {
            if (joystickLeft == null || !(joystickLeft instanceof AcceleroJoystick) || joystickLeft.isAbsoluteControl() != settings.isAbsoluteControlEnabled()) {
                joystickLeft = JoystickFactory.createAcceleroJoystick(this, settings.isAbsoluteControlEnabled(), rollPitchListener);
            } else {
                joystickLeft.setOnAnalogueChangedListener(rollPitchListener);
                joystickRight.setAbsolute(settings.isAbsoluteControlEnabled());
            }
        }

        if (rightType == JoystickType.ANALOGUE) {
            if (joystickRight == null || !(joystickRight instanceof AnalogueJoystick) || joystickRight.isAbsoluteControl() != settings.isAbsoluteControlEnabled()) {
                joystickRight = JoystickFactory.createAnalogueJoystick(this, false, gazYawListener);
            } else {
                joystickRight.setOnAnalogueChangedListener(gazYawListener);
                joystickRight.setAbsolute(false);
            }
        } else if (rightType == JoystickType.ACCELERO) {
            if (joystickRight == null || !(joystickRight instanceof AcceleroJoystick) || joystickRight.isAbsoluteControl() != settings.isAbsoluteControlEnabled()) {
                joystickRight = JoystickFactory.createAcceleroJoystick(this, false, gazYawListener);
            } else {
                joystickRight.setOnAnalogueChangedListener(gazYawListener);
                joystickRight.setAbsolute(false);
            }
        }

        if (!isLeftHanded) {
            view.setJoysticks(joystickLeft, joystickRight);
        } else {
            view.setJoysticks(joystickRight, joystickLeft);
        }
    }

    @Override
    protected void onDestroy()
    {
        if (view != null) {
            view.onDestroy();
        }

        this.deviceOrientationManager.destroy();

        soundPool.release();
        soundPool = null;

        unbindService(mConnection);

        super.onDestroy();
        Log.d(TAG, "ControlDroneActivity destroyed");
        System.gc();
    }


   /* public void initP2PReceiver(){
        mManager=(WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel=mManager.initialize(this, getMainLooper(),null);
        p2pStateReceiver=new P2PStateReceiver(mChannel,mManager,this);
    }

    public void initP2PIntentFilter(){

        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList npeers) {
        List<WifiP2pDevice> rPeers= (List<WifiP2pDevice>) npeers.getDeviceList();
        if(!rPeers.equals(peers)){
            peers.clear();
            peers.addAll(rPeers);
        }

        if(peers.size()==0){
            Log.d(TAG,"No peer found");
        }else{
            p2pConnect();
        }
    }*/
    private void registerReceivers()
    {
        // System wide receiver
        registerReceiver(wifiSignalReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
      //  registerReceiver(p2pStateReceiver, p2pIntentFilter);

        // Local receivers
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(videoRecordingStateReceiver, new IntentFilter(DroneControlService.VIDEO_RECORDING_STATE_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneEmergencyReceiver, new IntentFilter(DroneControlService.DRONE_EMERGENCY_STATE_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneBatteryReceiver, new IntentFilter(DroneControlService.DRONE_BATTERY_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneFlyingStateReceiver, new IntentFilter(DroneControlService.DRONE_FLYING_STATE_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneCameraReadyChangedReceiver, new IntentFilter(DroneControlService.CAMERA_READY_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneRecordReadyChangeReceiver, new IntentFilter(DroneControlService.RECORD_READY_CHANGED_ACTION));

    }

    private void unregisterReceivers()
    {
        // Unregistering system receiver
        unregisterReceiver(wifiSignalReceiver);
       // unregisterReceiver(p2pStateReceiver);
        // Unregistering local receivers
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.unregisterReceiver(videoRecordingStateReceiver);
        localBroadcastMgr.unregisterReceiver(droneEmergencyReceiver);
        localBroadcastMgr.unregisterReceiver(droneBatteryReceiver);
        localBroadcastMgr.unregisterReceiver(droneFlyingStateReceiver);
        localBroadcastMgr.unregisterReceiver(droneCameraReadyChangedReceiver);
        localBroadcastMgr.unregisterReceiver(droneRecordReadyChangeReceiver);
    }

    public void displayWifiStatus(String status){
        Toast.makeText(this,status, Toast.LENGTH_SHORT).show();;
    }

   /* public void p2pConnect(){
        WifiP2pDevice device=peers.get(0);

        WifiP2pConfig config=new WifiP2pConfig();
        config.deviceAddress= device.deviceAddress;
        config.wps.setup= WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "P2P Connection failed");
            }
        });

    }

    public void discoverPeers(){
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"Peer discovery process starts successfully");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Peer discovery process failed to initiate");
            }
        });
    }*/
    @Override
    protected void onResume()
    {
        if (view != null) {
            view.onResume();
        }

        if (droneControlService != null) {
            droneControlService.resume();
        }

        registerReceivers();
        refreshWifiSignalStrength();

        // Start tracking device orientation
        deviceOrientationManager.resume();
        magnetoAvailable = deviceOrientationManager.isMagnetoAvailable();

       // discoverPeers();  *** WifiP2P and Wifi can't work simultaneously
        startVIONavDataService();
        super.onResume();


    }

    @Override
    protected void onPause()
    {
        if (view != null) {
            view.onPause();
        }

        if (droneControlService != null) {
            droneControlService.pause();
        }

        unregisterReceivers();

        // Stop tracking device orientation
        deviceOrientationManager.pause();

        stopEmergencySound();

        System.gc();
        super.onPause();
    }

    /**
     * Called when we connected to DroneControlService
     */
    protected void onDroneServiceConnected()
    {
        if (droneControlService != null) {
            droneControlService.resume();
            droneControlService.requestDroneStatus();
        } else {
            Log.w(TAG, "DroneServiceConnected event ignored as DroneControlService is null");
        }

        settingsDialog = new SettingsDialog(this, this, droneControlService, magnetoAvailable);

        applySettings(settings);

        initListeners();
        runTranscoding();
        
        if (droneControlService.getMediaDir() != null) {
            view.setRecordButtonEnabled(true);
            view.setCameraButtonEnabled(true);
        }
    }


    @Override
    public void onDroneFlyingStateChanged(boolean flying)
    {
        this.flying = flying;
        view.setIsFlying(flying);

        updateBackButtonState();
        Log.d(TAG,"onDroneFlyinState");
    }

    @SuppressLint("NewApi")
    public void onDroneRecordReadyChanged(boolean ready)
    {
        if (!recording) {
            view.setRecordButtonEnabled(ready);
        } else {
            view.setRecordButtonEnabled(true);
        }
    }


    protected void onNotifyLowDiskSpace()
    {
        showWarningDialog(getString(R.string.your_device_is_low_on_disk_space), WARNING_MESSAGE_DISMISS_TIME);
    }


    protected void onNotifyLowUsbSpace()
    {
        showWarningDialog(getString(R.string.USB_drive_full_Please_connect_a_new_one), WARNING_MESSAGE_DISMISS_TIME);
    }


    protected void onNotifyNoMediaStorageAvailable()
    {
        showWarningDialog(getString(R.string.Please_insert_a_SD_card_in_your_Smartphone), WARNING_MESSAGE_DISMISS_TIME);
    }


    public void onCameraReadyChanged(boolean ready)
    {
        view.setCameraButtonEnabled(ready);
        cameraReady = ready;
        
        updateBackButtonState();
    }
    

    public void onDroneEmergencyChanged(int code)
    {
        view.setEmergency(code);

        if (code == NavData.ERROR_STATE_EMERGENCY_VBAT_LOW || code == NavData.ERROR_STATE_ALERT_VBAT_LOW) {
            playEmergencySound();
        } else {
            stopEmergencySound();
        }

        controlLinkAvailable = (code != NavData.ERROR_STATE_NAVDATA_CONNECTION); 
        
        if (!controlLinkAvailable) {
            view.setRecordButtonEnabled(false);
            view.setCameraButtonEnabled(false);
            view.setSwitchCameraButtonEnabled(false);
        } else {
            view.setSwitchCameraButtonEnabled(true);
            view.setRecordButtonEnabled(true);
            view.setCameraButtonEnabled(true);
        }
        
        updateBackButtonState();
        
        view.setEmergencyButtonEnabled(!NavData.isEmergency(code));
    }
    
    public void onDroneBatteryChanged(int value)
    {
        view.setBatteryValue(value);
    }

    public void onWifiSignalStrengthChanged(int strength)
    {
        view.setWifiValue(strength);
    }


    public void onDroneRecordVideoStateChanged(boolean recording, boolean usbActive, int remaining)
    {
        if (droneControlService == null)
            return;
        
        prevRecording = this.recording;
        this.recording = recording;

        view.setRecording(recording);
        view.setUsbIndicatorEnabled(usbActive);
        view.setUsbRemainingTime(remaining);

        updateBackButtonState();

        if (!recording) {
            if (prevRecording != recording && droneControlService != null
                    && droneControlService.getDroneVersion() == EDroneVersion.DRONE_1) {
                runTranscoding();
                showWarningDialog(getString(R.string.Your_video_is_being_processed_Please_do_not_close_application), WARNING_MESSAGE_DISMISS_TIME);
            }
        }
        
        if (prevRecording != recording) {
            if (usbActive && droneControlService.getDroneConfig().isRecordOnUsb() && remaining == 0) {
                onNotifyLowUsbSpace();
            }
        }
    }

    protected void showSettingsDialog()
    {
        view.setSettingsButtonEnabled(false);
        
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("settings");

        if (prev != null) {
            return;
        }

        ft.addToBackStack(null);

        settingsDialog.show(ft, "settings");
        
        if (pauseVideoWhenOnSettings) {
            view.onPause();
        }
    }

    @Override
    public void onBackPressed()
    {
        if (canGoBack()) {
            super.onBackPressed();
        }
    }
    
    
    private boolean canGoBack()
    {
        return !((flying || recording || !cameraReady) && controlLinkAvailable);
    }
    
    
    private void applyJoypadConfig(ControlMode controlMode, boolean isLeftHanded)
    {
        switch (controlMode) {
        case NORMAL_MODE:
            initVirtualJoysticks(JoystickType.ANALOGUE, JoystickType.ANALOGUE, isLeftHanded);
            acceleroEnabled = false;
            break;
        case ACCELERO_MODE:
            initVirtualJoysticks(JoystickType.ACCELERO, JoystickType.ANALOGUE, isLeftHanded);
            acceleroEnabled = true;
            break;
        case ACE_MODE:
            initVirtualJoysticks(JoystickType.NONE, JoystickType.COMBINED, isLeftHanded);
            acceleroEnabled = true;
            break;
        }
    }
    
    
    private void applySettings(ApplicationSettings settings)
    {
        applySettings(settings, false);
    }

    private void applySettings(ApplicationSettings settings, boolean skipJoypadConfig)
    {
        magnetoEnabled = settings.isAbsoluteControlEnabled();
        Log.d(TAG, "magnetoEnabled "+magnetoEnabled);

        if (magnetoEnabled) {
            if (droneControlService.getDroneVersion() == EDroneVersion.DRONE_1 || !deviceOrientationManager.isMagnetoAvailable() || NookUtils.isNook()) {
                // Drone 1 doesn't have compass, so we need to switch magneto
                // off.
                magnetoEnabled = false;
                settings.setAbsoluteControlEnabled(false);
            }
        }

        if (droneControlService != null)
            droneControlService.setMagnetoEnabled(magnetoEnabled);

        if (!skipJoypadConfig) {
            applyJoypadConfig(settings.getControlMode(), settings.isLeftHanded());
        }

        // TODO we have to hide touch controllers for google TV
        view.setInterfaceOpacity(this.isGoogleTV ? 0 : settings.getInterfaceOpacity());

        view.setInterfaceOpacity(0);
    }

    private ApplicationSettings getSettings()
    {
        return ((FreeFlightApplication) getApplication()).getAppSettings();
    }

    public void refreshWifiSignalStrength()
    {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int signalStrength = WifiManager.calculateSignalLevel(manager.getConnectionInfo().getRssi(), 4);
        onWifiSignalStrengthChanged(signalStrength);
    }

    private void showWarningDialog(final String message, final int forTime)
    {
        final String tag = message;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(tag);
        
        if (prev != null) {
            return;
        }

        ft.addToBackStack(null);

        // Create and show the dialog.
        WarningDialog dialog = new WarningDialog();

        dialog.setMessage(message);
        dialog.setDismissAfter(forTime);
        dialog.show(ft, tag);
    }

    private void playEmergencySound()
    {
        if (effectsStreamId != 0) {
            soundPool.stop(effectsStreamId);
            effectsStreamId = 0;
        }

        effectsStreamId = soundPool.play(batterySoundId, 1, 1, 1, -1, 1);
    }

    
    private void stopEmergencySound()
    {
        soundPool.stop(effectsStreamId);
        effectsStreamId = 0;
    }

    
    private void updateBackButtonState()
    {
        if (canGoBack()) {
            view.setBackButtonVisible(true);
        } else {
            view.setBackButtonVisible(false);   
        }
    }
    
    
    private void runTranscoding()
    {
        if (droneControlService.getDroneVersion() == EDroneVersion.DRONE_1) {
        	File mediaDir = droneControlService.getMediaDir();
        	
        	if (mediaDir != null) {
	            Intent transcodeIntent = new Intent(this, TranscodingService.class);
	            transcodeIntent.putExtra(TranscodingService.EXTRA_MEDIA_PATH, mediaDir.toString());
	            startService(transcodeIntent);
        	} else {
        		Log.d(TAG, "Transcoding skipped SD card is missing.");
        	}
        }
    }

    public void onDeviceOrientationChanged(float[] orientation, float magneticHeading, int magnetoAccuracy)
    {
        if (droneControlService == null) {
            return;
        }

        if (magnetoEnabled && magnetoAvailable) {
            float heading = magneticHeading * 57.2957795f;

            if (screenRotationIndex == 1) {
                heading += 90.f;
            }

            droneControlService.setDeviceOrientation((int) heading, 0);
        } else {
            droneControlService.setDeviceOrientation(0, 0);
        }

        if (running == false) {
            pitchBase = orientation[PITCH];
            rollBase = orientation[ROLL];
            droneControlService.setPitch(0);
            droneControlService.setRoll(0);
        } else {

            float x = (orientation[PITCH] - pitchBase);
            float y = (orientation[ROLL] - rollBase);

            if (screenRotationIndex == 0) {
                // Xoom
                if (acceleroEnabled && (Math.abs(x) > ACCELERO_TRESHOLD || Math.abs(y) > ACCELERO_TRESHOLD)) {
                    x *= -1;
                    droneControlService.setPitch(x);
                    droneControlService.setRoll(y);

                }
            } else if (screenRotationIndex == 1) {
                if (acceleroEnabled && (Math.abs(x) > ACCELERO_TRESHOLD || Math.abs(y) > ACCELERO_TRESHOLD)) {
                    x *= -1;
                    y *= -1;

                    droneControlService.setPitch(y);
                    droneControlService.setRoll(x);

                }
            } else if (screenRotationIndex == 3) {
                // google tv
                if (acceleroEnabled && (Math.abs(x) > ACCELERO_TRESHOLD || Math.abs(y) > ACCELERO_TRESHOLD)) {

                    droneControlService.setPitch(y);
                    droneControlService.setRoll(x);

                }
            }
        }
    }

    public void prepareDialog(SettingsDialog dialog)
    {
        dialog.setAcceleroAvailable(deviceOrientationManager.isAcceleroAvailable());

        if (NookUtils.isNook()) {
            // System see the magnetometer but actually it is not functional. So
            // we need to disable magneto
            dialog.setMagnetoAvailable(false);
        } else {
            dialog.setMagnetoAvailable(deviceOrientationManager.isMagnetoAvailable());
        }

        dialog.setFlying(flying);
        dialog.setConnected(controlLinkAvailable);
        dialog.enableAvailableSettings();
    }

    
    @Override
    public void onOptionChangedApp(SettingsDialog dialog, EAppSettingProperty property, Object value)
    {
        if (value == null || value == null) {
            throw new IllegalArgumentException("Property can not be null");
        }
        
        ApplicationSettings appSettings = getSettings();
        
        switch (property) {
        case LEFT_HANDED_PROP:
        case MAGNETO_ENABLED_PROP:
        case CONTROL_MODE_PROP:
            applyJoypadConfig(appSettings.getControlMode(), appSettings.isLeftHanded());
            break;
        case INTERFACE_OPACITY_PROP:
            if (value instanceof Integer) {
                if (!isGoogleTV) {
                    view.setInterfaceOpacity(((Integer) value).floatValue());
                }
            }
            break;
        default:
            // Ignoring any other option change. They should be processed in onDismissed
            
        }
    }
    
    
    @Override
    public void onDismissed(SettingsDialog settingsDialog)
    {
        if (this.isGoogleTV) {
            this.applyHandDependendTVControllers();
        }
         	 
    	// pauseVideoWhenOnSettings option is not mandatory and is set depending to device in config.xml.
        if (pauseVideoWhenOnSettings) {
            view.onResume();
        }

        AsyncTask<Integer, Integer, Boolean> loadPropTask = new AsyncTask<Integer, Integer, Boolean>()
        {
            @Override
            protected Boolean doInBackground(Integer... params)
            {
                // Skipping joypad configuration as it was already done in onPropertyChanged
                // We do this because on some devices joysticks re-initialization takes too
                // much time.
                applySettings(getSettings(), true);
                return Boolean.TRUE;
            }

            @Override
            protected void onPostExecute(Boolean result)
            {
                view.setSettingsButtonEnabled(true);
            }
        };

        loadPropTask.execute();
    }
    

    private boolean isLowOnDiskSpace()
    {
        boolean lowOnSpace = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            DroneConfig config = droneControlService.getDroneConfig();
            if (!recording && !config.isRecordOnUsb()) {
                File mediaDir = droneControlService.getMediaDir();
                long freeSpace = 0;
                
                if (mediaDir != null) {
                    freeSpace = mediaDir.getUsableSpace();
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
                        && freeSpace < LOW_DISK_SPACE_BYTES_LEFT) {
                    lowOnSpace = true;
                }
            }
        } else {
            // TODO: Provide alternative implementation. Probably using StatFs
        }
        
        return lowOnSpace;
    }

    private void onRecord()
    {
        if (droneControlService != null) {
            DroneConfig droneConfig = droneControlService.getDroneConfig();
            
            boolean sdCardMounted = droneControlService.isMediaStorageAvailable();
            boolean recordingToUsb = droneConfig.isRecordOnUsb() && droneControlService.isUSBInserted();

            if (recording) {
                // Allow to stop recording
                view.setRecordButtonEnabled(false);
                droneControlService.record();
            } else {           
                // Start recording
                if (!sdCardMounted) {
                    if (recordingToUsb) {
                        view.setRecordButtonEnabled(false);
                        droneControlService.record();
                    } else {
                        onNotifyNoMediaStorageAvailable();
                    }
                } else {
                    if (!recordingToUsb && isLowOnDiskSpace()) {
                        onNotifyLowDiskSpace();
                    }
                    
                    view.setRecordButtonEnabled(false);
                    droneControlService.record();
                }      
            }
        }
    }

    protected void onTakePhoto()
    {
        if (droneControlService.isMediaStorageAvailable()) {
            view.setCameraButtonEnabled(false);
            droneControlService.takePhoto();
        } else {
           onNotifyNoMediaStorageAvailable();
        }
    }


}
