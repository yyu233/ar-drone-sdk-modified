package com.parrot.freeflight;

/**
 * Created by yizheng on 6/30/17.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;

import com.parrot.freeflight.ui.HudViewController;
import com.parrot.freeflight.ui.hud.Sprite;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MapStageRenderer implements Renderer {

  //  private GLBGVideoSprite bgSprite;

    private ArrayList<Sprite> sprites;
    private Map<Integer, Sprite> idSpriteMap;

    private float fps;

    public  int screenWidth;
    public  int screenHeight;

    //**********************
    private float[] mVMatrix = new float[16];
    private float[] mProjMatrix = new float[16];
    private float [] mMapVMatrix=new float[16];
    private float [] mMapProjMatrix=new float[16];
    private float[] mMapRotMatrix=new float[16];



    private int program;


    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;   \n" +
                    "attribute vec4 vPosition; \n" +
                    "attribute vec2 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main(){              \n" +
                    "  gl_Position = uMVPMatrix * vPosition; \n" +
                    "  vTextureCoord = aTextureCoord;\n" +
                    "}                         \n";

    private final String fragmentShaderCode =
            "precision mediump float;  \n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "uniform float fAlpha ;\n" +
                    "void main(){              \n" +
                    " vec4 color = texture2D(sTexture, vTextureCoord); \n" +
                    " gl_FragColor = vec4(color.xyz, color.w * fAlpha );\n" +
                    " //gl_FragColor = vec4(0.6, 0.7, 0.2, 1.0); \n" +
                    "}                         \n";

    private long startTime;

    private long endTime;

    private boolean scaled;
    private boolean rotated;
    private static final String TAG= "MapStageRenderer";
    private float fov;
    private  float scaleCentreX;
    private float scaleCentreY;
    private float scaleFactor;
    private  Point rotCentre;
    private float rotAngle;
    //***********************


    public MapStageRenderer(Context context, Bitmap initialTexture)
    {
        //bgSprite = new GLBGVideoSprite(context.getResources());
        //bgSprite.setAlpha(1.0f);

        idSpriteMap = new Hashtable<Integer, Sprite>();
        sprites = new ArrayList<Sprite>(4);
        fov=90.0f;

    }


    public void addSprite(Integer id, Sprite sprite)
    {
        if (!idSpriteMap.containsKey(id)) {
            idSpriteMap.put(id, sprite);
            synchronized (sprites) {
                sprites.add(sprite);
            }
        }
    }



    public Sprite getSprite(Integer id)
    {
        return idSpriteMap.get(id);
    }


    public void removeSprite(Integer id)
    {
        if (idSpriteMap.containsKey(id)) {
            Sprite sprite = idSpriteMap.get(id);
            synchronized (sprites) {
                sprites.remove(sprite);
                idSpriteMap.remove(id);
            }
        }
    }


    public void onDrawFrame(Canvas canvas)
    {
       // bgSprite.onDraw(canvas, 0, 0);

        synchronized (sprites) {
            int spritesSize = sprites.size();

            for (int i=0; i<spritesSize; ++i) {
                Sprite sprite = sprites.get(i);

                if (!sprite.isInitialized() && screenWidth != 0 && screenHeight != 0) {
                    onSurfaceChanged(canvas, screenWidth, screenHeight);
                    sprite.surfaceChanged(canvas);
                }

                if (sprite != null) {
                    sprite.draw(canvas);
                }
            }
        }
    }


    public void onDrawFrame(GL10 gl)
    {
        // Limiting framerate in order to save some CPU time
        endTime = System.currentTimeMillis();
        long dt = endTime - startTime;

        if (dt < 33) try {
            Thread.sleep(33 - dt);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        startTime = System.currentTimeMillis();

        // Drawing scene
       // bgSprite.onDraw(gl, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


        synchronized (sprites) {
            int spritesSize = sprites.size();
            if(scaled){
                //idSpriteMap.get(HudViewController.MAP_ID).setViewAndProjectionMatrices(mMapVMatrix,mMapProjMatrix);
                //idSpriteMap.get(HudViewController.MAP_ID).setViewAndProjectionMatrices(mVMatrix, mMapProjMatrix,mMapRotMatrix);
                idSpriteMap.get(HudViewController.MAP_ID).setScaleParam(scaleCentreX,scaleCentreY,scaleFactor);//update Map
                scaled=false;
            }
            if(rotated){
                idSpriteMap.get(HudViewController.MAP_ID).setRotateParam(rotCentre,rotAngle);
                rotated=false;
            }
            for (int i=0; i<spritesSize; ++i) {
                Sprite sprite = sprites.get(i);
                if (sprite != null) {
                    if (!sprite.isInitialized() && screenWidth != 0 && screenHeight != 0) {
                        sprite.init(gl, program);
                        sprite.surfaceChanged(null, screenWidth, screenHeight);
                        sprite.setViewAndProjectionMatrices(mVMatrix, mProjMatrix);
                    }

                    sprite.draw(gl);
                }
            }
        }
    }


    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        screenWidth = width;
        screenHeight = height;
        GLES20.glViewport(0, 0, width, height);
        Matrix.orthoM(mProjMatrix, 0, 0, width, 0, height, 0, 100f);
        //Matrix.perspectiveM(mProjMatrix,0,fov ,(float)(width/height),1.5f,-5f);

        mMapProjMatrix=mProjMatrix;

       // bgSprite.setViewAndProjectionMatrices(mVMatrix, mProjMatrix);
        //bgSprite.onSurfaceChanged(gl, width, height);

        synchronized (sprites) {
            int size = sprites.size();
            for (int i=0; i<size; ++i) {
                Sprite sprite = sprites.get(i);

                if (sprite != null) {
                    sprite.setViewAndProjectionMatrices(mVMatrix, mProjMatrix);
                    sprite.surfaceChanged(null, width, height);
                }
            }
        }
    }


    public void onSurfaceChanged (Canvas canvas, int width, int height)
    {
        screenWidth = width;
        screenHeight = height;

        //bgSprite.onSurfaceChanged(null, width, height);
    }


    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        startTime = System.currentTimeMillis();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);

        GLES20.glLinkProgram(program);
       // bgSprite.init(gl, program);

        // Init sprites
        synchronized (sprites) {
            for (int i=0; i<sprites.size(); ++i) {
                sprites.get(i).init(gl, program);
            }
        }

        Matrix.setLookAtM(mVMatrix, 0, /*x*/0, /*y*/0, /*z*/1.5f, 0f, 0f, -5f, 0, 1f, 0.0f);
        mMapVMatrix=mVMatrix;
    }


    public float getFPS()
    {
        return fps;
    }


   /* public boolean updateVideoFrame()
    {
        return bgSprite.updateVideoFrame();
    }*/


    private int loadShader(int type, String code)
    {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0)
        {
            Log.e("opengl", "Could not compile shader");
            Log.e("opengl", GLES20.glGetShaderInfoLog(shader));
            Log.e("opengl", code);
        }

        return shader;
    }


    public void clearSprites()
    {
        synchronized (sprites) {
            for (int i=0; i<sprites.size(); ++i) {
                Sprite sprite = sprites.get(i);
                sprite.freeResources();
            }

            sprites.clear();
        }
    }

