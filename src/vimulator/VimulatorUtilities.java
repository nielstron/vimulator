/*
 * VimulatorUtilities.java - Utility class for Vimulator extension functions Copyright (C) 2000,
 * 2002 mike dillon
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

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import javax.swing.text.Segment;

import vimulator.inputhandler.*;
import vimulator.*;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.util.Log;

import java.lang.Character;
import java.util.ArrayList;
import java.util.Map;

public class VimulatorUtilities {


    public static boolean checkEmulation(View view) {
        return view.getInputHandler() instanceof VimulatorInputHandler;
    }

    public static int getMode(View view) {
        if (!checkEmulation(view))
            return -1;

        return ((VimulatorInputHandler) view.getInputHandler()).getMode();
    }

    public static void setMode(View view, int mode) {
        if (!checkEmulation(view))
            return;

        Log.log(Log.WARNING, null, "Setting mode to "+mode);
        ((VimulatorInputHandler) view.getInputHandler()).setMode(mode);

        JEditTextArea textArea = view.getTextArea();

        int lastAllowed = getLastAllowedOffset(view, textArea);
        if (textArea.getCaretPosition() > lastAllowed)
            textArea.moveCaretPosition(lastAllowed);
        
        textArea.setOverwriteEnabled((mode & VimulatorConstants.OVERWRITE) != 0);
        textArea.setRectangularSelectionEnabled((mode & VimulatorConstants.BLOCK) != 0);
        if(mode == VimulatorConstants.COMMAND){
            textArea.selectNone();
            textArea.setMultipleSelectionEnabled(false);
        }
        
        setStatus(
            view,
            jEdit.getProperty(
                "vimulator.msg." +
                VimulatorConstants.stringOfMode.get(mode).toLowerCase() +
                "-mode")
        );
    }

    public static void setInsertMode(View view) {
        setMode(view, VimulatorConstants.INSERT);
    }

    public static void setReplaceMode(View view) {
        setMode(view, VimulatorConstants.REPLACE);
    }

    public static void setCommandMode(View view) {
        setMode(view, VimulatorConstants.COMMAND);
    }

    public static void setVisualMode(View view) {
        setMode(view, VimulatorConstants.VISUAL);
    }

    public static void setVisualBlockMode(View view) {
        setMode(view, VimulatorConstants.VISUAL_BLOCK);
    }

    public static void setVisualLineMode(View view) {
        setMode(view, VimulatorConstants.VISUAL_LINE);
    }

    public static void beep(View view) {
        view.getToolkit().beep();
    }

    public static void setStatus(View view, String status) {
        VimulatorPlugin.getCommandLine(view).setStatus(status);
    }

    public static int getLastAllowedOffset(View view, JEditTextArea textArea) {
        return getLastAllowedOffset(view, textArea, textArea.getCaretLine());
    }

    public static int getLastAllowedOffset(View view, JEditTextArea textArea, int line) {
        if (line < 0 || line >= textArea.getLineCount()) {
            // view.getToolkit().beep();
            return -1;
        }

        int lastAllowed = textArea.getLineEndOffset(line) - 1;
        if (checkEmulation(view)
                && ((VimulatorInputHandler) view.getInputHandler()).getMode() == VimulatorConstants.COMMAND)
            lastAllowed--;

        lastAllowed = Math.max(lastAllowed, textArea.getLineStartOffset(line));

        return lastAllowed;
    }

    public static void goToWordEnd(JEditTextArea textArea) {
        textArea.moveCaretPosition(findWordEnd(textArea)-1);
    }

    public static void goToNextWordStart(JEditTextArea textArea) {
        textArea.moveCaretPosition(findNextWordStart(textArea));
    }

    public static void selectWordEnd(JEditTextArea textArea) {
        textArea.extendSelection(textArea.getCaretPosition(), findWordEnd(textArea)-1);
        goToWordEnd(textArea);
    }

    public static void selectNextWordStart(JEditTextArea textArea) {
        textArea.extendSelection(textArea.getCaretPosition(), findNextWordStart(textArea));
        goToNextWordStart(textArea);
    }


    /**
     * Joins the current and the next line, optionally adding a space between them.
     * 
     * @param addSpace True if a space should be added, false otherwise
     * @return position of join, or -1
     */
    public static int joinLines(JEditTextArea textArea, boolean addSpace) {
        JEditBuffer buffer = textArea.getBuffer();
        int lineNo = textArea.getCaretLine();
        int start = buffer.getLineStartOffset(lineNo);
        int end = buffer.getLineEndOffset(lineNo);
        if (end > buffer.getLength()) {
            Toolkit.getDefaultToolkit().beep();
            return -1;
        }
        buffer.remove(end - 1, leadingWhitespace(buffer, lineNo) + 1);
        if (addSpace)
            buffer.insert(end - 1, " ");

        return end - 1;
    }

    public static KeyStroke parseKeyStroke(String keyStroke) {
        if (keyStroke == null)
            return null;
        int modifiers = 0;
        int index = keyStroke.indexOf('+');
        if (index == 0) {
            index = -1;
        } else if (index != -1) {
            for (int i = 0; i < index; i++) {
                switch (Character.toUpperCase(keyStroke.charAt(i))) {
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
        if (key.length() == 1) {
            char ch = Character.toUpperCase(key.charAt(0));
            if (modifiers == 0)
                return KeyStroke.getKeyStroke(ch);
            else
                return KeyStroke.getKeyStroke(ch, modifiers);
        } else if (key.length() == 0) {
            Log.log(Log.ERROR, VimulatorUtilities.class, "Invalid key stroke: " + keyStroke);
            return null;
        } else {
            int ch;

            try {
                ch = KeyEvent.class.getField("VK_".concat(key)).getInt(null);
            } catch (Exception e) {
                Log.log(Log.ERROR, VimulatorUtilities.class, "Invalid key stroke: " + keyStroke);
                return null;
            }

            return KeyStroke.getKeyStroke(ch, modifiers);
        }
    }

    public static void replaceChar(JEditTextArea textArea, char replace) {
        int pos = textArea.getCaretPosition();
        textArea.selectNone();

        JEditBuffer buffer = textArea.getBuffer();
        buffer.remove(pos, 1);
        buffer.insert(pos, String.valueOf(replace));
        textArea.moveCaretPosition(pos);
    }

    public static void findChar(View view, char find, boolean reverse, boolean until) {
        findChar(view, find, reverse, until, true);
    }

    public static void findChar(View view, char find, boolean reverse, boolean until, boolean setLastFindChar) {
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
        if (reverse) {
            caret--;
            inc = -1;
            limit = caret;
        } else {
            caret++;
            inc = 1;
            limit = seg.count - caret;
        }

        caret += seg.offset;

        char ch;
        int pos = -1;
        for (int i = 0, j = caret; i < limit; i++, j += inc) {
            ch = seg.array[j];

            if (ch == find) {
                pos = j - seg.offset + lineStart;
                break;
            }
        }

        if (pos == -1) {
            beep(view);
            return;
        }

        if (until)
            pos -= inc;

        textArea.moveCaretPosition(pos);
    }

    public static void repeatFindChar(View view, boolean opposite) {
        VimulatorPlugin.CharacterSearch last = VimulatorPlugin.getLastFindChar();

        if (last == null) {
            beep(view);
            return;
        }

        findChar(view, last.ch, last.reverse ^ opposite, last.until, false);
    }

    public static int leadingWhitespace(JEditBuffer buffer, int line){
        int[] leadingWhitespace = { 0 };
        buffer.getCurrentIndentForLine(line, leadingWhitespace);
        return leadingWhitespace[0];
    }

    public static void insertEnterAndIndentBefore(JEditTextArea textArea, JEditBuffer buffer) {
        int line = textArea.getCaretLine();
        int caretPos = textArea.getLineStartOffset(line) + leadingWhitespace(buffer, line);
        textArea.moveCaretPosition(caretPos);
        textArea.insertEnterAndIndent();
        textArea.moveCaretPosition(caretPos);
    }

    public static void goToLine(View view, JEditTextArea textArea) {
        int rc = Math.min(
            view.getInputHandler().getRepeatCount(),
            textArea.getLineCount()
        );
        // Reset repeat count, this action should really only be invoked once
        view.getInputHandler().setRepeatCount(1);
        int line = rc - 1;
        int caretPos = textArea.getLineStartOffset(line);
        textArea.moveCaretPosition(caretPos);
    }


    /**
     * 
     * @param textArea
     * @param start True -> finds next word start False -> finds word end
     * @return
     */
    public static int findWord(JEditTextArea textArea, boolean start){
        int caretLine = textArea.getCaretLine();
        int caret = textArea.getCaretPosition();
        JEditBuffer buffer = textArea.getBuffer();

        // Copied from textArea.deleteWord from here (with modifications)
		int lineStart = textArea.getLineStartOffset(caretLine);
        // start off by one to skip the current end of word
		int _caret = caret - lineStart + 1;

		String lineText = textArea.getLineText(caretLine);

        // addition: skip leading whitespace
        while(_caret >= lineText.length() || (!start && Character.isWhitespace(lineText.charAt(_caret)))){
            _caret += 1;
            if(_caret >= lineText.length()-1)
            {
                if(lineStart + _caret == buffer.getLength())
                {
                    return buffer.getLength();
                }
                caretLine += 1;
                lineText = textArea.getLineText(caretLine);
                lineStart = textArea.getLineStartOffset(caretLine);
                _caret = 0;
            }
        }
        String noWordSep = buffer.getStringProperty("noWordSep");
        boolean camelCasedWords = buffer.getBooleanProperty("camelCasedWords");
        _caret = TextUtilities.findWordEnd(lineText,
            _caret+1,noWordSep,true,camelCasedWords,start);
        return _caret + lineStart;
    }

    public static int findWordEnd(JEditTextArea textArea){
        return findWord(textArea, false);
    }

    public static int findNextWordStart(JEditTextArea textArea){
        return findWord(textArea, true); // broken
    }


    public static void yank(String s){
        Registers.setRegister('y', s);
    }

    public static String unYank(){
        return Registers.getRegister('y').toString();
    }

    public static void yankLine(JEditTextArea textArea) {
        String s = textArea.getLineText(textArea.getCaretLine()) + "\n";
        Registers.setRegister('y', s);
    }

    public static void yankEndLine(JEditTextArea textArea) {
        int caretPos = textArea.getCaretPosition();
        String s = textArea.getText(caretPos, textArea.getLineEndOffset(textArea.getCaretLine()) - caretPos - 1);
        yank(s);
    }

    public static void yankWordEnd(JEditTextArea textArea) {
        int caretPos = textArea.getCaretPosition();
        String s = textArea.getText(caretPos, findWordEnd(textArea) - caretPos);
        yank(s);
    }

    public static void yankStartLine(JEditTextArea textArea) {
        int lineStartPos = textArea.getLineStartOffset(textArea.getCaretLine());
        String s = textArea.getText(lineStartPos, textArea.getCaretPosition() - lineStartPos);
        yank(s);
    }

    public static void deleteLine(JEditTextArea textArea) {
        yankLine(textArea);
        textArea.deleteLine();
    }

    public static void deleteEndLine(View view, JEditTextArea textArea) {
        yankEndLine(textArea);
        textArea.deleteToEndOfLine();
        //goToNextCol(view, textArea);
    }

    public static void deleteStartLine(JEditTextArea textArea) {
        yankStartLine(textArea);
        textArea.deleteToStartOfLine();
    }

    public static void deleteWordEnd(View view, JEditTextArea textArea) {
        yankWordEnd(textArea);
        int caretPos = textArea.getCaretPosition();
        textArea.getBuffer().remove(caretPos, findWordEnd(textArea) - caretPos);;
        //goToNextCol(view, textArea);
    }

    public static void paste(JEditTextArea textArea, JEditBuffer buffer, boolean after) {
        String s = unYank();
        int caretPos;
        if (s.endsWith("\n") || s.endsWith("\r")) {
            int caretLine = textArea.getCaretLine();
            if (after)
                caretPos = textArea.getLineEndOffset(caretLine);
            else
                caretPos = textArea.getLineStartOffset(caretLine);
        } else
            caretPos = textArea.getCaretPosition() + (after? 1:0);
        buffer.insert(caretPos, s);
    }

    public static void selectCaret(JEditTextArea textArea){
        int caret = textArea.getCaretPosition();
        Selection s = new Selection.Range(caret, caret+1);
        textArea.addToSelection(new Selection[]{s});
    }

    public static void goToNextCol(View view, JEditTextArea textArea, boolean select){
        int lastAllowed = getLastAllowedOffset(view, textArea);

        if (textArea.getCaretPosition() >= lastAllowed)
        {
            view.getToolkit().beep();
            return;
        }

        textArea.goToNextCharacter(select);
    }

    public static void goToPrevCol(View view, JEditTextArea textArea){
        goToPrevCol(view, textArea, false);
    }

    public static void goToPrevCol(View view, JEditTextArea textArea, boolean select){
        if (textArea.getCaretPosition() == textArea.getLineStartOffset(textArea.getCaretLine()))
        {
            view.getToolkit().beep();
            return;
        }

        textArea.goToPrevCharacter(false);
    }

    public static void goToNextCol(View view, JEditTextArea textArea){
        goToNextCol(view, textArea, false);
    }

    public static void selectNextCol(View view, JEditTextArea textArea){
        goToNextCol(view, textArea, true);
    }

    public static void yankSelection(JEditTextArea textArea){
        yank(textArea.getSelectedText());
    }

    public static void deleteSelection(JEditTextArea textArea){
        yankSelection(textArea);
        textArea.setSelectedText("");
    }

    public static void append(View view, JEditTextArea textArea){
        vimulator.VimulatorUtilities.setInsertMode(view);
        int caretLine = textArea.getCaretLine();
        int pos = textArea.getCaretPosition() + 1;
        if (pos >= textArea.getLineEndOffset(caretLine))
        {
            pos--;
        }
        textArea.moveCaretPosition(pos);
    }

    public static void selectLine(JEditTextArea textArea){
        textArea.moveCaretPosition(textArea.getLineStartOffset(textArea.getCaretLine()));
        textArea.goToNextLine(true);
    }

    public static int findWordStart(JEditTextArea textArea){
        // TODO fix beginning of line
        int caretLine = textArea.getCaretLine();
        JEditBuffer buffer = textArea.getBuffer();
        int caret = textArea.getCaretPosition();
        int lineStart = textArea.getLineStartOffset(caretLine);
		int _caret = caret - lineStart;
		if(_caret == 0 && lineStart == 0)
		{
            javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(null); 
            return caret;
		}
        _caret--;

		String lineText = textArea.getLineText(caretLine);
        // addition: skip leading whitespace
        while(_caret < 0 || Character.isWhitespace(lineText.charAt(_caret))){
            if(_caret < 0)
            {
                if(lineStart + _caret < 0)
                {
                    return 0;
                }
                caretLine -= 1;
                lineText = textArea.getLineText(caretLine);
                lineStart = textArea.getLineStartOffset(caretLine);
                _caret = textArea.getLineEndOffset(caretLine);
            }
            _caret -= 1;
        }

        String noWordSep = buffer.getStringProperty("noWordSep");
        boolean camelCasedWords = buffer.getBooleanProperty("camelCasedWords");
        _caret = TextUtilities.findWordStart(lineText,_caret,
            noWordSep,true,camelCasedWords,false,true);
		return _caret + lineStart;
    }

    public static void goToWordStart(JEditTextArea textArea){
        textArea.moveCaretPosition(findWordStart(textArea));
    }

    public static void selectWordStart(JEditTextArea textArea){
        textArea.extendSelection(textArea.getCaretPosition(), findWordStart(textArea));
        textArea.moveCaretPosition(findWordStart(textArea));
    }

    public static void yankWordStart(JEditTextArea textArea){
        int wordStartPos = findWordStart(textArea);
        String s = textArea.getText(wordStartPos, wordStartPos - textArea.getCaretPosition());
        yank(s);
    }

    public static void deleteWordStart(JEditTextArea textArea){
        yankWordStart(textArea);
        int wordStartPos = findWordStart(textArea);
        textArea.getBuffer().remove(wordStartPos, wordStartPos - textArea.getCaretPosition());
    }

    public static void multilineEdit(View view, JEditTextArea textArea, boolean replace){
        // Some black magic to enable multi-caret editing (insertion and replacement)
        // Idea courtesy to Skeeve https://sourceforge.net/p/jedit/feature-requests/499/
        JEditBuffer buffer = textArea.getBuffer();
        List<Selection> news = new ArrayList<Selection>(textArea.getSelectedLines().length);
        int selectionStart = buffer.getLength();
        Selection s = textArea.getSelection()[0];
        selectionStart = Math.min(selectionStart, s.getStart());

        for(int j : textArea.getSelectedLines()){
            int start = s.getStart(buffer, j);
            int end = s.getEnd(buffer, j);
            // TODO insert dummy value at selection beginnings
            // for correct multi-caret insertion
            news.add(new Selection.Range(start, replace ? end : start+1));
        }
        textArea.setMultipleSelectionEnabled(true);
        textArea.setSelection(news.toArray(new Selection.Range[0]));
        textArea.moveCaretPosition(selectionStart);
        setInsertMode(view);
    }

    // private members
    private VimulatorUtilities() {
    }
}
