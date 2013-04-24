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
import java.util.*;

import org.bukkit.Location;

public class Chunk {
	public String ownerName;
	public String worldName;
	public int modifiedBlocks = 0;
	public ArrayList<String> builderNames = new ArrayList<String>();
	public Date modifiedDate;
	public Date claimDate;
	public boolean inDataStore;
	public int x;
	public int z;
	public boolean marked = false;
	public boolean inspected = false;

	Chunk(Location location, String o) {
		this.x = location.getChunk().getX();
		this.z = location.getChunk().getZ();
		this.worldName = location.getWorld().getName();
		this.ownerName = o;
		this.claimDate = new Date();
	}	
	Chunk(Location location, String o,ArrayList<String> b) {
		this.x = location.getChunk().getX();
		this.z = location.getChunk().getZ();
		this.worldName = location.getWorld().getName();
		this.ownerName = o;
		for(int i = 0; i < b.size(); i++) {
			this.builderNames.add(b.get(i));
		}
		this.claimDate = new Date();
	}	
	
	Chunk(int px, int pz, String w, String o, Date cd, String [] b) {
		this.x = px;
		this.z = pz;
		this.worldName = w;
		this.ownerName = o;
		for(int i = 0; i < b.length; i++) {
			if(!b[i].equals(""))
				builderNames.add(b[i]);
		}
		this.claimDate = cd;
	}
	Chunk(int px, int pz, String w, String o, String [] b) {
		this.x = px;
		this.z = pz;
		this.worldName = w;
		this.ownerName = o;
		for(int i = 0; i < b.length; i++) {
			if(!b[i].equals(""))
				builderNames.add(b[i]);
		}
		this.claimDate = new Date();
	}
	Chunk(int px, int pz, String w, String o) {
		this.x = px;
		this.z = pz;
		this.worldName = w;
		this.ownerName = o;
		this.claimDate = new Date();
	}
	Chunk(int px, int pz, String w) {
		this.x = px;
		this.z = pz;
		this.worldName = w;
	}
	public boolean contains(Location location) {
		
		org.bukkit.Chunk bukkitChunk = location.getChunk();
		
		int locationX = bukkitChunk.getX();
		int locationZ = bukkitChunk.getZ();
		String locationWorldName = bukkitChunk.getWorld().getName();
		
		if(locationX == this.x && locationZ == this.z && locationWorldName.equals(this.worldName)) {
			return true;
		} else {
			return false;
		}
	}
	public void modify() {
		
		if(this.modifiedBlocks<0) {
			return;
		} else {
			this.modifiedBlocks++;
			if(this.modifiedBlocks >= ChunkClaim.plugin.config_minModBlocks) {
				this.modifiedBlocks=-1;
			}
			ChunkClaim.plugin.dataStore.writeChunkToStorage(this);
		}
	}
	public void mark() {
			this.modifiedBlocks=0;
			ChunkClaim.plugin.dataStore.writeChunkToStorage(this);
	}
	public void unmark() {
		this.modifiedBlocks=-1;
		ChunkClaim.plugin.dataStore.writeChunkToStorage(this);
}
	
	public boolean isTrusted(String playerName) {
		if(this.builderNames.contains(playerName) || this.ownerName.equals(playerName) || ChunkClaim.plugin.dataStore.getPlayerData(playerName).ignorechunks) {
			return true;
		} else {
			return false;
		}
	}
}
