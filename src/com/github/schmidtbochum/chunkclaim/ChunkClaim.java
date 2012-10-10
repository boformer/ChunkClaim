package com.github.schmidtbochum.chunkclaim;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkClaim extends JavaPlugin implements Listener {
	public static ChunkClaim plugin;
	public static final Logger logger = Logger.getLogger("Minecraft");
	
	public DataStore dataStore;
	
	public List<String> config_worlds; 
	
	public void onDisable() {
		
	}
	public void onEnable() {

		//register events
		getServer().getPluginManager().registerEvents(this, this);
		
		//copy default config		
		this.getConfig().options().copyDefaults(true);

        this.saveConfig();
        
        this.config_worlds = this.getConfig().getStringList("worlds");
        
        
        
        
    	try {
    		this.dataStore = new FlatFileDataStore(config_worlds);
    	} catch(Exception e) {
    		addLogEntry("Unable to initialize the file system data store. Details:");
    		addLogEntry(e.getMessage());
    	}
    	
    	//Example Claim (debugging)
    	if(this.dataStore.chunks.size() == 0) {
    		Chunk newChunk = new Chunk(0, 0, "Colony6", "schmidtbochum");
    		newChunk.claimDate = new Date();
    		
    		this.dataStore.addChunk(newChunk);
    		
    	}
    	
    	
    }

	public static void addLogEntry(String entry) {
		logger.info("ChunkClaim: " + entry);
	}


	//when a player breaks a block...
	@EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Location location = block.getLocation();
		player.sendMessage("Chunk: " + location.getChunk().getX() + "; " + location.getChunk().getZ());
		
		/*
		Player player = breakEvent.getPlayer();
		Block block = breakEvent.getBlock();
		Location location = block.getLocation();

		int bx = location.getBlockX();
		int bz = location.getBlockZ();
		
		int x = (int) (bx / 16 + (bx>=0?1:-1));
		int z = (int) (bz / 16 + (bz>=0?1:-1));
		
		
		player.sendMessage(+ x + "|" + z);
		
		if(!chunkX.containsKey(x) || !chunkZ.containsKey(z)) {
			Chunk chunk = new Chunk(x,z);
			
			claimChunk(chunk,player);
			return;
			
		} else if(chunkX.get(x).ownerName.equals(player.getName())) {
			return;
			
		} else {
			player.sendMessage("You don't have " + chunkX.get(x).ownerName + "'s permission to build here.");
			breakEvent.setCancelled(true);
			return;
			
		}
		*/
	}
	//when a player places a block...
	@EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent placeEvent) {
		/*
		Player player = placeEvent.getPlayer();
		Block block = placeEvent.getBlock();
		Location location = block.getLocation();
		
		int bx = location.getBlockX();
		int bz = location.getBlockZ();
		
		int x = (int) (bx / 16 + (bx>=0?1:-1));
		int z = (int) (bz / 16 + (bz>=0?1:-1));
		
		
		player.sendMessage(location.getBlockX() + "|" +location.getBlockZ() + "; " +x + "|" + z);
		
		if(!chunkX.containsKey(x) || !chunkZ.containsKey(z)) {
			Chunk chunk = new Chunk(x,z);
			player.sendMessage("You claimed this chunk.");
			claimChunk(chunk,player);
			return;
			
		} else if(chunkX.get(x).ownerName.equals(player.getName())) {
			return;
			
		} else {
			player.sendMessage("You don't have " + chunkX.get(x).ownerName + "'s permission to build here.");
			placeEvent.setCancelled(true);
			return;
			
		}
		*/
	}	
	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		String worldName = event.getWorld().getName();
		try {
			dataStore.loadWorldData(worldName);
			int claimedChunks = dataStore.worlds.get(worldName).chunkTable.size();
			ChunkClaim.addLogEntry("Loaded "+ claimedChunks + " claimed chunks for world \"" + worldName + "\".");
		} catch(Exception e) {
			ChunkClaim.addLogEntry("Unable to load data for world \"" + worldName + "\": " + e.getMessage());
		}
	}
	@EventHandler
	public void onWorldUnload(WorldUnloadEvent event) {
		String worldName = event.getWorld().getName();
		dataStore.unloadWorldData(worldName);
	}
}