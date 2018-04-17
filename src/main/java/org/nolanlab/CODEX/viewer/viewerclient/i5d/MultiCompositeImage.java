package org.nolanlab.CODEX.viewer.viewerclient.i5d;


import ij.*;
import ij.io.FileInfo;
import ij.macro.Interpreter;
import ij.plugin.frame.Channels;
import ij.plugin.frame.ContrastAdjuster;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import org.nolanlab.CODEX.viewer.viewerclient.gui.MultichannelViewerWindow;

import java.awt.*;
import java.awt.image.*;

public class MultiCompositeImage extends CompositeImage {

    /** Display modes (note: TRANSPARENT mode has not yet been implemented) */
    public static final int COMPOSITE=1, COLOR=2, GRAYSCALE=3, TRANSPARENT=4;
    public static final int MAX_CHANNELS = 256;
    int[] rgbPixels;
    boolean newPixels;
    MemoryImageSource imageSource;
    Image awtImage;
    WritableRaster rgbRaster;
    SampleModel rgbSampleModel;
    BufferedImage rgbImage;
    ColorModel rgbCM;
    ImageProcessor[] cip;
    Color[] colors =null;

    LUT[] lut = new LUT[MAX_CHANNELS];
    int currentChannel = -1;
    int previousChannel;
    int currentSlice = 1;
    int currentFrame = 1;
    boolean singleChannel;
    boolean[] active = new boolean[MAX_CHANNELS];
    int mode = COMPOSITE;
    int bitDepth;
    double[] displayRanges;
    byte[][] channelLuts;
    boolean customLuts;
    boolean syncChannels;

    @Override
    public boolean isComposite() {
        return super.isComposite();
    }

    public MultiCompositeImage(String title, ImageStack stack2, int channels, int z, int t){

        super(new ImagePlus(title, stack2));

        if(colors == null){
            colors=new Color[MAX_CHANNELS];
            for (int i = 0; i < MAX_CHANNELS; i++) {
                float val = i/((float)MAX_CHANNELS);

                val*=5;
                colors[i]= new Color(Color.HSBtoRGB(0.60f - (float) val * 0.60f, 1, 1f));
                lut[i]= LUT.createLutFromColor(colors[i]);
            }
        }


        this.mode = mode;
        bitDepth = getBitDepth();
        int stackSize = stack2.getSize();

        if (channels==1 && stackSize<=MAX_CHANNELS )
            channels = stackSize;
        if (channels<1 || (stackSize%channels)!=0)
            throw new IllegalArgumentException("stacksize not multiple of channels");
        if (mode==COMPOSITE && channels>MAX_CHANNELS) {
            this.mode = COLOR;
        }
        compositeImage = true;

        if (channels==stackSize || channels*z*t!=stackSize)
            setDimensions(channels, stackSize/channels, 1);
        else
            setDimensions(channels, z, t);
        setStack(title, stack2);

        /*
        FileInfo fi = imp.getOriginalFileInfo();
        if (fi!=null) {
            displayRanges = fi.displayRanges;
            channelLuts = fi.channelLuts;
        }
        setFileInfo(fi);
        Object info = imp.getProperty("Info");
        if (info!=null)
            setProperty("Info", imp.getProperty("Info"));
            */

        /*
        if (mode==COMPOSITE) {
            for (int i=0; i<MAX_CHANNELS; i++)
                active[i] = true;
        } else*/

        active[0] = true;
        //if (!(channels==3&&stackSize==3))
        /*
        setRoi(imp.getRoi());
        setOverlay(imp.getOverlay());
        */
        if (channels!=stackSize)
            setOpenAsHyperStack(true);
    }



    public MultiCompositeImage(ImagePlus imp) {
        this(imp, COLOR);
    }

