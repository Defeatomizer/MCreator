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

package net.mcreator.ui.action.impl.workspace.resources;

import net.mcreator.generator.GeneratorStats;
import net.mcreator.io.FileIO;
import net.mcreator.minecraft.RegistryNameFixer;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.action.ActionRegistry;
import net.mcreator.ui.action.BasicAction;
import net.mcreator.ui.dialogs.file.FileDialogs;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.modgui.ModElementGUI;
import net.mcreator.util.FilenameUtilsPatched;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class StructureImportActions {

	public static class ImportStructure extends BasicAction {
		public ImportStructure(ActionRegistry actionRegistry) {
			super(actionRegistry, L10N.t("action.workspace.resources.import_structure"), actionEvent -> {
				File[] schs = FileDialogs.getMultiOpenDialog(actionRegistry.getMCreator(), new String[] { ".nbt" });
				if (schs != null)
					importStructure(actionRegistry.getMCreator(), schs);
			});
		}

		@Override public boolean isEnabled() {
			return actionRegistry.getMCreator().getGeneratorStats().getBaseCoverageInfo().get("structures")
					!= GeneratorStats.CoverageStatus.NONE;
		}
	}

	public static void importStructure(MCreator mcreator, File[] schs) {
		Arrays.stream(schs).forEach(sch -> FileIO.copyFile(sch, new File(mcreator.getFolderManager().getStructuresDir(),
				Objects.requireNonNull(RegistryNameFixer.fix(sch.getName())))));
		mcreator.reloadWorkspaceTabContents();
		if (mcreator.getTabs().getCurrentTab().getContent() instanceof ModElementGUI)
			((ModElementGUI<?>) mcreator.getTabs().getCurrentTab().getContent()).reloadDataLists();
	}

	public static class ImportStructureFromMinecraft extends BasicAction {
		public ImportStructureFromMinecraft(ActionRegistry actionRegistry) {
			super(actionRegistry, L10N.t("action.workspace.resources.import_structure_from_minecraft"), actionEvent -> {
				List<Structure> mcstucts = new ArrayList<>();
				File[] saves = new File(actionRegistry.getMCreator().getFolderManager().getClientRunDir(),
						"saves/").listFiles();
				for (File save : saves != null ? saves : new File[0]) {
					if (save.isDirectory()) {
						// support < 1.13
						File[] structures = new File(save, "/structures/").listFiles();
						for (File structure : structures != null ? structures : new File[0]) {
							mcstucts.add(new Structure(
									save.getName() + ": " + FilenameUtilsPatched.removeExtension(structure.getName()),
									structure));
						}
						// support >1.13
						File[] generated = new File(save, "/generated/").listFiles();
						for (File generatedsect : generated != null ? generated : new File[0]) {
							structures = new File(generatedsect, "/structures/").listFiles();
							for (File structure : structures != null ? structures : new File[0]) {
								mcstucts.add(new Structure(save.getName() + ": " + FilenameUtilsPatched.removeExtension(
										structure.getName()), structure));
							}
						}
					}
				}
				Structure[] mcsturcturesarray = mcstucts.toArray(new Structure[0]);
				Structure selected = (Structure) JOptionPane.showInputDialog(actionRegistry.getMCreator(),
						L10N.t("dialog.workspace.resources.import_structure_from_minecraft.message"),
						L10N.t("dialog.workspace.resources.import_structure_from_minecraft.title"),
						JOptionPane.QUESTION_MESSAGE, null, mcsturcturesarray, "");
				if (selected != null) {
					File sch = selected.file();
					if (sch.isFile()) {
						FileIO.copyFile(sch,
								new File(actionRegistry.getMCreator().getFolderManager().getStructuresDir(),
										RegistryNameFixer.fix(sch.getName())));
					}
				}
				actionRegistry.getMCreator().reloadWorkspaceTabContents();
				if (actionRegistry.getMCreator().getTabs().getCurrentTab()
						.getContent() instanceof ModElementGUI<?> modElementGUI)
					modElementGUI.reloadDataLists();
			});
		}

		@Override public boolean isEnabled() {
			return actionRegistry.getMCreator().getGeneratorStats().getBaseCoverageInfo().get("structures")
					!= GeneratorStats.CoverageStatus.NONE;
		}
	}

	private record Structure(String name, File file) {

		@Override public String toString() {
			return name;
		}
	}

}
