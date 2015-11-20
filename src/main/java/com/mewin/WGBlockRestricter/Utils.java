/*
 * Copyright (C) 2014 mewin <mewin001@hotmail.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mewin.WGBlockRestricter;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.*;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * @author mewin <mewin001@hotmail.de>
 */
@SuppressWarnings("unchecked")
public final class Utils{

	public static final EnumMap<Material, Material> baseMaterials = new EnumMap<Material, Material>(Material.class); // used for material groups (like pistons)
	public static final HashMap<String, Material> aliases = new HashMap<String, Material>(); // used for remapping names and allowing specific non-block-materials

	public static boolean blockAllowedInRegion(ProtectedRegion region, Material blockType){
		if(region == null){ return false; }
		HashSet<Material> allowedBlocks = (HashSet<Material>) region.getFlag(WGBlockRestricterPlugin.ALLOWED_BLOCK_FLAG);
		HashSet<Material> blockedBlocks = (HashSet<Material>) region.getFlag(WGBlockRestricterPlugin.BLOCKED_BLOCK_FLAG);
		boolean good = false;
		if(blockedBlocks != null && (blockedBlocks.contains(blockType) || blockedBlocks.contains(Material.AIR))){
			good = false;
		}
		if(allowedBlocks != null && (allowedBlocks.contains(blockType) || allowedBlocks.contains(Material.AIR))){
			good = true;
		}
		return good;
	}

	public static boolean placeAllowedAtLocation(WorldGuardPlugin wgp, Material mat, Location loc){
		Material blockType = mat;
		if(baseMaterials.containsKey(blockType)){
			blockType = baseMaterials.get(blockType);
		}
		RegionManager rm = wgp.getRegionManager(loc.getWorld());
		if(rm == null){ return true; }
		ApplicableRegionSet regions = rm.getApplicableRegions(loc);
		Iterator<ProtectedRegion> itr = regions.iterator();
		Map<ProtectedRegion, Boolean> regionsToCheck = new HashMap<ProtectedRegion, Boolean>();
		Set<ProtectedRegion> ignoredRegions = new HashSet<ProtectedRegion>();
		while(itr.hasNext()){
			ProtectedRegion region = itr.next();
			if(ignoredRegions.contains(region)){
				continue;
			}
			boolean allowed = placeAllowedInRegion(region, blockType);
			if(allowed){
				ProtectedRegion parent = region.getParent();
				while(parent != null){
					ignoredRegions.add(parent);
					parent = parent.getParent();
				}
				regionsToCheck.put(region, allowed);
			}
		}
		if(regionsToCheck.size() >= 1){
			Iterator<Entry<ProtectedRegion, Boolean>> itr2 = regionsToCheck.entrySet().iterator();
			while(itr2.hasNext()){
				Entry<ProtectedRegion, Boolean> entry = itr2.next();
				ProtectedRegion region = entry.getKey();
				if(ignoredRegions.contains(region)){
					continue;
				}
				return entry.getValue();// Always true
			}
			return false;
		}else{
			return placeAllowedInRegion(rm.getRegion("__global__"), blockType);
		}
	}

	public static boolean breakAllowedAtLocation(WorldGuardPlugin wgp, Material mat, Location loc){
		Material blockType = mat;
		if(baseMaterials.containsKey(blockType)){
			blockType = baseMaterials.get(blockType);
		}
		RegionManager rm = wgp.getRegionManager(loc.getWorld());
		if(rm == null){ return true; }
		ApplicableRegionSet regions = rm.getApplicableRegions(loc);
		Iterator<ProtectedRegion> itr = regions.iterator();
		Map<ProtectedRegion, Boolean> regionsToCheck = new HashMap<ProtectedRegion, Boolean>();
		Set<ProtectedRegion> ignoredRegions = new HashSet<ProtectedRegion>();
		while(itr.hasNext()){
			ProtectedRegion region = itr.next();
			if(ignoredRegions.contains(region)){
				continue;
			}
			Object allowed = breakAllowedInRegion(region, blockType);
			if(allowed != null){
				ProtectedRegion parent = region.getParent();
				while(parent != null){
					ignoredRegions.add(parent);
					parent = parent.getParent();
				}
				regionsToCheck.put(region, (Boolean) allowed);
			}
		}
		if(regionsToCheck.size() >= 1){
			Iterator<Entry<ProtectedRegion, Boolean>> itr2 = regionsToCheck.entrySet().iterator();
			while(itr2.hasNext()){
				Entry<ProtectedRegion, Boolean> entry = itr2.next();
				ProtectedRegion region = entry.getKey();
				boolean value = entry.getValue();
				if(ignoredRegions.contains(region)){
					continue;
				}
				if(value){ // allow > deny
					return true;
				}
			}
			return false;
		}else{
			return breakAllowedInRegion(rm.getRegion("__global__"), blockType);
		}
	}

	public static boolean breakAllowedInRegion(ProtectedRegion region, Material blockType){
		if(region == null){ return false; }
		HashSet<Material> allowedBlocks = (HashSet<Material>) region.getFlag(WGBlockRestricterPlugin.ALLOWED_BREAK_FLAG);
		HashSet<Material> blockedBlocks = (HashSet<Material>) region.getFlag(WGBlockRestricterPlugin.BLOCKED_BREAK_FLAG);
		boolean good = blockAllowedInRegion(region, blockType);
		if(blockedBlocks != null && (blockedBlocks.contains(blockType) || blockedBlocks.contains(Material.AIR))){
			good = false;
		}
		if(allowedBlocks != null && (allowedBlocks.contains(blockType) || allowedBlocks.contains(Material.AIR))){
			good = true;
		}
		return good;
	}

