package io.github.gk0wk.railexpress;

import me.lucko.helper.config.ConfigurationNode;
import org.bukkit.Material;

import java.util.HashMap;

public class RailConfig {
    protected final boolean powerRailOnly, allowNonPlayer;
    protected final HashMap<Material, Double> blockSpeedMap = new HashMap<>();
    public RailConfig(boolean powerRailOnly, boolean allowNonPlayer, ConfigurationNode blockType) {
        this.powerRailOnly = powerRailOnly;
        this.allowNonPlayer = allowNonPlayer;

        blockType.getChildrenMap().forEach((key, node) -> blockSpeedMap.
                put(Material.valueOf(((String) key).toUpperCase()), node.getDouble(RailExpress.DEFAULT_SPEED)));
    }
}
