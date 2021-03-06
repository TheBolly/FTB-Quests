package com.feed_the_beast.ftbquests;

import com.feed_the_beast.ftblib.events.FTBLibPreInitRegistryEvent;
import com.feed_the_beast.ftblib.lib.data.FTBLibAPI;
import com.feed_the_beast.ftblib.lib.gui.GuiIcons;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftbquests.block.BlockLootCrateOpener;
import com.feed_the_beast.ftbquests.block.BlockLootCrateStorage;
import com.feed_the_beast.ftbquests.block.BlockProgressDetector;
import com.feed_the_beast.ftbquests.block.BlockProgressScreen;
import com.feed_the_beast.ftbquests.block.BlockProgressScreenPart;
import com.feed_the_beast.ftbquests.block.BlockQuestChest;
import com.feed_the_beast.ftbquests.block.BlockTaskScreen;
import com.feed_the_beast.ftbquests.block.BlockTaskScreenPart;
import com.feed_the_beast.ftbquests.block.FTBQuestsBlocks;
import com.feed_the_beast.ftbquests.block.ItemBlockProgressScreen;
import com.feed_the_beast.ftbquests.block.ItemBlockScreen;
import com.feed_the_beast.ftbquests.item.ItemLootCrate;
import com.feed_the_beast.ftbquests.item.ItemQuestBook;
import com.feed_the_beast.ftbquests.quest.ITeamData;
import com.feed_the_beast.ftbquests.quest.ServerQuestFile;
import com.feed_the_beast.ftbquests.quest.loot.LootCrate;
import com.feed_the_beast.ftbquests.quest.reward.ChoiceReward;
import com.feed_the_beast.ftbquests.quest.reward.CommandReward;
import com.feed_the_beast.ftbquests.quest.reward.FTBQuestsRewards;
import com.feed_the_beast.ftbquests.quest.reward.ItemReward;
import com.feed_the_beast.ftbquests.quest.reward.LootReward;
import com.feed_the_beast.ftbquests.quest.reward.QuestRewardType;
import com.feed_the_beast.ftbquests.quest.reward.RandomReward;
import com.feed_the_beast.ftbquests.quest.reward.XPLevelsReward;
import com.feed_the_beast.ftbquests.quest.reward.XPReward;
import com.feed_the_beast.ftbquests.quest.task.DimensionTask;
import com.feed_the_beast.ftbquests.quest.task.FTBQuestsTasks;
import com.feed_the_beast.ftbquests.quest.task.FluidTask;
import com.feed_the_beast.ftbquests.quest.task.ForgeEnergyTask;
import com.feed_the_beast.ftbquests.quest.task.ItemTask;
import com.feed_the_beast.ftbquests.quest.task.KillTask;
import com.feed_the_beast.ftbquests.quest.task.LocationTask;
import com.feed_the_beast.ftbquests.quest.task.QuestTaskData;
import com.feed_the_beast.ftbquests.quest.task.QuestTaskType;
import com.feed_the_beast.ftbquests.quest.task.StatTask;
import com.feed_the_beast.ftbquests.quest.task.XPTask;
import com.feed_the_beast.ftbquests.tile.TileLootCrateOpener;
import com.feed_the_beast.ftbquests.tile.TileLootCrateStorage;
import com.feed_the_beast.ftbquests.tile.TileProgressDetector;
import com.feed_the_beast.ftbquests.tile.TileProgressScreenCore;
import com.feed_the_beast.ftbquests.tile.TileProgressScreenPart;
import com.feed_the_beast.ftbquests.tile.TileQuestChest;
import com.feed_the_beast.ftbquests.tile.TileTaskScreenCore;
import com.feed_the_beast.ftbquests.tile.TileTaskScreenPart;
import com.feed_the_beast.ftbquests.util.ConfigQuestObject;
import com.feed_the_beast.ftbquests.util.FTBQuestsInventoryListener;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Collections;
import java.util.List;

/**
 * @author LatvianModder
 */
@Mod.EventBusSubscriber(modid = FTBQuests.MOD_ID)
public class FTBQuestsEventHandler
{
	private static Block withName(Block block, String name)
	{
		block.setCreativeTab(FTBQuests.TAB);
		block.setRegistryName(name);
		block.setTranslationKey(FTBQuests.MOD_ID + "." + name);
		return block;
	}

