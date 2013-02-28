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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BlockEventHandler implements Listener {
	
	private DataStore dataStore;
	
	public BlockEventHandler(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	
	//when a player breaks a block...
	@EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockBreak(BlockBreakEvent event) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(event.getBlock().getWorld().getName())) return;
		
		
		
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Location location = block.getLocation();
		
		
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Chunk chunk = dataStore.getChunkAt(location, playerData.lastChunk);
		
		if(playerData.ignorechunks) return;
		
		if(chunk == null) {
			String playerName = player.getName();
			
			if(!player.hasPermission("chunkclaim.claim")) {
				ChunkClaim.plugin.sendMsg(player,"You don't have permissions for claiming chunks.");
				event.setCancelled(true);
				return;
			}
			if(!dataStore.ownsNear(location, playerName)) {
				ChunkClaim.plugin.sendMsg(player,"You don't own a chunk next to this one.");
				if(!ChunkClaim.plugin.config_nextToForce || playerData.chunksOwning == 0)
					ChunkClaim.plugin.sendMsg(player,"Confirm with /chunk claim. Please dont spam claimed chunks.");
				event.setCancelled(true);
				Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
				Visualization.Apply(player, visualization);
				return;
			} else
			
			
			if(playerData.getCredits() > 0) {
				Chunk newChunk = new Chunk(location,playerName,playerData.builderNames);
				
				this.dataStore.addChunk(newChunk);
				
				playerData.credits--;
				playerData.chunksOwning++;
				playerData.lastChunk=newChunk;
				//newChunk.modify();
				this.dataStore.savePlayerData(playerName, playerData);
				
				ChunkClaim.plugin.sendMsg(player,"You claimed this chunk. Credits left: " + playerData.getCredits());
				
				Visualization visualization = Visualization.FromChunk(newChunk, location.getBlockY(), VisualizationType.Chunk, location);
				Visualization.Apply(player, visualization);
				
			} else {
				
				
				
				ChunkClaim.plugin.sendMsg(player,"Not enough credits to claim this chunk.");
				
				
				if(playerData.lastChunk!=chunk) {
					playerData.lastChunk=chunk;
					Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
					Visualization.Apply(player, visualization);
				}
				
				event.setCancelled(true);
			}
			return;
		}
		else if(chunk.isTrusted(player.getName())) {
			
			/*
			if(playerData.lastChunk!=chunk) {
				playerData.lastChunk=chunk;
				Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.Chunk, location);
				Visualization.Apply(player, visualization);
			}
			*/
			//chunk.modify();
			
			return;
		} else {
			
			
			ChunkClaim.plugin.sendMsg(player,"You don't have " + chunk.ownerName + "'s permission to build here.");
			
			if(playerData.lastChunk!=chunk) {
				playerData.lastChunk=chunk;
				Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.ErrorChunk, location);
				Visualization.Apply(player, visualization);
			}
			
			event.setCancelled(true);
			return;
		}
	}	
	
	//when a player places a block...
	@EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(event.getBlock().getWorld().getName())) return;
		
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Location location = block.getLocation();

		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Chunk chunk = dataStore.getChunkAt(location, playerData.lastChunk);
		
		if(playerData.ignorechunks) return;
		
		if(chunk == null) {
			
			if(!player.hasPermission("chunkclaim.claim")) {
				ChunkClaim.plugin.sendMsg(player,"You don't have permissions for claiming chunks.");
				event.setCancelled(true);
			}
			else{
			
				String playerName = player.getName();
				
				//prevent fire spam
				if(block.getType() == Material.FIRE) {
					event.setCancelled(true);
					return;
				}			
				//prevent tree spam
				if(block.getType() == Material.SAPLING) {
					ChunkClaim.plugin.sendMsg(player,"Please dont spam chunks with trees.");
					event.setCancelled(true);
					return;
				}
				
				//prevent not nextTo chunks be claimed without command
				if(!dataStore.ownsNear(location, playerName)) {
					ChunkClaim.plugin.sendMsg(player,"You don't own a chunk next to this one.");
					if(!ChunkClaim.plugin.config_nextToForce || playerData.chunksOwning == 0)
						ChunkClaim.plugin.sendMsg(player,"Confirm with /chunk claim. Please dont spam claimed chunks.");
					Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
					Visualization.Apply(player, visualization);
					event.setCancelled(true);
					return;
				} else
				
				//check credits
				if(playerData.getCredits() <= 0) {
					ChunkClaim.plugin.sendMsg(player,"Not enough credits to claim this chunk.");
					
					Visualization visualization = Visualization.FromBukkitChunk(location.getChunk(), location.getBlockY(), VisualizationType.Public, location);
					Visualization.Apply(player, visualization);
					
					event.setCancelled(true);
					return;
				}
				//claim the chunk
				Chunk newChunk = new Chunk(location,playerName,playerData.builderNames);
				
				this.dataStore.addChunk(newChunk);
				
				playerData.credits--;
				playerData.chunksOwning++;
				playerData.lastChunk=newChunk;
				newChunk.modify();
				this.dataStore.savePlayerData(playerName, playerData);
				
				ChunkClaim.plugin.sendMsg(player,"You claimed this chunk. Credits left: " + playerData.getCredits());
				
				Visualization visualization = Visualization.FromChunk(newChunk, location.getBlockY(), VisualizationType.Chunk, location);
				Visualization.Apply(player, visualization);
				return;
			}
		}
		else if(chunk.isTrusted(player.getName())) {
			/*
			if(playerData.lastChunk!=chunk) {
				playerData.lastChunk=chunk;
				Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.Chunk, location);
				Visualization.Apply(player, visualization);
			}
			*/
			chunk.modify();
			
			return;
		} else {
			ChunkClaim.plugin.sendMsg(player,"You don't have " + chunk.ownerName + "'s permission to build here.");
			
			if(playerData.lastChunk!=chunk) {
				playerData.lastChunk=chunk;
				Visualization visualization = Visualization.FromChunk(chunk, location.getBlockY(), VisualizationType.ErrorChunk, location);
				Visualization.Apply(player, visualization);
			}
			
			
			event.setCancelled(true);
			return;
		}
	}
	//blocks "pushing" other players' blocks around (pistons)
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockPistonExtend (BlockPistonExtendEvent event) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(event.getBlock().getWorld().getName())) return;
		
		List<Block> blocks = event.getBlocks();
		
		Block piston = event.getBlock();
		Chunk pistonChunk = this.dataStore.getChunkAt(piston.getLocation(), null);	
		String pistonOwnerName = (pistonChunk==null)? null : pistonChunk.ownerName;	
		
		//if no blocks moving, then only check to make sure we're not pushing into a claim from outside
		//this avoids pistons breaking non-solids just inside a claim, like torches, doors, and touchplates
		if(blocks.size() == 0) {

			Block invadedBlock = piston.getRelative(event.getDirection());
			Chunk invadedBlockChunk = this.dataStore.getChunkAt(invadedBlock.getLocation(), null);
			String invadedBlockOwnerName = (invadedBlockChunk==null)? null : invadedBlockChunk.ownerName;

			if(pistonOwnerName==null || invadedBlockOwnerName==null || (!invadedBlockChunk.isTrusted(pistonOwnerName))) {
				event.setCancelled(true);	
				return;
			}
			return;
		}
		
		
		Chunk chunk;
		//which blocks are being pushed?
		for(int i = 0; i < blocks.size(); i++) {
			//if ANY of the pushed blocks are owned by someone other than the piston owner, cancel the event
			Block block = blocks.get(i);
			chunk = this.dataStore.getChunkAt(block.getLocation(), null);
			if(chunk == null || !chunk.isTrusted(pistonOwnerName)) {
				event.setCancelled(true);
				event.getBlock().getWorld().createExplosion(event.getBlock().getLocation(), 0);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(event.getBlock().getType()));
				event.getBlock().setType(Material.AIR);
				return;
			}
		}
		
		Block block = blocks.get(blocks.size()-1).getRelative(event.getDirection());

		chunk = this.dataStore.getChunkAt(block.getLocation(), null);
		if(chunk == null || !chunk.isTrusted(pistonOwnerName)) {
			event.setCancelled(true);
			event.getBlock().getWorld().createExplosion(event.getBlock().getLocation(), 0);
			event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(event.getBlock().getType()));
			event.getBlock().setType(Material.AIR);
			return;
		}


	}
	//blocks theft by pulling blocks out of a claim (again pistons)
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockPistonRetract (BlockPistonRetractEvent event) {

		if(!ChunkClaim.plugin.config_worlds.contains(event.getBlock().getWorld().getName())) return;
		
		//we only care about sticky pistons
		if(!event.isSticky()) return;

		//who owns the moving block, if anyone?
		Chunk movingBlockChunk = this.dataStore.getChunkAt(event.getRetractLocation(), null);
		if(movingBlockChunk == null) {
			event.setCancelled(true);
			return;
		}		
		String movingBlockOwnerName = movingBlockChunk.ownerName;
		
		//who owns the piston, if anyone?
		Chunk pistonChunk = this.dataStore.getChunkAt(event.getBlock().getLocation(), null);
		if(pistonChunk == null) {
			event.setCancelled(true);
			return;
		}
		String pistonOwnerName = pistonChunk.ownerName;
		
		if((!pistonChunk.isTrusted(movingBlockOwnerName) && !movingBlockChunk.isTrusted(pistonOwnerName))) {
			event.setCancelled(true);	
			return;
		}
	}
	
	//blocks are ignited ONLY by flint and steel
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockIgnite (BlockIgniteEvent igniteEvent) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(igniteEvent.getBlock().getWorld().getName())) return;
		
		if(igniteEvent.getCause() != IgniteCause.FLINT_AND_STEEL) {	
			igniteEvent.setCancelled(true);
		}
	}
	
	//fire doesn't spread, but other blocks still do (mushrooms and vines, for example)
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockSpread (BlockSpreadEvent spreadEvent) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(spreadEvent.getBlock().getWorld().getName())) return;
		
		if(spreadEvent.getSource().getType() == Material.FIRE) {
			spreadEvent.setCancelled(true);
		}
	}
	
	//blocks are not destroyed by fire
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockBurn (BlockBurnEvent burnEvent) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(burnEvent.getBlock().getWorld().getName())) return;
		burnEvent.setCancelled(true);
	}
	
	//ensures fluids don't flow out of chunks, unless into another chunk where the owner is trusted to build
	private Chunk lastSpreadChunk = null;
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onBlockFromTo (BlockFromToEvent spreadEvent) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(spreadEvent.getBlock().getWorld().getName())) return;
		
		//always allow fluids to flow straight down
		if(spreadEvent.getFace() == BlockFace.DOWN) return;
		
		//from where?
		Block fromBlock = spreadEvent.getBlock();
		Chunk fromChunk = this.dataStore.getChunkAt(fromBlock.getLocation(), this.lastSpreadChunk);
		if(fromChunk != null) {
			this.lastSpreadChunk = fromChunk;
		}
		
		//where to?
		Block toBlock = spreadEvent.getToBlock();	
		Chunk toChunk = this.dataStore.getChunkAt(toBlock.getLocation(), fromChunk);
		
		//if it's within the same claim or wilderness to wilderness, allow it
		if(fromChunk == toChunk) return;
		
		//block any spread into the wilderness from a claim
		if(fromChunk != null && toChunk == null) {
			spreadEvent.setCancelled(true);
			return;
		}
		//if spreading into a claim
		else if(toChunk != null) {	
			//who owns the spreading block, if anyone?
			String fromOwner = null;	
			if(fromChunk != null) {	
				fromOwner = fromChunk.ownerName;
			}

			//cancel unless the owner of the spreading block is allowed to build in the receiving claim
			if(fromOwner == null || !toChunk.isTrusted(fromOwner))	{
				spreadEvent.setCancelled(true);
			}
		}
	}
	
	//ensures dispensers can't be used to dispense a block(like water or lava) or item across a claim boundary
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onDispense(BlockDispenseEvent dispenseEvent) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(dispenseEvent.getBlock().getWorld().getName())) return;
		
		//from where?
		Block fromBlock = dispenseEvent.getBlock();

		//to where?
		Vector velocity = dispenseEvent.getVelocity();
		int xChange = 0;
		int zChange = 0;
		if(Math.abs(velocity.getX()) > Math.abs(velocity.getZ())) {
			if(velocity.getX() > 0) xChange = 1; else xChange = -1;	
		} else {
			if(velocity.getZ() > 0) zChange = 1; else zChange = -1;
		}

		Block toBlock = fromBlock.getRelative(xChange, 0, zChange);
		
		Chunk fromChunk = this.dataStore.getChunkAt(fromBlock.getLocation(), null);
		Chunk toChunk = this.dataStore.getChunkAt(toBlock.getLocation(), fromChunk);
		
		Material materialDispensed = dispenseEvent.getItem().getType();
		if(materialDispensed == Material.WATER_BUCKET || materialDispensed == Material.LAVA_BUCKET) {
			
			//wilderness is NOT OK
			if(fromChunk == null || toChunk == null) {
				dispenseEvent.setCancelled(true);
				return;
			}
	
			//within chunks is OK
			if(fromChunk == toChunk) return;
			
			//chunks are ok
			if(toChunk.isTrusted(fromChunk.ownerName))	{
				return;
			}
			
			//everything else is NOT OK
			dispenseEvent.setCancelled(true);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onTreeGrow (StructureGrowEvent growEvent) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(growEvent.getLocation().getBlock().getWorld().getName())) return;
		
		
		Location rootLocation = growEvent.getLocation();
		Chunk rootChunk = this.dataStore.getChunkAt(rootLocation, null);
		String rootOwnerName = (rootChunk==null) ? null : rootChunk.ownerName;
		
		//for each block growing
		for(int i = 0; i < growEvent.getBlocks().size(); i++) {
			BlockState block = growEvent.getBlocks().get(i);
			Chunk blockChunk = this.dataStore.getChunkAt(block.getLocation(), rootChunk);
			if(blockChunk != null) {
				if(rootOwnerName == null || !blockChunk.isTrusted(rootOwnerName)) {
					growEvent.getBlocks().remove(i--);
				}
			} else if(blockChunk == null && rootOwnerName != null) {
				growEvent.getBlocks().remove(i--);
			}
		}
	}
}

