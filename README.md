MaskOfFutures
=============

This plugin contains four features of varying degrees of completeness originally prepared for Nerd.Nu's Halloween 2014 event.

- Death signs (incomplete)
- Brick dropping from wither sounds
- Death messages
- Undead horse spawning and taming


The original concept for death signs was the idea that a sign reporting player name, reason of death, and time of death (server time) would be placed at the location of the player's death.  Policy and maintainability concerns ended development of this partway through, but bits of this are still present in the code.

This plugin has been tested against Spigot-1.8.8-R01-SNAPSHOT.jar.

The plugin is named as a reference to the Kanohi Olisi, the Mask of Alternate Futures, worn by Karzahni in the BIONICLE mythos.

===
MoF In-Game Config Changing
===

`/mof` will bring up the current config states of brick dropping and custom death messages.  These can be altered in-game with:

`/mof (brick-dropping|death-msgs) (true|false)`

Additionally, if the config is changed while the plugin is running, 

`/mof reload`

will reload the config file.

These commands are intended to be reserved for server administrators.

===
Brick Dropping
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

===
Undead horse spawning and taming
===

Zombie and skeleton horses can be spawned in using `/zhorse [ownername]` and `/shorse [ownername]`, respectively.  An owner can be included as an argument to spawn in a tamed horse; absence of arguments will spawn in an untamed horse.  

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
10.5:
   - Added Maven build.
   - Expanded README.md (note: undead horse spawning and taming is part of the plugin but was previously not mentioned in the README).
   - Undead horses are now forced to be adults on spawn-in.

10.4: 
   - Fixed brick dropping.  
   - Plugin now catches dispenser potion kills and Wither skull kills.
