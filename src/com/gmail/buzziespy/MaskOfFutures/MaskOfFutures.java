package com.gmail.buzziespy.MaskOfFutures;

import java.util.ArrayList;
import java.util.List;

import nu.nerd.modmode.ModMode;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.SpawnEgg;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/*
 * Comments for MoF have been added to aid in maintainability.
 * NOTE: Most of the interesting code is over in BeingListener, which handles all the listening
 * for this plugin.
 */

public final class MaskOfFutures extends JavaPlugin{
	
	//added to check for players in modmode for brick dropping - to preclude the possibility of
	//bricks appearing to drop out of the sky, modmode players do not drop bricks, but are logged
	//so brick reinbursing can occur at a later time
	ModMode mmode;
	
	@Override
	public void onEnable()
	{
		if (getServer().getPluginManager().getPlugin("ModMode") != null && getServer().getPluginManager().getPlugin("ModMode").isEnabled()) 
		{
			mmode = (ModMode)getServer().getPluginManager().getPlugin("ModMode");
		}
		//enable the listener
		new BeingListener(this);
	}
	
	@Override
	public void onDisable()
	{
		this.saveConfig();
	}
	
	//some commands
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
	
		//zhorse and shorse both spawn in a zombie/skeleton horse tamed to someone specified
		//NOTE: Currently both have a chance of spawning in foals - this is most likely undesirable for
		//events.  Perhaps need to add something to ensure that the horse is never a foal.
		if (cmd.getName().equals("zhorse"))
		{
			if (sender instanceof Player)
			{
				if (args.length < 2)
				{
					Player p = (Player)sender;
					
					/*
					 * Aim for average horse stats:
					 * (unknown at the moment)
					 * Later testing showed that Bukkit's horse spawning automatically randomly chooses stats
					 */
					Horse h = (Horse)p.getWorld().spawnEntity(p.getLocation(), EntityType.HORSE);
					h.setVariant(Horse.Variant.UNDEAD_HORSE);
					h.setTamed(true);
					if (args.length == 0)
					{
						h.setOwner(p);
					}
					else if (args.length == 1)
					{
						h.setOwner(getServer().getOfflinePlayer(args[0]));
					}
					if (!h.isAdult())
					{
						h.setAdult();
					}
					//h.setMaxHealth(22.5);
					//h.setJumpStrength(0.7);
					//how do I set top speed of a horse?
					//h.setHealth(22.5);
					sender.sendMessage(ChatColor.GREEN + "Spawned in a zombie horse tamed to " + h.getOwner().getName().toString());
				}
				
			}
			else if (sender instanceof ConsoleCommandSender) //doesn't take command blocks into account
			{
				//must take one argument, the player name
				sender.sendMessage("You must be in game to run this command.");
			}
			return true;
		}
		else if (cmd.getName().equals("shorse"))
		{
			if (sender instanceof Player)
			{
				Player p = (Player)sender;
				
				/*
				 * Aim for average horse stats:
				 * (unknown at the moment)
				 */
				Horse h = (Horse)p.getWorld().spawnEntity(p.getLocation(), EntityType.HORSE);
				h.setVariant(Horse.Variant.SKELETON_HORSE);
				h.setTamed(true);
				if (args.length == 0)
				{
					h.setOwner(p);
				}
				else if (args.length == 1)
				{
					h.setOwner(getServer().getOfflinePlayer(args[0]));
				}
				if (!h.isAdult())
				{
					h.setAdult();
				}
				//h.setMaxHealth(22.5);
				//h.setJumpStrength(0.7);
				//how do I set top speed of a horse?
				//h.setHealth(22.5);
				//optional message here?
				sender.sendMessage(ChatColor.GREEN + "Spawned in a skeleton horse tamed to " + h.getOwner().getName().toString());
			}
			else if (sender instanceof ConsoleCommandSender) //doesn't take command blocks into account
			{
				//must take one argument, the player name
				sender.sendMessage("You must be in game to run this command.");
			}
			return true;
		}
		//Command to allow admins to refund bricks - this creates the brick and drops it at the
		//command runner's feet.  It is assumed the command runner has a way to send the brick to
		//whoever it is for.
		else if (cmd.getName().equals("wbrick"))
		{
			//takes in a player name as argument, generates a brick with that player name
			if (args.length == 1 && sender instanceof Player)
			{
				Player p = (Player)sender;
				ItemStack woolbrick = new ItemStack(Material.CLAY_BRICK, 1);
				ItemMeta woolbrickInfo = woolbrick.getItemMeta();
				List<String> bricklore = new ArrayList<String>(); //not sure how to optimize this
				bricklore.add(ChatColor.GOLD + "" + ChatColor.ITALIC + args[0] + " dropped this on hearing a Wither");
				woolbrickInfo.setLore(bricklore);
				woolbrick.setItemMeta(woolbrickInfo);
				p.getWorld().dropItemNaturally(p.getLocation(), woolbrick);
				//Next update: Put brick in player's inventory automatically; don't drop
				return true;
			}
		}
		//Commands related to main feature config display and modification (brick dropping, death messages)
		else if (cmd.getName().equals("mof"))
		{
			if (args.length == 2)
			{
				if (args[0].equalsIgnoreCase("death-msgs"))
				{
					try
					{
						if (args[1].equalsIgnoreCase("true"))
						{
							//forcibly set config option for death messages to true, save config and implement new option
							getConfig().set("death-msgs", true);
							saveConfig();
							sender.sendMessage(ChatColor.GREEN + "Death message set to true.  Death messages will appear in game.");
						}
						else if (args[1].equalsIgnoreCase("false"))
						{
							//forcibly set option to false
							getConfig().set("death-msgs", false);
							saveConfig();
							sender.sendMessage(ChatColor.RED + "Death message set to false.  Death messages will print to console.");
						}
					}
					catch (RuntimeException e)
					{
						sender.sendMessage(ChatColor.RED + "WARNING: That is not valid syntax!");
					}
				}
				else if (args[0].equalsIgnoreCase("brick-dropping"))
				{
					try
					{
						if (args[1].equalsIgnoreCase("true"))
						{
							//forcibly set config option for death messages to true, save config and implement new option
							getConfig().set("brick-dropping", true);
							saveConfig();
							sender.sendMessage(ChatColor.GREEN + "Brick dropping set to true.  Players online will drop bricks when a Wither spawns.");
						}
						else if (args[1].equalsIgnoreCase("false"))
						{
							//forcibly set option to false
							getConfig().set("brick-dropping", false);
							saveConfig();
							sender.sendMessage(ChatColor.RED + "Brick dropping set to false.  Players will not drop bricks when a Wither spawns.");
						}
					}
					catch (RuntimeException e)
					{
						sender.sendMessage(ChatColor.RED + "WARNING: That is not valid syntax!");
					}
				}
			}
			else if (args.length == 1)
			{
				if (args[0].equalsIgnoreCase("reload"))
				{
					reloadConfig();
					sender.sendMessage(ChatColor.AQUA + "MaskOfFutures config reloaded!");
				}
			}
			else 
			{
				sender.sendMessage(ChatColor.AQUA + "=====Mask of Futures=====");
				sender.sendMessage(ChatColor.AQUA + "Brick dropping: " + getConfig().getBoolean("brick-dropping"));
				sender.sendMessage(ChatColor.AQUA + "Death messages: " + getConfig().getBoolean("death-msgs"));
			}
			return true;
		}
		
