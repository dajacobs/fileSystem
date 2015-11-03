package fileSys;

import java.io.File;
import java.io.RandomAccessFile;

public class Disk {
    public final static int BLOCK_SIZE = 512;
    public final static int NUM_BLOCKS = 1000;
    public final static int POINTERS_PER_BLOCK = (BLOCK_SIZE/4);
    private final int readCount = 0;
    private final int writeCount = 0;
    private File fileName;
    private RandomAccessFile disk;
}