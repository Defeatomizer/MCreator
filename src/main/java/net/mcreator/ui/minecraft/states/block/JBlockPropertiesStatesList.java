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
import net.mcreator.generator.mapping.NameMapper;
import net.mcreator.minecraft.DataListEntry;
import net.mcreator.minecraft.DataListLoader;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.dialogs.DataListSelectorDialog;
import net.mcreator.ui.dialogs.StateEditorDialog;
import net.mcreator.ui.help.HelpUtils;
import net.mcreator.ui.help.IHelpContext;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.minecraft.JEntriesList;
import net.mcreator.ui.minecraft.states.PropertyData;
import net.mcreator.ui.validation.AggregatedValidationResult;
import net.mcreator.ui.validation.validators.PropertyNameValidator;
import net.mcreator.ui.validation.validators.RegistryNameValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JBlockPropertiesStatesList extends JEntriesList {

	private final List<JBlockPropertiesListEntry> propertiesList = new ArrayList<>();
	private final List<JBlockStatesListEntry> statesList = new ArrayList<>();
	private final AtomicInteger propertyId = new AtomicInteger(0);

	private final ButtonGroup statesGroup = new ButtonGroup();
	private JBlockStatesListEntry lastSelected;
	private final ItemListener defaultStateUpdater;

	private final NameMapper propertyNameMapper;
	private final List<String> builtinPropertyNames;
	private final Map<String, PropertyData> builtinProperties = new LinkedHashMap<>();

	private final PropertyData logicMapper = new PropertyData(Boolean.class, null, null, null);
	private final BiFunction<Integer, Integer, PropertyData> numMapper = (min, max) -> new PropertyData(Integer.class,
			min, max, null);
	private final Function<String[], PropertyData> enumMapper = arr -> new PropertyData(String.class, null, null, arr);

	private final JPanel propertyEntries = new JPanel(new GridLayout(0, 1, 5, 5));
	private final JPanel stateEntries = new JPanel(new GridLayout(0, 1, 5, 5));

	private final JButton addProperty = new JButton(UIRES.get("16px.add.gif"));
	private final JButton addState = new JButton(UIRES.get("16px.add.gif"));
	private final JComboBox<String> statesType = new JComboBox<>(new String[] { "Variants", "Multipart" });

	public JBlockPropertiesStatesList(MCreator mcreator, IHelpContext gui) {
		super(mcreator, new BorderLayout(), gui);

		List<DataListEntry> builtinPropertyList = DataListLoader.loadDataList("blockstateproperties");
		builtinPropertyList.forEach(e -> {
			if (e.getType().equals("Logic")) {
				builtinProperties.put(e.getName(), logicMapper);
			} else if (e.getOther() instanceof Map<?, ?> map) {
				switch (e.getType()) {
				case "Number" -> builtinProperties.put(e.getName(),
						numMapper.apply(Integer.parseInt((String) map.get("min")),
								Integer.parseInt((String) map.get("max"))));
				case "Enum", "Direction" -> builtinProperties.put(e.getName(), enumMapper.apply(
						((List<?>) map.get("enumValues")).stream().map(Object::toString).toArray(String[]::new)));
				}
			}
		});
		builtinPropertyNames = builtinPropertyList.stream().map(DataListEntry::getName).toList();
		propertyNameMapper = new NameMapper(mcreator.getWorkspace(), "blockstateproperties");

		setOpaque(false);
		propertyEntries.setOpaque(false);
		stateEntries.setOpaque(false);

		propertyEntries.addContainerListener(new ContainerAdapter() {
			@Override public void componentRemoved(ContainerEvent e) {
				trimStates();
			}
		});

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttons.setOpaque(false);

		addProperty.setText(L10N.t("elementgui.block.custom_properties.add"));
		addProperty.addActionListener(e -> showPropertyTypeChooser());
		buttons.add(addProperty);

		addState.setText(L10N.t("elementgui.block.custom_states.add"));
		addState.addActionListener(e -> editState(null));
		addState.setEnabled(false);
		buttons.add(addState);

		statesType.addActionListener(e -> {
			if (e.getModifiers() != 0) {
				addState.setEnabled(Objects.equals(statesType.getSelectedItem(), "Multipart"));
				statesList.forEach(s -> s.setEditingLimited(Objects.equals(statesType.getSelectedItem(), "Multipart")));
			}
		});

		defaultStateUpdater = e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				if (lastSelected != null) {
					lastSelected.getValidationResult().validateIsErrorFree();
					lastSelected.window.setEnabled(true);
				}
				try {
					lastSelected = (JBlockStatesListEntry) ((JRadioButton) e.getItemSelectable()).getParent();
					lastSelected.setBorder(JBlockStatesListEntry.selected);
					lastSelected.window.setEnabled(false);
				} catch (ClassCastException ignored) {
				}
			}
		};

		// default state type is "variants", so we initialize default state entry
		JRadioButton initial = addStatesEntry(0).isDefault;
		initial.setSelected(true);
		defaultStateUpdater.itemStateChanged(
				new ItemEvent(initial, ItemEvent.ITEM_STATE_CHANGED, initial, ItemEvent.SELECTED));

		JPanel topbar = new JPanel(new BorderLayout());
		topbar.setOpaque(false);
		topbar.add("West", HelpUtils.wrapWithHelpButton(gui.withEntry("block/states_type"),
				L10N.label("elementgui.block.custom_states.type")));
		topbar.add("East", statesType);

		JScrollPane left = new JScrollPane(PanelUtils.pullElementUp(propertyEntries));
		left.setOpaque(false);
		left.getViewport().setOpaque(false);
		left.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder((Color) UIManager.get("MCreatorLAF.BRIGHT_COLOR"), 2),
				L10N.t("elementgui.block.custom_properties.title"), 0, 0, getFont().deriveFont(12.0f),
				(Color) UIManager.get("MCreatorLAF.BRIGHT_COLOR")));

		JScrollPane right = new JScrollPane(PanelUtils.pullElementUp(stateEntries));
		right.setOpaque(false);
		right.getViewport().setOpaque(false);
		right.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder((Color) UIManager.get("MCreatorLAF.BRIGHT_COLOR"), 2),
				L10N.t("elementgui.block.custom_states.title"), 0, 0, getFont().deriveFont(12.0f),
				(Color) UIManager.get("MCreatorLAF.BRIGHT_COLOR")));

		add("North", PanelUtils.centerInPanel(HelpUtils.wrapWithHelpButton(gui.withEntry("common/custom_states"),
				PanelUtils.westAndEastElement(buttons, topbar), SwingConstants.LEFT)));
		add("Center", PanelUtils.gridElements(1, 0, left, right));
	}

	@Override public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		addProperty.setEnabled(enabled);
		addState.setEnabled(enabled);
		statesType.setEnabled(enabled);

		propertiesList.forEach(e -> e.setEnabled(enabled));
		statesList.forEach(e -> e.setEnabled(enabled));
	}

	public void reloadDataLists() {
		statesList.forEach(JBlockStatesListEntry::reloadDataLists);
	}

	private JBlockPropertiesListEntry addPropertiesEntry(int propertyId) {
		JBlockPropertiesListEntry pe = new JBlockPropertiesListEntry(mcreator, gui, propertyEntries, propertiesList,
				propertyId);

		pe.name.setValidator(new PropertyNameValidator(pe.name, "Property name",
				() -> propertiesList.stream().map(e -> e.name.getText()), builtinPropertyNames,
				new RegistryNameValidator(pe.name, "Property name")));
		pe.name.enableRealtimeValidation();
		pe.name.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e) {
				propertyRenamed(pe);
			}

			@Override public void removeUpdate(DocumentEvent e) {
				propertyRenamed(pe);
			}

			@Override public void changedUpdate(DocumentEvent e) {
				propertyRenamed(pe);
			}
		});

		pe.type.addActionListener(e -> propertyChanged(pe, true));
		pe.defaultLogicValue.addActionListener(e -> propertyChanged(pe, true));
		pe.minNumberValue.addChangeListener(e -> propertyChanged(pe, true));
		pe.maxNumberValue.addChangeListener(e -> propertyChanged(pe, true));
		pe.enumValues.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e) {
				propertyChanged(pe, true);
			}

			@Override public void removeUpdate(DocumentEvent e) {
				propertyChanged(pe, true);
			}

			@Override public void changedUpdate(DocumentEvent e) {
				propertyChanged(pe, true);
			}
		});
		if (Objects.equals(statesType.getSelectedItem(), "Variants"))
			propertyChanged(pe, false);
		registerEntryUI(pe);

		return pe;
	}

	private JBlockStatesListEntry addStatesEntry(int stateId) {
		JBlockStatesListEntry se = new JBlockStatesListEntry(mcreator, gui, stateEntries, statesList, stateId,
				this::editState);

		statesGroup.add(se.isDefault);
		se.isDefault.addItemListener(defaultStateUpdater);
		if (Objects.equals(statesType.getSelectedItem(), "Variants"))
			se.setEditingLimited(false);

		registerEntryUI(se);
		return se;
	}

	private void trimStates() {
		if (propertiesList.size() > 0) {
			Map<String, PropertyData> propertiesMap = buildPropertiesMap();
			statesList.forEach(s -> s.state.setText(Arrays.stream(s.state.getText().split(","))
					.filter(el -> propertiesMap.containsKey(el.split("=")[0])).collect(Collectors.joining(","))));
			Set<String> duplicates = new HashSet<>(); // when states are trimmed, we remove possible duplicates
			statesList.stream().toList().forEach(e -> {
				if (e.state.getText() == null || e.state.getText().equals("") || !duplicates.add(e.state.getText()))
					e.remove.doClick();
			});
		} else if (Objects.equals(statesType.getSelectedItem(), "Variants")) {
			statesList.forEach(e -> e.remove.doClick());
		}
	}

	private String updatePropertyValue(String state, String property, String oldValue, String newValue) {
		return ("," + state).replaceFirst("," + property + "=" + oldValue + ",", "," + property + "=" + newValue + ",")
				.substring(1);
	}

	private List<String> propertyValues(Block.PropertyEntry entry) {
		List<String> retVal = new ArrayList<>();
		switch (Objects.requireNonNullElse(entry.type, "Logic")) {
		case "Logic" -> retVal.addAll(Arrays.asList("false", "true"));
		case "Number" -> {
			for (int i = entry.minNumberValue; i <= entry.maxNumberValue; i++)
				retVal.add(Integer.toString(i));
		}
		case "Enum" -> retVal.addAll(Arrays.asList(entry.enumValues));
		}
		return retVal;
	}

	private void showPropertyTypeChooser() {
		JPopupMenu chooser = new JPopupMenu();

		JMenuItem logic = new JMenuItem("Logic");
		logic.addActionListener(e -> {
			propertyId.set(Math.max(propertiesList.size(), propertyId.get()) + 1);
			addPropertiesEntry(propertyId.get()).type.setSelectedItem("Logic");
		});
		chooser.add(logic);

		JMenuItem number = new JMenuItem("Number");
		number.addActionListener(e -> {
			propertyId.set(Math.max(propertiesList.size(), propertyId.get()) + 1);
			addPropertiesEntry(propertyId.get()).type.setSelectedItem("Number");
		});
		chooser.add(number);

		JMenuItem string = new JMenuItem("Enum");
		string.addActionListener(e -> {
			propertyId.set(Math.max(propertiesList.size(), propertyId.get()) + 1);
			addPropertiesEntry(propertyId.get()).type.setSelectedItem("Enum");
		});
		chooser.add(string);

		JMenuItem builtin = new JMenuItem("Built-in...");
		builtin.addActionListener(e -> addBuiltinProperty());
		chooser.add(builtin);

		chooser.show(addProperty, addProperty.getX(), addProperty.getY() + addProperty.getHeight() + 3);
	}

	private void addBuiltinProperty() {
		List<String> names = propertiesList.stream().map(e -> e.name.getText()).toList();
		DataListEntry property = DataListSelectorDialog.openSelectorDialog(mcreator,
				w -> DataListLoader.loadDataList("blockstateproperties").stream()
						.filter(e -> !names.contains(e.getReadableName())).toList(),
				L10N.t("elementgui.block.custom_properties.add.title"),
				L10N.t("elementgui.block.custom_properties.add.message"));
		if (property.getOther() instanceof Map<?, ?> data) {
			Block.PropertyEntry entry = new Block.PropertyEntry();
			entry.name = property.getReadableName();
			entry.type = property.getType();
			entry.builtin = true;

			entry.defaultLogicValue =
					entry.type.equals("Logic") && Boolean.parseBoolean(data.get("default").toString());

			entry.defaultNumberValue = entry.type.equals("Number") ?
					Integer.parseInt(data.get("default").toString()) :
					0;
			entry.minNumberValue = entry.type.equals("Number") ? Integer.parseInt(data.get("min").toString()) : 0;
			entry.maxNumberValue = entry.type.equals("Number") ? Integer.parseInt(data.get("max").toString()) : 1;

			if (entry.type.equals("Enum")) {
				String[] values = propertyNameMapper.getMapping(property.getName(), 2).split(",");
				entry.enumValues = Arrays.stream(values).map(e -> e.split(":")[1]).toArray(String[]::new);
			} else {
				entry.enumValues = new String[] {};
			}

			addPropertiesEntry(0).setBuiltin().setEntry(entry.name, entry);
		}
	}

	private void propertyChanged(JBlockPropertiesListEntry entry, boolean existing) {
		if (Objects.equals(statesType.getSelectedItem(), "Variants")) {
			new Thread(() -> {
				try {
					SwingUtilities.invokeAndWait(() -> {
						try {
							propertyChanged0(entry, existing); //TODO: Maybe it's better to have single unified method?
						} catch (Exception ex) {
							LogManager.getLogger("WeAreThere").error(ex.getMessage(), ex);
						}
					});
				} catch (InterruptedException | InvocationTargetException e) {
					LogManager.getLogger("WeAreThere").fatal(e.getMessage(), e);
					try {
						propertyChanged0(entry, existing);
					} catch (Exception ex) {
						LogManager.getLogger("WeAreThere").warn(ex.getMessage(), ex);
					}
				}
			}).start();
		}
	}

	// TODO: When we have 0 or 1 states, when switching from multipart to variants
	private void propertyChanged0(JBlockPropertiesListEntry entry, boolean existing) {
		Logger LOG = LogManager.getLogger("WeAreHere");
		String entryName = entry.name.getText();
		List<String> oldVals = propertiesList.isEmpty() ? Collections.singletonList("") : propertyValues(entry.cached);
		List<String> newVals = propertyValues(entry.getEntry());
		String firstState = statesList.get(0).state.getText();
		Block.ModelEntry firstModel = statesList.get(0).getEntry();
		int item = 0, done = 0;
		while (done <= statesList.size()) {
			LOG.debug("Iterating at " + done + ":" + item);
			LOG.debug((item > oldVals.size()) + "(old=" + oldVals.size() + ")-(new=" + newVals.size() + ")" + (item > newVals.size()));
			if (item == 0) {
				LOG.fatal("Over!..");
				firstState = statesList.get(done).state.getText();
				firstModel = statesList.get(done).getEntry();
				LOG.fatal(firstState);
			}
			if (item >= oldVals.size() && item >= newVals.size()) {
				LOG.error("i = 0");
				done += item;
				item = 0;
			} else if (item >= oldVals.size()) {
				LOG.warn("model[i] = model[0]");
				addStatesEntry(done + item).setEntry(
						updatePropertyValue(firstState, entryName, oldVals.get(0), newVals.get(item)), firstModel);
			} else if (item >= newVals.size()) {
				LOG.warn("state[i] = state[0]");
				statesList.get(done + item).state.setText(firstState);
			} else {
				String currState = statesList.get(done + item).state.getText();
				LOG.info(currState);
				statesList.get(done + item).state.setText(existing ?
						updatePropertyValue(currState, entryName, oldVals.get(item), newVals.get(item)) :
						(currState.equals("") ? "" : currState + ",") + entryName + "=" + newVals.get(item));
			}
			LOG.debug("And, as we continue...");
			item++;
		}
		LOG.debug("Nicely done!");
		trimStates();
	}

	private void propertyRenamed(JBlockPropertiesListEntry entry) {
		getValidationResult(false).validateIsErrorFree(); // this highlights all the property names errors
		statesList.forEach(s -> {
			int indexBuiltin = (int) Arrays.stream(s.state.getText().split(","))
					.filter(el -> builtinPropertyNames.contains(el.split("=")[0])).count();
			int indexCustom = propertiesList.stream()
					.filter(e -> ("," + s.state.getText()).contains("," + e.nameString + "=")).toList().indexOf(entry);
			s.propertyRenamed(entry.nameString, entry.name.getText(), indexBuiltin + indexCustom);
		});
		entry.nameString = entry.name.getText();
	}

	private void editState(JBlockStatesListEntry entry) {
		if (Objects.equals(statesType.getSelectedItem(), "Multipart") && !propertiesList.isEmpty()
				&& getValidationResult(false).validateIsErrorFree()) {
			String newState = StateEditorDialog.open(mcreator, entry != null ? entry.state.getText() : "!new",
					buildPropertiesMap(), "block/custom_state").replace("!new", "").replace("!esc", "");
			if (newState.equals("")) // all properties were unchecked
				JOptionPane.showMessageDialog(mcreator, L10N.t("elementgui.block.custom_states.add.error_empty"),
						L10N.t("elementgui.block.custom_states.add.error_empty.title"), JOptionPane.ERROR_MESSAGE);
			else if (statesList.stream().anyMatch(el -> el != entry && el.state.getText().equals(newState)))
				JOptionPane.showMessageDialog(mcreator, L10N.t("elementgui.block.custom_states.add.error_duplicate"),
						L10N.t("elementgui.block.custom_states.add.error_duplicate.title"), JOptionPane.ERROR_MESSAGE);
			else // valid state was returned
				(entry != null ? entry : addStatesEntry(statesList.size())).state.setText(newState);
		} else {
			Toolkit.getDefaultToolkit().beep();
		}
	}

	private Map<String, PropertyData> buildPropertiesMap() {
		Map<String, PropertyData> props = new LinkedHashMap<>(builtinProperties);
		propertiesList.forEach(e -> {
			switch (Objects.requireNonNullElse((String) e.type.getSelectedItem(), "Logic")) {
			case "Logic" -> props.put(e.name.getText(), logicMapper);
			case "Number" -> props.put(e.name.getText(),
					numMapper.apply((int) e.minNumberValue.getValue(), (int) e.maxNumberValue.getValue()));
			case "Enum" -> props.put(e.name.getText(), enumMapper.apply(e.enumValues.getText().split(",")));
			}
		});
		return props;
	}

	public Map<String, Block.PropertyEntry> getProperties() {
		Map<String, Block.PropertyEntry> retVal = new LinkedHashMap<>();
		propertiesList.forEach(e -> retVal.put(e.name.getText(), e.getEntry()));
		return retVal;
	}

	public void setProperties(Map<String, Block.PropertyEntry> properties) {
		properties.forEach((name, value) -> {
			propertyId.set(Math.max(propertiesList.size(), propertyId.get()) + 1);
			if (name.startsWith("property")) {
				try {
					propertyId.set(Math.max(propertyId.get(), Integer.parseInt(name.substring("property".length()))));
				} catch (NumberFormatException ignored) {
				}
			}
			addPropertiesEntry(propertyId.get()).setEntry(name, value);
		});
	}

	public Map<String, Block.ModelEntry> getStates() {
		Map<String, Block.ModelEntry> retVal = new LinkedHashMap<>();
		statesList.forEach(e -> retVal.put(e.state.getText(), e.getEntry()));
		return retVal;
	}

	public void setStates(Map<String, Block.ModelEntry> states) {
		states.forEach((state, model) -> addStatesEntry(statesList.size()).setEntry(state, model));
	}

	public AggregatedValidationResult getValidationResult(boolean includeStates) {
		AggregatedValidationResult validationResult = new AggregatedValidationResult();
		propertiesList.forEach(e -> validationResult.addValidationGroup(e.getValidationResult()));
		if (includeStates)
			statesList.forEach(e -> validationResult.addValidationGroup(e.getValidationResult()));
		return validationResult;
	}
}