package fileSys;

public class FileTable {
    // Maximum files
    public static final int MAX_FILES = 21;
    // Bitmap array
    public int bitmap[];
    // File descriptor array
    public FileDescriptor[] fdArray;
    // File table constructor
    public FileTable() {
        fdArray = new FileDescriptor[MAX_FILES];
        bitmap = new int[MAX_FILES];
        for(int i = 0; i < MAX_FILES; i++) {
            bitmap[i] = 0;
        }
    }
    // Gets the index of the file table
    public int allocate() {
        for(int i = 0; i < MAX_FILES; i++) {
            if(bitmap[i] == 0) {
                return i;
            }
        }
        System.out.println("Cannot open file... filetable is full.");
        return -1;
    }
    // Adder method for inodes
    public int add(Inode inode, int iNumb, int fd) {
        if(bitmap[fd] != 0) {
            return -1;
        }
        bitmap[fd] = 1;
        fdArray[fd] = new FileDescriptor(inode, iNumb);
        return 0;
    }
    // Free-up the bitmap
    public void free(int fd) {
        bitmap[fd] = 0;
    }
    // Validate file descriptor and max files
    public boolean isValid(int fd) {
        if(fd < 0 || fd >= MAX_FILES) {
            System.out.println("ERROR: Invalid file descriptor (must be 0 <= fd <= " +MAX_FILES +") : " +fd);
            return false;
        } else return bitmap[fd] > 0;
    }
    // Getter for inode
    public Inode getInode(int fd) {
        if(bitmap[fd] == 0) {
            return null;
        } else {
            return fdArray[fd].getInode();
        }
    }
    // Getter for inumber
    public int getInumb(int fd) {
        if(bitmap[fd] == 0) {
            return 0;
        } else {
            return fdArray[fd].getInumb();
        }
    }
    // Getter for seek pointer
    public int getSptr(int fd) {
        if(bitmap[fd] == 0) {
            return 0;
        } else {
            return fdArray[fd].getSptr();
        } 
    }
    // Getter for file descriptor with inumber
    public int getFDInumb(int iNumb) {
        for(int i = 0; i < MAX_FILES; i++) {
            if(bitmap[i] == 1) {
                if(fdArray[i].getInumb() == iNumb) {
                    return i;
                }
            }
        }
        return -1;
    }
    // Setter for seek pointer with new pointer
    public int setSptr(int fd, int newPointer) {
        if(bitmap[fd] == 0) {
            return 0;
        } else {
            fdArray[fd].setSptr(newPointer);
            return 1;
        }
    }
    // Setter for file size
    public int setFileSize(int fd, int newFileSize) {
        if(bitmap[fd] == 0) {
            return 0;
        } else {
            fdArray[fd].setFileSize(newFileSize);
            return 1;
        }
    }
}