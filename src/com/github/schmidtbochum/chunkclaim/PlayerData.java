package com.github.schmidtbochum.chunkclaim;

import java.util.ArrayList;
import java.util.Date;

public class PlayerData {
	// String[] builderNames zu arraylist constructor
	public ArrayList<Chunk> chunks = new ArrayList<Chunk>();
	public int credits = 9;
	public int bonus = 0;
	public String[] builderNames;
	public String playerName;
	public Date lastLogin = new Date();
	public Date firstJoin = new Date();
}
