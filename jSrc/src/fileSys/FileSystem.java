package fileSys;

public interface FileSystem {
    // Formats the disk
    public int formatDisk(int size, int iSize);
    // Close all files
    public int shutDown();
    // Creates empty file
    public int create();
    // The inumber of the open file
    public int iNumb(int fd);
    // Open file by inumber
    public int open(int iNumb);
    // Read from file to byte buffer
    public int read(int fd, byte[] buffer);
    // Write from file to byte buffer
    public int write(int fd, byte[] buffer);
    // Updates seek pointer with offset/where value
    public int seek(int fd, int offset, int where);
    // Writes file to disk and free-up file table
    public int close(int fd);
    // Delete the file according to inumber
    public int delete(int iNumb);
}