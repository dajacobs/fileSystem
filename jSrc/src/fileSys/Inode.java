package fileSys;

public class Inode {
    // Size in bytes
    public final static int SIZE = 64;
    // Flags of inode
    int flags;
    // Owner of inode
    int owner;
    // Size of the file
    int fileSize;
    // Pointer to the elemets
    int pointer[] = new int[13];
    
    // To string method to print variables
    @Override
    public String toString() {
        String s = "[Flags: " +flags +" Size: " +fileSize + " ";
        for(int i = 0; i < 13; i++) {
            s += "|" +pointer[i];
        }
        return s + "]";
    }
}