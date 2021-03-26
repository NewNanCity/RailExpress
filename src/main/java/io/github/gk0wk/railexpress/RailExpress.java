package io.github.gk0wk.railexpress;

import co.aikar.commands.PaperCommandManager;
import io.github.gk0wk.violet.config.ConfigManager;
import io.github.gk0wk.violet.config.ConfigUtil;
import io.github.gk0wk.violet.i18n.LanguageManager;
import io.github.gk0wk.violet.message.MessageManager;
import me.lucko.helper.Events;
import me.lucko.helper.config.ConfigurationNode;
import me.lucko.helper.event.filter.EventFilters;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import java.io.IOException;
import java.util.*;

public final class RailExpress extends ExtendedJavaPlugin {

    protected ConfigManager configManager;
    private LanguageManager languageManager;
    protected MessageManager messageManager;

    private static RailExpress instance = null;
    public static RailExpress getInstance() {
        return instance;
    }

    /**
     * 游戏默认矿车极速为0.4，最高为1.5
     */
    protected final static double DEFAULT_SPEED = 0.4;

    @Override
    protected void load() {
        // 初始化ConfigManager
        configManager = new ConfigManager(this);
        configManager.touch("config.yml");

        // 初始化LanguageManager
        try {
            Locale locale = new Locale("config");
            languageManager = new LanguageManager(this)
                    .register(locale, "config.yml")
                    .setMajorLanguage(locale);
        } catch (LanguageManager.FileNotFoundException | ConfigManager.UnknownConfigFileFormatException | IOException e) {
            e.printStackTrace();
            this.onDisable();
        }

        // 初始化MessageManager
        messageManager = new MessageManager(this)
                .setLanguageProvider(languageManager);
        messageManager.setPlayerPrefix(messageManager.sprintf("$msg.prefix$"));

        instance = this;
    }

    @Override
    protected void enable() {
        // 初始化CommandManager - 不能在load()里面初始化！
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.usePerIssuerLocale(true, false);
        try {
            commandManager.getLocales().loadYamlLanguageFile("config.yml", new Locale("config"));
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            this.onDisable();
        }

        // 注册指令
        commandManager.registerCommand(new RailExpressCommand());

        // 注册事件
        Events.subscribe(VehicleExitEvent.class, EventPriority.LOWEST)
                .filter(EventFilters.ignoreCancelled())
                .filter(event -> event.getVehicle() instanceof Minecart)
                .filter(event -> railConfigMap.containsKey(event.getVehicle().getWorld()))
                .filter(event -> event.getVehicle().isEmpty())
                .handler(event -> ((Minecart) event.getVehicle()).setMaxSpeed(DEFAULT_SPEED));
        Events.subscribe(VehicleMoveEvent.class, EventPriority.HIGHEST)
                .filter(event -> event.getVehicle() instanceof Minecart)
                .filter(event -> railConfigMap.containsKey(event.getVehicle().getWorld()))
                .filter(event -> !event.getVehicle().isEmpty())
                .handler(event -> {
                    Block curBlock = event.getVehicle().getLocation().getBlock();
                    Material material = curBlock.getType();
                    RailConfig config = railConfigMap.get(event.getVehicle().getWorld());

                    boolean flag = material.equals(Material.POWERED_RAIL);
                    if (!flag && !config.powerRailOnly) {
                        flag = material.equals(Material.RAILS) ||
                                material.equals(Material.DETECTOR_RAIL) ||
                                material.equals(Material.ACTIVATOR_RAIL);
                    }

                    if (flag && !config.allowNonPlayer) {
                        for (Entity entity : event.getVehicle().getPassengers()) {
                            if (entity instanceof Player) {
                                continue;
                            }
                            flag = false;
                            break;
                        }
                    }

                    if (flag) {
                        // 看看铁轨下面的方块是什么，赋予相应的速度
                        Block belowBlock = curBlock.getRelative(BlockFace.DOWN);
                        ((Minecart) event.getVehicle()).setMaxSpeed(config.blockSpeedMap.getOrDefault(belowBlock.getType(), DEFAULT_SPEED));
                    }
                    // 否则设置为默认速度
                    else ((Minecart) event.getVehicle()).setMaxSpeed(DEFAULT_SPEED);
                });

        // 载入配置
        reload();
    }

    private final Map<World, RailConfig> railConfigMap = new HashMap<>();

    protected void reload() {
        railConfigMap.clear();
        try {
            ConfigurationNode worlds = configManager.get("config.yml").getNode("config");

            worlds.getChildrenList().forEach(config -> {
                boolean powerRailOnly = config.getNode("power-rail-only").getBoolean(true);
                boolean allowNonPlayer = config.getNode("allow-non-player").getBoolean(false);
                RailConfig railConfig = new RailConfig(powerRailOnly, allowNonPlayer, config.getNode("block-type"));

                ConfigUtil.setListIfNull(config.getNode("world")).getList(Object::toString)
                        .forEach(world -> railConfigMap.put(Bukkit.getWorld(world), railConfig));
            });
        } catch (IOException | ConfigManager.UnknownConfigFileFormatException e) {
            e.printStackTrace();
            this.onDisable();
        }

    }
}