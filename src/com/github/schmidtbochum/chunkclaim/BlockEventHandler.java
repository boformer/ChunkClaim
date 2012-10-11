package com.github.schmidtbochum.chunkclaim;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockEventHandler implements Listener {
	
	private DataStore dataStore;
	
	public BlockEventHandler(DataStore dataStore) {
		this.dataStore = dataStore;
	}
	
	//when a player breaks a block...
	@EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Location location = block.getLocation();

		Chunk chunk = dataStore.getChunkAt(location, null);
		
		if(chunk == null) {
			String playerName = player.getName();
			PlayerData playerData = this.dataStore.getPlayerData(playerName);
			
			if(playerData.credits > 0) {
				Chunk newChunk = new Chunk(location,playerName);
				this.dataStore.addChunk(newChunk);
				playerData.credits--;
				this.dataStore.savePlayerData(playerName, playerData);
				player.sendMessage("You claimed this chunk. Credits left: " + playerData.credits);
			} else {
				player.sendMessage("Not enough credits to claim this chunk.");
				event.setCancelled(true);
			}
			return;
		}
		if(chunk.ownerName.equals(player.getName())) {
			return;
		} else {
			player.sendMessage("You don't have " + chunk.ownerName + "'s permission to build here.");
			event.setCancelled(true);
			return;
		}
	}	
	
	//when a player places a block...
	@EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Location location = block.getLocation();

		Chunk chunk = dataStore.getChunkAt(location, null);
		
		if(chunk == null) {
			String playerName = player.getName();
			PlayerData playerData = this.dataStore.getPlayerData(playerName);
			
			if(playerData.credits > 0) {
				Chunk newChunk = new Chunk(location,playerName);
				this.dataStore.addChunk(newChunk);
				playerData.credits--;
				this.dataStore.savePlayerData(playerName, playerData);
				player.sendMessage("You claimed this chunk. Credits left: " + playerData.credits);
			} else {
				player.sendMessage("Not enough credits to claim this chunk.");
				event.setCancelled(true);
			}
			return;
		}
		if(chunk.ownerName.equals(player.getName())) {
			return;
		} else {
			player.sendMessage("You don't have " + chunk.ownerName + "'s permission to build here.");
			event.setCancelled(true);
			return;
		}
	}	
}