    public MultiCompositeImage(ImagePlus imp, int mode) {
        super(imp, mode);

        if (mode<COMPOSITE || mode>GRAYSCALE)
            mode = COLOR;
        this.mode = mode;
        int channels = imp.getNChannels();
        bitDepth = getBitDepth();
        if (IJ.debugMode) IJ.log("CompositeImage: "+imp+" "+mode+" "+channels);
        ImageStack stack2;
        boolean isRGB = imp.getBitDepth()==24;
        if (isRGB) {
            if (imp.getImageStackSize()>1)
                throw new IllegalArgumentException("RGB stacks not supported");
            stack2 = getRGBStack(imp);
        } else
            stack2 = imp.getImageStack();
        int stackSize = stack2.getSize();
        if (channels==1 && isRGB)
            channels = 3;
        if (channels==1 && stackSize<=MAX_CHANNELS && !imp.isDisplayedHyperStack())
            channels = stackSize;
        if (channels<1 || (stackSize%channels)!=0)
            throw new IllegalArgumentException("stacksize not multiple of channels");
        if (mode==COMPOSITE && channels>MAX_CHANNELS)
            this.mode = COLOR;
        compositeImage = true;
        int z = imp.getNSlices();
        int t = imp.getNFrames();
        if (channels==stackSize || channels*z*t!=stackSize)
            setDimensions(channels, stackSize/channels, 1);
        else
            setDimensions(channels, z, t);
        setStack(imp.getTitle(), stack2);
        setCalibration(imp.getCalibration());
        FileInfo fi = imp.getOriginalFileInfo();
        if (fi!=null) {
            displayRanges = fi.displayRanges;
            channelLuts = fi.channelLuts;
        }
        setFileInfo(fi);
        Object info = imp.getProperty("Info");
        if (info!=null)
            setProperty("Info", imp.getProperty("Info"));
        if (mode==COMPOSITE) {
            for (int i=0; i<MAX_CHANNELS; i++)
                active[i] = true;
        } else
            active[0] = true;
        //if (!(channels==3&&stackSize==3))
        setRoi(imp.getRoi());
        setOverlay(imp.getOverlay());
        if (channels!=stackSize)
            setOpenAsHyperStack(true);
    }

    @Override
    public void setC(int channel) {
        ensureCIPnotNull(channel-1);
        ip = cip[channel-1];
        super.setC(channel);
    }

    @Override
    public void setT(int frame) {
        for (int i = 0; i < active.length; i++) {
            if(active[i]){
                ensureCIPnotNull(i);
            }
        }
        ip = cip[getC()];
        super.setT(frame);

    }

    @Override
    public void setZ(int slice) {
        for (int i = 0; i < active.length; i++) {
            if(active[i]){
                ensureCIPnotNull(i);
            }
        }
        ip = cip[getC()];
        super.setZ(slice);
    }

    public Image getImage() {
        if (img==null)
            updateImage();
        return img;
    }

    public void updateChannelAndDraw() {
        if (!customLuts) singleChannel = true;
        updateAndDraw();
    }

    public void updateAllChannelsAndDraw() {
        if (mode!=COMPOSITE)
            updateChannelAndDraw();
        else {
            syncChannels = true;
            singleChannel = false;
            updateAndDraw();
        }
    }

    public ImageProcessor getChannelProcessor() {
        if (cip!=null && currentChannel!=-1)
            return cip[currentChannel];
        else
            return getProcessor();
    }

    synchronized void setup(int channels, ImageStack stack2) {
        if (stack2!=null && stack2.getSize()>0 && (stack2.getProcessor(1) instanceof ColorProcessor)) { // RGB?
            cip = null;
            lut = null;
            return;
        }
        setupLuts(channels);
        if (mode==COMPOSITE) {
            cip = new ImageProcessor[channels];
            for (int i=0; i<channels; ++i) {
                if(i==0||active[i]) {
                    cip[i] = stack2.getProcessor(i + 1);
                    cip[i].setLut(lut[i]);
                }
            }
            currentSlice = currentFrame = 1;
        }
    }