	private static Item withName(Item item, String name)
	{
		item.setCreativeTab(FTBQuests.TAB);
		item.setRegistryName(name);
		item.setTranslationKey(FTBQuests.MOD_ID + "." + name);
		return item;
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event)
	{
		event.getRegistry().registerAll(
				withName(new BlockTaskScreen(), "screen"),
				withName(new BlockTaskScreenPart(), "screen_part"),
				withName(new BlockProgressDetector(), "progress_detector"),
				withName(new BlockProgressScreen(), "progress_screen"),
				withName(new BlockProgressScreenPart(), "progress_screen_part"),
				withName(new BlockQuestChest(), "chest"),
				withName(new BlockLootCrateStorage(), "loot_crate_storage"),
				withName(new BlockLootCrateOpener(), "loot_crate_opener")
		);

		GameRegistry.registerTileEntity(TileTaskScreenCore.class, new ResourceLocation(FTBQuests.MOD_ID, "screen_core"));
		GameRegistry.registerTileEntity(TileTaskScreenPart.class, new ResourceLocation(FTBQuests.MOD_ID, "screen_part"));
		GameRegistry.registerTileEntity(TileProgressDetector.class, new ResourceLocation(FTBQuests.MOD_ID, "progress_detector"));
		GameRegistry.registerTileEntity(TileProgressScreenCore.class, new ResourceLocation(FTBQuests.MOD_ID, "progress_screen_core"));
		GameRegistry.registerTileEntity(TileProgressScreenPart.class, new ResourceLocation(FTBQuests.MOD_ID, "progress_screen_part"));
		GameRegistry.registerTileEntity(TileQuestChest.class, new ResourceLocation(FTBQuests.MOD_ID, "chest"));
		GameRegistry.registerTileEntity(TileLootCrateStorage.class, new ResourceLocation(FTBQuests.MOD_ID, "loot_crate_storage"));
		GameRegistry.registerTileEntity(TileLootCrateOpener.class, new ResourceLocation(FTBQuests.MOD_ID, "loot_crate_opener"));
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event)
	{
		event.getRegistry().registerAll(
				new ItemBlockScreen(FTBQuestsBlocks.SCREEN).setRegistryName("screen"),
				new ItemBlock(FTBQuestsBlocks.PROGRESS_DETECTOR).setRegistryName("progress_detector"),
				new ItemBlockProgressScreen(FTBQuestsBlocks.PROGRESS_SCREEN).setRegistryName("progress_screen"),
				new ItemBlock(FTBQuestsBlocks.CHEST).setRegistryName("chest"),
				new ItemBlock(FTBQuestsBlocks.LOOT_CRATE_STORAGE).setRegistryName("loot_crate_storage"),
				new ItemBlock(FTBQuestsBlocks.LOOT_CRATE_OPENER).setRegistryName("loot_crate_opener"),

				withName(new ItemQuestBook(), "book"),
				withName(new ItemLootCrate(), "lootcrate").setTranslationKey(FTBQuests.MOD_ID + ".lootcrate")
		);
	}

	@SubscribeEvent
	public static void registerFTBLib(FTBLibPreInitRegistryEvent event)
	{
		event.getRegistry().registerConfigValueProvider(ConfigQuestObject.ID, () -> new ConfigQuestObject(null, null));
	}

	@SubscribeEvent
	public static void registerTasks(RegistryEvent.Register<QuestTaskType> event)
	{
		event.getRegistry().registerAll(
				FTBQuestsTasks.ITEM = new QuestTaskType(ItemTask::new).setRegistryName("item").setIcon(Icon.getIcon("minecraft:items/diamond")),
				FTBQuestsTasks.FLUID = new QuestTaskType(FluidTask::new).setRegistryName("fluid").setIcon(Icon.getIcon(FluidRegistry.WATER.getStill(new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME)).toString()).combineWith(Icon.getIcon(FluidTask.TANK_TEXTURE.toString()))),
				FTBQuestsTasks.FORGE_ENERGY = new QuestTaskType(ForgeEnergyTask::new).setRegistryName("forge_energy").setIcon(Icon.getIcon(ForgeEnergyTask.EMPTY_TEXTURE.toString()).combineWith(Icon.getIcon(ForgeEnergyTask.FULL_TEXTURE.toString()))),
				FTBQuestsTasks.XP = new QuestTaskType(XPTask::new).setRegistryName("xp").setIcon(Icon.getIcon("minecraft:items/experience_bottle")),
				FTBQuestsTasks.DIMENSION = new QuestTaskType(DimensionTask::new).setRegistryName("dimension").setIcon(Icon.getIcon("minecraft:blocks/portal")),
				FTBQuestsTasks.STAT = new QuestTaskType(StatTask::new).setRegistryName("stat").setIcon(Icon.getIcon("minecraft:items/iron_sword")),
				FTBQuestsTasks.KILL = new QuestTaskType(KillTask::new).setRegistryName("kill").setIcon(Icon.getIcon("minecraft:items/diamond_sword")),
				FTBQuestsTasks.LOCATION = new QuestTaskType(LocationTask::new).setRegistryName("location").setIcon(Icon.getIcon("minecraft:items/compass_00"))
		);

