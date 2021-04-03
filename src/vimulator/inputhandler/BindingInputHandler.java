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

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.*;
import javax.swing.KeyStroke;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.Macros;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.gui.HistoryModel;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.gui.KeyEventTranslator.Key;
import org.gjt.sp.util.Log;

public class BindingInputHandler extends InputHandler {

    public BindingInputHandler(View view, int mode) {
        super(view);

        this.bindings = new Hashtable();
        this.mode = mode;
        this.setCurrentBindings(bindings);
    }

    public BindingInputHandler(View view, int mode, Hashtable localBindings) {
        super(view);

        this.bindings = localBindings;
        this.mode = mode;
        this.setCurrentBindings(localBindings);
    }

    public BindingInputHandler(View view, BindingInputHandler chain) {
        super(view);

        this.bindings = chain.bindings;
        this.mode = chain.mode;
        this.setCurrentBindings(this.bindings);
    }

    public int getMode() {
        return mode;
    }

	//{{{ invokeAction() method
	/**
	 * Invokes the specified action, repeating and recording it as
	 * necessary.
     * This method is changed with respect to the super method in two aspects
     *  - every iteration checks the global repeatCount, hence its current state is observable
     *  - the user is never asked whether the current action should be repeated
	 * @param action The action
	 */
	@Override
	public void invokeAction(EditAction action)
	{
		JEditBuffer buffer = view.getBuffer();

		/* if(buffer.insideCompoundEdit())
			buffer.endCompoundEdit(); */

		// remember the last executed action
		if(!action.noRememberLast())
		{
			HistoryModel.getModel("action").addItem(action.getName());
			if(lastAction == action)
				lastActionCount++;
			else
			{
				lastAction = action;
				lastActionCount = 1;
			}
		}

		// remember old values, in case action changes them

        if(repeatCount == 1)
            // inside compound edit actions like "undo" don't work anymore
            action.invoke(view);
        else {
            try {
                buffer.beginCompoundEdit();

                for(int i = 0; i < repeatCount; i++)
                    action.invoke(view);
            }
            finally {
                buffer.endCompoundEdit();
            }
        }

		Macros.Recorder recorder = view.getMacroRecorder();

		if(recorder != null && !action.noRecord())
			recorder.record(repeatCount,action.getCode());

		// If repeat was true originally, clear it
		// Otherwise it might have been set by the action, etc
		if(repeatCount != 1)
		{
			// first of all, if this action set a
			// readNextChar, do not clear the repeat
			if(readNextChar != null)
				return;

			repeatCount = 1;
			view.getStatus().setMessage(null);
		}
	} //}}}


    // private members
    // current VI edit mode
    private int mode;
    // User requested a repeated command before
    private boolean requestedRepeat;


    private void resetState() {
        this.setCurrentBindings(bindings);
        this.requestedRepeat = false;
        this.setRepeatCount(1);
    }

    private void commandKeyPressed(KeyEvent evt) {
        int keyCode = evt.getKeyCode();
        int modifiers = evt.getModifiersEx();
        char c = evt.getKeyChar();
        boolean simple = (modifiers & ~KeyEvent.SHIFT_DOWN_MASK) == 0;

        if (!simple || evt.isActionKey() || keyCode == KeyEvent.VK_SPACE
                || keyCode == KeyEvent.VK_BACK_SPACE || keyCode == KeyEvent.VK_DELETE
                || keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER
                || keyCode == KeyEvent.VK_TAB) {
            readNextChar = null;

            KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);

            Object o = currentBindings.get(keyStroke);
            if (o instanceof Hashtable) {
                //Log.log(Log.WARNING, this, "Pressed, Result of keystroke: New hashtable");
                currentBindings = (Hashtable) o;
                evt.consume();
                return;
            } else if (o instanceof EditAction) {
                //Log.log(Log.WARNING, this,
                //        "Pressed, Result of keystroke: New Action " + ((EditAction) o).getName());
                invokeAction((EditAction) o);
                evt.consume();
                resetState();
                return;
            }

            Toolkit.getDefaultToolkit().beep();

            if (currentBindings != bindings) {
                // F10 should be passed on, but C+e F10
                // shouldn't
                evt.consume();
            }
            resetState();
        }
    }

    private void commandKeyTyped(KeyEvent evt) {
        int modifiers = evt.getModifiersEx();
        char c = evt.getKeyChar();

        if (readNextChar != null) {
            invokeReadNextChar(c);
            resetState();
            evt.consume();
            return;
        }

        c = Character.toUpperCase(c);

        // ignore
        if (c == '\b' || c == ' ')
            return;

        readNextChar = null;

        KeyStroke keyStroke;
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0 && c != Character.toLowerCase(c)) {
            // Shift+letter
            //Log.log(Log.WARNING, this, "Shift + " + c);
            keyStroke = KeyStroke.getKeyStroke(c, modifiers);
        } else {
            // Plain letter or Shift+punct
            keyStroke = KeyStroke.getKeyStroke(c);
        }

        Object o = currentBindings.get(keyStroke);

        if (currentBindings == bindings && Character.isDigit(c)
                && (this.requestedRepeat || c != '0')) {
            if (this.requestedRepeat) {
                this.repeatCount *= 10;
                this.repeatCount += c - '0';
            } else
                this.repeatCount = c - '0';
            this.requestedRepeat = true;
            evt.consume();
            return;
        } else if (o instanceof Hashtable) {
            //Log.log(Log.WARNING, this, "Typed, Result of keystroke: New hashtable");
            currentBindings = (Hashtable) o;
            evt.consume();
            return;
        } else if (o instanceof EditAction) {
            //Log.log(Log.WARNING, this,
            //        "Typed, Result of keystroke: New Action " + ((EditAction) o).getLabel());
            invokeAction((EditAction) o);
            evt.consume();
            if (readNextChar == null)
                resetState();
            return;
        }

        Toolkit.getDefaultToolkit().beep();
        resetState();
    }

    @Override
    public boolean handleKey(Key key, boolean global) {
        return true;
    }

    @Override
    public void processKeyEvent(java.awt.event.KeyEvent evt, int from, boolean global) {
        if (evt.getID() == java.awt.event.KeyEvent.KEY_PRESSED) {
            commandKeyPressed(evt);
        } else if (evt.getID() == java.awt.event.KeyEvent.KEY_TYPED) {
            commandKeyTyped(evt);
        }
    }
}

