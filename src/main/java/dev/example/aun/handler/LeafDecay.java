package dev.example.aun.handler;

import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Fast leaf decay. When a log is broken, we check the leaves around it. A leaf is
 * "supported" if a log can be reached from it through at most 6 connected leaves (exactly
 * vanilla's rule). Unsupported leaves decay quickly, a few per tick.
 * Only NATURAL leaves are touched: leaves placed by players have the "persistent" flag
 * and are never removed, so builds that use leaves are safe.
 */
public final class LeafDecay {

    private static final int MAX_DISTANCE = 6;
    private static final int LOG_SEARCH = 12;

    private record Job(ServerLevel level, BlockPos pos, long due) {}
    private record Removal(ServerLevel level, BlockPos pos) {}

    private static final List<Job> JOBS = new ArrayList<>();
    private static final ArrayDeque<Removal> REMOVALS = new ArrayDeque<>();

    private LeafDecay() {}

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel serverLevel
                && AunConfigManager.get().fastLeafDecay
                && state.is(BlockTags.LOGS)) {
                JOBS.add(new Job(serverLevel, pos.immutable(), serverLevel.getGameTime() + 5));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    private static void tick() {
        for (int i = 0; i < 12 && !REMOVALS.isEmpty(); i++) {
            Removal removal = REMOVALS.poll();
            BlockState state = removal.level().getBlockState(removal.pos());
            if (isNaturalLeaf(state)) {
                Block.dropResources(state, removal.level(), removal.pos());
                removal.level().removeBlock(removal.pos(), false);
            }
        }
        int budget = 2;
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext() && budget > 0) {
            Job job = it.next();
            if (job.level().getGameTime() >= job.due()) {
                it.remove();
                budget--;
                scan(job);
            }
        }
    }

    private static boolean isNaturalLeaf(BlockState state) {
        return state.is(BlockTags.LEAVES)
            && state.hasProperty(LeavesBlock.PERSISTENT)
            && !state.getValue(LeavesBlock.PERSISTENT);
    }

    private static void scan(Job job) {
        ServerLevel level = job.level();
        BlockPos origin = job.pos();

        // 1) natural leaves close enough to have depended on the broken log
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -MAX_DISTANCE; dx <= MAX_DISTANCE; dx++) {
            for (int dy = -MAX_DISTANCE; dy <= MAX_DISTANCE; dy++) {
                for (int dz = -MAX_DISTANCE; dz <= MAX_DISTANCE; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > MAX_DISTANCE) {
                        continue;
                    }
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (isNaturalLeaf(level.getBlockState(pos))) {
                        candidates.add(pos);
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        // 2) flood outward from every log nearby, through leaves, up to 6 steps
        Map<Long, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (int dx = -LOG_SEARCH; dx <= LOG_SEARCH; dx++) {
            for (int dy = -LOG_SEARCH; dy <= LOG_SEARCH; dy++) {
                for (int dz = -LOG_SEARCH; dz <= LOG_SEARCH; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (level.getBlockState(pos).is(BlockTags.LOGS)) {
                        distance.put(pos.asLong(), 0);
                        queue.add(pos);
                    }
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            int next = distance.get(current.asLong()) + 1;
            if (next > MAX_DISTANCE) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = current.relative(direction);
                if (!distance.containsKey(neighbour.asLong())
                    && level.getBlockState(neighbour).is(BlockTags.LEAVES)) {
                    distance.put(neighbour.asLong(), next);
                    queue.add(neighbour);
                }
            }
        }

        // 3) leaves nobody supports any more decay, in random order
        List<BlockPos> unsupported = new ArrayList<>();
        for (BlockPos pos : candidates) {
            if (!distance.containsKey(pos.asLong())) {
                unsupported.add(pos);
            }
        }
        Collections.shuffle(unsupported);
        for (BlockPos pos : unsupported) {
            REMOVALS.add(new Removal(level, pos));
        }
    }
}