    void setupLuts(int channels) {
        if (lut==null || lut.length<channels) {
            if (displayRanges!=null && channels!=displayRanges.length/2)
                displayRanges = null;
            if (displayRanges==null&&ip.getMin()==0.0&&ip.getMax()==0.0)
                ip.resetMinAndMax();
            lut = new LUT[channels];
            LUT lut2 = channels>MAX_CHANNELS?createLutFromColor(Color.white):null;
            for (int i=0; i<channels; ++i) {
                if (channelLuts!=null && i<channelLuts.length) {
                    lut[i] = createLutFromBytes(channelLuts[i]);
                    customLuts = true;
                } else if (i<MAX_CHANNELS)
                    lut[i] = createLutFromColor(colors[i]);
                else
                    lut[i] = (LUT)lut2.clone();
                if (displayRanges!=null) {
                    lut[i].min = displayRanges[i*2];
                    lut[i].max = displayRanges[i*2+1];
                } else {
                    lut[i].min = ip.getMin();
                    lut[i].max = ip.getMax();
                }
            }
            displayRanges = null;
        }
    }

    public void resetDisplayRanges() {
        int channels = getNChannels();
        if (lut==null)
            setupLuts(channels);
        ImageStack stack2 = getImageStack();
        if (lut==null || channels!=lut.length || channels>stack2.getSize() || channels>MAX_CHANNELS)
            return;
        for (int i=0; i<channels; ++i) {
            ImageProcessor ip2 = stack2.getProcessor(i+1);
            ip2.resetMinAndMax();
            lut[i].min = ip2.getMin();
            lut[i].max = ip2.getMax();
        }
    }

    public void updateAndDraw() {
        updateImage();
        if (win!=null)
            notifyListeners(UPDATED);
        draw();
    }

    public synchronized void updateImage() {
        int imageSize = width*height;
        int nChannels = getNChannels();
        int redValue, greenValue, blueValue;
        int ch = getChannel();

        //IJ.log("updateImage: "+ch+"/"+nChannels+" "+currentSlice+" "+currentFrame);
        if (ch>nChannels) ch = nChannels;
        boolean newChannel = false;
        if (ch-1!=currentChannel) {
            previousChannel = currentChannel;
            currentChannel = ch-1;
            newChannel = true;
        }

        ImageProcessor ip = getProcessor();
        if (mode!=COMPOSITE) {
            if (newChannel) {
                setupLuts(nChannels);
                LUT cm = lut[currentChannel];
                if (mode==COLOR)
                    ip.setColorModel(cm);
                if (!(cm.min==0.0&&cm.max==0.0))
                    ip.setMinAndMax(cm.min, cm.max);
                if (!IJ.isMacro()) ContrastAdjuster.update();
                for (int i=0; i<MAX_CHANNELS; i++)
                    active[i] = i==currentChannel?true:false;
                Channels.updateChannels();
            }
            if (ip!=null)
                img = ip.createImage();
            return;
        }

        if (nChannels==1) {
            cip = null;
            rgbPixels = null;
            awtImage = null;
            if (ip!=null)
                img = ip.createImage();
            return;
        }

        if (cip==null||cip[0].getWidth()!=width||cip[0].getHeight()!=height||getBitDepth()!=bitDepth) {
            setup(nChannels, getImageStack());
            rgbPixels = null;
            rgbSampleModel = null;
            if (currentChannel>=nChannels) {
                setSlice(1);
                currentChannel = 0;
                newChannel = true;
            }
            bitDepth = getBitDepth();
        }

        if (newChannel) {
            ensureCIPnotNull(currentChannel);
            getProcessor().setMinAndMax(cip[currentChannel].getMin(), cip[currentChannel].getMax());
            if (!IJ.isMacro()) ContrastAdjuster.update();
        }
        //IJ.log(nChannels+" "+ch+" "+currentChannel+"  "+newChannel);

        if (getSlice()!=currentSlice || getFrame()!=currentFrame) {
            currentSlice = getSlice();
            currentFrame = getFrame();
            int position = getStackIndex(1, currentSlice, currentFrame);
            if (cip==null) return;
            for (int i=0; i<nChannels; ++i)
                if(active[i]) {
                    cip[i].setPixels(getImageStack().getProcessor(position + i).getPixels());
                }
        }

        if (rgbPixels == null) {
            rgbPixels = new int[imageSize];
            newPixels = true;
            imageSource = null;
            rgbRaster = null;
            rgbImage = null;
        }
        ensureCIPnotNull(currentChannel);

        cip[currentChannel].setMinAndMax(ip.getMin(),ip.getMax());
        if (singleChannel && nChannels<=3) {
            switch (currentChannel) {
                case 0: cip[0].updateComposite(rgbPixels, 1); break;
                case 1: cip[1].updateComposite(rgbPixels, 2); break;
                case 2: cip[2].updateComposite(rgbPixels, 3); break;
            }
        } else {
            if (cip==null) return;
            if (syncChannels) {
                ImageProcessor ip2 = getProcessor();
                double min=ip2.getMin(), max=ip2.getMax();
                for (int i=0; i<nChannels; i++) {
                    cip[i].setMinAndMax(min, max);
                    lut[i].min = min;
                    lut[i].max = max;
                }
                syncChannels = false;
            }
            if (active[0])
                cip[0].updateComposite(rgbPixels, 4);
            else
            {for (int i=1; i<imageSize; i++) rgbPixels[i] = 0;}
            if (cip==null || nChannels>cip.length)
                return;
            for (int i=1; i<nChannels; i++)
                if (active[i]) {
                    ensureCIPnotNull(i);
                    cip[i].updateComposite(rgbPixels, 5);
                }
        }
        if (IJ.isJava16())
            createBufferedImage();
        else
            createImage();
        if (img==null && awtImage!=null)
            img = awtImage;
        singleChannel = false;
    }