		//internal MoF command for taming undead horses - apparently in vanilla, undead horses cannot be tamed
		else if (cmd.getName().equals("tame"))
		{
			if (args.length == 0 && sender instanceof Player)
			{
				Player p = (Player)sender;
				if (!p.hasMetadata("MaskOfFutures.tame"))
				{
					p.setMetadata("MaskOfFutures.tame", new FixedMetadataValue(this, "true"));
					p.sendMessage(ChatColor.GREEN + "Right-click on an undead untamed horse to tame it to you.");
				}
			}
			else if (args.length == 1 && sender instanceof Player)
			{
				Player p = (Player)sender;
				if (!p.hasMetadata("MaskOfFutures.tame") && p.hasPermission("mof.horse"))
				{
					p.setMetadata("MaskOfFutures.tameadmin", new FixedMetadataValue(this, args[0]));
					p.sendMessage(ChatColor.GREEN + "Right-click on an undead untamed horse to tame it to " + args[0] + ".");
				}
			}
			return true;
		}
		
		//special horse spawn eggs - when used, plugin will detect it and spawn the undead horse
		//WIP?  at any rate it doesn't work right now
		/*else if (cmd.getName().equals("zhegg"))
		{
			if (sender instanceof Player)
			{
				//TODO: Pull this lore from config
				ItemStack i = new ItemStack(Material.MONSTER_EGG, 1);
				i.setDurability((short)100);
				ItemMeta ii = i.getItemMeta();
				List<String> egglore = new ArrayList<String>();
				egglore.add(ChatColor.GREEN + "" + ChatColor.ITALIC + "Zombie Horse");
				ii.setLore(egglore);
				i.setItemMeta(ii);
				
				Player p = (Player)sender;
				p.getWorld().dropItem(p.getLocation(), i);
			}
			else if (sender instanceof ConsoleCommandSender) //doesn't take command blocks into account
			{
				//must take one argument, the player name
				sender.sendMessage("You must be in game to run this command.");
			}
			
			return true;
		}*/
		
		/* This is commented out since /wear will be enabled with CH
		else if (cmd.getName().equals("hat"))
		{
			if (sender instanceof Player)
			{
				//Switch item in player's hand with item in helmet slot
				Player p = (Player)sender;
				ItemStack hat = p.getInventory().getItemInHand();
				ItemStack helm = p.getInventory().getHelmet();
				p.getInventory().setItemInHand(helm);
				p.getInventory().setHelmet(hat);
				//optional message here?
			}
			else if (sender instanceof ConsoleCommandSender) //doesn't take command blocks into account
			{
				//must take one argument, the player name
				sender.sendMessage("You must be in game to run this command.");
			}
			return true;
		}
		*/
		return false;
	}
	
	
}