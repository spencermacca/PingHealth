package co.spencermedia.pingboard;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PingBoardPlugin extends JavaPlugin {

    private PingBoard pingBoard;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Settings settings = Settings.load(getConfig(), getLogger());

        PingSource pingSource = switch (settings.pingSource()) {
            case KEEPALIVE -> Player::getPing;
            case PROBE -> new CookieProbe(new NamespacedKey(this, "rtt"));
        };

        pingBoard = new PingBoard(this, settings, pingSource);
        getServer().getPluginManager().registerEvents(pingBoard, this);
        registerCommand("ping", "Toggle the ping sidebar for yourself.", new PingCommand(pingBoard));
    }

    @Override
    public void onDisable() {
        if (pingBoard != null) {
            pingBoard.shutdown();
        }
    }
}
