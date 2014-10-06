package com.gmail.buzziespy.MaskOfFutures;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class MaskOfFutures extends JavaPlugin{
	
	@Override
	public void onEnable()
	{
		//enable the listener
		new BeingListener(this);
	}
	
	@Override
	public void onDisable()
	{
		
	}
	
	//some commands
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
	
		//Currently being tested - needs to be set up with proper perms later
		if (cmd.getName().equals("zhorse"))
		{
			if (sender instanceof Player)
			{
				//Switch item in player's hand with item in helmet slot
				Player p = (Player)sender;
				
				/*
				 * Aim for average horse stats:
				 * (unknown at the moment)
				 */
				Horse h = (Horse)p.getWorld().spawnEntity(p.getLocation(), EntityType.HORSE);
				h.setVariant(Horse.Variant.UNDEAD_HORSE);
				h.setTamed(true);
				h.setOwner(p);
				//h.setMaxHealth(22.5);
				//h.setJumpStrength(0.7);
				//how do I set top speed of a horse?
				//h.setHealth(22.5);
				//optional message here?
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
				//Switch item in player's hand with item in helmet slot
				Player p = (Player)sender;
				
				/*
				 * Aim for average horse stats:
				 * (unknown at the moment)
				 */
				Horse h = (Horse)p.getWorld().spawnEntity(p.getLocation(), EntityType.HORSE);
				h.setVariant(Horse.Variant.SKELETON_HORSE);
				h.setTamed(true);
				h.setOwner(p);
				//h.setMaxHealth(22.5);
				//h.setJumpStrength(0.7);
				//how do I set top speed of a horse?
				//h.setHealth(22.5);
				//optional message here?
			}
			else if (sender instanceof ConsoleCommandSender) //doesn't take command blocks into account
			{
				//must take one argument, the player name
				sender.sendMessage("You must be in game to run this command.");
			}
			return true;
		}
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