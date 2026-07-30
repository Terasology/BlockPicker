// Copyright 2026 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockPicker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.terasology.blockPicker.components.BlockPickerComponent;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.integrationenvironment.jupiter.IntegrationEnvironment;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.registry.In;
import org.terasology.module.inventory.components.InventoryComponent;
import org.terasology.module.inventory.systems.InventoryManager;

/**
 * Covers {@link org.terasology.blockPicker.systems.BlockPickerSystem} — the "infinite inventory"
 * behaviour. A block picker's slots are meant to look untouched no matter what a player takes
 * out of them: stacks refill to full, and swapping something in leaves the original in place.
 * <p>
 * Both behaviours are driven by events {@code InventoryUtils} sends synchronously from
 * {@code entity.send(...)}, so the assertions can run straight after the inventory call
 * without ticking the engine.
 */
@IntegrationEnvironment(dependencies = {"BlockPicker", "Inventory"})
public class BlockPickerSystemTest {

    private static final byte MAX_STACK = (byte) 99;
    private static final int SLOT = 0;

    @In
    private EntityManager entityManager;
    @In
    private InventoryManager inventoryManager;

    @Test
    public void takingFromAStackRefillsItToMaximum() {
        EntityRef picker = createPickerWithFullStack("blockPickerTest:refill");

        EntityRef removed = inventoryManager.removeItem(picker, EntityRef.NULL, SLOT, true, 10);
        Assertions.assertNotNull(removed, "removeItem reported failure — the slot was not set up as expected");

        EntityRef inSlot = inventoryManager.getItemInSlot(picker, SLOT);
        Assertions.assertEquals(MAX_STACK, inventoryManager.getStackSize(inSlot),
                "a block picker stack should refill to its maximum after items are taken from it");
    }

    @Test
    public void swappingIntoAPickerSlotLeavesTheOriginalItemInPlace() {
        EntityRef picker = createPickerWithFullStack("blockPickerTest:original");

        EntityRef donor = entityManager.create();
        donor.addComponent(new InventoryComponent(1));
        EntityRef intruder = createStackableItem("blockPickerTest:intruder", (byte) 1);
        Assertions.assertTrue(inventoryManager.giveItem(donor, EntityRef.NULL, intruder, 0),
                "failed to seed the donor inventory");

        inventoryManager.switchItem(donor, EntityRef.NULL, 0, picker, SLOT);

        EntityRef inSlot = inventoryManager.getItemInSlot(picker, SLOT);
        ItemComponent item = inSlot.getComponent(ItemComponent.class);
        Assertions.assertNotNull(item, "the picker slot should still hold an item after a swap");
        Assertions.assertEquals("blockPickerTest:original", item.stackId,
                "a block picker slot should keep its original item when something is swapped in");
    }

    /** An inventory entity carrying {@link BlockPickerComponent} with one full stack in {@link #SLOT}. */
    private EntityRef createPickerWithFullStack(String stackId) {
        EntityRef picker = entityManager.create();
        picker.addComponent(new InventoryComponent(4));

        // Seed the slot *before* marking it as a block picker: BlockPickerSystem reverts
        // slot changes, so an item given afterwards would be destroyed on arrival.
        EntityRef item = createStackableItem(stackId, MAX_STACK);
        Assertions.assertTrue(inventoryManager.giveItem(picker, EntityRef.NULL, item, SLOT),
                "failed to seed the picker inventory");

        picker.addComponent(new BlockPickerComponent());
        return picker;
    }

    private EntityRef createStackableItem(String stackId, byte count) {
        EntityRef item = entityManager.create();
        ItemComponent itemComponent = new ItemComponent();
        itemComponent.stackId = stackId;
        itemComponent.maxStackSize = MAX_STACK;
        itemComponent.stackCount = count;
        item.addComponent(itemComponent);
        return item;
    }
}
