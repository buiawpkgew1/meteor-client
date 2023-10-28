/*
 * This file is part of the Meteor Client distribution (https://github.com/buiawpkgew1/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import baritone.api.BaritoneAPI;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class LocateCommand extends Command {
    private Vec3d firstStart;
    private Vec3d firstEnd;
    private Vec3d secondStart;
    private Vec3d secondEnd;

    private final List<Block> netherFortressBlocks = Arrays.asList(
        Blocks.NETHER_BRICKS,
        Blocks.NETHER_BRICK_FENCE,
        Blocks.NETHER_WART
    );

    private final List<Block> monumentBlocks = Arrays.asList(
        Blocks.PRISMARINE_BRICKS,
        Blocks.SEA_LANTERN,
        Blocks.DARK_PRISMARINE
    );

    private final List<Block> strongholdBlocks = Arrays.asList(
        Blocks.END_PORTAL_FRAME
    );

    public LocateCommand() {
        super("locate", "定位结构", "loc");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("buried_treasure").executes(s -> {
            ItemStack stack = mc.player.getInventory().getMainHandStack();
            if (stack.getItem() != Items.FILLED_MAP) {
                error("你首先需要手持一张藏宝图.");
                return SINGLE_SUCCESS;
            }
            NbtCompound tag = stack.getNbt();
            NbtList nbt1 = (NbtList) tag.get("Decorations");
            if (nbt1 == null) {
                error("无法定位十字架.你是否手持一张(highlight)treasure map(default)？");
                return SINGLE_SUCCESS;
            }

            NbtCompound iconNBT = nbt1.getCompound(0);
            if (iconNBT == null) {
                error("无法定位十字架.你是否手持一张(highlight)treasure map(default)？");
                return SINGLE_SUCCESS;
            }

            Vec3d coords = new Vec3d(iconNBT.getDouble("x"), iconNBT.getDouble("y"), iconNBT.getDouble("z"));
            MutableText text = Text.literal("发现埋藏的宝藏在 ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("lodestone").executes(s -> {
            ItemStack stack = mc.player.getInventory().getMainHandStack();
            if (stack.getItem() != Items.COMPASS) {
                error("你需要手持一枚磁石指南针(lodestone compass)");
                return SINGLE_SUCCESS;
            }
            NbtCompound tag = stack.getNbt();
            if (tag == null) {
                error("无法获取NBT数据.你是否手持一枚(highlight)lodestone(default)？");
                return SINGLE_SUCCESS;
            }
            NbtCompound nbt1 = tag.getCompound("LodestonePos");
            if (nbt1 == null) {
                error("无法获取NBT数据.你是否手持一枚(highlight)lodestone(default)？");
                return SINGLE_SUCCESS;
            }

            Vec3d coords = new Vec3d(nbt1.getDouble("X"), nbt1.getDouble("Y"), nbt1.getDouble("Z"));
            MutableText text = Text.literal("发现磁石(Lodestone)位于 ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("mansion").executes(s -> {
            ItemStack stack = mc.player.getInventory().getMainHandStack();
            if (stack.getItem() != Items.FILLED_MAP) {
                error("你首先需要手持一张丛林探险家地图.");
                return SINGLE_SUCCESS;
            }
            NbtCompound tag = stack.getNbt();
            NbtList nbt1 = (NbtList) tag.get("Decorations");
            if (nbt1 == null) {
                error("无法找到豪宅.你是否手持一张(highlight)woodland explorer map(default)？");
                return SINGLE_SUCCESS;
            }

            NbtCompound iconNBT = nbt1.getCompound(0);
            if (iconNBT == null) {
                error("无法找到豪宅.你是否手持一张(highlight)woodland explorer map(default)？");
                return SINGLE_SUCCESS;
            }

            Vec3d coords = new Vec3d(iconNBT.getDouble("x"), iconNBT.getDouble("y"), iconNBT.getDouble("z"));
            MutableText text = Text.literal("豪宅位于 ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("stronghold").executes(s -> {
            if (!BaritoneUtils.IS_AVAILABLE) {
                error("Locating this structure requires Baritone.");
                return SINGLE_SUCCESS;
            }

            boolean foundEye = InvUtils.testInHotbar(Items.ENDER_EYE);

            if (foundEye) {
                PathManagers.get().follow(entity -> entity instanceof EyeOfEnderEntity);
                firstStart = null;
                firstEnd = null;
                secondStart = null;
                secondEnd = null;
                MeteorClient.EVENT_BUS.subscribe(this);
                info("请投掷第一个末影之眼（Eye of Ender）");
            } else {
                Vec3d coords = findByBlockList(strongholdBlocks);
                if (coords == null) {
                    error("附近未找到要塞.你可以使用（highlight）末影之眼（Ender Eyes）(default) 来提高成功率.");
                    return SINGLE_SUCCESS;
                }
                MutableText text = Text.literal("要塞位于 ");
                text.append(ChatUtils.formatCoords(coords));
                text.append(".");
                info(text);
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("nether_fortress").executes(s -> {
            if (!BaritoneUtils.IS_AVAILABLE) {
                error("Locating this structure requires Baritone.");
                return SINGLE_SUCCESS;
            }

            Vec3d coords = findByBlockList(netherFortressBlocks);
            if (coords == null) {
                error("未找到地狱堡垒（Nether Fortress）");
                return SINGLE_SUCCESS;
            }
            MutableText text = Text.literal("地狱堡垒位于 ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("monument").executes(s -> {
            if (!BaritoneUtils.IS_AVAILABLE) {
                error("Locating this structure requires Baritone.");
                return SINGLE_SUCCESS;
            }

            ItemStack stack = mc.player.getInventory().getMainHandStack();
            if (stack.getItem() == Items.FILLED_MAP) {
                NbtCompound tag = stack.getNbt();
                if (tag != null) {
                    NbtList nbt1 = (NbtList) tag.get("Decorations");
                    if (nbt1 != null) {
                        NbtCompound iconNBT = nbt1.getCompound(0);
                        if (iconNBT != null) {
                            Vec3d coords = new Vec3d(iconNBT.getDouble("x"), iconNBT.getDouble("y"), iconNBT.getDouble("z"));
                            MutableText text = Text.literal("海底神殿位于 ");
                            text.append(ChatUtils.formatCoords(coords));
                            text.append(".");
                            info(text);
                            return SINGLE_SUCCESS;
                        }
                    }
                }
            }
            Vec3d coords = findByBlockList(monumentBlocks);
            if (coords == null) {
                error("No monument found. You can try using a (highlight)Ocean explorer map(default) for more success.");
                return SINGLE_SUCCESS;
            }
            MutableText text = Text.literal("水下庙宇位于 ");
            text.append(ChatUtils.formatCoords(coords));
            text.append(".");
            info(text);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("cancel").executes(s -> {
            cancel();
            return SINGLE_SUCCESS;
        }));
    }

    private void cancel() {
        warning("Locate canceled");
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    private Vec3d findByBlockList(List<Block> blockList) {
        List<BlockPos> posList = BaritoneAPI.getProvider().getWorldScanner().scanChunkRadius(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext(), blockList, 64, 10, 32);
        if (posList.isEmpty()) {
            return null;
        }
        if (posList.size() < 3) {
            warning("仅找到 %d 个方块.这可能是误报." , posList.size());
        }
        return new Vec3d(posList.get(0).getX(), posList.get(0).getY(), posList.get(0).getZ());
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntitySpawnS2CPacket packet) {
            if (packet.getEntityType() == EntityType.EYE_OF_ENDER) {
                firstPosition(packet.getX(), packet.getY(), packet.getZ());
            }
        }
        if (event.packet instanceof PlaySoundS2CPacket packet) {
            if (packet.getSound().value() == SoundEvents.ENTITY_ENDER_EYE_DEATH) {
                lastPosition(packet.getX(), packet.getY(), packet.getZ());
            }
        }
    }

    private void firstPosition(double x, double y, double z) {
        Vec3d pos = new Vec3d(x, y, z);
        if (this.firstStart == null) {
            this.firstStart = pos;
        }
        else {
            this.secondStart = pos;
        }
    }

    private void lastPosition(double x, double y, double z) {
        info("被保存的%s末影之眼轨迹.", (this.firstEnd == null) ? "第一个" : "第二个");
        Vec3d pos = new Vec3d(x, y, z);
        if (this.firstEnd == null) {
            this.firstEnd = pos;
            info("请从不同的位置投掷第二个末影之眼.");
        }
        else {
            this.secondEnd = pos;
            findStronghold();
        }
    }

    private void findStronghold() {
        PathManagers.get().stop();

        if (this.firstStart == null || this.firstEnd == null || this.secondStart == null || this.secondEnd == null) {
            error("缺失位置数据");
            cancel();
            return;
        }
        final double[] start = new double[]{this.secondStart.x, this.secondStart.z, this.secondEnd.x, this.secondEnd.z};
        final double[] end = new double[]{this.firstStart.x, this.firstStart.z, this.firstEnd.x, this.firstEnd.z};
        final double[] intersection = calcIntersection(start, end);
        if (Double.isNaN(intersection[0]) || Double.isNaN(intersection[1]) || Double.isInfinite(intersection[0]) || Double.isInfinite(intersection[1])) {
            error("直线平行");
            cancel();
            return;
        }
        MeteorClient.EVENT_BUS.unsubscribe(this);
        Vec3d coords = new Vec3d(intersection[0], 0, intersection[1]);
        MutableText text = Text.literal("大约位于的要塞 ");
        text.append(ChatUtils.formatCoords(coords));
        text.append(".");
        info(text);
    }

    private double[] calcIntersection(double[] line, double[] line2) {
        final double a1 = line[3] - line[1];
        final double b1 = line[0] - line[2];
        final double c1 = a1 * line[0] + b1 * line[1];

        final double a2 = line2[3] - line2[1];
        final double b2 = line2[0] - line2[2];
        final double c2 = a2 * line2[0] + b2 * line2[1];

        final double delta = a1 * b2 - a2 * b1;

        return new double[]{(b2 * c1 - b1 * c2) / delta, (a1 * c2 - a2 * c1) / delta};
    }
}
