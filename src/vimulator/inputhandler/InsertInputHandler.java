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
import java.awt.event.KeyEvent;
import java.util.*;
import javax.swing.KeyStroke;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DefaultInputHandler;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.gui.KeyEventTranslator.Key;

public class InsertInputHandler extends InputHandler {
    public InsertInputHandler(View view) {
        super(view);

        this.defaultInputHandler = view == null ? null : view.getInputHandler();
        insertBindings = new Hashtable();
    }

    public InsertInputHandler(View view, InsertInputHandler chain) {
        super(view);

        this.defaultInputHandler = view == null ? null : view.getInputHandler();
        insertBindings = chain.insertBindings;
    }

    public InsertInputHandler(View view, Hashtable bindings) {
        super(view);

        this.defaultInputHandler = view == null ? null : view.getInputHandler();
        insertBindings = bindings;
    }

    private Hashtable insertBindings;
    private InputHandler defaultInputHandler;

    public int getMode() {
        return VimulatorConstants.INSERT;
    }

    public void setMode(int mode) {
        return;
    }

    @Override
    public void processKeyEvent(KeyEvent evt, int from, boolean global) {
        KeyStroke k = KeyStroke.getKeyStroke(evt.getKeyCode(), evt.getModifiersEx());
        Object o = this.insertBindings.get(k);
        if (evt.getID() == KeyEvent.KEY_PRESSED && o instanceof EditAction) {
            this.view.getBuffer().beginCompoundEdit();
            this.invokeAction((EditAction) o);
            this.view.getBuffer().endCompoundEdit();
            evt.consume();
            return;
        }
        this.defaultInputHandler.processKeyEvent(evt, from, global);
    }

    @Override
    public boolean handleKey(Key key, boolean global) {
        return this.defaultInputHandler.handleKey(key, global);
    }

}
