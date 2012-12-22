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

import org.bukkit.Location;
import org.bukkit.entity.Player;

//FEATURE: give players credits for playing, as long as they're not away from their computer

//runs every 5 minutes in the main thread, grants blocks per hour / 12 to each online player who appears to be actively playing
public class DeliverCreditsTask implements Runnable {

	@Override
	public void run()
	{
		
		DataStore dataStore = ChunkClaim.plugin.dataStore;
		
		dataStore.cleanUp(100);
		
		
		Player [] players = ChunkClaim.plugin.getServer().getOnlinePlayers();
		
		//ensure players get at least 1 block (if accrual is totally disabled, this task won't even be scheduled)
		float accruedCredits = ChunkClaim.plugin.config_creditsPerHour / 12L;
		
		//for each online player
		for(int i = 0; i < players.length; i++)
		{
			Player player = players[i];
			
			PlayerData playerData = dataStore.getPlayerData(player.getName());
			
			Location lastLocation = playerData.lastAfkCheckLocation;
			try  //distance squared will throw an exception if the player has changed worlds
			{
				//if he's not in a vehicle and has moved at least five blocks since the last check
				if(!player.isInsideVehicle() && (lastLocation == null || lastLocation.distanceSquared(player.getLocation()) >= 25))
				{					
					//if player is over accrued limit, accrued limit was probably reduced in config file AFTER he accrued
					//in that case, leave his credits where they are
					if(playerData.credits >  ChunkClaim.plugin.config_maxCredits) continue;
					
					//add blocks
					playerData.credits += accruedCredits;
					
					
					//respect limits
					if(playerData.credits > ChunkClaim.plugin.config_maxCredits)
					{
						playerData.credits = ChunkClaim.plugin.config_maxCredits; 
					}
					
					//intentionally NOT saving data here to reduce overall secondary storage access frequency
					//many other operations will cause this players data to save, including his eventual logout
					//dataStore.savePlayerData(player.getName(), playerData);
				}
			}
			catch(Exception e) { }
			
			//remember current location for next time
			playerData.lastAfkCheckLocation = player.getLocation();
		}
	}
}
