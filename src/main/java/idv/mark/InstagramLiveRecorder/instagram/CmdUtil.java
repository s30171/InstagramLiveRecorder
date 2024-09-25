package idv.mark.InstagramLiveRecorder.instagram;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;

@Slf4j
public class CmdUtil {
    public static String exec(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).start();
            p.info().command().ifPresent(log::info);
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[err] " + line);
                    stderr.append(line).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()));
                 ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[out] " + line);
                    stdout.append(line).append("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            int exitVal = p.waitFor();
            String std;
            String cmdStr = String.join(" ", cmd);
            if (exitVal == 0) {
                std = stdout.toString();
                log.info("[cmd success]: {}", cmdStr);
                log.info("[cmd stdout]: {}", stdout);
            } else {
                std = stderr.toString();
                log.error("[cmd fail]: {}", cmdStr);
                log.info("[cmd stderr]: {}", stderr);
            }
            p.destroy();
            return std;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
