package fileSys;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Test {
    // Java file system object
    private static JavaFileSystem fs;
    public static void main(String[] args) {
        // Declare file system
        fs = new JavaFileSystem();
        // Buffered reader initialize
        BufferedReader data = new BufferedReader(new InputStreamReader(System.in));
    }
}