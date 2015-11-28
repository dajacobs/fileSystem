package fileSys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Test {
    // Java file system object
    private static JavaFileSystem fs;
    public static void main(String[] args) {
        // Declare file system
        fs = new JavaFileSystem();
        // Buffered reader initialize
        BufferedReader data = new BufferedReader(new InputStreamReader(System.in));
        // Read input
        for(;;) {
            try {
                System.out.print("--> ");
                System.out.flush();
                String line = data.readLine();
                line = line.trim();
                if(line.length() == 0) {
                    System.out.println();
                    continue;
                }
                StringTokenizer cmds = new StringTokenizer(line);
                String cmd = cmds.nextToken();
                int result = 0;
                if(cmd.equalsIgnoreCase("format")) { 
                    //int arg1 = Integer.parseInt(cmds.nextToken());
                    //int arg2 = Integer.parseInt(cmds.nextToken());
                    //result = fs.formatDisk(arg1, arg2);
                    result = fs.formatDisk(499, 998);
                } else if(cmd.equalsIgnoreCase("shutdown")) {
                    result = fs.shutdown();
                } else if(cmd.equalsIgnoreCase("create")) {
                    result = fs.create();
                    System.out.println(result);
                } else if(cmd.equalsIgnoreCase("close")) {
                    result = fs.close(Integer.parseInt(cmds.nextToken()));
                } else if(cmd.equalsIgnoreCase("write")) {
                    int arg1 = Integer.parseInt(cmds.nextToken());
                    String arg2 = cmds.nextToken();
                    result = writeTest(arg1, arg2);
                } else if(cmd.equalsIgnoreCase("quit")) {
                    System.exit(0);
                } else if(cmd.equalsIgnoreCase("help")) {
                    help();
                } else {
                    System.out.println("Command Unknown");
                }
                System.out.println("     result is " +result);
            } catch(NumberFormatException | NoSuchElementException | IOException e) {
                
            }
        }
    }
    // Test write to file descriptor
    private static int writeTest(int fd, String str) {
        byte buffer[] = new byte[str.length()];
        for(int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte)str.charAt(i % str.length());
        }
        return fs.write(fd, buffer);
    }
    // Help list
    private static void help() {
        System.out.println("\tformat size iSize");
        System.out.println("\tshutdown");
        System.out.println("\tcreate");
        System.out.println("\topen inum");
        System.out.println("\tinumber fd");
        System.out.println("\tread fd size");
        System.out.println("\twrite fd pattern size");
        System.out.println("\tseek fd offset whence");
        System.out.println("\tclose fd");
        System.out.println("\tdelete inum");
        System.out.println("\tquit");
        System.out.println("\tvars");
        System.out.println("\thelp");
    }
}