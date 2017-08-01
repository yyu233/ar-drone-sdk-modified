/*
 * HudViewController
 *
 *  Created on: July 5, 2011
 *      Author: Dmytro Baryskyy
 */

package com.parrot.freeflight.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.parrot.freeflight.MapStageRenderer;
import com.parrot.freeflight.OnRotateGestureListener;
import com.parrot.freeflight.R;
import com.parrot.freeflight.RotateGestureDetector;
import com.parrot.freeflight.drone.NavData;
import com.parrot.freeflight.gestures.EnhancedGestureDetector;
import com.parrot.freeflight.ui.hud.Button;
import com.parrot.freeflight.ui.hud.Image;
import com.parrot.freeflight.ui.hud.Image.SizeParams;
import com.parrot.freeflight.ui.hud.Indicator;
import com.parrot.freeflight.ui.hud.JoystickBase;
import com.parrot.freeflight.ui.hud.Sprite;
import com.parrot.freeflight.ui.hud.Sprite.Align;
import com.parrot.freeflight.ui.hud.Text;
import com.parrot.freeflight.ui.hud.ToggleButton;
import com.parrot.freeflight.utils.FontUtils.TYPEFACE;
import com.parrot.freeflight.video.VideoStageView;

public class HudViewController 
	implements OnTouchListener,
			   OnGestureListener,
		OnRotateGestureListener

