/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nolanlab.CODEX.uploader.uplclient;

import org.nolanlab.CODEX.utils.microscope.MicroscopeTypeEnum;


import java.util.HashMap;


import static org.nolanlab.CODEX.utils.microscope.MicroscopeTypeEnum.KEYENCE;
import static org.nolanlab.CODEX.utils.microscope.MicroscopeTypeEnum.LEICA;
import static org.nolanlab.CODEX.utils.microscope.MicroscopeTypeEnum.ZEISS;


/**
 *
 * @author Nikolay
 */
public class Experiment {

    private String userName;
    private String name;
    private String date;
    private String projName;
    private String codex_instrument;
    private MicroscopeTypeEnum microscope;
    private String deconvolution;
    private String objectiveType;
    private int magnification;
    private double numerical_aperture;
    private double per_pixel_XY_resolution;
    private double z_pitch;
    private int num_z_planes;
    private String channel_arrangement;
    private String[] channel_names;
    private int[] emission_wavelengths;
    private int drift_comp_channel;
    private int driftCompReferenceCycle;
    private int best_focus_channel;
    private int bestFocusReferenceCycle;
    private int num_cycles;
    private int cycle_upper_limit;
    private int cycle_lower_limit;
    private int[] regIdx;
    private String[] region_names;
    private String tiling_mode;
    private int region_width;
    private int region_height;
    private int tile_overlap_X;
    private int tile_overlap_Y;
    private boolean HandEstain;
    private int tile_height;
    private int tile_width;
    private int[] readout_channels;
    private boolean optionalFocusFragment;
    private int focusing_offset;
    private boolean bgSub;
    private boolean useBleachMinimizingCrop;
    private boolean useBlindDeconvolution;

    private static transient HashMap<String, String> projectNameCache = new HashMap<>();
    private static transient final MicroscopeTypeEnum[] microscopeTypes = new MicroscopeTypeEnum[]{KEYENCE, ZEISS, LEICA};

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCodex_instrument() {
        return codex_instrument;
    }

    public void setCodex_instrument(String codex_instrument) {
        this.codex_instrument = codex_instrument;
    }

    public MicroscopeTypeEnum getMicroscope() {
        return microscope;
    }

    public void setMicroscope(MicroscopeTypeEnum microscope) {
        this.microscope = microscope;
    }

    public String getDeconvolution() {
        return deconvolution;
    }

    public void setDeconvolution(String deconvolution) {
        this.deconvolution = deconvolution;
    }

    public int getMagnification() {
        return magnification;
    }

    public void setMagnification(int magnification) {
        this.magnification = magnification;
    }

    public double getNumerical_aperture() {
        return numerical_aperture;
    }

    public void setNumerical_aperture(double numerical_aperture) {
        this.numerical_aperture = numerical_aperture;
    }

    public double getPer_pixel_XY_resolution() {
        return per_pixel_XY_resolution;
    }

    public void setPer_pixel_XY_resolution(double per_pixel_XY_resolution) {
        this.per_pixel_XY_resolution = per_pixel_XY_resolution;
    }

    public double getZ_pitch() {
        return z_pitch;
    }

    public void setZ_pitch(double z_pitch) {
        this.z_pitch = z_pitch;
    }

    public int getNum_z_planes() {
        return num_z_planes;
    }

    public void setNum_z_planes(int num_z_planes) {
        this.num_z_planes = num_z_planes;
    }

    public String getChannel_arrangement() {
        return channel_arrangement;
    }

    public void setChannel_arrangement(String channel_arrangement) {
        this.channel_arrangement = channel_arrangement;
    }

    public String[] getChannel_names() {
        return channel_names;
    }

    public void setChannel_names(String[] channel_names) {
        this.channel_names = channel_names;
    }

    public int[] getEmission_wavelengths() {
        return emission_wavelengths;
    }

    public void setEmission_wavelengths(int[] emission_wavelengths) {
        this.emission_wavelengths = emission_wavelengths;
    }

    public int getDrift_comp_channel() {
        return drift_comp_channel;
    }

    public void setDrift_comp_channel(int drift_comp_channel) {
        this.drift_comp_channel = drift_comp_channel;
    }

    public int getBest_focus_channel() {
        return best_focus_channel;
    }

    public void setBest_focus_channel(int best_focus_channel) {
        this.best_focus_channel = best_focus_channel;
    }

    public int getNum_cycles() {
        this.num_cycles = this.getCycle_upper_limit()-this.getCycle_lower_limit()+1;
        return num_cycles;
    }

