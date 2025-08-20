# LandProtection addon for the main plugin GravesX

## You will need to disable worldguard implementation in config.yml. Not doing so may cause conflicts.

## WorldGuard/Lands/GriefDefender/GriefPrevention* Flags
- gravesx-grave-create - Allows/Disallows a grave to be created in a region
- gravesx-grave-teleport - Allows/Disallows teleporting to a grave in a region
- gravesx-grave-loot - Allows/Disallows a grave to be looted in a region
- gravesx-grave-autoloot - Allows/Disallows a grave to be auto looted in a region
- gravesx-grave-walkover - Allows/Disallows a grave to be walked over
- gravesx-grave-projectile - Allows/Disallows a grave to have a projectile hit a grave
- gravesx-grave-break - Allows/Disallows a grave to be broken in a region

## Towny
Must be a member of a town, or else the grave will not spawn and instead drop all contents.

## GriefPrevention
For best experience, we recommend [GPFlags](https://modrinth.com/plugin/gpflags to enable the flags above. Without, it will fall back to trust instead.

## Commands
- /gxlp - Prints Debug Information
- /gxlp reload - Reloads all configurations

## Permissions
- gravesx.landprotection.debug - Allows access to `/gxlp`
- gravesx.landprotection.reload - Allows access to `/gxlp reload`