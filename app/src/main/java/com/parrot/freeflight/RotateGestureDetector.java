package com.parrot.freeflight;

import android.view.MotionEvent;

/**
 * Created by yizheng on 7/12/17.
 */

public class RotateGestureDetector {
    private OnRotateGestureListener listener;
    private int p1_ID, p2_ID;
    private boolean mInProgress;
    private float p1_x, p1_y;
    private float p2_x, p2_y;
    private float p1_nx, p1_ny;
    private float p2_nx, p2_ny;
    private float dx, dy, ndx, ndy;
    private float rAngle;

    public RotateGestureDetector(OnRotateGestureListener Listener){
        this.listener=Listener;
        p1_ID=-1;
        p2_ID=p1_ID;
        mInProgress=false;
    }

    public boolean onTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                p1_ID=event.getPointerId(event.getActionIndex());
                mInProgress=true;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                p2_ID=event.getPointerId(event.getActionIndex());
                p1_x=event.getX(event.findPointerIndex(p1_ID));
                p1_y=event.getY(event.findPointerIndex(p1_ID));
                p2_x=event.getX(event.getActionIndex());
                p2_y=event.getY(event.getActionIndex());
                dx=p2_x-p1_x;
                dy=p2_y-p1_y;
                break;
            case MotionEvent.ACTION_MOVE:
                if(p2_ID !=-1){
                    p1_nx=event.getX(event.findPointerIndex(p1_ID));
                    p1_ny=event.getY(event.findPointerIndex(p1_ID));
                    p2_nx=event.getX(event.findPointerIndex(p2_ID));
                    p2_ny=event.getY(event.findPointerIndex(p2_ID));
                    ndx=p2_nx-p1_nx;
                    ndy=p2_ny-p1_ny;
                    rAngle=rotateAngle(dx,dy,ndx,ndy);

                    listener.onRotate(this);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                p1_ID=-1;
                p2_ID=-1;
                mInProgress=false;
                break;
            case MotionEvent.ACTION_UP:
                p1_ID=-1;
                mInProgress=false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                p2_ID=-1;
                break;
        }
        return true;
    }

    public boolean isInProgess(){
        return mInProgress;
    }

    public float getrAngle(){
        return  rAngle;
    }

    public float rotateAngle(float dx, float dy, float ndx, float ndy){
        float angle= (float)Math.toDegrees(Math.atan(dy/dx));
        float nangle=(float)Math.toDegrees(Math.atan(ndy/ndx));

        float rangle=(angle-nangle)%360;
        if(rangle<-180)
            rangle +=360;
        if(rangle >180)
            rangle -=360;
        return rangle;
    }
}
