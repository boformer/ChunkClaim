package com.github.schmidtbochum.chunkclaim;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldEventHandler implements Listener {
	
	private DataStore dataStore;
	
	public WorldEventHandler(DataStore dataStore) {
		this.dataStore = dataStore;
	}
	
	
	//when a world gets loaded
	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		String worldName = event.getWorld().getName();
		if(ChunkClaim.plugin.config_worlds.contains(worldName)) {
			try {
				dataStore.loadWorldData(worldName);
				int claimedChunks = dataStore.worlds.get(worldName).chunkTable.size();
				ChunkClaim.addLogEntry("Loaded "+ claimedChunks + " claimed chunks for world \"" + worldName + "\".");
				System.gc();
			} catch(Exception e) {
				ChunkClaim.addLogEntry("Unable to load data for world \"" + worldName + "\": " + e.getMessage());
			}
		}
	}
	//when a world gets unloaded
	@EventHandler
	public void onWorldUnload(WorldUnloadEvent event) {
		String worldName = event.getWorld().getName();
		if(ChunkClaim.plugin.config_worlds.contains(worldName)) {
			dataStore.unloadWorldData(worldName);
			System.gc();
		}
	}
}
