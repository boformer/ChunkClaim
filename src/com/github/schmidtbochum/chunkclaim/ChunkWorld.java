package com.github.schmidtbochum.chunkclaim;

import com.google.common.collect.HashBasedTable;

public class ChunkWorld {
	public String worldName;
	public HashBasedTable<Integer,Integer,Chunk> chunkTable = HashBasedTable.create();

	
	public Chunk getChunk(int x, int z) {
		return chunkTable.get(x, z);
	}
	
	
	public void addChunk(Chunk newChunk) {
		chunkTable.put(newChunk.x, newChunk.z,newChunk);
	}
	public void removeChunk(Chunk chunk) {
		chunkTable.remove(chunk.x, chunk.z);
		
	}
	ChunkWorld(String name) {
		this.worldName = name;
	}
}
