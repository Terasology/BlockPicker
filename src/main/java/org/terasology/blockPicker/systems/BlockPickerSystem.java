// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockPicker.systems;

import org.terasology.blockPicker.components.BlockPickerComponent;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.registry.In;
import org.terasology.inventory.logic.InventoryComponent;
import org.terasology.inventory.logic.InventoryManager;
import org.terasology.inventory.logic.events.InventorySlotChangedEvent;
import org.terasology.inventory.logic.events.InventorySlotStackSizeChangedEvent;

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
