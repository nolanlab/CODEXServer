/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nolanlab.CODEX.uploader.uplserver.driffta;


import ij.*;
import ij.io.Opener;
import ij.plugin.*;
import ij.process.LUT;
import ij.process.StackProcessor;
import org.apache.commons.io.FilenameUtils;
import org.nolanlab.CODEX.uploader.uplclient.Experiment;
import org.nolanlab.CODEX.utils.*;
import org.nolanlab.CODEX.utils.codexhelper.BestFocusHelper;
import org.nolanlab.CODEX.utils.codexhelper.ExperimentHelper;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Nikolay Samusik
 */
public class MakeMontage {

    private static File[] seqFiles;

    public static void createMontages(String user, String expName, String fc) throws Exception {

        String serverConfig = System.getProperty("user.dir") + File.separator + "data";
        String mkMonLoc = serverConfig + File.separator + user + File.separator + expName +File.separator + "processed" + File.separator + "stitched";
        int factor = Integer.parseInt(fc);

        File mkMonDir = new File(mkMonLoc);
        if(!mkMonDir.exists()) {
            mkMonDir.mkdir();
        }

        File tilesDir = new File(mkMonDir.getParentFile() + File.separator + "tiles");
        File expFile = new File(tilesDir.getParentFile().getParentFile() + File.separator + "experiment.json");

        ExperimentHelper expHelper = new ExperimentHelper();
        BestFocusHelper bfHelper = new BestFocusHelper();

        Experiment exp = expHelper.loadFromJSON(expFile);

        File bfFile = new File(tilesDir + File.separator + "bestFocus.json");
        Map<String, Integer> tileVsBf = bfHelper.load(bfFile);

        ImagePlus[] impArr;

//        if(exp.isExportImgSeq()) {
            seqFiles = tilesDir.listFiles(t -> t.isDirectory() && t.getName().startsWith("reg"));
            impArr = new ImagePlus[seqFiles.length];

            for(int i=0; i<seqFiles.length; i++) {
                ArrayList<ImagePlus> stacks = new ArrayList<>();
                File[] tifFiles = seqFiles[i].listFiles(tif -> tif.getName().endsWith("tif") || tif.getName().endsWith("tiff"));
                for(File aTifFile : tifFiles) {
                    int z = tileVsBf.get(seqFiles[i].getName());
                    //Search for _Z instead
                    String zNumberStr = aTifFile.getName().substring(21, 24);
                    int zNumber = zNumberStr == null ? 0 : Integer.parseInt(zNumberStr);
                    if(zNumber != z) {
                        continue;
                    }
                    else {
                        ImagePlus im = IJ.openImage(aTifFile.getPath());
                        stacks.add(im);
                    }
                }
                ImagePlus concatenatedStacks = new Concatenator().concatenate(stacks.toArray(new ImagePlus[stacks.size()]), false);
                ImagePlus hyp = HyperStackConverter.toHyperStack(concatenatedStacks, exp.getChannel_names().length, 1, exp.getNum_cycles(), "default", "Composite");
                hyp.setTitle(seqFiles[i].getName());
                impArr[i] = hyp ;
            }

            logger.print("Found " + seqFiles.length + " image sequence folders:");
            for (File file1 : seqFiles) {
                logger.print(file1);
            }
//        }
        //deprecated
//        else {
//            tifFiles = tilesDir.listFiles(t -> t.getName().endsWith(".tif") || t.getName().endsWith(".tiff"));
//            impArr = new ImagePlus[tifFiles.length];
//
//            for(int i=0; i<tifFiles.length; i++) {
//                ImagePlus opIm = IJ.openImage(tifFiles[i].getPath());
//                String tifName = FilenameUtils.removeExtension(tifFiles[i].getName());
//                int z = tileVsBf.get(tifName);
//                ImagePlus aImp = dup.run(opIm, 1, opIm.getNChannels(), z, z, 1, opIm.getNFrames());
//                aImp.setTitle(aImp.getTitle().replaceAll("DUP_", ""));
//                impArr[i] = aImp;
//            }
//
//            logger.print("Found " + tifFiles.length + " TIFF files:");
//            for (File file1 : tifFiles) {
//                logger.print(file1);
//            }
//        }

        runMontageAlgo(impArr, mkMonDir, tileVsBf, factor);
    }


