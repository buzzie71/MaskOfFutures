package com.gmail.buzziespy.MaskOfFutures;

import java.io.File;
import java.util.AbstractSet;

/*
 * Comments for MoF have been added to aid in maintainability.
 * NOTE: Most of the interesting code is over in BeingListener, which handles all the listening
 * for this plugin.
 *
 * This code was written back in 1.7, before the Bukkit DMCA madness.  It was discovered when
 * nerd.nu transitioned to 1.8 that odd things about the 1.8 Spigot version in use broke some things
 * that previously worked.  Among them:
 *
 * - Brick dropping no longer works even with the config option set to true
 * - Kills with a potion are shown as "<X> was killed by entity.ThrownPotion.name" or something similarly esoteric
 * 		- Custom death messages cannot catch it
 * 		- This appears to be the case even with the plugin disabled
 * - There is a "<X> was pummeled by Wither" vanilla death message that I haven't been able to reproduce
 * 		- Custom death messages apparently do not catch this
 *
 * All of the above have been fixed in the 0.10.4 release.
 *
 * Other than that, there is plenty of room for code condensing if anyone feels like taking a stab at it.
 *
 * There is some extraneous code related to a sign appearing at the location of a death present in the
 * code below - in the interests of completing the plugin I had opted to leave it in as long as it did
 * not interfere with the other features being programmed.  I have endeavored to mark as many of these
 * as possible below.  Whether the implementation is finished is left to whoever chooses to undertake
 * it.
 */


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;

import nu.nerd.modmode.ModMode;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Slime;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Spider;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

import com.gmail.buzziespy.MaskOfFutures.MaskOfFutures;


public final class BeingListener implements Listener{

		static MaskOfFutures plugin;
		//final boolean SADIST_MODE = true;

		public BeingListener(MaskOfFutures p)
		{
			plugin = p;
			plugin.getServer().getPluginManager().registerEvents(this, plugin);

			plugin.getLogger().info("Loading config file...");
			//load up the config file
			plugin.getConfig();
			//check for listShare and itemMsg keys; if the config does not contain them, create them
			//apparently if these don't exist the config will pull the values from the default config?
			//so this code may be unnecessary
			/* commented out - enough time has passed that config users should have this already
			if (!plugin.getConfig().contains("itemMsg"))
			{
				plugin.getConfig().createSection("itemMsg");
				List<String> newItemMsgList = new LinkedList<String>();
				newItemMsgList.add("cod");
				newItemMsgList.add("feather");
				newItemMsgList.add("glowstone");
				newItemMsgList.add("glowstone_dust");
				plugin.getConfig().set("itemMsg", newItemMsgList);
				plugin.getLogger().info(ChatColor.GREEN + "Added itemMsg key with default settings since it was absent in the config");
				plugin.saveConfig();
			}
			if (!plugin.getConfig().contains("listShare"))
			{
				plugin.getConfig().createSection("listShare.husk");
				plugin.getConfig().set("listShare.husk", "zombie");
				plugin.getConfig().createSection("listShare.zombie_villager");
				plugin.getConfig().set("listShare.zombie_villager", "zombie");
				plugin.getConfig().createSection("listShare.stray");
				plugin.getConfig().set("listShare.stray", "skeleton");
				plugin.getConfig().createSection("listShare.wither_skeleton");
				plugin.getConfig().set("listShare.wither_skeleton", "skeleton");
				plugin.getLogger().info(ChatColor.GREEN + "Added listShare key with default settings since it was absent in the config");
				plugin.saveConfig();
			}*/
			//code to convert previous format of listShare key-string pairs to new format of string list
			if (!plugin.getConfig().isList("listShare") && plugin.getConfig().contains("listShare")) //assume that if listShare is not a list (presumably of strings) then it's using the old version
			{
				//get all the different keys that are in there, process share origins and targets into new format
				AbstractSet<String> sharelist = (AbstractSet<String>) plugin.getConfig().getConfigurationSection("listShare").getKeys(false);
				Iterator<String> it = sharelist.iterator();
				List<String> newShareList = new LinkedList<String>();
				while (it.hasNext())
				{
					String keyToAdd = it.next();
					newShareList.add(keyToAdd+","+plugin.getConfig().getString("listShare."+keyToAdd));
				}
				//clear listShare and then remake it as a string list with newShareList
				plugin.getConfig().set("listShare", null);
				plugin.getConfig().createSection("listShare");
				plugin.getConfig().set("listShare", newShareList);
				plugin.getLogger().info("[MOF] listShare was converted from old format to new format.");
			}
			
			//save copy
			plugin.saveDefaultConfig();
		}

		//handles possible brick dropping on wither explode if this is enabled in the config
		//when a wither explodes shortly after it is built and the brick dropping option is enabled,
		//every player online drops a brick at their feet with special lore.
		//this brick dropping is skipped for anyone who happens to be in ModMode at the time, 
		//and their names are instead logged to console
		//note that players who are too close to the wither when it explodes may have their 
		//brick vaporized before they can pick it up
		//input e: the EntityExplodeEvent called whenever an entity explodes (I think?)
		@EventHandler
		public void onWitherExplode(EntityExplodeEvent e)
		{
			//plugin.getLogger().info("EntityExplodeEvent! " + e.getEntity().getName().toString());
			if (plugin.getConfig().getBoolean("brick-dropping") && e.getEntityType().equals(EntityType.WITHER))
			{
				ItemStack woolbrick = new ItemStack(Material.BRICK, 1);
				ItemMeta woolbrickInfo = woolbrick.getItemMeta();
				List<String> bricklore = new ArrayList<String>(); //not sure how to optimize this


				//Player[] playerList = (Player[])plugin.getServer().getOnlinePlayers().toArray();
				Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
				Player[] playerList = players.toArray(new Player[players.size()]);

				String log = "Players who dropped bricks on hearing wither: ";
				for (Player p: playerList)
				{
					if (!p.hasMetadata("MaskOfFutures.wither"))
					{
						//if player is in modmode, log to console
						bricklore.add(ChatColor.GOLD + "" + ChatColor.ITALIC + p.getName() + " dropped this on hearing a Wither");
						woolbrickInfo.setLore(bricklore);
						woolbrick.setItemMeta(woolbrickInfo);

						//Quick and dirty way of doing this is to check for survival mode and flight enabled
						if (plugin.mmode != null)
						{
							if (plugin.mmode.isModMode(p))//player is in modmode - log intended drop, don't actually do it
							{
								plugin.getLogger().info(p.getName() + " is in ModMode; brick dropping canceled");
							}
							else //if player is not in modmode, just drop naturally
							{
								p.getWorld().dropItemNaturally(p.getLocation(), woolbrick);
							}
						}
						else //if modmode is not present just drop naturally
						{
							p.getWorld().dropItemNaturally(p.getLocation(), woolbrick);
						}
						bricklore.clear();

						p.setMetadata("MaskOfFutures.wither", new FixedMetadataValue(plugin, "true"));
					}
					else //if player has the metadata then the brick has already been dispensed
					{
						p.removeMetadata("MaskOfFutures.wither", plugin);
					}

					log += p.getName() + " ";
				}

				plugin.getLogger().info(log);
			}
			else
			{
				if (e.getEntityType().equals(EntityType.WITHER))
				{
					plugin.getLogger().info("Brick dropping canceled; not enabled in config");
				}
			}
		}

