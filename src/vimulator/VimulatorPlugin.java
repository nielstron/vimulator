/*
 * VimulatorPlugin.java - Vimulator plugin Copyright (C) 2000, 2001, 2002 mike dillon
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

import java.util.*;
import javax.swing.event.*;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.textarea.*;

import org.gjt.sp.util.Log;

public class VimulatorPlugin extends EBPlugin {
    public static final int COMMAND = 1 << 0;
    public static final int INSERT = 1 << 1;
    public static final int VISUAL = 1 << 2;
    public static final int EX = 1 << 3;
    public static final int APPEND = 1 << 4;

    @Override
    public void start() {
        Log.log(Log.DEBUG, this, "Started Plugin");
        initKeyBindings();
    }

    // public JMenuItem createMenuItems()
    // {
    // return new JMenuItem[]{GUIUtilities.loadMenuItem("toggle-vi-mode")};
    // }

    public void createOptionPanes(OptionsDialog optionsDialog) {
        optionsDialog.addOptionPane(new VimulatorOptionPane());
    }

    public void handleMessage(EBMessage msg) {
        if (msg instanceof ViewUpdate) {
            ViewUpdate vmsg = (ViewUpdate) msg;
            View view = vmsg.getView();

            if (vmsg.getWhat() == ViewUpdate.CREATED) {
                commandLines.put(view, new VimulatorCommandLine(view));
                handlers.put(view, new VimulatorInputHandler(view, rootHandler));

                if (jEdit.getBooleanProperty("vimulator.enabled", true)) {
                    toggleEmulation(view);
                }
            } else if (vmsg.getWhat() == ViewUpdate.CLOSED) {
                commandLines.remove(view);
                handlers.remove(view);
            }
        } else if (msg instanceof BufferUpdate) {
            BufferUpdate bmsg = (BufferUpdate) msg;
            Buffer buffer = bmsg.getBuffer();

            if (bmsg.getWhat() == BufferUpdate.LOADED) {
                int lineCount = buffer.getLineCount();
                int charCount = buffer.getLength();
                String[] args = {buffer.getName(), Integer.toString(lineCount),
                        Integer.toString(charCount)};

                updateBufferStatus(buffer, jEdit.getProperty("vimulator.msg.simple-status", args),
                        null);
            }
        } else if (msg instanceof EditPaneUpdate) {
            EditPaneUpdate emsg = (EditPaneUpdate) msg;
            EditPane editPane = emsg.getEditPane();

            if (emsg.getWhat() == EditPaneUpdate.BUFFER_CHANGED) {
                Buffer buffer = editPane.getBuffer();

                List<View> forViews = Arrays.asList(new View[] {editPane.getView()});

                if (buffer.isNewFile()) {
                    updateBufferStatus(buffer, null, forViews);
                    return;
                }

                int lineCount = buffer.getLineCount();
                int charCount = buffer.getLength();
                String[] args = {buffer.getName(), Integer.toString(lineCount),
                        Integer.toString(charCount)};

                updateBufferStatus(buffer, jEdit.getProperty("vimulator.msg.simple-status", args),
                        forViews);
            } else if (emsg.getWhat() == EditPaneUpdate.CREATED) {
                JEditTextArea textArea = editPane.getTextArea();
                textArea.addCaretListener(new VimulatorCaretListener(editPane.getView(), textArea));
            }
        }
    }

    public static String getVersionMessage() {
        return "Vimulator " + jEdit.getProperty("plugin.vimulator.VimulatorPlugin.version");
    }

    public static void toggleEmulation(View view) {
        InputHandler newHandler = (InputHandler) handlers.get(view);

        // Check if we are switching to VimulatorInputHandler
        // or back to the default
        if (newHandler instanceof VimulatorInputHandler) {
            // Switch to COMMAND mode
            ((VimulatorInputHandler) newHandler).setMode(COMMAND);

            // Install command line
            view.addToolBar(View.BOTTOM_GROUP, View.ABOVE_SYSTEM_BAR_LAYER, getCommandLine(view));
        } else {
            // Remove command line
            view.removeToolBar(getCommandLine(view));
        }

        // Store original handler
        handlers.put(view, view.getInputHandler());

        // Install new handler
        view.setInputHandler(newHandler);
    }

    public static boolean emulationActive(View view) {
        return !(handlers.get(view) instanceof VimulatorInputHandler);
    }

    public static void enterExCommand(View view) {
        getCommandLine(view).beginExCommand();
    }

    public static void leaveExCommand(View view) {
        getCommandLine(view).endExCommand();
    }

    public static void leaveExSearch(View view) {
        getCommandLine(view).endExCommand();
    }

    public static void enterSearch(View view) {
        enterSearch(view, false);
    }

    public static void enterSearch(View view, boolean reverse) {
        if (reverse)
            getCommandLine(view).beginReverseSearch();
        else
            getCommandLine(view).beginSearch();
    }

    public static void repeatSearch(View view) {
        repeatSearch(view, false);
    }

    public static void repeatSearch(View view, boolean reverse) {
        getCommandLine(view).repeatSearch(reverse);
    }

    public static VimulatorCommandLine getCommandLine(View view) {
        return (VimulatorCommandLine) commandLines.get(view);
    }

    public static CharacterSearch getLastFindChar() {
        return lastFindChar;
    }

    public static void setLastFindChar(char ch, boolean reverse, boolean until) {
        lastFindChar = new CharacterSearch(ch, reverse, until);
    }

    // private members
    private static VimulatorInputHandler rootHandler = new VimulatorInputHandler(null);

    private static Hashtable commandLines = new Hashtable();
    private static Hashtable handlers = new Hashtable();

    private static CharacterSearch lastFindChar;

    private static void updateBufferStatus(Buffer buffer, String status, List<View> views) {
        if (views == null)
            views = jEdit.getViewManager().getViews();

        for (int i = 0; i < views.size(); ++i) {
            if (!emulationActive(views.get(i)))
                continue;

            if (views.get(i).getBuffer() == buffer) {
                views.get(i).getStatus().setMessage(status);
            }
        }
    }

    private void initKeyBindings() {
        initKeyBindings("command", COMMAND);
        initKeyBindings("insert", INSERT);
    }

    private void initKeyBindings(String setPrefix, int mode) {
        String[] actionNames = jEdit.getActionNames();
        String prefix = "vimulator.keys." + setPrefix + ".";

        for (int i = 0; i < actionNames.length; i++) {
            String actionName = actionNames[i];
            EditAction action = jEdit.getAction(actionName);

            String propName = null;
            int propIndex = 0;
            String prop = null;

            while (true) {
                propName = prefix + actionName;
                if (++propIndex >= 2)
                    propName = propName + "." + propIndex;
                prop = jEdit.getProperty(propName);

                if (prop == null)
                    break;

                rootHandler.addKeyBinding(prop, action, mode);
            }
        }
    }

    class VimulatorCaretListener implements CaretListener {
        VimulatorCaretListener(View view, JEditTextArea textArea) {
            this.view = view;
            this.textArea = textArea;
        }

        public void caretUpdate(CaretEvent e) {
            if (!VimulatorUtilities.checkEmulation(view))
                return;

            int lastAllowed = VimulatorUtilities.getLastAllowedOffset(view, textArea);
            if (textArea.getCaretPosition() <= lastAllowed)
                return;

            textArea.setCaretPosition(lastAllowed);
        }

        private View view;
        private JEditTextArea textArea;
    }

    static class CharacterSearch {
        public final char ch;
        public final boolean reverse;
        public final boolean until;

        CharacterSearch(char ch, boolean reverse, boolean until) {
            this.ch = ch;
            this.reverse = reverse;
            this.until = until;
        }
    }
}
