// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockPicker.events;


import org.terasology.engine.input.BindButtonEvent;
import org.terasology.engine.input.DefaultBinding;
import org.terasology.engine.input.RegisterBindButton;
import org.terasology.nui.input.InputType;
import org.terasology.nui.input.Keyboard;

@RegisterBindButton(id = "blockPickerScreen", description = "Open Block Picker", category = "General")
@DefaultBinding(type = InputType.KEY, id = Keyboard.KeyId.B)
public class BlockPickerScreenButton extends BindButtonEvent {
}
