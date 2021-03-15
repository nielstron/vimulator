/*
 * VimulatorInputHandler.java - Modal InputHandler for Vimulator Copyright (C) 2000, 2001 mike
 * dillon
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

package vimulator.inputhandler;

import vimulator.*;

import java.util.*;
import org.gjt.sp.jedit.View;

public class CommandInputHandler extends BindingInputHandler {

    public CommandInputHandler(View view) {
        super(view, VimulatorConstants.COMMAND);
    }

    public CommandInputHandler(View view, Hashtable commandBindings) {
        super(view, VimulatorConstants.COMMAND, commandBindings);
    }

    public CommandInputHandler(View view, CommandInputHandler chain) {
        super(view, chain);
    }
}
