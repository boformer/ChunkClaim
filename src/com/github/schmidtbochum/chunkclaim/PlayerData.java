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
import java.util.Date;

import org.bukkit.Location;

public class PlayerData {
	// String[] builderNames zu arraylist constructor
	public float credits = ChunkClaim.plugin.config_startCredits;
	public float bonus = 0L;
	public int chunksOwning=0;
	public String playerName;
	public ArrayList<String> builderNames = new ArrayList<String>();
	public Date lastLogin = new Date();
	public Date firstJoin = new Date();
	public Chunk lastChunk = null;
	public Visualization currentVisualization = null;
	public Location lastAfkCheckLocation = null;
	public boolean ignorechunks = false;
	public int getCredits() {
		return (int) credits;
		
	}
}