{


	public enum JoystickType {
		NONE,
		ANALOGUE,
		ACCELERO,
		COMBINED,
		MAGNETO
	}

	private static final String TAG = "HudViewController";

	private static final int JOY_ID_LEFT = 1;
	private static final int JOY_ID_RIGHT = 2;
	private static final int ALERT_ID = 3;
	private static final int TAKE_OFF_ID = 4;
	private static final int TOP_BAR_ID = 5;
	private static final int BOTTOM_BAR_ID = 6;
	private static final int CAMERA_ID = 7;
	private static final int RECORD_ID = 8;
	private static final int PHOTO_ID = 9;
	private static final int SETTINGS_ID = 10;
	private static final int BATTERY_INDICATOR_ID = 11;
	private static final int WIFI_INDICATOR_ID = 12;
	private static final int EMERGENCY_LABEL_ID = 13;
	private static final int BATTERY_STATUS_LABEL_ID = 14;
	private static final int RECORD_LABEL_ID = 15;
	private static final int USB_INDICATOR_ID = 16;
	private static final int USB_INDICATOR_TEXT_ID = 17;
	private static final int BACK_BTN_ID = 18;
	private static final int LAND_ID = 19;
    //Map
    //private static final int MAP_ID=20;
	private static final int DRONE_ID=20;
	private static final int GO_ID=21;
    private static final int STOP_ID=22;
    public static final int DRONE_DST_ID=23;

	public static final int MAP_ID=24;

	private Image bottomBarBg;
	
	private Button btnSettings;
	private Button btnTakeOff;
	private Button btnLand;
	private Button btnEmergency;
	private Button btnCameraSwitch;
	private Button btnPhoto;
	private Button btnBack;
	private ToggleButton btnRecord;
	
	private Button[] buttons;
	
	private Indicator batteryIndicator;
	private Indicator wifiIndicator;
	private Image  usbIndicator;
//	private TextView txtVideoFps;
	private TextView txtSceneFps;
	
	private Text txtBatteryStatus;
	private Text txtAlert;
	private Text txtRecord;
	private Text txtUsbRemaining;
	
	private GLSurfaceView glView;
	private VideoStageView canvasView;
	
	private JoystickBase[] joysticks;
	private float joypadOpacity;
	private GestureDetector gestureDetector;
	private ScaleGestureDetector sGestureDetector;
	private RotateGestureDetector rGestureDetector;
	//private VideoStageRenderer renderer;
	private Activity context;
	
	private boolean useSoftwareRendering;
	private int prevRemainingTime;

	private SparseIntArray emergencyStringMap;

	//private Button btnMAP;
	private Button btnGO;
    private Button btnSTOP;
	private Image drone;
	private Image engr;
	private MapStageRenderer renderer;
	//private MapStageView canvasView;


	public static int SCREEN_WIDTH;
	public static int SCREEN_HEIGHT;
	private static final float MAX_ZOOM=5.0f;
	private static final float MIN_ZOOM=1.0f;
	private float curr_zoom;
	private float scaleCentreX;
	private float scaleCentreY;
	private Rect scrollBounds;
	private int mapCentreX;
	private int mapCentreY;
	private static final float SCROLL_THRESHOLD =10;

	public HudViewController(Activity context, boolean useSoftwareRendering)
	{

		SCREEN_WIDTH=context.getResources().getDisplayMetrics().widthPixels;
		SCREEN_HEIGHT=context.getResources().getDisplayMetrics().heightPixels;

		curr_zoom=1.0f;
		scrollBounds=new Rect(-SCREEN_WIDTH+100, -SCREEN_HEIGHT+200, SCREEN_WIDTH-100, SCREEN_HEIGHT-200);

		Log.d(TAG, "Screen width: "+ SCREEN_WIDTH + " Height: " +SCREEN_HEIGHT);

		joypadOpacity = 1f;
		this.context = context;
		this.useSoftwareRendering = useSoftwareRendering;
		gestureDetector = new EnhancedGestureDetector(context, this);
		sGestureDetector=new ScaleGestureDetector(context, new ScaleListener());
		rGestureDetector=new RotateGestureDetector(this);
		canvasView = null;
		joysticks = new JoystickBase[2];

		
		glView = new GLSurfaceView(context);
		glView.setEGLContextClientVersion(2);
		
		context.setContentView(glView);
		
		//renderer = new VideoStageRenderer(context, null);
		renderer=new MapStageRenderer(context,null);
		
		if (useSoftwareRendering){
			// Replacing OpneGl based view with Canvas based one
//			RelativeLayout root = (RelativeLayout) context.findViewById(R.id.controllerRootLayout);
//			root.removeView(glView);
			glView = null;
			
			canvasView = new VideoStageView(context);
			//canvasView=new MapStageView(context);
			canvasView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//			root.addView(canvasView, 0);
		}
		

		initNavdataStrings();
		//initCanvasSurfaceView();
		initGLSurfaceView();

		Resources res = context.getResources();

		btnSettings = new Button(res, R.drawable.btn_settings, R.drawable.btn_settings_pressed, Align.TOP_LEFT);
		btnSettings.setMargin(0, 0, 0, (int)res.getDimension(R.dimen.hud_btn_settings_margin_left));
		
		btnBack = new Button(res, R.drawable.btn_back, R.drawable.btn_back_pressed, Align.TOP_LEFT);
		btnBack.setMargin(0, 0, 0, res.getDimensionPixelOffset(R.dimen.hud_btn_back_margin_left));
		
		btnEmergency = new Button(res, R.drawable.btn_emergency_normal, R.drawable.btn_emergency_pressed, Align.TOP_CENTER);
		btnTakeOff = new Button(res, R.drawable.btn_take_off_normal, R.drawable.btn_take_off_pressed, Align.BOTTOM_CENTER);		
		btnLand = new Button(res, R.drawable.btn_landing, R.drawable.btn_landing_pressed, Align.BOTTOM_CENTER);      
		btnLand.setVisible(false);
		
		Image topBarBg = new Image(res, R.drawable.barre_haut, Align.TOP_CENTER);
		topBarBg.setSizeParams(SizeParams.FILL_SCREEN, SizeParams.NONE);
		topBarBg.setAlphaEnabled(false);
		
		bottomBarBg = new Image(res, R.drawable.barre_bas, Align.BOTTOM_CENTER);
		bottomBarBg.setSizeParams(SizeParams.FILL_SCREEN, SizeParams.NONE);
		bottomBarBg.setAlphaEnabled(false);
		

	    btnPhoto = new Button(res, R.drawable.btn_photo, R.drawable.btn_photo_pressed, Align.TOP_RIGHT);
		btnRecord = new ToggleButton(res, R.drawable.btn_record, R.drawable.btn_record_pressed, 
		                                    R.drawable.btn_record1, R.drawable.btn_record1_pressed,
		                                    R.drawable.btn_record2, Align.TOP_RIGHT);
		btnRecord.setMargin(0, res.getDimensionPixelOffset(R.dimen.hud_btn_rec_margin_right), 0, 0);
		
		txtRecord = new Text(context, "REC", Align.TOP_RIGHT);
		txtRecord.setMargin((int)res.getDimension(R.dimen.hud_rec_text_margin_top), (int)res.getDimension(R.dimen.hud_rec_text_margin_right), 0, 0);
		txtRecord.setTextColor(Color.WHITE);
		txtRecord.setTypeface(TYPEFACE.Helvetica(context));
		txtRecord.setTextSize(res.getDimensionPixelSize(R.dimen.hud_rec_text_size));
		
		usbIndicator = new Image(res, R.drawable.picto_usb_actif, Align.TOP_RIGHT);
		usbIndicator.setMargin(0, res.getDimensionPixelOffset(R.dimen.hud_usb_indicator_margin_right), 0, 0);
		
		prevRemainingTime = -1;
		txtUsbRemaining = new Text(context, "KO", Align.TOP_RIGHT);
		txtUsbRemaining.setMargin(res.getDimensionPixelOffset(R.dimen.hud_usb_indicator_text_margin_top), 
									res.getDimensionPixelOffset(R.dimen.hud_usb_indicator_text_margin_right), 0, 0);
		txtUsbRemaining.setTypeface(TYPEFACE.Helvetica(context));
		txtUsbRemaining.setTextSize(res.getDimensionPixelSize(R.dimen.hud_usb_indicator_text_size));
		
		btnCameraSwitch = new Button(res, R.drawable.btn_camera, R.drawable.btn_camera_pressed, Align.TOP_RIGHT);
		btnCameraSwitch.setMargin(0, res.getDimensionPixelOffset(R.dimen.hud_btn_camera_switch_margin_right), 0, 0);
		
		int batteryIndicatorRes[] = {R.drawable.btn_battery_0,
									R.drawable.btn_battery_1,
									R.drawable.btn_battery_2,
									R.drawable.btn_battery_3
		};
		
		batteryIndicator = new Indicator(res, batteryIndicatorRes, Align.TOP_LEFT);
		batteryIndicator.setMargin(0, 0, 0, (int)res.getDimension(R.dimen.hud_battery_indicator_margin_left));
		
		txtBatteryStatus = new Text(context, "0%", Align.TOP_LEFT);
		txtBatteryStatus.setMargin((int)res.getDimension(R.dimen.hud_battery_text_margin_top),0,0, 			
									(int)res.getDimension(R.dimen.hud_battery_indicator_margin_left) + batteryIndicator.getWidth());
		txtBatteryStatus.setTextColor(Color.WHITE);
		txtBatteryStatus.setTypeface(TYPEFACE.Helvetica(context));
		txtBatteryStatus.setTextSize((int)res.getDimension(R.dimen.hud_battery_text_size));
		

		int wifiIndicatorRes[] = {
			R.drawable.btn_wifi_0,
			R.drawable.btn_wifi_1,
			R.drawable.btn_wifi_2,
			R.drawable.btn_wifi_3
		};
		
		wifiIndicator = new Indicator(res, wifiIndicatorRes, Align.TOP_LEFT);
		wifiIndicator.setMargin(0, 0, 0, (int)res.getDimension(R.dimen.hud_wifi_indicator_margin_left));
        //Map button
		//btnMAP=new Button(res, R.drawable.btn_map_normal, R.drawable.btn_map_pressed,Align.BOTTOM_RIGHT);

		drone=new Image(res,R.drawable.btn_map_normal, Align.CENTER);

		btnGO=new Button(res, R.drawable.btn_go_normal,R.drawable.btn_go_pressed,Align.BOTTOM_RIGHT);
        btnSTOP=new Button(res, R.drawable.btn_stop_normal, R.drawable.btn_stop_pressed, Align.BOTTOM_RIGHT);
		btnGO.setEnabled(false);
		btnSTOP.setVisible(false);
		btnSTOP.setEnabled(false);


		engr=new Image(res, R.drawable.engr_003,Align.CENTER);


		buttons = new Button[10];
		buttons[0] = btnSettings;
		buttons[1] = btnEmergency;
		buttons[2] = btnTakeOff;
		buttons[3] = btnLand;
		buttons[4] = btnPhoto;
		buttons[5] = btnRecord;
		buttons[6] = btnCameraSwitch;
		buttons[7] = btnBack;
        buttons[8] = btnGO;
        buttons[9] = btnSTOP;
		
		txtAlert = new Text(context, "", Align.TOP_CENTER);
		txtAlert.setMargin((int)res.getDimension(R.dimen.hud_alert_text_margin_top), 0, 0, 0);
		txtAlert.setTextColor(Color.RED);
		txtAlert.setTextSize((int)res.getDimension(R.dimen.hud_alert_text_size));
		txtAlert.setBold(true);
		txtAlert.blink(true);

		renderer.addSprite(MAP_ID, engr);
		renderer.addSprite(TOP_BAR_ID, topBarBg);
		renderer.addSprite(BOTTOM_BAR_ID, bottomBarBg);
		renderer.addSprite(SETTINGS_ID, btnSettings);
		renderer.addSprite(BACK_BTN_ID, btnBack);
		renderer.addSprite(PHOTO_ID, btnPhoto);
		renderer.addSprite(RECORD_ID, btnRecord);
		renderer.addSprite(CAMERA_ID, btnCameraSwitch);
		renderer.addSprite(ALERT_ID, btnEmergency);
		renderer.addSprite(TAKE_OFF_ID, btnTakeOff);
		renderer.addSprite(LAND_ID, btnLand);
		renderer.addSprite(BATTERY_INDICATOR_ID, batteryIndicator);
		renderer.addSprite(WIFI_INDICATOR_ID, wifiIndicator);
		renderer.addSprite(EMERGENCY_LABEL_ID, txtAlert);
		renderer.addSprite(BATTERY_STATUS_LABEL_ID, txtBatteryStatus);
		renderer.addSprite(RECORD_LABEL_ID, txtRecord);
		renderer.addSprite(USB_INDICATOR_ID, usbIndicator);
		renderer.addSprite(USB_INDICATOR_TEXT_ID, txtUsbRemaining);
        //MAP
        //renderer.addSprite(MAP_ID,btnMAP);
		renderer.addSprite(DRONE_ID,drone);
        renderer.addSprite(GO_ID,btnGO);
        renderer.addSprite(STOP_ID,btnSTOP);


	}

	/*public VideoStageRenderer getRenderer(){
		return renderer;
	}*/

	public MapStageRenderer getRenderer(){
		return renderer;
	}
    public void createDroneDst(float tap_x, float tap_y, Resources res){
        Image drone_dst=new Image(res, R.drawable.btn_map_pressed, Sprite.Align.NO_ALIGN);

        //set the center of destination marker as the tap location
        drone_dst.setPositionTo((int)(tap_x-(drone_dst.getWidth()/2)),(int)(tap_y-(drone_dst.getHeight()/2)));

        this.renderer.addSprite(DRONE_DST_ID,drone_dst);
    }

    public Image getDrone(){return drone;}

	public Button getBtnGO(){return btnGO;}

	public Button getBtnSTOP(){return btnSTOP;}
	
	private void initNavdataStrings()
	{
		emergencyStringMap = new SparseIntArray(17);
		
		emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_CUTOUT,       R.string.CUT_OUT_EMERGENCY);
		emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_MOTORS,       R.string.MOTORS_EMERGENCY);
		emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_CAMERA,       R.string.CAMERA_EMERGENCY);
		emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_PIC_WATCHDOG, R.string.PIC_WATCHDOG_EMERGENCY);
		emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_PIC_VERSION,  R.string.PIC_VERSION_EMERGENCY);
		emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_ANGLE_OUT_OF_RANGE,  R.string.TOO_MUCH_ANGLE_EMERGENCY);
		emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_VBAT_LOW,     R.string.BATTERY_LOW_EMERGENCY);
		emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_USER_EL,      R.string.USER_EMERGENCY);
		emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_ULTRASOUND,   R.string.ULTRASOUND_EMERGENCY);
		emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_UNKNOWN,      R.string.UNKNOWN_EMERGENCY);
		emergencyStringMap.put(NavData.ERROR_STATE_NAVDATA_CONNECTION,     R.string.CONTROL_LINK_NOT_AVAILABLE);
		emergencyStringMap.put(NavData.ERROR_STATE_START_NOT_RECEIVED,     R.string.START_NOT_RECEIVED);
		emergencyStringMap.put(NavData.ERROR_STATE_ALERT_CAMERA,           R.string.VIDEO_CONNECTION_ALERT);
		emergencyStringMap.put(NavData.ERROR_STATE_ALERT_VBAT_LOW,         R.string.BATTERY_LOW_ALERT);
		emergencyStringMap.put(NavData.ERROR_STATE_ALERT_ULTRASOUND,       R.string.ULTRASOUND_ALERT);
		emergencyStringMap.put(NavData.ERROR_STATE_ALERT_VISION, 		   R.string.VISION_ALERT);
		emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_UNKNOWN,      R.string.UNKNOWN_EMERGENCY);
	}
	
	/*private void initCanvasSurfaceView()
	{
		if (canvasView != null) {
			canvasView.setRenderer(renderer);
			canvasView.setOnTouchListener(this);
		}
	}*/
	
	
	private void initGLSurfaceView() {
		if (glView != null) {
			glView.setRenderer(renderer);
			glView.setOnTouchListener(this);
		}
	}
	
	
	public void setJoysticks(JoystickBase left, JoystickBase right)
	{
		joysticks[0] = left;
		if (left != null)   {
		    joysticks[0].setAlign(Align.BOTTOM_LEFT);
		    joysticks[0].setAlpha(joypadOpacity);
		}
		joysticks[1] = right;
		if (right != null)	{
		    joysticks[1].setAlign(Align.BOTTOM_RIGHT);
		    joysticks[1].setAlpha(joypadOpacity);
		}
	
		for (int i=0; i<joysticks.length; ++i) {
		    JoystickBase joystick = joysticks[i];
		    
			if (joystick != null) {
				if (!useSoftwareRendering) {
					joystick.setInverseYWhenDraw(true);
				} else {
					joystick.setInverseYWhenDraw(false);
				}
				
				int margin = context.getResources().getDimensionPixelSize(R.dimen.hud_joy_margin);
				
				joystick.setMargin(0, margin, bottomBarBg.getHeight() + margin, margin);
			}
		}
		
		renderer.removeSprite(JOY_ID_LEFT);
		renderer.removeSprite(JOY_ID_RIGHT);

		if (left != null) {
			renderer.addSprite(JOY_ID_LEFT, left);
		}
		
		if (right != null) {
			renderer.addSprite(JOY_ID_RIGHT, right);
		}
	}
	
	
	public JoystickBase getJoystickLeft()
	{
	    return joysticks[0];
	}
	
	
	public JoystickBase getJoystickRight()
	{
	    return joysticks[1];
	}
	

	public void setInterfaceOpacity(float opacity)
	{
		if (opacity < 0 || opacity > 100.0f) {
			Log.w(TAG, "Can't set interface opacity. Invalid value: " + opacity);
			return;
		}
		
		joypadOpacity = opacity / 100f;
		
		Sprite joystick = renderer.getSprite(JOY_ID_LEFT);
		joystick.setAlpha(joypadOpacity);
		
		joystick = renderer.getSprite(JOY_ID_RIGHT);
		joystick.setAlpha(joypadOpacity);
	}
	
	
	public void setIsFlying(final boolean isFlying)
	{
		if (isFlying) {
			btnTakeOff.setVisible(false);
			btnLand.setVisible(true);

			btnGO.setEnabled(true);//only when take off

		} else {
	         btnTakeOff.setVisible(true);
	         btnLand.setVisible(false);

			btnGO.setVisible(true);
			btnGO.setEnabled(false);//go button is enabled only when drone takes off
			btnSTOP.setVisible(false);
			btnSTOP.setEnabled(false);

		}
	}

	
	public void setBatteryValue(final int percent)
	{
		if (percent > 100 || percent < 0) {
			Log.w(TAG, "Can't set battery value. Invalid value " + percent);
			return;
		}
				
		int imgNum = Math.round((float) percent / 100.0f * 3.0f);

		txtBatteryStatus.setText(percent + "%");
		
		if (imgNum < 0)
			imgNum = 0;
		
		if (imgNum > 3) 
			imgNum = 3;

		if (batteryIndicator != null) {
			batteryIndicator.setValue(imgNum);
		}
	}
	
	
	public void setWifiValue(final int theNum)
	{
		if (wifiIndicator != null) {
			wifiIndicator.setValue(theNum);
		}
	}
	
	
	public void setUsbRemainingTime(int seconds)
	{
		boolean needColor = false;		
		String remainingTime = null;
		
	    if (seconds != prevRemainingTime) {
	        if (3600 < seconds) {
	        	remainingTime = "> 1h";
	        } else if (2700 < seconds) {
	        	remainingTime = "45m";
	        } else if (1800 < seconds) {
	        	remainingTime = "30m";
	        } else if (900 < seconds) {
	        	remainingTime = "15m";
	        } else if (600 < seconds) {
	        	remainingTime = "10m";
	        } else if (300 < seconds) {
	        	remainingTime = "5m";
	        } else {
	            if (30 > seconds) {
	                needColor = true;
	            } // No else
	            
	            int remMin = seconds / 60;
	            int remSec = seconds % 60;
	            
	            if (0 == remSec && 0 == remMin) {
	               remainingTime = "FULL";
	            } else {
	            	remainingTime = "" + remMin + ":" + (remSec>=10?remSec:("0" + remSec));
	            }
	        }
	        
	        prevRemainingTime = seconds; 
            txtUsbRemaining.setText(remainingTime);
           
            if (needColor) {
            	txtUsbRemaining.setTextColor(0xffAA0000);
            } else {
            	txtUsbRemaining.setTextColor(Color.WHITE);
            }
	    }
	}
	
	
	public void setUsbIndicatorEnabled(boolean enabled)
	{
		if (enabled) {
			usbIndicator.setAlpha(1.0f);
			txtUsbRemaining.setAlpha(1.0f);
		} else {
			usbIndicator.setAlpha(0.0f);
			txtUsbRemaining.setAlpha(0.0f);
		}
	}
	
	
	public void setBackButtonVisible(boolean visible)
	{
		if (visible) {
			btnBack.setEnabled(true);			
			btnBack.setAlpha(1.0f);
		} else {
			btnBack.setEnabled(false);
			btnBack.setAlpha(0.0f);
		}
	}
	
	public void setSettingsButtonEnabled(boolean enabled)
	{
		btnSettings.setEnabled(enabled);
	}
	
	
	public void setSwitchCameraButtonEnabled(boolean enabled)
	{
		btnCameraSwitch.setEnabled(enabled);
	}
	
	
	public void setRecordButtonEnabled(boolean enabled)
	{
		btnRecord.setEnabled(enabled);
		txtRecord.setEnabled(enabled);
	}
	
	
	public void setCameraButtonEnabled(boolean enabled)
	{
		btnPhoto.setEnabled(enabled);
	}

	//MAP
   // public void setMapButtonEnabled(boolean enabled){btnMAP.setEnabled(enabled);}
    public void setGoButtonEnabled(boolean enabled){btnGO.setEnabled(enabled);}

	public void setGoButtonVisible(boolean visible){btnGO.setVisible(visible);}

	public void setStopButtonEnabled(boolean enabled){btnSTOP.setEnabled(enabled);}

	public void setStopButtonVisible(boolean visible){btnSTOP.setVisible(visible);}
	
	public void setFpsVisible(final boolean visible)
	{
		Runnable runnable = new Runnable() {
			public void run() {
				if (visible) {
					//txtVideoFps.setVisibility(View.VISIBLE);
					txtSceneFps.setVisibility(View.VISIBLE);
				} else {
					//txtVideoFps.setVisibility(View.INVISIBLE);
					txtSceneFps.setVisibility(View.INVISIBLE);			
				}
			}
		};
		
		context.runOnUiThread(runnable);
	}
	
	
	public void setEmergency(final int code)
	{
		int res = emergencyStringMap.get(code);
		
		if (res != 0) {
			txtAlert.setText(context.getString(res));
			txtAlert.setVisibility(Text.VISIBLE);
			txtAlert.blink(true);
		} else {
			txtAlert.setVisibility(Text.INVISIBLE);
			txtAlert.blink(false);
		}
	}
	
	
	public void setRecording(boolean inProgress) 
	{
		btnRecord.setChecked(inProgress);

		if (txtRecord != null) {
			if (inProgress) {
				txtRecord.setTextColor(Color.RED);
			} else {
				txtRecord.setTextColor(Color.WHITE);
			}
		}
	}
	
	
	public void setBtnTakeOffClickListener(OnClickListener listener)
	{
		this.btnTakeOff.setOnClickListener(listener);
		this.btnLand.setOnClickListener(listener);
	}
	

	public void setBtnEmergencyClickListener(OnClickListener listener) 
	{
		this.btnEmergency.setOnClickListener(listener);		
	}
	
	
	public void setBtnPhotoClickListener(OnClickListener listener)
	{
		this.btnPhoto.setOnClickListener(listener);
	}
	
	
	public void setBtnRecordClickListener(OnClickListener listener)
	{
		this.btnRecord.setOnClickListener(listener);
	}

	
	public void setSettingsButtonClickListener(OnClickListener listener)
	{
		this.btnSettings.setOnClickListener(listener);
	}
	
	
	public void setBtnCameraSwitchClickListener(OnClickListener listener)
	{
		this.btnCameraSwitch.setOnClickListener(listener);
	}
	
	
	public void setDoubleTapClickListener(OnDoubleTapListener listener) 
	{
		gestureDetector.setOnDoubleTapListener(listener);	
	}
	//MAP
   /* public void setBtnMAPClickListener(OnClickListener listener)
    {
        this.btnMAP.setOnClickListener(listener);
    }*/
	public void setBtnGoClickListener(OnClickListener listener){
        this.btnGO.setOnClickListener(listener);
    }

    public void setBtnStopClickListener(OnClickListener listener){
		this.btnSTOP.setOnClickListener(listener);
	}
	public void onPause()
	{
		if (glView != null) {
			glView.onPause();
		}
		
		if (canvasView != null) {
			canvasView.onStop();
		}
	}
	
	
	public void onResume()
	{
		if (glView != null) {
			glView.onResume();
		}
		
		if (canvasView != null) {
			canvasView.onStart();
		}
	}


	public boolean onTouch(View v, MotionEvent event)
	{
		boolean result = false;
		
		for (int i=0; i<buttons.length; ++i) {
			if (buttons[i].processTouch(v, event)) {
				result = true;
				break;
			}
		}
		if(result != true){
			//rGestureDetector.onTouchEvent(event);
			sGestureDetector.onTouchEvent(event);
			if(!sGestureDetector.isInProgress()){
				gestureDetector.onTouchEvent(event);
			}
			result=true;
		}
		/*
		if (result != true) {	
			gestureDetector.onTouchEvent(event);
			
			for (int i=0; i<joysticks.length; ++i) {
				JoystickBase joy = joysticks[i];
				if (joy != null) {
					if (joy.processTouch(v, event)) {
						
						result = true;
					}
				}
			}
		}*/
		
			
//		if (event.getAction() == MotionEvent.ACTION_MOVE) {
//		    // Trying to avoid flooding of ACTION_MOVE events
//			try {
//				Thread.sleep(33);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		
		return result;
	}

	
	public void onDestroy()
	{
	    renderer.clearSprites();
		
		if (canvasView != null) {
			canvasView.onStop();
		}
	}


	public boolean onDown(MotionEvent e) 
	{
		return false;
	}


	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) 
	{
		Log.d(TAG,"velocityX: " +velocityX +" velocityY: " +velocityY);
		return false;
	}


	public void onLongPress(MotionEvent e) 
	{
    	// Left unimplemented	
	}


	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) 
	{
		if(Math.abs(distanceX)<SCROLL_THRESHOLD && Math.abs(distanceY)<SCROLL_THRESHOLD){
			return true;
		}
		Image map=(Image)renderer.getSprite(MAP_ID);
		Log.d(TAG,"Map RectLeft: "+map.getRectLeft() +" RectTop: "+ map.getRectTop());
		int nX=map.getRectLeft()-(int)distanceX;
		int nY= map.getRectTop()-(int)distanceY;
		if(nX<scrollBounds.left || nX>scrollBounds.right || nY <scrollBounds.top || nY >scrollBounds.bottom){
			//if the new position falls out the scrollBounds
			return true;
		}
		map.setPositionTo(nX,nY);
		return true;
	}


	public void onShowPress(MotionEvent e) 
	{
	    // Left unimplemented
	}


	public boolean onSingleTapUp(MotionEvent e) 
	{
		return false;
	}


	public void setBtnBackClickListener(OnClickListener listener) 
	{
		btnBack.setOnClickListener(listener);
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
		public boolean onScale (ScaleGestureDetector detector){
			Log.d(TAG, "onScale works");

			float scaleFactor = detector.getScaleFactor();
			float tempX=detector.getFocusX();
			float tempY=detector.getFocusY();

			scaleCentreX=(Math.abs(tempX-scaleCentreX)>50)?tempX :scaleCentreX;//change only if new focus point falls out of the range
			scaleCentreY=(Math.abs(tempY-scaleCentreY)>50)?tempY:scaleCentreY;
			Log.d(TAG,"centreX:"+scaleCentreX +" centreY: " +scaleCentreY);



			scaleFactor=Math.max(0.122f, Math.min(scaleFactor, 5.0f));//limit the scalefactor magnitude for a more smooth user experience
			curr_zoom *=scaleFactor;
			Log.d(TAG,"curr zoom: " +curr_zoom);
			Log.d(TAG,"ScaleFactor: "+scaleFactor);
			if(curr_zoom >=MIN_ZOOM && curr_zoom <=MAX_ZOOM){ //  zoom values ranged from 1.0 to 5.0

				renderer.zoom(scaleCentreX, scaleCentreY,curr_zoom);//not reach the limit, ask renderer to update

			}
			if(curr_zoom <MIN_ZOOM){
				curr_zoom=MIN_ZOOM;
			}
			if(curr_zoom>MAX_ZOOM){
				curr_zoom=MAX_ZOOM;
			}

			/*float scaleDis=detector.getCurrentSpan() -detector.getPreviousSpan();
			renderer.updatePMatrix(scaleDis);
			Log.d(TAG,"scale distance: "+scaleDis);*/
			return true;
		}
	}

	@Override
	public void onRotate(RotateGestureDetector rotateGestureDetector) {

		int tempX=engr.getCentre().x;
		int tempY=engr.getCentre().y;
		mapCentreX=(Math.abs(tempX-mapCentreX)>50)?tempX:mapCentreX;
		mapCentreY=(Math.abs(tempY-mapCentreY)>50)?tempY:mapCentreY;
		Point centre=new Point(mapCentreX,mapCentreY);
		renderer.rotate(centre,rotateGestureDetector.getrAngle());
	}
	public View getRootView()
	{
	    if (glView != null) {
	        return glView;
	    } else if (canvasView != null) {
	        return canvasView;
	    }
	    
	    Log.w(TAG, "Can't find root view");
	    return null;
	}


    public void setEmergencyButtonEnabled(boolean enabled)
    {
        btnEmergency.setEnabled(enabled);
    }


}

