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

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import org.bukkit.Location;

public abstract class DataStore {
	
	public int nextChunkId = 0;
	
	
	int minModifiedBlocks = 10;
	
	ArrayList<Chunk> chunks = new ArrayList<Chunk> ();
	ArrayList<Chunk> unusedChunks = new ArrayList<Chunk> ();

	HashMap<String,ChunkWorld> worlds = new HashMap<String,ChunkWorld> ();
	
	protected HashMap<String, PlayerData> playerNameToPlayerDataMap = new HashMap<String, PlayerData>();
	
	protected final static String dataLayerFolderPath = "plugins" + File.separator + "ChunkClaim";
	
	void initialize() throws Exception {
		ChunkClaim.addLogEntry(this.chunks.size() + " total claimed chunks loaded.");
		
		Random rand = new Random();
		
		nextChunkId = (this.chunks.size()>0?rand.nextInt(this.chunks.size()):0);
		
		Vector<String> playerNames = new Vector<String>();
		
		for(int i = 0; i < this.chunks.size(); i++)	{
			Chunk chunk = this.chunks.get(i);
			
			if(!playerNames.contains(chunk.ownerName)) {
				playerNames.add(chunk.ownerName);
			}
		}
		
		ChunkClaim.addLogEntry(playerNames.size() + " players have claimed chunks in loaded worlds.");		

		System.gc();
	}
	
	abstract void loadWorldData(String worldName) throws Exception;
	
	

	public void cleanUp(int n) {
		
		if(this.chunks.size()<1) return;
		
		Date now = new Date();
		double deletionTime = (1000 * 60 * 60 * 24)*ChunkClaim.plugin.config_autoDeleteDays;

		for(int i = 0; i < n; i++) {
			nextChunkId++;
			
			if(nextChunkId >= this.chunks.size()) nextChunkId = 0;
			
			Chunk chunk = chunks.get(nextChunkId);
			if(chunk.modifiedBlocks >=0) {
				long diff = now.getTime()-chunk.claimDate.getTime();
				double chunkDeletionTime = deletionTime;
				if(diff> chunkDeletionTime) {
					PlayerData playerData = this.getPlayerData(chunk.ownerName);
					playerData.credits++;
					playerData.chunksOwning--;
					this.savePlayerData(chunk.ownerName, playerData);
					this.playerNameToPlayerDataMap.remove(chunk.ownerName);
					ChunkClaim.addLogEntry("Auto-deleted chunk by "+chunk.ownerName+" at " + (chunk.x*16) + " | " + (chunk.z*16));
					this.deleteChunk(chunk);
				};
			}
		}
		
	}
	synchronized void unloadWorldData(String worldName) {
		this.worlds.remove(worldName);
		for(int i = 0; i<this.chunks.size(); i++) {
			while(this.chunks.get(i).worldName.equals(worldName)) {
				this.chunks.remove(i);
			}
		}
	}
	
	
	synchronized void clearCachedPlayerData(String playerName) {
		this.playerNameToPlayerDataMap.remove(playerName);
	}
	
	synchronized public void changeChunkOwner(Chunk chunk, String newOwnerName) throws Exception {
		PlayerData ownerData = this.getPlayerData(chunk.ownerName);
		PlayerData newOwnerData = this.getPlayerData(newOwnerName);
		
		//modify chunk
		chunk.ownerName = newOwnerName;
		this.saveChunk(chunk);
		
		//modify previous owner data
		ownerData.credits++;
		ownerData.chunksOwning--;
		this.savePlayerData(chunk.ownerName, ownerData);
		
		//modify new owner data
		newOwnerData.credits--;
		newOwnerData.chunksOwning++;
		this.savePlayerData(newOwnerName, newOwnerData);
		
	}

	synchronized void addChunk(Chunk newChunk) {
		this.chunks.add(newChunk);
		
		if(this.worlds.containsKey(newChunk.worldName)) {
			this.worlds.get(newChunk.worldName).addChunk(newChunk);
			newChunk.inDataStore = true;
			this.saveChunk(newChunk);
		}
		
		
	}

