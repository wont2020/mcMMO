package com.gmail.nossr50.skills.herbalism;

import org.bukkit.CropState;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.CocoaPlant.CocoaPlantSize;
import org.bukkit.material.MaterialData;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.AdvancedConfig;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.config.mods.CustomBlocksConfig;
import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.datatypes.mods.CustomBlock;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.runnables.GreenThumbTimer;
import com.gmail.nossr50.skills.AbilityType;
import com.gmail.nossr50.skills.SkillType;
import com.gmail.nossr50.skills.Skills;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.ModChecks;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.Users;

public class Herbalism {
    public static int farmersDietRankLevel1 = AdvancedConfig.getInstance().getFarmerDietRankChange();
    public static int farmersDietRankLevel2 = farmersDietRankLevel1 * 2;
    public static int farmersDietMaxLevel = farmersDietRankLevel1 * 5;

    public static int greenThumbStageChangeLevel = AdvancedConfig.getInstance().getGreenThumbStageChange();
    public static int greenThumbStageMaxLevel = greenThumbStageChangeLevel * 4;

    public static double greenThumbMaxChance = AdvancedConfig.getInstance().getGreenThumbChanceMax();
    public static int greenThumbMaxLevel = AdvancedConfig.getInstance().getGreenThumbMaxLevel();

    public static double doubleDropsMaxChance = AdvancedConfig.getInstance().getHerbalismDoubleDropsChanceMax();
    public static int doubleDropsMaxLevel = AdvancedConfig.getInstance().getHerbalismDoubleDropsMaxLevel();
    public static boolean doubleDropsDisabled = Config.getInstance().herbalismDoubleDropsDisabled();

    public static double hylianLuckMaxChance = AdvancedConfig.getInstance().getHylianLuckChanceMax();
    public static int hylianLuckMaxLevel = AdvancedConfig.getInstance().getHylianLucksMaxLevel();

    public static boolean greenTerraWalls = Config.getInstance().getHerbalismGreenThumbCobbleWallToMossyWall();
    public static boolean greenTerraSmoothBrick = Config.getInstance().getHerbalismGreenThumbSmoothbrickToMossy();
    public static boolean greenTerraDirt = Config.getInstance().getHerbalismGreenThumbDirtToGrass();
    public static boolean greenTerraCobble = Config.getInstance().getHerbalismGreenThumbCobbleToMossy();

    public static void farmersDiet(Player player, int rankChange, FoodLevelChangeEvent event) {
        if (!Permissions.farmersDiet(player)) {
            return;
        }

        Skills.handleFoodSkills(player, SkillType.HERBALISM, event, farmersDietRankLevel1, farmersDietMaxLevel, rankChange);
    }

    /**
     * Activate the Green Terra ability.
     *
     * @param player The player activating the ability
     * @param block The block to be changed by Green Terra
     */
    public static void greenTerra(Player player, Block block) {
        PlayerInventory inventory = player.getInventory();
        boolean hasSeeds = inventory.contains(Material.SEEDS);

        if (!hasSeeds) {
            player.sendMessage(LocaleLoader.getString("Herbalism.Ability.GTe.NeedMore"));
            return;
        }

        if (!block.getType().equals(Material.WHEAT)) {
            inventory.removeItem(new ItemStack(Material.SEEDS));
            player.updateInventory();   // Needed until replacement available
            greenTerraConvert(player, block);
        }
    }

    public static void greenTerraConvert(Player player, Block block) {
        if (Misc.blockBreakSimulate(block, player, false)) {
            Material type = block.getType();

            switch (type) {
            case SMOOTH_BRICK:
                if (greenTerraSmoothBrick && block.getData() == 0x0) {
                    block.setData((byte) 0x1);
                }
                return;

            case DIRT:
                if (greenTerraDirt) {
                    block.setType(Material.GRASS);
                }
                return;

            case COBBLESTONE:
                if (greenTerraCobble) {
                    block.setType(Material.MOSSY_COBBLESTONE);
                }
                return;

            case COBBLE_WALL:
                if (greenTerraWalls && block.getData() == 0x0) {
                    block.setData((byte) 0x1);
                }
                return;

            default:
                return;
            }
        }
    }

