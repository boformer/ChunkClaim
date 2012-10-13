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

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkClaim extends JavaPlugin {
	public static ChunkClaim plugin;
	public static final Logger logger = Logger.getLogger("Minecraft");
	
	public DataStore dataStore;
	
	public List<String> config_worlds; 
	public boolean config_protectContainers;
	public boolean config_protectSwitches;
	public boolean config_mobsForCredits;
	public int config_creditsPerHour;
	public int config_maxCredits;
	
	public void onDisable() {
		Player [] players = this.getServer().getOnlinePlayers();
		for(int i = 0; i < players.length; i++)
		{
		Player player = players[i];
		String playerName = player.getName();
		PlayerData playerData = this.dataStore.getPlayerData(playerName);
		this.dataStore.savePlayerData(playerName, playerData);
		}

		this.dataStore.close();
	}
	public void onEnable() {
		plugin = this;
		
		//copy default config		
		this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        
        this.config_worlds = this.getConfig().getStringList("worlds");
        this.config_protectSwitches = true;
        this.config_protectContainers = true;
        this.config_mobsForCredits = true;
        this.config_creditsPerHour = 12;
        this.config_maxCredits = 500;
        
        
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
		
		//register entity events
		EntityEventHandler entityEventHandler = new EntityEventHandler(dataStore);
		pluginManager.registerEvents(entityEventHandler, this);
		
		//register world events
		WorldEventHandler worldEventHandler = new WorldEventHandler(dataStore);
		pluginManager.registerEvents(worldEventHandler, this);

		if(this.config_creditsPerHour > 0)	{
			DeliverCreditsTask task = new DeliverCreditsTask();
			this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60 * 5, 20L * 60 * 5);
		}
		
    	//Example Claim (debugging)
    	if(this.dataStore.chunks.size() == 0) {
    		Chunk newChunk = new Chunk(0, 0, "Colony6", "Notch");
    		this.dataStore.addChunk(newChunk);
    		
    	}
    }
	//handles slash commands
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){

		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		if(cmd.getName().equalsIgnoreCase("abandonchunk") && player != null) {
			Chunk chunk = this.dataStore.getChunkAt(player.getLocation(), null);
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			Location location = player.getLocation();
			if(chunk==null) {
				player.sendMessage("This chunk is public.");
				Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
				Visualization.Apply(player, visualization);
				
			} else if(chunk.ownerName.equals(player.getName())) {
				this.dataStore.deleteChunk(chunk);
				playerData.credits++;
				this.dataStore.savePlayerData(player.getName(), playerData);
				player.sendMessage("Chunk abandoned. Credits: " + playerData.credits);
				Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.Public, location);
				Visualization.Apply(player, visualization);
				
				return true;
			} else {
				if(playerData.lastChunk!=chunk) {
					playerData.lastChunk=chunk;
					Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.ErrorChunk, location);
					Visualization.Apply(player, visualization);
				}
				player.sendMessage("You don't own this chunk. Only " + chunk.ownerName + " or the staff can delete it.");
				return true;
			}
		}
		else if(cmd.getName().equalsIgnoreCase("chunk") && player != null) {
			Chunk chunk = this.dataStore.getChunkAt(player.getLocation(), null);
			Location location = player.getLocation();
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			if(chunk==null) {
				player.sendMessage("This chunk is public.");
				Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
				Visualization.Apply(player, visualization);
			} else if(chunk.ownerName.equals(player.getName())) {
				
				if(chunk.builderNames.size()>0) {
					StringBuilder builders = new StringBuilder();
					for(int i = 0; i < chunk.builderNames.size(); i++) {
						builders.append(chunk.builderNames.get(i));
						if(i<chunk.builderNames.size()-1) {
							builders.append(", ");
						}
					}
					Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.Chunk, location);
					Visualization.Apply(player, visualization);
					player.sendMessage("You own this chunk. Trusted Builders:");
					player.sendMessage(builders.toString());

				} else {
					player.sendMessage("You own this chunk. Use /trust <player> to add other builders.");
				}					
				Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.Chunk, location);
				Visualization.Apply(player, visualization);
				return true;
			} else {
				if(chunk.isTrusted(player.getName())) {
					player.sendMessage(chunk.ownerName + " owns this chunk. You have build rights!");
					if(playerData.lastChunk!=chunk) {
						playerData.lastChunk=chunk;
						Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.Chunk, location);
						Visualization.Apply(player, visualization);
					}
				} 
				else {
					player.sendMessage(chunk.ownerName + " owns this chunk. You can't build here.");		
					if(playerData.lastChunk!=chunk) {
						playerData.lastChunk=chunk;
						Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.ErrorChunk, location);
						Visualization.Apply(player, visualization);
					}
				}
				return true;
			}
		}
		else if(cmd.getName().equalsIgnoreCase("credits") && player != null) {
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			player.sendMessage("You have " + playerData.credits + " credits.");
		}
		else if(cmd.getName().equalsIgnoreCase("trust") && player != null) {
			Chunk chunk = this.dataStore.getChunkAt(player.getLocation(), null);
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			Location location = player.getLocation();
			if(chunk==null) {
				player.sendMessage("This chunk is public. Claim this chunk by building something in it.");
				Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
				Visualization.Apply(player, visualization);
			} else if(chunk.ownerName.equals(player.getName())) {
				if(args.length==1) {
					if(resolvePlayer(args[0])!=null) {
						chunk.builderNames.add(args[0]);
						dataStore.writeChunkToStorage(chunk);
						player.sendMessage("Trusted " + args[1] + " in this chunk.");
					} else {
						player.sendMessage("Player not found.");
					}
					return true;
				} else {
					return false;
				}
				
			} else {
				player.sendMessage("You don't own this chunk.");
				if(playerData.lastChunk!=chunk) {
					playerData.lastChunk=chunk;
					Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.ErrorChunk, location);
					Visualization.Apply(player, visualization);
				}
				return true;
			}
		}
		else if(cmd.getName().equalsIgnoreCase("untrust") && player != null) {
			Chunk chunk = this.dataStore.getChunkAt(player.getLocation(), null);
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			Location location = player.getLocation();
			if(chunk==null) {
				player.sendMessage("This chunk is public. Claim this chunk by building something in it.");
				Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
				Visualization.Apply(player, visualization);
			} else if(chunk.ownerName.equals(player.getName())) {
				if(args.length==1) {
					if(resolvePlayer(args[0])!=null) {
						chunk.builderNames.remove(args[0]);
						dataStore.writeChunkToStorage(chunk);
						player.sendMessage("Untrusted " + args[1] + " in this chunk.");
					} else {
						player.sendMessage("Player not found.");
					}
					return true;
				} else {
					return false;
				}
				
			} else {
				player.sendMessage("You don't own this chunk.");
				if(playerData.lastChunk!=chunk) {
					playerData.lastChunk=chunk;
					Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.ErrorChunk, location);
					Visualization.Apply(player, visualization);
				}
				return true;
			}
		}
		return false;	
	}
	
	public void regenerateChunk(Chunk chunk) {
		getServer().getWorld(chunk.worldName).regenerateChunk(chunk.x, chunk.z);
		
	}
	public OfflinePlayer resolvePlayer(String name) {
		
		Player player = this.getServer().getPlayer(name);
		if(player != null) return player;

		//then search offline players
		OfflinePlayer [] offlinePlayers = this.getServer().getOfflinePlayers();
		for(int i = 0; i < offlinePlayers.length; i++) {
			if(offlinePlayers[i].getName().equalsIgnoreCase(name))	{
				return offlinePlayers[i];
			}
		}

		//if none found, return null
		return null;
		
		
	}
	

	public static void addLogEntry(String entry) {
		logger.info("ChunkClaim: " + entry);
	}





}