    public int getCycle_upper_limit() {
        return cycle_upper_limit;
    }

    public void setCycle_upper_limit(int cycle_upper_limit) {
        this.cycle_upper_limit = cycle_upper_limit;
    }

    public int getCycle_lower_limit() {
        return cycle_lower_limit;
    }

    public void setCycle_lower_limit(int cycle_lower_limit) {
        this.cycle_lower_limit = cycle_lower_limit;
    }

    public int[] getRegIdx() {
        return regIdx;
    }

    public void setRegIdx(int[] regIdx) {
        this.regIdx = regIdx;
    }

    public String[] getRegion_names() {
        return region_names;
    }

    public void setRegion_names(String[] region_names) {
        this.region_names = region_names;
    }

    public String getTiling_mode() {
        return tiling_mode;
    }

    public void setTiling_mode(String tiling_mode) {
        this.tiling_mode = tiling_mode;
    }

    public int getRegion_width() {
        return region_width;
    }

    public void setRegion_width(int region_width) {
        this.region_width = region_width;
    }

    public int getRegion_height() {
        return region_height;
    }

    public void setRegion_height(int region_height) {
        this.region_height = region_height;
    }

    public int getTile_overlap_X() {
        return tile_overlap_X;
    }

    public void setTile_overlap_X(int tile_overlap_X) {
        this.tile_overlap_X = tile_overlap_X;
    }

    public int getTile_overlap_Y() {
        return tile_overlap_Y;
    }

    public void setTile_overlap_Y(int tile_overlap_Y) {
        this.tile_overlap_Y = tile_overlap_Y;
    }

    public int[] getReadout_channels() {
        int k = 0;
        for (int i = 1; i <= this.getChannel_names().length; i++) {
            if (i == this.getDrift_comp_channel()) {
                continue;
            }
            readout_channels[k++] = i;
        }
        return readout_channels;
    }

    public String getObjectiveType() {
        return objectiveType;
    }

    public void setObjectiveType(String objectiveType) {
        this.objectiveType = objectiveType;
    }

    public boolean isHandEstain() {
        return HandEstain;
    }

    public void setHandEstain(boolean handEstain) {
        HandEstain = handEstain;
    }

    public int getTile_height() {
        return tile_height;
    }

    public void setTile_height(int tile_height) {
        this.tile_height = tile_height;
    }

    public int getTile_width() {
        return tile_width;
    }

    public void setTile_width(int tile_width) {
        this.tile_width = tile_width;
    }

    public int getDriftCompReferenceCycle() {
        return driftCompReferenceCycle;
    }

    public void setDriftCompReferenceCycle(int driftCompReferenceCycle) {
        this.driftCompReferenceCycle = driftCompReferenceCycle;
    }

    public int getBestFocusReferenceCycle() {
        return bestFocusReferenceCycle;
    }

    public void setBestFocusReferenceCycle(int bestFocusReferenceCycle) {
        this.bestFocusReferenceCycle = bestFocusReferenceCycle;
    }

    public String getProjName() {
        return projName;
    }

    public void setProjName(String projName) {
        this.projName = projName;
    }

    public boolean isOptionalFocusFragment() {
        return optionalFocusFragment;
    }

    public void setOptionalFocusFragment(boolean optionalFocusFragment) {
        this.optionalFocusFragment = optionalFocusFragment;
    }

    public int getFocusing_offset() {
        return focusing_offset;
    }

    public void setFocusing_offset(int focusing_offset) {
        this.focusing_offset = focusing_offset;
    }

    public boolean isBgSub() {
        return bgSub;
    }

    public void setBgSub(boolean bgSub) {
        this.bgSub = bgSub;
    }

    public boolean isUseBleachMinimizingCrop() {
        return useBleachMinimizingCrop;
    }

    public void setUseBleachMinimizingCrop(boolean useBleachMinimizingCrop) {
        this.useBleachMinimizingCrop = useBleachMinimizingCrop;
    }

    public boolean isUseBlindDeconvolution() {
        return useBlindDeconvolution;
    }

    public void setUseBlindDeconvolution(boolean useBlindDeconvolution) {
        this.useBlindDeconvolution = useBlindDeconvolution;
    }

    public static HashMap<String, String> getProjectNameCache() {
        return projectNameCache;
    }

    public static void setProjectNameCache(HashMap<String, String> projectNameCache) {
        Experiment.projectNameCache = projectNameCache;
    }

    public static MicroscopeTypeEnum[] getMicroscopeTypes() {
        return microscopeTypes;
    }

}
