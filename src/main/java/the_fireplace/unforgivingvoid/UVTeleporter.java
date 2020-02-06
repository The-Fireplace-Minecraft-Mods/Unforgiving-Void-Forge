package the_fireplace.unforgivingvoid;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;

public class UVTeleporter implements ITeleporter {

    private final BlockPos targetPos;

    public UVTeleporter(BlockPos targetPos)
    {
        this.targetPos = targetPos;
    }

    @Override
    public void placeEntity(World world, Entity entity, float yaw) {
        entity.moveToBlockPosAndAngles(targetPos, yaw, entity.rotationPitch);
    }

    //Technically the super will also always evaluate to false but this is better for performance
    @Override
    public boolean isVanilla() {
        return false;
    }
}
