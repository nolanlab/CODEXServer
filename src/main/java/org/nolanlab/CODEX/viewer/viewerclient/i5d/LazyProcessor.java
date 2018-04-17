package org.nolanlab.CODEX.viewer.viewerclient.i5d;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.*;

public class LazyProcessor extends ImageProcessor{
    private ImageProcessor ip;

    private String filePath;

    public LazyProcessor (String filePath){
        this.filePath=filePath;
    }

    private ImageProcessor getIP(){
        if(ip == null){
            System.out.println("Loading image:"+filePath);
            ImagePlus imagePlus  =IJ.openImage(filePath);
            if(imagePlus== null){
                throw new IllegalStateException("Image could not be opened:" + filePath);
            }
            ip = imagePlus.getProcessor();
        }
        return ip;
    }

    @Override
    public int getWidth() {
        return getIP().getWidth();
    }

    @Override
    public int getHeight() {
        return getIP().getHeight();
    }

    @Override
    public void setMinAndMax(double min, double max) {
        getIP().setMinAndMax(min,max);
    }

    @Override
    public int[] getHistogram() {
        return getIP().getHistogram();
    }

    @Override
    public void filter(int type) {
        getIP().filter(type);
    }

    @Override
    public float getf(int index) {
        return getIP().getf(index);
    }

    @Override
    public void putPixel(int x, int y, int value) {
        getIP().putPixelValue(x,y,value);
    }

    @Override
    public void threshold(int level) {
        getIP().threshold(level);
    }

    @Override
    public Object getPixels() {
        return getIP().getPixels();
    }

    @Override
    public int getPixel(int x, int y) {
        return getIP().getPixel(x,y);
    }

    @Override
    public void setf(int x, int y, float value) {
        getIP().setf(x,y,value);
    }

    @Override
    public Object getSnapshotPixels() {
        return getIP().getSnapshotPixels();
    }

    @Override
    public double getMax() {
        return getIP().getMax();
    }

    @Override
    public int get(int index) {
        return getIP().get(index);
    }

    @Override
    public int getPixelInterpolated(double x, double y) {
        return getIP().getPixelInterpolated(x,y);
    }

    @Override
    public Image createImage() {
        return getIP().createImage();
    }

    @Override
    public void reset(ImageProcessor mask) {
        getIP().reset(mask);
    }

    @Override
    public void convolve(float[] kernel, int kernelWidth, int kernelHeight) {
        getIP().convolve(kernel,kernelWidth,kernelHeight);
    }

    @Override
    public void rotate(double angle) {
        getIP().rotate(angle);
    }

    @Override
    public ImageProcessor crop() {
        return getIP().crop();
    }

    @Override
    public void putPixelValue(int x, int y, double value) {
        getIP().putPixelValue(x,y,value);
    }

    @Override
    public void set(int x, int y, int value) {
        getIP().set(x,y,value);
    }

    @Override
    public double getInterpolatedPixel(double x, double y) {
        return getIP().getInterpolatedPixel(x,y);
    }

    @Override
    public float getf(int x, int y) {
        return getIP().getf(x,y);
    }

    @Override
    public void snapshot() {
        getIP().snapshot();
    }

    @Override
    public ImageProcessor duplicate() {
        return getIP().duplicate();
    }

    @Override
    public void setBackgroundValue(double value) {
        getIP().setBackgroundValue(value);
    }

    @Override
    public void setValue(double value) {
        getIP().setValue(value);
    }

    @Override
    public void set(int index, int value) {
        getIP().set(index, value);
    }

    @Override
    public FloatProcessor toFloat(int channelNumber, FloatProcessor fp) {
        return getIP().toFloat(channelNumber,fp);
    }

    @Override
    public void copyBits(ImageProcessor ip, int xloc, int yloc, int mode) {
        getIP().copyBits(ip,xloc,yloc,mode);
    }

    @Override
    public ImageProcessor createProcessor(int width, int height) {
        return getIP().createProcessor(width, height);
    }

    @Override
    public double getMin() {
        return getIP().getMin();
    }

    @Override
    public void scale(double xScale, double yScale) {
        getIP().scale(xScale,yScale);
    }

    @Override
    public void setColor(Color color) {
        getIP().setColor(color);
    }

    @Override
    public void convolve3x3(int[] kernel) {
        getIP().convolve3x3(kernel);
    }

    @Override
    public float getPixelValue(int x, int y) {
        return getIP().get(x,y);
    }

    @Override
    public int get(int x, int y) {
        return getIP().get(x,y);
    }

    @Override
    public void setf(int index, float value) {
        getIP().setf(index,value);
    }

    @Override
    public void fill(ImageProcessor imageProcessor) {
        getIP().fill(imageProcessor);
    }

    @Override
    public double getBackgroundValue() {
        return getIP().getBackgroundValue();
    }

    @Override
    public void erode() {
        getIP().erode();
    }

    @Override
    public void applyTable(int[] lut) {
        getIP().applyTable(lut);
    }

    @Override
    public void medianFilter() {
        getIP().medianFilter();
    }

    @Override
    public void drawPixel(int x, int y) {
        getIP().drawPixel(x,y);
    }

    @Override
    public void swapPixelArrays() {
        getIP().swapPixelArrays();
    }

    @Override
    public Object getPixelsCopy() {
        return getIP().getPixelsCopy();
    }

    @Override
    public void setSnapshotPixels(Object pixels) {
        getIP().setSnapshotPixels(pixels);
    }



    @Override
    public void flipVertical() {
        getIP().flipVertical();
    }

    @Override
    public void setPixels(Object pixels) {
        getIP().setPixels(pixels);
    }

    @Override
    public void setPixels(int channelNumber, FloatProcessor fp) {
        getIP().setPixels(channelNumber, fp);
    }

    @Override
    public ImageProcessor resize(int x, int y) {
        return getIP().resize(x,y);
    }

    @Override
    public void dilate() {
        getIP().dilate();
    }

    @Override
    public void reset() {
        getIP().reset();
    }

    @Override
    public void noise(double standardDeviation) {
        getIP().noise(standardDeviation);
    }


}
