// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockPicker.systems;

import org.terasology.blockPicker.events.BlockPickerScreenButton;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.characters.CharacterComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.NUIManager;
import org.terasology.inventory.logic.InventoryComponent;

@RegisterSystem(RegisterMode.CLIENT)
public class BlockPickerClientSystem extends BaseComponentSystem {
    @In
    NUIManager nuiManager;

    @ReceiveEvent(components = {CharacterComponent.class, InventoryComponent.class})
    public void onOpenContainer(BlockPickerScreenButton event, EntityRef entity) {
        if (event.getState().isDown()) {
            nuiManager.toggleScreen("BlockPicker:BlockPickerScreen");
            event.consume();
        }
    }
}
