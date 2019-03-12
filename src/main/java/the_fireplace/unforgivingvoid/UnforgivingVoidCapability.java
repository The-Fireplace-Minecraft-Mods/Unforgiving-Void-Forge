package the_fireplace.unforgivingvoid;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public interface UnforgivingVoidCapability {

    boolean getFallingFromTravel();

    void setFallingFromTravel(boolean fallingFromTravel);

    class Default implements UnforgivingVoidCapability {
        private boolean isFallingFromTravel;

        public Default() {
            isFallingFromTravel = false;
        }

        @Override
        public void setFallingFromTravel(boolean fallingFromTravel) {
            isFallingFromTravel = fallingFromTravel;
        }

        @Override
        public boolean getFallingFromTravel() {
            return isFallingFromTravel;
        }
    }

    class Storage implements Capability.IStorage<UnforgivingVoidCapability> {

        @Nullable
        @Override
        public NBTBase writeNBT(Capability<UnforgivingVoidCapability> capability, UnforgivingVoidCapability instance, EnumFacing side) {
            return new NBTTagByte(instance.getFallingFromTravel() ? (byte) 1 : 0);
        }

        @Override
        public void readNBT(Capability<UnforgivingVoidCapability> capability, UnforgivingVoidCapability instance, EnumFacing side, NBTBase nbt) {
            if (nbt instanceof NBTTagByte)
                instance.setFallingFromTravel(((NBTTagByte) nbt).getByte() == 1);
        }
    }
}