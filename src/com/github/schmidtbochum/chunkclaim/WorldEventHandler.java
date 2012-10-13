/*
    ChunkClaim Plugin for Minecraft Bukkit Servers
    Copyright (C) 2012 Felix Schmidt
    
    This file is part of ChunkClaim.

    ChunkClaim is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ChunkClaim is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ChunkClaim.  If not, see <http://www.gnu.org/licenses/>.
 */

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
