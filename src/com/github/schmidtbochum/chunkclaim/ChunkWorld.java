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