/*  public void updatePMatrix(float scaleDis){
        if(fov >=1.0f && fov<=45.0f){
            fov +=scaleDis;
        }
        if(fov <=1.0f){
            fov=1.0f;
        }
        if(fov >=45.0f){
            fov=45.0f;
        }
        Log.d(TAG,"fov: " +fov);
        Matrix.perspectiveM(mMapProjMatrix,0,fov ,(float)(GLSprite.SCREEN_WIDTH/GLSprite.SCREEN_HEIGHT),0f,100f);
        scaled=true;
    }*/

  public void zoom(float scaleCentreX, float scaleCentreY,float scaleFactor){
      scaled=true;

      //Matrix.scaleM(mMapProjMatrix,0,scaleFactor,scaleFactor,1.0f);
      this.scaleCentreX=scaleCentreX;
      this.scaleCentreY=scaleCentreY;
      this.scaleFactor=scaleFactor;
//windows coordinates starts at top left whereas Opengl coordinates starts at bottom left.

  }
  public void rotate(Point rotCentre, float rotAngle){
      rotated=true;

      this.rotCentre=rotCentre;
      this.rotAngle=rotAngle;

      //Matrix.rotateM(mMapVMatrix,0,rotateFactor,0,0,-1);
     // Matrix.setIdentityM(mMapRotMatrix,0);
     // Matrix.setRotateM(mMapRotMatrix, 0,rotateFactor,0,0,1);

  }
}