	public static boolean placeAllowedInRegion(ProtectedRegion region, Material blockType){
		if(region == null){ return false; }
		HashSet<Material> allowedBlocks = (HashSet<Material>) region.getFlag(WGBlockRestricterPlugin.ALLOWED_PLACE_FLAG);
		HashSet<Material> blockedBlocks = (HashSet<Material>) region.getFlag(WGBlockRestricterPlugin.BLOCKED_PLACE_FLAG);
		boolean good = blockAllowedInRegion(region, blockType);
		if(blockedBlocks != null && (blockedBlocks.contains(blockType) || blockedBlocks.contains(Material.AIR))){
			good = false;
		}
		if(allowedBlocks != null && (allowedBlocks.contains(blockType) || allowedBlocks.contains(Material.AIR))){
			good = true;
		}
		return good;
	}

	public static void init(){
		baseMaterials.put(Material.DIODE_BLOCK_OFF, Material.DIODE);
		baseMaterials.put(Material.DIODE_BLOCK_ON, Material.DIODE);
		baseMaterials.put(Material.STATIONARY_LAVA, Material.LAVA);
		baseMaterials.put(Material.LAVA_BUCKET, Material.LAVA);
		baseMaterials.put(Material.LEAVES_2, Material.LEAVES);
		baseMaterials.put(Material.LOG_2, Material.LOG);
		baseMaterials.put(Material.PISTON_EXTENSION, Material.PISTON_BASE);
		baseMaterials.put(Material.PISTON_MOVING_PIECE, Material.PISTON_BASE);
		baseMaterials.put(Material.PISTON_STICKY_BASE, Material.PISTON_BASE);
		baseMaterials.put(Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_COMPARATOR);
		baseMaterials.put(Material.REDSTONE_COMPARATOR_ON, Material.REDSTONE_COMPARATOR);
		baseMaterials.put(Material.REDSTONE_LAMP_OFF, Material.REDSTONE_LAMP_ON);
		baseMaterials.put(Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON);
		baseMaterials.put(Material.WALL_SIGN, Material.SIGN);
		baseMaterials.put(Material.SIGN_POST, Material.SIGN);
		baseMaterials.put(Material.SUGAR_CANE_BLOCK, Material.SUGAR_CANE);
		baseMaterials.put(Material.STATIONARY_LAVA, Material.WATER);
		baseMaterials.put(Material.STRING, Material.TRIPWIRE);
		baseMaterials.put(Material.WATER_BUCKET, Material.WATER);
		aliases.put("piston", Material.PISTON_BASE);
		aliases.put("redstone_lamp", Material.REDSTONE_LAMP_ON);
		aliases.put("stone_brick", Material.SMOOTH_BRICK);
		aliases.put("painting", Material.PAINTING);
		aliases.put("item_frame", Material.ITEM_FRAME);
		aliases.put("any", Material.AIR);
		aliases.put("all", Material.AIR);
		aliases.put("sign", Material.SIGN);
		aliases.put("diode", Material.DIODE);
		aliases.put("seed", Material.CROPS);
		File itemCsv = new File("item.csv");
		if(itemCsv.exists() && itemCsv.isFile()){
			Bukkit.getLogger().log(Level.INFO, "item.csv found. Attempting to load mod materials.");
			FileInputStream in = null;
			try{
				in = new FileInputStream(itemCsv);
				String line;
				while((line = readLine(in)) != null){
					String[] split = line.split(",");
					if(split.length < 5){
						continue;
					}
					try{
						int id = Integer.parseInt(split[0]);
						String type = split[1];
						String mod = split[2];
						String name = split[3];
						if(!type.equalsIgnoreCase("block") || mod.equalsIgnoreCase("Minecraft") || mod.equalsIgnoreCase("null")){
							continue;
						}
						aliases.put(mod + "." + name, Material.getMaterial(id));
						String shortName = name.substring(name.indexOf(".") + 1);
						if(!aliases.containsKey(shortName)){
							aliases.put(shortName, Material.getMaterial(id));
						}
						Bukkit.getLogger().log(Level.INFO, "Added material {0} of mod {1}.", new Object[]{name, mod});
					}catch(NumberFormatException ex){
						continue;
					}
				}
			}catch(IOException ex){
				Bukkit.getLogger().log(Level.WARNING, "Failed to load item.csv", ex);
			}finally{
				try{
					if(in != null){
						in.close();
					}
				}catch(IOException ex){}
			}
		}else{
			Bukkit.getLogger().log(Level.INFO, "No item.csv found.");
		}
	}

	private static String readLine(InputStream in) throws IOException{
		Queue<Byte> bQ = new LinkedList<Byte>();
		int i;
		while((i = in.read()) != -1){
			switch (i){
				case '\n':
					return bQToString(bQ);
				case '\r':
					continue;
				default:
					bQ.add((byte) i);
			}
		}
		if(bQ.size() < 1){
			return null;
		}else{
			return bQToString(bQ);
		}
	}

	private static String bQToString(Queue<Byte> bQ){
		byte[] bytes = new byte[bQ.size()];
		for(int i = 0; i < bytes.length; i++){
			bytes[i] = bQ.poll();
		}
		return new String(bytes);
	}
}
