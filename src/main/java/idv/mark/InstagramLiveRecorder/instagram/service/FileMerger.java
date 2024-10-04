package idv.mark.InstagramLiveRecorder.instagram.service;

import idv.mark.InstagramLiveRecorder.instagram.CmdUtil;
import idv.mark.InstagramLiveRecorder.instagram.FileUtil;
import idv.mark.InstagramLiveRecorder.instagram.model.ParameterSetting;
import idv.mark.InstagramLiveRecorder.instagram.model.SegmentStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 合併影片和音訊, 寫檔案的類別
 */
@Data
@Slf4j
@RequiredArgsConstructor
public class FileMerger {
    private final ParameterSetting parameterSetting;

    // 將影片和音訊寫入檔案
    public void writeFile(Collection<SegmentStatus> segmentStatusList, Map<Long, byte[]> m4vVideoDataMap, Map<Long, byte[]> m4aAudioDataMap) {
        String ffmpegOutputParameter = parameterSetting.getFfmpegOutputParameter();
        String[] ffmpegParameterArray = ffmpegOutputParameter.split(" ");
        List<String> cmdList = new ArrayList<>();
        String outputPath = parameterSetting.getOutputPath();
        String fileName = parameterSetting.getOutputFileName().replace(String.format(".%s", parameterSetting.getOutputFileExtension()), "");
        String fullFileName = String.format("%s/%s.mp4", outputPath, fileName);
        String anotherPath = askWantToOverwrite(fullFileName);
        boolean overwrite = "y".equals(anotherPath);
        if (!"y".equals(anotherPath)) {
            fullFileName = anotherPath;
        }
        outputPath = FileUtil.getFilePath(fullFileName);
        File file = new File(outputPath);
        if (!file.exists())  file.mkdirs();

        String ffmpegPath = StringUtils.isAllBlank(parameterSetting.getFfmpegPath()) ? "ffmpeg" : String.format("%s/ffmpeg", parameterSetting.getFfmpegPath());
        cmdList.add(ffmpegPath);
        cmdList.add("-i");
        cmdList.add(String.format("%s/temp.m4v", outputPath));
        cmdList.add("-i");
        cmdList.add(String.format("%s/temp.m4a", outputPath));
        cmdList.addAll(Arrays.asList(ffmpegParameterArray));
        if (overwrite) cmdList.add("-y");
        cmdList.add(fullFileName);

        List<SegmentStatus> segmentStatuses = new ArrayList<>(segmentStatusList);
        segmentStatuses.sort((o1, o2) -> (int) (o1.getSegmentId() - o2.getSegmentId()));
        try (ByteArrayOutputStream videoOutputStream = new ByteArrayOutputStream();
             ByteArrayOutputStream audioOutputStream = new ByteArrayOutputStream()) {
            for (SegmentStatus segmentStatus : segmentStatuses) {
                long segmentId = segmentStatus.getSegmentId();
                byte[] videoBytes = m4vVideoDataMap.get(segmentId);
                byte[] audioBytes = m4aAudioDataMap.get(segmentId);
                if ((videoBytes == null ||videoBytes.length == 0) || (audioBytes == null || audioBytes.length == 0)) {
                    log.error("segmentId={} download fail, skip segment", segmentId);
                    continue;
                }
                videoOutputStream.write(m4vVideoDataMap.get(segmentId));
                audioOutputStream.write(m4aAudioDataMap.get(segmentId));
            }
            FileUtil.writeToFile(videoOutputStream.toByteArray(), outputPath, "temp.m4v");
            FileUtil.writeToFile(audioOutputStream.toByteArray(), outputPath, "temp.m4a");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CmdUtil.exec(cmdList.toArray(new String[0]));

        // 刪除暫存檔
        File tempOutputM4v = new File(String.format("%s/temp.m4v", outputPath));
        File tempOutputM4a = new File(String.format("%s/temp.m4a", outputPath));
        if (tempOutputM4v.exists()) {
            tempOutputM4v.delete();
        }
        if (tempOutputM4a.exists()) {
            tempOutputM4a.delete();
        }
        log.info("output file: {}", fullFileName);
    }

    // 讓使用者選擇是否覆蓋或提供新檔案路徑
    public static String askWantToOverwrite(String fullFileName) {
        File file = new File(fullFileName);
        if (!file.exists()) {
            return fullFileName;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.printf("file %s exist。choose:\n- type 'y' to overrite file\n- or set path for file (ex: test/record.mp4)", fullFileName);
        String userInput = scanner.nextLine().trim();
        if ("y".equals(userInput)) {
            return "y";
        } else {
            return askWantToOverwrite(userInput);
        }
    }

}
