/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nolanlab.CODEX.uploader.uplserver.driffta;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.plugin.ChannelSplitter;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;
import org.nolanlab.CODEX.uploader.uplclient.Experiment;
import org.nolanlab.CODEX.utils.codexhelper.ExperimentHelper;
import org.nolanlab.CODEX.utils.logger;

import java.awt.*;

import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.nolanlab.CODEX.uploader.uplserver.driffta.Driffta.log;

/**
 *
 * @author CODEX
 */
public class BestFocus {

    public static ImagePlus createBestFocusStackFromHyperstack(ImagePlus imp, int[] bestFocusZIndices) {

        logger.print("Best focus on stack: " + imp.getTitle());

        ImagePlus[] stack = new ImagePlus[imp.getNFrames()];
        ExecutorService es = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
        List<Callable<Entry<Integer, ImagePlus>>> fut = new ArrayList<>();
        Duplicator dup = new Duplicator();

        for (int frame = 1; frame <= imp.getNFrames(); frame++) {
            final int fr = frame;
            final ImagePlus tp = dup.run(imp, 1, imp.getNChannels(), 1, imp.getNSlices(), fr, fr);
            int z= bestFocusZIndices[fr-1];
            stack[fr-1] = retrieveFocusedPlane(tp, z);
        }

        ImagePlus focused = new Concatenator().concatenate(stack, false);
        ImagePlus hyp = HyperStackConverter.toHyperStack(focused, imp.getNChannels(), 1, imp.getNFrames(), "xyczt", "composite");
        if (hyp.getNChannels() == 4) {
            ((CompositeImage) hyp).setLuts(new LUT[]{LUT.createLutFromColor(Color.WHITE), LUT.createLutFromColor(Color.RED), LUT.createLutFromColor(Color.GREEN), LUT.createLutFromColor(new Color(0, 70, 255))});
        }

        return hyp;
    }

    public static int findBestFocusStackFromSingleTimepoint(ImagePlus imp, int focusChannel, boolean optionalFocusFragment) {
        Find_focused_slices plg = new Find_focused_slices();
        ImageStack ch = ChannelSplitter.getChannel(imp, focusChannel);
        imp = new ImagePlus("ch" + focusChannel, ch);
        ImageStatistics is = imp.getStatistics();
        int yStep = (imp.getHeight() / 4) + 1;
        int xStep = (imp.getWidth() / 4) + 1;
        ArrayList<Integer> al = new ArrayList<>();
        if(optionalFocusFragment) {
            for (int x = 0; x < imp.getWidth(); x += xStep) {
                for (int y = 0; y < imp.getHeight(); y += yStep) {
                    ImageStack out = null;
                    for (int i = 1; i <= imp.getNSlices(); i++) {
                        ImageProcessor ip = imp.getStack().getProcessor(i);
                        ip.setRoi(x, y, xStep, yStep);
                        ImageProcessor cropped;
                        cropped = ip.crop();
                        if (out == null) {
                            out = new ImageStack(cropped.getWidth(), cropped.getHeight());
                        }
                        out.addSlice("slice" + i, cropped);
                    }

                    ImagePlus tmp = new ImagePlus("tmp_crop", out);
                    ImageStatistics isTmp = tmp.getStatistics();

                    if (isTmp.mean > is.mean - is.stdDev) {
                        plg.setup("select=100 variance=0.000", tmp);
                        int z = plg.run(null);
                        if (z == 0) {
                            double maxIntens = 0;
                            int maxZ = 0;
                            for (int i = 1; i <= imp.getNSlices(); i++) {
                                ImageProcessor ip = imp.getStack().getProcessor(i);
                                ImageStatistics isTmp2 = ip.getStatistics();
                                if (isTmp2.mean > maxIntens) {
                                    maxIntens = isTmp2.mean;
                                    maxZ = i;
                                }
                            }
                            z = maxZ;
                        }
                        logger.print("trying subtile x=" + x + ", y=" + y + ", best_slice=" + z);
                        al.add(z);
                    }
                }
            }

            int bestSlice = imp.getNSlices() / 2;

            if (al.size() > 0) {
                Collections.sort(al);
                bestSlice = al.get(al.size() / 2);
            }

            logger.print("best Z = " + bestSlice);
            return bestSlice;
        }
        else{
            plg.setup("select=100 variance=0.000", imp);
            int z = plg.run(null);
            if (z == 0) {
                double maxIntens = 0;
                int maxZ = 0;
                for (int i = 1; i <= imp.getNSlices(); i++) {
                    ImageProcessor ip = imp.getStack().getProcessor(i);
                    ImageStatistics isTmp2 = ip.getStatistics();
                    if (isTmp2.mean > maxIntens) {
                        maxIntens = isTmp2.mean;
                        maxZ = i;
                    }
                }
                z = maxZ;
            }
            return z;
        }
    }

