package fileSys;

public class JavaFileSystem {
    // Disk declaration
    private Disk disk;
    // Super block declaration
    private SuperBlock superBlock;
    // List cache
    private IndirectBlock freeList;
    // File table declaration
    private FileTable fileTable;
    
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
        // Initialize super block
        superBlock.size = size;
        superBlock.iSize = iSize;
        superBlock.freeList = iSize + 1;
        // Write the block to the disk
        disk.write(0, superBlock);
        // Cache the first block
        freeList = b;
        return 0;
    }
    // Create
    public int create() {
        // Allocate a file descriptor
        int fd = fileTable.allocate();
        if(fd < 0) {
            return -1;
        }
        // Free-up file table descriptor
        fileTable.free(fd);
        // Set-up new inode;
        Inode inode = new Inode();
        inode.flags = 1;
        inode.owner = 0;
        inode.fileSize = 0;
        for(int i = 0; i < inode.pointer.length; i++) {
            inode.pointer[i] = 0;
        }
        int ind = fileTable.getInumb(fd);
        if(ind < 0) {
            return -1;
        }
        return open(ind, inode);
    }
    // Close 
    public int close(int fd) {
        // Get file table number
        int iNum = fileTable.getInumb(fd);
        // Get file table inode
        Inode inode = fileTable.getInode(fd);
        // Free-up file descriptor
        fileTable.free(fd);
        // Update inode
        return writeInode(iNum, inode);
    }
    // Shutdown
    public int shutdown() {
        for(int i = 0; i < FileTable.MAX_FILES; i++) {
            close(i);
        }
        if((freeList != null) && (superBlock.freeList > 0)) {
            disk.write(superBlock.freeList, freeList);
        }
        disk.write(superBlock.freeList, superBlock);
        return 0;
    }
    // Write inode 
    private int writeInode(int iNum, Inode inode) {
        if(iNum <= 0) {
            return -1;
        }
        int block = (iNum - 1)/8 + 1;
        if((block < 1) || (block > superBlock.iSize)) {
            return -1;
        }
        // Read and write the inode block
        InodeBlock ib = new InodeBlock();
        disk.read(block, ib);
        ib.node[(iNum - 1)%8] = inode;
        disk.write(block, ib);
        return 0;
    }
    // Find and allocate the first free inode
    private int allocInode(Inode inode) {
        InodeBlock ib = new InodeBlock();
        // Traverse inode of the disk
        for(int i = 1; i <= superBlock.iSize; i++) {
            disk.read(i, ib);
            // Traverse through inode block
            for(int j = 0; j < ib.node.length; j++) {
                if(ib.node[j].flags == 0) {
                    ib.node[j] = inode;
                    disk.write(i, ib);
                    return (i - 1)*8 + j + 1;
                }
            }
        }
 {        // Too many files in disk
        return -1;
    }
    // Open
    private int open(int iNum, Inode inode) {
        if(inode.flags == 0) {
            // Not found
            return -1;
        } else if(fileTable.getFDInumb(iNum) >= 0) {
            // Already open
            return -1;
        }
        // Allocate a file descriptor
        int fd = fileTable.allocate();
        if((fd < 0) || (fd >= FileTable.MAX_FILES)) {
            return -1;
        }
        int status = fileTable.add(inode, iNum, fd);
        if(status < 0) {
            // Error
            fileTable.free(fd);
            return -1;
        }
        return fd;
    }
}