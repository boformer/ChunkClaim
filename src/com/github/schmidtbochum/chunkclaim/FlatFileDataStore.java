package com.github.schmidtbochum.chunkclaim;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.bukkit.Bukkit;

public class FlatFileDataStore extends DataStore {

	private final static String playerDataFolderPath = dataLayerFolderPath + File.separator + "PlayerData";
	private final static String worldDataFolderPath = dataLayerFolderPath + File.separator + "ChunkData";

	
	static boolean hasData() {
		File playerDataFolder = new File(playerDataFolderPath);
		File worldDataFolder = new File(worldDataFolderPath);

		return playerDataFolder.exists() || worldDataFolder.exists();
	}
	
	FlatFileDataStore(List<String> worldNameList) throws Exception {
		this.initialize(worldNameList);
	}
	
	@Override
	void initialize(List<String> worldNameList) throws Exception {
		
		//ensure data folders exist
		new File(playerDataFolderPath).mkdirs();
		new File(worldDataFolderPath).mkdirs();
		
		//load chunk data into memory
		//get world folders
	
		for(int i = 0; i < worldNameList.size(); i++) {
			
			String worldName = worldNameList.get(i);
			
			if(Bukkit.getServer().getWorld(worldName) == null) continue;  //skips unloaded worlds

			this.loadWorldData(worldName);
		}
		super.initialize(worldNameList);
	}
	@Override
	synchronized void loadWorldData(String worldName) throws Exception {

		//create a new world object and register it
		ChunkWorld world = new ChunkWorld(worldName);
		this.worlds.put(worldName,world);
		
		
		//load chunks data into memory
		//get a list of all the chunks in the world folder
		String chunkDataFolderPath = worldDataFolderPath + File.separator + worldName;
		
		File chunkDataFolder = new File(chunkDataFolderPath);
		
		//ensure data folder exist
		chunkDataFolder.mkdirs();
		
		File [] files = chunkDataFolder.listFiles();
		
		for(int j = 0; j < files.length; j++) {
			
			//avoids folders
			if(files[j].isFile()) { 
				
				String fileName = files[j].getName();

				
				//get the chunk coordinates from the file name
				String[] chunkIdSep = fileName.split(";");
				
				//skip the file if no separator was found
				if(fileName.equals(chunkIdSep[0])) continue;
				
				int x;
				int z;
				
				try {	
			        x = Integer.parseInt(chunkIdSep[0]);
			        z = Integer.parseInt(chunkIdSep[1]);
				} catch (Exception e) {
					continue; //skip the file
				}
				
				BufferedReader inStream = null;
				
				try {
				
					inStream = new BufferedReader(new FileReader(files[j].getAbsolutePath()));
				
					//1. Line: Owner Name
					String ownerName = inStream.readLine();
					
					
					//2. Line: Chunk Creation timestamp
					String claimDateString = inStream.readLine();
					
					//3. Line: Number of modified blocks
					int modifiedBlocks = Integer.parseInt(inStream.readLine());
						
					//4. Line: List of Builders
					String [] builderNames = inStream.readLine().split(";");



					inStream.close();
				
					
					DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
					Date claimDate = dateFormat.parse(claimDateString);
					
					Chunk chunk = new Chunk(x,z,worldName,ownerName, claimDate, builderNames);
					
					chunk.modifiedDate = new Date(files[j].lastModified());
					
					chunk.modifiedBlocks = modifiedBlocks;
					
					this.chunks.add(chunk);
					if(modifiedBlocks < this.minModifiedBlocks) {
						this.unusedChunks.add(chunk);
					}
					this.worlds.get(chunk.worldName).addChunk(chunk);
					
					chunk.inDataStore = true;

				
				} catch(Exception e) {
					ChunkClaim.addLogEntry("Unable to load data for chunk \"" + files[j].getName() + "\": " + e.getMessage());
					
				}
				
				//close the file
				try	{
					if(inStream != null) inStream.close();
				} catch(IOException exception) {}
			}
		}
	}
	@Override
	synchronized void writeChunkToStorage(Chunk chunk) {
		
		String fileName = chunk.x + ";" + chunk.z;
		
		String worldName = chunk.worldName;
		String chunkDataFolderPath = worldDataFolderPath + File.separator + worldName;
		
		//ensure that the world folder exists
		new File(chunkDataFolderPath).mkdirs();
		
		BufferedWriter outStream = null;
		
		try {
			//open the chunks's file	
			File chunkFile = new File(chunkDataFolderPath + File.separator + fileName);
			chunkFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(chunkFile));
			
			//write chunk to the file
			this.writeChunkData(chunk, outStream);
			
			//update date
			chunk.modifiedDate = new Date(chunkFile.lastModified());
			
		} catch (Exception e) {
			
			ChunkClaim.addLogEntry("Unexpected exception saving data for chunk \"" + worldName + "\\" + fileName + "\": " + e.getMessage());
		}
		
