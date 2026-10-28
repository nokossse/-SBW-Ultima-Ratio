package com.nokosse.ultimaratio;

import net.minecraftforge.common.ForgeConfigSpec;

public final class UltimaRatioConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue SAMPT_MIN_TARGET_HEIGHT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        SAMPT_MIN_TARGET_HEIGHT = b
                .comment("SAMP/T MAMBA auto mode: targets lower than (launcher Y + this value) are ignored.",
                        "0 = engages every target above its own Y, 20 = ignores anything within 20 blocks of its Y.")
                .defineInRange("samptMinTargetHeight", 20.0, 0.0, 1024.0);
        SPEC = b.build();
    }

    private UltimaRatioConfig() {
    }
}