    public static void runMontageAlgo(ImagePlus[] impArr, File mkMonDir, Map<String, Integer> tileVsBf, int factor) throws Exception {
        Duplicator dup = new Duplicator();
        //Create the tileMap.txt file with the TileObject content here
        logger.print("Creating tileMap.txt file...");
        createTileMap(impArr, new File(mkMonDir.getParentFile() + File.separator + "tiles"));

        ImagePlus firstImp = impArr[0];
        ImagePlus imp;
//        if(isImgSeq) {
            imp = dup.run(firstImp, 1, firstImp.getNChannels(), tileVsBf.get(seqFiles[0].getName()), tileVsBf.get(seqFiles[0].getName()), 1, firstImp.getNFrames());
//        }
//        else {
//            imp = dup.run(firstImp, 1, firstImp.getNChannels(), tileVsBf.get(FilenameUtils.removeExtension(tifFiles[0].getName())), tileVsBf.get(FilenameUtils.removeExtension(tifFiles[0].getName())), 1, firstImp.getNFrames());
//        }
        Arrays.asList(impArr).stream().collect(Collectors.groupingBy(t -> t.getTitle().split("_")[0])).forEach((regname, filesInReg) -> {
            int maxX = 0;
            int maxY = 0;
            for (ImagePlus f2 : filesInReg) {
                try {
                    int[] coord = extractXYCoord(f2);
                    maxX = Math.max(maxX, coord[0]);
                    maxY = Math.max(maxY, coord[1]);
                } catch (Exception e) {

                }
            }
            ImageStack[][] grid = new ImageStack[maxX][maxY];

            for (ImagePlus f2 : filesInReg) {
                try {
                    int[] coord = extractXYCoord(f2);

                    ImagePlus tmp = f2;
                    ImageStack is = tmp.getImageStack();
                    StackProcessor sp = new StackProcessor(is);
                    grid[coord[0] - 1][coord[1] - 1] = sp.resize(tmp.getWidth() / factor, tmp.getHeight() / factor);
                } catch (Exception e) {
                    logger.showException(e);
                }
            }

            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[x].length; y++) {
                    if (grid[x][y] == null) {
                        throw new IllegalStateException("tile is null: " + regname + " X=" + (x + 1) + ", Y=" + (y + 1));
                    }
                }
            }

            ImageStack[] horizStacks = new ImageStack[grid[0].length];
            StackCombiner stackCombiner = new StackCombiner();

            for (int y = 0; y < horizStacks.length; y++) {
                horizStacks[y] = grid[0][y];
                for (int x = 1; x < grid.length; x++) {
                    horizStacks[y] = stackCombiner.combineHorizontally(horizStacks[y], grid[x][y]);
                }
            }

            ImageStack out = horizStacks[0];

            for (int i = 1; i < horizStacks.length; i++) {
                if (horizStacks[i] != null) {
                    out = stackCombiner.combineVertically(out, horizStacks[i]);
                }
            }

            ImagePlus comb = new ImagePlus(regname, out);

            logger.print("combined stack has " + comb.getStackSize() + " slices");
            logger.print("imp stack has " + imp.getStackSize() + " slices, " + imp.getNChannels() + " channels, " + imp.getNFrames() + " frames, " + imp.getNSlices() + " slices");

            ImagePlus hyp = HyperStackConverter.toHyperStack(comb, imp.getNChannels(), 1, imp.getStackSize() / imp.getNChannels(), "xyczt", "composite");
            if (hyp.getNChannels() == 4) {
                ((CompositeImage) hyp).setLuts(new LUT[]{LUT.createLutFromColor(Color.WHITE), LUT.createLutFromColor(Color.RED), LUT.createLutFromColor(Color.GREEN), LUT.createLutFromColor(new Color(0, 70, 255))});
            }

            IJ.saveAsTiff(hyp, mkMonDir.getAbsolutePath() + File.separator + regname + "_montage.tif");
        });
    }

    public static void createTileMap(ImagePlus[] tiff, File f) throws IOException {

        //Arrays.sort(tiff);
        PrintWriter bwTileMap = null;
        LinkedList<TileObject> tiles = new LinkedList<>();

        for(ImagePlus aTifImp : tiff) {
            if (aTifImp != null) {
                TileObject aTile = new TileObject(aTifImp, aTifImp.getTitle());
                tiles.add(aTile);
            }
        }

        int xPos = 0;
        int yPos = 0;

        if(tiles == null || tiles.isEmpty()) {
            return;
        }
        TileObject firstTile = tiles.getFirst();

        List<Integer> xTracker = new LinkedList<>();
        List<Integer> refList = new LinkedList<>();

        try {
            if(f == null) {
                throw new IOException("Output directory to store tileMap.txt not found!");
            }
            bwTileMap = new PrintWriter(f.getParent() + File.separator + "tileMap.txt");
            bwTileMap.write("RegionNumber\tTileX\tTileY\tXposition\tYposition");
            bwTileMap.println();
            for (TileObject currentTile : tiles) {
                if (firstTile.getRegionNumber() != currentTile.getRegionNumber()) {
                    xPos = 0;
                    yPos = 0;
                    firstTile = currentTile;
                    xTracker = new LinkedList<>();
                    refList = new LinkedList<>();
                }

                bwTileMap.write("" + currentTile.getRegionNumber() + "\t");
                bwTileMap.write("" + currentTile.getXNumber() + "\t");
                bwTileMap.write("" + currentTile.getYNumber() + "\t");
                if (firstTile.getXNumber() == currentTile.getXNumber()) {
                    if (refList.isEmpty()) {
                        bwTileMap.write("" + xPos + "\t");
                        bwTileMap.write("" + yPos);
                        bwTileMap.println();
                    } else {
                        xPos = 0;
                        xPos += refList.get(currentTile.getYNumber() - 1);
                        bwTileMap.write("" + xPos + "\t");
                        bwTileMap.write("" + yPos);
                        bwTileMap.println();
                    }
                    xTracker.add(currentTile.getWidth());
                    yPos += currentTile.getHeight();
                } else if (firstTile.getXNumber() != currentTile.getXNumber()) {
                    if (!refList.isEmpty()) {
                        for (int i = 0; i < refList.size(); i++) {
                            refList.set(i, refList.get(i)+currentTile.getWidth());
                        }
                    }
                    else {
                        for (int i = 0; i < xTracker.size(); i++) {
                            refList.add(xTracker.get(i));
                        }
                    }
                    xTracker.clear();
                    xTracker.add(currentTile.getWidth());
                    xPos = 0;
                    xPos += refList.get(currentTile.getYNumber() - 1);
                    yPos = 0;
                    bwTileMap.write("" + xPos + "\t");
                    bwTileMap.write("" + yPos);
                    bwTileMap.println();
                    yPos += currentTile.getHeight();
                }
                firstTile = currentTile;
            }
        }
        catch (IOException e) {
            logger.print(e.getMessage());
        }
        finally {
            bwTileMap.close();
        }
    }

    public static int[] extractXYCoord(ImagePlus f) {
        String[] s = f.getTitle().split("[_\\.]");
        int[] ret = new int[]{Integer.parseInt(s[1].substring(1)), Integer.parseInt(s[2].substring(1))};
        return ret;
    }
}