		//TODO: Add in code to drop a brick for dragon death screams
		//Need to find the event that can hook into this first.
		/*
		@EventHandler
		public void onDragonDeathScream(SomeEvent e)
		{
			if (plugin.getConfig().getBoolean("brick-dropping") && e.getEntityType().equals(EntityType.WITHER))
			{
				ItemStack woolbrick = new ItemStack(Material.CLAY_BRICK, 1);
				ItemMeta woolbrickInfo = woolbrick.getItemMeta();
				List<String> bricklore = new ArrayList<String>(); //not sure how to optimize this


				//Player[] playerList = (Player[])plugin.getServer().getOnlinePlayers().toArray();
				Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
				Player[] playerList = players.toArray(new Player[players.size()]);

				String log = "Players who dropped bricks on hearing dragon: ";
				for (Player p: playerList)
				{
					if (!p.hasMetadata("MaskOfFutures.dragon"))
					{
						//if player is in modmode, log to console
						bricklore.add(ChatColor.GOLD + "" + ChatColor.ITALIC + p.getName() + " dropped this on hearing a slain Ender Dragon");
						woolbrickInfo.setLore(bricklore);
						woolbrick.setItemMeta(woolbrickInfo);

						//Quick and dirty way of doing this is to check for survival mode and flight enabled
						if (plugin.mmode != null)
						{
							if (plugin.mmode.isModMode(p))//player is in modmode - log intended drop, don't actually do it
							{
								plugin.getLogger().info(p.getName() + " is in ModMode; brick dropping canceled");
							}
							else //if player is not in modmode, just drop naturally
							{
								p.getWorld().dropItemNaturally(p.getLocation(), woolbrick);
							}
						}
						else //if modmode is not present just drop naturally
						{
							p.getWorld().dropItemNaturally(p.getLocation(), woolbrick);
						}
						bricklore.clear();

						p.setMetadata("MaskOfFutures.dragon", new FixedMetadataValue(plugin, "true"));
					}
					else //if player has the metadata then the brick has already been dispensed
					{
						p.removeMetadata("MaskOfFutures.dragon", plugin);
					}

					log += p.getName() + " ";
				}

				plugin.getLogger().info(log);
			}
			else
			{
				if (e.getEntityType().equals(EntityType.WITHER))
				{
					plugin.getLogger().info("Brick dropping canceled; not enabled in config");
				}
			}
		}*/
		
		//related to sign drop on death
		//removed - signs with last line starting with &a do not drop themselves, unintended
		@EventHandler
		public void onSignBreak(BlockBreakEvent e)
		{
			//checking for signs attached to floor for now since death signs are by code sign posts (see onPlayerDeath())
			/*if (e.getBlock().getType().equals(Material.SIGN_POST))
			{
				Sign s = (Sign)e.getBlock().getState();
				if (s.getLine(3).startsWith("&a"))
				{
					e.setCancelled(true);
					e.getBlock().setType(Material.AIR);
				}
			}*/
		}

		//for undead horse taming
		@EventHandler
		public void onMobClick(PlayerInteractEntityEvent e)
		{
			if (e.getPlayer().hasMetadata("MaskOfFutures.tame"))
			{
				e.getPlayer().removeMetadata("MaskOfFutures.tame", plugin);
				if (e.getRightClicked() instanceof Horse)
				{
					Horse h = (Horse)e.getRightClicked();
					if ((h.getVariant() == Horse.Variant.SKELETON_HORSE || h.getVariant() == Horse.Variant.UNDEAD_HORSE) && h.isTamed() == false)
					{
						h.setTamed(true);
						h.setOwner(e.getPlayer());
						e.getPlayer().sendMessage(ChatColor.GREEN + "You have tamed this " + h.getVariant() + ".");
						plugin.getServer().getPluginManager().callEvent(new EntityTameEvent(h, e.getPlayer()));
					}
					else if ((h.getVariant() == Horse.Variant.SKELETON_HORSE || h.getVariant() == Horse.Variant.UNDEAD_HORSE) && h.isTamed() == true && h.getOwner() == null && plugin.getConfig().getBoolean("tame-traps"))//skeleton/maybe zombie? horse exception - tamed but with no owner
					{
						//h.setTamed(true);
						h.setOwner(e.getPlayer());
						e.getPlayer().sendMessage(ChatColor.GREEN + "You have tamed this trap " + h.getVariant() + ".");
						plugin.getServer().getPluginManager().callEvent(new EntityTameEvent(h, e.getPlayer()));
					}
					else
					{
						e.getPlayer().sendMessage(ChatColor.RED + "This horse is either tamed or not undead.");
					}
				}
				else
				{
					e.getPlayer().sendMessage(ChatColor.RED + "This is not an undead horse.");
				}
			}
			else if (e.getPlayer().hasMetadata("MaskOfFutures.tameadmin"))
			{
				String playerToTameTo = e.getPlayer().getMetadata("MaskOfFutures.tameadmin").get(0).asString();
				e.getPlayer().removeMetadata("MaskOfFutures.tameadmin", plugin);
				if (e.getRightClicked() instanceof Horse)
				{
					Horse h = (Horse)e.getRightClicked();
					if ((h.getVariant() == Horse.Variant.SKELETON_HORSE || h.getVariant() == Horse.Variant.UNDEAD_HORSE) && h.isTamed() == false)
					{
						h.setTamed(true);
						h.setOwner(e.getPlayer().getServer().getOfflinePlayer(playerToTameTo));
						e.getPlayer().sendMessage(ChatColor.GREEN + "You have tamed this " + h.getVariant() + " to " + playerToTameTo + ".");
						//This spams console with a stacktrace if the player is offline
						//plugin.getServer().getPluginManager().callEvent(new EntityTameEvent(h, e.getPlayer().getServer().getOfflinePlayer(playerToTameTo)));
					}
					else
					{
						e.getPlayer().sendMessage(ChatColor.RED + "This horse is either tamed or not undead.");
					}
				}
				else
				{
					e.getPlayer().sendMessage(ChatColor.RED + "This is not an undead horse.");
				}
			}
		}

