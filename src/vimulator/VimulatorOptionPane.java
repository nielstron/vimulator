/*
 * VimulatorOptionPane.java Copyright &copy; 2001 mike dillon
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package vimulator;

import javax.swing.*;

import org.gjt.sp.jedit.*;

public class VimulatorOptionPane extends AbstractOptionPane {
    public VimulatorOptionPane() {
        super("vimulator");
    }

    // begin OptionPane implementation
    protected void _init() {
        // LineGuides enabled by default
        enableCheckBox = new JCheckBox(jEdit.getProperty("options.vimulator.enable"));
        enableCheckBox.setSelected(jEdit.getBooleanProperty("vimulator.enabled", false));
        addComponent(enableCheckBox);
    }

    protected void _save() {
        jEdit.setBooleanProperty("vimulator.enabled", enableCheckBox.isSelected());
    }
    // end OptionPane implementation

    private JCheckBox enableCheckBox;
}
