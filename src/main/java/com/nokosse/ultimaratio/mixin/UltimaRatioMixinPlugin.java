package com.nokosse.ultimaratio.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class UltimaRatioMixinPlugin implements IMixinConfigPlugin {
    private static boolean has(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Embeddium/Rubidium replace setupRender wholesale: the player getX/Y/Z redirects have nothing to hook.
        if (mixinClassName.endsWith("LevelRendererSetupMixin")) return !has("embeddium") && !has("rubidium");
        if (mixinClassName.endsWith("BetterFpsDistMixin")) return has("betterfpsdist");
        return true;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String t, ClassNode c, String m, IMixinInfo i) {}
    @Override public void postApply(String t, ClassNode c, String m, IMixinInfo i) {}
}
