package fileSys;

public class SuperBlock {
    // Number of block in file system
    int size;
    // Number of index blocks in file system
    int iSize;
    // First block of list
    int freeList;
    // To string method to bring variables
    public String toString() {
        return "SUPERBLOCK:\n" +"Size: " +size +" Isize: " +iSize +" freeList: " +freeList;
    }
}