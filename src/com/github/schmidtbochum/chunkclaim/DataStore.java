package com.github.schmidtbochum.chunkclaim;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.bukkit.Location;

public abstract class DataStore {
	
	int minModifiedBlocks = 10;
	
	ArrayList<Chunk> chunks = new ArrayList<Chunk> ();
	ArrayList<Chunk> unusedChunks = new ArrayList<Chunk> ();

	HashMap<String,ChunkWorld> worlds = new HashMap<String,ChunkWorld> ();
	
	protected HashMap<String, PlayerData> playerNameToPlayerDataMap = new HashMap<String, PlayerData>();
	
	protected final static String dataLayerFolderPath = "plugins" + File.separator + "ChunkClaim_Alpha";
	
	void initialize(List<String> worldNameList) throws Exception {
		
		ChunkClaim.addLogEntry(this.chunks.size() + " total claimed chunks loaded.");
		
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
		ownerData.chunks.remove(chunk);
		ownerData.credits--;
		this.savePlayerData(chunk.ownerName, ownerData);
		
		//modify new owner data
		newOwnerData.chunks.add(chunk);
		newOwnerData.credits++;
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
			
			for(int i = 0; i < this.chunks.size(); i++)	{
				Chunk chunk = this.chunks.get(i);
				if(chunk.ownerName.equals(playerName)) {
					playerData.chunks.add(chunk);
				}
			}
			
			this.playerNameToPlayerDataMap.put(playerName, playerData);
		}
		
		return this.playerNameToPlayerDataMap.get(playerName);
	}
	
	abstract PlayerData getPlayerDataFromStorage(String playerName);
	
	synchronized public void deleteChunk(Chunk chunk) {
		for(int i = 0; i < this.chunks.size(); i++) {
			if(this.chunks.get(i).x == chunk.x && this.chunks.get(i).z == chunk.z && this.chunks.get(i).worldName.equals(chunk.worldName)) {
				this.chunks.remove(i);
				chunk.inDataStore = false;
				break;
			}
		}
		this.deleteChunkFromSecondaryStorage(chunk);
		
		PlayerData ownerData = this.getPlayerData(chunk.ownerName);
		for(int i = 0; i < ownerData.chunks.size(); i++) {
			if(ownerData.chunks.get(i).x == chunk.x && ownerData.chunks.get(i).z == chunk.z && ownerData.chunks.get(i).worldName.equals(chunk.worldName)) {
				ownerData.chunks.remove(i);
				break;
			}
		}
		this.savePlayerData(chunk.ownerName, ownerData);
	}
	
	abstract void deleteChunkFromSecondaryStorage(Chunk chunk);
	
	synchronized public Chunk getChunkAt(Location location, Chunk cachedChunk) {
		if(cachedChunk != null && cachedChunk.inDataStore && cachedChunk.contains(location)) return cachedChunk;
		
		if(!worlds.containsKey(location.getWorld().getName())) return null;
		
		int x = location.getChunk().getX();
		int z = location.getChunk().getZ();
		
		return worlds.get(location.getWorld().getName()).getChunk(x,z);
	}
	
	/*
	synchronized public Chunk getChunkAt(int x, int z, String worldName) {
		
		if(!worlds.containsKey(worldName)) return null;

		return worlds.get(worldName).getChunk(x,z);
	}
	*/
	
	public abstract void savePlayerData(String playerName, PlayerData playerData);

	synchronized public void deleteChunksForPlayer(String playerName) {
		ArrayList <Chunk> playerChunks = playerNameToPlayerDataMap.get(playerName).chunks;
		for(int i = 0; i < playerChunks.size(); i++) {
			Chunk chunk = playerChunks.get(i);
			chunk.removeSurfaceFluids(null);
			this.deleteChunk(chunk);
			
		}
	}
	abstract void close();	
}
