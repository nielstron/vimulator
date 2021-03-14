/*
 * VimulatorInputHandler.java - Modal InputHandler for Vimulator
 * Copyright (C) 2000, 2001 mike dillon
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package vimulator;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.*;
import javax.swing.KeyStroke;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DefaultInputHandler;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.gui.KeyEventTranslator.Key;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

public class CommandInputHandler extends InputHandler
{
	public CommandInputHandler(View view)
	{
		super(view);

		commandBindings = new Hashtable();
		visualBindings = new Hashtable();

		setMode(VimulatorPlugin.COMMAND);
	}

	public CommandInputHandler(View view, Hashtable commandBindings, Hashtable visualBindings)
	{
		super(view);

		this.commandBindings = commandBindings;
		this.visualBindings = visualBindings;

		setMode(VimulatorPlugin.COMMAND);
	}

	public CommandInputHandler(View view, CommandInputHandler chain)
	{
		super(view);

		commandBindings = chain.commandBindings;
		visualBindings = chain.visualBindings;

		setMode(chain.getMode());
	}

	public int getMode()
	{
		return mode;
	}

	public void setMode(int mode)
	{
		if (view == null) return;

		Buffer buffer = view.getBuffer();

		switch (mode)
		{
			case VimulatorPlugin.COMMAND:
			case VimulatorPlugin.VISUAL:
				if (buffer.insideCompoundEdit())
					buffer.endCompoundEdit();
				this.setBindings(this.commandBindings);
                break;
			default:
				return;
		}

		this.mode = mode;
		this.setCurrentBindings(this.bindings);
	}


	// private members
    // current VI edit mode
	private int mode;
    // User requested a repeated command before
    private boolean requestedRepeat;

    // Key bindings for different modes
	private Hashtable commandBindings;
	private Hashtable visualBindings;


	private void resetState()
	{
		this.setCurrentBindings(bindings);
        this.requestedRepeat = false;
		this.setRepeatCount(1);
	}

	private void commandKeyPressed(KeyEvent evt)
	{
		int keyCode = evt.getKeyCode();
		int modifiers = evt.getModifiersEx();
		char c = evt.getKeyChar();
		boolean simple = (modifiers & ~KeyEvent.SHIFT_DOWN_MASK) == 0;

		if (!simple
			|| evt.isActionKey()
			|| keyCode == KeyEvent.VK_SPACE
			|| keyCode == KeyEvent.VK_BACK_SPACE
			|| keyCode == KeyEvent.VK_DELETE
			|| keyCode == KeyEvent.VK_ESCAPE
			|| keyCode == KeyEvent.VK_ENTER
			|| keyCode == KeyEvent.VK_TAB)
		{
			readNextChar = null;

			KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);

			Object o = currentBindings.get(keyStroke);
			if (o instanceof Hashtable)
			{
                Log.log(Log.WARNING, this, "Pressed, Result of keystroke: New hashtable");
				currentBindings = (Hashtable)o;
				evt.consume();
				return;
			}
			else if (o instanceof EditAction)
			{
                Log.log(Log.WARNING, this, "Pressed, Result of keystroke: New Action " + ((EditAction)o).getName());
				invokeAction((EditAction)o);
				resetState();
				evt.consume();
				return;
			}

			Toolkit.getDefaultToolkit().beep();

			if (currentBindings != bindings)
			{
				// F10 should be passed on, but C+e F10
				// shouldn't
				evt.consume();
			}
			resetState();
		}
	}

	private void commandKeyTyped(KeyEvent evt)
	{
		int modifiers = evt.getModifiersEx();
		char c = evt.getKeyChar();

		if (readNextChar != null)
		{
			invokeReadNextChar(c);
			resetState();
			evt.consume();
			return;
		}

		c = Character.toUpperCase(c);

		// ignore
		if (c == '\b' || c == ' ') return;

		readNextChar = null;

		KeyStroke keyStroke;
		if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0
			&& c != Character.toLowerCase(c))
		{
			// Shift+letter
            Log.log(Log.WARNING, this, "Shift + " + c);
			keyStroke = KeyStroke.getKeyStroke(c, modifiers);
		}
		else
		{
			// Plain letter or Shift+punct
			keyStroke = KeyStroke.getKeyStroke(c);
		}

		Object o = currentBindings.get(keyStroke);

		if (currentBindings == bindings && Character.isDigit(c)
			&& (this.requestedRepeat || c != '0'))
		{
            if(this.requestedRepeat){
                this.repeatCount *= 10;
                this.repeatCount += c - '0';
            }
            else this.repeatCount = c - '0';
            this.requestedRepeat = true;
			evt.consume();
			return;
		}
		else if (o instanceof Hashtable)
		{
            Log.log(Log.WARNING, this, "Typed, Result of keystroke: New hashtable");
			currentBindings = (Hashtable)o;
			evt.consume();
			return;
		}
		else if (o instanceof EditAction)
		{
            Log.log(Log.WARNING, this, "Typed, Result of keystroke: New Action " + ((EditAction)o).getLabel());
			invokeAction((EditAction)o);
			if (readNextChar == null) resetState();
			evt.consume();
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
        //Log.log(Log.WARNING, this, "KeyCode: " + evt.getKeyCode());
        //Log.log(Log.WARNING, this, "KeyChar: " + evt.getKeyChar());
        //Log.log(Log.WARNING, this, "Modifier: " + evt.getModifiersEx());
        //Log.log(Log.WARNING, this, "ID: " + evt.getID());
        if(evt.getID() == java.awt.event.KeyEvent.KEY_PRESSED){
            switch (mode)
            {
                case VimulatorPlugin.COMMAND:
                default:
                    commandKeyPressed(evt);
                    break;
            }
        }
        else if (evt.getID() == java.awt.event.KeyEvent.KEY_TYPED){
            switch (mode)
            {
                case VimulatorPlugin.COMMAND:
                default:
                    commandKeyTyped(evt);
                    break;
            }
        }
    }
}
