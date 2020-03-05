package sh.chuu.port.mc.portchuuextras.commands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.ImmutableList;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import sh.chuu.port.mc.portchuuextras.PortChuuExtras;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CmdFly implements TabExecutor {
    private final PortChuuExtras plugin = PortChuuExtras.getInstance();
    private final ProtocolManager pm = ProtocolLibrary.getProtocolManager();
    private final Map<Player, PlayerStatus> runner = new LinkedHashMap<>();
    private final Listener event = new FlyingPlayer();
    private boolean eventRegistered = false;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player p = (Player) sender;
        if (p.isGliding()) {
            p.sendActionBar("You're gliding!");
            return true;
        }

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline())
                p.kickPlayer(plugin.getServer().spigot().getPaperConfig().getString("messages.kick.flying-player"));
        }, 200);

        runner.put(p, new PlayerStatus(task, p.getLocation(), p.getVelocity(), p.getFallDistance(), p.getRemainingAir()));
        p.setInvulnerable(true);
        levitate(p);

        if (!eventRegistered) {
            plugin.getServer().getPluginManager().registerEvents(event, plugin);
            eventRegistered = true;
        }


        return true;
    }

    private void levitate(Player p) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EFFECT);

        packet.getBytes()
                .write(0, (byte) (PotionEffectType.LEVITATION.getId()))
                .write(1, (byte) 127) // Amplifier
                .write(2, (byte) 2); // flags - 1 ambient | 2 particle | 4 icon
        packet.getIntegers()
                .write(0, p.getEntityId())
                .write(1, 1); // Duration

        try {
            pm.sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(
                    "Cannot send packet " + packet, e);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return ImmutableList.of();
    }

    private class FlyingPlayer implements Listener {
        @EventHandler
        public void disableInteract(PlayerInteractEvent ev) {
            if (runner.containsKey(ev.getPlayer()))
                ev.setCancelled(true);
        }

        @EventHandler
        public void disableInteract(PlayerInteractEntityEvent ev) {
            if (runner.containsKey(ev.getPlayer()))
                ev.setCancelled(true);
        }

        @EventHandler
        public void disableElytra(EntityToggleGlideEvent ev) {
            //noinspection SuspiciousMethodCalls
            if (runner.containsKey(ev.getEntity()))
                ev.setCancelled(true);
        }

        @EventHandler
        public void flyKickEvent(PlayerKickEvent ev) {
            Player p = ev.getPlayer();
            PlayerStatus s = runner.remove(p);
            if (s == null) return;

            p.setInvulnerable(false);
            p.teleport(s.location);
            p.setVelocity(s.velocity);
            p.setFallDistance(s.fallDistance);
            p.setRemainingAir(s.remainingAir);
            s.task.cancel();
            if (runner.size() == 0) {
                HandlerList.unregisterAll(this);
                eventRegistered = false;
            }
        }
    }

    private final class PlayerStatus {
        private final BukkitTask task;
        private final Location location;
        private final Vector velocity;
        private final float fallDistance;
        private final int remainingAir;

        private PlayerStatus(BukkitTask task, Location location, Vector velocity, float fallDistance, int remainingAir) {
            this.task = task;
            this.location = location;
            this.velocity = velocity;
            this.fallDistance = fallDistance;
            this.remainingAir = remainingAir;
        }
    }
}
