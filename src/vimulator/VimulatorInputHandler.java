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
import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.swing.KeyStroke;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DefaultInputHandler;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.gui.KeyEventTranslator.Key;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

public class VimulatorInputHandler extends InputHandler
{
	public VimulatorInputHandler(View view)
	{
		super(view);

		commandBindings = new Hashtable();
		insertBindings = new Hashtable();
		visualBindings = new Hashtable();

		setMode(VimulatorPlugin.COMMAND);
	}

	public VimulatorInputHandler(View view, VimulatorInputHandler chain)
	{
		super(view);

		commandBindings = chain.commandBindings;
		insertBindings = chain.insertBindings;
		visualBindings = chain.visualBindings;

		setMode(VimulatorPlugin.COMMAND);
	}

	public int getMode()
	{
		return mode;
	}

	public void setMode(int mode)
	{
		if (this.mode == mode || view == null) return;

		Buffer buffer = view.getBuffer();

		switch (mode)
		{
			case VimulatorPlugin.COMMAND:
				if (buffer.insideCompoundEdit())
					buffer.endCompoundEdit();
				this.bindings = commandBindings;
				break;
			case VimulatorPlugin.INSERT:
				buffer.beginCompoundEdit();
				this.bindings = insertBindings;
				break;
			case VimulatorPlugin.VISUAL:
				if (buffer.insideCompoundEdit())
					buffer.endCompoundEdit();
				this.bindings = insertBindings;
				break;
			default:
				return;
		}

		this.mode = mode;
		this.setCurrentBindings(this.bindings);
	}

	public void addKeyBinding(String binding, EditAction action)
	{
		addKeyBinding(binding, action, this.bindings);
	}

	public void addKeyBinding(String binding, EditAction action, int mode)
	{
		switch (mode)
		{
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

	public void removeKeyBinding(String binding)
	{
		throw new InternalError("Not yet implemented");
	}

	public void removeAllKeyBindings()
	{
		bindings.clear();
	}

	public void removeAllKeyBindings(int mode)
	{
		switch (mode)
		{
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
	private int mode;


	private Hashtable commandBindings;
	private Hashtable insertBindings;
	private Hashtable visualBindings;

	private void addKeyBinding(String binding, EditAction action, Hashtable current)
	{
        // current is a hashtable that recursively refers to further hashtables until
        // as long as there are more string tokens
		StringTokenizer st = new StringTokenizer(binding);
		while(st.hasMoreTokens())
		{
			KeyStroke keyStroke = VimulatorUtilities.parseKeyStroke(st.nextToken());
			if (keyStroke == null) return;

			if (st.hasMoreTokens())
			{
				Object o = current.getOrDefault(keyStroke, null);
				if (! (o instanceof Hashtable)) {
					o = new Hashtable();
					current.put(keyStroke, o);
				}
                current = (Hashtable)o;
			}
			else
			{
				current.put(keyStroke,action);
			}
		}
	}

	private void resetState()
	{
		this.setCurrentBindings(bindings);
		this.setRepeatCount(0);
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
                Log.log(Log.WARNING, this, "Pressed, Result of keystroke: New Action " + ((EditAction)o).getLabel());
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
			keyStroke = KeyStroke.getKeyStroke(c, modifiers);
		}
		else
		{
			// Plain letter or Shift+punct
			keyStroke = KeyStroke.getKeyStroke(c);
		}

		Object o = currentBindings.get(keyStroke);

		if (currentBindings == bindings && Character.isDigit(c)
			&& (repeatCount > 0 || c != '0'))
		{
			repeatCount *= 10;
			repeatCount += c - '0';
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
			//if (readNextChar == null)
             resetState();
			evt.consume();
			return;
		}

		Toolkit.getDefaultToolkit().beep();
		resetState();
	}

	private void insertKeyPressed(KeyEvent evt)
	{
		int keyCode = evt.getKeyCode();
		int modifiers = evt.getModifiersEx();
		boolean simple = (modifiers & ~KeyEvent.SHIFT_DOWN_MASK) == 0;

		if (modifiers == 0 && bindings == currentBindings
			&& (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_TAB))
		{
			userInput((char)keyCode);
			evt.consume();
			return;
		}

		if (!simple
			|| evt.isActionKey()
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
				currentBindings = (Hashtable)o;
				evt.consume();
				return;
			}
			else if (o instanceof EditAction)
			{
				invokeAction((EditAction)o);
				resetState();
				evt.consume();
				return;
			}

			// Don't beep if the user presses some
			// key we don't know about unless a
			// prefix is active. Otherwise it will
			// beep when caps lock is pressed, etc.
			if (currentBindings != bindings)
			{
				Toolkit.getDefaultToolkit().beep();
				// F10 should be passed on, but C+e F10
				// shouldn't
				evt.consume();
			}
			resetState();
		}
	}

	private void insertKeyTyped(java.awt.event.KeyEvent evt)
	{
		int modifiers = evt.getModifiersEx();
		char c = evt.getKeyChar();
		// ignore
		if (c == '\b')
			return;

		if (currentBindings != bindings)
		{
			readNextChar = null;

			KeyStroke keyStroke = KeyStroke.getKeyStroke(
				Character.toUpperCase(c));
			Object o = currentBindings.get(keyStroke);

			if (o instanceof Hashtable)
			{
				currentBindings = (Hashtable)o;
				evt.consume();
				return;
			}
			else if (o instanceof EditAction)
			{
				invokeAction((EditAction)o);
				resetState();
				evt.consume();
				return;
			}

			currentBindings = bindings;
		}

		if (repeatCount > 0 && Character.isDigit(c))
		{
			repeatCount *= 10;
			repeatCount += (c - '0');
		}
		else
		{
			userInput(c);
		}
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
        if(evt.getID() == java.awt.event.KeyEvent.KEY_PRESSED){
            switch (mode)
            {
                case VimulatorPlugin.INSERT:
                    insertKeyPressed(evt);
                    break;
                case VimulatorPlugin.COMMAND:
                default:
                    commandKeyPressed(evt);
                    break;
            }
        }
        else if (evt.getID() == java.awt.event.KeyEvent.KEY_TYPED){
            switch (mode)
            {
                case VimulatorPlugin.INSERT:
                    insertKeyTyped(evt);
                    break;
                case VimulatorPlugin.COMMAND:
                default:
                    commandKeyTyped(evt);
                    break;
            }
        }
    }
}
