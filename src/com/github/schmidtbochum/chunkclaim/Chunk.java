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
	Chunk(int px, int pz, String w, String o, Date cd, String [] b) {
		this.x = px;
		this.z = pz;
		this.worldName = w;
		this.ownerName = o;
		for(int i = 0; i < b.length; i++) {
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
	public void removeSurfaceFluids(Object object) {
		// TODO Auto-generated method stub
		
	}
}
