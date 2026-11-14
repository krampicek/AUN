package dev.example.aun.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Gets a passenger out of its boat/minecart and puts it on the closest solid ground. */
final class DismountHelper {

    private static final int RADIUS = 4;

    private DismountHelper() {}

    static void dismountToGround(Entity passenger) {
        Entity vehicle = passenger.getVehicle();
        if (vehicle == null) {
            return;
        }
        Level level = passenger.level();
        passenger.stopRiding(); // vanilla puts it on top of the vehicle; we fix that below

        Vec3 here = passenger.position();
        AABB passengerBox = passenger.getBoundingBox();
        AABB vehicleBox = vehicle.getBoundingBox();
        Vec3 vehicleCenter = vehicle.position();
        BlockPos origin = vehicle.blockPosition();

        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos feet = origin.offset(dx, dy, dz);
                    BlockPos floor = feet.below();
                    BlockState floorState = level.getBlockState(floor);
                    if (!floorState.isFaceSturdy(level, floor, Direction.UP)
                        || !level.getFluidState(feet).isEmpty()) {
                        continue;
                    }
                    Vec3 target = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
                    AABB box = passengerBox.move(target.subtract(here));
                    if (box.intersects(vehicleBox) || !level.noCollision(passenger, box)) {
                        continue;
                    }
                    double distance = target.distanceToSqr(vehicleCenter);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = target;
                    }
                }
            }
        }
        if (best != null) {
            passenger.setPos(best.x, best.y, best.z);
            passenger.setDeltaMovement(Vec3.ZERO);
            passenger.resetFallDistance();
        }
    }
}
