package fileSys;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Disk {
    // Size of bytes of each disk block
    public final static int BLOCK_SIZE = 512;
    // Number of disk blocks in system
    public final static int NUM_BLOCKS = 1000;
    // Number of pointers in a disk block
    public final static int POINTERS_PER_BLOCK = (BLOCK_SIZE/4);
    // Number of reads to file system
    private final int readCount = 0;
    // Number of writes to file system
    private final int writeCount = 0;
    // File representing the disk
    private File fileName;
    // RAF representing the disk
    private RandomAccessFile disk;
    // Simulated disk
    public Disk() {
        try {
            fileName = new File("DISK");
            disk = new RandomAccessFile(fileName, "rw");
        } catch(IOException e) {
            System.err.println("Unable to start disk");
            System.exit(1);
        }
    }
    // Sets the pointer to the block number
    private void seek(int blockNum) throws IOException {
        if(blockNum < 0 || blockNum >= NUM_BLOCKS) {
            throw new RuntimeException("Attempt to read block " +blockNum+ " is out of range");
        }
        disk.seek((long)(blockNum * BLOCK_SIZE));
    }
}