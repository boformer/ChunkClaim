package com.github.schmidtbochum.chunkclaim;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkClaim extends JavaPlugin {
	public static ChunkClaim plugin;
	public static final Logger logger = Logger.getLogger("Minecraft");
	
	public DataStore dataStore;
	
	public List<String> config_worlds; 
	
	public void onDisable() {
		
	}
	public void onEnable() {
		plugin = this;
		
		//copy default config		
		this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        
        this.config_worlds = this.getConfig().getStringList("worlds");
        
    	try {
    		this.dataStore = new FlatFileDataStore();
    	} catch(Exception e) {
    		addLogEntry("Unable to initialize the file system data store. Details:");
    		addLogEntry(e.getMessage());
    	}
    	
		//register for events
		PluginManager pluginManager = this.getServer().getPluginManager();
		
		//register block events
		BlockEventHandler blockEventHandler = new BlockEventHandler(dataStore);
		pluginManager.registerEvents(blockEventHandler, this);
		
		//register player events
		PlayerEventHandler playerEventHandler = new PlayerEventHandler(dataStore);
		pluginManager.registerEvents(playerEventHandler, this);
		
		//register world events
		WorldEventHandler worldEventHandler = new WorldEventHandler(dataStore);
		pluginManager.registerEvents(worldEventHandler, this);
    	
    	//Example Claim (debugging)
    	if(this.dataStore.chunks.size() == 0) {
    		Chunk newChunk = new Chunk(0, 0, "Colony6", "Notch");
    		this.dataStore.addChunk(newChunk);
    		
    	}
    }

	public static void addLogEntry(String entry) {
		logger.info("ChunkClaim: " + entry);
	}





}