		FTBQuests.PROXY.setTaskGuiProviders();
	}

	@SubscribeEvent
	public static void registerRewards(RegistryEvent.Register<QuestRewardType> event)
	{
		event.getRegistry().registerAll(
				FTBQuestsRewards.ITEM = new QuestRewardType(ItemReward::new).setRegistryName("item").setIcon(Icon.getIcon("minecraft:items/diamond")),
				FTBQuestsRewards.CHOICE = new QuestRewardType(ChoiceReward::new).setRegistryName("choice").setIcon(GuiIcons.COLOR_RGB).setExcludeFromListRewards(true),
				FTBQuestsRewards.RANDOM = new QuestRewardType(RandomReward::new).setRegistryName("random").setIcon(GuiIcons.DICE).setExcludeFromListRewards(true),
				FTBQuestsRewards.LOOT = new QuestRewardType(LootReward::new).setRegistryName("loot").setIcon(GuiIcons.MONEY_BAG).setExcludeFromListRewards(true),
				FTBQuestsRewards.XP = new QuestRewardType(XPReward::new).setRegistryName("xp").setIcon(Icon.getIcon("minecraft:items/experience_bottle")),
				FTBQuestsRewards.XP_LEVELS = new QuestRewardType(XPLevelsReward::new).setRegistryName("xp_levels").setIcon(Icon.getIcon("minecraft:items/experience_bottle")),
				FTBQuestsRewards.COMMAND = new QuestRewardType(CommandReward::new).setRegistryName("command").setIcon(Icon.getIcon("minecraft:blocks/command_block_back"))
		);

		FTBQuests.PROXY.setRewardGuiProviders();
	}

	@SubscribeEvent
	public static void onLivingDrops(LivingDropsEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();

		if (e.world.isRemote || e instanceof EntityPlayer)
		{
			return;
		}

		if (!ServerQuestFile.INSTANCE.dropLootCrates)
		{
			return;
		}

		LootCrate crate = ServerQuestFile.INSTANCE.getRandomLootCrate(e, e.world.rand);

		if (crate != null)
		{
			EntityItem ei = new EntityItem(e.world, e.posX, e.posY, e.posZ, crate.createStack());
			ei.setPickupDelay(10);
			event.getDrops().add(ei);
		}
	}

	@SubscribeEvent
	public static void onPlayerKillEvent(LivingDeathEvent event)
	{
		if (event.getSource().getTrueSource() instanceof EntityPlayerMP)
		{
			List<KillTask> killTasks = ServerQuestFile.INSTANCE.getKillTasks();

			if (killTasks.isEmpty())
			{
				return;
			}

			EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
			ITeamData data = ServerQuestFile.INSTANCE.getData(FTBLibAPI.getTeamID(player.getUniqueID()));

			if (data == null)
			{
				return;
			}

			for (KillTask task : killTasks)
			{
				QuestTaskData taskData = data.getQuestTaskData(task);

				if (taskData.getProgress() < task.getMaxProgress() && task.quest.canStartTasks(data))
				{
					((KillTask.Data) taskData).kill(event.getEntityLiving());
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event)
	{
		if (event.phase == TickEvent.Phase.END && !event.player.world.isRemote)
		{
			List<LocationTask> locationTasks = ServerQuestFile.INSTANCE.getLocationTasks();

			if (locationTasks.isEmpty())
			{
				return;
			}

			ITeamData data = ServerQuestFile.INSTANCE.getData(FTBLibAPI.getTeamID(event.player.getUniqueID()));

			if (data == null)
			{
				return;
			}

			for (LocationTask task : locationTasks)
			{
				QuestTaskData taskData = data.getQuestTaskData(task);

				if (taskData.getProgress() == 0L && task.quest.canStartTasks(data))
				{
					taskData.submitTask((EntityPlayerMP) event.player, Collections.emptyList(), false);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onItemCraftedEvent(PlayerEvent.ItemCraftedEvent event)
	{
		if (event.player instanceof EntityPlayerMP && !event.crafting.isEmpty())
		{
			FTBQuestsInventoryListener.detect((EntityPlayerMP) event.player, Collections.singleton(event.crafting));
		}
	}

	@SubscribeEvent
	public static void onItemSmeltEvent(PlayerEvent.ItemSmeltedEvent event)
	{
		if (event.player instanceof EntityPlayerMP && !event.smelting.isEmpty())
		{
			FTBQuestsInventoryListener.detect((EntityPlayerMP) event.player, Collections.singleton(event.smelting));
		}
	}
}