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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;


//represents a visualization sent to a player
public class Visualization {
	public ArrayList<VisualizationElement> elements = new ArrayList<VisualizationElement>();
	
	
	
	//sends a visualization to a player
	public static void Apply(Player player, Visualization visualization) {
		
		PlayerData playerData = ChunkClaim.plugin.dataStore.getPlayerData(player.getName());
		
		//if he has any current visualization, clear it first
		if(playerData.currentVisualization != null)	{
			Visualization.Revert(player);
		}
		//if he's online, create a task to send him the visualization in about half a second
		if(player.isOnline()) {
			playerData.currentVisualization = visualization;
			ChunkClaim.plugin.getServer().getScheduler().scheduleSyncDelayedTask(ChunkClaim.plugin, new VisualizationApplicationTask(player, playerData, visualization), 10L);
			
			//clear it after 20 seconds
			ChunkClaim.plugin.getServer().getScheduler().scheduleSyncDelayedTask(ChunkClaim.plugin, new VisualizationClearTask(player, playerData, visualization), 400L);
		}
		
	}
	
	//reverts a visualization by sending another block change list, this time with the real world block values
	public static void Revert(Player player) {
		
		PlayerData playerData = ChunkClaim.plugin.dataStore.getPlayerData(player.getName());
		
		Visualization visualization = playerData.currentVisualization;
		
		if(playerData.currentVisualization != null) {
			
			if(player.isOnline()) {	
				
				for(int i = 0; i < visualization.elements.size(); i++) {
					VisualizationElement element = visualization.elements.get(i);
					
					if(element.location!=null) {
						Block block = element.location.getBlock();
						player.sendBlockChange(element.location, block.getType(), block.getData());
					}
				}
			}

			playerData.currentVisualization = null;
		}
	}
	
	//convenience method to build a visualization from a claim
	//visualizationType determines the style (gold blocks, silver, red, diamond, etc)
	public static Visualization FromChunk(Chunk chunk, int height, VisualizationType visualizationType, Location not) {
		
		Visualization visualization = new Visualization();
		
		visualization.addChunkElements(chunk, height, visualizationType, not);
		
		return visualization;
	
	}
	public static Visualization FromBukkitChunk(org.bukkit.Chunk bukkitChunk, int height, VisualizationType visualizationType, Location not) {
		
		Chunk chunk = new Chunk(bukkitChunk.getX(),bukkitChunk.getZ(),bukkitChunk.getWorld().getName());
		
		Visualization visualization = new Visualization();
		
		visualization.addChunkElements(chunk, height, visualizationType, not);
		
		return visualization;
	
	}

	private void addChunkElements(Chunk chunk, int height, VisualizationType visualizationType, Location not) {
		

		World world = ChunkClaim.plugin.getServer().getWorld(chunk.worldName);
		
		int smallx = chunk.x*16;
		int smallz = chunk.z*16;
		
		int bigx = (chunk.x+1)*16-1;
		int bigz = (chunk.z+1)*16-1;
		
		Material cornerMaterial = Material.SNOW_BLOCK; 
		Material accentMaterial = Material.SNOW_BLOCK;
		Byte cornerByte = (byte)0; 
		Byte accentByte = (byte)0;
		
		if(visualizationType == VisualizationType.Chunk) {
			cornerMaterial = Material.WOOL;
			accentMaterial = Material.WOOL;
			cornerByte = (byte)1; 
			accentByte = (byte)1;
		}
		
		else if(visualizationType == VisualizationType.ErrorChunk) {
			cornerMaterial = Material.NETHERRACK;
			accentMaterial = Material.NETHERRACK;
		}
		else if(visualizationType == VisualizationType.Public) {
			cornerMaterial = Material.WOOL;
			accentMaterial = Material.WOOL;
			cornerByte = (byte)11; 
			accentByte = (byte)11;
		}
		
		//bottom left corner
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx, height, smallz, not), cornerMaterial,cornerByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx + 1, height, smallz, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx + 2, height, smallz, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx, height, smallz + 1, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx, height, smallz + 2, not), accentMaterial, accentByte));
		
		//bottom right corner
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx, height, smallz, not), cornerMaterial,cornerByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx - 1, height, smallz, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx - 2, height, smallz, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx, height, smallz + 1, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx, height, smallz + 2, not), accentMaterial, accentByte));
		
		//top right corner
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx, height, bigz, not), cornerMaterial,cornerByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx - 1, height, bigz, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx - 2, height, bigz, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx, height, bigz - 1, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx, height, bigz - 2, not), accentMaterial, accentByte));
		
		//top left corner
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx, height, bigz, not), cornerMaterial,cornerByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx + 1, height, bigz, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx + 2, height, bigz, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx, height, bigz - 1, not), accentMaterial, accentByte));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx, height, bigz - 2, not), accentMaterial, accentByte));		
		


		
	}

	//finds a block the player can probably see. this is how visualizations "cling" to the ground or ceiling
	private static Location getVisibleLocation(World world, int x, int y, int z, Location not) {
		Block block = world.getBlockAt(x, y, z);
		BlockFace direction = (isTransparent(block)) ? BlockFace.DOWN : BlockFace.UP;
	
		while(	block.getY() >= 1 && block.getY() < world.getMaxHeight() - 1 &&	(!isTransparent(block.getRelative(BlockFace.UP)) || isTransparent(block))) {
			block = block.getRelative(direction);
		}
		Location location = block.getLocation();
		if(not!=null &&location.getX()==not.getX() && location.getY()==not.getY() && location.getZ()==not.getZ()) {
			return null;
		} else {
			return location;
		}
	}

	//helper method for above.  allows visualization blocks to sit underneath partly transparent blocks like grass and fence
	private static boolean isTransparent(Block block)
	{
		return (
			block.getType() == Material.AIR ||
			block.getType() == Material.LONG_GRASS ||
			block.getType() == Material.FENCE ||
			block.getType() == Material.LEAVES ||
			block.getType() == Material.RED_ROSE ||
			block.getType() == Material.CHEST ||
			block.getType() == Material.YELLOW_FLOWER 
		);
	}
}

