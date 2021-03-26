package io.github.gk0wk.railexpress;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import org.bukkit.command.CommandSender;

@CommandAlias("railexpress")
public class RailExpressCommand extends BaseCommand {
    @Subcommand("reload")
    @CommandPermission("railexpress.reload")
    @Description("{@@msg.help-reload}")
    public static void onReload(CommandSender sender) {
        RailExpress.getInstance().reload();
        RailExpress.getInstance().messageManager.printf(sender, "$msg.reload$");
    }

    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        RailExpress.getInstance().messageManager.printf(sender, "$msg.help-head$");
        help.showHelp();
    }
}
