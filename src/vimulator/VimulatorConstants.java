package vimulator;

public class VimulatorConstants {
    
    public static final int COMMAND = 1 << 0;
    public static final int INSERT = 1 << 1;
    public static final int VISUAL = 1 << 2;
    public static final int BLOCK = 1 << 3;
    public static final int LINE = 1 << 4;
    public static final int VISUAL_BLOCK = VISUAL | BLOCK;
    public static final int VISUAL_LINE = VISUAL | LINE;

    public static final int EX = 1 << 5;
}
