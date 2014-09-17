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
package org.terasology.blockPicker.ui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.terasology.blockPicker.components.BlockPickerComponent;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.registry.In;
import org.terasology.rendering.nui.CoreScreenLayer;
import org.terasology.rendering.nui.databinding.Binding;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryGrid;
import org.terasology.rendering.nui.widgets.UIDropdown;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.items.BlockItemComponent;
import org.terasology.world.block.items.BlockItemFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BlockPickerScreen extends CoreScreenLayer {

    @In
    EntityManager entityManager;
    @In
    InventoryManager inventoryManager;

    Set<EntityRef> allItemEntities;
    EntityRef inventoryEntity;

    @Override
    protected void initialise() {
        refreshAllItemEntities();

        inventoryEntity = entityManager.create(new InventoryComponent(), new BlockPickerComponent());
        inventoryEntity.setPersistent(false);

        InventoryGrid inventoryGrid = find("inventoryGrid", InventoryGrid.class);
        inventoryGrid.setTargetEntity(inventoryEntity);

        UIDropdown dropdown = find("categoryDropDown", UIDropdown.class);

        List<String> options = Lists.newArrayList();
        for (EntityRef entityRef : allItemEntities) {
            BlockItemComponent blockItemComponent = entityRef.getComponent(BlockItemComponent.class);
            if (blockItemComponent != null && blockItemComponent.blockFamily != null) {
                for (String category : blockItemComponent.blockFamily.getCategories()) {
                    if (!options.contains(category)) {
                        options.add(category);
                    }
                }
            }
        }
        Collections.sort(options);
        options.add(0, "All");
        options.add(1, "Items");

        dropdown.setOptions(options);

        dropdown.bindSelection(new Binding() {
            String selectedValue;

            @Override
            public Object get() {
                return selectedValue;
            }

            @Override
            public void set(Object value) {
                selectedValue = (String) value;

                InventoryComponent inventoryComponent = new InventoryComponent();

                for (EntityRef item : allItemEntities) {
                    BlockItemComponent blockItemComponent = item.getComponent(BlockItemComponent.class);
                    if ((blockItemComponent != null && blockItemComponent.blockFamily != null && blockItemComponent.blockFamily.hasCategory(selectedValue))
                            || selectedValue.equals("All")
                            || (blockItemComponent == null && selectedValue.equals("Items"))) {
                        inventoryComponent.itemSlots.add(item);
                    }
                }

                inventoryEntity.saveComponent(inventoryComponent);
            }
        });
        dropdown.setSelection("All");

    }

    @Override
    public boolean isModal() {
        return false;
    }

    private void refreshAllItemEntities() {
        allItemEntities = Sets.newHashSet();

        BlockManager blockManager = CoreRegistry.get(BlockManager.class);
        PrefabManager prefabManager = CoreRegistry.get(PrefabManager.class);

        for (Prefab prefab : prefabManager.listPrefabs()) {
            ItemComponent itemComp = prefab.getComponent(ItemComponent.class);
            if (itemComp != null) {
                EntityRef entity = entityManager.create(prefab);
                entity.setPersistent(false);
                if (entity.exists() && entity.getComponent(ItemComponent.class) != null) {
                    // ensure there are the maximum amount of items in the stack
                    ItemComponent itemComponent = entity.getComponent(ItemComponent.class);
                    if (!itemComponent.stackId.isEmpty()) {
                        itemComponent.stackCount = itemComponent.maxStackSize;
                        entity.saveComponent(itemComponent);
                    }
                    allItemEntities.add(entity);
                }
            }
        }

        BlockItemFactory blockFactory = new BlockItemFactory(entityManager);
        Set<BlockUri> blocks = Sets.newHashSet();


        Iterables.addAll(blocks, blockManager.listRegisteredBlockUris());
        Iterables.addAll(blocks, blockManager.listAvailableBlockUris());
        Iterables.addAll(blocks, blockManager.listFreeformBlockUris());

        for (BlockUri block : blocks) {
            EntityRef entity = blockFactory.newInstance(blockManager.getBlockFamily(block.getFamilyUri()), 99);
            entity.setPersistent(false);
            if (entity.exists()) {
                allItemEntities.add(entity);
            }
        }
    }
}
