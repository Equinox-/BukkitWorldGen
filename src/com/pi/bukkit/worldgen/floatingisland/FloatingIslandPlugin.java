package com.pi.bukkit.worldgen.floatingisland;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.pi.bukkit.worldgen.tropicfjord.TropicFjordGenerator;

public class FloatingIslandPlugin extends JavaPlugin implements Listener {
	private Logger log = Logger.getLogger("Minecraft");

	@Override
	public void onLoad() {
		logMessage("Load");
		File wrld = new File("world");
		delFolder(wrld);
	}

	private void delFolder(File wrld) {
		File[] files = wrld.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f != null) {
					if (f.isDirectory()) {
						delFolder(f);
					} else {
						f.delete();
					}
				}
			}
		}
		wrld.delete();
	}

	@Override
	public void onEnable() {
		logMessage("Enabled");
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		logMessage("Disabled");
	}

	public void logMessage(String s) {
		PluginDescriptionFile pd = this.getDescription();
		log.info(pd.getName() + "  " + pd.getVersion() + ": " + s);
	}

	@Override
	public ChunkGenerator getDefaultWorldGenerator(String world, String id) {
		return new TropicFjordGenerator();//new FloatingIslandGenerator(this);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		e.getPlayer().sendMessage("Re-join the server to fly again");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerJoin(PlayerJoinEvent e) {
		e.getPlayer().setAllowFlight(true);
		e.getPlayer().setFlying(true);
		e.getPlayer().saveData();
		e.getPlayer().sendMessage(
				"You can fly 'cause otherwise navigation is a nightmare");
	}
}
