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
        int ind = allocInode(inode);
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
        // Too many files in disk
        return -1;
    }
    // Allocate block with file descriptor
    private int allocateBlock(int fd, int where) {
        if(superBlock.freeList <= 0) {
            return -1;
        }
        int block = allocateBlock();
        if(block < 0) {
            return -1;
        }
        Inode I = fileTable.getInode(fd);
        if(addBlock(I, block, where) < 0) {
            freeBlock(block);
            return -1;
        }
        return block;
    }
    // Allocate block
    private int allocateBlock() {
        if(superBlock.freeList <= 0) {
            return -1;
        }
        if(freeList == null) {
            freeList = new IndirectBlock();
            disk.read(superBlock.freeList, freeList);
        }
        int offset;
        for(offset = 1; (offset < Disk.BLOCK_SIZE/4 - 1) && (freeList.pointer[offset] <= 0); offset++) {
          
        }
        int freeBlock = freeList.pointer[offset];
            freeList.pointer[offset] = 0;
            if(freeBlock == 0) {
                freeBlock = superBlock.freeList;
                superBlock.freeList = freeList.pointer[0];
                offset = 0;
                freeList = null;
            }
        return freeBlock;
    }
    // Add inode block with block
    private int addBlock(Inode inode, int block, int where) {
        final int N = Disk.BLOCK_SIZE/4;
        int level, p, i0, i1 = 0, i2 = 0, i3 = 0;
        // Empty block
        if(where <= 9) {
            level = 0;
            i0 = p = where;
        // Single block    
        } else if(where <= (9 + N)) {
            level = 1;
            p = where - 10;
            i0 = 10;
            i1 = p;
        }
    }
    // Read inode
    private Inode readInode(int inum) {
        if(inum <= 0) {
            return null;
        }
        // Get inum block
        int block = (inum - 1)/8 + 1;
        if((block < 1) || (block > superBlock.iSize)) {
            return null;
        }
        InodeBlock ib = new InodeBlock();
        disk.read(block, ib);
        return ib.node[(inum - 1)%8];
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
    // Free-up block
    private int freeBlock(int block) {
        // Check input
        if(block <= 0) {
            return -1;
        }
        if(superBlock.freeList <= 0) {
            superBlock.freeList = block;
            freeList = new IndirectBlock();
            freeList.clear();
            return 0;
        }
        // Fill cache if empty
        if(freeList == null) {
            freeList = new IndirectBlock();
            disk.read(superBlock.freeList, freeList);
        }
        // Find last element, set offset if full
        int offset;
        for(offset = Disk.BLOCK_SIZE/4 - 1; (offset > 0) && (freeList.pointer[offset] > 0); offset--) {
            if(offset <= 0) {
                // Write in-memory free block to disk
                disk.write(superBlock.freeList, freeList);
                freeList = new IndirectBlock();
		freeList.clear();
		freeList.pointer[0] = superBlock.freeList;
            } else {
                freeList.pointer[offset] = block;
            }
        }
        return 0;
    }
    // Get block number
    private int getBlock(Inode inode, int block) {
        int size = (inode.fileSize + Disk.BLOCK_SIZE - 1)/Disk.BLOCK_SIZE;
        if(block < 0) {
            return -1;
        }
        final int N = 128;
        int level, p, i0, i1 = 0, i2 = 0, i3 = 0;
        if(block <= 9) {
            level = 0;
            i0 = p = block;
        // Single indirect block 
        } else if(block <= (9 + N)) {
            level = 1;
            p = block - 10;
            i0 = 10;
            i1 = p;
        // Double indirect block    
        } else if(block <= (9 + N + N * N)) {
            level = 2;
            p = block - (10 + N);
            i0 = 11;
            i1 = p / N;
            i2 = p % N;
        // Triple indirect block    
        } else if(block <= (9 + N + N * N + N * N * N)) {
            level = 3;
            p = block - (10 + N + N * N);
            i0 = 12;
            i1 = p / (N * N);
            i2 = (p/N) % N;
            i3 = p % N;
        } else {
            return -1;
        }
        if(level == 0) {
            return inode.pointer[i0];
        }
        IndirectBlock ib = new IndirectBlock();
        int disk_i1 = inode.pointer[i0];
        if(disk_i1 <= 0) {
            return -1;
        } else {
            disk.read(disk_i1, ib);
        }
        if(level == 1) { 
            return ib.pointer[i1]; 
        }
	int disk_i2 = ib.pointer[i1];
	if(disk_i2 <= 0) { 
            return -1; 
        } else { 
            disk.read(disk_i2, ib); 
        }
	if(level == 2) { 
            return ib.pointer[i2]; 
        }
	int disk_i3 = ib.pointer[i2];
	if(disk_i3 <= 0) { 
            return -1; 
        } else { 
            disk.read(disk_i3, ib); 
        }
	return ib.pointer[i3];
    }
}