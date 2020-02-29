package sh.chuu.port.mc.portchuuextras.commands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import sh.chuu.port.mc.portchuuextras.PortChuuExtras;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CmdXray implements TabExecutor {
    private final PortChuuExtras plugin = PortChuuExtras.getInstance();
    private final ProtocolManager pm = ProtocolLibrary.getProtocolManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player p = (Player) sender;
        int x = p.getLocation().getChunk().getX();
        int z = p.getLocation().getChunk().getZ();
        int[][] coords = {{x,z},{x-1,z},{x-1,z-1},{x,z-1},{x+1,z-1},{x+1,z},{x+1,z+1},{x,z+1},{x-1,z+1}};
        theThing(p, coords, 0);
        return true;
    }

    public void theThing(Player p, int[][] coords, int i) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (i < 9) {
                unload(p, coords[i][0], coords[i][1]);
                theThing(p, coords, i+1);
            } else {
                p.kickPlayer("checkmate yo nice try lmao");
            }
        }, 10L);
    }

    public void unload(Player p, int x, int z) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.UNLOAD_CHUNK);
        packet.getIntegers()
                .write(0, x)
                .write(1, z);
        try {
            pm.sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(
                    "Cannot send packet " + packet, e);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