    void createImage() {
        if (imageSource==null) {
            rgbCM = new DirectColorModel(32, 0xff0000, 0xff00, 0xff);
            imageSource = new MemoryImageSource(width, height, rgbCM, rgbPixels, 0, width);
            imageSource.setAnimated(true);
            imageSource.setFullBufferUpdates(true);
            awtImage = Toolkit.getDefaultToolkit().createImage(imageSource);
            newPixels = false;
        } else if (newPixels){
            imageSource.newPixels(rgbPixels, rgbCM, 0, width);
            newPixels = false;
        } else
            imageSource.newPixels();
    }

    /** Uses less memory but only works correctly with Java 1.6 and later. */
    void createBufferedImage() {
        if (rgbSampleModel==null)
            rgbSampleModel = getRGBSampleModel();
        if (rgbRaster==null) {
            DataBuffer dataBuffer = new DataBufferInt(rgbPixels, width*height, 0);
            rgbRaster = Raster.createWritableRaster(rgbSampleModel, dataBuffer, null);
        }
        if (rgbImage==null)
            rgbImage = new BufferedImage(rgbCM, rgbRaster, false, null);
        awtImage = rgbImage;
    }

    SampleModel getRGBSampleModel() {
        rgbCM = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
        WritableRaster wr = rgbCM.createCompatibleWritableRaster(1, 1);
        SampleModel sampleModel = wr.getSampleModel();
        sampleModel = sampleModel.createCompatibleSampleModel(width, height);
        return sampleModel;
    }

    public void setActiveChannel(int channel, boolean a, boolean update) {
        active[channel] = a;
        if(update) {
            updateAndDraw();
        }
        Channels.updateChannels();
    }

    /** Controls which channels in a composite color image are displayed,
     where 'channels' is a list of ones and zeros that specify the channels to
     display. For example, "101" causes channels 1 and 3 to be displayed. */
    public void setActiveChannels(String channels) {
        boolean[] active = ((CompositeImage)this).getActiveChannels();
        for (int i=0; i<active.length; i++) {
            boolean b = false;
            if (channels.length()>i && channels.charAt(i)=='1')
                b = true;
            active[i] = b;
        }
        updateAndDraw();
        Channels.updateChannels();
    }

