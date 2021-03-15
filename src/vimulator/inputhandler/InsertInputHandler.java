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
import org.gjt.sp.jedit.gui.KeyEventTranslator.Key;

public class InsertInputHandler extends DefaultInputHandler {
    public InsertInputHandler(View view) {
        super(view);

        insertBindings = new Hashtable();
    }

    public InsertInputHandler(View view, InsertInputHandler chain) {
        super(view);

        insertBindings = chain.insertBindings;
    }

    public InsertInputHandler(View view, Hashtable bindings) {
        super(view);

        insertBindings = bindings;
    }

    private Hashtable insertBindings;

    public int getMode() {
        return VimulatorConstants.INSERT;
    }

    public void setMode(int mode) {
        return;
    }


    private void insertKeyTyped(java.awt.event.KeyEvent evt) {
        int modifiers = evt.getModifiersEx();
        char c = evt.getKeyChar();

        userInput(c);
    }

    @Override
    public void processKeyEvent(KeyEvent evt, int from, boolean global) {
        KeyStroke k = KeyStroke.getKeyStroke(evt.getKeyCode(), evt.getModifiersEx());
        Object o = this.insertBindings.get(k);
        if (evt.getID() == KeyEvent.KEY_PRESSED && o instanceof EditAction) {
            this.invokeAction((EditAction) o);
            evt.consume();
            return;
        }
        // TODO Auto-generated method stub
        super.processKeyEvent(evt, from, global);
    }

    @Override
    public boolean handleKey(Key key, boolean global) {
        return super.handleKey(key, global);
    }

}
