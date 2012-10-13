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

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.painting.PaintingBreakByEntityEvent;
import org.bukkit.event.painting.PaintingBreakEvent;
import org.bukkit.event.painting.PaintingPlaceEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;

public class EntityEventHandler implements Listener {
	private DataStore dataStore;

	public EntityEventHandler(DataStore dataStore) {
		this.dataStore = dataStore;
	}
	
	//don't allow endermen to change blocks
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityChangeBLock(EntityChangeBlockEvent event) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(event.getBlock().getWorld().getName())) return;
		
		if(event.getEntityType() == EntityType.ENDERMAN) event.setCancelled(true);
	}
	//don't allow zombies to break down doors
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onZombieBreakDoor(EntityBreakDoorEvent event) {

		if(!ChunkClaim.plugin.config_worlds.contains(event.getBlock().getWorld().getName())) return;
		
		event.setCancelled(true);
	}
	
	//don't allow entities to trample crops
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityInteract(EntityInteractEvent event) {

		if(!ChunkClaim.plugin.config_worlds.contains(event.getBlock().getWorld().getName())) return;
		
		if(event.getBlock().getType() == Material.SOIL)	event.setCancelled(true);
	}
	
	//when an entity explodes...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent explodeEvent) {

		if(!ChunkClaim.plugin.config_worlds.contains(explodeEvent.getEntity().getWorld().getName())) return;		
		
		List<Block> blocks = explodeEvent.blockList();
		
		for(int i = 0; i < blocks.size(); i++) {
			blocks.remove(i--);
		}
	}
	
	//when an item spawns...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onItemSpawn(ItemSpawnEvent event) {

		if(!ChunkClaim.plugin.config_worlds.contains(event.getEntity().getWorld().getName())) return;		
		
		Entity entity = event.getEntity();
		Item item = (Item) entity;
		Material material = item.getItemStack().getType();

		
		//allow dropping books
		if(material == Material.WRITTEN_BOOK || material == Material.BOOK_AND_QUILL){
			return;
		} else {
			event.setCancelled(true);
		}
	}
	
	//when an experience bottle explodes...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onExpBottle(ExpBottleEvent event) {
		
		event.setExperience(0);
	}
	
	//when a creature spawns...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntitySpawn(CreatureSpawnEvent event) {
		if(!ChunkClaim.plugin.config_worlds.contains(event.getEntity().getWorld().getName())) return;		
		
		SpawnReason reason = event.getSpawnReason();
		if(reason != SpawnReason.SPAWNER_EGG) event.setCancelled(true);

	}
	
	//when an entity dies...
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		
		event.setDroppedExp(0);
		event.getDrops().clear();
	}
	
	//when an entity picks up an item
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityPickup(EntityChangeBlockEvent event) {
		if(event.getEntity() instanceof Enderman) {
			event.setCancelled(true);
		}
	}
	
	//when a painting is broken
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPaintingBreak(PaintingBreakEvent event) {

		if(!ChunkClaim.plugin.config_worlds.contains(event.getPainting().getWorld().getName())) return;		
		
		if(!(event instanceof PaintingBreakByEntityEvent))   {
	         event.setCancelled(true);
	         return;
	     }
		
		PaintingBreakByEntityEvent entityEvent = (PaintingBreakByEntityEvent)event;
		
		//who is removing it?
		Entity remover = entityEvent.getRemover();
		
		//again, making sure the breaker is a player
		if(!(remover instanceof Player)) {
			event.setCancelled(true);
			return;
        }
		
		Chunk chunk = this.dataStore.getChunkAt(event.getPainting().getLocation(), null);
		
		if(chunk==null) {
			event.setCancelled(true);
			return;
		}
		if(!chunk.isTrusted(((Player) remover).getName())) {
			event.setCancelled(true);
			((Player) remover).sendMessage("You don't have " + chunk.ownerName + "'s permission to build here.");
			return;
		}
	}
	//when a painting is placed
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPaintingPlace(PaintingPlaceEvent event) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(event.getPlayer().getWorld().getName())) return;		
		
		Player player = event.getPlayer();
		Chunk chunk = this.dataStore.getChunkAt(event.getPainting().getLocation(), null);
		
		if(chunk==null) {
			event.setCancelled(true);
			return;
		}
		if(!chunk.isTrusted(player.getName())) {
			event.setCancelled(true);
			player.sendMessage("You don't have " + chunk.ownerName + "'s permission to build here.");
			return;
		}
	}
	//when an entity is damaged
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityDamage (EntityDamageEvent event) {
		
		if(!ChunkClaim.plugin.config_worlds.contains(event.getEntity().getWorld().getName())) return;		
		//only actually interested in entities damaging entities (ignoring environmental damage)
		if(!(event instanceof EntityDamageByEntityEvent)) return;
		
		//monsters are never protected
		if(event.getEntity() instanceof Monster) return;
		EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

		//determine which player is attacking, if any
		Player attacker = null;
		Arrow arrow = null;
		Entity damageSource = subEvent.getDamager();
		if(damageSource instanceof Player) {
			attacker = (Player)damageSource;
		} else if(damageSource instanceof Arrow) {
			arrow = (Arrow)damageSource;
			if(arrow.getShooter() instanceof Player) {
				attacker = (Player)arrow.getShooter();
			}
		}
		else if(damageSource instanceof ThrownPotion) {
			ThrownPotion potion = (ThrownPotion)damageSource;
			if(potion.getShooter() instanceof Player) {
				attacker = (Player)potion.getShooter();
			}
		}
		//FEATURE: protect claimed animals, boats, minecarts
		if(event instanceof EntityDamageByEntityEvent) {
			//if the entity is an non-monster creature (remember monsters disqualified above), or a vehicle
			if ((subEvent.getEntity() instanceof Creature)) {
				Chunk cachedChunk = null;
				PlayerData playerData = null;
				if(attacker != null) {
					playerData = this.dataStore.getPlayerData(attacker.getName());
					cachedChunk = playerData.lastChunk;
				}
				Chunk chunk = dataStore.getChunkAt(event.getEntity().getLocation(), cachedChunk);
				
				//if it's claimed
				if(chunk != null) {
					if(attacker == null) {
						event.setCancelled(true);
					}
					else {
						if(!chunk.isTrusted(attacker.getName())) {
							event.setCancelled(true);
							attacker.sendMessage("Not permitted.");
						}
						//cache claim for later
						if(playerData != null) {
							playerData.lastChunk = chunk;
						}	
						
					}
				}
				
			}
			
		}
	}
	//when an entity is damaged
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPotionSplash (PotionSplashEvent event) {
		if(!ChunkClaim.plugin.config_worlds.contains(event.getEntity().getWorld().getName())) return;		
		event.setCancelled(true);
	}
	
	//when a vehicle is damaged
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onVehicleDamage (VehicleDamageEvent event) {
		if(!ChunkClaim.plugin.config_worlds.contains(event.getVehicle().getWorld().getName())) return;		
		
		//determine which player is attacking, if any
		Player attacker = null;
		Arrow arrow = null;
		Entity damageSource = event.getAttacker();
		if(damageSource instanceof Player) {
			attacker = (Player)damageSource;
		} else if(damageSource instanceof Arrow) {
			arrow = (Arrow)damageSource;
			if(arrow.getShooter() instanceof Player) {
				attacker = (Player)arrow.getShooter();
			}
		}
		else if(damageSource instanceof ThrownPotion) {
			ThrownPotion potion = (ThrownPotion)damageSource;
			if(potion.getShooter() instanceof Player) {
				attacker = (Player)potion.getShooter();
			}
		}
		Chunk cachedChunk = null;
		PlayerData playerData = null;
		if(attacker != null) {
			playerData = this.dataStore.getPlayerData(attacker.getName());
			cachedChunk = playerData.lastChunk;
		}
		Chunk chunk = dataStore.getChunkAt(event.getVehicle().getLocation(), cachedChunk);
		
		//if it's claimed
		if(chunk != null) {
			if(attacker == null) {
				event.setCancelled(true);
			}
			else {
				if(!chunk.isTrusted(attacker.getName())) {
					event.setCancelled(true);
					attacker.sendMessage("Not permitted.");
				}
				//cache claim for later
				if(playerData != null) {
					playerData.lastChunk = chunk;
				}	
				
			}
		}
		


	}
}