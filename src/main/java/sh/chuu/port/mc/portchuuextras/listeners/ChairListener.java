package sh.chuu.port.mc.portchuuextras.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import sh.chuu.port.mc.portchuuextras.PortChuuExtras;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChairListener implements Listener {
    private static final String SIT_PERM = "portchuuextras.interact.sit";
    private final PortChuuExtras plugin = PortChuuExtras.getInstance();
    private final Map<ArmorStand, Block> mounted = new LinkedHashMap<>();
    private final Map<Player, Location> postDismountTeleport = new LinkedHashMap<>();

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
        Material mainhand = ev.getMaterial();
        Material offhand = p.getInventory().getItemInOffHand().getType();
        if (!p.hasPermission(SIT_PERM)
                || p.isInsideVehicle()
                || p.isSneaking()
                || p.isSprinting()
                || ev.getAction() != Action.RIGHT_CLICK_BLOCK
                || ev.isBlockInHand()
                || itemInteractable(mainhand)
                || itemInteractable(offhand)
        ) return;

        Block b = ev.getClickedBlock();
        if (b != null) {
            // Distance check - deny if too far distance or if y level too low
            Location bLoc = b.getLocation();
            Location pLoc = p.getLocation();
            if (bLoc.getY() > pLoc.getY() + 0.5 || bLoc.distanceSquared(pLoc) > 4d)
                return;

            if (b.getBlockData() instanceof Stairs) {
                Stairs s = (Stairs) b.getBlockData();
                if (canSit(b, s, ev.getBlockFace())) {
                    ev.setCancelled(true);
                    chairMount(ev.getPlayer(), b, s);
                }
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
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN), 2);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void chairDeath(EntityDeathEvent ev) {
        if (ev.getEntity() instanceof ArmorStand && mounted.remove(ev.getEntity()) != null)
            ev.getDrops().clear();
    }

    private boolean canSit(Block b, Stairs s, BlockFace click) {
        if (s.getHalf() != Bisected.Half.BOTTOM
                || !b.getRelative(BlockFace.UP).isPassable()
                || click == s.getFacing()
                || click == BlockFace.DOWN
        )
            return false;

        if (click == BlockFace.UP
                || click == s.getFacing().getOppositeFace())
            return true;

        boolean left;
        switch (s.getShape()) {
            case OUTER_LEFT:
            case OUTER_RIGHT:
                return true;
            case INNER_LEFT:
                left = true;
                break;
            case INNER_RIGHT:
                left = false;
                break;
            default:
                return false;
        }

        switch (s.getFacing()) {
            case EAST:
                return left && click == BlockFace.SOUTH
                        || click == BlockFace.NORTH
                ;
            case SOUTH:
                return left && click == BlockFace.WEST
                        || click == BlockFace.EAST
                ;
            case WEST:
                return left && click == BlockFace.NORTH
                        || click == BlockFace.SOUTH
                ;
            case NORTH:
                return left && click == BlockFace.EAST
                        || click == BlockFace.WEST
                ;
            default:
                return false;
        }
    }

    private boolean itemInteractable(Material m) {
        if (m.isEdible())
            return true;

        switch (m) {
            //<editor-fold defaultstate="collapsed" desc="Right click action items">
            case BOW:
            case POTION:
            case SPLASH_POTION:
            case LINGERING_POTION:
            case FLINT_AND_STEEL:
            case SHIELD:
            case ITEM_FRAME:
            case ARMOR_STAND:
            case PAINTING:
            //</editor-fold>
                return true;
        }
        return false;
    }

    private void chairMount(Player p, Block b, Stairs s) {
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

        ArmorStand as = b.getWorld().spawn(l, ArmorStand.class);

        as.setCanMove(false);
        as.setBasePlate(false);
        as.setVisible(false);

        mounted.put(as, b);
        Location to = p.getLocation();
        to.setYaw(yaw);
        p.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN);
        as.addPassenger(p);
    }

    private void chairDismount(Entity armorStand, boolean tpImmediately) {
        if (armorStand instanceof ArmorStand) {
            Block b = mounted.remove(armorStand);
            if (b != null) {

                Stairs s = (Stairs) b.getBlockData();
                double x, y, z;

                Block facingLow = b.getRelative(s.getFacing().getOppositeFace());
                Block facingHigh = facingLow.getRelative(BlockFace.UP);



                if (facingHigh.isPassable()) {
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
                                    ? 0.0
                                    : right
                                    ? 1.0
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
                                    ? 1.0
                                    : right
                                    ? 0.0
                                    : 0.5;
                            break;
                        default:
                            x = z = 0.5;
                    }
                    y = facingLow.isPassable() ? 0.5 : 1.0;
                } else {
                    x = z = 0.5;
                    y = 1.0;
                }

                ejectAll(armorStand, b.getLocation().add(x, y, z), tpImmediately);
                armorStand.remove();
            }
        }
    }

    private void ejectAll(Entity armorStand, Location loc, boolean tpImmediately) {
        for (Entity e : armorStand.getPassengers()) {
            Location to = loc.clone();
            Location el = e.getLocation();
            to.setYaw(el.getYaw());
            to.setPitch(el.getPitch());
            if (tpImmediately || !(e instanceof Player)) {
                e.leaveVehicle();
                e.teleport(to, PlayerTeleportEvent.TeleportCause.UNKNOWN);
            } else {
                Player p = (Player) e;
                p.setNoDamageTicks(10);
                postDismountTeleport.put(p, to);
                e.leaveVehicle();
            }
        }
    }
}
