package idv.mark.InstagramLiveRecorder.instagram;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {

    // 解析輸出路徑 (檔名)
    public static String getFileName(String outputFilePath) {
        String[] split = outputFilePath.split("/");
        return split[split.length - 1];
    }

    // 解析輸出路徑 (檔案資料夾)
    public static String getFilePath(String outputFilePath) {
        String[] split = outputFilePath.split("/");
        if (split.length > 1) {
            return outputFilePath.substring(0, outputFilePath.length() - split[split.length - 1].length() - 1);
        } else {
            return "";
        }
    }

    // 解析輸出路徑 (副檔名)
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    // 將byte[]寫入檔案
    public static void writeToFile(byte[] data, String filePath, String fileName) {
        try (FileOutputStream fos = new FileOutputStream(filePath + File.separator + fileName)) {
            new File(filePath).mkdirs();
            fos.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
