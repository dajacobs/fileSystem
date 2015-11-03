package fileSys;

public final class IndirectBlock {
    // Pointer to elements
    public int pointer[] = new int[Disk.BLOCK_SIZE/4];
    // Indirect block constructor
    public IndirectBlock() {
        clear();
    }
    // Clear method to clear our indirect blocks
    public void clear() {
        for(int i = 0; i < Disk.BLOCK_SIZE/4; i++) {
            pointer[i] = 0;
        }
    }
    // To string method to print variables
    @Override
    public String toString() {
        String s = new String();
        s += "INDIRECTBLOCK:\n";
        for(int i = 0; i < pointer.length; i++) {
            s += pointer[i] + "|";
        }
        return s;
    }
}