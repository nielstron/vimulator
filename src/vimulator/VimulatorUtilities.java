/*
 * VimulatorUtilities.java - Utility class for Vimulator extension functions
 * Copyright (C) 2000, 2002 mike dillon
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
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.util.Log;

public class VimulatorUtilities
{
	public static boolean checkEmulation(View view)
	{
		return view.getInputHandler() instanceof VimulatorInputHandler;
	}

	public static int getMode(View view)
	{
		if (!checkEmulation(view)) return -1;

		return ((VimulatorInputHandler)view.getInputHandler()).getMode();
	}

	public static void setMode(View view, int mode)
	{
		if (!checkEmulation(view)) return;

		((VimulatorInputHandler)view.getInputHandler()).setMode(mode);

		JEditTextArea textArea = view.getTextArea();

		int lastAllowed = getLastAllowedOffset(view, textArea);
		if (textArea.getCaretPosition() > lastAllowed)
			textArea.setCaretPosition(lastAllowed);

		if (mode == VimulatorPlugin.COMMAND)
		{
			setStatus(view, "");
		}
		else if (mode == VimulatorPlugin.INSERT)
		{
			String msg = textArea.isOverwriteEnabled()
				? jEdit.getProperty("vimulator.msg.replace-mode")
				: jEdit.getProperty("vimulator.msg.insert-mode");
			setStatus(view, msg);
		}
		else if (mode == VimulatorPlugin.VISUAL)
		{
			setStatus(view,
				jEdit.getProperty("vimulator.msg.visual-mode"));
		}
	}

	public static void beep(View view)
	{
		view.getToolkit().beep();
	}

	public static void setStatus(View view, String status)
	{
		VimulatorPlugin.getCommandLine(view).setStatus(status);
	}

	public static int getLastAllowedOffset(View view, JEditTextArea textArea)
	{
		return getLastAllowedOffset(view, textArea, textArea.getCaretLine());
	}

	public static int getLastAllowedOffset(View view, JEditTextArea textArea, int line)
	{
		if (line < 0 || line >= textArea.getLineCount())
		{
			//view.getToolkit().beep();
			return -1;
		}

		int lastAllowed = textArea.getLineEndOffset(line) - 1;
		if (checkEmulation(view) &&
			((VimulatorInputHandler)view.getInputHandler()).getMode() != VimulatorPlugin.INSERT)
			lastAllowed--;

		if (lastAllowed < textArea.getLineStartOffset(line))
			lastAllowed = textArea.getLineStartOffset(line);

		return lastAllowed;
	}

	public static void goToNextWordEnd(JEditTextArea textArea)
	{
		JEditBuffer buffer = textArea.getBuffer();

		int caret = textArea.getCaretPosition();
		int line = textArea.getCaretLine();
		int lineStart = textArea.getLineStartOffset(line);
		caret -= lineStart;

		String lineText = textArea.getLineText(line);

		if (caret == lineText.length())
		{
			if (lineStart + caret == buffer.getLength())
			{
				Toolkit.getDefaultToolkit().beep();
				return;
			}

			caret++;
		}
		else
		{
			String noWordSep = (String)buffer.getProperty("noWordSep");
			caret = TextUtilities.findWordEnd(lineText, caret + 1, noWordSep);
		}

		while (Character.isWhitespace(lineText.charAt(caret)))
		{
			if (caret < lineText.length() - 1)
			{
				++caret;
			}
			else if (line < textArea.getLineCount())
			{
				lineText = textArea.getLineText(++line);
				lineStart = textArea.getLineStartOffset(line);
				caret = 0;
			}
		}

		caret += lineStart;

		textArea.setCaretPosition(caret);
	}

	public static void goToNextWordStart(JEditTextArea textArea)
	{
        // TODO
	}

	/**
	 * Joins the current and the next line, optionally adding a space
	 * between them.
	 * @param addSpace True if a space should be added, false otherwise
	 * @return position of join, or -1
	 */
	public static int joinLines(JEditTextArea textArea, boolean addSpace)
	{
		JEditBuffer buffer = textArea.getBuffer();
		int lineNo = textArea.getCaretLine();
		int start = buffer.getLineStartOffset(lineNo);
		int end = buffer.getLineEndOffset(lineNo);
		if(end > buffer.getLength())
		{
			Toolkit.getDefaultToolkit().beep();
			return -1;
		}
		buffer.beginCompoundEdit();
        int[] leadingWhitespace = {0};
        buffer.getCurrentIndentForLine(lineNo, leadingWhitespace);
        Log.log(Log.WARNING, null, leadingWhitespace[0]);
		buffer.remove(end, leadingWhitespace[0]);
		if (addSpace) buffer.insert(end, " ");
		buffer.endCompoundEdit();

		return end - 1;
	}

	public static KeyStroke parseKeyStroke(String keyStroke)
	{
		if(keyStroke == null)
			return null;
		int modifiers = 0;
		int index = keyStroke.indexOf('+');
		if(index == 0)
		{
			index = -1;
		}
		else if(index != -1)
		{
			for(int i = 0; i < index; i++)
			{
				switch(Character.toUpperCase(keyStroke
					.charAt(i)))
				{
				case 'A':
					modifiers |= KeyEvent.ALT_DOWN_MASK;
					break;
				case 'C':
					modifiers |= KeyEvent.CTRL_DOWN_MASK;
					break;
				case 'M':
					modifiers |= KeyEvent.META_DOWN_MASK;
					break;
				case 'S':
					modifiers |= KeyEvent.SHIFT_DOWN_MASK;
					break;
				}
			}
		}
		String key = keyStroke.substring(index + 1);
		if(key.length() == 1)
		{
			char ch = Character.toUpperCase(key.charAt(0));
			if(modifiers == 0)
				return KeyStroke.getKeyStroke(ch);
			else
				return KeyStroke.getKeyStroke(ch,modifiers);
		}
		else if(key.length() == 0)
		{
			Log.log(Log.ERROR,VimulatorUtilities.class,
				"Invalid key stroke: " + keyStroke);
			return null;
		}
		else
		{
			int ch;

			try
			{
				ch = KeyEvent.class.getField("VK_".concat(key))
					.getInt(null);
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,VimulatorUtilities.class,
					"Invalid key stroke: "
					+ keyStroke);
				return null;
			}

			return KeyStroke.getKeyStroke(ch,modifiers);
		}
	}

	public static void replaceChar(JEditTextArea textArea, char replace)
	{
		int pos = textArea.getCaretPosition();
		textArea.selectNone();

		JEditBuffer buffer = textArea.getBuffer();
		buffer.beginCompoundEdit();
		buffer.remove(pos, 1);
		buffer.insert(pos, String.valueOf(replace));
		textArea.setCaretPosition(pos);
		buffer.endCompoundEdit();
	}

	public static void findChar(View view, char find,
		boolean reverse, boolean until)
	{
		findChar(view, find, reverse, until, true);
	}

	public static void findChar(View view, char find,
		boolean reverse, boolean until, boolean setLastFindChar)
	{
		if (setLastFindChar)
			VimulatorPlugin.setLastFindChar(find, reverse, until);

		JEditTextArea textArea = view.getTextArea();

		int line = textArea.getCaretLine();
		int lineStart = textArea.getLineStartOffset(line);
		int caret = textArea.getCaretPosition() - lineStart;

		Segment seg = new Segment();
		JEditBuffer buffer = textArea.getBuffer();
		buffer.getLineText(line, seg);

		int inc, limit;
		if (reverse)
		{
			caret--;
			inc = -1;
			limit = caret;
		}
		else
		{
			caret++;
			inc = 1;
			limit = seg.count - caret;
		}

		caret += seg.offset;

		char ch;
		int pos = -1;
		for (int i = 0, j = caret; i < limit; i++, j += inc)
		{
			ch = seg.array[j];

			if (ch == find)
			{
				pos = j - seg.offset + lineStart;
				break;
			}
		}

		if (pos == -1)
		{
			beep(view);
			return;
		}

		if (until) pos -= inc;

		textArea.setCaretPosition(pos);
	}

	public static void repeatFindChar(View view, boolean opposite)
	{
		VimulatorPlugin.CharacterSearch last =
			VimulatorPlugin.getLastFindChar();

		if (last == null)
		{
			beep(view);
			return;
		}

		findChar(view, last.ch, last.reverse ^ opposite, last.until, false);
	}

	public static void insertEnterAndIndentBefore(JEditTextArea textArea, JEditBuffer buffer){
        int line = textArea.getCaretLine();
        int[] leadingWhitespace = {0};
        buffer.getCurrentIndentForLine(line, leadingWhitespace);
        int caretPos = textArea.getLineStartOffset(line) + leadingWhitespace[0];
        textArea.setCaretPosition(caretPos);
        textArea.insertEnterAndIndent();
        textArea.setCaretPosition(caretPos);
    }

	// private members
	private VimulatorUtilities() {}
}
