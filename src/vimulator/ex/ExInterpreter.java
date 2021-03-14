package vimulator.ex;

import java.util.Hashtable;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;

public class ExInterpreter {
    public static void bind(String command, String action) {
        commands.put(command, action);
    }

    public static void eval(View view, String expr) throws BadExCommandException {
        // parse(expr);
        String command = expr;

        EditAction action;
        Object binding = commands.get(command);

        if (binding != null) {
            try {
                action = (EditAction) binding;
            } catch (ClassCastException e) {
                action = jEdit.getAction((String) binding);
            }

            if (action != null) {
                action.invoke(view);
                return;
            }
        }

        try {
            JEditTextArea textArea = view.getTextArea();
            int line = Integer.parseInt(command);

            if (line < 1)
                line = 1;
            else if (line > textArea.getLineCount())
                line = textArea.getLineCount();

            textArea.setCaretPosition(textArea.getLineStartOffset(line - 1));
            textArea.goToStartOfWhiteSpace(false);
            return;
        } catch (NumberFormatException e) {
        }

        action = jEdit.getAction(command);
        if (action != null) {
            action.invoke(view);
            return;
        }

        throw new BadExCommandException(view, command);
    }

    private static Hashtable commands;

    static {
        commands = new Hashtable();

        bind("w", "save");
        bind("wa", "save-all");
        bind("wq", "vi-save-close-buffer");
        bind("wq!", "vi-save-close-buffer-force");
        bind("wqa", "vi-save-close-all");
        bind("wqa!", "vi-save-close-all-force");

        bind("q", "close-buffer");
        bind("q!", "vi-close-buffer-force");
        bind("qa", "close-all");
        bind("qa!", "vi-close-all-force");

        bind("bn", "next-buffer");
        bind("bp", "prev-buffer");

        bind("e!", "reload");
        // Vimulator extension
        bind("ea!", "reload-all");

        bind("reg", "view-registers");
        bind("registers", "view-registers");

        // TODO introduce search+replace
    }

    private static void parse(String expr) {
        for (int i = 0; i < expr.length(); i++) {
            // TODO
        }
    }
}
