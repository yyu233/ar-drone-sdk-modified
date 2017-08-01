/*
 * GLSprite
 * 
 * Created on: Apr 26, 2011
 * Author: Dmytro Baryskyy
 */

package com.parrot.freeflight.ui.gl;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;

import com.parrot.freeflight.utils.TextureUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLU.gluUnProject;

public class GLSprite
{
    private static final int VERTEX_BUFFER = 0;
    private static final int INDEX_BUFFER = 1;
    private static final String TAG = GLSprite.class.getSimpleName();
    
    private static final int _COUNT = 4;
    private static final int VERTEX_COORDS_SIZE = 3;
    private static final int TEXTURE_COORDS_SIZE = 2;
    private static final int FLOAT_SIZE_BYTES = 4;

    // Left public in order to save method calls
    public int width;
    public int height;

    public int imageWidth;
    public int imageHeight;

    public int textureWidth;
    public int textureHeight;

    public float alpha;

    public Bitmap texture;
    private Paint currPaint;

    protected boolean readyToDraw;

    private int positionHandle;
    private int textureHandle;
    private int mvpMatrixHandle;
    private int fAlphaHandle;

    protected int[] textures = {
        -1
    };
    protected int[] buffers = {
            -1, -1
    };
    private float[] mMVPMatrix = new float[16];
    private float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private float[] mMVMatrix=new float[16];
    private float[] mProjMatrix = new float[16];
    private float[] mRotMatrix=new float[16];


    private Rect srcRect;
    private Rect dstRect;

    protected int program;
    private Buffer vertices;
    private Buffer indexes;

    private boolean updateVertexBuffer;
    private boolean recalculateMatrix;
    private boolean updateTexture;

    private float prevX;
    private float prevY;

    private boolean useWorkaroundsForSDK8 = false;

    public static int SCREEN_WIDTH;

    public static int SCREEN_HEIGHT;

    private Bitmap bitMap;

    private boolean scaled;
    private boolean rotated;

    private volatile float objScaleCentreX;
    private volatile float objScaleCentreY;
    private float scaleFactor;
    private float transX;
    private float transY;

    private int[] currView=new int[4];
    private float[] objPoint=new float[4];
    private float scaleCentreX;
    private float scaleCentreY;

    private Point rotCentre;
    private float objRotCentreX;
    private float objRotCentreY;
    private float [] objRot=new float[4];
    private float rotAngle;


    public GLSprite(Resources resources, int bitmapId)
    {
        this(resources, BitmapFactory.decodeResource(resources, bitmapId));
    }


    public GLSprite(Resources res, Bitmap bmp)
    {

        SCREEN_WIDTH=res.getDisplayMetrics().widthPixels;
        SCREEN_HEIGHT=res.getDisplayMetrics().heightPixels;

        currView[0]=0;
        currView[1]=0;
        currView[2]=SCREEN_WIDTH;
        currView[3]=SCREEN_HEIGHT;

        rotCentre=new Point(0,0);

        useWorkaroundsForSDK8 = Build.VERSION.SDK_INT < 9;
        updateVertexBuffer = false;
        recalculateMatrix = true;
        alpha = 1.0f;
        readyToDraw = false;

        srcRect = new Rect();
        dstRect = new Rect();

        if (bmp != null) {

            width = bmp.getWidth();
            height = bmp.getHeight();
            if(width >SCREEN_WIDTH || height >SCREEN_HEIGHT){//if sprite is larger than the screen
                bmp=bmp.createScaledBitmap(bmp, SCREEN_WIDTH, SCREEN_HEIGHT, false);//automatically resize and fit the sreen
                width=SCREEN_WIDTH;
                height=SCREEN_HEIGHT;
            }
            bitMap=bmp;
            texture = TextureUtils.makeTexture( bmp);
            srcRect.set(0, 0, width, height);
        } else {
            texture = Bitmap.createBitmap(32, 32, Bitmap.Config.RGB_565);
            width = 0;
            height = 0;
        }

        imageWidth = width;
        imageHeight = height;

        textureWidth = texture.getWidth();
        textureHeight = texture.getHeight();

        currPaint = new Paint();
        Matrix.setIdentityM(mRotMatrix,0);
    }

