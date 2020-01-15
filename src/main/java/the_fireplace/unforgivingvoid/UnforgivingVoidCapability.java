package the_fireplace.unforgivingvoid;

import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
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
        public INBT writeNBT(Capability<UnforgivingVoidCapability> capability, UnforgivingVoidCapability instance, Direction side) {
            return instance.getFallingFromTravel() ? ByteNBT.field_229670_c_ : ByteNBT.field_229669_b_;
        }

        @Override
        public void readNBT(Capability<UnforgivingVoidCapability> capability, UnforgivingVoidCapability instance, Direction side, INBT nbt) {
            if (nbt instanceof ByteNBT)
                instance.setFallingFromTravel(((ByteNBT) nbt).getByte() == 1);
        }
    }
}