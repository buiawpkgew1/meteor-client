/*
 * This file is part of the Meteor Client distribution (https://github.com/buiawpkgew1/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import meteordevelopment.meteorclient.events.Cancellable;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;

public class RenderItemEntityEvent extends Cancellable {
    private static final RenderItemEntityEvent INSTANCE = new RenderItemEntityEvent();

    public ItemEntity itemEntity;
    public ItemEntityRenderState renderState;
    public float tickDelta;
    public MatrixStack matrixStack;
    public VertexConsumerProvider vertexConsumerProvider;
    public int light;
    public ItemRenderer itemRenderer;

    public static RenderItemEntityEvent get(ItemEntityRenderState renderState, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, ItemRenderer itemRenderer) {
        INSTANCE.setCancelled(false);
        INSTANCE.itemEntity = (ItemEntity) ((IEntityRenderState) renderState).meteor$getEntity();
        INSTANCE.renderState = renderState;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.matrixStack = matrixStack;
        INSTANCE.vertexConsumerProvider = vertexConsumerProvider;
        INSTANCE.light = light;
        INSTANCE.itemRenderer = itemRenderer;
        return INSTANCE;
    }
}
