package the_fireplace.unforgivingvoid;

import com.google.common.collect.Lists;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@SuppressWarnings("WeakerAccess")
@MethodsReturnNonnullByDefault
@Mod(UnforgivingVoid.MODID)
public class UnforgivingVoid {
	public static final String MODID = "unforgivingvoid";
	private static Logger LOGGER = LogManager.getLogger(MODID);

	@CapabilityInject(UnforgivingVoidCapability.class)
	public static final Capability<UnforgivingVoidCapability> UNFORGIVING_VOID_CAP = null;
	private static final ResourceLocation unforgiving_void_cap_res = new ResourceLocation(MODID, "unforgiving_void_cap");

	public UnforgivingVoid() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, cfg.COMMON_SPEC);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverConfig);
	}

	@SuppressWarnings("unused")
	public void preInit(FMLCommonSetupEvent event) {
		CapabilityManager.INSTANCE.register(UnforgivingVoidCapability.class, new UnforgivingVoidCapability.Storage(), UnforgivingVoidCapability.Default::new);
		MinecraftForge.EVENT_BUS.register(this);
	}

	public void serverConfig(ModConfig.ModConfigEvent event) {
		if (event.getConfig().getType() == ModConfig.Type.COMMON)
			cfg.load();
	}

	@SubscribeEvent
	public void attachPlayerCaps(AttachCapabilitiesEvent<Entity> e){
		if(e.getObject() instanceof PlayerEntity) {
			//noinspection ConstantConditions
			assert UNFORGIVING_VOID_CAP != null;
			e.addCapability(unforgiving_void_cap_res, new ICapabilitySerializable() {
				UnforgivingVoidCapability inst = UNFORGIVING_VOID_CAP.getDefaultInstance();

				@Override
				public INBT serializeNBT() {
					return UNFORGIVING_VOID_CAP.getStorage().writeNBT(UNFORGIVING_VOID_CAP, inst, null);
				}

				@Override
				public void deserializeNBT(INBT nbt) {
					UNFORGIVING_VOID_CAP.getStorage().readNBT(UNFORGIVING_VOID_CAP, inst, null, nbt);
				}

				@Override
				public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
					//noinspection unchecked
					return capability.getName().equals(UnforgivingVoidCapability.class.getCanonicalName()) ? LazyOptional.of(() -> (T) inst) : LazyOptional.empty();
				}
			});
		}
	}

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		boolean doTeleport = cfg.dimensionFilter.contains("*");
		if(event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START)
			for (String dim : cfg.dimensionFilter)
				try{
					if(event.player.getEntityWorld().getDimension().getType().getId() == Integer.parseInt(dim)) {
						doTeleport = !doTeleport;
						break;
					}
				}catch(NumberFormatException e){
					if(!dim.equals("*") && (Objects.requireNonNull(event.player.getEntityWorld().getDimension().getType().getRegistryName()).toString().toLowerCase().equals(dim.toLowerCase()) || event.player.getEntityWorld().getDimension().getType().getRegistryName().getPath().toLowerCase().equals(dim.toLowerCase()))){
						doTeleport = !doTeleport;
						break;
					}
				}

		if(doTeleport && event.player.getPosition().getY() <= cfg.triggerAtY) {
			event.player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 60, 3));
			Random rand = new Random();
			BlockPos spawnPos = new BlockPos(event.player.getPosition().getX()/8 - cfg.baseDistanceOffset + rand.nextInt(cfg.baseDistanceOffset * 2), rand.nextInt(100)+16, event.player.getPosition().getZ()/8 - cfg.baseDistanceOffset + rand.nextInt(cfg.baseDistanceOffset * 2));
			event.player.setPortal(spawnPos);
			if(event.player.changeDimension(DimensionType.THE_NETHER) != null && event.player.dimension.equals(DimensionType.THE_NETHER)) {
				int expandMult = 1;
				while(event.player.world.getBlockState(spawnPos).isNormalCube(event.player.world, spawnPos)){
					spawnPos = new BlockPos(event.player.getPosition().getX()/8 - cfg.baseDistanceOffset * expandMult + rand.nextInt(cfg.baseDistanceOffset * 2 * expandMult), rand.nextInt(100)+16, event.player.getPosition().getZ()/8 - cfg.baseDistanceOffset * expandMult + rand.nextInt(cfg.baseDistanceOffset * 2 * expandMult));
					event.player.setPortal(spawnPos);
					expandMult++;
				}
				event.player.setPositionAndUpdate(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
				//Even though the player doesn't spawn a portal by doing this, their sudden appearance should destroy any existing portal in the spot they appear.
				for(int x=event.player.getPosition().getX()-2;x<=event.player.getPosition().getX()+2;x++){
					for(int y=event.player.getPosition().getY()-3;y<=event.player.getPosition().getY()+3;y++){
						for(int z=event.player.getPosition().getZ()-2;z<=event.player.getPosition().getZ()+2;z++){
							if(event.player.world.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof NetherPortalBlock) {
								event.player.world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
								event.player.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 2.0f);
							}
						}
					}
				}

				if(cfg.dropObsidian)
					event.player.world.addEntity(new ItemEntity(event.player.world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), new ItemStack(Blocks.OBSIDIAN, 10+rand.nextInt(4))));
				if(!event.player.isCreative() && !event.player.isSpectator())
					getUnforgivingVoidCap(event.player).setFallingFromTravel(true);

				if(cfg.lavaPlatform) {
					while (event.player.world.getBlockState(spawnPos).getBlock() instanceof AirBlock)
						spawnPos = spawnPos.down();
					if (event.player.world.getBlockState(spawnPos).getBlock() == Blocks.LAVA) {
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
			} else if(!event.player.world.isRemote && event.player.isAlive())
				LOGGER.warn("Error: Unable to teleport player to the Nether from the void. Unfortunately, the player will die. If this happens, please report it.");
		}
	}

	public static UnforgivingVoidCapability getUnforgivingVoidCap(PlayerEntity player) {
		//noinspection ConstantConditions
		return player.getCapability(UNFORGIVING_VOID_CAP).orElseThrow(() -> new IllegalStateException("Unforgiving Void Capability is not present for a player!"));
	}

	@SubscribeEvent
	public void onPlayerFall(LivingFallEvent event) {
		if(event.getEntityLiving() instanceof PlayerEntity && event.getEntityLiving().isServerWorld()) {
			if(getUnforgivingVoidCap((PlayerEntity) event.getEntityLiving()).getFallingFromTravel()) {
				float damage = cfg.damageOnFall;
				if(cfg.preventFallingDeath && event.getEntityLiving().getHealth() - damage <= 0)
					damage = event.getEntityLiving().getHealth() - 1f;
				event.getEntity().attackEntityFrom(DamageSource.FALL, damage);
				event.setDamageMultiplier(0f);
				event.setCanceled(true);
				getUnforgivingVoidCap((PlayerEntity) event.getEntity()).setFallingFromTravel(false);
			}
		}
	}

	public static class cfg {
		public static final ServerConfig COMMON;
		public static final ForgeConfigSpec COMMON_SPEC;
		static {
			final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
			COMMON_SPEC = specPair.getRight();
			COMMON = specPair.getLeft();
		}

		public static int triggerAtY;
		public static int damageOnFall;
		public static boolean preventFallingDeath;
		public static boolean dropObsidian;
		public static boolean lavaPlatform;
		public static int baseDistanceOffset;
		public static List<String> dimensionFilter;

		public static void load() {
			triggerAtY = COMMON.triggerAtY.get();
			damageOnFall = COMMON.damageOnFall.get();
			preventFallingDeath = COMMON.preventFallingDeath.get();
			dropObsidian = COMMON.dropObsidian.get();
			lavaPlatform = COMMON.lavaPlatform.get();
			baseDistanceOffset = COMMON.baseDistanceOffset.get();
			dimensionFilter = COMMON.dimensionFilter.get();
		}

		public static class ServerConfig {
			//General clan config
			public ForgeConfigSpec.IntValue triggerAtY;
			public ForgeConfigSpec.IntValue damageOnFall;
			public ForgeConfigSpec.BooleanValue preventFallingDeath;
			public ForgeConfigSpec.BooleanValue dropObsidian;
			public ForgeConfigSpec.BooleanValue lavaPlatform;
			public ForgeConfigSpec.IntValue baseDistanceOffset;
			public ForgeConfigSpec.ConfigValue<List<String>> dimensionFilter;

			ServerConfig(ForgeConfigSpec.Builder builder) {
				builder.push("general");
				triggerAtY = builder
						.comment("The y level at which Unforgiving Void should send the player to the Nether.")
						.translation("Trigger At Y")
						.defineInRange("triggerAtY", -32, Integer.MIN_VALUE, Integer.MAX_VALUE);
				damageOnFall = builder
						.comment("The amount of damage applied to the player when they land in the Nether.")
						.translation("Damage On Fall")
						.defineInRange("damageOnFall", 19, 0, Integer.MAX_VALUE);
				preventFallingDeath = builder
						.comment("Prevent death by fall damage when you fall to the Nether (limits damage to leave at least 0.5 hearts)")
						.translation("Prevent Falling Death")
						.define("preventFallingDeath", true);
				dropObsidian = builder
						.comment("Drop obsidian for the player when they appear in the Nether.")
						.translation("Drop Obsidian")
						.define("dropObsidian", true);
				lavaPlatform = builder
						.comment("Attempt to place a platform for the player when they appear in the Nether above lava.")
						.translation("Lava Platform")
						.define("lavaPlatform", false);
				baseDistanceOffset = builder
						.comment("The base offset from which the player will spawn from after falling through the void. This is not going to be the exact offset. Height is random.")
						.translation("Base Distance Offset")
						.defineInRange("baseDistanceOffset", 512, 1, Integer.MAX_VALUE);
				dimensionFilter = builder
						.comment("This is the Dimension Filter. It specifies which dimensions Unforgiving Void is active in. If it contains *, it is a blacklist. Otherwise, it is a whitelist.")
						.translation("Dimension Filter")
						.define("dimensionFilter", Lists.newArrayList("*"));
				builder.pop();
			}
		}
	}
}
