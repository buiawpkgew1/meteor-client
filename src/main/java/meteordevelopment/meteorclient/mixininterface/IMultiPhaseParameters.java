/*
 * This file is part of the Meteor Client distribution (https://github.com/buiawpkgew1/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.client.render.RenderPhase;

public interface IMultiPhaseParameters {
    RenderPhase.Target meteor$getTarget();
}
