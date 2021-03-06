package net.torocraft.toroquest.entities;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.torocraft.toroquest.ToroQuest;
import net.torocraft.toroquest.civilization.CivilizationType;
import net.torocraft.toroquest.civilization.player.PlayerCivilizationCapabilityImpl;
import net.torocraft.toroquest.config.ToroQuestConfiguration;
import net.torocraft.toroquest.entities.ai.EntityAINearestAttackableCivTarget;
import net.torocraft.toroquest.entities.render.RenderSentry;

public class EntitySentry extends EntityToroNpc {

	public static String NAME = "sentry";

	static {
		if (ToroQuestConfiguration.specificEntityNames) {
			NAME = ToroQuestEntities.ENTITY_PREFIX + NAME;
		}
	}

	public static void init(int entityId) {
		EntityRegistry.registerModEntity(new ResourceLocation(ToroQuest.MODID, NAME), EntitySentry.class, NAME, entityId, ToroQuest.INSTANCE, 80, 2,
				true, 0x3f3024, 0xe0d6b9);
	}

	public static void registerRenders() {
		RenderingRegistry.registerEntityRenderingHandler(EntitySentry.class, new IRenderFactory<EntitySentry>() {
			@Override
			public Render<EntitySentry> createRenderFor(RenderManager manager) {
				return new RenderSentry(manager);
			}
		});
	}

	public EntitySentry(World world, CivilizationType civ) {
		super(world, civ);
	}

	public EntitySentry(World world) {
		super(world, null);
	}

	@Override
	protected boolean canDespawn() {
		return true;
	}

	protected void initEntityAI() {
		tasks.addTask(0, new EntityAISwimming(this));
		tasks.addTask(2, new EntityAIAttackMelee(this, 0.5D, false));
		tasks.addTask(7, new EntityAIWander(this, 0.35D));
		tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
		tasks.addTask(8, new EntityAILookIdle(this));

		targetTasks.addTask(2, new EntityAINearestAttackableCivTarget(this));
		targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityMob>(this, EntityMob.class, 2, false, false, new Predicate<EntityMob>() {
			@Override
			public boolean apply(EntityMob target) {
				return !(target instanceof EntityCreeper);
			}
		}));
	}

	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(35.0D);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
		// TODO call for backup
		// this.getAttributeMap().registerAttribute(SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(this.rand.nextDouble()
		// *
		// net.minecraftforge.common.ForgeModContainer.zombieSummonBaseChance);
	}

	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (!super.attackEntityFrom(source, amount)) {
			return false;
		}

		EntityLivingBase attacker = this.getAttackTarget();
		if (attacker == null && source.getTrueSource() instanceof EntityLivingBase) {
			setAttackTarget((EntityLivingBase) source.getTrueSource());
			callForHelp((EntityLivingBase) source.getTrueSource());
		}
		return true;
	}

	public boolean attackEntityAsMob(Entity victum) {
		super.attackEntityAsMob(victum);
		removeTargetIfNotFoe(victum);
		return true;
	}

	private void removeTargetIfNotFoe(Entity victum) {
		if (victum instanceof EntityPlayer) {
			if (!isFoe((EntityPlayer) victum)) {
				setAttackTarget(null);
			}
		}
	}

	protected boolean isFoe(EntityPlayer target) {
		EntityToroNpc npc = (EntityToroNpc) this;
		CivilizationType civ = npc.getCivilization();
		if (civ == null) {
			return false;
		}
		int rep = PlayerCivilizationCapabilityImpl.get(target).getReputation(civ);
		return rep < -100;
	}

	@Nullable
	public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
		livingdata = super.onInitialSpawn(difficulty, livingdata);

		setCanPickUpLoot(true);
		setEquipmentBasedOnDifficulty(difficulty);
		setEnchantmentBasedOnDifficulty(difficulty);

		setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD, 1));

		addArmor();

		return livingdata;
	}

	protected void addArmor() {
		setItemStackToSlot(EntityEquipmentSlot.HEAD, colorArmor(new ItemStack(Items.LEATHER_HELMET, 1)));
		setItemStackToSlot(EntityEquipmentSlot.FEET, colorArmor(new ItemStack(Items.LEATHER_BOOTS, 1)));
		setItemStackToSlot(EntityEquipmentSlot.LEGS, colorArmor(new ItemStack(Items.LEATHER_LEGGINGS, 1)));
		setItemStackToSlot(EntityEquipmentSlot.CHEST, colorArmor(new ItemStack(Items.LEATHER_CHESTPLATE, 1)));
	}

	protected ItemStack colorArmor(ItemStack stack) {
		if (getCivilization() == null) {
			return stack;
		}
		ItemArmor armor = (ItemArmor) stack.getItem();
		armor.setColor(stack, determineColorByCiv());
		return stack;
	}

	private int determineColorByCiv() {
		int color = 0;
		switch (getCivilization()) {
		case EARTH:
			color = 6717235;
			break;
		case FIRE:
			color = 0xff9900;
			break;
		case MOON:
			color = 0x333333;
			break;
		case SUN:
			color = 0xffff00;
			break;
		case WATER:
			color = 0x2B65EC;
			break;
		case WIND:
			color = 0xffffff;
			break;
		}
		return color;
	}

}
