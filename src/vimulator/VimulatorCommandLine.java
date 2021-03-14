/*
 * VimulatorCommandLine.java - Vimulator command entry field
 * Copyright (C) 2002 mike dillon
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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.util.Log;

import vimulator.ex.*;

public class VimulatorCommandLine extends JPanel
	implements DocumentListener, ActionListener
{
	public VimulatorCommandLine(View view)
	{
		super(new BorderLayout());

		this.view = view;

		setBorder(BorderFactory.createLoweredBevelBorder());

		statusLabel = new JLabel(VimulatorPlugin.getVersionMessage());

		textField = new HistoryTextField("vimulator");
		//textField.getDocument().addDocumentListener(this);
		//textField.addActionListener(this);

		setComponent(statusLabel);
	}

	public void setStatus(String message)
	{
		statusLabel.setText(message);
		setComponent(statusLabel);
	}

	public void beginExCommand()
	{
		textField.setModel("vimulator");
		beginCommandInput(":");
	}

	public void endExCommand()
	{
        endCommandInput("");
	}

	public void beginSearch()
	{
		textField.setModel("vimulator-search");
		beginCommandInput("/");
	}

	public void endSearch()
	{
        endCommandInput("");
	}

	public void repeatSearch()
	{
		Registers.Register reg = Registers.getRegister('/');
		String pattern;
		if (reg == null || (pattern = reg.toString()) == null)
		{
			VimulatorUtilities.beep(view);
			return;
		}
		processSearch(pattern);
	}

	public void beginReverseSearch()
	{
		view.getToolkit().beep();
		return;

/*		textField.setModel("vimualtor-search");
		beginCommandInput("?");*/
	}

	public void beginShellCommand()
	{
		if (jEdit.getPlugin("console.ConsolePlugin") == null)
		{
			VimulatorUtilities.beep(view);
			return;
		}

		beginCommandInput("!");
	}

	public Dimension getMinimumSize()
	{
		return textField.getMinimumSize();
	}

	public Dimension getPreferredSize()
	{
		return textField.getPreferredSize();
	}

	public Dimension getMaximumSize()
	{
		return textField.getMaximumSize();
	}

	// ActionListener
	public void actionPerformed(ActionEvent e)
	{
		try
		{
			Document doc = textField.getDocument();
			processCommand(doc.getText(0, doc.getLength()));
		}
		catch (BadLocationException ble) {}
	}

	// DocumentListener
	public void changedUpdate(DocumentEvent e) {}

	public void removeUpdate(DocumentEvent e)
	{
		if (e.getDocument().getLength() == 0)
			endCommandInput(null);
	}

	public void insertUpdate(DocumentEvent e) {}

	public String getPrefix() { return prefix; }

	// private members
	private View view;

	private String prefix;

	private Component comp;
	private JLabel statusLabel;
	private HistoryTextField textField;

	private void setComponent(Component comp)
	{
		if (this.comp == comp) return;

		if (this.comp != null) remove(this.comp);
		this.comp = comp;

		add(this.comp, BorderLayout.CENTER);

		if (this.getParent() != null)
		{
			revalidate();
			repaint();
		}
	}

	private void beginCommandInput(String prefix)
	{
		this.prefix = prefix;

		textField.setText(prefix);
		textField.getDocument().addDocumentListener(this);
		textField.addActionListener(this);

		setComponent(textField);
		textField.requestFocus();
	}

	private void endCommandInput(String message)
	{
		//prefix = null;

		textField.getDocument().removeDocumentListener(this);
		textField.removeActionListener(this);

		setStatus(message);
		view.getTextArea().requestFocus();
	}

	private void processCommand(String command)
	{
		endCommandInput(null);

		command = command.substring(prefix.length());

		if (":".equals(prefix))
		{
			processExCommand(command);
		}
		else if ("/".equals(prefix) || "?".equals(prefix))
		{
			processSearch(command);
		}
		else if ("!".equals(prefix))
		{
			processShellCommand(command);
		}
		else
		{
			throw new InternalError("Bad command prefix: " + prefix);
		}
	}

	private void processExCommand(String expr)
	{
		try
		{
			ExInterpreter.eval(view, expr);
		}
		catch (BadExCommandException e)
		{
			Object[] args = new Object[] { expr };
			setStatus(jEdit.getProperty(
				"vimulator.msg.bad-ex-command", args));
		}
	}

	private void processSearch(String expr)
	{
		if ("".equals(expr))
		{
			repeatSearch();
			return;
		}

		char delim = prefix.charAt(0); // prefix is '/' or '?'
		String pattern = ViSearchMatcher.extractRegexString(expr, delim);

		Registers.setRegister('/', pattern);

		try
		{
			PatternSearchMatcher matcher = new PatternSearchMatcher(
                ViSearchMatcher.extractRegex(expr, delim), false, false
            );

			SearchAndReplace.setSearchMatcher(matcher);
			SearchAndReplace.setSearchFileSet(new CurrentBufferSet());
			SearchAndReplace.find(view);
		}
		catch (Exception e) {}
	}

	private void processShellCommand(String command)
	{
		setStatus(prefix + command);
	}

	private boolean getVIMode()
	{
		return (VimulatorUtilities.getMode(view) & VimulatorPlugin.EX) == 0;
	}

	private void setVIMode(boolean mode)
	{
		if (!(getVIMode() ^ mode)) return;
	}

	private void enterEXMode(String initial)
	{
		if (!getVIMode()) return;
	}
}
