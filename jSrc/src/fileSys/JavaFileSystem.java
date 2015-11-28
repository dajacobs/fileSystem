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
    // Read
    public int read(int fd, byte buffer[]) {
        int sp = fileTable.getSptr(fd);
        // Empty buffers ignored
	if(buffer.length <= 0) {
            return 0;
        }
        Inode I = fileTable.getInode(fd);
        // Starting point to read
        int block  = sp / 512;
        int offset = sp % Disk.BLOCK_SIZE;
        int bp = 0;
        byte readBytes[] = new byte[Disk.BLOCK_SIZE];
        while(bp < buffer.length && sp < I.fileSize) {
            int disk_block = getBlock(I, block);
            // Error
            if(disk_block < 0) { 
                return bp; 
            }
            // Number of bytes to read
            int readSize = buffer.length - bp;
            if((offset + readSize) > Disk.BLOCK_SIZE) { 
                readSize = Disk.BLOCK_SIZE - offset; 
            }
            if((sp + readSize) > I.fileSize) { 
                readSize = I.fileSize - sp; 
            }
            if(disk_block == 0) {
                for(int i = offset; i < offset + readSize; i++) {
                    buffer[bp++] = 0;
                }
            } else {
                disk.read(disk_block, readBytes);
                for(int i = offset; i < offset + readSize; i++) {
                    buffer[bp++] = readBytes[i];
                }
            }
            // Increment position
            offset += readSize;
            if(offset >= Disk.BLOCK_SIZE) {
                offset = 0;
                block++;
            }
            sp += readSize;
            fileTable.setSptr(fd, sp);
        }
        return bp;
    }
    // Write to file
    public int write(int fd, byte buffer[]) {
        if(buffer.length <= 0) { 
            return 0; 
        }
        // Get the pointer value
        int sp = fileTable.getSptr(fd);
        // Get the inode
        Inode I = fileTable.getInode(fd);
        // Get start point to write
        int block  = sp / Disk.BLOCK_SIZE;
        int offset = sp % 512;
        int bp = 0;
        while(bp < buffer.length) {
            // Get block number and allocate
            int disk_block = getBlock(I, block);
            boolean justAllocated = false;
            if(disk_block == 0) {
                disk_block = allocateBlock(fd, block);
                justAllocated = true;
                if(disk_block < 0) { 
                    return bp; 
                }
            }
            // If errors with block number
            if(disk_block <= 0) { 
                return -1; 
            }
            // Number of bytes to write
            int writeSize = buffer.length - bp;
            if((offset + writeSize) > 512) { 
                writeSize = 512 - offset; 
            }
            // If writing beyond end of file
            boolean writingBeyondEOF = writeSize + sp > I.fileSize;
            // If needed to read before write to file
            boolean needToRead = true;
            // Write whole block
            if((offset == 0) && (writeSize >= 512)) {
                needToRead = false;
            // Write at the end of file with offset value
            } else if((offset == 0) && (writingBeyondEOF)) {
                needToRead = false;
            // Block just allocated    
            } else if(justAllocated) {
                needToRead = false;
            }
            // Prepare to write
            byte writeBytes[] = new byte[Disk.BLOCK_SIZE];
            if(justAllocated) {
                for(int i = 0; i < Disk.BLOCK_SIZE; i++) {
                    writeBytes[i] = 0;
                }
            }
            if(needToRead) {
                disk.read(disk_block, writeBytes);
            }
            for(int i = offset; i < (offset + writeSize); i++) {
                writeBytes[i] = buffer[bp++];
            }
            disk.writer(disk_block, writeBytes);
            offset += writeSize;
            if(offset >= Disk.BLOCK_SIZE) {
                offset = 0;
                block++;
            }
            sp += writeSize;
            fileTable.setSptr(fd, sp);
            // Update the size
            if(writingBeyondEOF) { 
                I.fileSize = block * Disk.BLOCK_SIZE + offset; 
            }
        }
        return bp;
    }
    // Seek
    public int seek(int fd, int offset, int whence) {
        int p;
        int oldSeek = fileTable.getSptr(fd);
        int fileSize = fileTable.getInode(fd).fileSize;
        switch(whence) {
            case 0: p = offset; break;
            case 1: p = oldSeek + offset; break;
            case 2: p = fileSize + offset; break;
            // Invalid
            default: return -1;
        }
        // Error
        if(p < 0) { 
            return -1; 
        }//
        if(fileTable.setSptr(fd, p) < 0) { 
            return -1; 
        }
        return p;
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
    // Delete
    public int delete(int iNumber) {
        // Error
        if(fileTable.getFDInumb(iNumber) >= 0) { 
            return -1; 
        }
        Inode I = readInode(iNumber);
        // Inode error
        if(I == null) { 
            return -1; 
        }
        // File not found
        if(I.flags == 0) { 
            return -1; 
        }
        int size = (I.fileSize + Disk.BLOCK_SIZE - 1) / Disk.BLOCK_SIZE;
        int count = 0;
        for(int i = 0; i < 10; i++) {
            if(i < size && I.pointer[i] > 0) {
                freeBlock(I.pointer[i]);
                count++;
            }
        }
        count += freeIndirect(I.pointer[10], 1);
        count += freeIndirect(I.pointer[11], 2);
        count += freeIndirect(I.pointer[12], 3);
        I.flags = 0;
        return writeInode(iNumber, I);
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
        int freeBlock = 0;
        for(offset = 1; (offset < Disk.BLOCK_SIZE/4 - 1) && (freeList.pointer[offset] <= 0); offset++) {
            freeBlock = freeList.pointer[offset];
            freeList.pointer[offset] = 0;
            if(freeBlock == 0) {
                freeBlock = superBlock.freeList;
                superBlock.freeList = freeList.pointer[0];
                offset = 0;
                freeList = null;
            }
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
        // Double block    
        } else if(where <= (9 + N + N * N)) {
            level = 2;
            p = where - (10 + N);
            i0 = 11;
            i1 = p / N;
            i2 = p % N;
        // Triple block    
        } else if(where <= (9 + N + N * N + N * N * N)) {
            level = 3;
            p = where - (10 + N + N * N);
            i0 = 12;
            i1 = p / (N * N);
            i2 = (p / N) % N;
            i3 = p % N;
        } else {
            return -1;
        }
        if(level == 0) {
            inode.pointer[i0] = block;
            return 0;
        }
        int allocated = 0;
        int[] allocatedBlocks = new int[3];
        IndirectBlock ib = new IndirectBlock();
        int disk_i1 = inode.pointer[i0];
        if(disk_i1 <= 0) {
            for(int i = 0; i < level; i++) {
                int b = allocateBlock();
                if(b <= 0) {
                    for(int j = 0; j < i; j++) { 
                        freeBlock(allocatedBlocks[j]); 
                    }
                    return -1;
                }
                allocatedBlocks[i] = b;
                allocated++;
            }
            disk_i1 = inode.pointer[i0] = allocatedBlocks[--allocated];
            ib.clear();
        } else {
            disk.read(disk_i1, ib);
        }
        if(level == 1) {
            ib.pointer[i1] = block;
            disk.write(disk_i1, ib);
            return 0;
        }
        boolean toBeAllocated = allocated > 0;
        int disk_i2 = (toBeAllocated) ? (allocatedBlocks[--allocated]) : (ib.pointer[i1]);
        // Empty
        if(toBeAllocated || (disk_i2 <= 0)) {
            if(disk_i2 <= 0) {
                if(allocated > 0) {
                    for(int j = 0; j < allocated; j++) { 
                        freeBlock(allocatedBlocks[j]); 
                    }
                    return -1;
                }
                for(int i = 0; i < level - 1; i++) {
                    int b = allocateBlock();
                    if(b <= 0) {
                        for(int j = 0; j < i; j++) { 
                            freeBlock(allocatedBlocks[j]); 
                        }
                        return -1;
                    }
                    allocatedBlocks[i] = b;
                    allocated++;
                }
                disk_i2 = ib.pointer[i1] = allocatedBlocks[--allocated];
                disk.write(disk_i1, ib);
                ib.clear();
            } else {
                ib.pointer[i1] = disk_i2;
                disk.write(disk_i1, ib);
                ib.clear();
            }
        } else { 
            disk.read(disk_i2, ib); 
        }
        // Second level
        if(level == 2) {
            ib.pointer[i2] = block;
            disk.write(disk_i2, ib);
            return 0;
        }
        toBeAllocated = allocated > 0;
        int disk_i3 = (toBeAllocated) ? (allocatedBlocks[--allocated]) : (ib.pointer[i2]);
        // Empty
        if(toBeAllocated || (disk_i3 <= 0)) {
            if(disk_i3 <= 0) {
                if(allocated > 0) {
                    for(int j = 0; j < allocated; j++) {
                        freeBlock(allocatedBlocks[j]);
                    }
                    return -1;
                }
                int b = allocateBlock();
                if(b <= 0) {
                    freeBlock(b);
                    return -1;
                }
                disk_i3 = ib.pointer[i2] = b;
                disk.write(disk_i2, ib);
                ib.clear();
            } else {
                ib.pointer[i2] = disk_i3;
                disk.write(disk_i2, ib);
                ib.clear();
            }
        } else { 
            disk.read(disk_i3, ib); 
        }
        ib.pointer[i3] = block;
        disk.write(disk_i3, ib);
        return 0;
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