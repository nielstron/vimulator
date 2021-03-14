package vimulator;

import java.util.regex.*;

public class ViSearchMatcher {

    public static String extractRegexString(String command, char delim) {
        boolean backslash = false;
        char lastCh = 0, ch;
        int brackets = 0;

        for (int i = 0; i < command.length(); i++) {
            if (backslash) {
                backslash = false;
                continue;
            }

            ch = command.charAt(i);

            switch (ch) {
                case '\\':
                    backslash = true;
                    break;

                case '[':
                    brackets++;
                    break;

                case ']':
                    if (lastCh != '[' && brackets > 0)
                        brackets--;
                    break;

                default:
                    if (ch == delim && brackets == 0) {
                        if (i == 0)
                            return "";
                        return command.substring(0, i);
                    }
            }

            lastCh = ch;
        }

        return command;
    }

    public static Pattern extractRegex(String command, char delim) {
        return Pattern.compile(ViSearchMatcher.extractRegexString(command, delim),
                Pattern.MULTILINE);
        // removed the RE_SYNTAX_POSIX_EXTENDED
    }

}
