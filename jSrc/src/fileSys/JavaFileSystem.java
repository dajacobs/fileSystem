package fileSys;

public class JavaFileSystem {
    // Disk declaration
    private final Disk disk;
    // Super block declaration
    private final SuperBlock superBlock;
    // List cache
    private final IndirectBlock freeList;
    // File table declaration
    private final FileTable fileTable;
    
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
}