    public static ImagePlus retrieveFocusedPlane(ImagePlus imp, int z) {
        ImageStack out = new ImageStack(imp.getWidth(), imp.getHeight());

        for (int i = 1; i <= imp.getNChannels(); i++) {
            ImageStack s = ChannelSplitter.getChannel(imp, i);
            out.addSlice("ch" + i, s.getProcessor(z));
        }

        ImagePlus ret = new ImagePlus(imp.getTitle() + "bestFocus", out);
        ImagePlus hyp = HyperStackConverter.toHyperStack(ret, imp.getNChannels(), 1, 1, "xyczt", "composite");

        return hyp;
    }

    public static void makeBestFocus(String user, String expName) {
        String serverConfig = System.getProperty("user.dir") + File.separator + "data";
        File expFolder = new File(serverConfig + File.separator + user + File.separator + expName);
        String bestFocusLoc = expFolder.getPath()+File.separator+ "processed" + File.separator + "bestFocus";
        File bf = new File(bestFocusLoc);
        ExperimentHelper experimentHelper = new ExperimentHelper();
        if(!bf.exists()) {
            log("Best focus folder is not present. Running it for all the tiffs inside the processed folder.");
            File mkBestFocus = new File(bestFocusLoc);
            mkBestFocus.mkdir();
            File processed = bf.getParentFile();
            if(processed.isDirectory()) {
                File[] procTiff = processed.listFiles(fName -> (fName.getName().endsWith(".tiff") || fName.getName().endsWith(".tif")));
                File expJsonFile = new File(processed.getPath() + File.separator + "Experiment.json");
                if (!expJsonFile.exists()) {
                    throw new IllegalStateException("Experiment JSON file not found: " + expJsonFile);
                }
                try {
                    Experiment exp = experimentHelper.loadFromJSON(expJsonFile);
                    for (File aTif : procTiff) {
                        ImagePlus p = IJ.openImage(aTif.getPath());
                        int[] bestFocusPlanes = new int[p.getNFrames()];
                        Duplicator dup = new Duplicator();
                        ImagePlus rp = dup.run(p, exp.getBest_focus_channel(), exp.getBest_focus_channel(), 1, p.getNSlices(), exp.getBestFocusReferenceCycle() - exp.getCycle_lower_limit() + 1, exp.getBestFocusReferenceCycle() - exp.getCycle_lower_limit() + 1);
                        int refZ = Math.max(1, BestFocus.findBestFocusStackFromSingleTimepoint(rp, 1, exp.isOptionalFocusFragment()));

                        //Add offset here
                        refZ = refZ + exp.getFocusing_offset();
                        Arrays.fill(bestFocusPlanes, refZ);

                        ImagePlus focused = BestFocus.createBestFocusStackFromHyperstack(p, bestFocusPlanes);
                        log("Saving the focused tiff " + aTif.getName() + "where Z: " + bestFocusPlanes[0]);
                        FileSaver fs = new FileSaver(focused);
                        fs.saveAsTiff(bestFocusLoc + File.separator + experimentHelper.getDestStackFileNameWithZIndexForTif(exp.getTiling_mode(), aTif.getName(), bestFocusPlanes[0]));
                    }
                }
                catch (Exception e) {
                    log(e.getMessage());
                }
            }
        }
        else {
            log("Best Focus folder was already created from Driffta...");
        }
    }
}
