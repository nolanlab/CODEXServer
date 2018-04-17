/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nolanlab.CODEX.uploader.uplserver.driffta;


import fiji.stacks.Hyperstack_rearranger;
import ij.*;
import ij.plugin.*;
import ij.process.ImageConverter;
import ij.process.LUT;
import org.apache.commons.io.FilenameUtils;
import org.nolanlab.CODEX.utils.codexhelper.BestFocusHelper;
import org.nolanlab.CODEX.utils.codexhelper.ExperimentHelper;
import org.nolanlab.CODEX.utils.logger;
import org.nolanlab.CODEX.uploader.uplclient.Experiment;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Nikolay
 */
public class Driffta {

    private static final int version = 12;
    private static Deconvolve_mult dec;
    private static final boolean copy = false;
    private static boolean color = false;
    private static HashMap<String, Double> expVsMs = new HashMap<>();
    private static LinkedHashMap<String, Integer> tileVsBf = new LinkedHashMap<>();

    public static void drifftaProcessing(String user, String expName, String r, String t) throws Exception {

        ExperimentHelper expHelper = new ExperimentHelper();
        BestFocusHelper bfHelper = new BestFocusHelper();

        String serverConfig = System.getProperty("user.dir") + File.separator + "data";

        Properties config = new Properties();
        config.load(new FileInputStream(System.getProperty("user.home") + File.separator + "config.txt"));
        final String TMP_SSD_DRIVE = config.get("TMP_SSD_DRIVE").toString();
        final String numGPUs = config.get("numGPU").toString();

        String baseDir = TMP_SSD_DRIVE + File.separator + user + File.separator + expName;
        String outDir = serverConfig + File.separator + user + File.separator + expName + File.separator + "processed" + File.separator + "tiles";

        File userExpDir = new File(outDir);
        if(!userExpDir.exists()) {
            userExpDir.mkdirs();
        }

        //copy experiment.json file to experiment folder
        if(!(new File(userExpDir.getParentFile().getParentFile() + File.separator + "experiment.json")).exists()) {
            copyFileFromSourceToDest(new File(baseDir + File.separator + "experiment.json"), userExpDir.getParentFile().getParentFile());
        }

        createConfigFilesInsideProcessedDir(baseDir, userExpDir.getParentFile().getParentFile());

        final Experiment exp = expHelper.loadFromJSON(new File(baseDir + File.separator + "Experiment.json"));

        final int region = Integer.parseInt(r);
        final int tile = Integer.parseInt(t);
        
        File d;
        //if (!exp.isExportImgSeq()) {
//            d = new File(serverConfig + File.separator + exp.getUsername() + File.separator + exp.getName() + File.separator + "processed" + File.separator + "tiles" + File.separator + expHelper.getDestStackFileName(exp.getTiling_mode(), tile, region, exp.getRegion_width()));
//        } else {
            d = new File(serverConfig + File.separator + exp.getUserName() + File.separator + exp.getName() + File.separator + "processed" + File.separator + "tiles" + File.separator + FilenameUtils.removeExtension(expHelper.getDestStackFileName(exp.getTiling_mode(), tile, region, exp.getRegion_width())));
        //}

        if (!d.exists()) {

            try {
                log("Starting drift compensation. Version " + version);

                color = exp.getChannel_arrangement().toLowerCase().trim().equals("color");

                if (!exp.getDeconvolution().equals("Microvolution")) {
                    log("Deconvolution disabled based on Experiment.json");
                }
                final int numDeconvolutionDevices = (!StringUtils.isBlank(numGPUs) ? Integer.parseInt(numGPUs) : 0);

                File chNamesFile = new File(baseDir + File.separator + "channelNames.txt");
                ArrayList<String> chNamesAL = new ArrayList<>();
                try {
                    BufferedReader br = new BufferedReader(new FileReader(chNamesFile));
                    String s = null;
                    while ((s = br.readLine()) != null) {
                        chNamesAL.add(s);
                    }
                    br.close();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                final String[] channelNames = chNamesAL.toArray(new String[chNamesAL.size()]);

                int[][] deconvIterations = new int[exp.getNum_cycles()][exp.getChannel_names().length];

                for (int i = 0; i < deconvIterations.length; i++) {
                    for (int j = 0; j < deconvIterations[i].length; j++) {
                        if (i == 0 || j != exp.getDrift_comp_channel() - 1) {
                            if (i == exp.getNum_cycles() - 1 && exp.isHandEstain()) {
                                continue;
                            }
                            deconvIterations[i][j] = 25;
                        }
                    }
                }


                ImagePlus imp = null;

                //Read stack array from the uploader cache directory.
                log("Opening files");
                final ImagePlus[] stack = new ImagePlus[exp.getNum_cycles() * exp.getNum_z_planes() * exp.getChannel_names().length];

                File upCache = new File(baseDir);
                if(!upCache.exists() || !upCache.isDirectory()) {
                    return;
                }

                File[] tifFiles = upCache.listFiles(tif -> tif.getName().endsWith(".tif") || tif.getName().endsWith("tiff"));
                if(stack.length != tifFiles.length) {
                    throw new IllegalStateException("Input files and stack size are not equal. Uploader Cache must be cleared on server side");
                }
                for(int i=0; i < stack.length; i++) {
                    stack[i] = IJ.openImage(tifFiles[i].getPath());
                }

                if (exp.isHandEstain()) {
                    int cycle = exp.getNum_cycles();
                    for (int zSlice = 1; zSlice <= exp.getNum_z_planes(); zSlice++) {
                        int ch = -1;
                        for (int chIdx = 0; chIdx < exp.getChannel_names().length; chIdx++) {
                            int idx = ((exp.getNum_z_planes() * exp.getChannel_names().length) * (cycle - 1)) + (exp.getChannel_names().length * (zSlice - 1)) + chIdx;
                            if (stack[idx] != null) {
                                ch = chIdx;
                            }
                        }
                        if (ch == -1) {
                            throw new IllegalStateException("H&E image slice is absent for z=" + zSlice);
                        }

                        int idx = ((exp.getNum_z_planes() * exp.getChannel_names().length) * (cycle - 1)) + (exp.getChannel_names().length * (zSlice - 1)) + ch;

                        ImagePlus he = stack[idx];

                        if (he.getBitDepth() != 24) {
                            throw new IllegalStateException("Expected a 24-bit RGB image");
                        }

                        //ImagePlus he_R = he.getImageStack();
                        int numCh = exp.getChannel_names().length;

                        String[] colorNames = new String[]{"R", "G", "B"};

                        ImageConverter ic = new ImageConverter(he);
                        ic.convertToRGBStack();
                        ic.convertToGray16();

                        int k = 1;
                        for (int i = numCh - 3; i < numCh; i++) {
                            idx = ((exp.getNum_z_planes() * exp.getChannel_names().length) * (cycle - 1)) + (exp.getChannel_names().length * (zSlice - 1)) + i;
                            ImagePlus he_S = new ImagePlus("HandE_" + colorNames[i - 1], he.getStack().getProcessor(k++).duplicate());
                            stack[idx] = he_S;
                            stack[idx].getProcessor().multiply(250);
                        }

                        int driftCh = exp.getDrift_comp_channel();
                        ImagePlus he_R = new ImagePlus("HandE_R_inv", he.getStack().getProcessor(1).duplicate());
                        he_R.getProcessor().invert();
                        idx = ((exp.getNum_z_planes() * exp.getChannel_names().length) * (cycle - 1)) + (exp.getChannel_names().length * (zSlice - 1)) + (driftCh - 1);
                        stack[idx] = he_R;
                    }
                }
                imp = new Concatenator().concatenate(stack, false);

                final double XYres = exp.getPer_pixel_XY_resolution();
                final double Zpitch = exp.getZ_pitch();

                final int[] wavelenghths = exp.getEmission_wavelengths();
                //Alt impl

                ImagePlus hyp = HyperStackConverter.toHyperStack(imp, exp.getChannel_names().length, exp.getNum_z_planes(), exp.getNum_cycles(), "xyczt", "composite");

                imp = null;

                //run best focus
                Duplicator dup = new Duplicator();
                //log("Value of hyp: " + hyp);
                int[] bestFocusPlanes = new int[hyp.getNFrames()];

                ImagePlus rp = dup.run(hyp, exp.getBest_focus_channel(), exp.getBest_focus_channel(), 1, hyp.getNSlices(), exp.getBestFocusReferenceCycle() - exp.getCycle_lower_limit() + 1, exp.getBestFocusReferenceCycle() - exp.getCycle_lower_limit() + 1);
                int refZ = Math.max(1, BestFocus.findBestFocusStackFromSingleTimepoint(rp, 1, exp.isOptionalFocusFragment()));
                //Add offset here
                refZ = refZ + exp.getFocusing_offset();
                Arrays.fill(bestFocusPlanes, refZ);
                //log("The bestZ plane: "+ refZ);

                log("Drift compensation");
                log("Waiting for driftcomp interlock");
                DriftcompInterlockDispatcher.gainLock();
                log("Interlock acquired");
                Driftcomp.compensateDrift(hyp, exp.getDrift_comp_channel() - 1, exp.getDriftCompReferenceCycle() - 1);

                DriftcompInterlockDispatcher.releaseLock();

                log("Cropping");

                int Y = Integer.parseInt(expHelper.getDestStackFileName(exp.getTiling_mode(), tile, region, exp.getRegion_width()).split("Y")[1].split("\\.")[0]);
                int X = Integer.parseInt(expHelper.getDestStackFileName(exp.getTiling_mode(), tile, region, exp.getRegion_width()).split("X")[1].split("_")[0]);

                if (exp.isUseBleachMinimizingCrop()) {
                    hyp = (Y % 2 == 1) ? new ImagePlus(hyp.getTitle(), hyp.getImageStack().crop(X == 1 ? 0 : exp.getTile_overlap_X(), Y == 1 ? 0 : exp.getTile_overlap_Y(), 0, hyp.getWidth() - (X == 1 ? 0 : exp.getTile_overlap_X()), hyp.getHeight() - (Y == 1 ? 0 : exp.getTile_overlap_Y()), hyp.getStackSize()))
                            : new ImagePlus(hyp.getTitle(), hyp.getImageStack().crop(0, exp.getTile_overlap_Y(), 0, hyp.getWidth() - (X == exp.getRegion_width() ? 0 : exp.getTile_overlap_X()), hyp.getHeight() - exp.getTile_overlap_Y(), hyp.getStackSize()));
                } else {
                    hyp = new ImagePlus(hyp.getTitle(), hyp.getImageStack().crop((int) Math.floor(exp.getTile_overlap_X() / 2), (int) Math.floor(exp.getTile_overlap_Y() / 2), 0, hyp.getWidth() - (int) Math.ceil(exp.getTile_overlap_X()), hyp.getHeight() - (int) Math.ceil(exp.getTile_overlap_Y()), hyp.getStackSize()));
                }

                hyp = HyperStackConverter.toHyperStack(hyp, exp.getChannel_names().length, exp.getNum_z_planes(), exp.getNum_cycles(), "xyczt", "composite");

                log("Running deconvolution");

                double ObjectiveRI = 1.0;

                if ("oil".equals(exp.getObjectiveType())) {
                    ObjectiveRI = 1.5115;
                }

                if ("water".equals(exp.getObjectiveType())) {
                    ObjectiveRI = 1.33;
                }

                log("Waiting for deconvolution interlock");
                DeconvolutionInterlockDispatcher.gainLock();
                log("Interlock acquired");
                dec = new Deconvolve_mult(!exp.getDeconvolution().equals("Microvolution"), numDeconvolutionDevices, exp.isUseBlindDeconvolution());

                dec.runDeconvolution(hyp, XYres, Zpitch, wavelenghths, deconvIterations, exp.getDrift_comp_channel() - 1, exp.getNumerical_aperture(), ObjectiveRI);

                DeconvolutionInterlockDispatcher.releaseLock();

                if (hyp.getNChannels() == 4) {
                    ((CompositeImage) hyp).setLuts(new LUT[]{LUT.createLutFromColor(Color.WHITE), LUT.createLutFromColor(Color.RED), LUT.createLutFromColor(Color.GREEN), LUT.createLutFromColor(new Color(0, 70, 255))});
                } else if (hyp.getNChannels() == 3) {
                    ((CompositeImage) hyp).setLuts(new LUT[]{LUT.createLutFromColor(Color.RED), LUT.createLutFromColor(Color.GREEN), LUT.createLutFromColor(new Color(0, 70, 255))});
                }

                //Do background subtraction if needed
                ImagePlus reorderedHyp = null;
                if (exp.isBgSub()) {
                    reorderedHyp = backgroundSubtraction(hyp, exp, baseDir, channelNames);
                }

                //Save prcessed files as normal tiffs or image sequence
//                if (!exp.isExportImgSeq()) {
//                    String outStr = outDir + File.separator + expHelper.getDestStackFileName(exp.getTiling_mode(), tile, region, exp.getRegion_width());
//                    log("Saving result tiff file: " + outStr);
//                    IJ.saveAsTiff(hyp, outStr);
//                } else {
                    if (reorderedHyp == null) {
                        String outStr = outDir + File.separator + FilenameUtils.removeExtension(expHelper.getDestStackFileName(exp.getTiling_mode(), tile, region, exp.getRegion_width()));
                        File out = new File(outStr);
                        if (!out.exists()) {
                            out.mkdir();
                        }

                        log("Saving result file as image sequence: " + outStr);
                        hyp.setTitle(FilenameUtils.removeExtension(expHelper.getDestStackFileName(exp.getTiling_mode(), tile, region, exp.getRegion_width())));
                        IJ.run(hyp, "Image Sequence... ", "format=TIFF save=" + outStr);
                    }
                    else{
                        String outStr = outDir + File.separator + FilenameUtils.removeExtension(expHelper.getDestStackFileName(exp.getTiling_mode(), tile, region, exp.getRegion_width()));
                        File out = new File(outStr);
                        if (!out.exists()) {
                            out.mkdir();
                        }
                        log("Saving result file as image sequence: " + outStr);
                        reorderedHyp.setTitle(FilenameUtils.removeExtension(expHelper.getDestStackFileName(exp.getTiling_mode(), tile, region, exp.getRegion_width())));
                        IJ.run(reorderedHyp, "Image Sequence... ", "format=TIFF save=" + outStr);
                    }
//                }

//                String bestFocus = outDir + File.separator + "bestFocus" + File.separator;
//
//                File bfFile = new File(bestFocus);
//                if (!bfFile.exists()) {
//                    bfFile.mkdirs();
//                }
//
//                log("Running best focus");
//                ImagePlus focused = BestFocus.createBestFocusStackFromHyperstack(hyp, bestFocusPlanes);
//                log("Saving the focused tiff");
//                IJ.saveAsTiff(focused, bestFocus + File.separator + expHelper.getDestStackFileNameWithZIndex(exp.getTiling_mode(), tile, region, exp.getRegion_width(), bestFocusPlanes[0]));

                File bestFocusFile = new File(outDir + File.separator + "bestFocus.json");
                tileVsBf.put(FilenameUtils.removeExtension(expHelper.getDestStackFileName(exp.getTiling_mode(), tile, region, exp.getRegion_width())), bestFocusPlanes[0]);
                bfHelper.saveToFile(tileVsBf, bestFocusFile);

                WindowManager.closeAllWindows();

                exp.setTile_width(hyp.getWidth());
                exp.setTile_height(hyp.getHeight());

                log("All files opened. Deleting temporary dir");
                //new Thread(() -> org.scijava.util.FileUtils.deleteRecursively(new File(outDir))).start();
                delete(new File(baseDir));
            } catch (Exception e) {
                e.printStackTrace();
                if (logStream != null) {
                    e.printStackTrace(logStream);
                    logStream.flush();
                    logStream.close();
                }
                System.exit(2);
            }
        } else {
            log("Processed files already exist. Skipping driffta...");
            delete(new File(baseDir));
        }
    }

    private static void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                delete(c);
            }
        }
        if (f.exists() && !f.delete()) {
            throw new IOException("File: " + f + " not removed. It is currently being used!");
        }
    }

    private static File logFile = null;
    private static PrintStream logStream;

    public static void log(String s) {
        logger.print(s);
    }


    @Override
    protected void finalize() throws Throwable {
        if (logStream != null) {
            logStream.println("writtenFromFinalize");
            logStream.flush();
            logStream.close();
        }
        DriftcompInterlockDispatcher.releaseLock();
        DeconvolutionInterlockDispatcher.releaseLock();
    }

    /**
     * Method to parse the content of exposureTimes.txt and store in a 2D String matrix
     * @param exposureTimes
     * @return
     * @throws IOException
     */
    private static String[][] parseExposureTimesTxtFile(File exposureTimes) throws IOException {
        Scanner in = new Scanner(exposureTimes);
        List<String[]> lines = new ArrayList<>();
        while(in.hasNextLine()) {
            String line = in.nextLine().trim();
            String[] splitStr = line.split("\\t");
            lines.add(splitStr);
        }

        String[][] result = new String[lines.size()][];
        for(int i = 0; i<result.length; i++) {
            result[i] = lines.get(i);
        }

        return result;
    }

    /**
     * Preserving order by iterating through channel entries from channelNames field in experiment.json
     * @param expTimes
     * @param chNames
     * @return
     */
    private static HashMap<Integer, List<String>> createOrderlyMapForExpTimes(String[][] expTimes, String[] chNames) {
        LinkedHashMap<Integer, List<String>> chVsExp = new LinkedHashMap<>();
        for(String ch: chNames) {
            List firstRow = Arrays.asList(expTimes[0]);
            int index = firstRow.indexOf(ch);
            if(index != -1) {
                for(int i=0; i<expTimes.length; i++){
                    if (!chVsExp.containsKey(Arrays.asList(chNames).indexOf(ch) + 1)) {
                        List<String> expTimesForAChannel = new ArrayList<>();
                        chVsExp.put(Arrays.asList(chNames).indexOf(ch) + 1, expTimesForAChannel);
                    }
                    if(i != 0) {
                        chVsExp.get(Arrays.asList(chNames).indexOf(ch) + 1).add(expTimes[i][index]);
                    }
                }
            }
        }
        return chVsExp;
    }

    /**
     * Method to identify the channel-cycle that contains the blank cycles
     * Returns a list of blank cycles for every channel if present
     * @param channelNames
     * @param exp
     * @return
     */
    private static HashMap<Integer, List<Integer>> findBlankCyclesAndChannels(String[] channelNames, Experiment exp) {
        LinkedHashMap<Integer, List<Integer>> chVsCyc = new LinkedHashMap<>();
        for(int i=0; i<channelNames.length; i++) {
            int channels_count = exp.getChannel_names().length;
            if(channelNames[i].contains("blank")) {
                int ch = (i%channels_count) + 1;
                //String ch = exp.channel_names[(i%channels_count)];
                int cycle = (i/channels_count) + 1;
                if (!chVsCyc.containsKey(ch)) {
                    List<Integer> cycList = new ArrayList<>();
                    chVsCyc.put(ch, cycList);
                }
                chVsCyc.get(ch).add(cycle);
            }
        }
        return chVsCyc;
    }

    /**
     * Method to find the channel-cycle with the highest exposure time for blank channels/cycles
     * Always returs one cycle with max exposure for one channel that has a blank cycle
     * @param expTimesForEveryCh
     * @param blankCyclesForEveryCh
     * @return
     */
    private static HashMap<Integer, Integer> getHighestExpCycForEveryChannel(HashMap<Integer, List<String>> expTimesForEveryCh, HashMap<Integer, List<Integer>> blankCyclesForEveryCh) {
        LinkedHashMap<Integer, Integer> maxExpTimeChVsCyc = new LinkedHashMap<>();
        for(Map.Entry<Integer, List<Integer>> blankCycEntry : blankCyclesForEveryCh.entrySet()) {
            List<Integer> blankCycForACh = blankCycEntry.getValue();
            int firstBlankCycIndex = blankCycForACh.get(0) - 1;
            List<String> expTimesForACh = expTimesForEveryCh.get(blankCycEntry.getKey());

            int cycForMaxExp = blankCycForACh.get(0);
            double maxExp = Double.parseDouble(expTimesForACh.get(firstBlankCycIndex));

            for(int i = 1; i<blankCycForACh.size(); i++) {
                double newMaxExp = Double.parseDouble(expTimesForACh.get(blankCycForACh.get(i) - 1));
                if(newMaxExp > maxExp) {
                    maxExp = newMaxExp;
                    cycForMaxExp = blankCycForACh.get(i);
                }
            }
            maxExpTimeChVsCyc.put(blankCycEntry.getKey(), cycForMaxExp);
        }
        return maxExpTimeChVsCyc;
    }

    /**
     * Method to perform background subtraction to eliminate noise from the processed image after drift compensation and deconvolution
     * @param hyp
     * @param exp
     * @param baseDir
     * @param channelNames
     * @throws IOException
     */
    private static ImagePlus backgroundSubtraction(ImagePlus hyp, Experiment exp, String baseDir, String[] channelNames) throws IOException {
        Duplicator dup = new Duplicator();

        File exposureTimesFile = new File(baseDir + File.separator + "exposure_times.txt");
        if(!exposureTimesFile.exists()) {
            throw new IllegalStateException("exposure_times.txt file not present. This is required for background subtraction to eliminate noise. Try again!");
        }
        String[][] exposureTimes = parseExposureTimesTxtFile(exposureTimesFile);
        populateExposureTimesMap();

        for(int i=0; i<exposureTimes.length; i++) {
            for(int j=0; j<exposureTimes[0].length; j++) {
                if(expVsMs.containsKey(exposureTimes[i][j])) {
                    exposureTimes[i][j] = String.valueOf(expVsMs.get(exposureTimes[i][j]));
                }
            }
        }

        HashMap<Integer, List<String>> expTimesMapForChannels = createOrderlyMapForExpTimes(exposureTimes, exp.getChannel_names());
        HashMap<Integer, List<Integer>> blankCycMapForChannels = findBlankCyclesAndChannels(channelNames, exp);
        HashMap<Integer, Integer> maxExpChVsCyc = getHighestExpCycForEveryChannel(expTimesMapForChannels, blankCycMapForChannels);

        ArrayList<ImagePlus> stacks = new ArrayList<>();

        for(int ch = 1; ch <= hyp.getNChannels(); ch++) {
            boolean sub = maxExpChVsCyc.containsKey(ch);

            //Get zSlices stack for the channel-cycle that has maximum exposure time.
            ImagePlus maxBlankStack =  sub ? dup.run(hyp, ch, ch, 1, hyp.getNSlices(), maxExpChVsCyc.get(ch), maxExpChVsCyc.get(ch)):null;

            //Iterate over the stacks
            for(int fr=0; fr<hyp.getNFrames(); fr++) {
                if(!sub) {
                    ImagePlus st = dup.run(hyp, ch, ch, 1, hyp.getNSlices(), fr+1, fr+1);
                    stacks.add(st);
                    continue;
                }
                //Get a list of all exposure times for this channel(for all cycles)
                List<String> expTimes = expTimesMapForChannels.get(ch);

                //find the exposure time that corresponds to the blank cycle
                double expBlankStack = Double.parseDouble(expTimes.get(maxExpChVsCyc.get(ch)-1));

                //find the exposure time that corresponds to the current stack
                double expStack = Double.parseDouble(expTimes.get(fr));

                double r = expStack/expBlankStack;

                //multiply the blank stack image processor with the factor r
                maxBlankStack.getProcessor().multiply(r);

                ImageCalculator ic = new ImageCalculator();

                //Subtract the blank stack from the current stack and store it in the current
                ImagePlus st = ic.run("Subtract create stack",  dup.run(hyp, ch, ch, 1, hyp.getNSlices(), fr+1, fr+1), maxBlankStack);
                stacks.add(st);
            }
        }

        ImagePlus concatenatedStacks = new Concatenator().concatenate(stacks.toArray(new ImagePlus[stacks.size()]), false);
        ImagePlus newHyp = HyperStackConverter.toHyperStack(concatenatedStacks, hyp.getNChannels(), hyp.getNSlices(), hyp.getNFrames(), "xyztc", "composite");
        ImagePlus reorderedHyp = Hyperstack_rearranger.reorderHyperstack(newHyp, "CZT", false, false);

        return reorderedHyp;
    }

    /**
     * Method to populate the exposure times map which is used to store exposure time in ms
     */
    private static void populateExposureTimesMap() {
        expVsMs.put("skip", (double)0);
        expVsMs.put("1/7500s", (double)1000 * 1/7500);
        expVsMs.put("1/5500s", (double)1000 * 1/5500);
        expVsMs.put("1/4500s", (double)1000 * 1/4500);
        expVsMs.put("1/4000s", (double)1000 * 1/4000);
        expVsMs.put("1/3200s", (double)1000 * 1/3200);
        expVsMs.put("1/2800s", (double)1000 * 1/2800);
        expVsMs.put("1/2500s", (double)1000 * 1/2500);
        expVsMs.put("1/2250s", (double)1000 * 1/2250);
        expVsMs.put("1/2000s", (double)1000 * 1/2000);
        expVsMs.put("1/1500s", (double)1000 * 1/1500);
        expVsMs.put("1/1300s", (double)1000 * 1/1300);
        expVsMs.put("1/1100s", (double)1000 * 1/1100);
        expVsMs.put("1/1000s", (double)1000 * 1/1000);
        expVsMs.put("1/800s", (double)1000 * 1/800);
        expVsMs.put("1/700s", (double)1000 * 1/700);
        expVsMs.put("1/600s", (double)1000 * 1/600);
        expVsMs.put("1/500s", (double)1000 * 1/500);
        expVsMs.put("1/400s", (double)1000 * 1/400);
        expVsMs.put("1/350s", (double)1000 * 1/350);
        expVsMs.put("1/300s", (double)1000 * 1/300);
        expVsMs.put("1/250s", (double)1000 * 1/250);
        expVsMs.put("1/200s", (double)1000 * 1/200);
        expVsMs.put("1/175s", (double)1000 * 1/175);
        expVsMs.put("1/150s", (double)1000 * 1/150);
        expVsMs.put("1/120s", (double)1000 * 1/120);
        expVsMs.put("1/100s", (double)1000 * 1/100);
        expVsMs.put("1/80s", (double)1000 * 1/80);
        expVsMs.put("1/70s", (double)1000 * 1/70);
        expVsMs.put("1/60s", (double)1000 * 1/60);
        expVsMs.put("1/50s", (double)1000 * 1/50);
        expVsMs.put("1/40s", (double)1000 * 1/40);
        expVsMs.put("1/35s", (double)1000 * 1/35);
        expVsMs.put("1/30s", (double)1000 * 1/30);
        expVsMs.put("1/25s", (double)1000 * 1/25);
        expVsMs.put("1/20s", (double)1000 * 1/20);
        expVsMs.put("1/15s", (double)1000 * 1/15);
        expVsMs.put("1/12s", (double)1000 * 1/12);
        expVsMs.put("1/10s", (double)1000 * 1/10);
        expVsMs.put("1/8.5s", 1000 * 1/8.5);
        expVsMs.put("1/7.5s", 1000 * 1/7.5);
        expVsMs.put("1/6s", (double)1000 * 1/6);
        expVsMs.put("1/5s", (double)1000 * 1/4);
        expVsMs.put("1/4s", (double)1000 * 1/4);
        expVsMs.put("1/3.5s", 1000 * 1/3.5);
        expVsMs.put("1/3s", (double)1000 * 1/3);
        expVsMs.put("1/2.5s", 1000 * 1/2.50);
        expVsMs.put("1/2.3s", 1000 * 1/2.3);
        expVsMs.put("1/2s", (double)1000 * 1/2);
        expVsMs.put("1/1.7s", 1000 * 1/1.7);
        expVsMs.put("1/1.5s", 1000 * 1/1.5);
        expVsMs.put("1/1.2s", 1000 * 1/1.2);
        expVsMs.put("1s", (double)1000 * 1);
        expVsMs.put("1.2s", 1000 * 1.2);
        expVsMs.put("1.5s", 1000 * 1.5);
        expVsMs.put("2s", (double)1000 * 2);
        expVsMs.put("2.5s", (double)1000 * 2.5);
        expVsMs.put("3s", (double)1000 * 3);
        expVsMs.put("3.5s", 1000 * 3.5);
        expVsMs.put("4s", (double)1000 * 4);
        expVsMs.put("4.5s", (double)1000 * 4.5);
        expVsMs.put("5s", (double)1000 * 5);
    }

    public static void copyFileFromSourceToDest(File source, File dest) {
        try {
            FileUtils.copyFileToDirectory(source, dest);
        } catch (IOException e) {
            log(e.getMessage());
        }
    }

    public static void createConfigFilesInsideProcessedDir(String baseDir, File userExpDir) {
        File chNamesFile = new File(baseDir + File.separator + "channelNames.txt");
        if (!chNamesFile.exists()) {
            throw new IllegalStateException("channelNames file not found: " + chNamesFile);
        }

        File expFile = new File(baseDir + File.separator + "Experiment.json");
        if (!expFile.exists()) {
            throw new IllegalStateException("Experiment JSON file not found: " + expFile);
        }

        File expTimesFile = new File(baseDir + File.separator + "exposure_times.txt");
        if (!expTimesFile.exists()) {
            throw new IllegalStateException("Processing Options JSON file not found: " + expTimesFile);
        }

        copyFileFromSourceToDest(chNamesFile, userExpDir);
        copyFileFromSourceToDest(expFile, userExpDir);
        copyFileFromSourceToDest(expTimesFile, userExpDir);
    }
}