	/*
	void createBlitterImage(int n) {
		ImageProcessor ip = cip[n-1].duplicate();
		if (ip instanceof FloatProcessor){
			FloatBlitter fb = new FloatBlitter((FloatProcessor)ip);
			for (int i=1; i<n; i++)
				fb.copyBits(cip[i], 0, 0, Blitter.COPY_ZERO_TRANSPARENT);
		} else if (ip instanceof ByteProcessor){
			ByteBlitter bb = new ByteBlitter((ByteProcessor)ip);
			for (int i=1; i<n; i++)
				bb.copyBits(cip[i], 0, 0, Blitter.OR);
		} else if (ip instanceof ShortProcessor){
			ShortBlitter sb = new ShortBlitter((ShortProcessor)ip);
			for (int i=n-2; i>=0; i--)
				sb.copyBits(cip[i], 0, 0, Blitter. OR);
		}
		img = ip.createImage();
		singleChannel = false;
	}
	*/

    @Override
    public void show() {
        super.show();
    }

    private boolean activated5d;

    @Override
    public void show(String statusMessage) {
        if (win!=null)
            return;

        if ((IJ.macroRunning() && IJ.getInstance()==null) || Interpreter.isBatchMode()) {
            WindowManager.setTempCurrentImage(this);
            Interpreter.addBatchModeImage(this);
            return;
        }

        activated5d = false;
        win = new MultichannelViewerWindow(this);
        if (roi!=null) roi.setImage(this);

        draw();
        IJ.showStatus(statusMessage);
        if (IJ.macroRunning()) { // wait for image to become activated
            //IJ.log("Waiting for image to be activated");
            long start = System.currentTimeMillis();
            while (!activated5d) {
                IJ.wait(5);
                if ((System.currentTimeMillis()-start)>2000) {
                    WindowManager.setTempCurrentImage(this);
                    break; // 2 second timeout
                }
            }
            //IJ.log(""+(System.currentTimeMillis()-start));
        }

        IJ.showStatus(statusMessage);

        notifyListeners(OPENED);
    }

    ImageStack getRGBStack(ImagePlus imp) {
        ImageProcessor ip = imp.getProcessor();
        int w = ip.getWidth();
        int h = ip.getHeight();
        int size = w*h;
        byte[] r = new byte[size];
        byte[] g = new byte[size];
        byte[] b = new byte[size];
        ((ColorProcessor)ip).getRGB(r, g, b);
        ImageStack stack = new ImageStack(w, h);
        stack.addSlice("Red", r);
        stack.addSlice("Green", g);
        stack.addSlice("Blue", b);
        stack.setColorModel(ip.getDefaultColorModel());
        return stack;
    }

    public LUT createLutFromColor(Color color) {
        return LUT.createLutFromColor(color);
    }

    LUT createLutFromBytes(byte[] bytes) {
        if (bytes==null || bytes.length!=768)
            return createLutFromColor(Color.white);
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        for (int i=0; i<256; i++) r[i] = bytes[i];
        for (int i=0; i<256; i++) g[i] = bytes[256+i];
        for (int i=0; i<256; i++) b[i] = bytes[512+i];
        return new LUT(r, g, b);
    }

    public Color getChannelColor() {
        if (lut==null || mode==GRAYSCALE)
            return Color.black;
        IndexColorModel cm = lut[getChannelIndex()];
        if (cm==null)
            return Color.black;
        int index = cm.getMapSize() - 1;
        int r = cm.getRed(index);
        int g = cm.getGreen(index);
        int b = cm.getBlue(index);
        //IJ.log(index+" "+r+" "+g+" "+b);
        if (r<100 || g<100 || b<100)
            return new Color(r, g, b);
        else
            return Color.black;
    }

    private void ensureCIPnotNull(int zeroBasedIndex){
        if(cip[zeroBasedIndex]==null){
            cip[zeroBasedIndex] = getStack().getProcessor(zeroBasedIndex+1);
            cip[zeroBasedIndex].setLut(lut[zeroBasedIndex]);
        }
    }

