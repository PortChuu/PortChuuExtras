package sh.chuu.port.mc.portchuuextras.listeners;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import sh.chuu.port.mc.portchuuextras.PortChuuExtras;

import java.util.Random;

public class CrazyBlockBreak implements Listener {
    private static final String PERMISSION_NODE = "portchuuextras.crazyblockbreak";
    private final PortChuuExtras plugin = PortChuuExtras.getInstance();
    @EventHandler
    public void onBlockBreak(BlockBreakEvent ev) {
        if (!ev.getPlayer().hasPermission(PERMISSION_NODE))
            return;
        Block b = ev.getBlock();
        Material m = b.getType();

        new BukkitRunnable() {
            private int i = 100;
            @Override
            public void run() {
                if (i-- == 0)
                    this.cancel();
                b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, m);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
