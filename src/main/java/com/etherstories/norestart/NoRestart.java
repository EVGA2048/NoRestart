package com.etherstories.norestart;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Blocks Spigot's built-in /restart, whose shutdown path deadlocks on Arclight:
 * the command runs on the server thread and calls System.exit(), so JVM shutdown
 * waits for the "Server thread" shutdown hook while that hook waits for the server
 * thread itself.
 */
public final class NoRestart extends JavaPlugin implements Listener {

    private static final String BLOCKED = "restart";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        removeCommand();
        getLogger().info("Blocked /restart; use /restartnow + /restartconfirm.");
    }

    /**
     * Also drop the command from the Bukkit command map, in case another plugin
     * or the console path dispatches it without firing the preprocess event.
     */
    private void removeCommand() {
        try {
            Object server = getServer();
            Object map = server.getClass().getMethod("getCommandMap").invoke(server);
            Command cmd = (Command) map.getClass().getMethod("getCommand", String.class).invoke(map, BLOCKED);
            if (cmd != null) {
                cmd.unregister((CommandMap) map);
                getLogger().info("Unregistered command: /" + BLOCKED);
            }
        } catch (Throwable t) {
            getLogger().warning("Could not unregister /restart: " + t);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isRestart(event.getMessage())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                "§e[重启] /restart 已停用。请使用 /restartnow，并在 60 秒内输入 /restartconfirm。");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsoleCommand(ServerCommandEvent event) {
        if (isRestart(event.getCommand())) {
            event.setCancelled(true);
            event.getSender().sendMessage(
                "[重启] /restart 已停用。请使用 /restartnow + /restartconfirm，或直接输入 stop。");
        }
    }

    static boolean isRestart(String raw) {
        if (raw == null) {
            return false;
        }
        String s = raw.trim();
        if (s.startsWith("/")) {
            s = s.substring(1);
        }
        int space = s.indexOf(' ');
        if (space >= 0) {
            s = s.substring(0, space);
        }
        int colon = s.indexOf(':');
        if (colon >= 0) {
            s = s.substring(colon + 1);
        }
        return s.equalsIgnoreCase(BLOCKED);
    }
}
