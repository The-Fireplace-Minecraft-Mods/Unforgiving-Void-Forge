package the_fireplace.unforgivingvoid;

import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockPortal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

@Mod(modid = UnforgivingVoid.MODID, name = UnforgivingVoid.MODNAME, acceptedMinecraftVersions = "[1.12,1.13)", acceptableRemoteVersions = "*", updateJSON = "https://bitbucket.org/The_Fireplace/minecraft-mod-updates/raw/master/lfv.json")
@Mod.EventBusSubscriber
public class UnforgivingVoid {

	public static final String MODID = "unforgivingvoid";
	public static final String MODNAME = "Unforgiving Void";

	private static Logger LOGGER = FMLLog.log;

	@CapabilityInject(UnforgivingVoidCapability.class)
	public static final Capability<UnforgivingVoidCapability> UNFORGIVING_VOID_CAP = null;
	private static final ResourceLocation unforgiving_void_cap_res = new ResourceLocation(MODID, "unforgiving_void_cap");

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		LOGGER = event.getModLog();
		CapabilityManager.INSTANCE.register(UnforgivingVoidCapability.class, new UnforgivingVoidCapability.Storage(), UnforgivingVoidCapability.Default::new);
	}

	@SubscribeEvent
	public static void attachPlayerCaps(AttachCapabilitiesEvent<Entity> e){
		if(e.getObject() instanceof EntityPlayer) {
			//noinspection ConstantConditions
			assert UNFORGIVING_VOID_CAP != null;
			//noinspection rawtypes
			e.addCapability(unforgiving_void_cap_res, new ICapabilitySerializable() {
				UnforgivingVoidCapability inst = UNFORGIVING_VOID_CAP.getDefaultInstance();

				@Override
				public NBTBase serializeNBT() {
					return UNFORGIVING_VOID_CAP.getStorage().writeNBT(UNFORGIVING_VOID_CAP, inst, null);
				}

				@Override
				public void deserializeNBT(NBTBase nbt) {
					UNFORGIVING_VOID_CAP.getStorage().readNBT(UNFORGIVING_VOID_CAP, inst, null, nbt);
				}

				@Override
				public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
					return capability == UNFORGIVING_VOID_CAP;
				}

				@SuppressWarnings("Duplicates")
				@Nullable
				@Override
				public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
					//noinspection unchecked
					return capability == UNFORGIVING_VOID_CAP ? (T) inst : null;
				}
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		boolean doTeleport = ArrayUtils.contains(ConfigValues.dimension_list, "*");
		if(event.side == Side.SERVER && event.phase == TickEvent.Phase.START)
			for (String dim : ConfigValues.dimension_list)
				try{
					if(event.player.getEntityWorld().provider.getDimension() == Integer.parseInt(dim)) {
						doTeleport = !doTeleport;
						break;
					}
				}catch(NumberFormatException e) {
					if(!dim.equals("*") && event.player.getEntityWorld().provider.getDimensionType().getName().toLowerCase().equals(dim.toLowerCase())){
						doTeleport = !doTeleport;
						break;
					}
				}

		if(doTeleport && event.player.getPosition().getY() <= ConfigValues.triggerAtY) {
			event.player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 3));
			Random rand = new Random();
			BlockPos spawnPos = new BlockPos(event.player.getPosition().getX()/8 - ConfigValues.baseDistanceOffset + rand.nextInt(ConfigValues.baseDistanceOffset * 2), rand.nextInt(100)+16, event.player.getPosition().getZ()/8 - ConfigValues.baseDistanceOffset + rand.nextInt(ConfigValues.baseDistanceOffset * 2));
			if(event.player.changeDimension(-1, new UVTeleporter(spawnPos)) != null && event.player.dimension == -1) {
				int expandMult = 1;
				while(event.player.world.getBlockState(spawnPos).isNormalCube()){
					spawnPos = new BlockPos(event.player.getPosition().getX()/8 - ConfigValues.baseDistanceOffset * expandMult + rand.nextInt(ConfigValues.baseDistanceOffset * 2 * expandMult), rand.nextInt(100)+16, event.player.getPosition().getZ()/8 - ConfigValues.baseDistanceOffset * expandMult + rand.nextInt(ConfigValues.baseDistanceOffset * 2 * expandMult));
					event.player.setPortal(spawnPos);
					expandMult++;
				}
				event.player.setPositionAndUpdate(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
				//Even though the player doesn't spawn a portal by doing this, their sudden appearance should destroy any existing portal in the spot they appear.
				for(int x=event.player.getPosition().getX()-2;x<=event.player.getPosition().getX()+2;x++){
					for(int y=event.player.getPosition().getY()-3;y<=event.player.getPosition().getY()+3;y++){
						for(int z=event.player.getPosition().getZ()-2;z<=event.player.getPosition().getZ()+2;z++){
							if(event.player.world.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BlockPortal) {
								event.player.world.setBlockToAir(new BlockPos(x, y, z));
								event.player.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 2.0f);
							}
						}
					}
				}

				if(ConfigValues.dropObsidian)
					event.player.world.spawnEntity(new EntityItem(event.player.world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), new ItemStack(Blocks.OBSIDIAN, 10+rand.nextInt(4))));
				if(!event.player.isCreative() && !event.player.isSpectator())
					getUnforgivingVoidCap(event.player).setFallingFromTravel(true);

				if(ConfigValues.saveFromLava) {
					while (event.player.world.getBlockState(spawnPos).getBlock() instanceof BlockAir)
						spawnPos = spawnPos.down();
					if (event.player.world.getBlockState(spawnPos).getBlock() == Blocks.LAVA || event.player.world.getBlockState(spawnPos).getBlock() == Blocks.FLOWING_LAVA) {
						event.player.getEntityWorld().setBlockState(spawnPos, Blocks.OBSIDIAN.getDefaultState());
						event.player.getEntityWorld().setBlockState(spawnPos.north(), Blocks.OBSIDIAN.getDefaultState());
						event.player.getEntityWorld().setBlockState(spawnPos.south(), Blocks.OBSIDIAN.getDefaultState());
						event.player.getEntityWorld().setBlockState(spawnPos.east(), Blocks.OBSIDIAN.getDefaultState());
						event.player.getEntityWorld().setBlockState(spawnPos.west(), Blocks.OBSIDIAN.getDefaultState());
						event.player.getEntityWorld().setBlockState(spawnPos.north().east(), Blocks.OBSIDIAN.getDefaultState());
						event.player.getEntityWorld().setBlockState(spawnPos.north().west(), Blocks.OBSIDIAN.getDefaultState());
						event.player.getEntityWorld().setBlockState(spawnPos.south().east(), Blocks.OBSIDIAN.getDefaultState());
						event.player.getEntityWorld().setBlockState(spawnPos.south().west(), Blocks.OBSIDIAN.getDefaultState());
					}
				}
			} else if(!event.player.world.isRemote && event.player.isEntityAlive())
				LOGGER.warn("Error: Unable to teleport player to the Nether from the void. Unfortunately, the player will probably die. If this happens, please report it.");
		}
	}

	public static UnforgivingVoidCapability getUnforgivingVoidCap(EntityPlayer player) {
		//noinspection ConstantConditions
		return player.getCapability(UNFORGIVING_VOID_CAP, null);
	}

	@SubscribeEvent
	public static void onPlayerFall(LivingFallEvent event) {
		if(event.getEntity() instanceof EntityPlayer && !event.getEntity().getEntityWorld().isRemote) {
			if(getUnforgivingVoidCap((EntityPlayer) event.getEntity()).getFallingFromTravel()) {
				float damage = ConfigValues.damageOnFall;
				if(ConfigValues.preventDeath && event.getEntityLiving().getHealth() - damage <= 0)
					damage = event.getEntityLiving().getHealth() - 1f;
				event.getEntity().attackEntityFrom(DamageSource.FALL, damage);
				event.setDamageMultiplier(0f);
				event.setCanceled(true);
				getUnforgivingVoidCap((EntityPlayer) event.getEntity()).setFallingFromTravel(false);
			}
		}
	}

	@Config(modid = MODID)
	public static class ConfigValues {
		@Config.Comment("The y level at which Unforgiving Void should send the player to the Nether.")
		@Config.RangeInt(min = -64, max = 0)
		@Config.LangKey("trigger_at_y")
		public static int triggerAtY = -32;

		@Config.Comment("The amount of damage applied to the player when they land.")
		@Config.RangeInt(min = 0, max = 40)
		@Config.LangKey("damage_on_fall")
		public static int damageOnFall = 19;

		@Config.Comment("Prevent death by fall damage when you fall to the Nether (limits damage to leave at least 0.5 hearts)")
		@Config.LangKey("prevent_death")
		public static boolean preventDeath = true;

		@Config.Comment("Drop obsidian for the player when they appear in the Nether")
		@Config.LangKey("drop_obsidian")
		public static boolean dropObsidian = true;

		@Config.Comment("Save the player from lava when they appear in the Nether")
		@Config.LangKey("save_from_lava")
		public static boolean saveFromLava = false;

		@Config.Comment("The base offset from which the player will spawn from after falling through the void. This is not going to be the exact offset. Height is random.")
		@Config.RangeInt(min = 128, max = 8192)
		@Config.LangKey("base_distance_offset")
		public static int baseDistanceOffset = 512;

		@Config.Comment("This is the Dimension Filter. If it contains *, it is a blacklist. Otherwise, it is a whitelist.")
		@Config.LangKey("dimension_list")
		public static String[] dimension_list = new String[]{"*"};
	}
}
