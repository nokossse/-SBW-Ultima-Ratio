package com.nokosse.ultimaratio;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class UltimaRatioConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue SAMPT_MIN_TARGET_HEIGHT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SAMPT_MIN_TARGET_HEIGHT = b
                .comment("SAMP/T MAMBA auto mode: targets lower than (launcher Y + this value) are ignored.",
                        "0 = engages every target above its own Y, 20 = ignores anything within 20 blocks of its Y.")
                .defineInRange("samptMinTargetHeight", 20.0, 0.0, 1024.0);
        SPEC = b.build();
    }

    private UltimaRatioConfig() {
    }
}
