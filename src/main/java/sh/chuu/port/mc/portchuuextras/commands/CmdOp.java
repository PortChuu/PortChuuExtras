package sh.chuu.port.mc.portchuuextras.commands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import sh.chuu.port.mc.portchuuextras.PortChuuExtras;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CmdOp implements TabExecutor {
    private final PortChuuExtras plugin = PortChuuExtras.getInstance();
    private final ProtocolManager pm = ProtocolLibrary.getProtocolManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(getUsage());
            return true;
        }
        Player p = (Player) sender;
        BaseComponent send = new TranslatableComponent("commands.op.success", args[0]);
        p.sendMessage(send);

        double x = p.getLocation().getX();
        double z = p.getLocation().getZ();
        double[][] coords = {{x,z},{x-1,z},{x-1,z-1},{x,z-1},{x+1,z-1},{x+1,z},{x+1,z+1},{x,z+1},{x-1,z+1}};
        double y = p.getLocation().getBlockY() + 1;
        theThing(p, coords, y, args[0]);
        return true;
    }

    private BaseComponent getUsage() {
        BaseComponent ret = new TextComponent("Usage: /op <player>");
        ret.setColor(ChatColor.RED);
        return ret;
    }

    private void theThing(Player p, double[][] coords, double y, String fakePlayer) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (double[] coord : coords) {
                tnt(p, coord[0], y, coord[1]);
            }

            BaseComponent send = new TranslatableComponent("chat.type.admin",
                    fakePlayer,
                    new TranslatableComponent("commands.summon.success",
                            new TranslatableComponent("entity.minecraft.tnt")
                    )
            );
            send.setColor(ChatColor.GRAY);
            send.setItalic(true);

            for (int i = 0; i < 20; i++) {
                p.sendMessage(send);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Fake Death Screen
                // p.kickPlayer("checkmate yo nice try lmao");
                for (int i = 0; i < 20; i++)
                    p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1f);
                showDeathScreen(p);
            }, 80L);
        }, 16L);
    }

    private void tnt(Player p, double x, double y, double z) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);

        packet.getEntityTypeModifier()
                .write(0, EntityType.PRIMED_TNT);
        packet.getIntegers()
                .write(0, new Random().nextInt())
                .write(1, 0) // v(x)
                .write(2, 1600) // v(y)
                .write(3, 0) // v(z)
                .write(4, 0) // pitch
                .write(5, 0) // yaw
                .write(6, 0); // data??
        packet.getUUIDs()
                .write(0, UUID.randomUUID());
        packet.getDoubles()
                .write(0, x)
                .write(1, y)
                .write(2, z);
        try {
            pm.sendServerPacket(p, packet);
            p.playSound(new Location(p.getWorld(), x, y, z), Sound.ENTITY_TNT_PRIMED, 0.8f, 1f);
            p.playSound(new Location(p.getWorld(), x, y, z), Sound.ENTITY_TNT_PRIMED, 0.8f, 1f);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(
                    "Cannot send packet " + packet, e);
        }
    }

    private void showDeathScreen(Player p) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.UPDATE_HEALTH);

        packet.getFloat()
                .write(0, 0f)
                .write(1, p.getSaturation());
        packet.getIntegers()
                .write(0, p.getFoodLevel());

        try {
            pm.sendServerPacket(p, packet);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                packet.getFloat()
                        .write(0, (float) p.getHealth());
                try {
                    pm.sendServerPacket(p, packet);
                } catch (InvocationTargetException ignore) {}
            }, 5L);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(
                    "Cannot send packet " + packet, e);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return null;
        return ImmutableList.of();
    }
}
