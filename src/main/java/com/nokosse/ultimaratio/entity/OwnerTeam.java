package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Who may switch an owned automatic weapon (MAMBA, Serval LAD): its owner and the owner's scoreboard team. */
final class OwnerTeam {
    private OwnerTeam() {
    }

    static boolean canControl(Level level, @Nullable UUID owner, Player player) {
        if (owner == null || owner.equals(player.getUUID())) {
            return true;
        }
        Player online = level.getPlayerByUUID(owner);
        if (online != null) {
            // SBW's own team test, so a team deathmatch (everyone hostile) is respected
            return SeekTool.IN_SAME_TEAM.test(online, player);
        }
        // Owner offline: the scoreboard still knows their team, by name
        MinecraftServer server = level.getServer();
        GameProfileCache profiles = server == null ? null : server.getProfileCache();
        String name = profiles == null ? null : profiles.get(owner).map(GameProfile::getName).orElse(null);
        PlayerTeam team = name == null ? null : level.getScoreboard().getPlayersTeam(name);
        return team != null && team == player.getTeam() && !TDMSavedData.enabledTDM(player);
    }
}
