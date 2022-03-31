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
import net.mcreator.element.types.Item;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.component.JEmptyBox;
import net.mcreator.ui.component.SearchableComboBox;
import net.mcreator.ui.component.util.ComboBoxUtil;
import net.mcreator.ui.component.util.ComponentUtils;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.dialogs.BlockItemTextureSelector;
import net.mcreator.ui.dialogs.MCreatorDialog;
import net.mcreator.ui.help.HelpUtils;
import net.mcreator.ui.help.IHelpContext;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.laf.renderer.ModelComboBoxRenderer;
import net.mcreator.ui.minecraft.TextureHolder;
import net.mcreator.ui.minecraft.boundingboxes.JBoundingBoxList;
import net.mcreator.ui.modgui.BlockGUI;
import net.mcreator.ui.validation.AggregatedValidationResult;
import net.mcreator.ui.validation.ValidationGroup;
import net.mcreator.ui.validation.validators.TileHolderValidator;
import net.mcreator.util.ListUtils;
import net.mcreator.workspace.resources.Model;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class JBlockStatesListEntry extends JPanel {

	private final MCreator mcreator;
	private final JComponent container;
	final JButton remove = new JButton(UIRES.get("16px.clear"));
	final JButton window = L10N.button("elementgui.block.custom_state.model_settings");

	final JLabel state = new JLabel();
	final JButton edit = new JButton(UIRES.get("16px.edit.gif"));
	final JComboBox<String> operator = new JComboBox<>(new String[] { "AND", "OR" });
	final JToggleButton isDefault;
	boolean multipart;

	private final TextureHolder texture;
	private final TextureHolder textureTop;
	private final TextureHolder textureLeft;
	private final TextureHolder textureFront;
	private final TextureHolder textureRight;
	private final TextureHolder textureBack;

	private final SearchableComboBox<Model> model = new SearchableComboBox<>(BlockGUI.builtInBlockModels());
	private final JSpinner xRot = new JSpinner(new SpinnerNumberModel(0, 0, 270, 90));
	private final JSpinner yRot = new JSpinner(new SpinnerNumberModel(0, 0, 270, 90));
	private final JCheckBox uvLock = L10N.checkbox("elementgui.block.custom_state.lock_textures");

	private final JBoundingBoxList bbList;
	private final JCheckBox bbOverride = L10N.checkbox("elementgui.block.custom_state.override_bounding_boxes");

	public JBlockStatesListEntry(MCreator mcreator, IHelpContext gui, JPanel parent,
			List<JBlockStatesListEntry> entryList, int index, boolean multipart) {
		super(new FlowLayout(FlowLayout.LEFT));
		this.mcreator = mcreator;
		this.multipart = multipart;

		container = PanelUtils.expandHorizontally(this);

		state.setOpaque(true);
		state.setBackground((Color) UIManager.get("MCreatorLAF.BLACK_ACCENT"));

		edit.setOpaque(false);
		edit.setMargin(new Insets(0, 0, 0, 0));
		edit.setBorder(BorderFactory.createEmptyBorder());
		edit.setContentAreaFilled(false);
		edit.setToolTipText(L10N.t("elementgui.block.custom_state.edit_state"));

		JButton copy = new JButton(UIRES.get("16px.copyclipboard"));
		copy.setOpaque(false);
		copy.setMargin(new Insets(0, 0, 0, 0));
		copy.setBorder(BorderFactory.createEmptyBorder());
		copy.setContentAreaFilled(false);
		copy.setToolTipText(L10N.t("elementgui.block.custom_state.copy_state"));
		copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new StringSelection(state.getText()), null));

		JScrollPane stateLabel = new JScrollPane(state);
		stateLabel.setOpaque(true);
		stateLabel.setPreferredSize(new Dimension(300, 30));

		JPanel statePane = multipart ? PanelUtils.join(edit, copy) : PanelUtils.join(copy);
		statePane.setOpaque(true);
		statePane.setBackground((Color) UIManager.get("MCreatorLAF.LIGHT_ACCENT"));

		isDefault = multipart ? new JCheckBox() : new JRadioButton();
		add(isDefault);
		add(stateLabel);
		add(statePane);
		add(operator);

		texture = new TextureHolder(new BlockItemTextureSelector(mcreator, BlockItemTextureSelector.TextureType.BLOCK));
		texture.setValidator(new TileHolderValidator(texture));
		textureTop = new TextureHolder(
				new BlockItemTextureSelector(mcreator, BlockItemTextureSelector.TextureType.BLOCK));
		textureTop.setValidator(new TileHolderValidator(textureTop));
		textureLeft = new TextureHolder(
				new BlockItemTextureSelector(mcreator, BlockItemTextureSelector.TextureType.BLOCK));
		textureLeft.setValidator(new TileHolderValidator(textureLeft));
		textureFront = new TextureHolder(
				new BlockItemTextureSelector(mcreator, BlockItemTextureSelector.TextureType.BLOCK));
		textureFront.setValidator(new TileHolderValidator(textureFront));
		textureRight = new TextureHolder(
				new BlockItemTextureSelector(mcreator, BlockItemTextureSelector.TextureType.BLOCK));
		textureRight.setValidator(new TileHolderValidator(textureRight));
		textureBack = new TextureHolder(
				new BlockItemTextureSelector(mcreator, BlockItemTextureSelector.TextureType.BLOCK));
		textureBack.setValidator(new TileHolderValidator(textureBack));

		textureLeft.setActionListener(e -> {
			if (!(texture.has() || textureTop.has() || textureBack.has() || textureFront.has() || textureRight.has())) {
				texture.setTextureFromTextureName(textureLeft.getID());
				textureTop.setTextureFromTextureName(textureLeft.getID());
				textureBack.setTextureFromTextureName(textureLeft.getID());
				textureFront.setTextureFromTextureName(textureLeft.getID());
				textureRight.setTextureFromTextureName(textureLeft.getID());
			}
		});

		JPanel textures = new JPanel(new GridLayout(3, 4));
		textures.setOpaque(false);

		textures.add(new JEmptyBox());
		textures.add(ComponentUtils.squareAndBorder(textureTop, L10N.t("elementgui.block.texture_place_top")));
		textures.add(new JEmptyBox());
		textures.add(new JEmptyBox());

		textures.add(ComponentUtils.squareAndBorder(textureLeft, new Color(126, 196, 255),
				L10N.t("elementgui.block.texture_place_left_overlay")));
		textures.add(ComponentUtils.squareAndBorder(textureFront, L10N.t("elementgui.block.texture_place_front_side")));
		textures.add(ComponentUtils.squareAndBorder(textureRight, L10N.t("elementgui.block.texture_place_right")));
		textures.add(ComponentUtils.squareAndBorder(textureBack, L10N.t("elementgui.block.texture_place_back")));

		textures.add(new JEmptyBox());
		textures.add(ComponentUtils.squareAndBorder(texture, new Color(125, 255, 174),
				L10N.t("elementgui.block.texture_place_bottom_main")));
		textures.add(new JEmptyBox());
		textures.add(new JEmptyBox());

		ComponentUtils.deriveFont(model, 16);
		model.setPreferredSize(new Dimension(350, 42));
		model.setRenderer(new ModelComboBoxRenderer());
		model.addActionListener(e -> modelChanged());
		reloadDataLists(); // we make sure that combo box can be properly shown

		JPanel visual = new JPanel();
		visual.setOpaque(false);
		JPanel modelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		modelPanel.setOpaque(false);

		modelPanel.add(HelpUtils.stackHelpTextAndComponent(gui.withEntry("block/state_model"),
				L10N.t("elementgui.block.custom_state.model"), model, 3));
		modelPanel.add(HelpUtils.stackHelpTextAndComponent(gui.withEntry("block/state_rotation"),
				L10N.t("elementgui.block.custom_state.rotation"), PanelUtils.join(xRot, yRot), 3));
		modelPanel.add(HelpUtils.wrapWithHelpButton(gui.withEntry("block/state_lock_textures"), uvLock));

		visual.add("North", textures);
		visual.add("South", modelPanel);

		bbList = new JBoundingBoxList(mcreator, gui);
		bbOverride.setSelected(true);

		JPanel bbPane = new JPanel(new BorderLayout());
		bbPane.setOpaque(false);
		bbPane.add("North", HelpUtils.wrapWithHelpButton(gui.withEntry("block/state_bounding_boxes"), bbOverride));
		bbPane.add("Center", bbList);

		JTabbedPane params = new JTabbedPane();
		params.addTab(L10N.t("elementgui.block.custom_state.model_textures"), visual);
		params.addTab(L10N.t("elementgui.block.custom_state.bounding_boxes"), bbPane);

		window.addActionListener(e -> {
			MCreatorDialog dialog = new MCreatorDialog(mcreator,
					L10N.t("elementgui.block.custom_state.model_settings"));
			dialog.getContentPane().add(params);
			dialog.setSize(800, 550);
			dialog.setLocationRelativeTo(mcreator);
			dialog.setVisible(true);
		});
		add(window);

		parent.add(container, index);
		entryList.add(index, this);

		remove.setText(L10N.t("elementgui.block.custom_state.remove"));
		remove.addActionListener(e -> removeEntry(parent, entryList));
		add(remove);

		parent.revalidate();
		parent.repaint();
	}

	void removeEntry(JPanel parent, List<JBlockStatesListEntry> entryList) {
		entryList.remove(JBlockStatesListEntry.this);
		parent.remove(container);
		parent.revalidate();
		parent.repaint();
	}

	private void modelChanged() {
		texture.setVisible(true);
		textureTop.setVisible(false);
		textureLeft.setVisible(false);
		textureFront.setVisible(false);
		textureRight.setVisible(false);
		textureBack.setVisible(false);

		if (BlockGUI.normal.equals(model.getSelectedItem())) {
			textureTop.setVisible(true);
			textureLeft.setVisible(true);
			textureFront.setVisible(true);
			textureRight.setVisible(true);
			textureBack.setVisible(true);
		} else if (BlockGUI.grassBlock.equals(model.getSelectedItem())) {
			textureTop.setVisible(true);
			textureLeft.setVisible(true);
			textureFront.setVisible(true);
		}
	}

	@Override public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		operator.setEnabled(enabled && multipart);
		window.setEnabled(enabled);
		remove.setEnabled(enabled && multipart);

		texture.setEnabled(enabled);
		textureTop.setEnabled(enabled);
		textureLeft.setEnabled(enabled);
		textureFront.setEnabled(enabled);
		textureRight.setEnabled(enabled);
		textureBack.setEnabled(enabled);

		model.setEnabled(enabled);
		xRot.setEnabled(enabled);
		yRot.setEnabled(enabled);
		uvLock.setEnabled(enabled);

		bbList.setEnabled(enabled);
		bbOverride.setEnabled(enabled);
	}

	public void reloadDataLists() {
		ComboBoxUtil.updateComboBoxContents(model, ListUtils.merge(Arrays.asList(BlockGUI.builtInBlockModels()),
				Model.getModelsWithTextureMaps(mcreator.getWorkspace()).stream()
						.filter(el -> el.getType() == Model.Type.JSON || el.getType() == Model.Type.OBJ).toList()));
	}

	void propertyRenamed(String property, String newName, int index) {
		String[] stateParts = state.getText().split(",");
		if (index >= stateParts.length || !stateParts[index].startsWith(property + "="))
			index = Arrays.stream(stateParts).map(e -> e.split("=")[0]).toList().indexOf(property);
		stateParts[index] = stateParts[index].replace(property + "=", newName + "=");
		state.setText(String.join(",", stateParts));
	}

	public Block.ModelEntry getEntry() {
		Block.ModelEntry retVal = new Block.ModelEntry();
		retVal.operator = (String) operator.getSelectedItem();

		retVal.modelTexture = texture.getID();
		retVal.modelTextureTop = textureTop.getID();
		retVal.modelTextureLeft = textureLeft.getID();
		retVal.modelTextureFront = textureFront.getID();
		retVal.modelTextureRight = textureRight.getID();
		retVal.modelTextureBack = textureBack.getID();

		retVal.modelName = Objects.requireNonNull(model.getSelectedItem()).getReadableName();
		retVal.renderType = Item.encodeModelType(Objects.requireNonNull(model.getSelectedItem()).getType());
		retVal.xRot = (int) xRot.getValue();
		retVal.yRot = (int) yRot.getValue();
		retVal.uvLock = uvLock.isSelected();

		retVal.boundingBoxes = bbList.getBoundingBoxes();
		retVal.bbOverride = bbOverride.isSelected();

		return retVal;
	}

	public void setEntry(String state, Block.ModelEntry value) {
		this.state.setText(state);
		this.operator.setSelectedItem(value.operator);

		this.texture.setTextureFromTextureName(value.modelTexture);
		this.textureTop.setTextureFromTextureName(value.modelTextureTop);
		this.textureLeft.setTextureFromTextureName(value.modelTextureLeft);
		this.textureFront.setTextureFromTextureName(value.modelTextureFront);
		this.textureRight.setTextureFromTextureName(value.modelTextureRight);
		this.textureBack.setTextureFromTextureName(value.modelTextureBack);

		this.model.setSelectedItem(value.getItemModel(mcreator.getWorkspace()));
		this.xRot.setValue(value.xRot);
		this.yRot.setValue(value.yRot);
		this.uvLock.setSelected(value.uvLock);

		this.bbList.setBoundingBoxes(value.boundingBoxes);
		this.bbOverride.setSelected(value.bbOverride);
	}

	public ValidationGroup getValidationResult() {
		return new AggregatedValidationResult(texture, textureTop, textureLeft, textureFront, textureRight,
				textureBack) {
			@Override public boolean validateIsErrorFree() {
				boolean retVal = super.validateIsErrorFree();
				JBlockStatesListEntry.this.setBackground(retVal ? new Color(0x464646) : new Color(0xBD4C4C));
				return retVal;
			}
		};
	}
}
