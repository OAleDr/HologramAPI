package org.inventivetalent.hologram;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.hologram.mcstats.MetricsLite;

public class HologramPlugin extends JavaPlugin implements Listener {

	protected static HologramPlugin instance;
	boolean usePackets = false;

	public void onEnable() {
		instance = this;
		Bukkit.getPluginManager().registerEvents(new HologramListeners(), this);
		if (Bukkit.getPluginManager().isPluginEnabled("PacketListenerApi")) {
			this.usePackets = true;
			new PacketListener(instance);
			HologramAPI.packetsEnabled = true;
		}
		if (Bukkit.getPluginManager().isPluginEnabled("ProtocolSupport"))
			HologramAPI.enableProtocolSupport();
		getLogger().info("\n " + (usePackets ? "Found PacketListenerAPI. Enabled touch-holograms.\n" : "") + (HologramAPI.useProtocolSupport ? "Found ProtocolSupport.\n": ""));
		try { 
			MetricsLite metrics = new MetricsLite(this);
			if (metrics.start()) {
				getLogger().info("=Metrics started");
			}
		} catch (Exception e) {
		}
	}

	public void onDisable() {
		for (Hologram h : HologramAPI.getHolograms())
			HologramAPI.removeHologram(h);
		if (this.usePackets) {
			 PacketListener.disable();
		}
	}
}
