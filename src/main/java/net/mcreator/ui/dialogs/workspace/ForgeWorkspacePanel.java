/*
 * MCreator (https://mcreator.net/)
 * Copyright (C) 2020 Pylo and contributors
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

package net.mcreator.ui.dialogs.workspace;

import net.mcreator.generator.Generator;
import net.mcreator.generator.GeneratorConfiguration;
import net.mcreator.generator.GeneratorFlavor;
import net.mcreator.ui.component.JEmptyBox;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;

import javax.swing.*;
import java.awt.*;

public class ForgeWorkspacePanel extends AbstractWorkspacePanel {

	public ForgeWorkspacePanel(Window parent) {
		super(parent);

		add(new JEmptyBox(20, 20));

		add(PanelUtils.westAndEastElement(L10N.label("dialog.new_workspace.forge.display_name"),
				workspaceDialogPanel.modName));

		add(new JEmptyBox(10, 10));

		add(PanelUtils.westAndEastElement(L10N.label("dialog.new_workspace.forge.modid"), workspaceDialogPanel.modID));

		add(new JEmptyBox(10, 10));

		add(PanelUtils.westAndEastElement(L10N.label("dialog.new_workspace.forge.generator"),
				workspaceDialogPanel.generatorSelector));

		add(new JEmptyBox(30, 30));

		add(PanelUtils.westAndEastElement(L10N.label("dialog.new_workspace.forge.package"),
				workspaceDialogPanel.packageName));

		add(new JEmptyBox(30, 30));

		add(PanelUtils.westAndEastElement(L10N.label("dialog.new_workspace.forge.folder"),
				PanelUtils.centerAndEastElement(workspaceFolder, selectWorkspaceFolder, 0, 0)));

		add(new JEmptyBox(30, 170));

		add(PanelUtils.join(FlowLayout.LEFT, new JLabel(UIRES.get("18px.info")), new JEmptyBox(0, 0),
				L10N.label("dialog.new_workspace.forge.notice")));

		validationGroup.addValidationElement(workspaceDialogPanel.modName);
		validationGroup.addValidationElement(workspaceDialogPanel.modID);
		validationGroup.addValidationElement(workspaceDialogPanel.packageName);
		validationGroup.addValidationElement(workspaceFolder);

		workspaceDialogPanel.setFlavorFilter(GeneratorFlavor.FORGE);

		workspaceDialogPanel.generator.removeAllItems();
		Generator.GENERATOR_CACHE.values().stream().filter(gc -> gc.getGeneratorFlavor() == GeneratorFlavor.FORGE)
				.forEach(workspaceDialogPanel.generator::addItem);

		GeneratorConfiguration generatorConfiguration = GeneratorConfiguration.getRecommendedGeneratorForFlavor(
				Generator.GENERATOR_CACHE.values(), GeneratorFlavor.FORGE);
		workspaceDialogPanel.generator.setSelectedItem(generatorConfiguration);
	}
}