    public ImageProcessor getProcessor(int channel) {



        if (cip==null || channel>cip.length)
            return null;
        else {

            ensureCIPnotNull(channel-1);

            return cip[channel - 1];
        }
    }

    public boolean[] getActiveChannels() {
        return active;
    }

    public synchronized void setMode(int mode) {
        if(true){
            this.mode=COMPOSITE;

            Channels.updateChannels();
            return;
        }

        if (mode<COMPOSITE || mode>GRAYSCALE)
            return;
        if (mode==COMPOSITE && getNChannels()>MAX_CHANNELS)
            mode = COLOR;
        for (int i=0; i<MAX_CHANNELS; i++)
            active[i] = true;
        if (this.mode!=COMPOSITE && mode==COMPOSITE)
            img = null;
        this.mode = mode;
        if (mode==COLOR || mode==GRAYSCALE) {
            if (cip!=null) {
                for (int i=0; i<cip.length; i++) {
                    if (cip[i]!=null) {
                        cip[i].setPixels(null);
                    }
                    cip[i] = null;
                }
            }
            cip = null;
            rgbPixels = null;
            awtImage = null;
            currentChannel = -1;
        }
        if (mode==GRAYSCALE || mode==TRANSPARENT)
            ip.setColorModel(ip.getDefaultColorModel());

    }

    public int getMode() {
        return mode;
    }

    public String getModeAsString() {
        switch (mode) {
            case COMPOSITE: return "composite";
            case COLOR: return "color";
            case GRAYSCALE: return "grayscale";
        }
        return "";
    }

    /* Returns the LUT used by the specified channel. */
    public LUT getChannelLut(int channel) {
        int channels = getNChannels();
        if (lut==null) setupLuts(channels);
        if (channel<1 || channel>lut.length)
            throw new IllegalArgumentException("Channel out of range: "+channel);
        return lut[channel-1];
    }

    /* Returns the LUT used by the current channel. */
    public LUT getChannelLut() {
        int c = getChannelIndex();
        return lut[c];
    }

    /* Returns a copy of this image's channel LUTs as an array. */
    public LUT[] getLuts() {
        int channels = getNChannels();
        if (lut==null) setupLuts(channels);
        LUT[] luts = new LUT[channels];
        for (int i=0; i<channels; i++)
            luts[i] = (LUT)lut[i].clone();
        return luts;
    }

    /* Sets the channel LUTs with clones of the LUTs in 'luts'. */
    public void setLuts(LUT[] luts) {
        int channels = getNChannels();
        if (lut==null) setupLuts(channels);
        if (luts==null || luts.length<channels)
            throw new IllegalArgumentException("Lut array is null or too small");
        for (int i=0; i<channels; i++)
            setChannelLut(luts[i], i+1);
    }

    /** Copies the LUTs and display mode of 'imp' to this image. Does
     nothing if 'imp' is not a CompositeImage or 'imp' and this
     image do not have the same number of channels. */
    public synchronized void copyLuts(ImagePlus imp) {
        int channels = getNChannels();
        if (!imp.isComposite() || imp.getNChannels()!=channels)
            return;
        ij.CompositeImage ci = (ij.CompositeImage)imp;
        LUT[] luts = ci.getLuts();
        if (luts!=null && luts.length==channels) {
            lut = luts;
            cip = null;
        }
        int mode2 = ci.getMode();
        setMode(mode2);
        if (mode2==COMPOSITE) {
            boolean[] active2 = ci.getActiveChannels();
            for (int i=0; i<MAX_CHANNELS; i++)
                active[i] = active2[i];
        }
        if (ci.hasCustomLuts())
            customLuts = true;
    }

    int getChannelIndex() {
        int channels = getNChannels();
        if (lut==null) setupLuts(channels);
        int index = getChannel()-1;
        return index;
    }

    public void reset() {
        int nChannels = getNChannels();
        if (nChannels>MAX_CHANNELS && getMode()==COMPOSITE)
            setMode(COLOR);
        setup(nChannels, getImageStack());
    }