  /*  public void updateBitMap(float scale){
        Log.d(TAG, "updateBitMap called");
        bitMap=bitMap.createScaledBitmap(bitMap, (int)(bitMap.getWidth()*scale),(int)(bitMap.getHeight()*scale),false);

        texture = TextureUtils.makeTexture(bitMap);



    }*/



    public  void  init(GL10 gl, int program)
    {
        this.program = program;

        GLES20.glUseProgram(program);
        checkGlError("glUseProgram program");
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        textureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        fAlphaHandle = GLES20.glGetUniformLocation(program, "fAlpha");
        checkGlError("glGetAttribLocation");

        recalculateTexturePosition();
    }


    @SuppressLint("NewApi")
    public void recalculateTexturePosition()
    {
        if (textures[0] != -1) {
            GLES20.glDeleteTextures(1, textures, 0);
        }

        if (buffers[0] != -1) {
            GLES20.glDeleteBuffers(buffers.length, buffers, 0);
        }

        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        checkGlError("glBindTexture");

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0);
        checkGlError("texImage2D");

        GLES20.glGenBuffers(buffers.length, buffers, 0);

        // Vertices
        vertices = createVertex(width, height);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[VERTEX_BUFFER]);
        checkGlError("glBindBuffer buffers[" + VERTEX_BUFFER + "]");
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 20 * FLOAT_SIZE_BYTES, vertices, GLES20.GL_STATIC_DRAW);
        checkGlError("glBufferData vertices");
        
        if (useWorkaroundsForSDK8) {
            fix.android.opengl.GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 5 * FLOAT_SIZE_BYTES, 0);
        } else {
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 5 * FLOAT_SIZE_BYTES, 0); 
        }

        GLES20.glEnableVertexAttribArray(positionHandle);

        if (useWorkaroundsForSDK8) {
            fix.android.opengl.GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false, 5 * FLOAT_SIZE_BYTES, 3 * FLOAT_SIZE_BYTES);
        } else {
            GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false, 5 * FLOAT_SIZE_BYTES, 3 * FLOAT_SIZE_BYTES);
        }
        
        GLES20.glEnableVertexAttribArray(textureHandle);

        // Indexes
        indexes = createIndex();
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[INDEX_BUFFER]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 4 * 2, indexes, GLES20.GL_STATIC_DRAW);
    }


    private FloatBuffer createVertex(float width, float height)
    {
        float texXcoef = (float) imageWidth / (float) textureWidth;
        float texYcoef = (float) imageHeight / (float) textureHeight;

        // Init vertex where we will draw texture
        float[] rectVerticesData = {
                // X Y Z U V
                width, 0f, 0f, texXcoef, texYcoef,
                width, height, 0f, texXcoef, 0,
                0, 0, 0, 0, texYcoef,
                0, height, 0, 0, 0
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(rectVerticesData.length * FLOAT_SIZE_BYTES);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer vertices = vbb.asFloatBuffer();
        vertices.put(rectVerticesData);
        vertices.position(0);

        return vertices;
    }


    private ShortBuffer createIndex()
    {
        short[] indexesData = {
                0, 1, 2, 3
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(indexesData.length * (Short.SIZE / 8));
        vbb.order(ByteOrder.nativeOrder());
        ShortBuffer indexes = vbb.asShortBuffer();
        indexes.put(indexesData);
        indexes.position(0);

        return indexes;
    }


    public void setSize(int width, int height)
    {
        this.width = width;
        this.height = height;
        vertices = createVertex(width, height);

        updateVertexBuffer = true;
    }


    protected void onUpdateTexture()
    {
        if (updateTexture) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0);
            updateTexture = false;

            vertices = createVertex(width, height);
            updateVertexBuffer = true;
        }
    }


    public void setViewAndProjectionMatrices(float[] vMatrix, float[] projMatrix)
    {
        this.mVMatrix = vMatrix;
        this.mProjMatrix = projMatrix;

        recalculateMatrix = true;

        readyToDraw = true;
    }

    public void setViewAndProjectionMatrices(float[] vMatrix, float[]projMatrix, float[]rotMatrix){
        this.mVMatrix=vMatrix;
        this.mProjMatrix=projMatrix;
        this.mRotMatrix=rotMatrix;

        recalculateMatrix=true;
        readyToDraw=true;
    }

    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        recalculateMatrix = true;
    }

    public void setScaleParam(float scaleCentreX, float scaleCentreY, float scaleFactor){

        if(this.scaleCentreX !=scaleCentreX || this.scaleCentreY !=scaleCentreY){//only when focus point of scale changes, recaculate
            this.scaleCentreX=scaleCentreX;
            this.scaleCentreY=scaleCentreY;
            //convert scale center on window coordinate system to OpenGL world coordinate system
            if(gluUnProject(scaleCentreX,scaleCentreY,0,mMVMatrix,0,mProjMatrix,0,currView,0,objPoint,0)
                    ==0){
                Log.d(TAG,"gluUnproject failed");
                objScaleCentreX=0;
                objScaleCentreY=0;
            }else{
                Log.d(TAG,"gluUnproject success");
                objScaleCentreX=objPoint[0]-SCREEN_WIDTH;
                objScaleCentreY=objPoint[1]-SCREEN_HEIGHT;
                Log.d(TAG,"objScaleCenterX: " +objScaleCentreX + " objScaleCenterY: " +objScaleCentreY);
            }
        }

        this.scaleFactor=scaleFactor;
        scaled=true;
        recalculateMatrix=true;
        readyToDraw=true;
    }

    public void setRotateParam(Point rotCentre, float rotAngle){
        if(!this.rotCentre.equals(rotCentre.x,rotCentre.y)){
            this.rotCentre=rotCentre;
            if(gluUnProject(rotCentre.x,rotCentre.y,0,mMVMatrix,0,mProjMatrix,0,currView,0,objRot,0)
                    ==0){
                Log.d(TAG,"gluUnproject failed");
                objRotCentreX=0;
                objRotCentreY=0;
            }else{
                Log.d(TAG,"gluUnproject success");
                objRotCentreX=objRot[0]-SCREEN_WIDTH;
                objRotCentreY=objRot[1]-SCREEN_HEIGHT;
                Log.d(TAG,"objRotCenterX: " +objRotCentreX + " objRotCenterY: " +objRotCentreY);
            }

        }
        this.rotAngle=rotAngle;
        rotated=true;
        recalculateMatrix=true;
        readyToDraw=true;
    }

    @SuppressLint("NewApi")
    public void onDraw(GL10 gl, float x, float y)
    {

        if (!readyToDraw)
            return;

            if (prevX != x || prevY != y) {
                recalculateMatrix = true;
                prevX = x;
                prevY = y;
            }



        GLES20.glUseProgram(program);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        onUpdateTexture();

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[VERTEX_BUFFER]);
        
        if (updateVertexBuffer) {
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, 20 * FLOAT_SIZE_BYTES, vertices);
            updateVertexBuffer = false;
        }

        int stride = 5 * FLOAT_SIZE_BYTES;
        if (useWorkaroundsForSDK8) {
            fix.android.opengl.GLES20.glVertexAttribPointer(positionHandle, VERTEX_COORDS_SIZE, GLES20.GL_FLOAT, false, stride, 0);
            fix.android.opengl.GLES20.glVertexAttribPointer(textureHandle, TEXTURE_COORDS_SIZE, GLES20.GL_FLOAT, false, stride, VERTEX_COORDS_SIZE * FLOAT_SIZE_BYTES);
        } else {            
            GLES20.glVertexAttribPointer(positionHandle, VERTEX_COORDS_SIZE, GLES20.GL_FLOAT, false, stride, 0);
            GLES20.glVertexAttribPointer(textureHandle, TEXTURE_COORDS_SIZE, GLES20.GL_FLOAT, false, stride, VERTEX_COORDS_SIZE * FLOAT_SIZE_BYTES);
        }


            if (recalculateMatrix) {
                Log.d(TAG, "recalculateMatrix ==true");
                Matrix.setIdentityM(mMMatrix, 0);
                //Matrix.multiplyMM(mMMatrix,0,mRotMatrix,0, mMMatrix,0);
                if(scaled){
                    Matrix.translateM(mMMatrix, 0, -objScaleCentreX, -objScaleCentreY,0);
                    Matrix.scaleM(mMMatrix,0,scaleFactor, scaleFactor,1);
                    Matrix.translateM(mMMatrix,0,objScaleCentreX,objScaleCentreY,0);

                }

                if(rotated){
                    Matrix.translateM(mMMatrix, 0,-objRotCentreX,-objScaleCentreY,0);
                    Matrix.rotateM(mMMatrix,0,rotAngle,0,0,-1);
                    Matrix.translateM(mMMatrix,0,objRotCentreX,objRotCentreY,0);
                }
                Matrix.translateM(mMMatrix, 0, x, y, 0);

                //Matrix.multiplyMM(mMMatrix,0,mRotMatrix,0, mMMatrix,0);

                Matrix.multiplyMM(mMVMatrix, 0, mVMatrix, 0, mMMatrix, 0);

               // Matrix.multiplyMM(mMVPMatrix,0,mRotMatrix,0,mMVPMatrix,0);

                Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVMatrix, 0);

                //Matrix.multiplyMM(mMVPMatrix,0,mMVPMatrix,0,mRotMatrix,0);

                recalculateMatrix = false;
            }



        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mMVPMatrix, 0);

        if (alpha < 1.0f) {
            GLES20.glUniform1f(fAlphaHandle, alpha);
        } else {
            GLES20.glUniform1f(fAlphaHandle, 1.0f);
        }

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[INDEX_BUFFER]);

            if (useWorkaroundsForSDK8) {

                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, _COUNT);
            } else {

                GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, _COUNT, GLES20.GL_UNSIGNED_SHORT, 0);
            }



        checkGlError("glDrawElements");
    }


    public void onDraw(Canvas canvas, float x, float y)
    {
        currPaint.setAlpha((int) (alpha * 255.0f));
        dstRect.set(srcRect);
        dstRect.offset((int) x, (int) y);
        canvas.drawBitmap(texture, srcRect, dstRect, currPaint);
    }


    public void setAlpha(float alpha)
    {
        if (alpha > 1.0f)
            this.alpha = 1.0f;

        if (alpha < 0.0f) {
            this.alpha = 0;
        }

        this.alpha = alpha;
    }


    public void updateTexture( Bitmap bitmap)
    {
        if (this.texture != null) {
            this.texture.recycle();
        }

            this.texture = TextureUtils.makeTexture( bitmap);

            width = bitmap.getWidth();
            height = bitmap.getHeight();

            srcRect.set(0, 0, width, height);
            imageWidth = width;
            imageHeight = height;

            textureWidth = texture.getWidth();
            textureHeight = texture.getHeight();

            updateTexture = true;


    }


    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            try {
                throw new RuntimeException(op + ": glError " + error);
            } catch (RuntimeException e) {
                // We catch this exception just because we want to display stack trace into the log
                // and continue running the app
                Log.w(TAG, Log.getStackTraceString(e));
            }
        }
    }
    
    
    public void freeResources()
    {
        if (texture != null) {
            texture.recycle();
        }
    }


    public boolean isReadyToDraw()
    {
        return readyToDraw;
    }
}
