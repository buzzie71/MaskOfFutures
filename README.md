MaskOfFutures
=============

This plugin contains four features of varying degrees of completeness originally prepared for Nerd.Nu's Halloween 2014 event.

- Death signs (incomplete)
- Brick dropping from wither sounds
- Death messages
- Undead horse spawning and taming


The original concept for death signs was the idea that a sign reporting player name, reason of death, and time of death (server time) would be placed at the location of the player's death.  Policy and maintainability concerns ended development of this partway through, but bits of this are still present in the code.

This plugin has been compiled and tested against Spigot-1.12-R01-SNAPSHOT.jar.

The plugin is named as a reference to the Kanohi Olisi, the Mask of Alternate Futures, worn by Karzahni in the BIONICLE mythos.

===
MoF In-Game Config Changing
===

`/mof` will bring up the current config states of brick dropping and custom death messages.  These can be altered in-game with:

`/mof [brick-dropping|brick-dropping-dragon|death-msgs|death-messages|tame-traps|log-vanilla-death] [true|false]`

Additionally, if the config is changed while the plugin is running, or if config settings are to be reverted to the file,

`/mof reload`

will reload the config file from disk.

Config changes can be saved to file with

`/mof save`

These commands are intended to be reserved for server administrators.

===
Brick Dropping
===

Whenever a Wither explodes during spawn-in, all players on the server will drop a brick at their feet with the lore: "-Player- dropped this on hearing a Wither".  Players in Modmode will not drop this brick, but their presence will instead be logged to console in the hope that they will be reimbursed some time after they leave Modmode.  These bricks can be generated with the administrative command:

`/wbrick (name)`

The list of players who received a brick will also be logged to console.

===
Brick Dropping (Dragon)
===

Whenever a Wither explodes during spawn-in, all players on the server will drop a brick at their feet with the lore: "-Player- dropped this on hearing a Wither".  Players in Modmode will not drop this brick, but their presence will instead be logged to console in the hope that they will be reimbursed some time after they leave Modmode.  These bricks can be generated with the administrative command:

`/wbrick (name)`

The list of players who received a brick will also be logged to console.


===
Death Messages
===

These custom death messages are specified in the config file (config.yml) and, when on, will replace the vanilla death messages with the custom one if one exists.  When off, the plugin will instead log debug information to the console while allowing the vanilla death message to be broadcast unhindered.  The debug information consists of:

- Reason of death
- Last entity to deal killing blow to player (if killed by entity such as a mob)
- Death message (if it exists; if this does not appear, then a death message for that particular reason and killer is missing!)

The configuration supports the use of color codes with a & prefix.  In addition to the Minecraft formatting codes, other codes are used to specify different strings:

- `&p` denotes the name of the killed player.
- `&z` denotes the name of the killing mob.  If the mob does not have a custom name, the name of the mob type is used instead.
- `&i` denotes the name of the item in the killing mob's hand at the time of the kill.  If the item does not have a custom name (that is, not named on an anvil or through plugins), then the item type as listed in the Bukkit API's Material enum is used instead.  (This is used in the with-item phrase contained in msg.mobtype.item, which is not included in the death message unless the killing mob is holding an item.  The behavior of using this code in the main death message when the mob is not holding an item is undefined.)
- `&w` denotes a space concatenated in front of the with-item phrase given in msg.mobtype.item.  This can be inserted in death messages for mobs that can kill with an item (given in msg.mobtype.noitem) to specify where this phrase goes in the message.  Such a death message will append the with-item phrase at the end of the given death message if the `&w` code is missing.

When a player dies and custom death messages are on, the plugin will randomly choose one death message from the config's list for the applicable death reason, replace flags with names as described above, and display it.  

If a player dies due to a mob that can visibly hold an item (eg. zombies, skeletons, players, witches), a random death message from the applicable death reason's `noitem` list is chosen.  If the killing mob is holding an item at time of player death, a random with-item phrase is chosen from the applicable death reason's `item` list and either appended to the end of the `noitem` death message or substituted in place of `&w` as described above.  The resulting death message is then displayed.

===
Suppressing death messages
===

Players can suppress death messages in their own chat feed by running `/ignore-deaths`.  The same command will also allow death messages in the feed if it was previously suppressed.

===
Altering death messages in-game
===

There exist commands intended for server administrators to alter the death messages in the config while in-game, and can be accessed through `/mofmsg`.  These commands are recommended for users with a deeper understanding of the plugin.  Note that the subcategories used in this set of commands correspond to keys in the config file, though without the `msg.` prefix.

   - `/mofmsg view [category]` will show a list of subcategories if they exist (eg. a query of `zombie` will show subcategories `item` and `noitem`).  Subcategories can be queried by appending them to categories after a period (eg. `zombie.noitem`).  Otherwise, the command will show a list of strings in that subcategory.  The actual key that is queried in the config is `msg.[category supplied in the command]`.
   - `/mofmsg add [category] [death message]` will add the specified death message to the category.  This can only be done for string lists.
   - `/mofmsg delete [category] [number]` will delete the string with that number from the list, as seen when querying it with `view`.  This can only be done for string lists.
   - `/mofmsg addcat [category]` will add a new category of strings or overwrite an existing category, as well as insert a placeholder string in the resulting list.  Be sure to replace the placeholder!  Useful for adding new death message categories, eg. if a new death method is added to the game but not yet to the config.
   - `/mofmsg delcat [category]` will delete the category.  The command will restore all the string lists in that category to its default values, if they exist in the default config.  Its effect on the parent keys of the string list is undefined, but they can be restored by using `addcat` to remake their place in the config.

