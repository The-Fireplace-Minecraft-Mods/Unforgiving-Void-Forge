package net.blay09.mods.forgivingvoid;

import net.minecraft.block.BlockPortal;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Random;

@Mod(modid = ForgivingVoid.MOD_ID, name = "Less Forgiving Void", acceptedMinecraftVersions = "[1.10.2]")
@Mod.EventBusSubscriber
public class ForgivingVoid {

	public static final String MOD_ID = "forgivingvoid";

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if(event.side == Side.SERVER && event.phase == TickEvent.Phase.START) {
			if(event.player.posY < 0) {
				event.player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 3));
				Random rand = new Random();
				BlockPos spawnPos = new BlockPos(event.player.posX/8-ModConfig.baseDistanceOffset *2+rand.nextInt(ModConfig.baseDistanceOffset *4), rand.nextInt(100)+16, event.player.posZ/8-ModConfig.baseDistanceOffset *2+rand.nextInt(ModConfig.baseDistanceOffset *4));
				event.player.setPortal(spawnPos);
				if(event.player.changeDimension(-1) != null) {
					int expandMult = 1;
					while(event.player.worldObj.getBlockState(spawnPos).isNormalCube()){
						spawnPos = new BlockPos(event.player.posX/8-ModConfig.baseDistanceOffset *2*expandMult+rand.nextInt(ModConfig.baseDistanceOffset *4*expandMult), rand.nextInt(100)+16, event.player.posZ/8-ModConfig.baseDistanceOffset *2*expandMult+rand.nextInt(ModConfig.baseDistanceOffset *4*expandMult));
						event.player.setPortal(spawnPos);
						expandMult++;
					}
					event.player.setPositionAndUpdate(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
					//Even though the player doesn't spawn a portal by doing this, their sudden appearance should destroy any existing portal in the spot they appear.
					for(int x=(int)event.player.posX-2;x<=event.player.posX+2;x++){
						for(int y=(int)event.player.posY-3;y<=event.player.posY+3;y++){
							for(int z=(int)event.player.posZ-2;z<=event.player.posZ+2;z++){
								if(event.player.worldObj.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BlockPortal) {
									event.player.worldObj.setBlockToAir(new BlockPos(x, y, z));
									event.player.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 2.0f);
								}
							}
						}
					}

					if(ModConfig.dropObsidian)
						event.player.worldObj.spawnEntityInWorld(new EntityItem(event.player.worldObj, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), new ItemStack(Blocks.OBSIDIAN, 10)));
				}else{
					event.player.setPositionAndUpdate(event.player.posX-ModConfig.baseDistanceOffset *16+rand.nextInt(ModConfig.baseDistanceOffset *32), rand.nextInt(ModConfig.baseDistanceOffset)+ModConfig.baseDistanceOffset /2, event.player.posZ-ModConfig.baseDistanceOffset *16+rand.nextInt(ModConfig.baseDistanceOffset *32));
				}
				event.player.getEntityData().setBoolean("ForgivingVoidNoFallDamage", true);
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerFall(LivingFallEvent event) {
		if(event.getEntity() instanceof EntityPlayer) {
			if(event.getEntity().getEntityData().getBoolean("ForgivingVoidNoFallDamage")) {
				float damage = ModConfig.damageOnFall;
				if(ModConfig.preventDeath && event.getEntityLiving().getHealth() - damage <= 0) {
					damage = event.getEntityLiving().getHealth() - 1f;
				}
				event.getEntity().attackEntityFrom(DamageSource.fall, damage);
				event.setDamageMultiplier(0f);
				event.setCanceled(true);
				event.getEntity().getEntityData().setBoolean("ForgivingVoidNoFallDamage", false);
			}
		}
	}

	@Config(modid = MOD_ID)
	public static class ModConfig {
		@Config.Comment("The amount of damage applied to the player when they land.")
		@Config.RangeInt(min = 0, max = 40)
		public static int damageOnFall = 19;

		@Config.Comment("Prevent death by fall damage when you fall to the nether (limits damage to leave at least 0.5 hearts)")
		public static boolean preventDeath = true;

		@Config.Comment("Drop obsidian for the player when they appear in the Nether")
		public static boolean dropObsidian = true;

		@Config.Comment("The base offset from which the player will spawn from after falling through the void. This is not going to be the exact offset. Height is random.")
		@Config.RangeInt(min = 128, max = 8192)
		public static int baseDistanceOffset = 512;
	}
}
