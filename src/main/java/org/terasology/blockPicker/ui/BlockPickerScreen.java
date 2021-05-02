// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockPicker.ui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.terasology.gestalt.assets.management.AssetManager;
import org.terasology.blockPicker.components.BlockPickerComponent;
import org.terasology.engine.entitySystem.entity.EntityBuilder;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.prefab.PrefabManager;
import org.terasology.engine.logic.common.DisplayNameComponent;
import org.terasology.module.inventory.components.InventoryComponent;
import org.terasology.module.inventory.systems.InventoryManager;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.CoreScreenLayer;
import org.terasology.module.inventory.ui.InventoryGrid;
import org.terasology.engine.world.block.BlockExplorer;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.engine.world.block.items.BlockItemComponent;
import org.terasology.engine.world.block.items.BlockItemFactory;
import org.terasology.nui.databinding.Binding;
import org.terasology.nui.databinding.DefaultBinding;
import org.terasology.nui.databinding.ReadOnlyBinding;
import org.terasology.nui.widgets.UIDropdown;
import org.terasology.nui.widgets.UIText;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BlockPickerScreen extends CoreScreenLayer {

    private static final String CATEGORY_ALL = "All";
    private static final String CATEGORY_ITEMS = "Items";

    @In
    EntityManager entityManager;
    @In
    InventoryManager inventoryManager;
    @In
    AssetManager assetManager;
    @In
    private LocalPlayer localPlayer;

    UIDropdown dropdown;
    UIText filterText;

    List<EntityRef> allItemEntities;
    Binding<EntityRef> inventoryEntity;


    @Override
    public void initialise() {
        refreshAllItemEntities();

        EntityBuilder entityBuilder = entityManager.newBuilder();
        entityBuilder.addComponent(new InventoryComponent());
        entityBuilder.addComponent(new BlockPickerComponent());
        entityBuilder.setPersistent(false);
        entityBuilder.setOwner(localPlayer.getClientEntity());
        inventoryEntity = new DefaultBinding<>(entityBuilder.build());


        InventoryGrid blockPickerGrid = find("blockPickerGrid", InventoryGrid.class);
        blockPickerGrid.bindTargetEntity(inventoryEntity);

        //bind local player inventory below the main block picker screen
        InventoryGrid inventory = find("inventory", InventoryGrid.class);
        inventory.bindTargetEntity(new ReadOnlyBinding<EntityRef>() {
            @Override
            public EntityRef get() {
                return localPlayer.getCharacterEntity();
            }
        });
        inventory.setCellOffset(10);

        filterText = find("filterText", UIText.class);
        filterText.bindText(new Binding<String>() {
            String currentText;

            @Override
            public String get() {
                return currentText;
            }

            @Override
            public void set(String value) {
                currentText = value;
                refreshInventory();

            }
        });

        dropdown = find("categoryDropDown", UIDropdown.class);

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
        options.add(0, CATEGORY_ALL);
        options.add(1, CATEGORY_ITEMS);

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
                refreshInventory();
            }
        });
        dropdown.setSelection(CATEGORY_ALL);
    }


    private void refreshInventory() {
        String selectedCategory = (String) dropdown.getSelection();
        String filter = filterText.getText();
        if (filter != null) {
            filter = filter.toLowerCase();
        }

        InventoryComponent inventoryComponent = new InventoryComponent();
        for (EntityRef item : allItemEntities) {
            BlockItemComponent blockItemComponent = item.getComponent(BlockItemComponent.class);
            DisplayNameComponent displayNameComponent = item.getComponent(DisplayNameComponent.class);

            if (!matchesCategory(blockItemComponent, selectedCategory)) {
                continue;
            }
            if (!matchesFilter(blockItemComponent, displayNameComponent, filter)) {
                continue;
            }

            inventoryComponent.itemSlots.add(item);
        }
        inventoryEntity.get().saveComponent(inventoryComponent);
    }

    /**
     * Checks whether the given {@link org.terasology.engine.world.block.items.BlockItemComponent} matches the specific category.
     *
     * @param itemComponent the item's the block item (might be {@code null}).
     * @param category      the category to test in lower case.
     * @return {@code true} if the item matches the category, {@code false} otherwise.
     */
    private boolean matchesCategory(final BlockItemComponent itemComponent, final String category) {
        return (category.equals(CATEGORY_ALL))
            || (itemComponent == null && category.equals(CATEGORY_ITEMS)
            || (itemComponent != null && itemComponent.blockFamily != null && itemComponent.blockFamily.hasCategory(category)));
    }

    /**
     * Checks whether the given item matches the filter.
     * <p/>
     * Usually the given {@link org.terasology.engine.world.block.items.BlockItemComponent} and {@link org.terasology.engine.logic.common.DisplayNameComponent} belong to the same {@link
     * org.terasology.engine.entitySystem.entity.EntityRef}.
     *
     * @param itemComponent the item's block component to test (might be {@code null}).
     * @param nameComponent the item's name component to test (might be {@code null}).
     * @param filter        the filter string in lower case.
     * @return {@code true} if the item matches the filter, {@code false} otherwise.
     */
    private boolean matchesFilter(final BlockItemComponent itemComponent, final DisplayNameComponent nameComponent, final String filter) {
        return (filter == null || filter.isEmpty())
            || familyMatchesFilter(itemComponent, filter)
            || categoryMatchesFilter(itemComponent, filter)
            || nameMatchesFilter(nameComponent, filter);
    }

    /**
     * Checks whether the given {@link org.terasology.engine.logic.common.DisplayNameComponent} matches the given filter.
     *
     * @param nameComponent the name component to test against (might be {@code null}).
     * @param filter        the filter string in lower case.
     * @return {@code true} if the name component matches the filter, {@code false} othewise.
     */
    private boolean nameMatchesFilter(final DisplayNameComponent nameComponent, final String filter) {
        return (nameComponent != null && nameComponent.name != null && nameComponent.name.toLowerCase().contains(filter));
    }

    /**
     * Checks whether the {@link org.terasology.engine.world.block.family.BlockFamily}'s categories match the given filter.
     *
     * @param itemComponent the block item (might be {@code null}).
     * @param filter        the filter string in lower case.
     * @return {@code true} if one of the block's categories match the filter, {@code false} otherwise.
     */
    private boolean categoryMatchesFilter(final BlockItemComponent itemComponent, final String filter) {
        if (itemComponent != null && itemComponent.blockFamily != null) {
            Iterable<String> categories = itemComponent.blockFamily.getCategories();
            if (categories == null) {
                return false;
            }
            for (String category : categories) {
                if (category.toLowerCase().contains(filter)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether the {@link org.terasology.engine.world.block.family.BlockFamily} of the given {@link org.terasology.engine.world.block.items.BlockItemComponent} matches the given
     * filter.
     *
     * @param itemComponent the block item (might be {@code null}).
     * @param filter        the filter string in lower case.
     * @return {@code true} if the block family matches the filter, {@code false} otherwise.
     */
    private boolean familyMatchesFilter(final BlockItemComponent itemComponent, final String filter) {
        if (itemComponent != null && itemComponent.blockFamily != null && itemComponent.blockFamily.getDisplayName() != null) {
            final String familyDisplayName = itemComponent.blockFamily.getDisplayName().toLowerCase();
            return familyDisplayName.contains(filter);
        }
        return false;
    }

    @Override
    public boolean isModal() {
        return false;
    }

    private void refreshAllItemEntities() {
        BlockExplorer blockExplorer = new BlockExplorer(assetManager);

        allItemEntities = Lists.newArrayList();

        BlockManager blockManager = CoreRegistry.get(BlockManager.class);
        PrefabManager prefabManager = CoreRegistry.get(PrefabManager.class);

        for (Prefab prefab : prefabManager.listPrefabs()) {
            ItemComponent itemComp = prefab.getComponent(ItemComponent.class);
            if (itemComp != null) {
                try {
                    EntityBuilder entityBuilder = entityManager.newBuilder(prefab);
                    entityBuilder.setPersistent(false);
                    EntityRef entity = entityBuilder.build();
                    if (entity.exists() && entity.getComponent(ItemComponent.class) != null) {
                        // ensure there are the maximum amount of items in the stack
                        ItemComponent itemComponent = entity.getComponent(ItemComponent.class);
                        if (!itemComponent.stackId.isEmpty()) {
                            itemComponent.stackCount = itemComponent.maxStackSize;
                            entity.saveComponent(itemComponent);
                        }
                        allItemEntities.add(entity);
                    }
                } catch (Exception ex) {
                    // ignore all exceptions,  it will prevent bad blocks from breaking everything.
                }
            }
        }

        BlockItemFactory blockFactory = new BlockItemFactory(entityManager);
        // hash set so that duplicates are eliminated
        Set<BlockUri> blocks = Sets.newHashSet();

        Iterables.addAll(blocks, blockManager.listRegisteredBlockUris());
        Iterables.addAll(blocks, blockExplorer.getAvailableBlockFamilies());
        Iterables.addAll(blocks, blockExplorer.getFreeformBlockFamilies());

        List<BlockUri> blockList = Lists.newArrayList(blocks);
        blockList.sort((BlockUri o1, BlockUri o2) -> o1.toString().compareTo(o2.toString()));

        for (BlockUri block : blockList) {
            if (!block.equals(BlockManager.AIR_ID) && !block.equals(BlockManager.UNLOADED_ID)) {
                BlockFamily blockFamily = blockManager.getBlockFamily(block.getFamilyUri());
                EntityBuilder builder = blockFactory.newBuilder(blockFamily, 99);
                builder.setPersistent(false);
                BlockItemComponent blockItemComponent = builder.getComponent(BlockItemComponent.class);
                if (blockItemComponent != null && blockItemComponent.blockFamily != null) {
                    EntityRef entity = builder.build();
                    if (entity.exists()) {
                        allItemEntities.add(entity);
                    }
                }
            }
        }
    }
}
