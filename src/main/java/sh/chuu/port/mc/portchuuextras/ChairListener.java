package sh.chuu.port.mc.portchuuextras;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChairListener implements Listener {
    private static final String SIT_PERM = "portchuuextras.interact.sit";
    private final PortChuuExtras plugin = PortChuuExtras.getInstance();
    private final Map<ArmorStand, Block> mounted = new LinkedHashMap<>();
    private final Map<Entity, Location> postDismountTeleport = new LinkedHashMap<>();

    public void onDisable() {
        mounted.entrySet().removeIf(as -> {
            ejectAll(as.getKey(), as.getValue().getLocation(), false);
            as.getKey().remove();
            return true;
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void chairSit(PlayerInteractEvent ev) {
        Player p = ev.getPlayer();
        if (!p.hasPermission(SIT_PERM)
                || p.isSneaking()
                || ev.getAction() != Action.RIGHT_CLICK_BLOCK
                || ev.isBlockInHand()
        ) return;

        Block b = ev.getClickedBlock();
        if (b != null && b.getBlockData() instanceof Stairs) {
            Stairs s = (Stairs) b.getBlockData();
            if (s.getHalf() == Bisected.Half.BOTTOM) {
                chairMount(ev.getPlayer(), b);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void chairDismount(EntityDismountEvent ev) {
        chairDismount(ev.getDismounted(), false);
    }

    @EventHandler(ignoreCancelled = true)
    public void chairDismount(PlayerQuitEvent ev) {
        chairDismount(ev.getPlayer().getVehicle(), true);
    }

    @EventHandler(ignoreCancelled = true)
    public void postDismountTeleportation(PlayerTeleportEvent ev) {
        if (ev.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN)
            return;

        Player p = ev.getPlayer();
        Location loc = postDismountTeleport.remove(p);
        if (loc != null) {
            ev.setTo(loc);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.teleport(loc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
            }, 2);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void chairDeath(EntityDeathEvent ev) {
        if (ev.getEntity() instanceof ArmorStand && mounted.remove(ev.getEntity()) != null)
            ev.getDrops().clear();
    }

    private void chairMount(Player p, Block b) {
        Stairs s = (Stairs) b.getBlockData();

        float yaw;
        switch (s.getFacing()) {
            case EAST:
                yaw = 90f;
                break;
            case SOUTH:
                yaw = 180f;
                break;
            case WEST:
                yaw = -90f;
                break;
            case NORTH:
            default:
                yaw = 0f;
        }

        switch (s.getShape()) {
            case INNER_LEFT:
            case OUTER_LEFT:
                yaw -= 45f;
                break;
            case INNER_RIGHT:
            case OUTER_RIGHT:
                yaw += 45f;
        }

        Location l = b.getLocation().add(0.5, -1.25, 0.5);
        l.setYaw(yaw);

        ArmorStand e = b.getWorld().spawn(l, ArmorStand.class);

        e.setCanMove(false);
        e.setBasePlate(false);
        e.setVisible(false);

        mounted.put(e, b);
        Location to = p.getLocation();
        to.setYaw(to.getYaw() + 180);
        p.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN);
        e.addPassenger(p);
    }

    private void chairDismount(Entity armorStand, boolean tpImmediately) {
        if (armorStand instanceof ArmorStand) {
            Block b = mounted.remove(armorStand);
            if (b != null) {
                Stairs s = (Stairs) b.getBlockData();
                double x, z;
                boolean left, right;

                switch (s.getShape()) {
                    case INNER_LEFT:
                    case OUTER_LEFT:
                        left = true;
                        right = false;
                        break;
                    case INNER_RIGHT:
                    case OUTER_RIGHT:
                        right = true;
                        left = false;
                        break;
                    default:
                        left = false;
                        right = false;
                }

                switch (s.getFacing()) {
                    case NORTH:
                        x = left
                                ? 1.0
                                : right
                                ? 0.0
                                : 0.5;
                        z = 1.0;
                        break;
                    case WEST:
                        x = 1.0;
                        z = left
                                ? 1.0
                                : right
                                ? 0.0
                                : 0.5;
                        break;
                    case SOUTH:
                        x = left
                                ? 0.0
                                : right
                                ? 1.0
                                : 0.5;
                        z = 0.0;
                        break;
                    case EAST:
                        x = 0.0;
                        z = left
                                ? 0.0
                                : right
                                ? 1.0
                                : 0.5;
                        break;
                    default:
                        x = z = 0.5;
                }

                ejectAll(armorStand, b.getLocation().add(x, 0.0, z), tpImmediately);
                armorStand.remove();
            }
        }
    }

    private void ejectAll(Entity armorStand, Location loc, boolean tpImmediately) {
        for (Entity e : armorStand.getPassengers()) {
            e.leaveVehicle();
            Location to = loc.clone();
            Location el = e.getLocation();
            to.setYaw(el.getYaw());
            to.setPitch(el.getPitch());
            if (tpImmediately) {
                e.teleport(to, PlayerTeleportEvent.TeleportCause.UNKNOWN);
            } else {
                postDismountTeleport.put(e, to);
            }
        }
    }
}
