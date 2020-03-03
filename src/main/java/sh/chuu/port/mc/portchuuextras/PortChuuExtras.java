package sh.chuu.port.mc.portchuuextras;

import org.bukkit.plugin.java.JavaPlugin;
import sh.chuu.port.mc.portchuuextras.commands.CmdOp;
import sh.chuu.port.mc.portchuuextras.commands.CmdXray;
import sh.chuu.port.mc.portchuuextras.listeners.ChairListener;

public class PortChuuExtras extends JavaPlugin {
    private static PortChuuExtras instance;
    private ChairListener chairListener;

    public static PortChuuExtras getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        PortChuuExtras.instance = this;

        getCommand("xray").setExecutor(new CmdXray());
        getCommand("op").setExecutor(new CmdOp());

        getServer().getPluginManager().registerEvents(chairListener = new ChairListener(), this);
    }

    @Override
    public void onDisable() {
        chairListener.onDisable();
    }
}
