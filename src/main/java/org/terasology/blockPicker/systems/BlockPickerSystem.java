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

import org.terasology.blockPicker.components.BlockPickerComponent;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.registry.In;
import org.terasology.module.inventory.components.InventoryComponent;
import org.terasology.module.inventory.events.InventorySlotChangedEvent;
import org.terasology.module.inventory.events.InventorySlotStackSizeChangedEvent;
import org.terasology.module.inventory.systems.InventoryManager;

@RegisterSystem
public class BlockPickerSystem extends BaseComponentSystem {

    @In
    EntityManager entityManager;
    @In
    InventoryManager inventoryManager;

    @ReceiveEvent(components = BlockPickerComponent.class)
    public void onBlockPickerItemsChanged(InventorySlotChangedEvent event, EntityRef entity) {
        // forcibly destroy the items being swapped with a copy of what was already there
        // this makes it look like the original items were never touched
        EntityRef duplicatedOldItem = event.getOldItem().copy();
        InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
        inventoryComponent.itemSlots.set(event.getSlot(), duplicatedOldItem);
        event.getNewItem().destroy();
    }

    @ReceiveEvent(components = BlockPickerComponent.class)
    public void onBlockPickerItemsChanged(InventorySlotStackSizeChangedEvent event, EntityRef entity) {
        // fill up the stack back to the maximum amount of items
        EntityRef item = inventoryManager.getItemInSlot(entity, event.getSlot());
        ItemComponent itemComponent = item.getComponent(ItemComponent.class);
        itemComponent.stackCount = itemComponent.maxStackSize;
        item.saveComponent(itemComponent);
    }
}
