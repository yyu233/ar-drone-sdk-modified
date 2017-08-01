package com.parrot.freeflight.ui.hud;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import com.parrot.freeflight.ui.gl.GLSprite;

import javax.microedition.khronos.opengles.GL10;

public class Image extends Sprite 
{
	public enum SizeParams {
		NONE,
		FILL_SCREEN
	}
	
	private GLSprite sprite;
	private SizeParams widthParam;
	private SizeParams heightParam;
	
	private boolean isInitialized;
	private static final String TAG="GLSprite";

	public Image(Resources resources, int resId, Align align)
	{
		super(align);
		
		widthParam = SizeParams.NONE;
		heightParam = SizeParams.NONE;
		
		isInitialized = false;
		sprite = new GLSprite(resources, resId);	
	}
	
	
	@Override
	public void init(GL10 gl, int program) 
	{
		sprite.init(gl, program);
		isInitialized = true;
	}

	
	@Override
	public void surfaceChanged(GL10 gl, int width, int height) 
	{
		sprite.onSurfaceChanged(gl, width, height);
		
		if (widthParam == SizeParams.FILL_SCREEN) {
			sprite.setSize(width, sprite.height);
		}
		
		super.surfaceChanged(gl, width, height);
	}

	
	@Override
	public void surfaceChanged(Canvas canvas)
	{
		super.surfaceChanged(canvas);
	}

	
	@Override
	public void draw(GL10 gl) 
	{
		sprite.onDraw(gl, bounds.left, surfaceHeight - bounds.top - sprite.height);

	}

	
	@Override
	public void draw(Canvas canvas)
	{
		sprite.onDraw(canvas, bounds.left, bounds.top);
	}
	

	@Override
	public boolean onTouchEvent(View v, MotionEvent event) 
	{
		return false;
	}

	
	@Override
    protected void onAlphaChanged(float newAlpha)
    {
	    sprite.setAlpha(newAlpha);
    }


	@Override
	public boolean isInitialized() 
	{
		return isInitialized;
	}

	
	@Override
	public void setViewAndProjectionMatrices(float[] vMatrix, float[] projMatrix) 
	{
		sprite.setViewAndProjectionMatrices(vMatrix, projMatrix);
	}

	public void setViewAndProjectionMatrices(float[] vMatrix, float[] projMatrix, float[] rotMatrix){
		sprite.setViewAndProjectionMatrices(vMatrix,projMatrix,rotMatrix);
	}
	
	public void setBounds(Rect rect)
	{
		this.bounds = rect;
	}

	
	public void setPositionTo(int x, int y)
	{

		bounds.offsetTo(x, y);
	}

	public void setPosition(int dx, int dy){
		bounds.offset(dx,dy);
	}
	@Override
	public int getWidth() 
	{
		return sprite.width;
	}

	@Override
	public int getHeight() 
	{
		return sprite.height;
	}
	

	public void setSizeParams(SizeParams width, SizeParams height) 
	{
		widthParam = width;
		heightParam = height;
	}

	
    @Override
    public void freeResources()
    {
        sprite.freeResources();
    }

   // public void updateBitMap(float scale){ sprite.updateBitMap(scale);}
	@Override
   public void setScaleParam(float scaleCentreX, float scaleCentreY, float scaleFactor){
	   sprite.setScaleParam(scaleCentreX,scaleCentreY,scaleFactor);
   }

   public void setRotateParam(Point rotCentre, float rotAngle) {
	   sprite.setRotateParam(rotCentre, rotAngle);
   }
	public Point getCentre(){
		Point cP=new Point (bounds.centerX(), bounds.centerY());
		return cP;
	}

}
