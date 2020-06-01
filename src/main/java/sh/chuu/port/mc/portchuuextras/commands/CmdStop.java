package sh.chuu.port.mc.portchuuextras.commands;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import sh.chuu.port.mc.portchuuextras.PortChuuExtras;

import java.util.List;

public class CmdStop implements TabExecutor {
    private static final PortChuuExtras plugin = PortChuuExtras.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender)
            return Bukkit.dispatchCommand(sender, "minecraft:stop");

        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            p.sendMessage(new TranslatableComponent("commands.stop.stopping"));
        }, 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline())
                p.kickPlayer(Bukkit.getShutdownMessage());
        }, 60L);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return ImmutableList.of();
    }
}