    /* Sets the LUT of the current channel. */
    public void setChannelLut(LUT table) {
        int c = getChannelIndex();
        double min = lut[c].min;
        double max = lut[c].max;
        lut[c] = table;
        lut[c].min = min;
        lut[c].max = max;
        if (mode==COMPOSITE && cip!=null && c<cip.length) {
            cip[c].setColorModel(lut[c] );
            imageSource = null;
            newPixels = true;
            img = null;
        }
        currentChannel = -1;
        getProcessor().setLut(table);
        customLuts = true;
        if (!IJ.isMacro()) ContrastAdjuster.update();
    }

    /* Sets the LUT of the specified channel using a clone of 'table'. */
    public synchronized void setChannelLut(LUT table, int channel) {
        int channels = getNChannels();
        if (lut==null) {
            setupLuts(channels);
        }
        if (channel<1 || channel>lut.length) {
            throw new IllegalArgumentException("Channel out of range");
        }
        lut[channel-1] = (LUT)table.clone();
        if (getWindow()!=null && channel==getChannel())
            getProcessor().setLut(lut[channel-1]);
        if (cip!=null && cip.length>=channel && cip[channel-1]!=null)
            cip[channel-1].setLut(lut[channel-1]);
        else
            cip = null;
        customLuts = true;
    }

    /* Sets the IndexColorModel of the current channel. */
    public void setChannelColorModel(IndexColorModel cm) {
        setChannelLut(new LUT(cm,0.0,0.0));
    }

    public void setDisplayRange(double min, double max) {
        ip.setMinAndMax(min, max);
        int c = getChannelIndex();
        lut[c].min = min;
        lut[c].max = max;
        if (getWindow()==null && cip!=null && c<cip.length)
            cip[c].setLut(lut[c]);
    }

    public double getDisplayRangeMin() {
        if (lut!=null)
            return lut[getChannelIndex()].min;
        else
            return 0.0;
    }

    public double getDisplayRangeMax() {
        if (lut!=null)
            return lut[getChannelIndex()].max;
        else
            return 255.0;
    }

    public void resetDisplayRange() {
        if (getType()==GRAY16 && getDefault16bitRange()!=0) {
            int defaultRange = getDefault16bitRange();
            for (int i=1; i<=getNChannels(); i++) {
                LUT lut = getChannelLut(i);
                lut.min = 0;
                lut.max = Math.pow(2,defaultRange)-1;
                if (getWindow()!=null)
                    setChannelLut(lut, i);
            }
        } else {
            ip.resetMinAndMax();
            int c = getChannelIndex();
            lut[c].min = ip.getMin();
            lut[c].max = ip.getMax();
        }
    }

    public boolean hasCustomLuts() {
        return customLuts && mode!=GRAYSCALE;
    }

    public void close() {
        super.close();
        rgbPixels = null;
        imageSource = null;
        awtImage = null;
        rgbRaster = null;
        rgbSampleModel = null;
        rgbImage = null;
        rgbCM = null;
        if (cip!=null) {
            for (int i=0; i<cip.length; i++)
                cip[i] = null;
            cip = null;
        }
        if (lut!=null) {
            for (int i=0; i<lut.length; i++)
                lut[i] = null;
            lut = null;
        }
        if (channelLuts!=null) {
            for (int i=0; i<channelLuts.length; i++)
                channelLuts[i] = null;
            channelLuts = null;
        }
    }

    /** Deprecated */
    public synchronized void setChannelsUpdated() {
        if (cip!=null) {
            for (int i=0; i<cip.length; i++) {
                if (cip[i]!=null) cip[i].setPixels(null);
                cip[i] = null;
            }
        }
        cip = null;
        lut = null;
        img = null;
        currentChannel = -1;
        previousChannel = 0;
        currentSlice = currentFrame = 1;
        singleChannel = false;
        rgbPixels = null;
        awtImage = null;
        channelLuts = null;
        boolean[] active = new boolean[MAX_CHANNELS];
    }

}

