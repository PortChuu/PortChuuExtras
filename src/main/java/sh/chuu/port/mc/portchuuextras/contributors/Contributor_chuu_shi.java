package sh.chuu.port.mc.portchuuextras.contributors;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

public class Contributor_chuu_shi implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        TextComponent send = new TextComponent(":eyes: hai");
        send.setColor(ChatColor.DARK_AQUA);
        sender.sendMessage(send);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return ImmutableList.of();
    }
}
