/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.blockPicker.systems;

import org.terasology.blockPicker.events.BlockPickerScreenButton;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.characters.CharacterComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.NUIManager;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.module.inventory.components.InventoryComponent;

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
