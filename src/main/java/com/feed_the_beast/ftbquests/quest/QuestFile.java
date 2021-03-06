package com.feed_the_beast.ftbquests.quest;

import com.feed_the_beast.ftblib.FTBLibConfig;
import com.feed_the_beast.ftblib.lib.config.ConfigGroup;
import com.feed_the_beast.ftblib.lib.config.ConfigItemStack;
import com.feed_the_beast.ftblib.lib.config.ConfigTimer;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftblib.lib.icon.IconAnimation;
import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.math.MathUtils;
import com.feed_the_beast.ftblib.lib.math.Ticks;
import com.feed_the_beast.ftblib.lib.util.NBTUtils;
import com.feed_the_beast.ftbquests.FTBQuests;
import com.feed_the_beast.ftbquests.events.ObjectCompletedEvent;
import com.feed_the_beast.ftbquests.net.MessageDisplayCompletionToast;
import com.feed_the_beast.ftbquests.quest.loot.EntityWeight;
import com.feed_the_beast.ftbquests.quest.loot.LootCrate;
import com.feed_the_beast.ftbquests.quest.loot.RewardTable;
import com.feed_the_beast.ftbquests.quest.reward.ItemReward;
import com.feed_the_beast.ftbquests.quest.reward.QuestReward;
import com.feed_the_beast.ftbquests.quest.reward.QuestRewardType;
import com.feed_the_beast.ftbquests.quest.task.QuestTask;
import com.feed_the_beast.ftbquests.quest.task.QuestTaskType;
import com.latmod.mods.itemfilters.item.ItemMissing;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.ForgeRegistry;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * @author LatvianModder
 */
public abstract class QuestFile extends QuestObject
{
	public static final int VERSION = 4;

	public final List<QuestChapter> chapters;
	public final List<QuestVariable> variables;
	public final List<RewardTable> rewardTables;

	private final Int2ObjectOpenHashMap<QuestObjectBase> map;
	public final RewardTable dummyTable;

	public final List<ItemStack> emergencyItems;
	public Ticks emergencyItemsCooldown;
	public int fileVersion;
	public boolean defaultRewardTeam;
	public boolean defaultConsumeItems;
	public EnumQuestShape defaultShape;
	public boolean dropLootCrates;
	public final EntityWeight lootCrateNoDrop;

	public QuestFile()
	{
		id = 1;
		fileVersion = 0;
		chapters = new ArrayList<>();
		variables = new ArrayList<>();
		rewardTables = new ArrayList<>();

		map = new Int2ObjectOpenHashMap<>();
		dummyTable = new RewardTable(this);

		emergencyItems = new ArrayList<>();
		emergencyItems.add(new ItemStack(Items.APPLE));
		emergencyItemsCooldown = Ticks.MINUTE.x(5);

		defaultRewardTeam = false;
		defaultConsumeItems = false;
		defaultShape = EnumQuestShape.CIRCLE;
		dropLootCrates = false;
		lootCrateNoDrop = new EntityWeight();
		lootCrateNoDrop.passive = 4000;
		lootCrateNoDrop.monster = 600;
		lootCrateNoDrop.boss = 0;
	}

	public abstract boolean isClient();

	@Override
	public QuestFile getQuestFile()
	{
		return this;
	}

	@Override
	public QuestObjectType getObjectType()
	{
		return QuestObjectType.FILE;
	}

	public boolean isLoading()
	{
		return false;
	}

	public boolean canEdit()
	{
		return false;
	}

	@Override
	public long getProgress(ITeamData data)
	{
		long progress = 0L;

		for (QuestChapter chapter : chapters)
		{
			progress += chapter.getProgress(data);
		}

		return progress;
	}

	@Override
	public long getMaxProgress()
	{
		long maxProgress = 0L;

		for (QuestChapter chapter : chapters)
		{
			maxProgress += chapter.getMaxProgress();
		}

		return maxProgress;
	}

	@Override
	public int getRelativeProgress(ITeamData data)
	{
		int progress = 0;

		for (QuestChapter chapter : chapters)
		{
			progress += chapter.getRelativeProgress(data);
		}

		return fixRelativeProgress(progress, chapters.size());
	}

