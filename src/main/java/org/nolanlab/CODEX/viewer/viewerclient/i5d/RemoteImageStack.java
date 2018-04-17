package org.nolanlab.CODEX.viewer.viewerclient.i5d;

//
// RemoteImageStack.java
//

/*
Written by Nikolay Samusik, Stanford University, (c) 2018
 */


        import com.google.gson.Gson;
        import ij.IJ;
        import ij.ImagePlus;
        import ij.VirtualStack;
        import ij.process.ImageProcessor;
        import org.nolanlab.CODEX.utils.RequestHandler;

        import java.awt.image.ColorModel;
        import java.io.IOException;
        import java.util.*;


/**
 * This class represents an array of disk-resident images.
 */
public class RemoteImageStack extends VirtualStack {

    private String[] URLs;

    private String path;
    private String host;

    public RemoteImageStack(String host, String pathToImageList) throws IOException{
       path = pathToImageList;
       this.host = host;
       String re = new String(RequestHandler.get(host + "/" + pathToImageList));

       List<String> lst = new Gson().fromJson(re, ArrayList.class);

       URLs = lst.toArray(new String[lst.size()]);
       this.setColorModel(ColorModel.getRGBdefault());
    }


    @Override
    public void addSlice(final String sliceLabel, final Object pixels) {}

    @Override
    public void addSlice(final String sliceLabel, final ImageProcessor ip) {}


    @Override
    public void addSlice(final String sliceLabel, final ImageProcessor ip, final int n) {}


    @Override
    public void deleteSlice(final int n) {}

    @Override
    public void deleteLastSlice() {}

    /**
     * Returns the pixel array for the specified slice, where {@code 1<=n<=nslices}.
     */
    @Override
    public Object getPixels(final int n) {
        final ImageProcessor ip = getProcessor(n);
        if (ip != null) return ip.getPixels();
        return null;
    }

    /**
     * Assigns a pixel array to the specified slice, where {@code 1<=n<=nslices}.
     */
    @Override
    public void setPixels(final Object pixels, final int n) {}



    ArrayList<Integer> cacheIdx = new ArrayList<>();
    HashMap<Integer,ImageProcessor> cache = new HashMap<>();

    private static final int MAX_CACHE = 10;

    /**
     * Returns an ImageProcessor for the specified slice, where
     * {@code 1<=n<=nslices}. Returns null if the stack is empty.
     */
    @Override
    public ImageProcessor getProcessor(final int n) {

        if(cache.get(n)!=null){
            return cache.get(n);
        }

        String path = host+"/"+URLs[n-1];

        System.out.println("Loading image:"+path);

//        Thread.dumpStack();
        ImagePlus imagePlus  =IJ.openImage(path);
        if(imagePlus== null){
            throw new IllegalStateException("Image could not be opened:" + path);
        }

        final ImageProcessor lp  = imagePlus.getProcessor();


        if(getColorModel()==null){
            setColorModel(lp.getColorModel());
        }

        cacheIdx.add(n);
        cache.put(n,lp);
        if(cacheIdx.size()>MAX_CACHE){
           cache.remove(cacheIdx.remove(0));
        }

        return lp;
    }

    /** Returns the directory of the stack. */
    public String getPath() {
        return path;
    }

    /** Returns the number of slices in this stack. */
    @Override
    public int getSize() {
        return URLs.length;
    }

    /** Returns the file name of the Nth image. */
    @Override
    public String getSliceLabel(final int n) {
        return URLs[n - 1];
    }

    /** Returns null. */
    @Override
    public Object[] getImageArray() {
        return null;
    }

    @Override
    public void setSliceLabel(final String label, final int n) {}

    /** Always return true. */
    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public void trim() {}

}