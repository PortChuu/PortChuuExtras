package sh.chuu.port.mc.portchuuextras;

import org.bukkit.plugin.java.JavaPlugin;
import sh.chuu.port.mc.portchuuextras.commands.*;
import sh.chuu.port.mc.portchuuextras.contributors.*;
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

        getCommand("op").setExecutor(new CmdOp());
        getCommand("fly").setExecutor(new CmdFly());
        getCommand("xray").setExecutor(new CmdXray());

        getServer().getPluginManager().registerEvents(chairListener = new ChairListener(), this);

        getCommand("simonorj").setExecutor(new SimonOrJ());
    }

    @Override
    public void onDisable() {
        chairListener.onDisable();
    }
}
