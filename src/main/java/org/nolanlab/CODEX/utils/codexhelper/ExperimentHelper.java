package org.nolanlab.CODEX.utils.codexhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.nolanlab.CODEX.uploader.uplclient.Experiment;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExperimentHelper {

    public String getDirName(int cycle, int region, String baseDir, Experiment exp) {

        String name = null;

        switch (exp.getMicroscope()) {
            case KEYENCE:
                name = "Cyc" + cycle + "_reg" + region;

                if (cycle == exp.getNum_cycles() && exp.isHandEstain()) {
                    name = getHandEDirName(region);
                }
                break;
            case ZEISS:
                if (region > 1) {
                    throw new UnsupportedOperationException("The processing of Zeiss data supports only 1 region at the moment  ");
                }
                File sourceDirF = new File(baseDir);

                Map<String, List<File>> map = Arrays.asList(sourceDirF.listFiles(z -> (z.isDirectory() && z.getName().contains("Image Export")))).stream().collect(Collectors.groupingBy(t -> t.getName().split("-")[t.getName().split("-").length - 1]));
                String key = String.format("%02d", cycle);
                List<File> f = map.get(key);
                if(f.size()==0){
                    throw new IllegalStateException("No directory fond for cycle = " + cycle + ", region = " + region + " basedir = " + baseDir);
                }
                if(f.size()>1){
                    throw new IllegalStateException("Multiple directories fond for cycle = " + cycle + ", region = " + region + " basedir = " + baseDir+ "\nPlease remove duplicate directories before continuing\n"+f.toString()) ;
                }
                name = f.get(0).getName();
                break;
            default:
                throw new IllegalArgumentException("Unsupported microscope: " + exp.getMicroscope());
        }

        return name;
    }


    public String getHandEDirName(int region) {
        return "HandE_reg" + region;
    }

    public String toJSON(Experiment experiment) {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT) // include static
                .create();
        String js = gson.toJson(experiment).replaceAll(",", ",\n");
        return js;
    }

    public Experiment loadFromJSON(File f) throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(f));
        Experiment exp = gson.fromJson(reader, Experiment.class);
        return exp;
    }


    public void saveToFile(Experiment exp, File f) throws IOException {
        String js = toJSON(exp);
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write(js);
        bw.flush();
        bw.close();
    }

    public String getDestStackFileName(final String tilingMode, final int tile, final int region, final int region_width) {
        final int zbTile = tile - 1;
        switch (tilingMode) {
            case "snake":
                int X = zbTile % region_width;
                int Y = zbTile / region_width;
                if (Y % 2 == 1) {
                    X = region_width - X;
                    X--;
                }
                X++;
                Y++;
                return String.format("reg%03d_X%02d_Y%02d", region, X, Y) + ".tif";
            default:
                throw new IllegalArgumentException("Unsupported tiling mode: " + tilingMode);
        }
    }

    public String getDestStackFileNameWithZIndex(final String tilingMode, final int tile, final int region, final int region_width, final int zIndex) {
        final int zbTile = tile - 1;
        switch (tilingMode) {
            case "snake":
                int X = zbTile % region_width;
                int Y = zbTile / region_width;
                if (Y % 2 == 1) {
                    X = region_width - X;
                    X--;
                }
                X++;
                Y++;
                return String.format("reg%03d_X%02d_Y%02d_Z%02d", region, X, Y, zIndex) + ".tif";
            default:
                throw new IllegalArgumentException("Unsupported tiling mode: " + tilingMode);
        }
    }

    public String getDestStackFileNameWithZIndexForTif(final String tilingMode, final String aTifFileName, final int zIndex) {
        switch (tilingMode) {
            case "snake":
                String[] strsplit = aTifFileName.split("_");
                int region = Integer.parseInt(strsplit[0].replaceAll("[^0-9]", ""));
                int X = Integer.parseInt(strsplit[1].replaceAll("[^0-9]", ""));
                int Y = Integer.parseInt(strsplit[2].replaceAll("[^0-9]", ""));
                return String.format("reg%03d_X%02d_Y%02d_Z%02d", region, X, Y, zIndex) + ".tif";
            default:
                throw new IllegalArgumentException("Unsupported tiling mode: " + tilingMode);
        }
    }

    public String getSourceFileName(final String sourceDir, final int tile, final int zSlice, final int channel, Experiment exp) {
        switch (exp.getMicroscope()) {
            case KEYENCE:
                String pname = Experiment.getProjectNameCache().get(sourceDir);
                if (pname == null) {
                    File[] f = new File(sourceDir).listFiles((a) -> (a.isFile() && a.getName().endsWith(".bcf")));
                    if (f == null) {
                        throw new IllegalArgumentException("Directory does not contain a .bcf file: " + sourceDir);
                    }
                    if (f.length == 0) {
                        throw new IllegalArgumentException("Directory does not contain a .bcf file: " + sourceDir);
                    }
                    pname = f[0].getName().substring(0, f[0].getName().length() - 4);
                    Experiment.getProjectNameCache().put(sourceDir, pname);
                }
                return pname + String.format("_%05d_Z%03d_", tile, zSlice) + exp.getChannel_names()[channel] + ".tif";
            case ZEISS:
                File sourceDirF = new File(sourceDir);
                String n1 = sourceDirF.getName();
                String n2 = String.format("z%02dc%01dm%02d", zSlice, channel+1, tile) + "_ORG.tif";
                return n1 + "_" + n2;
            default:
                throw new IllegalArgumentException("Unsupported microscope type: " + exp.getMicroscope());
        }
    }

    public String concat(String[] s) {
        if (s.length == 0) {
            return "";
        }
        String out = s[0];
        for (int i = 1; i < s.length; i++) {
            out += (";" + s[i]);
        }
        return out;
    }

    public String concat(int[] s) {
        if (s.length == 0) {
            return "";
        }
        String out = String.valueOf(s[0]);
        for (int i = 1; i < s.length; i++) {
            out += (";" + s[i]);
        }
        return out;
    }

    public void waitAndPrint(Process proc) throws IOException {
        do {
            try {
                BufferedReader brOut = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String s = null;
                while ((s = brOut.readLine()) != null) {
                    log(s);
                }

                BufferedReader brErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

                while ((s = brErr.readLine()) != null) {
                    log("ERROR>" + s);
                }

                Thread.sleep(100);

            } catch (InterruptedException e) {
                log("Process interrupted");
                return;
            }
        } while (proc.isAlive());
        log("Process done");
    }

    public void log(String s) {
        System.out.println(s);
    }
}