		//handles the player death event to display a custom death message
		//input e: the PlayerDeathEvent associated with a player dying
		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent e)
		{
			//DEBUG:
			if (!plugin.getConfig().getBoolean("death-msgs"))
			{
				//if (e.getEntity().getLastDamageCause() != null)
				if (!isDefaultDeath(e.getDeathMessage(), e.getEntity().getName()))
				{
					plugin.getLogger().info("Death reason: " + e.getEntity().getLastDamageCause().getCause());
				}
				else // default death message/death by plugin magic
				{
					plugin.getLogger().info("Death reason: default/killed by plugin");
				}
			}

			//related to sign drop on death
			//signpoint calculations here - handling the death reason takes place after handling the death message
			//in order to separate death messages from death signs
			String signReason = "";

			Location deathpoint = e.getEntity().getLocation();
			float yaw = deathpoint.getYaw();
			boolean isInAir = false;
			boolean isUnderGround = false; //I don't think this will matter at all but left this here in case

			Location signpoint = new Location(e.getEntity().getWorld(), deathpoint.getBlockX()+0.5, deathpoint.getBlockY()+0.5, deathpoint.getBlockZ()+0.5);

			//check if signpoint is on a solid block and not in air
			while (signpoint.getBlock().getRelative(0, -1, 0).isEmpty() || signpoint.getBlock().getRelative(0, -1, 0).isLiquid())
			{
				isInAir = true;
				signpoint = signpoint.add(0, -1, 0);
				if (signpoint.getBlockY() < 1)
				{
					signpoint.setY(1);
					break;
				}
			}

			//check if signpoint is on a solid block and not underground
			while (!signpoint.getBlock().isEmpty() && !signpoint.getBlock().getType().equals(Material.WATER))
			{
				isUnderGround = true;
				signpoint = signpoint.add(0, 1, 0);
				if (signpoint.getBlockY() > 256)
				{
					signpoint = null; //if signpoint goes beyond the world ceiling, do not place a sign on death
				}
			}

			//related to sign drop on death - proposed death sign message format
			/* Messages (Line char limit 15; one needs to be 13 to accomodate a color code):
			 *
			 *   " verylongname"
			 *   "had a great"
			 *   "fall"
			 *   "&110-31 00:00"
			 */

			Player victim = (Player)e.getEntity();
			
			//DEBUG
			//plugin.getLogger().info("Original death message: " + e.getDeathMessage());
			//plugin.getLogger().info("Predicted default message?: " + (e.getDeathMessage().equals(victim.getName() + " died")));
			
			
			
			//Handle zapping by TestPlugin (personal project, not a nerd plugin) first
			if (victim.hasMetadata("TestPlugin.lightningKill"))
			{
				//e.setDeathMessage(ChatColor.GOLD + victim.getName() + ChatColor.DARK_AQUA + " was zapped by " + ChatColor.RED + victim.getMetadata("TestPlugin.lightningKill").get(0).asString() + ChatColor.DARK_AQUA);
			}
			//Handle death by drinking in MoreBeverages (personal project, not a nerd plugin)
			else if (victim.hasMetadata("MoreBeverages.drunk"))
			{
				//e.setDeathMessage(ChatColor.GOLD + victim.getName() + ChatColor.DARK_AQUA + " had a bit too much to drink");
			}
			
			//handle default death message
			//this check is necessary since the last damage cause reporting does not
			//consistently report default death even if the vanilla death message does
			else if (isDefaultDeath(e.getDeathMessage(), e.getEntity().getName()))
			{
				//DEBUG
				//plugin.getLogger().info("Player default death!");
				dispatchDeathMessage(e, getDeathReason("default", e.getEntity().getName()));
			}

			else if (victim.getLastDamageCause() instanceof EntityDamageByEntityEvent)
			{ //handle all entity-related deaths here
				EntityDamageByEntityEvent ee = (EntityDamageByEntityEvent)victim.getLastDamageCause();

				//DEBUG:
				if (!plugin.getConfig().getBoolean("death-msgs"))
				{
					plugin.getLogger().info("Last damager: " + ee.getDamager().getType().toString());
					
					//DEBUG: for projectile death message key format
					if (ee.getDamager() instanceof Projectile)
					{
						ProjectileSource ps = ((Projectile)ee.getDamager()).getShooter();
						plugin.getLogger().info("Expected key for projectile kill: " + getFormattedProjectileDeathCat((Projectile)ee.getDamager(),ps));
					}
				}

				
				
				String killerMob = ee.getDamager().getType().toString().toLowerCase();
				//TODO: New handling of generic projectile kills
				if (ee.getDamager() instanceof Projectile)
				{
					Projectile p = (Projectile)ee.getDamager();
					killerMob = getFormattedProjectileDeathCat(p,p.getShooter());
				}
				
				//DEBUG
				//plugin.getLogger().info("Mob that killed player: " + ee.getDamager().getType().toString().toLowerCase());
				
				//Intercept the mob type and redirect to another death message list if it is specified in the config
				//this has been refactored to occur as part of getDeathReason() (all method variants)
				//killerMob = getProcessedMobType(killerMob);
				
				//thorns exception; if died to thorns handle this first
				//I think this only happens with LivingEntity entity kills?
				if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
				{
					LivingEntity z = (LivingEntity)ee.getDamager();
					ItemStack itemWeapon = z.getEquipment().getItemInMainHand();
					dispatchDeathMessage(e, getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
					return;
				}
				
				
				if (ee.getDamager() instanceof Player)
				{  //handle player kills here
					LivingEntity z = (LivingEntity)ee.getDamager();
					ItemStack itemWeapon = z.getEquipment().getItemInMainHand();
					if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
					{
					    dispatchDeathMessage(e, getDeathReason("player", e.getEntity().getName(), z, itemWeapon));
					}
					else if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
					{
					    dispatchDeathMessage(e, getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
					}
				}
				else
				{  //handle hostile mob kills here
					//Anvil kill
					//else 
					if (ee.getDamager() instanceof FallingBlock)
					{
						FallingBlock fb = (FallingBlock)ee.getDamager();
						String blockType = fb.getMaterial().toString().toLowerCase();
						//DEBUG:
						//plugin.getLogger().info("key: falling_block."+blockType);
						dispatchDeathMessage(e, getDeathReason("falling_block."+blockType, e.getEntity().getName()));
						//placeSignFromReason("anvil", signpoint, e.getEntity());
					}
					
					//Witch kill
					/*else if (ee.getDamager() instanceof Witch)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInMainHand();
						dispatchDeathMessage(e, getDeathReason("witch", e.getEntity().getName(), z, itemWeapon));
						//placeSignFromReason("witch", signpoint, e.getEntity());
					}*/
					
					//Shulker kills
					/*else if (ee.getDamager() instanceof ShulkerBullet)
					{
						//This method assumes that Shulkers are the sole shooter of their bullets
						//if other mobs (eg. players) can fire these, the config organization may need
						//to be reshuffled to distinguish between sources
						ShulkerBullet tp = (ShulkerBullet)ee.getDamager();
						ProjectileSource le = (ProjectileSource)tp.getShooter();
						//plugin.getLogger().info("Projectile source: " + le.toString());
						if (le instanceof Shulker)
						{
							Shulker w = (Shulker)le;
							dispatchDeathMessage(e, getDeathReason("shulker", e.getEntity().getName(), w));
						}
					}*/
					
					//Wither kill
					else if (ee.getDamager() instanceof Wither)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION))
						{
							dispatchDeathMessage(e, getDeathReason("wither.explosion", e.getEntity().getName(), z));
						}
						else
						{
							dispatchDeathMessage(e, getDeathReason("wither.kill", e.getEntity().getName(), z));
						}
					}
					//Wither skull kill - seems this is registered differently from Wither
					/*else if (ee.getDamager() instanceof WitherSkull)
					{
						WitherSkull tp = (WitherSkull)ee.getDamager();
						ProjectileSource le = (ProjectileSource)tp.getShooter();
						//plugin.getLogger().info("Projectile source: " + le.toString());
						if (le instanceof Wither)
						{
							Wither w = (Wither)le;
							dispatchDeathMessage(e, getDeathReason("wither.kill", e.getEntity().getName(), w));
						}
					}*/
					//arrow kill - note that in 1.13 Trident inherits Arrow, so need to check that the arrow is not a trident
					/*else if (ee.getDamager() instanceof Arrow && !(ee.getDamager() instanceof Trident))
					{
						//perhaps change this based on distance of shooter?
						Arrow arrow = (Arrow)ee.getDamager();
						/*if (arrow.getShooter() != null)
						{
							plugin.getLogger().info("Shooter: " + arrow.getShooter().toString());
						}
						else
						{
							plugin.getLogger().info("Shooter: null");
						}*/
						//1.9.2: dispenser exception
						/*
						if (arrow.getShooter() instanceof BlockProjectileSource)
						{
							dispatchDeathMessage(e, getDeathReason("arrow.dispenser", e.getEntity().getName(), "Dispenser"));
						}
						else//if an entity fired this arrow
						{
							LivingEntity le = (LivingEntity)arrow.getShooter();
							ItemStack itemWeapon = le.getEquipment().getItemInMainHand();
							if (le instanceof Skeleton)
							{
								dispatchDeathMessage(e, getDeathReason("arrow.skeleton", e.getEntity().getName(), le, itemWeapon));
							}
							else if (le instanceof Player)
							{
								dispatchDeathMessage(e, getDeathReason("arrow.player", e.getEntity().getName(), le, itemWeapon));
							}
							else if (le instanceof Pillager)
							{
								dispatchDeathMessage(e, getDeathReason("arrow.pillager", e.getEntity().getName(), le, itemWeapon));
							}
							else if (le instanceof Piglin)
							{
								dispatchDeathMessage(e, getDeathReason("arrow.piglin", e.getEntity().getName(), le, itemWeapon));
							}
							else if (le instanceof Illusioner)
							{
								dispatchDeathMessage(e, getDeathReason("arrow.illusioner", e.getEntity().getName(), le, itemWeapon));
							}
						}
						
					}*/
					/*else if (ee.getDamager() instanceof ThrownPotion)
					{
						//perhaps change this based on distance of shooter?
						ThrownPotion tp = (ThrownPotion)ee.getDamager();
						//LivingEntity le = (LivingEntity)tp.getShooter();
						ProjectileSource le = (ProjectileSource)tp.getShooter();
						//plugin.getLogger().info("Projectile source: " + le.toString());
						if (le instanceof Witch)
						{
							Witch w = (Witch)le;
							ItemStack itemWeapon = w.getEquipment().getItemInMainHand();
							dispatchDeathMessage(e, getDeathReason("potion.witch", e.getEntity().getName(), w, itemWeapon));
						}
						else if (le instanceof Player)
						{
							Player p = (Player)le;
							ItemStack itemWeapon = p.getEquipment().getItemInMainHand();
							dispatchDeathMessage(e, getDeathReason("potion.player", e.getEntity().getName(), p, itemWeapon));
						}
						else if (le instanceof BlockProjectileSource) //ie. dispenser
						{
							dispatchDeathMessage(e, getDeathReason("potion.dispenser", e.getEntity().getName(), "Dispenser"));
						}
					}*/
					/*else if (ee.getDamager() instanceof LargeFireball)
					{
						//perhaps change this based on distance of shooter?
						LargeFireball tp = (LargeFireball)ee.getDamager();
						//Given the uncertainty over Bukkit's future I will assume deprecated methods are fair game.
						LivingEntity le = (LivingEntity)tp.getShooter();
						if (le instanceof Ghast) //almost always is but just in case
						{
							dispatchDeathMessage(e, getDeathReason("fireball.ghast", e.getEntity().getName(), le));
						}

					}*/
					/*else if (ee.getDamager() instanceof SmallFireball)
					{
						//perhaps change this based on distance of shooter?
						SmallFireball tp = (SmallFireball)ee.getDamager();
						//Given the uncertainty over Bukkit's future I will assume deprecated methods are fair game.
						ProjectileSource le = (ProjectileSource)tp.getShooter();
						if (le instanceof Blaze)
						{
							dispatchDeathMessage(e, getDeathReason("fireball.blaze", e.getEntity().getName(), (Blaze)le));
						}
						//need handling for dispensed small fireballs (fire charges)
						else
						{
							dispatchDeathMessage(e, getDeathReason("fireball.dispenser", e.getEntity().getName()));
						}
					}*/
					//overzealous pearl suicides
					/*else if (ee.getDamager() instanceof EnderPearl)
					{
						dispatchDeathMessage(e, getDeathReason("pearl", e.getEntity().getName()));
					}*/
					//TNT kills (eg. in desert temples)
					else if (ee.getDamager() instanceof TNTPrimed)
					{
						//TNTPrimed t = (TNTPrimed) ee.getDamager();
						//Entity eee = t.getSource();
						//get the entity that ignites the TNT that kills the player...somehow
						//using getSource() on the TNTPrimed object returns null when I suicide with it
						//however, getting a skeleton to shoot a flame arrow at a TNT block that kills me
						//is in fact recognized by the server, who claims the skeleton is the igniter
						//so does null only apply when the player who dies is also the igniter?
						//DEBUG
						//if (eee != null)
						//{
						//	plugin.getServer().getLogger().info("TNT Entity source: " + eee);
						//}
						//else
						//{
						//	plugin.getServer().getLogger().info("TNT Entity source: null");
						//}
						//use the custom methods to get the attacker name if it exists
						String tntdeath = e.getDeathMessage();
						if (isTNTAttackDeath(tntdeath, e.getEntity().getName()))
						{
							dispatchDeathMessage(e, getDeathReason("tnt.entity", e.getEntity().getName(), getTNTAttackerName(tntdeath, e.getEntity().getName())));
						}
						else //if not killed by someone else
						{
							dispatchDeathMessage(e, getDeathReason("tnt.noentity", e.getEntity().getName()));
						}
						//dispatchDeathMessage(e, getDeathReason("tnt.noentity", e.getEntity().getName()));
					}
					//Lingering potion of harming kills
					else if (ee.getDamager() instanceof AreaEffectCloud)
					{
						AreaEffectCloud t = (AreaEffectCloud) ee.getDamager();
						ProjectileSource eee = t.getSource();
						//DEBUG
						/*if (eee != null)
						{
							plugin.getServer().getLogger().info("Cloud source: " + eee.toString());
						}
						else
						{
							plugin.getServer().getLogger().info("Cloud source: null");
						}*/
						//at the moment only players and dispensers can shoot lingering potions...also dragons
						if (eee instanceof Player) //if players 
						{
							Player p = (Player)eee;
							dispatchDeathMessage(e, getDeathReason("cloud.player", e.getEntity().getName(), p));
						}
						else if (eee == null) //as of 1.10.2, Ender dragon breath pools are being reported with source of null
						{
							dispatchDeathMessage(e, getDeathReason("cloud.generic", e.getEntity().getName()));
						}
						else if (eee instanceof EnderDragon)
						{
							EnderDragon ed = (EnderDragon)eee;
							dispatchDeathMessage(e, getDeathReason("cloud.dragon", e.getEntity().getName(), ed));
						}
						else if (eee instanceof BlockProjectileSource) //dispenser
						{
							dispatchDeathMessage(e, getDeathReason("cloud.dispenser", e.getEntity().getName(), "Dispenser"));
						}
					}
					//Evoker kills - assuming it cannot hold items. Evokers kill by fangs.
					else if (ee.getDamager() instanceof EvokerFangs)
					{
						EvokerFangs ef = (EvokerFangs)ee.getDamager();
						LivingEntity z = (LivingEntity)ef.getOwner();
						dispatchDeathMessage(e, getDeathReason("evoker", e.getEntity().getName(), z));
					}
					//Trident kills
					/*else if (ee.getDamager() instanceof Trident)
					{
						//perhaps change this based on distance of shooter?
						Trident trident = (Trident)ee.getDamager();
						/*if (trident.getShooter() != null)
						{
							plugin.getLogger().info("Shooter: " + arrow.getShooter().toString());
						}
						else
						{
							plugin.getLogger().info("Shooter: null");
						}*/
						//commented out dispenser code, dispensers cannot shoot tridents
						/*if (trident.getShooter() instanceof BlockProjectileSource)
						{
							dispatchDeathMessage(e, getDeathReason("arrow.dispenser", e.getEntity().getName(), "Dispenser"));
						}
						else//if an entity fired this arrow
						{*/
							/*LivingEntity le = (LivingEntity)trident.getShooter();
							ItemStack itemWeapon = le.getEquipment().getItemInMainHand();
							if (le instanceof Drowned)
							{
								dispatchDeathMessage(e, getDeathReason("trident.drowned", e.getEntity().getName(), le, itemWeapon));
							}
							else if (le instanceof Player)
							{
								dispatchDeathMessage(e, getDeathReason("trident.player", e.getEntity().getName(), le, itemWeapon));
							}
						//}
						
					}*/
					//Firework kills
					/*else if (ee.getDamager() instanceof Firework)
					{
							dispatchDeathMessage(e, getDeathReason("firework", e.getEntity().getName()));
					}*/
					//Llama kill
					/*else if (ee.getDamager() instanceof LlamaSpit)
					{
						LlamaSpit tp = (LlamaSpit)ee.getDamager();
						ProjectileSource le = (ProjectileSource)tp.getShooter();
						//plugin.getLogger().info("Projectile source: " + le.toString());
						if (le instanceof Llama)
						{
							Llama w = (Llama)le;
							dispatchDeathMessage(e, getDeathReason("llama", e.getEntity().getName(), w));
						}
					}*/
					//Ender Crystal kills
					else if (ee.getDamager() instanceof EnderCrystal)
					{
							dispatchDeathMessage(e, getDeathReason("endercrystal", e.getEntity().getName()));
					}
					//Lightning kills
					else if (ee.getDamager() instanceof LightningStrike)
					{
							dispatchDeathMessage(e, getDeathReason("lightning", e.getEntity().getName()));
					}
					
					//after checking for special kill handling,
					//use generic method for handling kills
					//Experimental rewrite
					else
					{
						//if the mob kill will result in a custom message (after accounting for list shares)
						//then display the appropriate death message
						String killerMobFinalCat = getProcessedMobType(killerMob);
						//DEBUG
						//plugin.getLogger().info("killerMob: " + killerMob + ", killerMobFinalCat: " + killerMobFinalCat);
						if (plugin.getConfig().contains("msg."+killerMobFinalCat))
						{
							//DEBUG
							//plugin.getLogger().info("Config contains msg." + killerMobFinalCat);
							if (plugin.getConfig().isList("msg."+killerMobFinalCat))
							{
								//DEBUG:
								//plugin.getLogger().info("Key " + "msg."+killerMob + " contains a death message list");
								if (ee.getDamager() instanceof Projectile)
								{
									Projectile p = (Projectile)ee.getDamager();
									ProjectileSource pz = p.getShooter();
									if (pz instanceof LivingEntity)
									{
										LivingEntity z = (LivingEntity)pz;
										dispatchDeathMessage(e, getDeathReason(killerMob, e.getEntity().getName(), z));
									}
									else if (pz instanceof BlockProjectileSource) //assume this can only be Dispensers
									{ 
										dispatchDeathMessage(e, getDeathReason(killerMob, e.getEntity().getName(), "Dispenser"));
									}
									else
									{
										//not entirely sure what will cause this case to trigger
										dispatchDeathMessage(e, getDeathReason("default", e.getEntity().getName()));
									}
								}
								else
								{
									LivingEntity z = (LivingEntity)ee.getDamager();
									dispatchDeathMessage(e, getDeathReason(killerMob, e.getEntity().getName(), z));
								}
								return;
							}
							else if (plugin.getConfig().isList("msg."+killerMobFinalCat+".noitem") && plugin.getConfig().isList("msg."+killerMobFinalCat+".item"))
							{
								//DEBUG:
								//plugin.getLogger().info("Key " + "msg."+killerMob + " contains death messages list with possible item reporting");
								LivingEntity z;
								if (ee.getDamager() instanceof Projectile) 
								{
								 //this section of code is primarily to get the living entity to pass into getDeathReason()
								 //note that projectile kills by dispenser cannot be handled in here normally (no reason to have .noitem and .item lists for dispenser projectile kills) 
									Projectile p = (Projectile)ee.getDamager();
									ProjectileSource pz = p.getShooter();
									if (pz instanceof LivingEntity)
									{
										z = (LivingEntity)pz;
									}
									else
									{
										z = null; //this should not happen I think; if code gets here then it should be a LivingEntity barring config mistake 
									}
								}
								else
								{
									z = (LivingEntity)ee.getDamager();
								}
								dispatchDeathMessage(e, getDeathReason(killerMob, e.getEntity().getName(), z, z.getEquipment().getItemInMainHand()));
								return;
							}
						}
						else //otherwise don't dispatch death message and log some debug info to console
						{
							plugin.getLogger().info("Key " + "msg."+killerMobFinalCat + " does not contain death messages");
							//Do not dispatch a death message - vanilla death message will draw attention to what's missing
							//and the line in console will also provide a clue about what is needed
							plugin.getLogger().info("Death reason: " + e.getEntity().getLastDamageCause().getCause());
							plugin.getLogger().info("Last damager: " + ee.getDamager().getType().toString());
						}
					}
					/*
					else if (plugin.getConfig().contains("msg."+killerMob))
					{
						//DEBUG:
						//plugin.getLogger().info("Key " + "msg."+killerMob + " was found in the config");
						if (plugin.getConfig().isList("msg."+killerMob))
						{
							//DEBUG:
							//plugin.getLogger().info("Key " + "msg."+killerMob + " contains a death message list");
							if (ee.getDamager() instanceof Projectile)
							{
								Projectile p = (Projectile)ee.getDamager();
								ProjectileSource pz = p.getShooter();
								if (pz instanceof LivingEntity)
								{
									LivingEntity z = (LivingEntity)pz;
									dispatchDeathMessage(e, getDeathReason(killerMob, e.getEntity().getName(), z));
								}
								else if (pz instanceof BlockProjectileSource) //assume this can only be Dispensers
								{ 
									dispatchDeathMessage(e, getDeathReason(killerMob, e.getEntity().getName(), "Dispenser"));
								}
								else
								{
									//not entirely sure what will cause this case to trigger
									dispatchDeathMessage(e, getDeathReason("default", e.getEntity().getName()));
								}
							}
							else
							{
								LivingEntity z = (LivingEntity)ee.getDamager();
								dispatchDeathMessage(e, getDeathReason(killerMob, e.getEntity().getName(), z));
							}
							return;
						}
						else if (plugin.getConfig().isList("msg."+killerMob+".noitem") && plugin.getConfig().isList("msg."+killerMob+".item"))
						{
							//DEBUG:
							//plugin.getLogger().info("Key " + "msg."+killerMob + " contains death messages list with possible item reporting");
							LivingEntity z;
							if (ee.getDamager() instanceof Projectile) 
							{
							 //this section of code is primarily to get the living entity to pass into getDeathReason()
							 //note that projectile kills by dispenser cannot be handled in here normally (no reason to have .noitem and .item lists for dispenser projectile kills) 
								Projectile p = (Projectile)ee.getDamager();
								ProjectileSource pz = p.getShooter();
								if (pz instanceof LivingEntity)
								{
									z = (LivingEntity)pz;
								}
								else
								{
									z = null; //this should not happen I think; if code gets here then it should be a LivingEntity barring config mistake 
								}
							}
							else
							{
								z = (LivingEntity)ee.getDamager();
							}
							dispatchDeathMessage(e, getDeathReason(killerMob, e.getEntity().getName(), z, z.getEquipment().getItemInMainHand()));
							return;
						}
						else
						{
							plugin.getLogger().info("Config contains msg."+killerMob+" but it is not a String list or configured for item reporting with String lists.\nThat's odd; didn't expect this to print.");
						}
					}
					
					else
					{
						plugin.getLogger().info("Key " + "msg."+killerMob + " does not contain death messages");
						//Do not dispatch a death message - vanilla death message will draw attention to what's missing
						//and the line in console will also provide a clue about what is needed
						plugin.getLogger().info("Death reason: " + e.getEntity().getLastDamageCause().getCause());
						plugin.getLogger().info("Last damager: " + ee.getDamager().getType().toString());
					}*/
				}
			}
			else //handle all non-entity-related deaths here
			{
				EntityDamageEvent.DamageCause lastHit = victim.getLastDamageCause().getCause();

				//DEBUG
				//plugin.getLogger().info("Damage Type: " + victim.getLastDamageCause().getCause());

				if (lastHit.equals(EntityDamageEvent.DamageCause.SUICIDE))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						//suicide kill...shouldn't be obtainable anyway, but Guardian kill is placeholder
					}
					else
					{
						e.setDeathMessage(ChatColor.GOLD + victim.getName() + ChatColor.DARK_AQUA + " was killed by " + ChatColor.YELLOW + "The Guardians");
					}
				}
				//Lightning kills now use this damage cause and are killed by entity Lightning; handle with other entity kills
				//else if (lastHit.equals(EntityDamageEvent.DamageCause.LIGHTNING))
				//{
				//	dispatchDeathMessage(e, getDeathReason("lightning", e.getEntity().getName()));
				//}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) //eg. bed explosion in Nether/End
				{
					dispatchDeathMessage(e, getDeathReason("bed", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.FALLING_BLOCK))
				{
					dispatchDeathMessage(e, getDeathReason("anvil", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.CONTACT))
				{
					//as of 1.14, this can be triggered by either cactus or berry bush, so need to differentiate between them using EntityDamageByBlockEvent
					EntityDamageByBlockEvent ebe = (EntityDamageByBlockEvent)victim.getLastDamageCause();
					String killingBlock = ebe.getDamager().getType().toString().toLowerCase();
					dispatchDeathMessage(e, getDeathReason("contact."+killingBlock, e.getEntity().getName()));
					/*if (killingBlock.equalsIgnoreCase("cactus"))
					{
						dispatchDeathMessage(e, getDeathReason("cactuspoke", e.getEntity().getName()));
					}
					else if (killingBlock.equalsIgnoreCase("sweet_berry_bush"))
					{
						dispatchDeathMessage(e, getDeathReason("bush", e.getEntity().getName()));
					}*/
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.FIRE_TICK) || lastHit.equals(EntityDamageEvent.DamageCause.FIRE))
				{
					dispatchDeathMessage(e, getDeathReason("fire", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.WITHER))
				{
					dispatchDeathMessage(e, getDeathReason("wither.wither", e.getEntity().getName()));
				}
				else //generic handling of remaining not-entity deaths
				{
					dispatchDeathMessage(e, getDeathReason(lastHit.toString().toLowerCase(), e.getEntity().getName()));
				}
			}
		}

		//for not-entity kill
		//Gets a custom death message according to reason and formats it for display
		//note that this is an overloaded method
		//input reason: the String that refers to the category in the config to get a custom death message from
		//    (ie. "msg.<reason>" should exist in the config) 
		//input playerName: the String of the victim player's name  
		//output: the formatted custom death message, with victim name added into the message
		public static String getDeathReason(String reason_raw, String playerName)
		{
			String reason = getProcessedMobType(reason_raw);
			List<String> deathReasonList = plugin.getConfig().getStringList("msg." + reason);
			int index = (int)(Math.random()*deathReasonList.size());
			String s = deathReasonList.get(index);
			s = s.replace(Matcher.quoteReplacement("&p"), playerName);
			s = ChatColor.translateAlternateColorCodes('&', s);
			return s;
		}

		//for entity no-item-in-hand kill
		//Gets a custom death message according to reason and formats it for display
		//note that this is an overloaded method
		//input reason: the String that refers to the category in the config to get a custom death message from
		//    (ie. "msg.<reason>" should exist in the config) 
		//input playerName: the String of the victim player's name 
		//input killerName: the LivingEntity that killed the victim player 
		//output: the formatted custom death message, with victim name and killer name added into the message
		public static String getDeathReason(String reason_raw, String playerName, LivingEntity killerName)
		{
			String reason = getProcessedMobType(reason_raw);
			List<String> deathReasonList = plugin.getConfig().getStringList("msg." + reason);
			int index = (int)(Math.random()*deathReasonList.size());
			String s = deathReasonList.get(index);
			s = s.replace(Matcher.quoteReplacement("&p"), playerName);
			String mobname = getProcessedMobName(killerName);
			s = s.replace("&z", mobname);
			s = ChatColor.translateAlternateColorCodes('&', s);
			return s;
		}

		//for special kills not by entity (eg. arrow from dispenser)
		//Gets a custom death message according to reason and formats it for display
		//note that this is an overloaded method
		//input reason: the String that refers to the category in the config to get a custom death message from
		//    (ie. "msg.<reason>" should exist in the config) 
		//input playerName: the String of the victim player's name 
		//input killerName: the String of the killing entity's name (eg. for dispensers this may just be "Dispenser") 
		//output: the formatted custom death message, with victim name and killer name added into the message
		public static String getDeathReason(String reason_raw, String playerName, String killerName)
		{
			String reason = getProcessedMobType(reason_raw);
			List<String> deathReasonList = plugin.getConfig().getStringList("msg." + reason);
			int index = (int)(Math.random()*deathReasonList.size());
			String s = deathReasonList.get(index);
			s = s.replace(Matcher.quoteReplacement("&p"), playerName);
			s = s.replace("&z", killerName);
			s = ChatColor.translateAlternateColorCodes('&', s);
			return s;
		}

		//Gets a custom death message according to reason and formats it for display
		//this is an overloaded method that is used to handle deaths with possible item reporting
		//(ie. message may show what item was in the mob's main hand)
		//input reason: the String that refers to the category in the config to get a custom death message from
		//    (ie. "msg.<reason>.item" and "msg.<reason>.noitem" should exist in the config) 
		//input playerName: the String of the victim player's name 
		//input killerName: the LivingEntity that killed the victim player
		//input item: the ItemStack held in the killing entity's main hand 
		//output: the formatted custom death message, with victim name, killer name, and possibly item added into the message
		public static String getDeathReason(String reason_raw, String playerName, LivingEntity killerName, ItemStack item)
		{
			String reason = getProcessedMobType(reason_raw);
			//Handle special kills first
			//change this to reference the item list
			//if the item of the mob is in that list, handle it as an item kill
			//otherwise handle it as usual
			
			//DEBUG
			//plugin.getLogger().info("Item involved in kill: " + item.getType().toString().toLowerCase());
			
			//items to be checked for custom item message are in the itemMsg list in the config
			//if this list does not exist, give it a copy from the config file
			if (plugin.itemMsgList == null)
			{
				plugin.itemMsgList = plugin.getConfig().getStringList("itemMsg");
			}
			
			//check if the item involved in this death is in the list
			for (String itemInList: plugin.itemMsgList)
			{
				if (itemInList.equalsIgnoreCase(item.getType().toString())) //if there is a match
				{
					//handle it as an item kill
					if (plugin.getConfig().contains("msg."+item.getType().toString().toLowerCase()))
					{
						//DEBUG
						//plugin.getLogger().info("Item death message was found: " + "msg."+item.getType().toString().toLowerCase());
						//plugin.getLogger().info("Death message");
						List<String> deathItemList = plugin.getConfig().getStringList("msg."+item.getType().toString().toLowerCase());
						int index = (int)(Math.random()*deathItemList.size());
						String s = deathItemList.get(index);
						String itemName = getIndefiniteArticle(item.getType().toString().toLowerCase()) + item.getType().toString().toLowerCase();
						itemName = itemName.replace('_', ' ');
						if (item.hasItemMeta())
						{
							if (item.getItemMeta().hasDisplayName())
							{
								itemName = item.getItemMeta().getDisplayName();
							}
						}
						s = s.replaceAll(Matcher.quoteReplacement("&i"), itemName);
						s = s.replaceAll(Matcher.quoteReplacement("&p"), playerName);
						String mobname = getProcessedMobName(killerName);
						
						s = s.replaceAll(Matcher.quoteReplacement("&z"), mobname);
						s = ChatColor.translateAlternateColorCodes('&', s);
		
						return s;
					}
					break;
				}
			}

			//non-special kills
			List<String> deathReasonList = plugin.getConfig().getStringList("msg." + reason + ".noitem");
			int index = (int)(Math.random()*deathReasonList.size());
			String s = deathReasonList.get(index);

			if (!item.getType().equals(Material.AIR))
			{
				List<String> deathItemList = plugin.getConfig().getStringList("msg." + reason + ".item");
				int index2 = (int)(Math.random()*deathItemList.size());
				String s2 = " " + deathItemList.get(index2);
				//use another tag to specify the with-item phrase, then replace that
				//this replaces the old behavior of simply appending the with-item phrase at the end
				//s = s + " " + s2;
				if (s.contains("&w"))
				{
					s = s.replaceAll(Matcher.quoteReplacement("&w"), s2);
				}
				else //if with-item tag is not present, append at the end as before
				{
					s = s + s2;
				}
				String itemName = getIndefiniteArticle(item.getType().toString().toLowerCase()) + item.getType().toString().toLowerCase();
				itemName = itemName.replace('_', ' ');
				if (item.hasItemMeta())
				{
					if (item.getItemMeta().hasDisplayName())
					{
						itemName = item.getItemMeta().getDisplayName();
					}
				}
				s = s.replaceAll(Matcher.quoteReplacement("&i"), itemName);
				//s = s.replaceAll(Matcher.quoteReplacement("&i"), item);
			}
			else
			{
				s = s.replaceAll(Matcher.quoteReplacement("&w"), "");
			}

			s = s.replaceAll(Matcher.quoteReplacement("&p"), playerName);
			String mobname = getProcessedMobName(killerName);
			s = s.replaceAll(Matcher.quoteReplacement("&z"), mobname);
			s = ChatColor.translateAlternateColorCodes('&', s);
			return s;
		}

		//related to sign drop on death
		//unused
		public static String getSignText1(String reason)
		{
			List<String> deathReasonList = plugin.getConfig().getStringList("signtext." + reason);
			int index = (int)(Math.random()*deathReasonList.size())/2;
			String s = deathReasonList.get(index);
			return s;
		}

		//currently unused? it's just a passthrough method for a String
		public static String getItemName(String material)
		{
			return material;
		}

		//related to sign drop on death
		//Below are methods used for death signs
		//this method currently does nothing
		public static void placeSignFromReason(String reason, Location signpoint, Player p)
		{
			/*if (plugin.getConfig().getBoolean("death-signs"))
			{
				if (signpoint != null)
				{
					//Choose a sign text
					//precondition: the number of lines under .1 and .2 are equal
					List<String> deathLine1 = plugin.getConfig().getStringList("signtext." + reason + ".1");
					List<String> deathLine2 = plugin.getConfig().getStringList("signtext." + reason + ".2");
					int index = (int)(Math.random()*deathLine1.size());
					String s1 = deathLine1.get(index);
					String s2 = deathLine2.get(index);

					int direction = (int)(Math.random()*16); //16 possible directions, so possible data values are 0-15

					signpoint.getBlock().setType(Material.SIGN);
					//signpoint.getBlock().setData((byte)direction);
					Sign s = (Sign)signpoint.getBlock().getState();
					s.setLine(0, p.getName());
					s.setLine(1, s1);
					s.setLine(2, s2);
					s.setLine(3, getGreenDate());

					s.update();
				}
			}
			else*/
				return;
		}

		//related to sign drop on death - this would have been placed on the sign
		//Gets the date and time and reports that in green: MM-DD HH:MM
		//output: String with format "<month>-<day> <hour>:<minute>" and green text
		public static String getGreenDate()
		{
			Calendar c = Calendar.getInstance();
			int month = c.get(Calendar.MONTH) + 1; //January is listed as 0 this way so increment by 1
			String m = "" + month;
			if (m.length() == 1)
			{
				m = "0" + m;
			}
			int day = c.get(Calendar.DAY_OF_MONTH); //this is reported correctly
			String d = "" + day;
			if (d.length() == 1)
			{
				d = "0" + d;
			}
			int hour = c.get(Calendar.HOUR_OF_DAY); //this is reported correctly
			String h = "" + hour;
			if (h.length() == 1)
			{
				h = "0" + h;
			}
			int minute = c.get(Calendar.MINUTE);
			String i = "" + minute;
			if (m.length() == 1)
			{
				m = "0" + m;
			}
			return ChatColor.GREEN + m + "-" + d + " " + h + ":" + i;
		}
		
		//check if a death message is a death by default
		//(vanilla death message for this is "<player> died")
		//input dm: the vanilla death message for this death
		//input name: the name of the player who died
		//output: true if dm is a default death, false otherwise
		public static boolean isDefaultDeath(String dm, String name)
		{
			return dm.equals(name + " died");
		}
		
		//check if a TNT death message string has an attacker name attached or not
		//check this by finding what the first letter of the second word is:
		//"<X> was blown up by <Y>" -> there was an attacker
		//"<X> blew up" -> no attacker
		//note: this expects a particular format (the default TNT death message);
		//if Mojang changes this, this needs to be rewritten.
		//input dm: the vanilla death message for this death
		//input name: the name of the player who died
		//output: true if TNT was set off by another player/mob, false otherwise
		//(note that if the TNT in this death was set off by dispenser this will return false) 
		public static boolean isTNTAttackDeath(String dm, String name)
		{
			if (dm.substring(name.length()+1, name.length()+2).equals("w"))
			{
				return true;
			}
			return false;
		}
		
		//get the name of the attacker if the TNT death message has attacker's name in it
		//"<X> was blown up by <Y>"
		//note: this expects a particular format (the default TNT death message);
		//if Mojang changes this, this needs to be rewritten.
		//input dm: the vanilla death message for this death
		//input name: the name of the player who died
		//output: String of the killer's name (player or mob) in this TNT kill
		public static String getTNTAttackerName(String dm, String name)
		{
			//make sure the string dm really contains the information sought
			if (!isTNTAttackDeath(dm, name))
			{
				return "null";
			}
			//extract the name
			return dm.substring(name.length()+17); //magic number - expects Mojang default TNT death message to work
		}
		
		//choose the correct indefinite article appended with a space
		//input s: the String of the item type to get the corresponding indefinite article
		//output: the indefinite article with a space, to be concatenated in front of String s
		public static String getIndefiniteArticle(String s)
		{
			char c = s.charAt(0);
			//this is a cheap way of coding the distinctions - exceptions are expected to be
			//hardcoded in later if/when they are discovered
			//NOTE: this will not be used for special item kills since the item used is known
			if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u')
			{
				return "an ";
			}
			else
			{
				return "a ";
			}
		}

		/**
		 * If the plugin is configured to send custom death messages, then
		 * update the death message in the specified event.
		 *
		 * Otherwise, simply log the death message.  This method is common code
		 * factored out of {@link BeingListener#onPlayerDeath(PlayerDeathEvent)}.
		 *
		 * @param event the PlayerDeathEvent.
		 * @param message the custom formatted death message.
		 */
		protected void dispatchDeathMessage(PlayerDeathEvent event, String message)
		{
			if (plugin.getConfig().getBoolean("death-msgs"))
			{
				//event.setDeathMessage(message);
				String vanillaMessage = event.getDeathMessage();
				if (plugin.getConfig().getBoolean("log-vanilla-death"))
				{
					//String vanillaMessage = event.getDeathMessage(); moved outside to accommodate vanilla death message setting
					Bukkit.getLogger().info(vanillaMessage);
				}
				
				//altered death message handling to be per-player
				event.setDeathMessage("");
				plugin.getLogger().info(message);
				//first send the death message to console
				
				Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
				Player[] playerList = players.toArray(new Player[players.size()]);
				
				for (Player p: playerList)
				{
					if (!p.hasMetadata("MaskOfFutures.mutedeath"))
					{
						if (p.hasMetadata("MaskOfFutures.oldMsg")) //if tagged with metadata send the vanilla message
						{
							p.sendMessage(vanillaMessage);
						}
						else if (playerIsInList(p.getUniqueId())) //if not tagged but in the list send the vanilla message
						{
							p.setMetadata("MaskOfFutures.oldMsg", new FixedMetadataValue(plugin, "true"));
							p.sendMessage(vanillaMessage);
						}
						else //send the custom message
						{
							p.sendMessage(message);
						}
					}
				}
				
			}
			else
			{
				plugin.getLogger().info(message);
			}
		}
		
		//checks if a UUID of a player is in the oldMsg list
		//input playerUUID: the UUID of the player
		//output: true if the UUID is in the list as a String, false if not
		private boolean playerIsInList(UUID playerUUID)
		{
			if (plugin.getoldMsgPlayersConfig().contains("oldMsg"))
			{
				if (plugin.getoldMsgPlayersConfig().getStringList("oldMsg").contains(playerUUID.toString()))
				{
					return true;
				}
			}
			return false;
		}
		
		//process the mob name string if the mob name happens to be two words
		//input killer: the LivingEntity that killed the player
		//output mobname: the reformatted String of the name of the mob type of killer
		private static String getProcessedMobName(LivingEntity killer)
		{
			String mobname = killer.getType().toString().toLowerCase();
			mobname = (char)(mobname.charAt(0)-32) + mobname.substring(1); //capitalize first letter
			if (killer.getCustomName() != null) //if it is named
			{
				mobname = killer.getCustomName();
			}
			else //if it does not have a custom name
			{
				//Zombie Pigman exception - if the mobname is calculated to be Pig_zombie and it was not nametagged to be that
				if (mobname.equals("Pig_zombie"))
				{
					mobname = "Zombie Pigman";
				}
				else if (killer instanceof Player)  //Player exception, since getCustomName() returns null for player
				{
					Player p = (Player)killer;
					mobname = p.getName();
				}
				else//generic mob type name handling for two word mob type names
				{   //if there is no underscore found, this will finish iterating and do nothing
					for (int i=0; i<mobname.length(); i++)
					{
						if (mobname.charAt(i) == 95) //95 is ASCII decimal for an underscore
						{
							mobname = mobname.substring(0,i) + " " + (char)(mobname.charAt(i+1)-32) + mobname.substring(i+2,mobname.length());
							break;
						}
					}
				}
				/*
				//Zombie Villager exception
				else if (mobname.equals("Zombie_villager"))
				{
					mobname = "Zombie Villager";
				}
				//Wither Skeleton exception
				else if (mobname.equals("Wither_skeleton"))
				{
					mobname = "Wither Skeleton";
				}
				else if (mobname.equals("Magma_cube"))
				{
					mobname = "Magma Cube";
				}
				else if (mobname.equals("Iron_golem"))
				{
					mobname = "Iron Golem";
				}
				else if (mobname.equals("Cave_spider"))
				{
					mobname = "Cave Spider";
				}
				else if (mobname.equals("Ender_dragon"))
				{
					mobname = "Ender Dragon";
				}
				else if (mobname.equals("Polar_bear"))
				{
					mobname = "Polar Bear";
				}
				else if (mobname.equals("Elder_guardian"))
				{
					mobname = "Elder Guardian";
				}
				else if (mobname.equals("Trader_llama"))
				{
					mobname = "Trader Llama";
				}*/
				
			}
			return mobname;
		}
		
		//different variant of mob name processing method to redirect certain
		//mob death messages to others, eg. wither skeleton -> skeleton
		//Input s: the original death message reason (eg. wither_skeleton, zombie, falling_block.chipped_anvil)
		//Output mobname: the name of the mob type whose death message to use given the input mob type name
		private static String getProcessedMobType(String s)
		{
			//by default, pass through the argument death message reason unless there is a list share setting for it
			String mobname = s;
			//redo to use list of strings containing list sharing information
			//if there is at least one list share specified, iterate through it to see if it matches the current death message reason string
			if (plugin.getConfig().contains("listShare"))
			{
				
				for (String share: plugin.getConfig().getStringList("listShare"))
				{
					if (s.equalsIgnoreCase(getShareOrigin(share)))
					{
						mobname = getShareTarget(share);
						//DEBUG
						//plugin.getLogger().info("Death message for " + s + " uses list for " + mobname);
						break;
					}
				}
			}
			
			//use another death message list if list sharing is specified
			/*if (plugin.getConfig().contains("listShare."+s))
			{
				mobname = plugin.getConfig().getString("listShare."+s);
				//DEBUG
				//plugin.getLogger().info("Death message for " + s + " uses list for " + mobname);
			}*/
			return mobname;
		}
		
		//Reformats the killermob string (ie. death message category in config to look for message)
		//if the entity that kills the player is a projectile (eg. arrow, trident, llama spit, shulker bullet)
		//This function outputs the string "<projectile>.<projectile source entity/dispenser>"
		//If for whatever reason the projectile source is neither living entity nor dispenser, outputs "default"
		private static String getFormattedProjectileDeathCat(Projectile p, ProjectileSource s)
		{
			String key;
			if (s instanceof LivingEntity) //mobs that shoot should fall under this
			{
				LivingEntity psle = (LivingEntity)s;
				key = p.getType().toString().toLowerCase() + "." + psle.getType().toString().toLowerCase();
			}
			else if (s instanceof BlockProjectileSource) //this should mostly be for dispensers
			{
				//assume it's from a dispenser
				key = p.getType().toString().toLowerCase() + ".dispenser";
			}
			else //just in case it's something else
			{
				key = "default"; //hoping this will alert players to this case without dumping too much exception data.
				//plugin.getLogger().info("The ProjectileSource "+ s.toString() +" was something other than a LivingEntity or BlockProjectileSource!");
			}
			
			return key;
		}
		
		private static String getShareOrigin(String listShareString)
		{
			//this method assumes that listShareString is a concatenation of death message categories
			//eg. "arrow.stray,arrow.skeleton"
			//this method returns the substring BEFORE the comma, blank string if there is no comma
			int pos = listShareString.indexOf(",");
			if (pos >= 0)
			{
				return listShareString.substring(0,pos);
			}
			else //if it's -1 because there is no comma
			{
				return "";
			}
		}
		
		private static String getShareTarget(String listShareString)
		{
			//this method assumes that listShareString is a concatenation of death message categories
			//eg. "arrow.stray,arrow.skeleton"
			//this method returns the substring AFTER the comma, blank string if there is no comma
			int pos = listShareString.indexOf(",");
			if (pos >= 0)
			{
				return listShareString.substring(pos+1,listShareString.length());
			}
			else //if it's -1 because there is no comma
			{
				return "";
			}
		}
}

