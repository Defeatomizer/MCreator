/*
 * MCreator (https://mcreator.net/)
 * Copyright (C) 2012-2020, Pylo
 * Copyright (C) 2020-2022, Pylo, opensource contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.mcreator.ui.minecraft.states.block;

import net.mcreator.element.types.Block;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.help.HelpUtils;
import net.mcreator.ui.help.IHelpContext;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.validation.AggregatedValidationResult;
import net.mcreator.ui.validation.ValidationGroup;
import net.mcreator.ui.validation.Validator;
import net.mcreator.ui.validation.component.VTextField;
import net.mcreator.ui.validation.validators.RegistryNameValidator;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

public class JBlockPropertiesListEntry extends JPanel {

	private final JComponent container;

	final VTextField name = new VTextField(20);
	boolean renaming = false; // TODO: Resolve cases of names with length 1
	private final Block.PropertyEntry cached = new Block.PropertyEntry();
	private boolean builtin = false;

	final JComboBox<String> type = new JComboBox<>(new String[] { "Logic", "Number", "Enum" });
	private final JComboBox<String> defaultLogicValue = new JComboBox<>(new String[] { "false", "true" });
	private final JSpinner defaultNumberValue = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
	final JSpinner minNumberValue = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE - 1, 1));
	final JSpinner maxNumberValue = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
	final VTextField enumValues = new VTextField(20);

	public JBlockPropertiesListEntry(MCreator mcreator, IHelpContext gui, JPanel parent,
			List<JBlockPropertiesListEntry> entryList, int propertyId) {
		super(new FlowLayout(FlowLayout.LEFT));
		cached.name = "property" + propertyId;
		name.setText(cached.name);

		container = PanelUtils.expandHorizontally(this);

		CardLayout layout = new CardLayout();
		JPanel tps = new JPanel(layout);
		tps.add("Logic", PanelUtils.centerInPanel(defaultLogicValue));
		tps.add("Number", PanelUtils.stack(3, defaultNumberValue, minNumberValue, maxNumberValue));
		tps.add("Enum", PanelUtils.centerInPanel(enumValues));
		type.addActionListener(e -> layout.show(tps, (String) type.getSelectedItem()));

		final String title = "Enum value strings";
		enumValues.setText("none,default");
		enumValues.setValidator(new RegistryNameValidator(enumValues, title) {
			@Override public ValidationResult validate() {
				if (Objects.equals(type.getSelectedItem(), "Enum")) {
					for (String text : enumValues.getText().split(",")) {
						if (text.length() == 0 && !isAllowEmpty())
							return new ValidationResult(Validator.ValidationResultType.ERROR,
									L10N.t("validators.registry_name.empty", title));
					}

					for (String text : enumValues.getText().split(",")) {
						if (text.length() > getMaxLength())
							return new ValidationResult(Validator.ValidationResultType.ERROR,
									L10N.t("validators.registry_name.length", title, getMaxLength()));
					}

					for (String text : enumValues.getText().split(",")) {
						char[] chars = text.toCharArray();
						boolean valid = true;
						int id = 0;
						for (char c : chars) {
							if (id == 0 && (c >= '0' && c <= '9' || getValidChars().contains(c))) {
								valid = false;
								break;
							}

							if (!isLCLetterOrDigit(c) && !getValidChars().contains(c)) {
								valid = false;
								break;
							}

							id++;
						}
						if (!valid)
							return new ValidationResult(Validator.ValidationResultType.ERROR,
									L10N.t("validators.registry_name.invalid", title, getValidChars().toString()));
					}
				}

				return ValidationResult.PASSED;
			}
		});

		add(HelpUtils.stackHelpTextAndComponent(gui.withEntry("block/custom_property_name"),
				L10N.t("elementgui.block.custom_property.name"), name, 3));
		add(HelpUtils.stackHelpTextAndComponent(gui.withEntry("block/custom_property_type"),
				L10N.t("elementgui.block.custom_property.type"), type, 3));
		add(HelpUtils.stackHelpTextAndComponent(gui.withEntry("block/custom_property_values"),
				L10N.t("elementgui.block.custom_property.default_value"), tps, 3));

		parent.add(container);
		entryList.add(this);

		JButton remove = new JButton(UIRES.get("16px.clear"));
		remove.setText(L10N.t("elementgui.block.custom_property.remove"));
		remove.addActionListener(e -> {
			entryList.remove(this);
			parent.remove(container);
			parent.revalidate();
			parent.repaint();
		});
		add(remove);

		parent.revalidate();
		parent.repaint();
	}

	@Override public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		name.setEnabled(enabled && !builtin);
		type.setEnabled(enabled && !builtin);
		defaultLogicValue.setEnabled(enabled);
		defaultNumberValue.setEnabled(enabled);
		minNumberValue.setEnabled(enabled && !builtin);
		maxNumberValue.setEnabled(enabled && !builtin);
		enumValues.setEnabled(enabled && !builtin);
	}

	JBlockPropertiesListEntry setBuiltin() {
		builtin = true;
		name.setEnabled(false);
		type.setEnabled(false);
		minNumberValue.setEnabled(false);
		maxNumberValue.setEnabled(false);
		enumValues.setEnabled(false);
		return this;
	}

	public Block.PropertyEntry getEntry() {
		return getEntry(false);
	}

	Block.PropertyEntry getEntry(boolean cache) {
		if (cache) {
			cached.name = name.getText();
			cached.type = Objects.requireNonNullElse((String) type.getSelectedItem(), "Logic");
			cached.builtin = builtin;
			cached.defaultLogicValue = Boolean.parseBoolean((String) defaultLogicValue.getSelectedItem());
			cached.defaultNumberValue = (int) defaultNumberValue.getValue();
			cached.minNumberValue = (int) minNumberValue.getValue();
			cached.maxNumberValue = (int) maxNumberValue.getValue();
			cached.enumValues = enumValues.getText().split(",");
		}
		return cached;
	}

	public void setEntry(String name, Block.PropertyEntry value) {
		this.name.setText(name);

		this.type.setSelectedItem(value.type);
		this.builtin = value.builtin;
		this.defaultLogicValue.setSelectedItem(Boolean.toString(value.defaultLogicValue));
		this.defaultNumberValue.setValue(value.defaultNumberValue);
		this.minNumberValue.setValue(value.minNumberValue);
		this.maxNumberValue.setValue(value.maxNumberValue);
		this.enumValues.setText(String.join(",", value.enumValues));
	}

	public ValidationGroup getValidationResult() {
		return Objects.equals(type.getSelectedItem(), "Enum") ?
				new AggregatedValidationResult(name, enumValues) :
				new AggregatedValidationResult(name);
	}
}