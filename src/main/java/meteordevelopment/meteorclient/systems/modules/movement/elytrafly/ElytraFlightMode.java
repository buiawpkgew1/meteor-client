/*
 * This file is part of the Meteor Client distribution (https://github.com/buiawpkgew1/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.elytrafly;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class ElytraFlightMode {
    protected final MinecraftClient mc;
    protected final ElytraFly elytraFly;
    private final ElytraFlightModes type;

    protected boolean lastJumpPressed;
    protected boolean incrementJumpTimer;
    protected boolean lastForwardPressed;
    protected int jumpTimer;
    protected double velX, velY, velZ;
    protected double ticksLeft;
    protected Vec3d forward, right;
    protected double acceleration;

    public ElytraFlightMode(ElytraFlightModes type) {
        this.elytraFly = Modules.get().get(ElytraFly.class);
        this.mc = MinecraftClient.getInstance();
        this.type = type;
    }

    public void onTick() {
        if (elytraFly.autoReplenish.get()) {
            FindItemResult fireworks = InvUtils.find(Items.FIREWORK_ROCKET);

            if (fireworks.found() && !fireworks.isHotbar()) {
                InvUtils.move().from(fireworks.slot()).toHotbar(elytraFly.replenishSlot.get() - 1);
            }
        }

        if (elytraFly.replace.get()) {
            ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);

            if (chestStack.getItem() == Items.ELYTRA) {
                if (chestStack.getMaxDamage() - chestStack.getDamage() <= elytraFly.replaceDurability.get()) {
                    FindItemResult elytra = InvUtils.find(stack -> stack.getMaxDamage() - stack.getDamage() > elytraFly.replaceDurability.get() && stack.getItem() == Items.ELYTRA);

                    InvUtils.move().from(elytra.slot()).toArmor(2);
                }
            }
        }
    }

    public void onPreTick() {
    }

    public void onPacketSend(PacketEvent.Send event) {
    }

    public void onPacketReceive(PacketEvent.Receive event) {
    }

    public void onPlayerMove() {
    }

    public void onActivate() {
        lastJumpPressed = false;
        jumpTimer = 0;
        ticksLeft = 0;
        acceleration = 0;
    }

    public void onDeactivate() {
    }

    public void autoTakeoff() {
        if (incrementJumpTimer) jumpTimer++;

        boolean jumpPressed = mc.options.jumpKey.isPressed();

        if ((elytraFly.autoTakeOff.get() && elytraFly.flightMode.get() != ElytraFlightModes.Pitch40 && elytraFly.flightMode.get() != ElytraFlightModes.Bounce) ||
            (!elytraFly.manualTakeoff.get() && elytraFly.flightMode.get() == ElytraFlightModes.Bounce) && jumpPressed) {
            if (!lastJumpPressed && !mc.player.isGliding()) {
                jumpTimer = 0;
                incrementJumpTimer = true;
            }

            if (jumpTimer >= 8) {
                jumpTimer = 0;
                incrementJumpTimer = false;
                mc.player.setJumping(false);
                mc.player.setSprinting(true);
                mc.player.jump();
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }

        lastJumpPressed = jumpPressed;
    }

    public void handleAutopilot() {
        if (!mc.player.isGliding()) return;

        if (elytraFly.autoPilot.get() && mc.player.getY() > elytraFly.autoPilotMinimumHeight.get() && elytraFly.flightMode.get() != ElytraFlightModes.Bounce) {
            mc.options.forwardKey.setPressed(true);
            lastForwardPressed = true;
        }

        if (elytraFly.useFireworks.get()) {
            if (ticksLeft <= 0) {
                ticksLeft = elytraFly.autoPilotFireworkDelay.get() * 20;

                FindItemResult itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
                if (!itemResult.found()) return;

                if (itemResult.isOffhand()) {
                    mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                    mc.player.swingHand(Hand.OFF_HAND);
                } else {
                    InvUtils.swap(itemResult.slot(), true);

                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.MAIN_HAND);

                    InvUtils.swapBack();
                }
            }
            ticksLeft--;
        }
    }

    public void handleHorizontalSpeed(PlayerMoveEvent event) {
        boolean a = false;
        boolean b = false;

        if (mc.options.forwardKey.isPressed()) {
            velX += forward.x * getSpeed() * 10;
            velZ += forward.z * getSpeed() * 10;
            a = true;
        } else if (mc.options.backKey.isPressed()) {
            velX -= forward.x * getSpeed() * 10;
            velZ -= forward.z * getSpeed() * 10;
            a = true;
        }

        if (mc.options.rightKey.isPressed()) {
            velX += right.x * getSpeed() * 10;
            velZ += right.z * getSpeed() * 10;
            b = true;
        } else if (mc.options.leftKey.isPressed()) {
            velX -= right.x * getSpeed() * 10;
            velZ -= right.z * getSpeed() * 10;
            b = true;
        }

        if (a && b) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }
    }

    public void handleVerticalSpeed(PlayerMoveEvent event) {
        if (mc.options.jumpKey.isPressed()) velY += 0.5 * elytraFly.verticalSpeed.get();
        else if (mc.options.sneakKey.isPressed()) velY -= 0.5 * elytraFly.verticalSpeed.get();
    }

    public void handleFallMultiplier() {
        if (velY < 0) velY *= elytraFly.fallMultiplier.get();
        else if (velY > 0) velY = 0;
    }

    public void handleAcceleration() {
        if (elytraFly.acceleration.get()) {
            if (!PlayerUtils.isMoving()) acceleration = 0;
            acceleration = Math.min(
                acceleration + elytraFly.accelerationMin.get() + elytraFly.accelerationStep.get() * .1,
                elytraFly.horizontalSpeed.get()
            );
        } else {
            acceleration = 0;
        }
    }

    public void zeroAcceleration() {
        acceleration = 0;
    }

    protected double getSpeed() {
        return elytraFly.acceleration.get() ? acceleration : elytraFly.horizontalSpeed.get();
    }

    public String getHudString() {
        return type.name();
    }
}
