Modified based on:https://github.com/SUPENTA/ardrone-sdk-android

14/06
Comment out 186-189(DashboardActivityBase) and paste 146-149
In DashboardAcivity, add onStartGames()->start ConnectActivity 
applySettings(): add view.setInterfaceopacity(0).(ControlDroneActivity line 1044)
 Hide touch controller

15/06
Comment out initRegularJoystics() (ControlDroneActivity  line 229)
Comment out btnmap (HudViewController line227)
Add Image drone, DRONE_ID, add render.addsprite(DRONE_ID, drone)
Add layout: CENTER (Sprite line 94) 
Add btnGO, GO_ID, addsprite(GO_ID), setBtnGoClickListener

18/06
Change false to true (ControlDroneActivity.java : applySettings  (line1024)
Implement onSingleTapConfirmed (line 636)
Add createDroneDst method (HudViewControler.java) (line 281)
Add btnGo.setVisible (HudViewControlle.java) (line 401)
Add btnStop, STOP_ID, btnStop.setVisible, btnstop.setOnClickListener
Bounds (Sprite.java) by default using int coordinates. getRawX and getRawY use float. 
Which unit should we follow? 

20/06
Drone flying states are two states: takeoff or land.
Add setgobuttonenable, setstopbuttonenable, setgobuttonvisible, setstopbuttonvisible. Set both enable and visible of stop button to false in onCreate in controldroneacitvity. By default, enable and visible of a sprite are both true.
Add view.getbtnGO and view.getbtnStop

24/06
Testing
It doesnt allow me to either set the applysettings(settings, true) (line 1146) or comment out if(!skipJoypadCongif) (line 1165). I wanted to make these changes because I hoped doing so can deactivate the intiVitualJoyStick method. But the app will crash if i make these changes.

Comment out Joystick relevant parts in onTouch (HudViewController: line 702)
Editing threads inside Go and stop ontouch method. 

25/06
Add log statements for debugging
Change running to true (line 225) 
Successfully display drone marker and go button. 
Thread works fine, but it seems like drone does not fly like I expected. Must be something wrong. 

30/06
The Next Episode. Oh yeah! 
Create a MapStageRender. Delete everything related to GLBGVideoSprite. 
HudViewController:  comment out line 145
Comment out line 161
Add MAP_ID =24, add Image engr 
Add MapStageRender (line 146)

Comment out method initCanvasSurfaceView() (line 340)

04/07 
Please Test if onDraw works (VideoStageView.java)
Comment out line 139. Add MapStageRenderer 
Create MapStageView. Comment out line 148. Add MapStageView
Test: ConnectActivity:  line 157,  change false to true to force software rendering
 (Crashed…. ) 
MUST USE HARDWARE RENDERING. SOFTWARE RENDERING HAS PROBLEM !!!


06/07
Implement onScroll (HudViewController) 
Add getRectLeft and getRectTop in Sprite 
Add SCREEN_WIDTH and SCREEN_HEIGHT in GLSprite 
Test if Image size is larger than screen. If true, set the image size to be fit in the screen. 

07/07
Add GLES20. glClear and glClearColor at onDraw and onSurfaceCreated method (MapStageRenderer.java).Without them, dragging map shows flickering.
Draggin passed test. 
Change setPosition name to setPositionTo (image.java)
Add new setPosition method 

12/07
Add ScaleListener,  implement onScale callback function 
Add updatePMatrix (MapStageRenderer.java)
Add code to respond scale action in onDrawFrame ( MapStageRenderer.java)
Add MAX_ZOOM & MIN_ZOOM 

16/07
Add OnRotateGestureListener, RotateGestureDetector 
Update onScale implementation
Update zoom function (MapStageRenderer)
Add setScaleParam (GLSprite) 
Update Matrix block in onDraw method (GLSprite) 
Now the map can scale around the focus point. It is still a little bit shaky. Not smooth. 
27/07
Add WifiP2p relevant methods in ControlDroneActivity.java
Add P2PStateReceiver.java and VIONavDataService.java

28/07
Comment out WifiP2p relevant methods because WifiP2p and Wifi can’t work simultaneously on the  Glaxaxy Nexus device from the lab. Some devices may allow this behaviour, please do some further research.  

30/07
Add VIONavDataService.java in adrone project 
Add VIONavDataReadyReceiver.java in adrone project 
Add startVIONavDataService() in adrone project
Add VIONavData.java in adrone project

Add startClientService() in HelloMotionTracking prokect 
Add setDisplayRotation() 
Add callback method to check the display change 
Add VIONavDataClientService.java (not use because not quite sure how to properly implement multi-threading communication)
Add TangoSupport library in the build.gradle. 
