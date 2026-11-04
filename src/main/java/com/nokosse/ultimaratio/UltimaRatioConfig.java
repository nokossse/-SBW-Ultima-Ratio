package com.nokosse.ultimaratio;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class UltimaRatioConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue SAMPT_MIN_TARGET_HEIGHT;
    public static final ModConfigSpec.BooleanValue DRONE_CHUNK_LOADING;
    public static final ModConfigSpec.DoubleValue GF300_RANGE;
    public static final ModConfigSpec.DoubleValue SAMPT_NG_RADAR_RANGE;
    public static final ModConfigSpec.DoubleValue ARABEL_RADAR_RANGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SAMPT_MIN_TARGET_HEIGHT = b
                .comment("SAMP/T MAMBA auto mode: targets lower than (launcher Y + this value) are ignored.",
                        "0 = engages every target above its own Y, 20 = ignores anything within 20 blocks of its Y.")
                .defineInRange("samptMinTargetHeight", 20.0, 0.0, 1024.0);
        DRONE_CHUNK_LOADING = b
                .comment("VSR700 / MQ-9: a drone keeps its own chunks loaded (a chunk ticket follows it), even without a pilot or a player nearby,",
                        "so it keeps flying/loitering and the GCS can still list it. Costs a few loaded chunks per drone: turn off on busy servers.")
                .define("droneChunkLoading", true);
        GF300_RANGE = b
                .comment("GF300 radar range in blocks (the wider mode doubles it).")
                .defineInRange("gf300Range", 1280.0, 16.0, 100000.0);
        SAMPT_NG_RADAR_RANGE = b
                .comment("SAMP/T NG radar range in blocks.")
                .defineInRange("samptNgRadarRange", 2000.0, 16.0, 100000.0);
        ARABEL_RADAR_RANGE = b
                .comment("Arabel (MAMBA radar) range in blocks.")
                .defineInRange("arabelRadarRange", 1000.0, 16.0, 100000.0);
        SPEC = b.build();
    }

    private UltimaRatioConfig() {
    }
}
