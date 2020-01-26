package com.gmail.buzziespy.MaskOfFutures;

import java.io.File;

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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
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
			//save copy
			plugin.saveDefaultConfig();
		}

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
					/*
					 * NOTE: Use the getDeathReason with the ItemStack parameter if you think a weapon can be
					 * used in the kill.  If you use that, you MUST specify item/noitem in the corresponding
					 * config entry!  Otherwise console will vomit exceptions because it can't find
					 * configkey.noitem and configkey.item in the config.
					 */
					//Zombie kill
					if (ee.getDamager() instanceof Zombie && !(ee.getDamager() instanceof PigZombie))
					{

						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInMainHand();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
						{
							dispatchDeathMessage(e, getDeathReason("zombie", e.getEntity().getName(), z, itemWeapon));
						}
						else if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
						{
							dispatchDeathMessage(e, getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
						}

						//placeSignFromReason("zombie", signpoint, e.getEntity());

					}

					//Skeleton kill
					else if (ee.getDamager() instanceof Skeleton)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInMainHand();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
						{
							dispatchDeathMessage(e, getDeathReason("skeleton", e.getEntity().getName(), z, itemWeapon));
						}
						else if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
						{
							dispatchDeathMessage(e, getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
						}
						//placeSignFromReason("skeleton", signpoint, e.getEntity());
					}

					//Zombie Pigman kill
					else if (ee.getDamager() instanceof PigZombie)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInMainHand();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
						{
							dispatchDeathMessage(e, getDeathReason("pigzombie", e.getEntity().getName(), z, itemWeapon));
						}
						else if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
						{
							dispatchDeathMessage(e, getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
						}
						//placeSignFromReason("pigzombie", signpoint, e.getEntity());
					}

					//Creeper kill
					else if (ee.getDamager() instanceof Creeper)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("creeper", e.getEntity().getName(), z));
						//placeSignFromReason("creeper", signpoint, e.getEntity());
					}
					//Phantom kills
					else if (ee.getDamager() instanceof Phantom)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("phantom", e.getEntity().getName(), z));
					}
					//Anvil kill
					else if (ee.getDamager() instanceof FallingBlock)
					{
						dispatchDeathMessage(e, getDeathReason("anvil", e.getEntity().getName()));
						//placeSignFromReason("anvil", signpoint, e.getEntity());
					}
					//Slime kill
					else if (ee.getDamager() instanceof Slime)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						String killer = (z instanceof MagmaCube) ? "magmacube" : "slime";
						dispatchDeathMessage(e, getDeathReason(killer, e.getEntity().getName(), z));
					}
					//Spider kill
					else if (ee.getDamager() instanceof Spider)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						String killer = (z instanceof CaveSpider) ? "cavespider" : "spider";
						dispatchDeathMessage(e, getDeathReason(killer, e.getEntity().getName(), z));
						//placeSignFromReason("spider", signpoint, e.getEntity());
					}
					//Witch kill
					else if (ee.getDamager() instanceof Witch)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInMainHand();
						dispatchDeathMessage(e, getDeathReason("witch", e.getEntity().getName(), z, itemWeapon));
						//placeSignFromReason("witch", signpoint, e.getEntity());
					}
					//Wolf kill
					else if (ee.getDamager() instanceof Wolf)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("wolf", e.getEntity().getName(), z));
						//placeSignFromReason("wolf", signpoint, e.getEntity());
					}
					//Blaze kill
					else if (ee.getDamager() instanceof Blaze)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("blaze", e.getEntity().getName(), z));
						//placeSignFromReason("blaze", signpoint, e.getEntity());
					}
					//Drowned kill
					else if (ee.getDamager() instanceof Drowned)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInMainHand();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
						{
							dispatchDeathMessage(e, getDeathReason("drowned", e.getEntity().getName(), z, itemWeapon));
						}
						else if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
						{
							dispatchDeathMessage(e, getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
						}

						//placeSignFromReason("drowned", signpoint, e.getEntity());
					}
					//Silverfish kill
					else if (ee.getDamager() instanceof Silverfish)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("silverfish", e.getEntity().getName(), z));
						//placeSignFromReason("silverfish", signpoint, e.getEntity());
					}
					//Iron golem kills
					else if (ee.getDamager() instanceof IronGolem)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("irongolem", e.getEntity().getName(), z));
						//placeSignFromReason("irongolem", signpoint, e.getEntity());
					}
					//Enderman kills
					else if (ee.getDamager() instanceof Enderman)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("enderman", e.getEntity().getName(), z));
						}
						else
						{
							e.setDeathMessage(getDeathReason("enderman", e.getEntity().getName(), z));
						}
					}
					//Enderdragon kills
					else if (ee.getDamager() instanceof EnderDragon)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("enderdragon", e.getEntity().getName(), z));
					}
					//Rabbit kills
					else if (ee.getDamager() instanceof Rabbit)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("rabbit", e.getEntity().getName(), z));
					}
					//Endermite kills
					else if (ee.getDamager() instanceof Endermite)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("endermite", e.getEntity().getName(), z));
					}
					//Polar Bear kills
					else if (ee.getDamager() instanceof PolarBear)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("polarbear", e.getEntity().getName(), z));
					}
					//Shulker kills
					else if (ee.getDamager() instanceof ShulkerBullet)
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
					}
					//Guardian kill
					if (ee.getDamager() instanceof Guardian)
					{

						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInMainHand();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
						{
							dispatchDeathMessage(e, getDeathReason("guardian", e.getEntity().getName(), z));
						}
						else if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
						{
							dispatchDeathMessage(e, getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
						}
						//placeSignFromReason("zombie", signpoint, e.getEntity());
					}
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
					else if (ee.getDamager() instanceof WitherSkull)
					{
						WitherSkull tp = (WitherSkull)ee.getDamager();
						//Given the uncertainty over Bukkit's future I will assume deprecated methods are fair game.
						//NOTE: Casting projectile shooters as LivingEntities no longer appears to work
						//LivingEntity le = (LivingEntity)tp.getShooter();
						ProjectileSource le = (ProjectileSource)tp.getShooter();
						//plugin.getLogger().info("Projectile source: " + le.toString());
						if (le instanceof Wither)
						{
							Wither w = (Wither)le;
							dispatchDeathMessage(e, getDeathReason("wither.kill", e.getEntity().getName(), w));
						}
					}
					//arrow kill - note that in 1.13 Trident inherits Arrow, so need to check that the arrow is not a trident
					else if (ee.getDamager() instanceof Arrow && !(ee.getDamager() instanceof Trident))
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
							else if (le instanceof Illusioner)
							{
								dispatchDeathMessage(e, getDeathReason("arrow.illusioner", e.getEntity().getName(), le, itemWeapon));
							}
						}
						
					}
					else if (ee.getDamager() instanceof ThrownPotion)
					{
						//perhaps change this based on distance of shooter?
						ThrownPotion tp = (ThrownPotion)ee.getDamager();
						//Given the uncertainty over Bukkit's future I will assume deprecated methods are fair game.
						//NOTE: Casting projectile shooters as LivingEntities no longer appears to work
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
					}
					else if (ee.getDamager() instanceof LargeFireball)
					{
						//perhaps change this based on distance of shooter?
						LargeFireball tp = (LargeFireball)ee.getDamager();
						//Given the uncertainty over Bukkit's future I will assume deprecated methods are fair game.
						LivingEntity le = (LivingEntity)tp.getShooter();
						if (le instanceof Ghast) //almost always is but just in case
						{
							dispatchDeathMessage(e, getDeathReason("fireball.ghast", e.getEntity().getName(), le));
						}

					}
					else if (ee.getDamager() instanceof SmallFireball)
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
					}
					//overzealous pearl suicides
					else if (ee.getDamager() instanceof EnderPearl)
					{
						dispatchDeathMessage(e, getDeathReason("pearl", e.getEntity().getName()));
					}
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
					//Vex kill
					else if (ee.getDamager() instanceof Vex)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInMainHand();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
						{
							dispatchDeathMessage(e, getDeathReason("vex", e.getEntity().getName(), z, itemWeapon));
						}
					}
					//Ravager kills
					else if (ee.getDamager() instanceof Ravager)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("ravager", e.getEntity().getName(), z));
					}
					//Vindicator kill - assuming it cannot wear thorns armor
					else if (ee.getDamager() instanceof Vindicator)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInMainHand();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
						{
							dispatchDeathMessage(e, getDeathReason("vindicator", e.getEntity().getName(), z, itemWeapon));
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
					else if (ee.getDamager() instanceof Trident)
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
							LivingEntity le = (LivingEntity)trident.getShooter();
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
						
					}
					//Pufferfish kills
					else if (ee.getDamager() instanceof PufferFish)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("pufferfish", e.getEntity().getName(), z));
					}
					//Panda kills (aggressive only)
					else if (ee.getDamager() instanceof Panda)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("panda", e.getEntity().getName(), z));
					}
					//Dolphin kills
					else if (ee.getDamager() instanceof Dolphin)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("dolphin", e.getEntity().getName(), z));
					}
					//Bee kills
					else if (ee.getDamager() instanceof Bee)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						dispatchDeathMessage(e, getDeathReason("bee", e.getEntity().getName(), z));
					}
					//Firework kills
					else if (ee.getDamager() instanceof Firework)
					{
							dispatchDeathMessage(e, getDeathReason("firework", e.getEntity().getName()));
					}
					//Llama kill
					else if (ee.getDamager() instanceof LlamaSpit)
					{
						LlamaSpit tp = (LlamaSpit)ee.getDamager();
						ProjectileSource le = (ProjectileSource)tp.getShooter();
						//plugin.getLogger().info("Projectile source: " + le.toString());
						if (le instanceof Llama)
						{
							Llama w = (Llama)le;
							dispatchDeathMessage(e, getDeathReason("llama", e.getEntity().getName(), w));
						}
					}
					//Ender Crystal kills
					else if (ee.getDamager() instanceof EnderCrystal)
					{
							dispatchDeathMessage(e, getDeathReason("endercrystal", e.getEntity().getName()));
					}
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
				//Lightning kills do not use this
				//else if (lastHit.equals(EntityDamageEvent.DamageCause.LIGHTNING))
				//{
				//	plugin.getLogger()info(getDeathReason( ;//e.setMessage(ChatColor.GOLD + victim.getName() + ChatColor.DARK_AQUA + " was zapped by Lightning");
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
					String killingBlock = ebe.getDamager().getType().toString();
					if (killingBlock.equalsIgnoreCase("cactus"))
					{
						dispatchDeathMessage(e, getDeathReason("cactuspoke", e.getEntity().getName()));
					}
					else if (killingBlock.equalsIgnoreCase("sweet_berry_bush"))
					{
						dispatchDeathMessage(e, getDeathReason("bush", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.FIRE_TICK) || lastHit.equals(EntityDamageEvent.DamageCause.FIRE))
				{
					dispatchDeathMessage(e, getDeathReason("fire", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.WITHER))
				{
					dispatchDeathMessage(e, getDeathReason("wither.wither", e.getEntity().getName()));
				}
				else
				{
					dispatchDeathMessage(e, getDeathReason(lastHit.toString().toLowerCase(), e.getEntity().getName()));
				}
				
				//Commented out some of the death-reason-from-damage-cause code 
				//to try to simplify and make it more robust to updates
				/*else if (lastHit.equals(EntityDamageEvent.DamageCause.FALL))
				{
					dispatchDeathMessage(e, getDeathReason("fall", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.LAVA))
				{
					dispatchDeathMessage(e, getDeathReason("lava", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.SUFFOCATION))
				{
					dispatchDeathMessage(e, getDeathReason("suffocation", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.DROWNING))
				{
					dispatchDeathMessage(e, getDeathReason("drowning", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.STARVATION))
				{
					dispatchDeathMessage(e, getDeathReason("starvation", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.VOID))
				{
					dispatchDeathMessage(e, getDeathReason("void", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.MAGIC))
				{
					dispatchDeathMessage(e, getDeathReason("magic", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.FLY_INTO_WALL))
				{
					dispatchDeathMessage(e, getDeathReason("fly_into_wall", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.HOT_FLOOR))
				{
					dispatchDeathMessage(e, getDeathReason("hot_floor", e.getEntity().getName()));
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION))
				{
					// Not currently handled - this fires on kill by Ender Crystal; this is handled with other entity kills above
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.CRAMMING))
				{
					dispatchDeathMessage(e, getDeathReason("cramming", e.getEntity().getName()));
				}*/
			}
		}

		//for not-entity kill
		public static String getDeathReason(String reason, String playerName)
		{
			List<String> deathReasonList = plugin.getConfig().getStringList("msg." + reason);
			int index = (int)(Math.random()*deathReasonList.size());
			String s = deathReasonList.get(index);
			s = s.replace(Matcher.quoteReplacement("&p"), playerName);
			s = ChatColor.translateAlternateColorCodes('&', s);
			return s;
		}

		//for no-item-in-hand kill
		public static String getDeathReason(String reason, String playerName, LivingEntity killerName)
		{
			List<String> deathReasonList = plugin.getConfig().getStringList("msg." + reason);
			int index = (int)(Math.random()*deathReasonList.size());
			String s = deathReasonList.get(index);
			s = s.replace(Matcher.quoteReplacement("&p"), playerName);
			String mobname = killerName.getType().toString().toLowerCase();
			mobname = (char)(mobname.charAt(0)-32) + mobname.substring(1);

			//Some mob names are reported with underscores - need to modify the mobname so they
			//don't actually appear unless intentionally named that way (eg. with nametag)
			if (mobname.equals("Magma_cube") && killerName.getCustomName() == null)
			{
				mobname = "Magma Cube";
			}
			else if (mobname.equals("Iron_golem") && killerName.getCustomName() == null)
			{
				mobname = "Iron Golem";
			}
			else if (mobname.equals("Cave_spider") && killerName.getCustomName() == null)
			{
				mobname = "Cave Spider";
			}
			else if (mobname.equals("Ender_dragon") && killerName.getCustomName() == null)
			{
				mobname = "Ender Dragon";
			}
			else if (mobname.equals("Polar_bear") && killerName.getCustomName() == null)
			{
				mobname = "Polar Bear";
			}
			else if (mobname.equals("Elder_guardian") && killerName.getCustomName() == null)
			{
				mobname = "Elder Guardian";
			}
			else if (mobname.equals("Trader_llama") && killerName.getCustomName() == null)
			{
				mobname = "Trader Llama";
			}
			if (killerName.getCustomName() != null)
			{
				mobname = killerName.getCustomName();
			}
			if (killerName instanceof Player)  //Player exception, since getCustomName() returns null for player
			{
				Player p = (Player)killerName;
				mobname = p.getName();
			}
			s = s.replace("&z", mobname);
			s = ChatColor.translateAlternateColorCodes('&', s);
			return s;
		}

		//for special kills not by entity (eg. arrow from dispenser)
		public static String getDeathReason(String reason, String playerName, String killerName)
		{
			List<String> deathReasonList = plugin.getConfig().getStringList("msg." + reason);
			int index = (int)(Math.random()*deathReasonList.size());
			String s = deathReasonList.get(index);
			s = s.replace(Matcher.quoteReplacement("&p"), playerName);
			s = s.replace("&z", killerName);
			s = ChatColor.translateAlternateColorCodes('&', s);
			return s;
		}

		//should also handle item kills
		public static String getDeathReason(String reason, String playerName, LivingEntity killerName, ItemStack item)
		{
			//Handle special kills first
			//Code gets repetitive here - feel free to condense!
			if (item.getType().equals(Material.FEATHER))
			{
				List<String> deathItemList = plugin.getConfig().getStringList("msg.feather");
				int index = (int)(Math.random()*deathItemList.size());
				String s = deathItemList.get(index);
				String itemName = "a " + item.getType().toString().toLowerCase();
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
				String mobname = killerName.getType().toString().toLowerCase();
				mobname = (char)(mobname.charAt(0)-32) + mobname.substring(1);
				if (killerName.getCustomName() != null)
				{
					mobname = killerName.getCustomName();
				}

				//Zombie Pigman exception - if the mobname is calculated to be Pig_zombie and it was not nametagged to be that
				if (mobname.equals("Pig_zombie") && killerName.getCustomName() == null)
				{
					mobname = "Zombie Pigman";
				}
				//Zombie Villager exception
				if (mobname.equals("Zombie_villager") && killerName.getCustomName() == null)
				{
					mobname = "Zombie Villager";
				}
				//Wither Skeleton exception
				if (mobname.equals("Wither_skeleton") && killerName.getCustomName() == null)
				{
					mobname = "Wither Skeleton";
				}
				if (killerName instanceof Player)  //Player exception, since getCustomName() returns null for player
				{
					Player p = (Player)killerName;
					mobname = p.getName();
				}


				s = s.replaceAll(Matcher.quoteReplacement("&z"), mobname);
				s = ChatColor.translateAlternateColorCodes('&', s);

				return s;
			}

			else if (item.getType().equals(Material.COD))
			{
				List<String> deathItemList = plugin.getConfig().getStringList("msg.rawfish");
				int index = (int)(Math.random()*deathItemList.size());
				String s = deathItemList.get(index);
				String itemName = "a " + item.getType().toString().toLowerCase();
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
				String mobname = killerName.getType().toString().toLowerCase();
				mobname = (char)(mobname.charAt(0)-32) + mobname.substring(1);
				if (killerName.getCustomName() != null)
				{
					mobname = killerName.getCustomName();
				}

				//Zombie Pigman exception - if the mobname is calculated to be Pig_zombie and it was not nametagged to be that
				if (mobname.equals("Pig_zombie") && killerName.getCustomName() == null)
				{
					mobname = "Zombie Pigman";
				}
				//Zombie Villager exception
				if (mobname.equals("Zombie_villager") && killerName.getCustomName() == null)
				{
					mobname = "Zombie Villager";
				}
				//Wither Skeleton exception
				if (mobname.equals("Wither_skeleton") && killerName.getCustomName() == null)
				{
					mobname = "Wither Skeleton";
				}
				if (killerName instanceof Player)
				{
					Player p = (Player)killerName;
					mobname = p.getName();
				}

				s = s.replaceAll(Matcher.quoteReplacement("&z"), mobname);
				s = ChatColor.translateAlternateColorCodes('&', s);

				return s;
			}

			else if (item.getType().equals(Material.GLOWSTONE) || item.getType().equals(Material.GLOWSTONE_DUST))
			{
				List<String> deathItemList = plugin.getConfig().getStringList("msg.glowstone");
				int index = (int)(Math.random()*deathItemList.size());
				String s = deathItemList.get(index);
				String itemName = "a " + item.getType().toString().toLowerCase();
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
				String mobname = killerName.getType().toString().toLowerCase();
				mobname = (char)(mobname.charAt(0)-32) + mobname.substring(1);
				if (killerName.getCustomName() != null)
				{
					mobname = killerName.getCustomName();
				}

				//Zombie Pigman exception - if the mobname is calculated to be Pig_zombie and it was not nametagged to be that
				if (mobname.equals("Pig_zombie") && killerName.getCustomName() == null)
				{
					mobname = "Zombie Pigman";
				}
				//Zombie Villager exception
				if (mobname.equals("Zombie_villager") && killerName.getCustomName() == null)
				{
					mobname = "Zombie Villager";
				}
				//Wither Skeleton exception
				if (mobname.equals("Wither_skeleton") && killerName.getCustomName() == null)
				{
					mobname = "Wither Skeleton";
				}
				if (killerName instanceof Player)
				{
					Player p = (Player)killerName;
					mobname = p.getName();
				}

				s = s.replaceAll(Matcher.quoteReplacement("&z"), mobname);
				s = ChatColor.translateAlternateColorCodes('&', s);

				return s;
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

			//System.out.println(s);
			s = s.replaceAll(Matcher.quoteReplacement("&p"), playerName);
			String mobname = killerName.getType().toString().toLowerCase();
			mobname = (char)(mobname.charAt(0)-32) + mobname.substring(1);
			if (killerName.getCustomName() != null)
			{
				mobname = killerName.getCustomName();
			}

			//Zombie Pigman exception - if the mobname is calculated to be Pig_zombie and it was not nametagged to be that
			if (mobname.equals("Pig_zombie") && killerName.getCustomName() == null)
			{
				mobname = "Zombie Pigman";
			}
			//Zombie Villager exception
			if (mobname.equals("Zombie_villager") && killerName.getCustomName() == null)
			{
				mobname = "Zombie Villager";
			}
			//Wither Skeleton exception
			if (mobname.equals("Wither_skeleton") && killerName.getCustomName() == null)
			{
				mobname = "Wither Skeleton";
			}

			if (killerName instanceof Player)
			{
				Player p = (Player)killerName;
				mobname = p.getName();
			}

			s = s.replaceAll(Matcher.quoteReplacement("&z"), mobname);
			s = ChatColor.translateAlternateColorCodes('&', s);
			return s;
		}

		//related to sign drop on death
		public static String getSignText1(String reason)
		{
			List<String> deathReasonList = plugin.getConfig().getStringList("signtext." + reason);
			int index = (int)(Math.random()*deathReasonList.size())/2;
			String s = deathReasonList.get(index);
			return s;
		}

		public static String getItemName(String material)
		{
			return material;
		}

		//related to sign drop on death
		//Below are methods used for death signs
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
		
		public static boolean isDefaultDeath(String dm, String name)
		{
			return dm.equals(name + " died");
		}
		
		//check if a TNT death message string has an attacker name attached or not
		//check this by finding what the first letter of the second word is:
		//"<X> was blown up by <Y>" -> there was an attacker
		//"<X> blew up" -> no attacker
		public static boolean isTNTAttackDeath(String dm, String name)
		{
			//currently unable to get the player that set off the killing TNT,
			//so print the vanilla death message in console instead
			//in a future update, can look into parsing the vanilla death message for
			//TNT kills made by someone other than the victim to get the killer for custom message
			//plugin.getServer().getLogger().info("TNT vanilla death message is: " + dm);
			//Expecting either the TNT death message will look like: "<X> blew up" or "<X> was blown up by <Y>"
			//messages can be distinguished by what the second word in the sentence is
			//plugin.getServer().getLogger().info("First letter of second word: " + dm.substring(name.length()+1, name.length()+2));
			if (dm.substring(name.length()+1, name.length()+2).equals("w"))
			{
				return true;
			}
			return false;
		}
		
		//get the name of the attacker if the TNT death message has attacker's name in it
		//"<X> was blown up by <Y>"
		public static String getTNTAttackerName(String dm, String name)
		{
			//make sure the string dm really contains the information sought
			if (!isTNTAttackDeath(dm, name))
			{
				return "null";
			}
			//extract the name
			return dm.substring(name.length()+17);
		}
		
		//choose the correct indefinite article appended with a space
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
}