===
Undead horse spawning and taming
===

Zombie and skeleton horses can be spawned in using `/zhorse [ownername]` and `/shorse [ownername]`, respectively.  An owner can be included as an argument to spawn in a tamed horse; absence of arguments will tame the horse to the command runner.  

Untamed undead horses can be tamed with `/tame` and right-clicking on the horse - this will fail if the horse is not undead or the horse is already tamed.  Additionally, undead horses can be tamed to someone other than the player running the command with '/tame (name)'.  

Note that if this plugin is used with CobraCorral (https://github.com/TheAcademician/CobraCorral), horses tamed using the latter method should also be locked with `/hlock`.

Undead horse statistics are randomly generated as is the case for spawning in a horse by egg.  

Note that these commands, with the exception of `/tame` (with no arguments) are intended to be reserved for server administrators.

===
Building the Plugin
===

MoF is built using Apache Maven.

MoF integrates with [ModMode](http://github.com/NerdNu/ModMode), if present.  In the absense of a NerdNu maven repository, you can install the ModMode plugin JAR as a local dependency, using the following command (substituting the correct path to the ModMode plugin JAR file):

```
mvn install:install-file -Dpackaging=jar -DgroupId=nu.nerd -DartifactId=modmode \
    -Dversion=3.5.0 -Dfile=/path/to/ModMode/target/ModMode-3.5.0.jar
```

You can then build MoF by running `mvn`.

===
Changelog
===
0.14.1
   - Added handling of deaths by dolphin, pufferfish, and phantom.  Existing versions of the plugin require the addition of the keys msg.dolphin, msg.pufferfish, msg.phantom, respectively.  
   - (holdover from 0.14) Commented out placeSignFromReason() contents, which was originally intended to be used for placing signs where a player dies (unimplemented feature).
   - Updated ModMode version to 3.7.6 and Bukkit version to 1.13-R0.1-SNAPSHOT in pom.xml.

0.14
   - Updated to work explicitly on Minecraft 1.13.  This is only a port of the previous version to 1.13; new deaths will be added in the next update.

0.13
   - Added `/mute-deaths` as another command to silence death messages in the client until the restart.  This functions identically to the existing `/ignore-deaths`.
   - Added a new config option: `brick-dropping-dragon`.  This option is not currently implemented.  This option was intended to regulate whether or not players will drop a lored brick at their feet whenever the dragon death scream is heard, similar to wither spawns for the existing `brick-dropping` config option.  

0.12
   - Added handling of deaths by illusioner.  Existing versions of the plugin require the addition of the keys msg.arrow.illusioner.item and msg.arrow.illusioner.noitem.  Note that this addition makes the assumption that illusioners can't use melee weapons.
   - Added personal muting of death messages until server restart.  Players can suppress death messages appearing in their chat feed until a server restart with the toggle command `/ignore-deaths`.  Death messages will be received again to the feed after running the same command.
   - The version number of the plugin now appears in the `/mof` config menu for administrators.
   - Added command for administrators to save the config to disk with `/mof save`.
   - Added a new config option: `log-vanilla-death`.  This option toggles whether or not the vanilla death message is printed in console at time of player death, along with the custom death message.  This option does nothing if `death-messages` is set to false (ie. custom death messages are disabled).  The option can be toggled by administrators with `/mof log-vanilla-death (true|false)`.  Existing versions of the plugin will require the addition of the key `log-vanilla-death` to the config, which holds either a true or false value.
   - Added administrative tools to alter the custom death messages in the config from the game.  These can be accessed through `/mofmsg`.  Note that `addcat` and `delcat` behavior has not been fully characterized.  It is highly advised that periodic backups of the config are kept in case the config is irreversibly or inconveniently altered from in-game.
   - This version was compiled using Spigot-1.12-R0.1-SNAPSHOT.jar, the most recent available from Spigot on July 8, 2017.
   - Failed to remove a prophetic comment line from the top of MaskOfFutures.java.

0.11
   - Changed version numbering somewhat
   - Added handling of deaths by llama, fireworks, evokers, vexes, and vindicators.  Existing versions of the plugin require the addition of the keys msg.llama, msg.firework, msg.evoker, msg.vex.item, msg.vex.noitem, msg.vindicator.item, msg.vindicator.noitem to display the custom death messages.  Note that the code addition makes certain assumptions about the mob loadouts found in the game (eg. vindicators don't ever wear Thorns armor); additional handling will be added in a future update if they are found to be false.
   - Deaths due to Zombie Villagers, Strays, and Wither Skeletons are now reported as being committed by them rather than by their parent mob type (ie. Zombie or Skeleton).  The death messages displayed for these mob variants, as well as Husks, are still those of their parent mob type.
   - Fixed death messages involving an item always defaulting to the indefinite article 'a'.
   - This version was compiled using Spigot-1.11.2-R0.1-SNAPSHOT.jar, the most recent available from Spigot on January 6, 2017.

0.10.9.2
   - Fixed deaths by ender dragon breaths not being caught.  Existing versions of this plugin require the key msg.cloud.generic in config.yml to display the custom death message.

0.10.9.1
   - Fixed polar bear death messages being reported as committed by "Polar_bear".
   - Fixed TNT attacker name reported with first letter truncated.

0.10.9:
   - Fixed death messages from arrows fired from dispensers not appearing.
   - Added a new config option: tame-traps.  Horse from a skeleton trap are considered by the game to be tamed but have no owner; the trap taming feature allows players to claim the trap horse as their own by forcibly setting the trap horse's owner to be them.  This is projected to be useful in conjunction with tame horse locking plugins such as CobraCorral; it is not anticipated to be as useful in their absence.  By default, this config option is set to false, but can be toggled both in-game and in console with `/mof tame-traps (true|false)`.  Existing versions of the plugin will require the key tame-traps to config.yml, which holds either a true or false value.
   - The config options list visible when typing `/mof` has been updated to include the new tame-traps config option.
   - The custom death messages config option can now be toggled with `/mof death-messages (true|false)` as well as the old `/mof death-msgs (true|false)`.
   - Updated README to detail the mechanics behind displaying a death message.
   - Added handling of deaths by polar bear and magma blocks.  Existing versions of the plugin require the addition of the keys msg.polarbear and msg.hotfloor to config.yml to display the custom death messages.
   - Added code in Zombie death handling to specifically report kills by Husks as committed by Husk.
   - Added TNT attacker reporting.  That is, if the vanilla TNT death message reports another player or entity ("Player was blown up by SomethingElse"), the custom death message will extract and report the attacker as well.  Existing versions of the plugin require the addition of the key msg.tnt.entity to config.yml to display this custom death message.
   - This version was compiled using Spigot-1.10.2-R0.1-SNAPSHOT.jar, the most recent available from Spigot on July 13, 2016.


0.10.8:
   - Added an example message for death by gliding into a wall in the default config (msg.crash).
   - Added handling of deaths by plugin or other miscellaneous methods (ie. the ones that result in the death message "[Player] died").  Existing versions of the plugin require the addition of the key msg.default to config.yml to display the custom death message.

0.10.7.1:
   - Fixed default config's TNT death message labeling - the no-entity variant no longer tries futilely to report the entity responsible for igniting TNT (&z) in the death message.
   - Note that the no-entity variant is currently the default for deaths due to TNT - the entity variants are not used at the moment.

0.10.7:
   - Readme updated to contain version numbers that match what is given in the plugin's plugin.yml.
   - Plugin now catches kills by shulkers, kills by lingering potion clouds, and kills by gliding into a wall with elytra.  Existing versions of the plugin require addition of the following keys to config.yml to display these death messages: msg.shulker, msg.cloud.player, msg.cloud.dispenser, msg.crash
   - The location of the with-item phrase can now be specified with the code `&w` for death messages that can display the killing mob's item info.
   - This version was compiled using Spigot-1.9-R0.1-SNAPSHOT.jar, the most recent available from Spigot on April 19, 2016.
   - This version contains debugging code for kills by TNT that will report in console the source of the primed TNT that kills a player as well as the vanilla death message for the death instance.  This is intended to pave the way for TNT death messages that contain information about the entity that set off the TNT.  For now, since the TNT custom death messages do not support inclusion of this information, the vanilla death message is outputted in the console.  To prevent the plugin from outputting this, lines 652 to 659 and line 668 in BeingListener.java should be commented out.

0.10.6:
   - Explosion Update: Plugin now catches kills by TNT, kills by beds outside the Overworld, and kills by Ender Crystals.  Existing versions of the plugin require addition of the following keys to config.yml to display these death messsages: msg.tnt.noentity, msg.bed, msg.endercrystal
   - This is intended to be the last update for Minecraft version 1.8.8.  
   - This version was compiled using Spigot-1.8.8-R0.1-SNAPSHOT.jar; as such, its behavior is undefined for Minecraft version 1.9.  However, preliminary testing suggests that the plugin may still function as is.

0.10.5:
   - Added Maven build.
   - Expanded README.md (note: undead horse spawning and taming is part of the plugin but was previously not mentioned in the README).
   - Undead horses are now forced to be adults on spawn-in.

0.10.4: 
   - Fixed brick dropping.  
   - Plugin now catches dispenser potion kills and Wither skull kills.
