/*
 * This file is part of the Meteor Client distribution (https://github.com/buiawpkgew1/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

public interface IRenderPipeline {
    void meteor$setLineSmooth(boolean lineSmooth);

    boolean meteor$getLineSmooth();
}
