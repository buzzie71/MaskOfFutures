package com.gmail.buzziespy.MaskOfFutures;

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
import java.util.List;
import java.util.regex.Matcher;

import nu.nerd.modmode.ModMode;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Horse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Spider;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
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
			plugin.getLogger().info("EntityExplodeEvent! " + e.getEntity().getName().toString());
			if (plugin.getConfig().getBoolean("brick-dropping") && e.getEntityType().equals(EntityType.WITHER))
			{
				ItemStack woolbrick = new ItemStack(Material.CLAY_BRICK, 1);
				ItemMeta woolbrickInfo = woolbrick.getItemMeta();
				List<String> bricklore = new ArrayList<String>(); //not sure how to optimize this
				
				
				Player[] playerList = (Player[]) plugin.getServer().getOnlinePlayers().toArray();
				
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
					
					log += p.getName();
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
		
		//related to sign drop on death
		@EventHandler
		public void onSignBreak(BlockBreakEvent e)
		{
			//checking for signs attached to floor for now since death signs are by code sign posts (see onPlayerDeath())
			if (e.getBlock().getType().equals(Material.SIGN_POST))
			{
				Sign s = (Sign)e.getBlock().getState();
				if (s.getLine(3).startsWith("&a"))
				{
					e.setCancelled(true);
					e.getBlock().setType(Material.AIR);
				}
			}
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
				plugin.getLogger().info("Death reason: " + e.getEntity().getLastDamageCause().getCause());
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
			//Handle zapping by TestPlugin (personal project, not a nerd plugin) first
			if (victim.hasMetadata("TestPlugin.lightningKill"))
			{
				//e.setMessage(ChatColor.GOLD + victim.getName() + ChatColor.DARK_AQUA + " was zapped by " + ChatColor.RED + victim.getMetadata("TestPlugin.lightningKill").get(0).asString() + ChatColor.DARK_AQUA);
			}
			//Handle death by drinking in MoreBeverages (personal project, not a nerd plugin)
			else if (victim.hasMetadata("MoreBeverages.drunk"))
			{
				//e.setMessage(ChatColor.GOLD + victim.getName() + ChatColor.DARK_AQUA + " had a bit too much to drink");
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
					ItemStack itemWeapon = z.getEquipment().getItemInHand();
					if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
					{
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("player", e.getEntity().getName(), z, itemWeapon));
						}
						else
						{
							e.setDeathMessage(getDeathReason("player", e.getEntity().getName(), z, itemWeapon));
						}
					}
					else if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
					{
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
						}
						else
						{
							e.setDeathMessage(getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
						}
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
						ItemStack itemWeapon = z.getEquipment().getItemInHand();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("zombie", e.getEntity().getName(), z, itemWeapon));
							}
							else
							{
								e.setDeathMessage(getDeathReason("zombie", e.getEntity().getName(), z, itemWeapon));
							}
						}
						else if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
							}
							else
							{
								e.setDeathMessage(getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
							}
						}
						
						placeSignFromReason("zombie", signpoint, e.getEntity());
						
					}
			
					//Skeleton kill
					else if (ee.getDamager() instanceof Skeleton)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInHand();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("skeleton", e.getEntity().getName(), z, itemWeapon));
							}
							else
							{
								e.setDeathMessage(getDeathReason("skeleton", e.getEntity().getName(), z, itemWeapon));
							}
						}
						else if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
							}
							else
							{
								e.setDeathMessage(getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
							}
						}
						placeSignFromReason("skeleton", signpoint, e.getEntity());
					}
					
					//Zombie Pigman kill
					else if (ee.getDamager() instanceof PigZombie)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInHand();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("pigzombie", e.getEntity().getName(), z, itemWeapon));
							}
							else
							{
								e.setDeathMessage(getDeathReason("pigzombie", e.getEntity().getName(), z, itemWeapon));
							}
						}
						else if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
							}
							else
							{
								e.setDeathMessage(getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
							}
						}
						placeSignFromReason("pigzombie", signpoint, e.getEntity());
					}
					
					
					
					//Creeper kill
					else if (ee.getDamager() instanceof Creeper)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("creeper", e.getEntity().getName(), z));
						}
						else
						{
							e.setDeathMessage(getDeathReason("creeper", e.getEntity().getName(), z));
						}
						placeSignFromReason("creeper", signpoint, e.getEntity());
					}
					//Anvil kill
					else if (ee.getDamager() instanceof FallingBlock)
					{
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("anvil", e.getEntity().getName()));
						}
						else
						{
							e.setDeathMessage(getDeathReason("anvil", e.getEntity().getName()));
						}
						placeSignFromReason("anvil", signpoint, e.getEntity());
					}
					//Slime kill
					else if (ee.getDamager() instanceof Slime)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (z instanceof MagmaCube)
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("magmacube", e.getEntity().getName(), z));
							}
							else
							{
								e.setDeathMessage(getDeathReason("magmacube", e.getEntity().getName(), z));
							}
						}
						else
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("slime", e.getEntity().getName(), z));
							}
							else
							{
								e.setDeathMessage(getDeathReason("slime", e.getEntity().getName(), z));
							}
						}
					}
					//Spider kill
					else if (ee.getDamager() instanceof Spider)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (z instanceof CaveSpider)
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("cavespider", e.getEntity().getName(), z));
							}
							else
							{
								e.setDeathMessage(getDeathReason("cavespider", e.getEntity().getName(), z));
							}
						}
						else //normal spider
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("spider", e.getEntity().getName(), z));
							}
							else
							{
								e.setDeathMessage(getDeathReason("spider", e.getEntity().getName(), z));
							}
						}
						placeSignFromReason("spider", signpoint, e.getEntity());
					}
					//Witch kill
					else if (ee.getDamager() instanceof Witch)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInHand();
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("witch", e.getEntity().getName(), z, itemWeapon));
						}
						else
						{
							e.setDeathMessage(getDeathReason("witch", e.getEntity().getName(), z, itemWeapon));
						}
						placeSignFromReason("witch", signpoint, e.getEntity());
					}
					//Wolf kill
					else if (ee.getDamager() instanceof Wolf)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("wolf", e.getEntity().getName(), z));
						}
						else
						{
							e.setDeathMessage(getDeathReason("wolf", e.getEntity().getName(), z));
						}
						placeSignFromReason("wolf", signpoint, e.getEntity());
					}
					//Blaze kill
					else if (ee.getDamager() instanceof Blaze)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("blaze", e.getEntity().getName(), z));
						}
						else
						{
							e.setDeathMessage(getDeathReason("blaze", e.getEntity().getName(), z));
						}
						placeSignFromReason("blaze", signpoint, e.getEntity());
					}
					//Silverfish kill
					else if (ee.getDamager() instanceof Silverfish)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("silverfish", e.getEntity().getName(), z));
						}
						else
						{
							e.setDeathMessage(getDeathReason("silverfish", e.getEntity().getName(), z));
						}
						placeSignFromReason("silverfish", signpoint, e.getEntity());
					}
					//Iron golem kills
					else if (ee.getDamager() instanceof IronGolem)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("irongolem", e.getEntity().getName(), z));
						}
						else
						{
							e.setDeathMessage(getDeathReason("irongolem", e.getEntity().getName(), z));
						}
						placeSignFromReason("silverfish", signpoint, e.getEntity());
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
					//Enderman kills
					else if (ee.getDamager() instanceof EnderDragon)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("enderdragon", e.getEntity().getName(), z));
						}
						else
						{
							e.setDeathMessage(getDeathReason("enderdragon", e.getEntity().getName(), z));
						}
					}
					//Rabbit kills
					else if (ee.getDamager() instanceof Rabbit)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("rabbit", e.getEntity().getName(), z));
						}
						else
						{
							e.setDeathMessage(getDeathReason("rabbit", e.getEntity().getName(), z));
						}
					}
					//Rabbit kills
					else if (ee.getDamager() instanceof Endermite)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("endermite", e.getEntity().getName(), z));
						}
						else
						{
							e.setDeathMessage(getDeathReason("endermite", e.getEntity().getName(), z));
						}
					}
					//Guardian kill
					if (ee.getDamager() instanceof Guardian)
					{
						
						LivingEntity z = (LivingEntity)ee.getDamager();
						ItemStack itemWeapon = z.getEquipment().getItemInHand();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("guardian", e.getEntity().getName(), z));
							}
							else
							{
								e.setDeathMessage(getDeathReason("guardian", e.getEntity().getName(), z));
							}
						}
						else if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.THORNS))
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
							}
							else
							{
								e.setDeathMessage(getDeathReason("thorns", e.getEntity().getName(), z, itemWeapon));
							}
						}
						
						//placeSignFromReason("zombie", signpoint, e.getEntity());
						
					}
					//Wither kill
					else if (ee.getDamager() instanceof Wither)
					{
						LivingEntity z = (LivingEntity)ee.getDamager();
						if (victim.getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION))
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("wither.explosion", e.getEntity().getName(), z));
							}
							else
							{
								e.setDeathMessage(getDeathReason("wither.explosion", e.getEntity().getName(), z));
							}
						}
						else
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("wither.kill", e.getEntity().getName(), z));
							}
							else
							{
								e.setDeathMessage(getDeathReason("wither.kill", e.getEntity().getName(), z));
							}
						}
					}
					//arrow kill
					else if (ee.getDamager() instanceof Arrow)
					{
						//perhaps change this based on distance of shooter?
						Arrow arrow = (Arrow)ee.getDamager();
						if (arrow.getShooter() != null) //if an entity fired this arrow
						{
							//Given the uncertainty over Bukkit's future I will assume deprecated methods are fair game.
							LivingEntity le = (LivingEntity)arrow.getShooter();
							if (le instanceof Skeleton)
							{
								ItemStack itemWeapon = le.getEquipment().getItemInHand();
								if (!plugin.getConfig().getBoolean("death-msgs"))
								{
									plugin.getLogger().info(getDeathReason("arrow.skeleton", e.getEntity().getName(), le, itemWeapon));
								}
								else
								{
									e.setDeathMessage(getDeathReason("arrow.skeleton", e.getEntity().getName(), le, itemWeapon));
								}
							}
							else if (le instanceof Player)
							{
								ItemStack itemWeapon = le.getEquipment().getItemInHand();
								if (!plugin.getConfig().getBoolean("death-msgs"))
								{
									plugin.getLogger().info(getDeathReason("arrow.player", e.getEntity().getName(), le, itemWeapon));
								}
								else
								{
									e.setDeathMessage(getDeathReason("arrow.player", e.getEntity().getName(), le, itemWeapon));
								}
							}
						}
						else
						{
							//most likely it came from a dispenser?
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("arrow.dispenser", e.getEntity().getName(), "Dispenser"));
							}
							else
							{
								e.setDeathMessage(getDeathReason("arrow.dispenser", e.getEntity().getName(), "Dispenser"));
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
						plugin.getLogger().info("Projectile source: " + le.toString());
						if (le instanceof Witch)
						{
							Witch w = (Witch)le;
							ItemStack itemWeapon = w.getEquipment().getItemInHand();
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("potion.witch", e.getEntity().getName(), w, itemWeapon));
							}
							else
							{
								e.setDeathMessage(getDeathReason("potion.witch", e.getEntity().getName(), w, itemWeapon));
							}
						}
						else if (le instanceof Player)
						{
							Player p = (Player)le;
							ItemStack itemWeapon = p.getEquipment().getItemInHand();
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("potion.player", e.getEntity().getName(), p, itemWeapon));
							}
							else
							{
								e.setDeathMessage(getDeathReason("potion.player", e.getEntity().getName(), p, itemWeapon));
							}
						}
						else if (le instanceof ThrownPotion)
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("potion.dispenser", e.getEntity().getName(), "Dispenser"));
							}
							else
							{
								e.setDeathMessage(getDeathReason("potion.dispenser", e.getEntity().getName(), "Dispenser"));
							}
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
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("fireball.ghast", e.getEntity().getName(), le));
							}
							else
							{
								e.setDeathMessage(getDeathReason("fireball.ghast", e.getEntity().getName(), le));
							}
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
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("fireball.blaze", e.getEntity().getName(), (Blaze)le));
							}
							else
							{
								e.setDeathMessage(getDeathReason("fireball.blaze", e.getEntity().getName(), (Blaze)le));
							}
						}
						//need handling for dispensed small fireballs (fire charges)
						else
						{
							if (!plugin.getConfig().getBoolean("death-msgs"))
							{
								plugin.getLogger().info(getDeathReason("fireball.dispenser", e.getEntity().getName()));
							}
							else
							{
								e.setDeathMessage(getDeathReason("fireball.dispenser", e.getEntity().getName()));
							}
						}
					}
					//overzealous pearl suicides
					else if (ee.getDamager() instanceof EnderPearl)
					{
						if (!plugin.getConfig().getBoolean("death-msgs"))
						{
							plugin.getLogger().info(getDeathReason("pearl", e.getEntity().getName()));
						}
						else
						{
							e.setDeathMessage(getDeathReason("pearl", e.getEntity().getName()));
						}
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
				else if (lastHit.equals(EntityDamageEvent.DamageCause.FALLING_BLOCK))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						plugin.getLogger().info(getDeathReason("anvil", e.getEntity().getName()));
					}
					else
					{
						e.setDeathMessage(getDeathReason("anvil", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.FALL))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						plugin.getLogger().info(getDeathReason("fall", e.getEntity().getName()));
					}
					else
					{
						e.setDeathMessage(getDeathReason("fall", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.LAVA))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						plugin.getLogger().info(getDeathReason("lava", e.getEntity().getName()));
					}
					else
					{
						e.setDeathMessage(getDeathReason("lava", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.CONTACT))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						plugin.getLogger().info(getDeathReason("cactuspoke", e.getEntity().getName()));
					}
					else
					{
						e.setDeathMessage(getDeathReason("cactuspoke", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.FIRE_TICK) || lastHit.equals(EntityDamageEvent.DamageCause.FIRE))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						plugin.getLogger().info(getDeathReason("fire", e.getEntity().getName()));
					}
					else
					{
						e.setDeathMessage(getDeathReason("fire", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.WITHER))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						plugin.getLogger().info(getDeathReason("wither.wither", e.getEntity().getName()));
					}
					else
					{
						e.setDeathMessage(getDeathReason("wither.wither", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.SUFFOCATION))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						plugin.getLogger().info(getDeathReason("suffocation", e.getEntity().getName()));
					}
					else
					{
						e.setDeathMessage(getDeathReason("suffocation", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.DROWNING))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						plugin.getLogger().info(getDeathReason("drowning", e.getEntity().getName()));
					}
					else
					{
						e.setDeathMessage(getDeathReason("drowning", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.STARVATION))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						plugin.getLogger().info(getDeathReason("starvation", e.getEntity().getName()));
					}
					else
					{
						e.setDeathMessage(getDeathReason("starvation", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.VOID))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						plugin.getLogger().info(getDeathReason("void", e.getEntity().getName()));
					}
					else
					{
						e.setDeathMessage(getDeathReason("void", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.MAGIC))
				{
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						plugin.getLogger().info(getDeathReason("magic", e.getEntity().getName()));
					}
					else
					{
						e.setDeathMessage(getDeathReason("magic", e.getEntity().getName()));
					}
				}
				else if (lastHit.equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION))
				{
					//if (e.getEntity())
					if (!plugin.getConfig().getBoolean("death-msgs"))
					{
						//plugin.getLogger().info(getDeathReason("magic", e.getEntity().getName()));
					}
					else
					{
						//e.setDeathMessage(getDeathReason("magic", e.getEntity().getName()));
					}
				}
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
			if (killerName.getCustomName() != null)
			{
				mobname = killerName.getCustomName();
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
				if (killerName instanceof Player)  //Player exception, since getCustomName() returns null for player
				{
					Player p = (Player)killerName;
					mobname = p.getName();
				}
				
				
				s = s.replaceAll(Matcher.quoteReplacement("&z"), mobname);
				s = ChatColor.translateAlternateColorCodes('&', s);
				
				return s;
			}
			
			else if (item.getType().equals(Material.RAW_FISH))
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
				String s2 = deathItemList.get(index2);
				s = s + " " + s2;
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
			if (plugin.getConfig().getBoolean("death-signs"))
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
					
					signpoint.getBlock().setType(Material.SIGN_POST);
					signpoint.getBlock().setData((byte)direction);
					Sign s = (Sign)signpoint.getBlock().getState();
					s.setLine(0, p.getName());
					s.setLine(1, s1);
					s.setLine(2, s2);
					s.setLine(3, getGreenDate());
					
					s.update();	
				}
			}
			else
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
		
		
}

