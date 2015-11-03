package fileSys;

import java.io.EOFException;
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
    private int readCount = 0;
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
    // Reads blockNum into byte buffer
    public void read(int blockNum, byte[] buffer) {
        if(buffer.length != BLOCK_SIZE) {
            throw new RuntimeException("Read: bad buffer size " +buffer.length);
        }
        try {
            seek(blockNum);
            disk.read(buffer);
        } catch(IOException e) {
            System.err.println(e);
            System.exit(1);
        }
        readCount++;
    }
    // Reads super into byte buffer
    public void read(int blockNum, SuperBlock block) {
        try {
            seek(blockNum);
            block.size = disk.readInt();
            block.iSize = disk.readInt();
            block.freeList = disk.readInt();
        } catch(EOFException e) {
            if(blockNum != 0) {
                System.err.println(e);
                System.exit(1);
            }
            block.size = block.iSize = block.freeList = 0;
        } catch(IOException e) {
            System.err.println(e);
            System.exit(1);
        }
        readCount++;
    }
    // Reads inode block into byte buffer
    public void read(int blockNum, InodeBlock block) {
        try {
            seek(blockNum);
            for(int i = 0; i < block.node.length; i++) {
                block.node[i].flags = disk.readInt();
                block.node[i].owner = disk.readInt();
                block.node[i].fileSize = disk.readInt();
                for(int j = 0; j < 13; j++) {
                    block.node[i].pointer[j] = disk.readInt();
                }
            }
        } catch(IOException e) {
            System.err.println(e);
            System.exit(1);
        }
        readCount++;
    }
}