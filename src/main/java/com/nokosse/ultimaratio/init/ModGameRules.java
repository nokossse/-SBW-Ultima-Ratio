package com.nokosse.ultimaratio.init;

import net.minecraft.world.level.GameRules;

public final class ModGameRules {
    /** Game rule names cannot hold spaces, so this is /gamerule ultimaratioSamptTargetAllies. */
    public static final GameRules.Key<GameRules.BooleanValue> SAMPT_TARGET_ALLIES =
            GameRules.register("ultimaratioSamptTargetAllies", GameRules.Category.MISC, GameRules.BooleanValue.create(false));

    /** /gamerule ultimaratioRadarChunkLoading: a deployed radar truck keeps its chunks loaded. */
    public static final GameRules.Key<GameRules.BooleanValue> RADAR_CHUNK_LOADING =
            GameRules.register("ultimaratioRadarChunkLoading", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    /** /gamerule ultimaratioDroneChunkLoading: VSR700 / MQ-9 keep their chunks loaded (also needs the droneChunkLoading config). */
    public static final GameRules.Key<GameRules.BooleanValue> DRONE_CHUNK_LOADING =
            GameRules.register("ultimaratioDroneChunkLoading", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    private ModGameRules() {}

    /** Touching the class registers the rule; call once while the mod loads. */
    public static void register() {}
}
