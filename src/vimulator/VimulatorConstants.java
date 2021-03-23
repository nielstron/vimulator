package vimulator;

import java.util.Map;
public class VimulatorConstants {
    
    public static final int COMMAND = 1 << 0;
    public static final int INSERT = 1 << 1;
    public static final int VISUAL = 1 << 2;
    public static final int BLOCK = 1 << 3;
    public static final int LINE = 1 << 4;
    public static final int OVERWRITE = 1 << 5;
    public static final int VISUAL_BLOCK = VISUAL | BLOCK;
    public static final int VISUAL_LINE = VISUAL | LINE;
    public static final int REPLACE = INSERT | OVERWRITE;

    public static final int EX = 1 << 6;

    public static final int[] editorModes = {
        COMMAND,
        INSERT,
        VISUAL,
        VISUAL_BLOCK,
        VISUAL_LINE,
        REPLACE
    };

    public static Map<Integer, String> stringOfMode = Map.ofEntries(
        Map.entry(VimulatorConstants.COMMAND, "command"),
        Map.entry(VimulatorConstants.VISUAL, "visual"),
        Map.entry(VimulatorConstants.VISUAL_BLOCK, "visual_block"),
        Map.entry(VimulatorConstants.VISUAL_LINE, "visual_line"),
        Map.entry(VimulatorConstants.INSERT, "insert"),
        Map.entry(VimulatorConstants.REPLACE, "replace")
    );
}
