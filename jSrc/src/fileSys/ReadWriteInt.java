package fileSys;

import java.io.RandomAccessFile;

public class ReadWriteInt {
    public static void main(String[] args) {
        try {
            try(RandomAccessFile raf = new RandomAccessFile("PATH_TO_FILE_YOU_WANT_CREATED", "rw")) {
                long l = raf.length();
                while(raf.getFilePointer() < 1) {
                    System.out.println(raf.readByte());
                }
            }
        } catch (Exception e) {
            System.out.println("Error with reading/writing integer");
        }
    }
}