	private void saveChunk(Chunk chunk) {
		this.writeChunkToStorage(chunk);
		
	}
	
	abstract void writeChunkToStorage(Chunk chunk);
	
	synchronized public PlayerData getPlayerData(String playerName) {
		
		PlayerData playerData = this.playerNameToPlayerDataMap.get(playerName);
		if(playerData == null) {
			playerData = this.getPlayerDataFromStorage(playerName);
			this.playerNameToPlayerDataMap.put(playerName, playerData);
		}
		
		return this.playerNameToPlayerDataMap.get(playerName);
	}
	
	abstract PlayerData getPlayerDataFromStorage(String playerName);
	
	synchronized public void deleteChunk(Chunk chunk) {
	
		for(int i = 0; i < this.chunks.size(); i++) {
			if(this.chunks.get(i).x == chunk.x && this.chunks.get(i).z == chunk.z && this.chunks.get(i).worldName.equals(chunk.worldName)) {
				this.chunks.remove(i);
				
				this.worlds.get(chunk.worldName).removeChunk(chunk);
				chunk.inDataStore = false;
				break;
			}
		}
		this.deleteChunkFromSecondaryStorage(chunk);
		
		ChunkClaim.plugin.regenerateChunk(chunk);
	}
	
	abstract void deleteChunkFromSecondaryStorage(Chunk chunk);
	
	synchronized public Chunk getChunkAt(Location location, Chunk cachedChunk) {
		if(cachedChunk != null && cachedChunk.inDataStore && cachedChunk.contains(location)) return cachedChunk;
		
		if(!worlds.containsKey(location.getWorld().getName())) return null;
		
		int x = location.getChunk().getX();
		int z = location.getChunk().getZ();
		
		return worlds.get(location.getWorld().getName()).getChunk(x,z);
	}
	synchronized public Chunk getChunkAtPos(int x, int z, String worldName) {
		
		if(!worlds.containsKey(worldName)) return null;
		
		return worlds.get(worldName).getChunk(x,z);
	}

	
	
	
	public abstract void savePlayerData(String playerName, PlayerData playerData);
	
	synchronized public ArrayList<Chunk> getAllChunksForPlayer(String playerName) {
		ArrayList <Chunk> playerChunks = new ArrayList <Chunk>();
		for(int i = 0; i < chunks.size(); i++) {
			Chunk chunk = chunks.get(i);
			if(chunk.ownerName.equals(playerName)) {
				playerChunks.add(chunk);
			}
		}
		return playerChunks;
	}
	
	synchronized public int deleteChunksForPlayer(String playerName) {
		ArrayList <Chunk> playerChunks = getAllChunksForPlayer(playerName);
		PlayerData playerData = this.getPlayerData(playerName);
		for(int i = 0; i < playerChunks.size(); i++) {
			Chunk chunk = playerChunks.get(i);
			this.deleteChunk(chunk);
			playerData.chunksOwning--;
			playerData.credits++;
		}
		return playerChunks.size();
	}
	abstract void close();

	/**
	 * @param location
	 * @return
	 */
	public boolean ownsNear(Location location, String playerName) {
		int x = location.getChunk().getX();
		int z = location.getChunk().getZ();
		String worldName = location.getWorld().getName();
		
		Chunk a = getChunkAtPos(x-1, z, worldName);
		Chunk c = getChunkAtPos(x+1, z, worldName);
		Chunk b = getChunkAtPos(x, z-1, worldName);
		Chunk d = getChunkAtPos(x, z+1, worldName);
		
		//ChunkClaim.addLogEntry((a==null) + ";" + (b==null) + ";" + (c==null) + ";" + (d==null));
		
		if(a==null&&b==null&&c==null&&d==null) {return false;}
		else if(a!=null&&a.isTrusted(playerName)) {return true;}
		else if(b!=null&&b.isTrusted(playerName)) {return true;}
		else if(c!=null&&c.isTrusted(playerName)) {return true;}
		else if(d!=null&&d.isTrusted(playerName)) {return true;}
		else {return false;}
	}	
}
