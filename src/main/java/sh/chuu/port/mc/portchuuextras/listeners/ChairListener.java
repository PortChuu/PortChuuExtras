package sh.chuu.port.mc.portchuuextras.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
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
import org.bukkit.inventory.EquipmentSlot;
import org.spigotmc.event.entity.EntityDismountEvent;
import sh.chuu.port.mc.portchuuextras.PortChuuExtras;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChairListener implements Listener {
    private static final String SIT_PERM = "portchuuextras.interact.sit";
    private final PortChuuExtras plugin = PortChuuExtras.getInstance();
    private final Map<ArmorStand, Block> mounted = new LinkedHashMap<>();

    public void onDisable() {
        mounted.entrySet().removeIf(as -> {
            ejectAll(as.getKey(), as.getValue().getLocation());
            as.getKey().remove();
            return true;
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void chairSit(PlayerInteractEvent ev) {
        Block b = ev.getClickedBlock();
        Player p = ev.getPlayer();

        if (ev.getAction() != Action.RIGHT_CLICK_BLOCK
                || ev.getHand() == EquipmentSlot.OFF_HAND  // Offhand activity is handled here (code won't probably reach here but /shrug)
                || !p.hasPermission(SIT_PERM)
                || b == null
                || !(b.getBlockData() instanceof Stairs s)
                || p.isInsideVehicle()
                || p.isSneaking()
                || p.isSprinting()
                || ev.isBlockInHand()
        ) return;

        Material mainhand = ev.getMaterial();
        Material offhand = p.getInventory().getItemInOffHand().getType();
        boolean isHungry = p.getFoodLevel() != 20;

        if (itemInteractable(mainhand, isHungry)
                || itemInteractable(offhand, isHungry)
                || !canSit(b, s, ev.getBlockFace())
        ) return;

        // Distance check - deny if too far distance or if y level too low
        Location bLoc = b.getLocation();
        Location pLoc = p.getLocation();
        if (bLoc.getY() > pLoc.getY() + 0.5) {
            p.sendActionBar("You may not sit now; the stair is too high");
            return;
        } else if (bLoc.distanceSquared(pLoc) > 6) {
            p.sendActionBar("You may not sit now; the stair is too far away");
            return;
        }

        ev.setCancelled(true);
        chairMount(ev.getPlayer(), b, s);
    }

    @EventHandler(ignoreCancelled = true)
    public void chairDismount(EntityDismountEvent ev) {
        chairDismount(ev.getDismounted());
    }

    @EventHandler(ignoreCancelled = true)
    public void chairDismount(PlayerQuitEvent ev) {
        chairDismount(ev.getPlayer().getVehicle());
    }

    @EventHandler(ignoreCancelled = true)
    public void chairDeath(EntityDeathEvent ev) {
        if (ev.getEntity() instanceof ArmorStand && mounted.remove(ev.getEntity()) != null)
            ev.getDrops().clear();
    }

    private boolean canSit(Block b, Stairs s, BlockFace click) {
        if (s.getHalf() != Bisected.Half.BOTTOM
                || !b.getRelative(BlockFace.UP).isPassable()
                || click == BlockFace.DOWN
        ) return false;

        if (click == BlockFace.UP
                || click == s.getFacing().getOppositeFace()
        ) return true;

        boolean left;
        switch (s.getShape()) {
            case STRAIGHT:
                return click != s.getFacing();
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

        // At this point the stair configuration is inners only (L shape from top)
        if (click == s.getFacing())
            return false;

        return switch (s.getFacing()) {
            case EAST -> left && click == BlockFace.SOUTH
                    || click == BlockFace.NORTH;
            case SOUTH -> left && click == BlockFace.WEST
                    || click == BlockFace.EAST;
            case WEST -> left && click == BlockFace.NORTH
                    || click == BlockFace.SOUTH;
            case NORTH -> left && click == BlockFace.EAST
                    || click == BlockFace.WEST;
            default -> false;
        };
    }

    private boolean itemInteractable(Material m, boolean isHungry) {
        if (isHungry && m.isEdible())
            return true;

        return switch (m) {
            //<editor-fold defaultstate="collapsed" desc="Right click action items">
            case BOW, POTION, SPLASH_POTION, LINGERING_POTION, FLINT_AND_STEEL, SHIELD, ITEM_FRAME, ARMOR_STAND, PAINTING ->
                    //</editor-fold>
                    true;
            default -> false;
        };
    }

    private void chairMount(Player p, Block b, Stairs s) {
        float yaw = switch (s.getFacing()) {
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> -90f;
            case NORTH -> 0f;
            default -> 0f;
        };

        switch (s.getShape()) {
            case INNER_LEFT, OUTER_LEFT -> yaw -= 45f;
            case INNER_RIGHT, OUTER_RIGHT -> yaw += 45f;
        }

        Location l = b.getLocation().add(0.5, -1.25, 0.5);
        l.setYaw(yaw);

        ArmorStand as = b.getWorld().spawn(l, ArmorStand.class);

        as.setCanMove(false);
        as.setBasePlate(false);
        as.setVisible(false);
        //noinspection ConstantConditions
        as.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(0);

        mounted.put(as, b);
        Location to = p.getLocation();
        to.setYaw(yaw);
        p.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN);
        as.addPassenger(p);
    }

    private void chairDismount(Entity armorStand) {
        if (armorStand instanceof ArmorStand) {
            Block b = mounted.remove(armorStand);
            if (b != null) {
                BlockData data = b.getBlockData();
                if (!(data instanceof Stairs s)) {
                    ejectAll(armorStand, b.getLocation().add(0.5, 0.0, 0.5));
                    armorStand.remove();
                    return;
                }

                double x, y, z;

                Block facingLow = b.getRelative(s.getFacing().getOppositeFace());
                Block facingHigh = facingLow.getRelative(BlockFace.UP);

                if (facingHigh.isPassable()) {
                    boolean left, right;
                    switch (s.getShape()) {
                        case INNER_LEFT, OUTER_LEFT -> {
                            left = true;
                            right = false;
                        }
                        case INNER_RIGHT, OUTER_RIGHT -> {
                            right = true;
                            left = false;
                        }
                        default -> {
                            left = false;
                            right = false;
                        }
                    }

                    switch (s.getFacing()) {
                        case NORTH -> {
                            x = left
                                    ? 1.0
                                    : right
                                    ? 0.0
                                    : 0.5;
                            z = 1.0;
                        }
                        case WEST -> {
                            x = 1.0;
                            z = left
                                    ? 0.0
                                    : right
                                    ? 1.0
                                    : 0.5;
                        }
                        case SOUTH -> {
                            x = left
                                    ? 0.0
                                    : right
                                    ? 1.0
                                    : 0.5;
                            z = 0.0;
                        }
                        case EAST -> {
                            x = 0.0;
                            z = left
                                    ? 1.0
                                    : right
                                    ? 0.0
                                    : 0.5;
                        }
                        default -> x = z = 0.5;
                    }
                    y = facingLow.isPassable() ? 0.5 : 1.0;
                } else {
                    x = z = 0.5;
                    y = 1.0;
                }

                ejectAll(armorStand, b.getLocation().add(x, y, z));
                armorStand.remove();
            }
        }
    }

    private void ejectAll(Entity armorStand, Location loc) {
        for (Entity e : armorStand.getPassengers()) {
            Location to = loc.clone();
            Location el = e.getLocation();
            to.setYaw(el.getYaw());
            to.setPitch(el.getPitch());
            e.leaveVehicle();
            e.teleport(to, PlayerTeleportEvent.TeleportCause.UNKNOWN);
        }
    }
}
