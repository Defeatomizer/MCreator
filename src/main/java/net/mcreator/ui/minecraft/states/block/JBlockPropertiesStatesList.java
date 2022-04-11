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
import net.mcreator.util.ListUtils;
import net.mcreator.util.Tuple;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JBlockPropertiesStatesList extends JEntriesList {

	private final List<JBlockPropertiesListEntry> propertiesList = new ArrayList<>();
	private final List<JBlockStatesListEntry> statesListVariants = new ArrayList<>();
	private final List<JBlockStatesListEntry> statesListMultipart = new ArrayList<>();
	private final AtomicInteger propertyId = new AtomicInteger(0);

	private final Border selected = BorderFactory.createLineBorder((Color) UIManager.get("MCreatorLAF.MAIN_TINT"), 2);
	private final Border unselected = BorderFactory.createEmptyBorder(2, 2, 2, 2);
	private final ButtonGroup variantsGroup = new ButtonGroup();
	private final ButtonGroup multipartGroup = new ButtonGroup();
	private JBlockStatesListEntry lastSelectedVariants;
	private JBlockStatesListEntry lastSelectedMultipart;
	private final ItemListener defaultVariantsStateUpdater;
	private final ItemListener defaultMultipartStateUpdater;
	private boolean updateRunning = false;

	private final JPanel propertyEntries = new JPanel(new GridLayout(0, 1, 5, 5));
	private final JPanel stateEntriesVariants = new JPanel(new GridLayout(0, 1, 5, 5));
	private final JPanel stateEntriesMultipart = new JPanel(new GridLayout(0, 1, 5, 5));

	private final JButton addProperty = new JButton(UIRES.get("16px.add.gif"));
	private final JButton addState = new JButton(UIRES.get("16px.add.gif"));
	private final JComboBox<String> statesType = new JComboBox<>(new String[] { "Variants", "Multipart" });

	private final List<String> builtinPropertyNames;
	//private final Map<String, PropertyData> builtinProperties = new LinkedHashMap<>();

	private final PropertyData logic = new PropertyData(Boolean.class, null, null, null);
	private final BiFunction<Integer, Integer, PropertyData> numMapper = (min, max) -> new PropertyData(Integer.class,
			min, max, null);
	private final Function<String[], PropertyData> enumMapper = arr -> new PropertyData(String.class, null, null, arr);

	public JBlockPropertiesStatesList(MCreator mcreator, IHelpContext gui) {
		super(mcreator, new BorderLayout(), gui);

		List<DataListEntry> builtinPropertyList = DataListLoader.loadDataList("blockstateproperties");
		/*builtinPropertyList.forEach(e -> {
			if (e.getType().equals("Logic")) {
				builtinProperties.put(e.getName(), logicMapper);
			} else if (e.getOther() instanceof Map<?, ?> map) {
				switch (e.getType()) {
				case "Number" -> builtinProperties.put(e.getName(),
						numMapper.apply(Integer.parseInt((String) map.get("min")),
								Integer.parseInt((String) map.get("max"))));
				case "Enum", "Direction" -> builtinProperties.put(e.getName(), enumMapper.apply(
						ListUtils.toStringArray((List<?>) map.get("enumValues"))));
				}
			}
		});*/
		builtinPropertyNames = builtinPropertyList.stream().map(DataListEntry::getName).toList();
		//propertyNameMapper = new NameMapper(mcreator.getWorkspace(), "blockstateproperties");

		setOpaque(false);
		propertyEntries.setOpaque(false);
		stateEntriesVariants.setOpaque(false);
		stateEntriesMultipart.setOpaque(false);

		propertyEntries.addContainerListener(new ContainerAdapter() {
			@Override public void componentRemoved(ContainerEvent event) {
				List<JBlockStatesListEntry> cache = ListUtils.merge(statesListVariants, statesListMultipart);
				if (propertiesList.size() > 0) {
					Map<String, PropertyData> propertiesMap = buildPropertiesMap();
					Set<String> filter = new HashSet<>();
					cache.forEach(e -> {
						e.state.setText(Arrays.stream(e.state.getText().split(","))
								.filter(el -> propertiesMap.containsKey(el.split("=")[0]))
								.collect(Collectors.joining(",")));
						if (e.state.getText() == null || (e.state.getText().equals("") && cache.size() > 0)
								|| !filter.add(e.state.getText()))
							e.removeEntry(stateEntriesVariants, statesListVariants);
					});
				} else {
					cache.forEach(e -> e.removeEntry(stateEntriesVariants, statesListVariants));
					addInitialState();
				}
			}
		});

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttons.setOpaque(false);

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

		addProperty.setText(L10N.t("elementgui.block.custom_properties.add"));
		addProperty.addActionListener(
				e -> chooser.show(addProperty, addProperty.getX(), addProperty.getY() + addProperty.getHeight() + 3));
		buttons.add(addProperty);

		addState.setText(L10N.t("elementgui.block.custom_states.add"));
		addState.addActionListener(e -> editState(null));
		addState.setEnabled(false);
		buttons.add(addState);

		defaultVariantsStateUpdater = e -> {
			if (lastSelectedVariants != null) {
				lastSelectedVariants.setBorder(unselected);
				lastSelectedVariants.window.setEnabled(true);
			}
			if (e.getStateChange() == ItemEvent.SELECTED) {
				try {
					lastSelectedVariants = (JBlockStatesListEntry) ((JToggleButton) e.getItemSelectable()).getParent();
					lastSelectedVariants.setBorder(selected);
					lastSelectedVariants.window.setEnabled(false);
				} catch (ClassCastException ignored) {
				}
			}
		};
		defaultMultipartStateUpdater = e -> {
			if (lastSelectedMultipart != null) {
				lastSelectedMultipart.setBorder(unselected);
				lastSelectedMultipart.window.setEnabled(true);
			}
			if (e.getStateChange() == ItemEvent.SELECTED) {
				try {
					lastSelectedMultipart = (JBlockStatesListEntry) ((JToggleButton) e.getItemSelectable()).getParent();
					lastSelectedMultipart.setBorder(selected);
					lastSelectedMultipart.window.setEnabled(false);
				} catch (ClassCastException ignored) {
				}
			}
		};

		JPanel topbar = new JPanel(new FlowLayout());
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

		CardLayout cards = new CardLayout();
		JPanel rightCards = new JPanel(cards);
		rightCards.setOpaque(false);
		rightCards.add("Variants", stateEntriesVariants);
		rightCards.add("Multipart", stateEntriesMultipart);

		statesType.addActionListener(e -> {
			if (e.getModifiers() != 0) {
				cards.show(rightCards, Objects.requireNonNullElse((String) statesType.getSelectedItem(), "Variants"));
				addState.setEnabled(Objects.equals(statesType.getSelectedItem(), "Multipart"));
			}
		});

		JScrollPane right = new JScrollPane(PanelUtils.pullElementUp(rightCards));
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
		statesListVariants.forEach(e -> e.setEnabled(enabled));
		statesListMultipart.forEach(e -> e.setEnabled(enabled));
	}

	public void reloadDataLists() {
		statesListVariants.forEach(JBlockStatesListEntry::reloadDataLists);
		statesListMultipart.forEach(JBlockStatesListEntry::reloadDataLists);
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

	private void addBuiltinProperty() {
		List<String> names = propertiesList.stream().map(e -> e.name.getText()).toList();
		DataListEntry property = DataListSelectorDialog.openSelectorDialog(mcreator,
				w -> DataListLoader.loadDataList("blockstateproperties").stream()
						.filter(e -> !e.getType().endsWith("Special") && !names.contains(e.getReadableName())).toList(),
				true, L10N.t("elementgui.block.custom_properties.add.title"),
				L10N.t("elementgui.block.custom_properties.add.message"));
		if (property.getOther() instanceof Map<?, ?> data) {
			Block.PropertyEntry entry = new Block.PropertyEntry();
			entry.name = property.getReadableName();
			entry.type = property.getType();
			entry.builtin = true;

			entry.defaultLogicValue =
					entry.type.equals("Logic") && Boolean.parseBoolean(data.get("default").toString());

			entry.minNumberValue = entry.type.equals("Number") ? Integer.parseInt(data.get("min").toString()) : 0;
			entry.maxNumberValue = entry.type.equals("Number") ? Integer.parseInt(data.get("max").toString()) : 1;
			entry.defaultNumberValue = entry.minNumberValue;

			/*if (entry.type.equals("Enum")) {
				String[] values = propertyNameMapper.getMapping(property.getName(), 2).split(",");
				entry.enumValues = Arrays.stream(values).map(e -> e.split(":")[1]).toArray(String[]::new);
			} else {
				entry.enumValues = new String[] {};
			}*/
			entry.enumValues = entry.type.equals("Enum") ?
					ListUtils.toStringArray(((Map<?, ?>) data.get("values")).keySet()) :
					new String[] {};

			addPropertiesEntry(0).setBuiltin().setEntry(entry.name, entry);
		}
	}

	private JBlockStatesListEntry addStatesEntry(boolean multipart) {
		JBlockStatesListEntry se = multipart ?
				new JBlockStatesListEntry(mcreator, gui, stateEntriesMultipart, statesListMultipart, true) :
				new JBlockStatesListEntry(mcreator, gui, stateEntriesVariants, statesListVariants, false);

		se.setBorder(unselected);
		se.edit.addActionListener(e -> editState(se));
		se.isDefault.addItemListener(multipart ? defaultMultipartStateUpdater : defaultVariantsStateUpdater);
		(Objects.equals(statesType.getSelectedItem(), "Variants") ? variantsGroup : multipartGroup).add(se.isDefault);
		if (multipart)
			se.isDefault.addActionListener(new ActionListener() {
				boolean status = false; // TODO

				@Override public void actionPerformed(ActionEvent e) {
					if (status)
						multipartGroup.clearSelection();
					status = se.isDefault.isSelected();
				}
			});

		registerEntryUI(se);
		return se;
	}

	public void addInitialState() {
		markStateAsDefault(addStatesEntry(false));
	}

	private void markStateAsDefault(JBlockStatesListEntry entry) {
		JToggleButton initial = entry.isDefault;
		initial.setSelected(true);
		defaultVariantsStateUpdater.itemStateChanged(
				new ItemEvent(initial, ItemEvent.ITEM_STATE_CHANGED, initial, ItemEvent.SELECTED));
	}

	private static List<String> propertyValues(Block.PropertyEntry entry) {
		return switch (Objects.requireNonNullElse(entry.type, "Logic")) {
			case "Logic" -> List.of("false", "true");
			case "Number" -> IntStream.rangeClosed(entry.minNumberValue, entry.maxNumberValue)
					.mapToObj(Integer::toString).toList();
			case "Enum" -> List.of(entry.enumValues);
			default -> List.of();
		};
	}

	private static String getValue(String state, String property) {
		String match = "," + property + "=";
		if (!("," + state).contains(match)) {
			return "";
		} else {
			String retVal = ("," + state).substring(("," + state).indexOf(match) + match.length());
			return !retVal.contains(",") ? retVal : retVal.substring(0, retVal.indexOf(","));
		}
	}

	private static String updateValue(String state, String property, String oldValue, String newValue) {
		if (state.equals("")) {
			return property + "=" + newValue;
		} else if (!("," + state).contains("," + property + "=")) {
			return state + "," + property + "=" + newValue;
		} else {
			String retVal = ("," + state + ",").replaceFirst("," + property + "=" + oldValue + ",",
					"," + property + "=" + newValue + ",");
			return retVal.substring(1, retVal.length() - 1);
		}
	}

	private static String removeValue(String state, String property) {
		String retVal = ("," + state + ",").replaceFirst("," + property + "=[a-z0-9-_]*,", ",");
		return retVal.equals(",") ? "" : retVal.substring(1, retVal.length() - 1);
	}

	private static List<Tuple<String, Block.ModelEntry>> mapStatesToEntries(List<JBlockStatesListEntry> list) {
		return list.stream().map(e -> new Tuple<>(e.state.getText(), e.getEntry())).toList();
	}

	private void propertyChanged(JBlockPropertiesListEntry entry, boolean existing) {
		if (!updateRunning && Objects.equals(statesType.getSelectedItem(), "Variants")) {
			updateRunning = true;
			new Thread(() -> {
				try {
					SwingUtilities.invokeAndWait(() -> {
						try {
							propertyChanged0(entry, existing); //TODO: Maybe it's better to have single unified method?
						} catch (Exception ignored) {
						}
					});
				} catch (InterruptedException | InvocationTargetException ignored) {
				}
				updateRunning = false;
			}).start();
		}
	}

	private void propertyChanged0(JBlockPropertiesListEntry entry, boolean existing) {
		String entryName = entry.name.getText();
		List<String> oldVals = !existing ? List.of("") : propertyValues(entry.getEntry(false));
		List<String> newVals = propertyValues(entry.getEntry(true));
		if (statesListVariants.isEmpty())
			addInitialState();

		String defaultState = lastSelectedVariants == null ? null : lastSelectedVariants.state.getText();
		boolean defaultFound = false;

		if (!existing) {
			List<Tuple<String, Block.ModelEntry>> cache = mapStatesToEntries(statesListVariants);
			for (int i = 0; i < cache.size(); i++) {
				String currState = cache.get(i).x();
				for (int j = 0; j < newVals.size(); j++) {
					int currIndex = i * newVals.size() + j;
					String newState = updateValue(currState, entryName, "", newVals.get(j));
					JBlockStatesListEntry se = currIndex < statesListVariants.size() ?
							statesListVariants.get(currIndex) :
							addStatesEntry(false);
					se.setEntry(newState, cache.get(i).y());
					if (!defaultFound && j == 0 && Objects.equals(defaultState, currState)) {
						markStateAsDefault(se);
						defaultFound = true;
					}
				}
			}
		} else {
			JPanel hidden = new JPanel();
			List<JBlockStatesListEntry> statesIterated = ListUtils.splitGroupsAndIterate(statesListVariants, oldVals.size(),
					e -> removeValue(e.state.getText(), entryName), e -> {
						Function<Integer, JBlockStatesListEntry> stateGetter = i -> i < e.size() ?
								e.get(i) :
								new JBlockStatesListEntry(mcreator, gui, hidden, e, false);
						String firstState = e.get(0).state.getText();
						Block.ModelEntry firstModel = e.get(0).getEntry();
						for (int i = 0; i < newVals.size(); i++) {
							if (i >= oldVals.size()) {
								String newState = updateValue(firstState, entryName, oldVals.get(0), newVals.get(i));
								stateGetter.apply(i).setEntry(newState, firstModel);
							} else {
								String newState = updateValue(e.get(i).state.getText(), entryName, oldVals.get(i),
										newVals.get(i));
								//Block.ModelEntry model = e.get(i).getEntry();
								stateGetter.apply(i).state.setText/*Entry*/(newState/*, model*/);
							}
						}
						if (e.size() >= newVals.size())
							e.subList(newVals.size(), e.size()).stream().toList().forEach(e::remove);
					});
			List<Tuple<String, Block.ModelEntry>> newStates = mapStatesToEntries(statesIterated);
			if (statesListVariants.size() >= newStates.size()) {
				statesListVariants.subList(newStates.size(), statesListVariants.size()).stream().toList()
						.forEach(e -> e.removeEntry(stateEntriesVariants, statesListVariants));
			}
			for (int i = 0; i < newStates.size(); i++) {
				Tuple<String, Block.ModelEntry> currState = newStates.get(i);
				JBlockStatesListEntry se =
						i < statesListVariants.size() ? statesListVariants.get(i) : addStatesEntry(false);
				se.setEntry(currState.x(), currState.y());
				String propVal = getValue(defaultState, entryName);
				if (!defaultFound && Objects.equals(defaultState, currState.x())) {
					markStateAsDefault(se);
					defaultFound = true;
				} else if (!defaultFound && defaultState != null && !newVals.contains(propVal) && Objects.equals(
						currState.x(), updateValue(defaultState, entryName, propVal, newVals.get(0)))) {
					markStateAsDefault(se);
					defaultFound = true;
				}
			}
			if (!defaultFound)
				markStateAsDefault(statesListVariants.get(0));
		}
	}

	private void propertyRenamed(JBlockPropertiesListEntry entry) {
		if (entry.renaming)
			return;

		entry.renaming = true;
		new Thread(() -> {
			if (getValidationResult(false).validateIsErrorFree()) {
				ListUtils.merge(statesListVariants, statesListMultipart).forEach(e -> e.state.setText(
						("," + e.state.getText()).replaceFirst("," + entry.getEntry(false).name + "=",
								"," + entry.name.getText() + "=").substring(1)));
				entry.getEntry(false).name = entry.name.getText();
			} else { // revert renaming to prevent possible conflicts
				SwingUtilities.invokeLater(() -> entry.name.setText(entry.getEntry(false).name));
			}
			entry.renaming = false;
		}).start();
	}

	private void editState(JBlockStatesListEntry entry) {
		if (Objects.equals(statesType.getSelectedItem(), "Multipart") && !propertiesList.isEmpty()
				&& getValidationResult(false).validateIsErrorFree()) {
			String newState = StateEditorDialog.open(mcreator, entry != null ? entry.state.getText() : "!new",
					buildPropertiesMap(), "block/custom_state").replace("!new", "").replace("!esc", "");
			if (/*newState.equals("")) // all properties were unchecked
				JOptionPane.showMessageDialog(mcreator, L10N.t("elementgui.block.custom_states.add.error_empty"),
						L10N.t("elementgui.block.custom_states.add.error_empty.title"), JOptionPane.ERROR_MESSAGE);
			else if (*/statesListMultipart.stream().anyMatch(el -> el != entry && el.state.getText().equals(newState)))
				JOptionPane.showMessageDialog(mcreator, L10N.t("elementgui.block.custom_states.add.error_duplicate"),
						L10N.t("elementgui.block.custom_states.add.error_duplicate.title"), JOptionPane.ERROR_MESSAGE);
			else if (!StateEditorDialog.isToken(newState)) // valid state was returned
				(entry != null ? entry : addStatesEntry(true)).state.setText(newState);
		} else {
			Toolkit.getDefaultToolkit().beep();
		}
	}

	private Map<String, PropertyData> buildPropertiesMap() {
		Map<String, PropertyData> props = new LinkedHashMap<>();
		propertiesList.forEach(e -> {
			switch (Objects.requireNonNullElse((String) e.type.getSelectedItem(), "Logic")) {
			case "Logic" -> props.put(e.name.getText(), logic);
			case "Number" -> props.put(e.name.getText(),
					numMapper.apply((int) e.minNumberValue.getValue(), (int) e.maxNumberValue.getValue()));
			case "Enum" -> props.put(e.name.getText(), enumMapper.apply(e.enumValues.getText().split(",")));
			}
		});
		return props;
	}

	public Map<String, Block.PropertyEntry> getProperties() {
		Map<String, Block.PropertyEntry> retVal = new LinkedHashMap<>();
		propertiesList.forEach(e -> retVal.put(e.name.getText(), e.getEntry(false)));
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

	public String getStatesType() {
		return Objects.requireNonNullElse((String) statesType.getSelectedItem(), "Variants");
	}

	public Map<String, Block.ModelEntry> getStatesVariants() {
		Map<String, Block.ModelEntry> retVal = new LinkedHashMap<>();
		statesListVariants.forEach(e -> retVal.put(e.state.getText(), e.getEntry()));
		return retVal;
	}

	public Map<String, Block.ModelEntry> getStatesMultipart() {
		Map<String, Block.ModelEntry> retVal = new LinkedHashMap<>();
		statesListMultipart.forEach(e -> retVal.put(e.state.getText(), e.getEntry()));
		return retVal;
	}

	public void setStatesType(String statesType) {
		this.statesType.setSelectedItem(statesType);
	}

	public void setStatesVariants(Map<String, Block.ModelEntry> states) {
		states.forEach((state, model) -> addStatesEntry(false).setEntry(state, model));
	}

	public void setStatesMultipart(Map<String, Block.ModelEntry> states) {
		states.forEach((state, model) -> addStatesEntry(true).setEntry(state, model));
	}

	public AggregatedValidationResult getValidationResult(boolean includeStates) {
		AggregatedValidationResult validationResult = new AggregatedValidationResult();
		propertiesList.forEach(e -> validationResult.addValidationGroup(e.getValidationResult()));
		if (includeStates)
			statesListVariants.forEach(e -> validationResult.addValidationGroup(e.getValidationResult()));
		return validationResult;
	}
}