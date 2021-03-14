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

package vimulator;

import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.swing.KeyStroke;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.gui.KeyEventTranslator.Key;
import org.gjt.sp.util.Log;

public class VimulatorInputHandler extends InputHandler {
    public VimulatorInputHandler(View view) {
        super(view);

        commandBindings = new Hashtable();
        insertBindings = new Hashtable();
        visualBindings = new Hashtable();

        insertHandler = new InsertInputHandler(view, insertBindings);
        commandHandler = new CommandInputHandler(view, commandBindings, visualBindings);

        setMode(VimulatorPlugin.COMMAND);
    }

    public VimulatorInputHandler(View view, VimulatorInputHandler chain) {
        super(view);

        commandBindings = chain.commandBindings;
        visualBindings = chain.visualBindings;
        insertBindings = chain.insertBindings;
        insertHandler = new InsertInputHandler(view, insertBindings);
        commandHandler = new CommandInputHandler(view, commandBindings, visualBindings);

        setMode(chain.getMode());
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        switch (mode) {
            case VimulatorPlugin.COMMAND:
            case VimulatorPlugin.VISUAL:
                commandHandler.setMode(mode);
                this.currentHandler = commandHandler;
                break;
            case VimulatorPlugin.INSERT:
                this.currentHandler = insertHandler;
                break;
            default:
                return;
        }

        this.mode = mode;
        this.setCurrentBindings(this.bindings);
    }

    public void addKeyBinding(String binding, EditAction action) {
        addKeyBinding(binding, action, this.bindings);
    }

    public void addKeyBinding(String binding, EditAction action, int mode) {
        switch (mode) {
            case VimulatorPlugin.COMMAND:
                addKeyBinding(binding, action, this.commandBindings);
                break;
            case VimulatorPlugin.INSERT:
                addKeyBinding(binding, action, this.insertBindings);
                break;
            case VimulatorPlugin.VISUAL:
                addKeyBinding(binding, action, this.visualBindings);
                break;
        }
    }

    public void removeKeyBinding(String binding) {
        throw new InternalError("Not yet implemented");
    }

    public void removeAllKeyBindings() {
        bindings.clear();
    }

    public void removeAllKeyBindings(int mode) {
        switch (mode) {
            case VimulatorPlugin.COMMAND:
                commandBindings.clear();
                break;
            case VimulatorPlugin.INSERT:
                insertBindings.clear();
                break;
            case VimulatorPlugin.VISUAL:
                visualBindings.clear();
                break;
        }
    }

    // private members
    // current VI edit mode
    private int mode;

    // Key bindings for different modes
    private Hashtable commandBindings;
    private Hashtable insertBindings;
    private Hashtable visualBindings;

    private InputHandler currentHandler;
    private InsertInputHandler insertHandler;
    private CommandInputHandler commandHandler;

    private void addKeyBinding(String binding, EditAction action, Hashtable current) {
        // current is a hashtable that recursively refers to further hashtables until
        // as long as there are more string tokens
        StringTokenizer st = new StringTokenizer(binding);
        while (st.hasMoreTokens()) {
            KeyStroke keyStroke = VimulatorUtilities.parseKeyStroke(st.nextToken());
            if (keyStroke == null)
                return;

            if (st.hasMoreTokens()) {
                Object o = current.getOrDefault(keyStroke, null);
                if (!(o instanceof Hashtable)) {
                    o = new Hashtable();
                    current.put(keyStroke, o);
                }
                current = (Hashtable) o;
            } else {
                current.put(keyStroke, action);
            }
        }
    }

    @Override
    public int getRepeatCount() {
        return this.currentHandler.getRepeatCount();
    }

    @Override
    public void setRepeatCount(int i) {
        this.currentHandler.setRepeatCount(i);
    }


    @Override
    public boolean handleKey(Key key, boolean global) {
        return true;
    }

    @Override
    public void processKeyEvent(java.awt.event.KeyEvent evt, int from, boolean global) {
        Log.log(Log.WARNING, this, "KeyCode: " + evt.getKeyCode());
        Log.log(Log.WARNING, this, "KeyChar: " + evt.getKeyChar());
        Log.log(Log.WARNING, this, "Modifier: " + evt.getModifiersEx());
        Log.log(Log.WARNING, this, "ID: " + evt.getID());
        this.currentHandler.processKeyEvent(evt, from, global);
    }
}
