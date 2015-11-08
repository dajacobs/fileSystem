package fileSys;

public class JavaFileSystem {
    // Disk declaration
    private final Disk disk;
    // Super block declaration
    private final SuperBlock superBlock;
    // List cache
    private final IndirectBlock freeList;
    // File table declaration
    private final FileTable fileTable;
    
    // Instantiate java file system
    public JavaFileSystem() {
        disk = new Disk();
        superBlock = new SuperBlock();
        fileTable = new FileTable();
        freeList = null;
        try {
            disk.read(0, superBlock);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }
    // Format the disk image
    public int formatDisk(int size, int iSize) {
        InodeBlock ib = new InodeBlock();
        // Mark as unused
        for(Inode node : ib.node) {
            node.flags = 0;
        }
        // Write the empty indoe block
        for(int i = 1; i <= iSize; i++) {
            disk.write(i, ib);
        }
        int offset = 127;
        int lastFreeListBlock = 0;
        
        IndirectBlock b = new IndirectBlock();
        for(int i = size - 1; i >= iSize + 1; i--) {
            if((offset == 0) || (i == iSize + 1)) {
                // Write and update the block reference
                b.pointer[0] = lastFreeListBlock;
                disk.write(i, b);
                // Cache last free block
                lastFreeListBlock = i;
                // Prepare next block, if not last
                if(i > iSize + 1) {
                    offset = 127;
                    b.clear();
                } else {
                    b.pointer[offset--] = i;
                }
            }
        }
    }
}