	@Override
	public boolean isComplete(ITeamData data)
	{
		for (QuestChapter chapter : chapters)
		{
			if (!chapter.isComplete(data))
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public void onCompleted(ITeamData data, List<EntityPlayerMP> onlineMembers)
	{
		super.onCompleted(data, onlineMembers);
		new ObjectCompletedEvent.FileEvent(data, this).post();

		for (EntityPlayerMP player : onlineMembers)
		{
			new MessageDisplayCompletionToast(id).sendTo(player);
		}
	}

	@Override
	public void changeProgress(ITeamData data, EnumChangeProgress type)
	{
		for (QuestChapter chapter : chapters)
		{
			chapter.changeProgress(data, type);
		}
	}

	@Override
	public void deleteSelf()
	{
		invalid = true;
	}

	@Override
	public void deleteChildren()
	{
		for (QuestChapter chapter : chapters)
		{
			chapter.deleteChildren();
			chapter.invalid = true;
		}

		chapters.clear();

		for (QuestVariable variable : variables)
		{
			variable.deleteChildren();
			variable.invalid = true;
		}

		variables.clear();

		for (RewardTable table : rewardTables)
		{
			table.deleteChildren();
			table.invalid = true;
		}

		rewardTables.clear();
	}

	@Nullable
	public QuestObjectBase getBase(int id)
	{
		if (id == 0)
		{
			return null;
		}
		else if (id == 1)
		{
			return this;
		}

		QuestObjectBase object = map.get(id);
		return object == null || object.invalid ? null : object;
	}

	@Nullable
	public QuestObject get(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof QuestObject ? (QuestObject) object : null;
	}

	@Nullable
	public QuestObjectBase remove(int id)
	{
		QuestObjectBase object = map.remove(id);

		if (object != null)
		{
			if (object instanceof QuestObject)
			{
				QuestObject o = (QuestObject) object;

				for (QuestChapter chapter : chapters)
				{
					for (Quest quest : chapter.quests)
					{
						quest.dependencies.remove(o);
					}
				}
			}

			object.invalid = true;
			refreshIDMap();
			return object;
		}

		return null;
	}

	@Nullable
	public QuestChapter getChapter(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof QuestChapter ? (QuestChapter) object : null;
	}

	@Nullable
	public Quest getQuest(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof Quest ? (Quest) object : null;
	}

	@Nullable
	public QuestTask getTask(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof QuestTask ? (QuestTask) object : null;
	}

	@Nullable
	public QuestVariable getVariable(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof QuestVariable ? (QuestVariable) object : null;
	}

	@Nullable
	public QuestReward getReward(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof QuestReward ? (QuestReward) object : null;
	}

	@Nullable
	public RewardTable getRewardTable(int id)
	{
		QuestObjectBase object = getBase(id);
		return object instanceof RewardTable ? (RewardTable) object : null;
	}

	@Nullable
	public LootCrate getLootCrate(String id)
	{
		if (!id.startsWith("#"))
		{
			for (RewardTable table : rewardTables)
			{
				if (table.lootCrate != null && table.lootCrate.stringID.equals(id))
				{
					return table.lootCrate;
				}
			}
		}

		RewardTable table = getRewardTable(getID(id));
		return table == null ? null : table.lootCrate;
	}

	@Nullable
	public RewardTable getRewardTable(String id)
	{
		LootCrate crate = getLootCrate(id);
		return crate != null ? crate.table : getRewardTable(getID(id));
	}

	public void refreshIDMap()
	{
		clearCachedData();
		map.clear();

		for (RewardTable table : rewardTables)
		{
			map.put(table.id, table);
		}

		for (QuestChapter chapter : chapters)
		{
			map.put(chapter.id, chapter);

			for (Quest quest : chapter.quests)
			{
				map.put(quest.id, quest);

				for (QuestTask task : quest.tasks)
				{
					map.put(task.id, task);
				}

				for (QuestReward reward : quest.rewards)
				{
					map.put(reward.id, reward);
				}
			}
		}

		for (QuestVariable variable : variables)
		{
			map.put(variable.id, variable);
		}

		clearCachedData();
	}

	public QuestObjectBase create(QuestObjectType type, int parent, NBTTagCompound extra)
	{
		switch (type)
		{
			case CHAPTER:
				return new QuestChapter(this);
			case QUEST:
			{
				QuestChapter chapter = getChapter(parent);

				if (chapter != null)
				{
					return new Quest(chapter);
				}

				throw new IllegalArgumentException("Parent chapter not found!");
			}
			case TASK:
			{
				Quest quest = getQuest(parent);

				if (quest != null)
				{
					QuestTask task = QuestTaskType.createTask(quest, extra.getString("type"));

					if (task != null)
					{
						return task;
					}

					throw new IllegalArgumentException("Unknown task type!");
				}

				throw new IllegalArgumentException("Parent quest not found!");
			}
			case VARIABLE:
				return new QuestVariable(this);
			case REWARD:
				Quest quest = getQuest(parent);

				if (quest != null)
				{
					QuestReward reward = QuestRewardType.createReward(quest, extra.getString("type"));

					if (reward != null)
					{
						return reward;
					}

					throw new IllegalArgumentException("Unknown reward type!");
				}

				throw new IllegalArgumentException("Parent quest not found!");
			case REWARD_TABLE:
				return new RewardTable(this);
			default:
				throw new IllegalArgumentException("Unknown type: " + type);
		}
	}

	@Override
	public final void writeData(NBTTagCompound nbt)
	{
		super.writeData(nbt);
		nbt.setInteger("version", VERSION);
		nbt.setBoolean("default_reward_team", defaultRewardTeam);
		nbt.setBoolean("default_consume_items", defaultConsumeItems);
		nbt.setString("default_quest_shape", defaultShape.getID());

		if (!emergencyItems.isEmpty())
		{
			NBTTagList list = new NBTTagList();

			for (ItemStack stack : emergencyItems)
			{
				list.appendTag(ItemMissing.write(stack, true));
			}

			nbt.setTag("emergency_items", list);
		}

		nbt.setString("emergency_items_cooldown", emergencyItemsCooldown.toString());
		nbt.setBoolean("drop_loot_crates", dropLootCrates);

		NBTTagCompound nbt1 = new NBTTagCompound();
		lootCrateNoDrop.writeData(nbt1);
		nbt.setTag("loot_crate_no_drop", nbt1);
	}

	@Override
	public final void readData(NBTTagCompound nbt)
	{
		super.readData(nbt);
		defaultRewardTeam = nbt.getBoolean("default_reward_team");
		defaultConsumeItems = nbt.getBoolean("default_consume_items");
		defaultShape = EnumQuestShape.NAME_MAP.get(nbt.getString("default_quest_shape"));
		emergencyItems.clear();

		NBTTagList list = nbt.getTagList("emergency_items", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < list.tagCount(); i++)
		{
			ItemStack stack = ItemMissing.read(list.getCompoundTagAt(i));

			if (!stack.isEmpty())
			{
				emergencyItems.add(stack);
			}
		}

		Ticks t = Ticks.get(nbt.getString("emergency_items_cooldown"));
		emergencyItemsCooldown = t.hasTicks() ? t : Ticks.MINUTE.x(5);
		dropLootCrates = nbt.getBoolean("drop_loot_crates");

		if (nbt.hasKey("loot_crate_no_drop"))
		{
			lootCrateNoDrop.readData(nbt.getCompoundTag("loot_crate_no_drop"));
		}
	}

	private NBTTagCompound createIndex(List<? extends QuestObjectBase> list)
	{
		int[] index = new int[list.size()];

		for (int i = 0; i < index.length; i++)
		{
			index[i] = list.get(i).id;
		}

		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setIntArray("index", index);
		return nbt;
	}

	public int[] readIndex(File file)
	{
		NBTTagCompound nbt = NBTUtils.readNBT(file);
		return nbt == null ? new int[0] : nbt.getIntArray("index");
	}

	public final void writeDataFull(File folder)
	{
		NBTTagCompound out = new NBTTagCompound();
		writeData(out);
		NBTUtils.writeNBTSafe(new File(folder, "file.nbt"), out);

		NBTUtils.writeNBTSafe(new File(folder, "chapters/index.nbt"), createIndex(chapters));
		NBTUtils.writeNBTSafe(new File(folder, "variables/index.nbt"), createIndex(variables));
		NBTUtils.writeNBTSafe(new File(folder, "reward_tables/index.nbt"), createIndex(rewardTables));

		for (QuestChapter chapter : chapters)
		{
			out = new NBTTagCompound();
			chapter.writeData(out);
			String chapterPath = "chapters/" + chapter.getCodeString() + "/";
			NBTUtils.writeNBTSafe(new File(folder, chapterPath + "chapter.nbt"), out);

			if (!chapter.quests.isEmpty())
			{
				for (Quest quest : chapter.quests)
				{
					if (quest.invalid)
					{
						continue;
					}

					out = new NBTTagCompound();
					quest.writeData(out);

					if (!quest.tasks.isEmpty())
					{
						NBTTagList t = new NBTTagList();

						for (QuestTask task : quest.tasks)
						{
							QuestTaskType type = task.getType();
							NBTTagCompound nbt3 = new NBTTagCompound();
							task.writeData(nbt3);
							nbt3.setInteger("uid", task.id);
							nbt3.setString("type", type.getTypeForNBT());
							t.appendTag(nbt3);
						}

						if (!t.isEmpty())
						{
							out.setTag("tasks", t);
						}
					}

					if (!quest.rewards.isEmpty())
					{
						NBTTagList r = new NBTTagList();

						for (QuestReward reward : quest.rewards)
						{
							QuestRewardType type = reward.getType();
							NBTTagCompound nbt3 = new NBTTagCompound();
							reward.writeData(nbt3);
							nbt3.setInteger("uid", reward.id);
							nbt3.setString("type", type.getTypeForNBT());
							r.appendTag(nbt3);
						}

						if (!r.isEmpty())
						{
							out.setTag("rewards", r);
						}
					}

					NBTUtils.writeNBTSafe(new File(folder, chapterPath + quest.getCodeString() + ".nbt"), out);
				}
			}
		}

		for (QuestVariable variable : variables)
		{
			out = new NBTTagCompound();
			variable.writeData(out);
			NBTUtils.writeNBTSafe(new File(folder, "variables/" + variable.getCodeString() + ".nbt"), out);
		}

		for (RewardTable table : rewardTables)
		{
			out = new NBTTagCompound();
			table.writeData(out);
			NBTUtils.writeNBTSafe(new File(folder, "reward_tables/" + table.getCodeString() + ".nbt"), out);
		}
	}

	public final void readDataOld(NBTTagCompound nbt)
	{
		fileVersion = nbt.getInteger("version");
		readData(nbt);

		chapters.clear();
		variables.clear();
		rewardTables.clear();

		NBTTagList rtl = nbt.getTagList("reward_tables", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < rtl.tagCount(); i++)
		{
			RewardTable table = new RewardTable(this);
			NBTTagCompound nbt1 = rtl.getCompoundTagAt(i);
			table.id = nbt1.getInteger("uid");
			table.readData(nbt1);
			rewardTables.add(table);
		}

		NBTTagList c = nbt.getTagList("chapters", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < c.tagCount(); i++)
		{
			QuestChapter chapter = new QuestChapter(this);
			NBTTagCompound ct = c.getCompoundTagAt(i);
			chapter.id = ct.getInteger("uid");
			chapters.add(chapter);

			NBTTagList q = ct.getTagList("quests", Constants.NBT.TAG_COMPOUND);

			for (int j = 0; j < q.tagCount(); j++)
			{
				Quest quest = new Quest(chapter);
				NBTTagCompound qt = q.getCompoundTagAt(j);
				quest.id = qt.getInteger("uid");
				chapter.quests.add(quest);

				NBTTagList t = qt.getTagList("tasks", Constants.NBT.TAG_COMPOUND);

				if (t.isEmpty())
				{
					NBTBase tt = qt.getTag("task");

					if (tt != null)
					{
						t.appendTag(tt);
					}
				}

				for (int k = 0; k < t.tagCount(); k++)
				{
					NBTTagCompound tt = t.getCompoundTagAt(k);
					QuestTask task = QuestTaskType.createTask(quest, tt.getString("type"));

					if (task != null)
					{
						task.id = tt.getInteger("uid");
						task.readData(tt);
						quest.tasks.add(task);
					}
				}

				NBTTagList r = qt.getTagList("rewards", Constants.NBT.TAG_COMPOUND);

				if (r.isEmpty())
				{
					NBTBase rt = qt.getTag("reward");

					if (rt != null)
					{
						r.appendTag(rt);
					}
				}

				for (int k = 0; k < r.tagCount(); k++)
				{
					NBTTagCompound rt = r.getCompoundTagAt(k);

					if (!rt.hasKey("type") && !rt.hasKey("item"))
					{
						ItemReward reward = new ItemReward(quest);
						reward.id = readID(rt.getInteger("uid"));
						reward.team = rt.getBoolean("team_reward");
						rt.removeTag("uid");
						rt.removeTag("team_reward");
						reward.stack = ItemMissing.read(rt);
						quest.rewards.add(reward);
						continue;
					}

					QuestReward reward = QuestRewardType.createReward(quest, rt.getString("type"));

					if (reward != null)
					{
						reward.id = rt.getInteger("uid");
						reward.readData(rt);
						quest.rewards.add(reward);
					}
				}
			}
		}

		NBTTagList v = nbt.getTagList("variables", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < v.tagCount(); i++)
		{
			QuestVariable variable = new QuestVariable(this);
			NBTTagCompound nbt1 = v.getCompoundTagAt(i);
			variable.id = nbt1.getInteger("uid");
			variable.readData(nbt1);
			variables.add(variable);
		}

		refreshIDMap();

		for (int i = 0; i < chapters.size(); i++)
		{
			QuestChapter chapter = chapters.get(i);
			NBTTagCompound ct = c.getCompoundTagAt(i);
			chapter.readData(ct);

			NBTTagList q = ct.getTagList("quests", Constants.NBT.TAG_COMPOUND);

			for (int j = 0; j < q.tagCount(); j++)
			{
				Quest quest = chapter.quests.get(j);
				NBTTagCompound qt = q.getCompoundTagAt(j);
				quest.readData(qt);
			}
		}

		for (QuestChapter chapter : chapters)
		{
			for (Quest quest : chapter.quests)
			{
				quest.verifyDependencies();
			}
		}
	}

	public final void readDataFull(File folder)
	{
		NBTTagCompound nbt = NBTUtils.readNBT(new File(folder, "file.nbt"));

		if (nbt != null)
		{
			fileVersion = nbt.getInteger("version");
			readData(nbt);
		}

		chapters.clear();
		variables.clear();
		rewardTables.clear();

		Int2ObjectOpenHashMap<NBTTagCompound> questFileCache = new Int2ObjectOpenHashMap<>();

		for (int i : readIndex(new File(folder, "chapters/index.nbt")))
		{
			QuestChapter chapter = new QuestChapter(this);
			chapter.id = i;
			chapters.add(chapter);

			File[] files = new File(folder, "chapters/" + chapter.getCodeString()).listFiles();

			if (files != null && files.length > 0)
			{
				for (File f : files)
				{
					if (!f.getName().equals("chapter.nbt"))
					{
						try
						{
							Quest quest = new Quest(chapter);
							quest.id = Long.decode("#" + f.getName().replace(".nbt", "")).intValue();

							nbt = NBTUtils.readNBT(new File(folder, "chapters/" + chapter.getCodeString() + "/" + quest.getCodeString() + ".nbt"));

							if (nbt != null)
							{
								questFileCache.put(quest.id, nbt);
								NBTTagList t = nbt.getTagList("tasks", Constants.NBT.TAG_COMPOUND);

								for (int k = 0; k < t.tagCount(); k++)
								{
									NBTTagCompound tt = t.getCompoundTagAt(k);
									QuestTask task = QuestTaskType.createTask(quest, tt.getString("type"));

									if (task != null)
									{
										task.id = tt.getInteger("uid");
										quest.tasks.add(task);
									}
								}

								NBTTagList r = nbt.getTagList("rewards", Constants.NBT.TAG_COMPOUND);

								for (int k = 0; k < r.tagCount(); k++)
								{
									NBTTagCompound rt = r.getCompoundTagAt(k);
									QuestReward reward = QuestRewardType.createReward(quest, rt.getString("type"));

									if (reward != null)
									{
										reward.id = rt.getInteger("uid");
										quest.rewards.add(reward);
									}
								}

								chapter.quests.add(quest);
							}
						}
						catch (Exception ex)
						{
							FTBQuests.LOGGER.warn("Failed to read quest ID " + f.getName());
						}
					}
				}
			}
		}

		for (int i : readIndex(new File(folder, "variables/index.nbt")))
		{
			QuestVariable variable = new QuestVariable(this);
			variable.id = i;
			variables.add(variable);
		}

		for (int i : readIndex(new File(folder, "reward_tables/index.nbt")))
		{
			RewardTable table = new RewardTable(this);
			table.id = i;
			rewardTables.add(table);
		}

		refreshIDMap();

		for (QuestChapter chapter : chapters)
		{
			nbt = NBTUtils.readNBT(new File(folder, "chapters/" + chapter.getCodeString() + "/chapter.nbt"));

			if (nbt != null)
			{
				chapter.readData(nbt);
			}

			for (Quest quest : chapter.quests)
			{
				nbt = questFileCache.get(quest.id);

				if (nbt == null)
				{
					continue;
				}

				quest.readData(nbt);

				NBTTagList t = nbt.getTagList("tasks", Constants.NBT.TAG_COMPOUND);

				for (int k = 0; k < t.tagCount(); k++)
				{
					NBTTagCompound tt = t.getCompoundTagAt(k);
					QuestTask task = getTask(tt.getInteger("uid"));

					if (task != null)
					{
						task.readData(tt);
					}
				}

				NBTTagList r = nbt.getTagList("rewards", Constants.NBT.TAG_COMPOUND);

				for (int k = 0; k < r.tagCount(); k++)
				{
					NBTTagCompound rt = r.getCompoundTagAt(k);
					QuestReward reward = getReward(rt.getInteger("uid"));

					if (reward != null)
					{
						reward.readData(rt);
					}
				}
			}
		}

		for (QuestVariable variable : variables)
		{
			nbt = NBTUtils.readNBT(new File(folder, "variables/" + variable.getCodeString() + ".nbt"));

			if (nbt != null)
			{
				variable.readData(nbt);
			}
		}

		for (RewardTable table : rewardTables)
		{
			nbt = NBTUtils.readNBT(new File(folder, "reward_tables/" + table.getCodeString() + ".nbt"));

			if (nbt != null)
			{
				table.readData(nbt);
			}
		}

		for (QuestChapter chapter : chapters)
		{
			for (Quest quest : chapter.quests)
			{
				quest.verifyDependencies();
			}
		}
	}

	@Override
	public final void writeNetData(DataOut data)
	{
		super.writeNetData(data);
		data.writeCollection(emergencyItems, DataOut.ITEM_STACK);
		data.writeVarLong(emergencyItemsCooldown.ticks());
		data.writeBoolean(defaultRewardTeam);
		data.writeBoolean(defaultConsumeItems);
		data.write(defaultShape, EnumQuestShape.NAME_MAP);

		data.writeBoolean(dropLootCrates);
		lootCrateNoDrop.writeNetData(data);
	}

	@Override
	public final void readNetData(DataIn data)
	{
		super.readNetData(data);
		data.readCollection(emergencyItems, DataIn.ITEM_STACK);
		emergencyItemsCooldown = Ticks.get(data.readVarLong());
		defaultRewardTeam = data.readBoolean();
		defaultConsumeItems = data.readBoolean();
		defaultShape = data.read(EnumQuestShape.NAME_MAP);
		dropLootCrates = data.readBoolean();
		lootCrateNoDrop.readNetData(data);
	}

	public final void writeNetDataFull(DataOut data)
	{
		int pos = data.getPosition();
		writeNetData(data);

		data.writeVarInt(rewardTables.size());

		for (RewardTable table : rewardTables)
		{
			data.writeInt(table.id);
		}

		data.writeVarInt(chapters.size());

		ForgeRegistry<QuestTaskType> taskTypes = QuestTaskType.getRegistry();
		ForgeRegistry<QuestRewardType> rewardTypes = QuestRewardType.getRegistry();

		for (QuestChapter chapter : chapters)
		{
			data.writeInt(chapter.id);
			data.writeVarInt(chapter.quests.size());

			for (Quest quest : chapter.quests)
			{
				data.writeInt(quest.id);
				data.writeVarInt(quest.tasks.size());

				for (QuestTask task : quest.tasks)
				{
					data.writeVarInt(taskTypes.getID(task.getType()));
					data.writeInt(task.id);
				}

				data.writeVarInt(quest.rewards.size());

				for (QuestReward reward : quest.rewards)
				{
					data.writeVarInt(rewardTypes.getID(reward.getType()));
					data.writeInt(reward.id);
				}
			}
		}

		data.writeVarInt(variables.size());

		for (QuestVariable variable : variables)
		{
			data.writeInt(variable.id);
		}

		for (RewardTable table : rewardTables)
		{
			table.writeNetData(data);
		}

		for (QuestChapter chapter : chapters)
		{
			chapter.writeNetData(data);

			for (Quest quest : chapter.quests)
			{
				quest.writeNetData(data);

				for (QuestTask task : quest.tasks)
				{
					task.writeNetData(data);
				}

				for (QuestReward reward : quest.rewards)
				{
					reward.writeNetData(data);
				}
			}
		}

		for (QuestVariable variable : variables)
		{
			variable.writeNetData(data);
		}

		if (FTBLibConfig.debugging.print_more_info)
		{
			FTBQuests.LOGGER.info("Wrote " + (data.getPosition() - pos) + " bytes");
		}
	}

	public final void readNetDataFull(DataIn data)
	{
		int pos = data.getPosition();
		readNetData(data);

		chapters.clear();
		variables.clear();
		rewardTables.clear();

		int rtl = data.readVarInt();

		for (int i = 0; i < rtl; i++)
		{
			RewardTable table = new RewardTable(this);
			table.id = data.readInt();
			rewardTables.add(table);
		}

		ForgeRegistry<QuestTaskType> taskTypes = QuestTaskType.getRegistry();
		ForgeRegistry<QuestRewardType> rewardTypes = QuestRewardType.getRegistry();

		int c = data.readVarInt();

		for (int i = 0; i < c; i++)
		{
			QuestChapter chapter = new QuestChapter(this);
			chapter.id = data.readInt();
			chapters.add(chapter);

			int q = data.readVarInt();

			for (int j = 0; j < q; j++)
			{
				Quest quest = new Quest(chapter);
				quest.id = data.readInt();
				chapter.quests.add(quest);

				int t = data.readVarInt();

				for (int k = 0; k < t; k++)
				{
					QuestTaskType type = taskTypes.getValue(data.readVarInt());
					QuestTask task = type.provider.create(quest);
					task.id = data.readInt();
					quest.tasks.add(task);
				}

				int r = data.readVarInt();

				for (int k = 0; k < r; k++)
				{
					QuestRewardType type = rewardTypes.getValue(data.readVarInt());
					QuestReward reward = type.provider.create(quest);
					reward.id = data.readInt();
					quest.rewards.add(reward);
				}
			}
		}

		int v = data.readVarInt();

		for (int i = 0; i < v; i++)
		{
			QuestVariable variable = new QuestVariable(this);
			variable.id = data.readInt();
			variables.add(variable);
		}

		refreshIDMap();

		for (RewardTable table : rewardTables)
		{
			table.readNetData(data);
		}

		for (QuestChapter chapter : chapters)
		{
			chapter.readNetData(data);

			for (Quest quest : chapter.quests)
			{
				quest.readNetData(data);

				for (QuestTask task : quest.tasks)
				{
					task.readNetData(data);
				}

				for (QuestReward reward : quest.rewards)
				{
					reward.readNetData(data);
				}
			}
		}

		for (QuestVariable variable : variables)
		{
			variable.readNetData(data);
		}

		if (FTBLibConfig.debugging.print_more_info)
		{
			FTBQuests.LOGGER.info("Read " + (data.getPosition() - pos) + " bytes");
		}
	}

	@Nullable
	public abstract ITeamData getData(short team);

	@Nullable
	public abstract ITeamData getData(String team);

	public abstract Collection<? extends ITeamData> getAllData();

	public abstract void deleteObject(int id);

	@Override
	public Icon getAltIcon()
	{
		List<Icon> list = new ArrayList<>();

		for (QuestChapter chapter : chapters)
		{
			list.add(chapter.getIcon());
		}

		return IconAnimation.fromList(list, false);
	}

	@Override
	public ITextComponent getAltDisplayName()
	{
		return new TextComponentTranslation("ftbquests.file");
	}

	@Override
	public void getConfig(ConfigGroup config)
	{
		super.getConfig(config);
		config.addList("emergency_items", emergencyItems, new ConfigItemStack(ItemStack.EMPTY), ConfigItemStack::new, ConfigItemStack::getStack);

		config.add("emergency_items_cooldown", new ConfigTimer(Ticks.NO_TICKS)
		{
			@Override
			public Ticks getTimer()
			{
				return emergencyItemsCooldown;
			}

			@Override
			public void setTimer(Ticks t)
			{
				emergencyItemsCooldown = t;
			}
		}, new ConfigTimer(Ticks.MINUTE.x(5)));

		config.addBool("drop_loot_crates", () -> dropLootCrates, v -> dropLootCrates = v, false);

		ConfigGroup defaultsGroup = config.getGroup("defaults");
		defaultsGroup.addBool("reward_team", () -> defaultRewardTeam, v -> defaultRewardTeam = v, false);
		defaultsGroup.addBool("consume_items", () -> defaultConsumeItems, v -> defaultConsumeItems = v, false);
		defaultsGroup.addEnum("quest_shape", () -> defaultShape, v -> defaultShape = v, EnumQuestShape.NAME_MAP);

		ConfigGroup d = config.getGroup("loot_crate_no_drop");
		d.addInt("passive", () -> lootCrateNoDrop.passive, v -> lootCrateNoDrop.passive = v, 0, 0, Integer.MAX_VALUE).setDisplayName(new TextComponentTranslation("ftbquests.loot.entitytype.passive"));
		d.addInt("monster", () -> lootCrateNoDrop.monster, v -> lootCrateNoDrop.monster = v, 0, 0, Integer.MAX_VALUE).setDisplayName(new TextComponentTranslation("ftbquests.loot.entitytype.monster"));
		d.addInt("boss", () -> lootCrateNoDrop.boss, v -> lootCrateNoDrop.boss = v, 0, 0, Integer.MAX_VALUE).setDisplayName(new TextComponentTranslation("ftbquests.loot.entitytype.boss"));
	}

	@Override
	public void clearCachedData()
	{
		super.clearCachedData();

		for (QuestChapter chapter : chapters)
		{
			chapter.clearCachedData();
		}
	}

	public int readID(int id)
	{
		while (id == 0 || id == 1 || map.get(id) != null)
		{
			id = MathUtils.RAND.nextInt();
		}

		return id;
	}

	public int getID(String id)
	{
		if (id.isEmpty())
		{
			return 0;
		}
		else if (id.charAt(0) == '*')
		{
			return 1;
		}

		try
		{
			return Long.valueOf(id.charAt(0) == '#' ? id.substring(1) : id, 16).intValue();
		}
		catch (Exception ex)
		{
			return 0;
		}
	}

	@Nullable
	public LootCrate getRandomLootCrate(Entity entity, Random random)
	{
		int totalWeight = lootCrateNoDrop.getWeight(entity);

		for (RewardTable table : rewardTables)
		{
			if (table.lootCrate != null)
			{
				totalWeight += table.lootCrate.drops.getWeight(entity);
			}
		}

		if (totalWeight <= 0)
		{
			return null;
		}

		int number = random.nextInt(totalWeight) + 1;
		int currentWeight = lootCrateNoDrop.getWeight(entity);

		if (currentWeight < number)
		{
			for (RewardTable table : rewardTables)
			{
				if (table.lootCrate != null)
				{
					currentWeight += table.lootCrate.drops.getWeight(entity);

					if (currentWeight >= number)
					{
						return table.lootCrate;
					}
				}
			}
		}

		return null;
	}

	@Override
	public final int refreshJEI()
	{
		return 255;
	}
}