		//close the file
		try	{
			if(outStream != null) outStream.close();
		} catch(IOException exception) {}
	}
	
	synchronized private void writeChunkData(Chunk chunk, BufferedWriter outStream) throws IOException {
		//1. Line: Owner Name
		outStream.write(chunk.ownerName);
		outStream.newLine();
		
		//2. Line: Chunk Creation timestamp
		DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
		outStream.write(dateFormat.format(chunk.claimDate));
		outStream.newLine();
	
		//3. Line: Number of modified blocks
		outStream.write(String.valueOf(chunk.modifiedBlocks));
			
		//4. Line: List of Builders
		for(int i = 0; i < chunk.builderNames.size(); i++) {

			outStream.write(chunk.builderNames.get(i) + ";");
		}
		outStream.newLine();
		
		//filled line to prevent null
		outStream.write("==========");
		outStream.newLine();
	}

	@Override
	void deleteChunkFromSecondaryStorage(Chunk chunk) {
		String fileName = chunk.x + ";" + chunk.z;
		
		String worldName = chunk.worldName;
		String chunkDataFolderPath = worldDataFolderPath + File.separator + worldName;
		
		//remove from disk
		File chunkFile = new File(chunkDataFolderPath + File.separator + fileName);
		
		if(chunkFile.exists() && !chunkFile.delete()) {
			ChunkClaim.addLogEntry("Error: Unable to delete chunk file \"" + worldName + "\\" + fileName + "\".");
		}	
		
	}
	
	@Override
	synchronized PlayerData getPlayerDataFromStorage(String playerName) {
		File playerFile = new File(playerDataFolderPath + File.separator + playerName);
		
		PlayerData playerData = new PlayerData();
		playerData.playerName = playerName;
		
		//if it doesn't exist as a file
		if(!playerFile.exists()) {
			
			//create a file with defaults
			this.savePlayerData(playerName, playerData);
		}
		
		//otherwise, read the file
		else {
			BufferedReader inStream = null;
			try {	
				
				inStream = new BufferedReader(new FileReader(playerFile.getAbsolutePath()));
				
				//first line is last login timestamp
				String lastLoginTimestampString = inStream.readLine();

				//convert that to a date and store it
				DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");	
				try {
					playerData.lastLogin = dateFormat.parse(lastLoginTimestampString);
				} catch(ParseException parseException) {
					ChunkClaim.addLogEntry("Unable to load last login for \"" + playerFile.getName() + "\".");
					playerData.lastLogin = null;
				}
				
				//second line is credits
				String creditsString = inStream.readLine();
				playerData.credits = Integer.parseInt(creditsString);
				
				//third line is any bonus credits granted by administrators
				String bonusString = inStream.readLine();	
				playerData.bonus = Integer.parseInt(bonusString);
				
				//fourth line are trusted builders (trusted on all chunks);
				String line = inStream.readLine();	
				playerData.builderNames = line.split(";");

				inStream.close();
				
			} catch(Exception e) {
				
				ChunkClaim.addLogEntry("Unable to load data for player \"" + playerName + "\": " + e.getMessage());	
			}
			
			//close the file
			try	{
				if(inStream != null) inStream.close();
			} catch(IOException exception) {}
		}
		
		return playerData;
	}
	
	@Override
	synchronized public void savePlayerData(String playerName, PlayerData playerData) {

		BufferedWriter outStream = null;
		try	{
			//open the player's file
			File playerDataFile = new File(playerDataFolderPath + File.separator + playerName);
			playerDataFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(playerDataFile));
			
			//first line is last login timestamp
			if(playerData.lastLogin == null) playerData.lastLogin = new Date();
			DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
			outStream.write(dateFormat.format(playerData.lastLogin));
			outStream.newLine();
			
			//second line is credits
			outStream.write(String.valueOf(playerData.credits));
			outStream.newLine();	
			
			//third line is bonus
			outStream.write(String.valueOf(playerData.bonus));
			outStream.newLine();
			
			//fourth line are trusted builders (trusted on all chunks);
			for(int i = 0; i < playerData.builderNames.length; i++) {

				outStream.write(playerData.builderNames[i] + ";");
			}
			outStream.newLine();

		} catch(Exception e) {
			
			ChunkClaim.addLogEntry("Unexpected exception saving data for player \"" + playerName + "\": " + e.getMessage());	
		}
		
		//close the file
		try	{
			if(outStream != null) outStream.close();
		} catch(IOException exception) {}
	}

	@Override
	synchronized void close() {}
}