    /**
     * Check for extra Herbalism drops.
     *
     * @param block The block to check for extra drops
     * @param player The player getting extra drops
     * @param event The event to use for Green Thumb
     * @param plugin mcMMO plugin instance
     */
    public static void herbalismProcCheck(final Block block, Player player, BlockBreakEvent event, mcMMO plugin) {
        final PlayerProfile profile = Users.getProfile(player);

        int herbLevel = profile.getSkillLevel(SkillType.HERBALISM);
        int id = block.getTypeId();
        Material type = block.getType();

        Byte data = block.getData();
        Location location = block.getLocation();
        Material mat = null;

        int xp = 0;
        int catciDrops = 0;
        int caneDrops = 0;

        boolean customPlant = false;

        int activationChance = Misc.calculateActivationChance(Permissions.luckyHerbalism(player));

        float chance = (float) ((doubleDropsMaxChance / doubleDropsMaxLevel) * herbLevel);
        if (chance > doubleDropsMaxChance) chance = (float) doubleDropsMaxChance;

        switch (type) {
        case BROWN_MUSHROOM:
        case RED_MUSHROOM:
            if (!mcMMO.placeStore.isTrue(block)) {
                mat = Material.getMaterial(id);
                xp = Config.getInstance().getHerbalismXPMushrooms();
            }
            break;

        case CACTUS:
            for (int y = 0;  y <= 2; y++) {
                Block b = block.getRelative(0, y, 0);
                if (b.getType().equals(Material.CACTUS)) {
                    mat = Material.CACTUS;
                    if (!mcMMO.placeStore.isTrue(b)) {
                        if (chance > Misc.getRandom().nextInt(activationChance)) {
                            catciDrops++;
                        }
                        xp += Config.getInstance().getHerbalismXPCactus();
                    }
                }
            }
            break;

        case CROPS:
            if (data == CropState.RIPE.getData()) {
                mat = Material.WHEAT;
                xp = Config.getInstance().getHerbalismXPWheat();

                if (Permissions.greenThumbWheat(player)) {
                    greenThumbWheat(block, player, event, plugin);
                }
            }
            break;

        case MELON_BLOCK:
            if (!mcMMO.placeStore.isTrue(block)) {
                mat = Material.MELON;
                xp = Config.getInstance().getHerbalismXPMelon();
            }
            break;

        case NETHER_WARTS:
            if (data == (byte) 0x3) {
                mat = Material.NETHER_STALK;
                xp = Config.getInstance().getHerbalismXPNetherWart();

                if (Permissions.greenThumbNetherwart(player)) {
                    greenThumbWheat(block, player, event, plugin);
                }
            }
            break;

        case PUMPKIN:
        case JACK_O_LANTERN:
            if (!mcMMO.placeStore.isTrue(block)) {
                mat = Material.getMaterial(id);
                xp = Config.getInstance().getHerbalismXPPumpkin();
            }
            break;

        case RED_ROSE:
        case YELLOW_FLOWER:
            if (!mcMMO.placeStore.isTrue(block)) {
                mat = Material.getMaterial(id);
                xp = Config.getInstance().getHerbalismXPFlowers();
            }
            break;

        case SUGAR_CANE_BLOCK:
            for (int y = 0;  y <= 2; y++) {
                Block b = block.getRelative(0, y, 0);
                if (b.getType().equals(Material.SUGAR_CANE_BLOCK)) {
                    mat = Material.SUGAR_CANE;
                    if (!mcMMO.placeStore.isTrue(b)) {
                        if (chance > Misc.getRandom().nextInt(activationChance)) {
                            caneDrops++;
                        }
                        xp += Config.getInstance().getHerbalismXPSugarCane();
                    }
                }
            }
            break;

        case VINE:
            if (!mcMMO.placeStore.isTrue(block)) {
                mat = type;
                xp = Config.getInstance().getHerbalismXPVines();
            }
            break;

        case WATER_LILY:
            if (!mcMMO.placeStore.isTrue(block)) {
                mat = type;
                xp = Config.getInstance().getHerbalismXPLilyPads();
            }
            break;

        case COCOA:
            CocoaPlant plant = (CocoaPlant) block.getState().getData();

            if (plant.getSize() == CocoaPlantSize.LARGE) {
                mat = type;
                xp = Config.getInstance().getHerbalismXPCocoa();

                if (Permissions.greenThumbCocoa(player)) {
                    greenThumbWheat(block, player, event, plugin);
                }
            }
            break;

        case CARROT:
            if (data == CropState.RIPE.getData()) {
                mat = Material.CARROT;
                xp = Config.getInstance().getHerbalismXPCarrot();


                if (Permissions.greenThumbCarrots(player)) {
                    greenThumbWheat(block, player, event, plugin);
                }
            }
            break;

        case POTATO:
            if (data == CropState.RIPE.getData()) {
                mat = Material.POTATO;
                xp = Config.getInstance().getHerbalismXPPotato();

                if (Permissions.greenThumbPotatoes(player)) {
                    greenThumbWheat(block, player, event, plugin);
                }
            }
            break;

        default:
            ItemStack item = (new MaterialData(block.getTypeId(), block.getData())).toItemStack(1);

            if (Config.getInstance().getBlockModsEnabled() && CustomBlocksConfig.getInstance().customHerbalismBlocks.contains(item)) {
                customPlant = true;
                xp = ModChecks.getCustomBlock(block).getXpGain();
            }
            break;
        }

        if (mat == null && !customPlant) {
            return;
        }

        if (Permissions.herbalismDoubleDrops(player)) {
            ItemStack is = null;

            if (customPlant) {
                is = new ItemStack(ModChecks.getCustomBlock(block).getItemDrop());
            }
            else {
                if (mat == Material.COCOA) {
                    try {
                        is = new ItemStack(Material.INK_SACK, 1, DyeColor.BROWN.getDyeData());
                    }
                    catch (Exception e) {
                        is = new ItemStack(Material.INK_SACK, 1, (short) 3);
                    }
                    catch (NoSuchMethodError e) {
                        is = new ItemStack(Material.INK_SACK, 1, (short) 3);
                    }
                }
                else if (mat == Material.CARROT) {
                    is = new ItemStack(Material.CARROT_ITEM);
                }
                else if (mat == Material.POTATO) {
                    is = new ItemStack(Material.POTATO_ITEM);
                }
                else {
                    is = new ItemStack(mat);
                }
            }

            if (chance > Misc.getRandom().nextInt(activationChance)) {
                Config configInstance = Config.getInstance();

                switch (type) {
                case BROWN_MUSHROOM:
                    if (configInstance.getBrownMushroomsDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case CACTUS:
                    if (configInstance.getCactiDoubleDropsEnabled()) {
                        Misc.dropItems(location, is, catciDrops);
                    }
                    break;

                case CROPS:
                    if (configInstance.getWheatDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case MELON_BLOCK:
                    if (configInstance.getMelonsDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case NETHER_WARTS:
                    if (configInstance.getNetherWartsDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case PUMPKIN:
                    if (configInstance.getPumpkinsDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case RED_MUSHROOM:
                    if (configInstance.getRedMushroomsDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case SUGAR_CANE_BLOCK:
                    if (configInstance.getSugarCaneDoubleDropsEnabled()) {
                        Misc.dropItems(location, is, caneDrops);
                    }
                    break;

                case VINE:
                    if (configInstance.getVinesDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case WATER_LILY:
                    if (configInstance.getWaterLiliesDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case YELLOW_FLOWER:
                    if (configInstance.getYellowFlowersDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case COCOA:
                    if (configInstance.getCocoaDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case CARROT:
                    if (configInstance.getCarrotDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                case POTATO:
                    if (configInstance.getPotatoDoubleDropsEnabled()) {
                        Misc.dropItem(location, is);
                    }
                    break;

                default:
                    if (customPlant) {
                        CustomBlock customBlock = ModChecks.getCustomBlock(block);
                        int minimumDropAmount = customBlock.getMinimumDropAmount();
                        int maximumDropAmount = customBlock.getMaximumDropAmount();

                        is = customBlock.getItemDrop();

                        if (minimumDropAmount != maximumDropAmount) {
                            Misc.dropItems(location, is, minimumDropAmount);
                            Misc.randomDropItems(location, is, 50, maximumDropAmount - minimumDropAmount);
                        }
                        else {
                            Misc.dropItems(location, is, minimumDropAmount);
                        }
                    }
                    break;
                }
            }
        }

        if (Config.getInstance().getHerbalismAFKDisabled() && player.isInsideVehicle())
            return;

        Skills.xpProcessing(player, profile, SkillType.HERBALISM, xp);
    }

    /**
     * Apply the Green Thumb ability to crops.
     *
     * @param block The block to apply the ability to
     * @param player The player using the ability
     * @param event The event triggering the ability
     * @param plugin mcMMO plugin instance
     */
    private static void greenThumbWheat(Block block, Player player, BlockBreakEvent event, mcMMO plugin) {
        PlayerProfile profile = Users.getProfile(player);
        int herbLevel = profile.getSkillLevel(SkillType.HERBALISM);
        PlayerInventory inventory = player.getInventory();
        boolean hasSeeds = false;
        Location location = block.getLocation();
        Material type = block.getType();

        switch(type) {
        case CROPS:
            hasSeeds = inventory.contains(Material.SEEDS);
            break;
        case COCOA:
            try {
                hasSeeds = inventory.containsAtLeast(new ItemStack(Material.INK_SACK, 1, DyeColor.BROWN.getDyeData()), 1);
            }
            catch(Exception e) {
                hasSeeds = inventory.containsAtLeast(new ItemStack(Material.INK_SACK, 1, (short) 3), 1);
            }
            catch(NoSuchMethodError e) {
                hasSeeds = inventory.containsAtLeast(new ItemStack(Material.INK_SACK, 1, (short) 3), 1);
            }
            break;
        case CARROT:
            hasSeeds = inventory.contains(Material.CARROT_ITEM);
            break;
        case POTATO:
            hasSeeds = inventory.contains(Material.POTATO_ITEM);
            break;
        case NETHER_WARTS:
            hasSeeds = inventory.contains(Material.NETHER_STALK);
            break;
        default:
            break;
        }

        int activationChance = Misc.calculateActivationChance(Permissions.luckyHerbalism(player));

        float chance = (float) ((greenThumbMaxChance / greenThumbMaxLevel) * herbLevel);
        if (chance > greenThumbMaxChance) chance = (float) greenThumbMaxChance;

        if (hasSeeds && profile.getAbilityMode(AbilityType.GREEN_TERRA) || hasSeeds && (chance > Misc.getRandom().nextInt(activationChance))) {
            event.setCancelled(true);

            switch(type) {
            case CROPS:
                Misc.dropItem(location, new ItemStack(Material.WHEAT));
                Misc.randomDropItems(location, new ItemStack(Material.SEEDS), 50, 3);
                inventory.removeItem(new ItemStack(Material.SEEDS));
                break;
            case COCOA:
                try {
                    Misc.dropItems(location, new ItemStack(Material.INK_SACK, 1, DyeColor.BROWN.getDyeData()), 3);
                    inventory.removeItem(new ItemStack(Material.INK_SACK, 1, DyeColor.BROWN.getDyeData()));
                }
                catch(Exception e) {
                    Misc.dropItems(location, new ItemStack(Material.INK_SACK, 1, (short) 3), 3);
                    inventory.removeItem(new ItemStack(Material.INK_SACK, 1, (short) 3));
                }
                catch(NoSuchMethodError e) {
                    Misc.dropItems(location, new ItemStack(Material.INK_SACK, 1, (short) 3), 3);
                    inventory.removeItem(new ItemStack(Material.INK_SACK, 1, (short) 3));
                }
                break;
            case CARROT:
                Misc.dropItem(location, new ItemStack(Material.CARROT_ITEM));
                Misc.randomDropItems(location, new ItemStack(Material.CARROT_ITEM), 50, 3);
                inventory.removeItem(new ItemStack(Material.CARROT_ITEM));
                break;
            case POTATO:
                Misc.dropItem(location, new ItemStack(Material.POTATO_ITEM));
                Misc.randomDropItems(location, new ItemStack(Material.POTATO_ITEM), 50, 3);
                Misc.randomDropItem(location, new ItemStack(Material.POISONOUS_POTATO), 2);
                inventory.removeItem(new ItemStack(Material.POTATO_ITEM));
                break;
            case NETHER_WARTS:
                Misc.dropItems(location, new ItemStack(Material.NETHER_STALK), 2);
                Misc.randomDropItems(location, new ItemStack(Material.NETHER_STALK), 50, 2);
                inventory.removeItem(new ItemStack(Material.NETHER_STALK));
                break;
            default:
                break;
            }

            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new GreenThumbTimer(block, profile, type), 1);
            player.updateInventory();   // Needed until replacement available
        }
    }

    /**
     * Apply the Green Thumb ability to blocks.
     *
     * @param is The item in the player's hand
     * @param player The player activating the ability
     * @param block The block being used in the ability
     */
    public static void greenThumbBlocks(ItemStack is, Player player, Block block) {
        PlayerProfile profile = Users.getProfile(player);
        int skillLevel = profile.getSkillLevel(SkillType.HERBALISM);
        int seeds = is.getAmount();

        player.setItemInHand(new ItemStack(Material.SEEDS, seeds - 1));

        int activationChance = Misc.calculateActivationChance(Permissions.luckyHerbalism(player));

        float chance = (float) ((greenThumbMaxChance / greenThumbMaxLevel) * skillLevel);
        if (chance > greenThumbMaxChance) chance = (float) greenThumbMaxChance;

        if (chance > Misc.getRandom().nextInt(activationChance)) {
            greenTerraConvert(player, block);
        }
        else {
            player.sendMessage(LocaleLoader.getString("Herbalism.Ability.GTh.Fail"));
        }
    }

    public static void hylianLuck(Block block, Player player, BlockBreakEvent event) {
        int skillLevel = Users.getProfile(player).getSkillLevel(SkillType.HERBALISM);

        float chance = (float) ((hylianLuckMaxChance / hylianLuckMaxLevel) * skillLevel);
        if (chance > hylianLuckMaxChance) chance = (float) hylianLuckMaxChance;

        int activationChance = Misc.calculateActivationChance(Permissions.luckyHerbalism(player));

        if (chance > Misc.getRandom().nextInt(activationChance)) {
            Location location = block.getLocation();
            int dropNumber = Misc.getRandom().nextInt(3);
            ItemStack item;

            switch (block.getType()) {
            case DEAD_BUSH:
            case LONG_GRASS:
            case SAPLING:
                if (dropNumber == 0) {
                    item = new ItemStack(Material.MELON_SEEDS);
                }
                else if (dropNumber == 1) {
                    item = new ItemStack(Material.PUMPKIN_SEEDS);
                }
                else {
                    try {
                        item = (new MaterialData(Material.INK_SACK, DyeColor.BROWN.getDyeData())).toItemStack(1);
                    }
                    catch (Exception e) {
                        item = (new MaterialData(Material.INK_SACK, (byte) 0x3)).toItemStack(1);
                    }
                    catch (NoSuchMethodError e) {
                        item = (new MaterialData(Material.INK_SACK, (byte) 0x3)).toItemStack(1);
                    }
                }
                break;

            case RED_ROSE:
            case YELLOW_FLOWER:
                if (mcMMO.placeStore.isTrue(block)) {
                    mcMMO.placeStore.setFalse(block);
                    return;
                }

                if (dropNumber == 0) {
                    item = new ItemStack(Material.POTATO);
                }
                else if (dropNumber == 1) {
                    item = new ItemStack(Material.CARROT);
                }
                else {
                    item = new ItemStack(Material.APPLE);
                }

                break;

            case FLOWER_POT:
                if (dropNumber == 0) {
                    item = new ItemStack(Material.EMERALD);
                }
                else if (dropNumber == 1) {
                    item = new ItemStack(Material.DIAMOND);
                }
                else {
                    item = new ItemStack(Material.GOLD_NUGGET);
                }
                break;

            default:
                return;
            }

            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
            Misc.dropItem(location, item);
            player.sendMessage(LocaleLoader.getString("Herbalism.HylianLuck"));
        }
    }
}
