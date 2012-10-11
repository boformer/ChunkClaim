package com.github.schmidtbochum.chunkclaim;

import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.PoweredMinecart;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEventHandler implements Listener {

	private DataStore dataStore;
	
	public PlayerEventHandler(DataStore dataStore) {
		this.dataStore = dataStore;
	}
	
	//when a player successfully joins the server...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	void onPlayerJoin(PlayerJoinEvent event) {
		
		String playerName = event.getPlayer().getName();
		
		//note login time
		PlayerData playerData = this.dataStore.getPlayerData(playerName);
		playerData.lastLogin = new Date();
		
		if(playerData.firstJoin==null) {
			playerData.firstJoin = new Date();
		}
		event.getPlayer().sendMessage(ChatColor.DARK_RED + "Running ChunkClaim Alpha. ONLY FOR TESTING!");
		this.dataStore.savePlayerData(playerName, playerData);
	}
	
	//when a player quits...
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//make sure his data is all saved
		this.dataStore.savePlayerData(player.getName(), playerData);
		
		//drop data about this player
		this.dataStore.clearCachedPlayerData(player.getName());
	}
	
	//when a player drops an item
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		
		//check if player has drop permission
		if(!player.hasPermission("chunkclaim.drops")) {
			event.setCancelled(true);
		}
	}
	
	//when a player interacts with an entity...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		
		Chunk chunk = this.dataStore.getChunkAt(entity.getLocation(), null);
		
		if(chunk!=null) {
			if(entity instanceof StorageMinecart || entity instanceof PoweredMinecart || entity instanceof Animals) {
				if(!chunk.isTrusted(player)) {
					player.sendMessage("Not permitted.");
					event.setCancelled(true);
				}
				
			}
			
		}
	}
	
	//block players from entering beds they don't have permission for
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST) 
	public void onPlayerBedEnter (PlayerBedEnterEvent bedEvent) {
		Player player = bedEvent.getPlayer();
		Block block = bedEvent.getBed();
		
		Chunk chunk = this.dataStore.getChunkAt(block.getLocation(), null);
		
		if(chunk!=null) {
			if(!chunk.isTrusted(player)) {
				player.sendMessage("Not permitted.");
				bedEvent.setCancelled(true);
			}
		}
	}
	
	//block use of buckets within other players' claims
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerBucketEmpty (PlayerBucketEmptyEvent bucketEvent) {
		Player player = bucketEvent.getPlayer();
		Block block = bucketEvent.getBlockClicked().getRelative(bucketEvent.getBlockFace());
		
		Chunk chunk = this.dataStore.getChunkAt(block.getLocation(), null);
		
		if(chunk == null) {
			bucketEvent.setCancelled(true);
			return;
		}
		if(chunk.ownerName.equals(player.getName())) {
			return;
		} else {
			player.sendMessage("You don't have " + chunk.ownerName + "'s permission to build here.");
			bucketEvent.setCancelled(true);
			return;
		}
		
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerBucketFill (PlayerBucketFillEvent bucketEvent) {
		Player player = bucketEvent.getPlayer();
		Block block = bucketEvent.getBlockClicked();
		Chunk chunk = this.dataStore.getChunkAt(block.getLocation(), null);
		
		if(chunk == null) {
			bucketEvent.setCancelled(true);
			return;
		}
		if(chunk.ownerName.equals(player.getName())) {
			return;
		} else {
			player.sendMessage("You don't have " + chunk.ownerName + "'s permission.");
			bucketEvent.setCancelled(true);
			return;
		}
	}
	
}
