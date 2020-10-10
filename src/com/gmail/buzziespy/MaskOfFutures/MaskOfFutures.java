package com.gmail.buzziespy.MaskOfFutures;

import java.io.File;
import java.io.IOException;

//IT WILL RETURN THROUGH PLAIN SIGHT

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import nu.nerd.modmode.ModMode;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
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
	
	//stores list of custom item messages
	public List<String> itemMsgList;
	
	//custom config stuff - https://www.spigotmc.org/wiki/config-files/#using-custom-configurations
	private File oldMsgPlayersFile;
	private FileConfiguration oldMsgPlayersConfig;
	
	public FileConfiguration getoldMsgPlayersConfig()
	{
		return this.oldMsgPlayersConfig;
	}
	
	public void createoldMsgPlayersConfig()
	{
		oldMsgPlayersFile = new File(getDataFolder(),"oldMsgPlayers.yml");
		if (!oldMsgPlayersFile.exists())
		{
			oldMsgPlayersFile.getParentFile().mkdirs();
			saveResource("oldMsgPlayers.yml",false);
		}
		
		oldMsgPlayersConfig = new YamlConfiguration();
		try
		{
			oldMsgPlayersConfig.load(oldMsgPlayersFile);
		}
		catch (IOException | InvalidConfigurationException e)
		{
			e.printStackTrace();
		}
	}
	//end custom config stuff
	
	@Override
	public void onEnable()
	{
		if (getServer().getPluginManager().getPlugin("ModMode") != null && getServer().getPluginManager().getPlugin("ModMode").isEnabled()) 
		{
			mmode = (ModMode)getServer().getPluginManager().getPlugin("ModMode");
		}
		//load up the custom config file
		createoldMsgPlayersConfig();
		//enable the listener
		new BeingListener(this);
	}
	
	@Override
	public void onDisable()
	{
		this.saveConfig();
		try {
			oldMsgPlayersConfig.save(oldMsgPlayersFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//some commands
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
	
		if (cmd.getName().equalsIgnoreCase("ignore-deaths")||cmd.getName().equalsIgnoreCase("mute-deaths"))
		{
			if (sender instanceof Player)
			{
				Player p = (Player)sender;
				if (!p.hasMetadata("MaskOfFutures.mutedeath"))
				{
					p.setMetadata("MaskOfFutures.mutedeath", new FixedMetadataValue(this, "true"));
					p.sendMessage(ChatColor.RED + "Death messages are now muted until the next restart.");
				}
				else
				{
					p.removeMetadata("MaskOfFutures.mutedeath", this);
					p.sendMessage(ChatColor.GREEN + "Death messages will display as normal.");
				}
			}
			else if (sender instanceof ConsoleCommandSender)
			{
				sender.sendMessage("You must be in game to run this command.");
			}
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("toggle-oldmsg"))
		{
			if (args.length == 1) //this is used for admins
			{
				//unused
			}
			else if (args.length == 0) //no args is what players use to toggle vanilla death messages for themselves
			{
				if (sender instanceof Player)
				{
					Player p = (Player)sender;
					if (getoldMsgPlayersConfig().contains("oldMsg"))
					{
						//oldMsg will only be used for a UUID list
						List<String> oldMsgPlayerList = (List<String>) getoldMsgPlayersConfig().getStringList("oldMsg");
						togglePlayerOnList(oldMsgPlayerList, p);
						getoldMsgPlayersConfig().set("oldMsg", oldMsgPlayerList);
					}
					else
					{
						List<String> oldMsgPlayerList = new LinkedList<String>();
						togglePlayerOnList(oldMsgPlayerList, p);
						getoldMsgPlayersConfig().set("oldMsg", oldMsgPlayerList);
					}
				}
			}
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("oldmsg-count"))
		{
			sender.sendMessage(ChatColor.AQUA + "" + getoldMsgPlayersConfig().getStringList("oldMsg").size() + " players are seeing the vanilla death messages");
			return true;
		}
		
		//zhorse and shorse both spawn in a zombie/skeleton horse tamed to someone specified
		//NOTE: Currently both have a chance of spawning in foals - this is most likely undesirable for
		//events.  Perhaps need to add something to ensure that the horse is never a foal.
		else if (cmd.getName().equals("zhorse"))
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
				ItemStack woolbrick = new ItemStack(Material.BRICK, 1);
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
		else if (cmd.getName().equals("mofmsg"))
		{
			if (args.length == 0 && (sender instanceof Player || sender instanceof ConsoleCommandSender))
			{
				sender.sendMessage(ChatColor.RED + "/mofmsg [view|add|delete|addcat|delcat|renamecat|additem|delitem|viewitem|additemreport|delitemreport|addshare|delshare|viewshare] (arguments)");
				displayDeathMessageList(sender);
				return true;
			}
			else if (args.length == 1 && (sender instanceof Player || sender instanceof ConsoleCommandSender))
			{
				//for commands with no arguments
				if (args[0].equalsIgnoreCase("view"))
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg view [category]");
					displayDeathMessageList(sender);
				}
				else if (args[0].equalsIgnoreCase("add"))
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg add [category] [message]\nUse /mofmsg to see list of permissible categories.");
				}
				else if (args[0].equalsIgnoreCase("delete"))
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg delete [category] [item number]\nUse /mofmsg to see list of permissible categories.");
				}
				else if (args[0].equalsIgnoreCase("viewitem"))
				{
					ArrayList<String> itemlist = (ArrayList<String>)getConfig().getStringList("itemMsg");
					Iterator<String> it = itemlist.iterator();
					int size = itemlist.size();
					String keylist = "itemMsg" + "\n======\n";
					for (int i=0; i<size; i++)
					{
						keylist = keylist + (i+1) + ". " + it.next();
						if (it.hasNext())
						{
							keylist = keylist + "\n";
						}
					}
					sender.sendMessage(ChatColor.AQUA + keylist);
				}
				//TODO: finish coding and testing this (list of shared death message links)
				else if (args[0].equalsIgnoreCase("viewshare"))
				{
					AbstractSet<String> sharemsglist = (AbstractSet<String>) getConfig().getConfigurationSection("listShare").getKeys(false);
					Iterator<String> it = sharemsglist.iterator();
					if (it.hasNext())
					{
						int size = sharemsglist.size();
						String keylist = "listShare" + "\n======\n";
						for (int i=0; i<size; i++)
						{
							String key = it.next();
							keylist = keylist + (i+1) + ". " + key + " -> " + getConfig().getString("listShare."+key);
							if (it.hasNext())
							{
								keylist = keylist + "\n";
							}
						}
						sender.sendMessage(ChatColor.AQUA + keylist);
					}
					
					
					/*
					if (it.hasNext())
					{
						String keylist = it.next(); //assumes there is always at least one key - this is less likely to be true!
						while (it.hasNext())
						{
							String key = it.next();
							keylist = keylist + ", " + it.next();
						}
						sender.sendMessage(ChatColor.AQUA + "Available death message categories: " + keylist);
					}*/
					else
					{
						sender.sendMessage(ChatColor.RED + "No list sharing configuration was found.");
					}
					
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg [view|add|delete|addcat|delcat|renamecat|additem|delitem|viewitem|additemreport|delitemreport|addshare|delshare|viewshare] (arguments)\nUse /mofmsg to see list of permissible categories.");
				}
				return true;
			}
			else if (args.length == 2 && (sender instanceof Player || sender instanceof ConsoleCommandSender))
			{
				if (args[0].equalsIgnoreCase("add")) //for instances of category specified but no message specified
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg add [category] [message]\nUse /mofmsg to see list of permissible categories.");
				}
				else if (args[0].equalsIgnoreCase("delete"))
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg delete [category] [item number]\nUse /mofmsg to see list of permissible categories.");
				}
				else if (args[0].equalsIgnoreCase("view"))
				{
					//DEBUG:
					//getLogger().info("Config is list: " + getConfig().isList("msg." + args[1]));
					//get category
					if (getConfig().contains("msg." + args[1]))
					{
						if (getConfig().isList("msg." + args[1]))
						{
							ArrayList<String> deathmsglist = (ArrayList<String>)getConfig().getStringList("msg."+args[1]);
							Iterator<String> it = deathmsglist.iterator();
							int size = deathmsglist.size();
							String keylist = args[1] + "\n======\n";
							for (int i=0; i<size; i++)
							{
								keylist = keylist + (i+1) + ". " + it.next();
								if (it.hasNext())
								{
									keylist = keylist + "\n";
								}
							}
							sender.sendMessage(ChatColor.AQUA + keylist);
						}
						else
						{
							//TODO: need to separate out between which categories involve items and which don't
							AbstractSet<String> deathmsglist = (AbstractSet<String>) getConfig().getConfigurationSection("msg." + args[1]).getKeys(false);
							Iterator<String> it = deathmsglist.iterator();
							String keylist = it.next(); //assumes there is always at least one key in the death messages config
							while (it.hasNext())
							{
								keylist = keylist + ", " + it.next();
							}
							sender.sendMessage(ChatColor.AQUA + "Available categories for " + args[1] + ": " + keylist);
						}
					}
					else
					{
						sender.sendMessage(ChatColor.RED + "msg."+args[1]+" is not a valid config key");
					}
				}
				else if (args[0].equalsIgnoreCase("addcat"))
				{
					getConfig().createSection("msg."+args[1]);
					sender.sendMessage(ChatColor.GREEN + "Added " + "msg."+args[1] + " to config with placeholder message.");
					List<String> defList = new LinkedList<String>();
					defList.add("&e&p&3 was killed by &ean unknown lethal force. Want more clarity? Petition your local Padmin today!&3");
					getConfig().set("msg."+args[1], defList);
					getLogger().info("[MOF] " + sender.getName() + " added a new death message category at msg." + args[1] + " with placeholder message.");
				}
				else if (args[0].equalsIgnoreCase("delcat"))
				{
					//if (getConfig().isList("msg."+args[1]))
					//{
						getConfig().set("msg."+args[1], null);
						sender.sendMessage(ChatColor.GREEN + "Deleted " + "msg."+args[1] + " from config.");
						getLogger().info("[MOF] " + sender.getName() + " deleted the death message category at msg." + args[1]);
					//}
				}
				else if (args[0].equalsIgnoreCase("additem"))
				{
					sender.sendMessage(ChatColor.GREEN + "Added " + args[1] + " to custom message items.");
					List<String> itemList = getConfig().getStringList("itemMsg"); 
					itemList.add(args[1]);
					getConfig().set("itemMsg", itemList);
					getLogger().info("[MOF] " + sender.getName() + " added item " + args[1] + " to list of custom message items.");
				}
				//note: this will throw an exception if the integer argument is negative or cannot be parsed
				else if (args[0].equalsIgnoreCase("delitem"))
				{
					//takes in number of the item on the list as an argument
					List<String> itemList = getConfig().getStringList("itemMsg");
					if (Integer.parseInt(args[1])-1 < itemList.size())
					{
						String deletedItem = itemList.get(Integer.parseInt(args[1])-1);
						itemList.remove(Integer.parseInt(args[1])-1);
						getConfig().set("itemMsg", itemList);
						sender.sendMessage(ChatColor.RED + "Removed from " + "itemMsg" + ": " + deletedItem);
						getLogger().info("[MOF] " + sender.getName() + " deleted a custom message item from itemMsg: " + deletedItem + "");
					}
					else
					{
						sender.sendMessage(ChatColor.RED + "/mofmsg delitem [item number]\nUse /mofmsg viewitem to see list of custom message items.");
					}
				}
				else if (args[0].equalsIgnoreCase("delshare"))
				{
					// /mofmsg delshare <mobtypename> (as numbered in the key list)
					if (getConfig().contains("listShare."+args[1])) //if it exists in the config
					{
						String oldmobtypename = getConfig().getString("listShare."+args[1]);
						getConfig().set("listShare."+args[1], null);
						sender.sendMessage(ChatColor.RED + "Deleted " + "listShare."+args[1] + " from config.  Death messages for " + args[1] + " will no longer use list for " + oldmobtypename + ".");
						getLogger().info("[MOF] " + sender.getName() + " deleted the list sharing at listShare." + args[1] + ". Previously these death messages used list for " + oldmobtypename + ".");
					}
					else //if it does not
					{
						sender.sendMessage(ChatColor.RED + "Cannot find death message sharing for mob " + args[1] + ".");
						sender.sendMessage(ChatColor.RED + "/mofmsg delshare [mobtype]\nUse /mofmsg viewshare to see current death message sharing.");
					}
				}
				//TODO: test item report conversion commands
				else if (args[0].equalsIgnoreCase("additemreport"))
				{
					// /mofmsg itemreport <mobtypename> - converts msg.<mobtypename> to msg.<mobtypename>.noitem and adds placeholder for msg.<mobtypename>.item
					if (getConfig().isList("msg."+args[1]))
					{
						List<String> deathMsgStringList = getConfig().getStringList("msg."+args[1]);
						getConfig().set("msg."+args[1], null);
						getConfig().createSection("msg."+args[1]+".noitem");
						getConfig().set("msg."+args[1]+".noitem", deathMsgStringList);
						getConfig().createSection("msg."+args[1]+".item");
						List<String> placeholdItemList = new LinkedList<String>();
						placeholdItemList.add("with &i");
						getConfig().set("msg."+args[1]+".item", placeholdItemList);
						sender.sendMessage(ChatColor.GREEN + "Converted " + args[1] + " to use item reporting.");
						getLogger().info("[MOF] " + sender.getName() + " converted death message category at msg." + args[1] + " to use item reporting.");
					}
					else if (getConfig().isList("msg."+args[1]+".noitem") && getConfig().isList("msg."+args[1]+".item"))
					{
						sender.sendMessage(ChatColor.RED + "Death messages for " + args[1] + " are already configured for item reporting.");
					}
					else
					{
						sender.sendMessage(ChatColor.RED + "Death messages for " + args[1] + " is either not a standalone or item-reporting list.");
					}
				}
				else if (args[0].equalsIgnoreCase("delitemreport"))
				{
					// /mofmsg itemreport <mobtypename> - converts msg.<mobtypename> to msg.<mobtypename>.noitem and adds placeholder for msg.<mobtypename>.item
					if (getConfig().isList("msg."+args[1]))
					{
						sender.sendMessage(ChatColor.RED + "Death messages for " + args[1] + " are already not configured for item reporting.");
						
					}
					else if (getConfig().isList("msg."+args[1]+".noitem") && getConfig().isList("msg."+args[1]+".item"))
					{
						List<String> deathMsgStringList = getConfig().getStringList("msg."+args[1]+".noitem");
						getConfig().set("msg."+args[1]+".noitem", null);
						getConfig().set("msg."+args[1]+".item", null);
						getConfig().createSection("msg."+args[1]);
						getConfig().set("msg."+args[1], deathMsgStringList);
						sender.sendMessage(ChatColor.GREEN + "Removed item reporting from " + args[1] + ".");
						getLogger().info("[MOF] " + sender.getName() + " removed item reporting from death message category at msg." + args[1] + ".");
					}
					else
					{
						sender.sendMessage(ChatColor.RED + "Death messages for " + args[1] + " is either not a standalone or item-reporting list.");
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg [view|add|delete|addcat|delcat|renamecat|additem|delitem|viewitem|additemreport|delitemreport|addshare|delshare|viewshare] (arguments)\nUse /mofmsg to see list of permissible categories.");
				}
				return true;
			}
			else if (args.length > 2 && (sender instanceof Player || sender instanceof ConsoleCommandSender))
			{
				if (args[0].equalsIgnoreCase("add"))
				{
					if (getConfig().contains("msg." + args[1]))
					{
						if (getConfig().isList("msg."+args[1]))
						{
							//takes in death message as argument
							//synthesize the death message from the other args
							String deathmsg = "";
							for (int i=2; i<args.length; i++)
							{
								deathmsg = deathmsg + args[i] + " ";
							}
							deathmsg = deathmsg.substring(0, deathmsg.length()-1);
							List<String> localMsgList = getConfig().getStringList("msg."+args[1]);
							localMsgList.add(deathmsg);
							getConfig().set("msg."+args[1], localMsgList);
							sender.sendMessage(ChatColor.GREEN + "Added to " + "msg."+args[1] + ": " + deathmsg);
							getLogger().info("[MOF] " + sender.getName() + " added a new death message to msg." + args[1] + ": \"" + deathmsg + "\"");
						}
						else
						{
							sender.sendMessage(ChatColor.RED + "That is not a permissible death message category to add to.");
						}
						
					}
					else
					{
						sender.sendMessage(ChatColor.RED + "msg."+args[1]+" is not a valid config key");
					}
				}
				else if (args[0].equalsIgnoreCase("delete"))
				{
					if (getConfig().contains("msg." + args[1]))
					{
						if (getConfig().isList("msg."+args[1]))
						{
							//takes in number of the death message on the list as an argument
							//synthesize the death message from the other args
							List<String> localMsgList = getConfig().getStringList("msg."+args[1]);
							String deletedMsg = localMsgList.get(Integer.parseInt(args[2])-1);
							localMsgList.remove(Integer.parseInt(args[2])-1);
							getConfig().set("msg."+args[1], localMsgList);
							sender.sendMessage(ChatColor.RED + "Removed from " + "msg."+args[1] + ": " + deletedMsg);
							getLogger().info("[MOF] " + sender.getName() + " deleted a death message from msg." + args[1] + ": \"" + deletedMsg + "\"");
						}
						else
						{
							sender.sendMessage(ChatColor.RED + "That is not a permissible death message category to delete from.");
						}
					}
					else
					{
						sender.sendMessage(ChatColor.RED + "msg."+args[1]+" is not a valid config key");
					}
				}
				else if (args[0].equalsIgnoreCase("additem"))
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg additem [item name in Material enum]\nFor the list of Material (ie. item) names, see https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html.");
				}
				else if (args[0].equalsIgnoreCase("addshare"))
				{
					// /mofmsg addshare <mobtypename1> <mobtypename2 whose list to use for kills by mobtypename1>
					if (args.length == 3)
					{
						if (getConfig().contains("listShare."+args[1])) //if the mob is already configured to use a list
						{
							String currentmobtypename = getConfig().getString("listShare."+args[1]);
							sender.sendMessage(ChatColor.RED + "Death messages for "+args[1] + " currently uses list for " + currentmobtypename + ".\nUse /mofmsg delshare [mobtypename] to remove it before re-adding it with the correct death message list to use.");
						}
						else
						{
							getConfig().createSection("listShare."+args[1]);
							sender.sendMessage(ChatColor.GREEN + "Added " + "listShare."+args[1] + " to config with setting " + args[2] + ". Death messages for " + args[1] + " will now use list for " + args[2] + ".");
							getConfig().set("listShare."+args[1], args[2]);
							getLogger().info("[MOF] " + sender.getName() + " configured deaths by " + args[1] + " to use message list for " + args[2] + ".");
						}
					}
					else
					{
						//display error message with correct syntax
						sender.sendMessage(ChatColor.RED + "/mofmsg addshare [killer mobtype] [mobtype whose death message list to use]\nFor list of mob type names recognized, see https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html.");
					}
				}
				else if (args[0].equalsIgnoreCase("renamecat"))
				{
					// intended syntax: /mofmsg renamecat <cat1_oldname> <cat_newname>
					if (args.length == 3)
					{
						//check to make sure <cat_newname> does not already exist and have more than one entry
						//(suggesting it is not from the default config)
						//if it does, stop and notify user
						if (getConfig().contains("msg."+args[2]) && getConfig().getStringList("msg."+args[2]).size() > 1)
						{
								sender.sendMessage(ChatColor.RED + "Category " + args[2] + " exists and has more than one entry.\nUse delcat to remove this first before renaming " + args[1] + ".");
						}
						else if (!getConfig().contains("msg."+args[1])) //if <cat1_oldname> does not exist
						{
							sender.sendMessage(ChatColor.RED + "Category " + args[1] + " does not exist.");
						}
						else
						{
							//create <cat_newname>, get String list from <cat_oldname> and store it in <cat_newname>, then delete <cat_oldname>
							List<String> oldCatList = getConfig().getStringList("msg."+args[1]);
							//if the destination section does not exist, create it
							if (!getConfig().contains("msg."+args[2]))
							{
								getConfig().createSection("msg."+args[2]);
							}
							getConfig().set("msg."+args[2], oldCatList);
							getConfig().set("msg."+args[1], null);
							sender.sendMessage(ChatColor.GREEN + "Renamed category " + "msg."+args[1] + " to " + "msg."+args[2] + ".");
							getLogger().info("[MOF] " + sender.getName() + " renamed death message category from msg." + args[1] + " to msg." + args[2] + ".");
						}
					}
					else
					{
						sender.sendMessage(ChatColor.RED + "/mofmsg renamecat [cat_oldname] [cat_newname]");
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg [view|add|delete|addcat|delcat|renamecat|additem|delitem|viewitem|additemreport|delitemreport|addshare|delshare|viewshare] (arguments)\nUse /mofmsg to see list of permissible categories.");
				}
				return true;
			}
		}
		//Commands related to main feature config display and modification (brick dropping, death messages)
		else if (cmd.getName().equals("mof"))
		{
			if (args.length == 2)
			{
				if (args[0].equalsIgnoreCase("death-msgs") || args[0].equalsIgnoreCase("death-messages"))
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
				else if (args[0].equalsIgnoreCase("brick-dropping-dragon"))
				{
					try
					{
						if (args[1].equalsIgnoreCase("true"))
						{
							//forcibly set config option for death messages to true, save config and implement new option
							getConfig().set("brick-dropping-dragon", true);
							saveConfig();
							sender.sendMessage(ChatColor.GREEN + "Brick dropping for dragons set to true.  Players online will drop bricks when a dragon is slain.");
						}
						else if (args[1].equalsIgnoreCase("false"))
						{
							//forcibly set option to false
							getConfig().set("brick-dropping-dragon", false);
							saveConfig();
							sender.sendMessage(ChatColor.RED + "Brick dropping for dragons set to false.  Players will not drop bricks when a dragon is slain.");
						}
					}
					catch (RuntimeException e)
					{
						sender.sendMessage(ChatColor.RED + "WARNING: That is not valid syntax!");
					}
				}
				else if (args[0].equalsIgnoreCase("tame-traps"))
				{
					try
					{
						if (args[1].equalsIgnoreCase("true"))
						{
							//forcibly set config option for death messages to true, save config and implement new option
							getConfig().set("tame-traps", true);
							saveConfig();
							sender.sendMessage(ChatColor.GREEN + "Trap taming set to true.  Players will be able to tame horses from skeleton traps.");
						}
						else if (args[1].equalsIgnoreCase("false"))
						{
							//forcibly set option to false
							getConfig().set("tame-traps", false);
							saveConfig();
							sender.sendMessage(ChatColor.RED + "Trap taming set to false.  Players will be unable to tame horses from skeleton traps.");
						}
					}
					catch (RuntimeException e)
					{
						sender.sendMessage(ChatColor.RED + "WARNING: That is not valid syntax!");
					}
				}
				else if (args[0].equalsIgnoreCase("log-vanilla-death"))
				{
					try
					{
						if (args[1].equalsIgnoreCase("true"))
						{
							//forcibly set config option for death messages to true, save config and implement new option
							getConfig().set("log-vanilla-death", true);
							saveConfig();
							sender.sendMessage(ChatColor.GREEN + "Vanilla death logging set to true.  The vanilla death message will be printed in console.");
						}
						else if (args[1].equalsIgnoreCase("false"))
						{
							//forcibly set option to false
							getConfig().set("log-vanilla-death", false);
							saveConfig();
							sender.sendMessage(ChatColor.RED + "Vanilla death logging set to false.  The vanilla death message will not be printed in console.");
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
				else if (args[0].equalsIgnoreCase("save"))
				{
					saveConfig();
					sender.sendMessage(ChatColor.AQUA + "MaskOfFutures config saved!");
				}
			}
			else 
			{
				sender.sendMessage(ChatColor.AQUA + "https://github.com/buzzie71/MaskOfFutures/blob/master/README.md");
				sender.sendMessage(ChatColor.AQUA + "=====Mask of Futures, v"+ getDescription().getVersion() +"=====");
				sender.sendMessage(ChatColor.AQUA + "Brick dropping: " + getConfig().getBoolean("brick-dropping"));
				//sender.sendMessage(ChatColor.AQUA + "Brick dropping (Dragon): " + getConfig().getBoolean("brick-dropping-dragon"));
				sender.sendMessage(ChatColor.AQUA + "Death messages: " + getConfig().getBoolean("death-msgs"));
				sender.sendMessage(ChatColor.AQUA + "Tame traps: " + getConfig().getBoolean("tame-traps"));
				sender.sendMessage(ChatColor.AQUA + "Log vanilla death: " + getConfig().getBoolean("log-vanilla-death"));
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
	
	//Displays to a command sender a master-level list of death message categories 
	//(ie. all possible keys under "msg." in the config)
	//input: the CommandSender that (indirectly) called this method (with /mofmsg ...)
	public void displayDeathMessageList(CommandSender sender)
	{
		AbstractSet<String> deathmsglist = (AbstractSet<String>) getConfig().getConfigurationSection("msg").getKeys(false);
		Iterator<String> it = deathmsglist.iterator();
		String keylist = it.next(); //assumes there is always at least one key in the death messages config
		while (it.hasNext())
		{
			keylist = keylist + ", " + it.next();
		}
		sender.sendMessage(ChatColor.AQUA + "Available death message categories: " + keylist);
	}
	
	//this method toggles a player's ability to see vanilla or custom death messages
	//by checking if the player's UUID is in the old message player list
	//if it is on the list already, this method removes it
	//if it is missing from the list, this method adds it
	//metadata is also given to/removed from the player so the plugin doesn't always need to check the old msg list file to see if the player is on it
	//input oldMsgPlayerList: the String List of player UUIDs to send vanilla-style death messages to
	//input p: the Player to either add to or remove from the old message player list
	public void togglePlayerOnList(List<String> oldMsgPlayerList, Player p)
	{
		//check if the player's UUID is in the list already
		if (oldMsgPlayerList.contains(p.getUniqueId().toString()))
		{
			oldMsgPlayerList.remove(p.getUniqueId().toString());
			if (p.hasMetadata("MaskOfFutures.oldMsg"))
			{
				p.removeMetadata("MaskOfFutures.oldMsg", this);
			}
			p.sendMessage(ChatColor.GREEN + "You will now receive custom death messages. Use /toggle-oldmsg to turn off custom death messages.");
		}
		else
		{
			oldMsgPlayerList.add(p.getUniqueId().toString());
			p.setMetadata("MaskOfFutures.oldMsgs", new FixedMetadataValue(this, "true"));
			p.sendMessage(ChatColor.RED + "You will now receive vanilla death messages. Use /toggle-oldmsg to turn on custom death messages.");
		}
		
		/* this implementation works
		UUID playerUUID = p.getUniqueId();
		for (String uuid: oldMsgPlayerList)
		{
			if (uuid.equalsIgnoreCase(playerUUID.toString()))
			{
				oldMsgPlayerList.remove(uuid); //if found, mechanics to remove it
				if (p.hasMetadata("MaskOfFutures.oldMsg"))
				{
					p.removeMetadata("MaskOfFutures.oldMsg", this);
				}
				p.sendMessage(ChatColor.GREEN + "You will now receive custom death messages. Use /oldmsg to turn off custom death messages.");
				return;
			}
		}
		//If player not found, add player UUID to the list and tag them with metadata
		oldMsgPlayerList.add(p.getUniqueId().toString());
		p.setMetadata("MaskOfFutures.oldMsgs", new FixedMetadataValue(this, "true"));
		p.sendMessage(ChatColor.RED + "You will now receive vanilla death messages. Use /oldmsg to turn on custom death messages.");
		return;
		*/
	}
	
	
	//TODO: test this if it ends up being used.
	//Reformats an existing list of no-item-reporting death messages into ones that can
	//accommodate item reporting - this will move the list of death messages found in
	//msg.<mobtypename> to msg.<mobtypename>.noitem, then create a msg.<mobtypename>.item
	//with one default element.
	//input mobtypename: the String name of the mob type of a mob that can kill the player
	//input sender: the CommandSender that issued the command that calls this method
	public void addItemReporting(String mobtypename, CommandSender sender)
	{
		//is it possible to check to make sure that mobtypename corresponds to a LivingEntity?
		//this will add another layer of protection against miscalling this method, 
		//eg. prevent this from being called on a list for kill by status effect cloud
		//TODO: checks to prevent misuse of this method
		
		//move death messages to .noitem list, then create a .item list with a default element
		List<String> deathMsgsList = getConfig().getStringList("msg."+mobtypename);
		getConfig().set("msg."+mobtypename, null);
		getConfig().createSection("msg."+mobtypename+".noitem");
		getConfig().set("msg."+mobtypename+".noitem", deathMsgsList);
		sender.sendMessage(ChatColor.GREEN + "Changed death message for " + mobtypename + " to use item reporting.");
		getConfig().createSection("msg."+mobtypename+".item");
		List<String> newItemList = new LinkedList<String>();
		newItemList.add("with &i");
		getConfig().set("msg."+mobtypename+".item", newItemList);
		getLogger().info("[MOF] " + sender.getName() + " changed msg." + mobtypename + " to use item reporting.");
	}
	
	//TODO: test this if it ends up being used.
	//Reformats an existing pair of item/noitem lists of item-reporting death messages into ones
	//that ignore item reporting - this will move the list of death messages found in
	//msg.<mobtypename>.noitem to msg.<mobtypename>, then delete the msg.<mobtypename>.item
	//list.
	//input mobtypename: the String name of the mob type of a mob that can kill the player
	//input sender: the CommandSender that issued the command that calls this method
	public void removeItemReporting(String mobtypename, CommandSender sender)
	{
		//TODO: checks to prevent misuse of this method
		
		//remove .item and .noitem lists and move death messages to list under msg.<mobtypename> directly
		List<String> deathMsgsList = getConfig().getStringList("msg."+mobtypename+".noitem");
		getConfig().set("msg."+mobtypename+".noitem", null);
		getConfig().set("msg."+mobtypename+".item", null);
		getConfig().createSection("msg."+mobtypename);
		getConfig().set("msg."+mobtypename, deathMsgsList);
		sender.sendMessage(ChatColor.GREEN + "Removed item reporting in death messages for " + mobtypename + ".");
		getLogger().info("[MOF] " + sender.getName() + " removed item reporting from msg." + mobtypename + ".");
	}
}