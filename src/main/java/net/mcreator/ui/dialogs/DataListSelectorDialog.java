/*
 * MCreator (https://mcreator.net/)
 * Copyright (C) 2012-2020, Pylo
 * Copyright (C) 2020-2021, Pylo, opensource contributors
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

package net.mcreator.ui.dialogs;

import net.mcreator.minecraft.DataListEntry;
import net.mcreator.minecraft.MCItem;
import net.mcreator.ui.MCreator;
import net.mcreator.util.image.ImageUtils;
import net.mcreator.workspace.Workspace;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

public class DataListSelectorDialog extends ListSelectorDialog<DataListEntry> {
	private final boolean rawNames;

	public DataListSelectorDialog(MCreator mcreator, Function<Workspace, List<DataListEntry>> entryProvider,
			boolean rawNames) {
		super(mcreator, entryProvider);
		this.rawNames = rawNames;
		list.setCellRenderer(rawNames ? new DataListCellRenderer() : new DataListReadableCellRenderer());
	}

	@Override Predicate<DataListEntry> getFilter(String term) {
		return e -> (rawNames ? e.getName() : e.getReadableName()).toLowerCase(Locale.ENGLISH)
				.contains(term.toLowerCase(Locale.ENGLISH));
	}

	public static DataListEntry openSelectorDialog(MCreator mcreator,
			Function<Workspace, List<DataListEntry>> entryProvider, boolean rawNames, String title, String message) {
		var dataListSelector = new DataListSelectorDialog(mcreator, entryProvider, rawNames);
		dataListSelector.setMessage(message);
		dataListSelector.setTitle(title);
		dataListSelector.setVisible(true);
		return dataListSelector.list.getSelectedValue();
	}

	public static List<DataListEntry> openMultiSelectorDialog(MCreator mcreator,
			Function<Workspace, List<DataListEntry>> entryProvider, boolean rawNames, String title, String message) {
		var dataListSelector = new DataListSelectorDialog(mcreator, entryProvider, rawNames);
		dataListSelector.setMessage(message);
		dataListSelector.list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		dataListSelector.setTitle(title);
		dataListSelector.setVisible(true);
		return dataListSelector.list.getSelectedValuesList();
	}

	private class DataListCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			var label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			DataListEntry entry = (DataListEntry) value;
			label.setText(entry.getName().replace("CUSTOM:", ""));
			if (entry.getName().contains("CUSTOM:"))
				setIcon(new ImageIcon(ImageUtils.resize(
						MCItem.getBlockIconBasedOnName(mcreator.getWorkspace(), entry.getName()).getImage(), 18)));
			return label;
		}
	}

	private class DataListReadableCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			var label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			DataListEntry entry = (DataListEntry) value;
			label.setText(entry.getReadableName().replace("CUSTOM:", ""));
			if (entry.getName().contains("CUSTOM:"))
				setIcon(new ImageIcon(ImageUtils.resize(
						MCItem.getBlockIconBasedOnName(mcreator.getWorkspace(), entry.getName()).getImage(), 18)));
			return label;
		}
	}
}