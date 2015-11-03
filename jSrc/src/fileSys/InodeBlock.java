package fileSys;

public class InodeBlock {
    // Inode block
    Inode node[] = new Inode[Disk.BLOCK_SIZE/Inode.SIZE];
    // Inode block constructor
    public InodeBlock() {
        for(int i = 0; i < Disk.BLOCK_SIZE/Inode.SIZE; i++) {
            node[i] = new Inode();
        }
    }
    // To string method to print nodes
    @Override
    public String toString() {
        String s = "INODEBLOCK:\n";
        for(int i = 0; i < node.length; i++) {
            s += node[i] + "\n";
        }
        return s;
    }
}