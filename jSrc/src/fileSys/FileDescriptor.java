package fileSys;

public class FileDescriptor {
    // Inode for file
    private Inode inode;
    // Number of inodes
    private int iNumb;
    // Seek pointer for files
    private int sPtr;
    
    // File descriptor constructor
    public FileDescriptor(Inode inode, int iNumb) {
        this.inode = inode;
        this.iNumb = iNumb;
        sPtr = 0;
    }
    // Getter for inode
    public Inode getInode() {
        return inode;
    }
    // Getter for inumber
    public int getInumb() {
        return iNumb;
    }
    // Getter for seek pointer
    public int getSptr() {
        return sPtr;
    }
    // Setter for seek pointer
    public void setSptr(int i) {
        sPtr = i;
    }
    // Setter for file size
    public void setFileSize(int newSize) {
        inode.fileSize = newSize;
    }
}