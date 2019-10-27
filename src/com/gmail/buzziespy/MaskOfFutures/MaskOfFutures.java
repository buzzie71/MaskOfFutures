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
			oldMsgPlayersConfig.load(oldMsgPlayersFile);;
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
				sender.sendMessage(ChatColor.RED + "/mofmsg [view|add|delete|addcat|delcat] [category] (arguments)");
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
				else
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg [view|add|delete|addcat|delcat] [category] (arguments)\nUse /mofmsg to see list of permissible categories.");
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
				else
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg [view|add|delete|addcat|delcat] [category] (arguments)\nUse /mofmsg to see list of permissible categories.");
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
				else
				{
					sender.sendMessage(ChatColor.RED + "/mofmsg [view|add|delete|addcat|delcat] [category] (arguments)\nUse /mofmsg to see list of permissible categories.");
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
	
}