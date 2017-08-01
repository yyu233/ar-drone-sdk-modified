package com.parrot.freeflight;

import java.io.Serializable;

/**
 * Created by yizheng on 7/30/17.
 */

public class VIONavData implements Serializable {

    private float translation[];
    private float orientation[];
    private double timestamp;

    public VIONavData(){

    }

    public VIONavData(float translation[], float orientation[], double timestamp){
        this.translation=translation.clone();
        this.orientation=orientation.clone();
        this.timestamp=timestamp;
    }

    public void setTranslation(float translation[]){
        this.translation=translation.clone();
    }

    public void setOrientation(float orientation[]){
        this.orientation=orientation.clone();
    }

    public void setTimestamp(double timestamp){
        this.timestamp=timestamp;
    }

    public float [] getTranslation(){
        return translation;
    }

    public float[] getOrientation(){
        return orientation;
    }

    public double getTimestamp(){
        return timestamp;
    }
}
