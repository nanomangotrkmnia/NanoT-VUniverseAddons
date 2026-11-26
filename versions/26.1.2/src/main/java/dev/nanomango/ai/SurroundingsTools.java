package dev.nanomango.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.nanomango.utils.BaritoneBridge;
import dev.nanomango.utils.ReachHelper;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class SurroundingsTools {
    private SurroundingsTools() {
    }

    public static String state(JsonObject arguments) {
        JsonObject out = new JsonObject();
        out.add("position", vec(mc.player.position()));
        out.add("block_position", pos(mc.player.blockPosition()));
        out.addProperty("dimension", mc.player.level().dimension().identifier().toString());
        out.addProperty("health", mc.player.getHealth());
        out.addProperty("food", mc.player.getFoodData().getFoodLevel());
        out.addProperty("on_ground", mc.player.onGround());
        out.addProperty("sneaking", mc.player.isShiftKeyDown());
        out.addProperty("sprinting", mc.player.isSprinting());
        out.addProperty("yaw", round(mc.player.getYRot()));
        out.addProperty("pitch", round(mc.player.getXRot()));
        out.addProperty("selected_slot", mc.player.getInventory().getSelectedSlot());
        out.add("held", stack(mc.player.getMainHandItem(), -1));
        out.add("offhand", stack(mc.player.getOffhandItem(), -1));
        out.add("user", PlayerTools.self());

        BlockPos below = mc.player.blockPosition().below();
        out.addProperty("standing_on", blockId(mc.level.getBlockState(below)));
        out.addProperty("reach", round(ReachHelper.serverReach()));
        out.addProperty("baritone", BaritoneBridge.available());
        AwarenessTools.state(out);
        return out.toString();
    }

    public static String scanArea(JsonObject arguments) {
        int radius = AiTools.integer(arguments, "radius", 12, 1, 32);
        BlockPos center = mc.player.blockPosition();
        int minY = Math.max(mc.level.getMinY(), center.getY() - 8);
        int maxY = center.getY() + 4;

        int waterSource = 0;
        int waterFlowing = 0;
        int lavaSource = 0;
        int lavaFlowing = 0;
        int oreCount = 0;
        int containerCount = 0;

        JsonArray fluids = new JsonArray();
        JsonArray ores = new JsonArray();
        JsonArray containers = new JsonArray();

        for (int y = minY; y <= maxY; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);

                    if (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA) {
                        boolean isLava = state.getBlock() == Blocks.LAVA;
                        boolean source = state.getFluidState().isSource();

                        if (isLava) {
                            if (source) lavaSource++;
                            else lavaFlowing++;
                        } else {
                            if (source) waterSource++;
                            else waterFlowing++;
                        }

                        if (fluids.size() < 8) {
                            JsonObject entry = new JsonObject();
                            entry.addProperty("fluid", (isLava ? "lava_" : "water_") + (source ? "source" : "flowing"));
                            entry.add("pos", pos(pos));
                            fluids.add(entry);
                        }
                    }

                    if (state.hasBlockEntity() && state.getMenuProvider(mc.level, pos) != null) {
                        containerCount++;
                        if (containers.size() < 8) {
                            JsonObject entry = new JsonObject();
                            entry.addProperty("id", blockId(state));
                            entry.add("pos", pos(pos));
                            containers.add(entry);
                        }
                    }

                    String id = blockId(state);
                    if (isOre(id)) {
                        oreCount++;
                        if (ores.size() < 8) {
                            JsonObject entry = new JsonObject();
                            entry.addProperty("id", id);
                            entry.add("pos", pos(pos));
                            ores.add(entry);
                        }
                    }
                }
            }
        }

        AABB box = new AABB(center.getX() - radius, minY, center.getZ() - radius,
            center.getX() + radius + 1, maxY + 1, center.getZ() + radius + 1);

        List<Entity> found = mc.level.getEntitiesOfClass(Entity.class, box,
            entity -> entity != mc.player && entity.isAlive());
        found.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(mc.player)));

        JsonArray entities = new JsonArray();
        for (int i = 0; i < Math.min(10, found.size()); i++) entities.add(entity(found.get(i)));

        JsonObject fluidSummary = new JsonObject();
        fluidSummary.addProperty("water_source", waterSource);
        fluidSummary.addProperty("water_flowing", waterFlowing);
        fluidSummary.addProperty("lava_source", lavaSource);
        fluidSummary.addProperty("lava_flowing", lavaFlowing);
        fluidSummary.add("positions", fluids);

        JsonObject oreSummary = new JsonObject();
        oreSummary.addProperty("count", oreCount);
        oreSummary.add("samples", ores);

        JsonObject containerSummary = new JsonObject();
        containerSummary.addProperty("count", containerCount);
        containerSummary.add("samples", containers);

        JsonObject out = new JsonObject();
        out.add("center", pos(center));
        out.addProperty("radius", radius);
        out.add("defaults", AiTools.defaults(arguments, "radius"));
        out.addProperty("floor_y", floor(center));
        out.add("fluids", fluidSummary);
        out.add("ores", oreSummary);
        out.add("containers", containerSummary);
        out.add("entities", entities);
        return out.toString();
    }

    public static String findBlocks(JsonObject arguments) {
        if (!AiTools.present(arguments, "block")) return AiTools.error("missing required argument \"block\"");

        String id = AiTools.string(arguments, "block", "");
        if (id.isBlank()) return AiTools.error("argument \"block\" must be a block id");
        if (!id.contains(":")) id = "minecraft:" + id;

        int radius = AiTools.integer(arguments, "radius", 16, 1, 32);
        int minY = AiTools.integer(arguments, "y_min", -8, -32, 0);
        int maxY = AiTools.integer(arguments, "y_max", 8, 0, 32);
        int limit = AiTools.integer(arguments, "limit", 20, 1, 64);

        BlockPos center = mc.player.blockPosition();
        List<BlockPos> found = new ArrayList<>();

        for (int y = minY; y <= maxY; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);

                    if (!state.isAir() && blockId(state).equals(id)) found.add(pos);
                }
            }
        }

        found.sort(Comparator.comparingDouble(pos ->
            mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))));

        JsonArray positions = new JsonArray();
        for (int i = 0; i < Math.min(limit, found.size()); i++) positions.add(pos(found.get(i)));

        JsonObject out = new JsonObject();
        out.addProperty("block", id);
        out.addProperty("count", found.size());
        out.addProperty("truncated", found.size() > limit);
        out.addProperty("radius", radius);
        out.addProperty("y_min", minY);
        out.addProperty("y_max", maxY);
        out.addProperty("limit", limit);
        out.add("defaults", AiTools.defaults(arguments, "radius", "y_min", "y_max", "limit"));
        out.add("positions", positions);
        return out.toString();
    }

    public static String getBlock(JsonObject arguments) {
        String error = AiTools.intError(arguments, "x", "y", "z");
        if (error != null) return AiTools.error(error);

        int x = AiTools.integer(arguments, "x", 0, -30000000, 30000000);
        int y = AiTools.integer(arguments, "y", 0, -64, 320);
        int z = AiTools.integer(arguments, "z", 0, -30000000, 30000000);
        boolean relative = AiTools.bool(arguments, "relative", true);

        BlockPos pos = relative ? mc.player.blockPosition().offset(x, y, z) : new BlockPos(x, y, z);
        BlockState state = mc.level.getBlockState(pos);

        JsonObject out = new JsonObject();
        out.add("pos", pos(pos));
        out.addProperty("id", blockId(state));
        out.addProperty("air", state.isAir());
        out.addProperty("replaceable", state.canBeReplaced());
        out.addProperty("block_entity", state.hasBlockEntity());
        out.addProperty("hardness", round(state.getDestroySpeed(mc.level, pos)));
        out.addProperty("fluid", fluidId(state));
        out.addProperty("breakable", !state.isAir() && BlockUtils.canBreak(pos, state));
        out.addProperty("in_reach", ReachHelper.inServerReach(pos));
        out.addProperty("distance", round(Math.sqrt(mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)))));
        out.addProperty("light", mc.level.getMaxLocalRawBrightness(pos));
        out.addProperty("relative", relative);
        out.add("defaults", AiTools.defaults(arguments, "relative"));
        return out.toString();
    }

    public static String inventory(JsonObject arguments) {
        Inventory inventory = mc.player.getInventory();
        boolean main = AiTools.bool(arguments, "main", false);

        JsonArray hotbar = new JsonArray();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) hotbar.add(stack(stack, slot));
        }

        String[] armorSlots = {"boots", "leggings", "chestplate", "helmet"};
        JsonArray armor = new JsonArray();
        for (int i = 0; i < armorSlots.length; i++) {
            ItemStack stack = inventory.getItem(36 + i);
            JsonObject entry = stack(stack, 36 + i);
            entry.addProperty("armor_slot", armorSlots[i]);
            armor.add(entry);
        }

        JsonObject out = new JsonObject();
        out.addProperty("selected", inventory.getSelectedSlot());
        out.add("hotbar", hotbar);
        out.add("offhand", stack(mc.player.getOffhandItem(), 40));
        out.add("armor", armor);
        out.addProperty("main", main);
        out.add("defaults", AiTools.defaults(arguments, "main"));

        if (main) {
            JsonArray rest = new JsonArray();
            for (int slot = 9; slot < Math.min(36, inventory.getContainerSize()); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty()) rest.add(stack(stack, slot));
            }
            out.add("main", rest);
        }

        return out.toString();
    }

    public static String findEntities(JsonObject arguments) {
        String kind = AiTools.string(arguments, "kind", "mob").toLowerCase();

        if (!kind.equals("player") && !kind.equals("hostile") && !kind.equals("animal")
            && !kind.equals("mob") && !kind.equals("all")) {
            return AiTools.error("unknown kind \"" + kind
                + "\"; valid values are player, hostile, animal, mob, all");
        }

        int radius = AiTools.integer(arguments, "radius", 24, 1, 64);
        int limit = AiTools.integer(arguments, "limit", 10, 1, 32);

        BlockPos center = mc.player.blockPosition();
        AABB box = new AABB(center.getX() - radius, center.getY() - 8, center.getZ() - radius,
            center.getX() + radius + 1, center.getY() + 8 + 1, center.getZ() + radius + 1);

        List<Entity> found = mc.level.getEntitiesOfClass(Entity.class, box,
            entity -> entity != mc.player && entity.isAlive() && matches(entity, kind));
        found.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(mc.player)));

        JsonArray entities = new JsonArray();
        for (int i = 0; i < Math.min(limit, found.size()); i++) entities.add(entity(found.get(i)));

        JsonObject out = new JsonObject();
        out.addProperty("kind", kind);
        out.addProperty("count", found.size());
        out.addProperty("radius", radius);
        out.addProperty("limit", limit);
        out.add("defaults", AiTools.defaults(arguments, "kind", "radius", "limit"));
        out.add("entities", entities);
        return out.toString();
    }

    private static boolean matches(Entity entity, String kind) {
        return switch (kind) {
            case "player" -> entity instanceof Player;
            case "hostile" -> entity instanceof Monster;
            case "animal" -> entity instanceof LivingEntity
                && !(entity instanceof Player) && !(entity instanceof Monster);
            case "all" -> true;
            default -> entity instanceof LivingEntity && !(entity instanceof Player);
        };
    }

    private static JsonObject entity(Entity entity) {
        JsonObject out = new JsonObject();
        out.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        out.addProperty("name", entity.getName().getString());
        out.add("pos", vec(entity.position()));
        out.addProperty("distance", round(Math.sqrt(entity.distanceToSqr(mc.player))));
        out.addProperty("player", entity instanceof Player);
        out.addProperty("hostile", entity instanceof Monster);

        if (entity instanceof LivingEntity living) {
            out.addProperty("health", round(living.getHealth()));
            out.addProperty("max_health", round(living.getMaxHealth()));
        }

        return out;
    }

    private static JsonObject stack(ItemStack stack, int slot) {
        JsonObject out = new JsonObject();
        if (stack == null || stack.isEmpty()) {
            out.addProperty("empty", true);
            return out;
        }

        if (slot >= 0) out.addProperty("slot", slot);
        out.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        out.addProperty("name", stack.getHoverName().getString());
        out.addProperty("count", stack.getCount());

        if (stack.isDamageableItem()) out.addProperty("durability", stack.getMaxDamage() - stack.getDamageValue());
        return out;
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static String fluidId(BlockState state) {
        FluidState fluid = state.getFluidState();
        if (fluid.isEmpty()) return "";

        return BuiltInRegistries.FLUID.getKey(fluid.getType()).toString();
    }

    private static boolean isOre(String id) {
        return id.endsWith("_ore") || id.equals("minecraft:ancient_debris");
    }

    private static int floor(BlockPos center) {
        BlockPos pos = center;
        int bottom = mc.level.getMinY();

        while (pos.getY() > bottom && mc.level.getBlockState(pos).getCollisionShape(mc.level, pos).isEmpty()) {
            pos = pos.below();
        }

        return pos.getY();
    }

    private static JsonObject pos(BlockPos pos) {
        JsonObject out = new JsonObject();
        out.addProperty("x", pos.getX());
        out.addProperty("y", pos.getY());
        out.addProperty("z", pos.getZ());
        return out;
    }

    private static JsonObject vec(Vec3 vec) {
        JsonObject out = new JsonObject();
        out.addProperty("x", round(vec.x));
        out.addProperty("y", round(vec.y));
        out.addProperty("z", round(vec.z));
        return out;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
