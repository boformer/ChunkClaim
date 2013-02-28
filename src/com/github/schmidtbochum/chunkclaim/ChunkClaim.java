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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
	public boolean config_nextToForce;
	public int config_mobPrice;
	public float config_creditsPerHour;
	public float config_maxCredits;
	public float config_startCredits;
	public int config_minModBlocks;
	public float config_autoDeleteDays;

	public void onDisable() {
		Player[] players = this.getServer().getOnlinePlayers();
		for (int i = 0; i < players.length; i++) {
			Player player = players[i];
			String playerName = player.getName();
			PlayerData playerData = this.dataStore.getPlayerData(playerName);
			this.dataStore.savePlayerData(playerName, playerData);
		}

		this.dataStore.close();
	}

	public void onEnable() {
		plugin = this;

		// copy default config
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();

		this.config_worlds = this.getConfig().getStringList("worlds");
		this.config_protectSwitches = this.getConfig().getBoolean("protectSwitches");
		this.config_protectContainers = this.getConfig().getBoolean("protectContainers");
		this.config_mobsForCredits = this.getConfig().getBoolean("mobsForCredits");
		this.config_mobPrice = this.getConfig().getInt("mobPrice");
		this.config_startCredits = (float) this.getConfig().getDouble("startCredits");
		this.config_creditsPerHour = (float) this.getConfig().getDouble("creditsPerHour");;
		this.config_maxCredits = (float) this.getConfig().getDouble("maxCredits");
		this.config_minModBlocks = this.getConfig().getInt("minModBlocks");
		this.config_autoDeleteDays = (float) this.getConfig().getDouble("autoDeleteDays");
		this.config_nextToForce = this.getConfig().getBoolean("nextToForce");
		
		
		try {
			this.dataStore = new FlatFileDataStore();
		} catch (Exception e) {
			addLogEntry("Unable to initialize the file system data store. Details:");
			addLogEntry(e.getMessage());
		}
		

		// register for events
		PluginManager pluginManager = this.getServer().getPluginManager();

		// register block events
		BlockEventHandler blockEventHandler = new BlockEventHandler(dataStore);
		pluginManager.registerEvents(blockEventHandler, this);

		// register player events
		PlayerEventHandler playerEventHandler = new PlayerEventHandler(
				dataStore);
		pluginManager.registerEvents(playerEventHandler, this);

		// register entity events
		EntityEventHandler entityEventHandler = new EntityEventHandler(
				dataStore);
		pluginManager.registerEvents(entityEventHandler, this);

		// register world events
		WorldEventHandler worldEventHandler = new WorldEventHandler(dataStore);
		pluginManager.registerEvents(worldEventHandler, this);

		if (this.config_creditsPerHour > 0) {
			DeliverCreditsTask task = new DeliverCreditsTask();
			this.getServer()
					.getScheduler()
					.scheduleSyncRepeatingTask(this, task, 20L * 60 * 5,
							20L * 60 * 5);
		}

		// Example Claim (debugging)
		/*
		if (this.dataStore.chunks.size() == 0) {
			Chunk newChunk = new Chunk(0, 0, "Colony6", "Notch");
			this.dataStore.addChunk(newChunk);

		}
		*/
	}

	// handles slash commands
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {

		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		if (cmd.getName().equalsIgnoreCase("chunk") && player != null) {
			if(!ChunkClaim.plugin.config_worlds.contains(player.getWorld().getName())) return true;
			
			if (args.length == 0) {

				Chunk chunk = this.dataStore.getChunkAt(player.getLocation(),
						null);
				Location location = player.getLocation();
				PlayerData playerData = this.dataStore.getPlayerData(player
						.getName());

				if(player.hasPermission("chunkclaim.admin")) {
					String adminstring = "ID: " + location.getChunk().getX() + "|" + location.getChunk().getZ();
					if (chunk != null) {
						adminstring += ", Permanent: " + (chunk.modifiedBlocks<0?"true":("false ("+  chunk.modifiedBlocks + ")"));
						long loginDays = ((new Date()).getTime()-this.dataStore.getPlayerData(chunk.ownerName).lastLogin.getTime())/(1000 * 60 * 60 * 24);
						adminstring += ", Last Login: " + loginDays +" days ago.";
					}
					sendMsg(player,adminstring);

				}
				
				if (chunk == null) {

					sendMsg(player,"This chunk is public.");
					Visualization visualization = Visualization
							.FromBukkitChunk(location.getChunk(),
									location.getBlockY(),
									VisualizationType.Public, location);
					Visualization.Apply(player, visualization);
					return true;
				}


				else if (chunk.ownerName.equals(player.getName())) {

					if (chunk.builderNames.size() > 0) {

						StringBuilder builders = new StringBuilder();
						for (int i = 0; i < chunk.builderNames.size(); i++) {

							builders.append(chunk.builderNames.get(i));

							if (i < chunk.builderNames.size() - 1) {

								builders.append(", ");
							}
						}
						Visualization visualization = Visualization.FromChunk(
								chunk, location.getBlockY(),
								VisualizationType.Chunk, location);
						Visualization.Apply(player, visualization);
						sendMsg(player,"You own this chunk. Trusted Builders:");
						sendMsg(player,builders.toString());

					} else {
						sendMsg(player,"You own this chunk. Use /chunk trust <player> to add other builders.");
					}
					Visualization visualization = Visualization.FromChunk(
							chunk, location.getBlockY(),
							VisualizationType.Chunk, location);
					Visualization.Apply(player, visualization);
					return true;
				}

				else {

					if (chunk.isTrusted(player.getName())) {

						sendMsg(player,chunk.ownerName
								+ " owns this chunk. You have build rights!");
						if (playerData.lastChunk != chunk) {
							playerData.lastChunk = chunk;
							Visualization visualization = Visualization
									.FromChunk(chunk, location.getBlockY(),
											VisualizationType.Chunk, location);
							Visualization.Apply(player, visualization);
						}
					} else {

						sendMsg(player,chunk.ownerName
								+ " owns this chunk. You can't build here.");
						if (playerData.lastChunk != chunk) {

							playerData.lastChunk = chunk;
							Visualization visualization = Visualization
									.FromChunk(chunk, location.getBlockY(),
											VisualizationType.ErrorChunk,
											location);
							Visualization.Apply(player, visualization);
						}
					}
					return true;
				}
			}

			else if (args[0].equalsIgnoreCase("abandon")) {
				
				Chunk chunk = this.dataStore.getChunkAt(player.getLocation(), null);
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				Location location = player.getLocation();
				
				if(args.length==2) {
					
					int radius;
					int abd = 0;
					
					try {
							
						radius = Integer.parseInt(args[1]);
						
						if(radius<0) {
							
							sendMsg(player,"Error: Negative Radius");
							return true;
						}
						
						if(radius>10) {
							sendMsg(player,"Error: Max Radius is 10.");
							return true;
						}
						
						
						ArrayList<Chunk> chunksInRadius = this.getChunksInRadius(chunk, player.getName(),radius);
						
						
						for(int i=0; i<chunksInRadius.size();i++) {

							this.dataStore.deleteChunk(chunksInRadius.get(i));
							playerData.credits++;
							abd++;
							
							
						}
						
						this.dataStore.savePlayerData(player.getName(), playerData);
						
						sendMsg(player,abd + " Chunks abandoned in radius "+radius+". Credits: " + playerData.getCredits());

						return true;

					} catch(Exception e) {
						
						sendMsg(player,"Usage: /chunk abandon [radius]");
						return true;
					}

				}
				else if(args.length==1) {
					if (chunk == null) {

						sendMsg(player,"This chunk is public.");
						Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
						Visualization.Apply(player, visualization);
					}

					else if (chunk.ownerName.equals(player.getName())) {

						this.dataStore.deleteChunk(chunk);
						playerData.credits++;
						this.dataStore.savePlayerData(player.getName(), playerData);
						sendMsg(player,"Chunk abandoned. Credits: "	+ playerData.getCredits());

						Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(),VisualizationType.Public, location);
						Visualization.Apply(player, visualization);

						return true;
					}

					else {
						if (playerData.lastChunk != chunk) {
							playerData.lastChunk = chunk;
							Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(),VisualizationType.ErrorChunk, location);
							Visualization.Apply(player, visualization);
						}
						sendMsg(player,"You don't own this chunk. Only "
								+ chunk.ownerName + " or the staff can delete it.");
						return true;
					}
					
				} 
				
				else {
					sendMsg(player,"Usage: /chunk abandon [radius]");
					return true;
				}
			}

			else if (args[0].equalsIgnoreCase("credits")) {
				
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());				
				sendMsg(player,"You have " + playerData.getCredits() + " credits.");
				return true;
			}

			else if (args[0].equalsIgnoreCase("trust")) {
				
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				
				if(args.length!=2) {
					sendMsg(player,"Usage: /chunk trust <player>");
					return true;
					
				}
				
				OfflinePlayer tp = resolvePlayer(args[1]);
				if (tp == null) {

					sendMsg(player,"Player not found.");
					return true;
				}
				String tName = tp.getName();
				if(tName.equals(player.getName())) {
					sendMsg(player,"You don't trust yourself?");
					return true;
				}
				

				ArrayList<Chunk> chunksInRadius = this.dataStore.getAllChunksForPlayer(player.getName());
				
				if(!playerData.builderNames.contains(tName)) {
					
					for(int i=0; i<chunksInRadius.size();i++) {
						if(!chunksInRadius.get(i).isTrusted(tName)) {
							chunksInRadius.get(i).builderNames.add(tName);
							dataStore.writeChunkToStorage(chunksInRadius.get(i));
						}
						
					}
					playerData.builderNames.add(tName);
					this.dataStore.savePlayerData(player.getName(), playerData);
					
				}
				sendMsg(player,"Trusted " + tName+ " in all your chunks.");
				return true;
				
				
				/*
				Chunk chunk = this.dataStore.getChunkAt(player.getLocation(), null);
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				Location location = player.getLocation();
				if(args.length==3) {
					
					int radius;
					int abd = 0;
					
					try {
						
		
						radius = Integer.parseInt(args[2]);
						sendMsg(player,"Radius: " + radius);
						
						OfflinePlayer tp = resolvePlayer(args[1]);
						if (tp == null) {

							sendMsg(player,"Player not found.");
							return true;
						}
						String tName = tp.getName();
						if(tName.equals(player.getName())) {
							sendMsg(player,"You don't trust yourself?");
							return true;
						}
						
						if(radius<0) {
							
							sendMsg(player,"Error: Negative Radius");
							return true;
						}
						
						if(radius>10) {
							sendMsg(player,"Error: Max Radius is 10.");
							return true;
						}
						
						ArrayList<Chunk> chunksInRadius = this.getChunksInRadius(chunk, player.getName(),radius);
						
						
						
						for(int i=0; i<chunksInRadius.size();i++) {
							if(!chunksInRadius.get(i).isTrusted(tName)) {
								chunksInRadius.get(i).builderNames.add(tName);
								dataStore.writeChunkToStorage(chunksInRadius.get(i));
								abd++;
							}
							
						}
						
						this.dataStore.savePlayerData(player.getName(), playerData);
						sendMsg(player,"Trusted " + tName+ " in " + abd + " Chunks.");

						return true;
						
					
					} catch(Exception e) {
						
						sendMsg(player,"Usage: /chunk trust <player> [radius]");
						return true;
					}
					

				}
				else if(args.length==2) {
					if (chunk == null) {

						sendMsg(player,"This chunk is public. Claim this chunk by building something in it.");
						Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
						Visualization.Apply(player, visualization);
					}

					else if (chunk.ownerName.equals(player.getName())) {

						OfflinePlayer tp = resolvePlayer(args[1]);
						if (tp != null) {
							String tName = tp.getName();
							if(tName.equals(player.getName())) {
								sendMsg(player,"You don't trust yourself?");
								return true;
							}
							if(!chunk.isTrusted(tName)) {
								chunk.builderNames.add(tName);
								dataStore.writeChunkToStorage(chunk);
								sendMsg(player,"Trusted " + tName
										+ " in this chunk.");
							}
						} else {
							sendMsg(player,"Player not found.");
						}

						return true;
					}

					else {
						if (playerData.lastChunk != chunk) {
							playerData.lastChunk = chunk;
							Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(),VisualizationType.ErrorChunk, location);
							Visualization.Apply(player, visualization);
						}
						sendMsg(player,"You don't own this chunk.");
						return true;
					}
					
				} 
				
				else {
					sendMsg(player,"Usage: /chunk trust <player> [radius]");
					return true;
				}
				*/

			} 
			//UNTRUST
			else if (args[0].equalsIgnoreCase("untrust")) {
				
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				
				if(args.length!=2) {
					sendMsg(player,"Usage: /chunk untrust <player>");
					return true;
					
				}
				
				OfflinePlayer tp = resolvePlayer(args[1]);
				if (tp == null) {

					sendMsg(player,"Player not found.");
					return true;
				}
				String tName = tp.getName();
				if(tName.equals(player.getName())) {
					sendMsg(player,"You don't trust yourself?");
					return true;
				}
				

				ArrayList<Chunk> chunksInRadius = this.dataStore.getAllChunksForPlayer(player.getName());
				
				if(playerData.builderNames.contains(tName)) {
					
					for(int i=0; i<chunksInRadius.size();i++) {
						if(chunksInRadius.get(i).isTrusted(tName)) {
							chunksInRadius.get(i).builderNames.remove(tName);
							dataStore.writeChunkToStorage(chunksInRadius.get(i));
						}
						
					}
					playerData.builderNames.remove(tName);
					this.dataStore.savePlayerData(player.getName(), playerData);
					
				}
				
				
				sendMsg(player,"Untrusted " + tName+ " in all your chunks.");
				return true;
				
				/*
				Chunk chunk = this.dataStore.getChunkAt(player.getLocation(), null);
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				Location location = player.getLocation();
				
				if(args.length==3) {
					
					int radius;
					int abd = 0;
					
					try {
							
						radius = Integer.parseInt(args[2]);
						sendMsg(player,"Radius: " + radius);
						
						OfflinePlayer tp = resolvePlayer(args[1]);
						if (tp == null) {

							sendMsg(player,"Player not found.");
							return true;
						}
						String tName = tp.getName();
						if(tName.equals(player.getName())) {
							sendMsg(player,"You don't trust yourself?");
							return true;
						}
						
						if(radius<0) {
							
							sendMsg(player,"Error: Negative Radius");
							return true;
						}
						
						if(radius>10) {
							sendMsg(player,"Error: Max Radius is 10.");
							return true;
						}
						
						ArrayList<Chunk> chunksInRadius = this.getChunksInRadius(chunk, player.getName(),radius);
						
						
						
						for(int i=0; i<chunksInRadius.size();i++) {
							if(chunksInRadius.get(i).isTrusted(tName)) {
								chunksInRadius.get(i).builderNames.remove(tName);
								dataStore.writeChunkToStorage(chunksInRadius.get(i));
								abd++;
							}
							
						}
						
						this.dataStore.savePlayerData(player.getName(), playerData);
						sendMsg(player,"Untrusted " + tName+ " in " + abd + " Chunks.");

						return true;

					} catch(Exception e) {
						
						sendMsg(player,"Usage: /chunk untrust <player> [radius]");
						return true;
					}

				}
				else if(args.length==2) {
					if (chunk == null) {

						sendMsg(player,"This chunk is public. Claim this chunk by building something in it.");
						Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
						Visualization.Apply(player, visualization);
					}

					else if (chunk.ownerName.equals(player.getName())) {

						OfflinePlayer tp = resolvePlayer(args[1]);
						if (tp != null) {
							String tName = tp.getName();
							if(tName.equals(player.getName())) {
								sendMsg(player,"You don't trust yourself?");
								return true;
							}
							
							if(chunk.isTrusted(tName)) {
								chunk.builderNames.remove(tName);
								dataStore.writeChunkToStorage(chunk);
								sendMsg(player,"Untrusted " + tName
										+ " in this chunk.");
							}
						} else {
							sendMsg(player,"Player not found.");
						}

						return true;
					}

					else {
						if (playerData.lastChunk != chunk) {
							playerData.lastChunk = chunk;
							Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(),VisualizationType.ErrorChunk, location);
							Visualization.Apply(player, visualization);
						}
						sendMsg(player,"You don't own this chunk.");
						return true;
					}
					
				} 
				
				else {
					sendMsg(player,"Usage: /chunk untrust <player> [radius]");
					return true;
				}
				*/
			}
			
			
			
			else if (args[0].equalsIgnoreCase("ignore")) {
				if(!player.hasPermission("chunkclaim.admin")) {
					sendMsg(player,"No permission.");
					return true;
				}
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				playerData.ignorechunks = !playerData.ignorechunks;
				if(playerData.ignorechunks) {
					sendMsg(player,"You now ignore chunks.");
				} else {
					sendMsg(player,"You now respect chunks.");
				}
				return true;
				
			} 
			
			else if (args[0].equalsIgnoreCase("delete")) {
				
				if(!player.hasPermission("chunkclaim.admin")) {
					sendMsg(player,"No permission.");
					return true;
				}
				

				Location location = player.getLocation();
				
				if(args.length==3) {
					
					int radius;
					int abd = 0;
					
					try {
							
						radius = Integer.parseInt(args[2]);
						
						if(radius<0) {
							
							sendMsg(player,"Error: Negative Radius");
							return true;
						}
						
						if(radius>10) {
							sendMsg(player,"Error: Max Radius is 10.");
							return true;
						}
						OfflinePlayer tp = resolvePlayer(args[1]);
						if (tp == null) {

							sendMsg(player,"Player not found.");
							return true;
						}
						String tName = tp.getName();
						PlayerData playerData = this.dataStore.getPlayerData(tName);
						
						org.bukkit.Chunk bukkitChunk = location.getChunk();
						Chunk chunk = new Chunk(bukkitChunk.getX(),bukkitChunk.getZ(),bukkitChunk.getWorld().getName());
						ArrayList<Chunk> chunksInRadius = this.getChunksInRadius(chunk, tName,radius);
						
						
						for(int i=0; i<chunksInRadius.size();i++) {

							this.dataStore.deleteChunk(chunksInRadius.get(i));
							playerData.credits++;
							abd++;
							
							
						}
						
						this.dataStore.savePlayerData(tName, playerData);
						
						sendMsg(player,abd + " Chunks deleted in radius "+radius+".");

						return true;

					} catch(Exception e) {
						
						sendMsg(player,"Usage: /chunk delete [<player> <radius>]");
						return true;
					}

				}
				else if(args.length==1) {
					Chunk chunk = this.dataStore.getChunkAt(player.getLocation(), null);
					
					
					if (chunk == null) {

						sendMsg(player,"This chunk is public.");
						Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
						Visualization.Apply(player, visualization);
					}

					else {
						PlayerData playerData = this.dataStore.getPlayerData(chunk.ownerName);
						this.dataStore.deleteChunk(chunk);
						playerData.credits++;
						this.dataStore.savePlayerData(chunk.ownerName, playerData);
						sendMsg(player,"Chunk deleted.");

						Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(),VisualizationType.Public, location);
						Visualization.Apply(player, visualization);

						return true;
					}
					
				} 
				
				else {
					sendMsg(player,"Usage: /chunk delete [<player> <radius>]");
					return true;
				}
				
			} 
			else if (args[0].equalsIgnoreCase("deleteall")) {
				if(!player.hasPermission("chunkclaim.admin")) {
					sendMsg(player,"No permission.");
					return true;
				}
				if(args.length==2) {
					OfflinePlayer tp = resolvePlayer(args[1]);
					if (tp == null) {
	
						sendMsg(player,"Player not found.");
						return true;
					}
					String tName = tp.getName();
					
					sendMsg(player,dataStore.deleteChunksForPlayer(tName) +" chunks deleted.");
					return true;
				}
				else {
					sendMsg(player,"Usage: /chunk deleteall <player>");
					return true;
				}
				
			} 
			else if (args[0].equalsIgnoreCase("bonus")) {
				if(!player.hasPermission("chunkclaim.admin")) {
					sendMsg(player,"No permission.");
					return true;
				}
				if(args.length==3) {
					try {
						
						int bonus = Integer.parseInt(args[2]);
						
						OfflinePlayer tp = resolvePlayer(args[1]);
						if (tp == null) {

							sendMsg(player,"Player not found.");
							return true;
						}
						String tName = tp.getName();
						PlayerData playerData = this.dataStore.getPlayerData(tName);
						
						playerData.credits+=bonus;
						playerData.bonus+=bonus;
						
					
						sendMsg(player,"Adjusted " + tName + "'s bonus by " + bonus + " credits. Total credits: " + playerData.getCredits());
						
						this.dataStore.savePlayerData(player.getName(), playerData);


						return true;

					} catch(Exception e) {
						
						sendMsg(player,"Usage: /chunk bonus <player> <credits>");
						return true;
					}
				}
				else {
					sendMsg(player,"Usage: /chunk bonus <player> <credits>");
					return true;
				}
				
			} 
			else if (args[0].equalsIgnoreCase("claim")) {
				if(args.length==1) {
					
					Location location = player.getLocation();
					if(!ChunkClaim.plugin.config_worlds.contains(location.getWorld().getName())) return true;
					
					PlayerData playerData = dataStore.getPlayerData(player.getName());
					Chunk chunk = dataStore.getChunkAt(location, playerData.lastChunk);
					
					if(chunk == null) {
						String playerName = player.getName();
						
						if(!player.hasPermission("chunkclaim.claim")) {
							sendMsg(player,"You don't have permissions for claiming chunks.");
							return true;
						}
						if(playerData.getCredits() > 0 && (playerData.chunksOwning == 0 || !this.config_nextToForce)) {
							Chunk newChunk = new Chunk(location,playerName,playerData.builderNames);
							
							this.dataStore.addChunk(newChunk);
							
							playerData.credits--;
							playerData.chunksOwning++;
							playerData.lastChunk=newChunk;
							//newChunk.modify();
							this.dataStore.savePlayerData(playerName, playerData);
							
							sendMsg(player,"You claimed this chunk. Credits left: " + playerData.getCredits());
							
							Visualization visualization = Visualization.FromChunk(newChunk, location.getBlockY(), VisualizationType.Chunk, location);
							Visualization.Apply(player, visualization);
							
						}
						else if (playerData.getCredits() > 0 && this.config_nextToForce){
							sendMsg(player,"The chunk must be next to your other chunks.");
							if(playerData.lastChunk!=chunk) {
								playerData.lastChunk=chunk;
								Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
								Visualization.Apply(player, visualization);
							}
						}
						else {
							
							sendMsg(player,"Not enough credits to claim this chunk.");
							
							if(playerData.lastChunk!=chunk) {
								playerData.lastChunk=chunk;
								Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
								Visualization.Apply(player, visualization);
							}
						}
						return true;
					} else {
						sendMsg(player,"This chunk is not public.");
					}
				}
				else {
					sendMsg(player,"Usage: /chunk claim");
					return true;
				}
				
			} 
			else if (args[0].equalsIgnoreCase("list")) {
				if(player.hasPermission("chunkclaim.admin")) {
					if(args.length==2) {
						
	
						OfflinePlayer tp = resolvePlayer(args[1]);
						if (tp == null) {
	
							sendMsg(player,"Player not found.");
							return true;
						}
						String tName = tp.getName();
	
						ArrayList<Chunk> chunksInRadius = this.dataStore.getAllChunksForPlayer(tName);
	
						long loginDays = ((new Date()).getTime()-this.dataStore.getPlayerData(tp.getName()).lastLogin.getTime())/(1000 * 60 * 60 * 24);
						long joinDays = ((new Date()).getTime()-this.dataStore.getPlayerData(tp.getName()).firstJoin.getTime())/(1000 * 60 * 60 * 24);
						String adminstring = tp.getName() + " | Last Login: " + loginDays +" days ago. First Join: " + joinDays + " days ago.";
						sendMsg(player,adminstring);
							
						for(int i=0; i<chunksInRadius.size();i++) {
							
							Chunk chunk = chunksInRadius.get(i);
			
	
							adminstring = "ID: " + chunk.x + "|" + chunk.z + "("+(chunk.x*16) + "|" + (chunk.z*16)+")";
							if (chunk != null) {
								adminstring += ", Permanent: " + (chunk.modifiedBlocks<0?"true":("false ("+  chunk.modifiedBlocks + ")"));

							}
							sendMsg(player,adminstring);
			

							
						}
						return true;
					}
					else {
						sendMsg(player,"Usage: /chunk list <player>");
						return true;
					}
			} else return false;
				
			} 
			else {
				return false;
			}
		}
		return false;
	}

	public ArrayList<Chunk> getChunksInRadius(Chunk chunk, String playerName, int radius) {
		
		ArrayList<Chunk> chunksInRadius = new ArrayList<Chunk>();

		for(int x = chunk.x-radius; x <= chunk.x+radius; x++) {
			for(int z = chunk.z-radius; z <= chunk.z+radius; z++) {
				
				Chunk foundChunk = this.dataStore.getChunkAtPos(x,z,chunk.worldName);
				
				if(foundChunk!=null && foundChunk.ownerName.equals(playerName)) {
					
					chunksInRadius.add(foundChunk);
					
				}
				
			}
		}
		
		return chunksInRadius;
	}
	
	public void regenerateChunk(Chunk chunk) {
		getServer().getWorld(chunk.worldName).regenerateChunk(chunk.x, chunk.z);
		getServer().getWorld(chunk.worldName).unloadChunkRequest(chunk.x, chunk.z);

	}

	public OfflinePlayer resolvePlayer(String name) {

		Player player = this.getServer().getPlayer(name);
		if (player != null)
			return player;

		// then search offline players
		OfflinePlayer[] offlinePlayers = this.getServer().getOfflinePlayers();
		for (int i = 0; i < offlinePlayers.length; i++) {
			if (offlinePlayers[i].getName().equalsIgnoreCase(name)) {
				return offlinePlayers[i];
			}
		}

		// if none found, return null
		return null;

	}

	public static void addLogEntry(String entry) {
		logger.info("ChunkClaim: " + entry);
	}
	public void sendMsg(Player player, String message) {
		player.sendMessage(ChatColor.YELLOW +  message);
	}

	public void broadcast(String message) {
		Player[] players = Bukkit.getServer().getOnlinePlayers();
		for(int i = 0; i < players.length; i++) {
			Player player = players[i];
			player.sendMessage(message);
		}
	}
}
