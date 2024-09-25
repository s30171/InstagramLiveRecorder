package idv.mark.InstagramLiveRecorder.instagram.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import idv.mark.InstagramLiveRecorder.instagram.model.MPD;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
public class InstagramHttpUtil {

    // 解析輸出路徑 (檔名)
    private static String getFileName(String outputFilePath) {
        String[] split = outputFilePath.split("/");
        return split[split.length - 1];
    }

    // 解析輸出路徑 (檔案資料夾)
    private static String getFilePath(String outputFilePath) {
        String[] split = outputFilePath.split("/");
        if (split.length > 1) {
            return outputFilePath.substring(0, outputFilePath.length() - split[split.length - 1].length() - 1);
        } else {
            return "";
        }
    }

    // 解析輸出路徑 (副檔名)
    private static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    private static final ObjectMapper xmlMapper = new XmlMapper();
    private final Map<String, String> WITH_SESSION_API_HEADERS = new HashMap<>(){{
        put("accept", "*/*");
        put("accept-language", "en-US,en;q=0.9,ru;q=0.8");
        put("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"103\", \"Google Chrome\";v=\"103\"");
        put("sec-ch-ua-mobile", "?0");
        put("sec-ch-ua-platform", "\"Windows\"");
        put("sec-fetch-dest", "empty");
        put("sec-fetch-mode", "cors");
        put("sec-fetch-site", "same-site");
        put("x-asbd-id", "198387");
        put("x-ig-app-id", "936619743392459"); // chrome browser
        put("x-ig-www-claim", "0");
        put("Referer", "https://www.instagram.com/");
        put("Referrer-Policy", "strict-origin-when-cross-origin");
    }};
    private final String[] NORMAL_HEADERS = {
            "accept", "*/*",
            "accept-language", "en-US,en;q=0.9,zh-TW;q=0.8,zh;q=0.7",
            "origin", "https://www.instagram.com",
            "referer", "https://www.instagram.com/",
            "sec-ch-ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"",
            "sec-ch-ua-mobile", "?0",
            "sec-ch-ua-platform", "\"Windows\"",
            "sec-fetch-dest", "empty",
            "sec-fetch-mode", "cors",
            "sec-fetch-site", "cross-site",
            "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
    };

    // 下載 Instagram 直播影片的片段 (主要會帶進來m4v, m4a的URL)
    public CompletableFuture<HttpResponse<byte[]>> downloadSegment(String segmentUrl) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(segmentUrl))
                .headers(NORMAL_HEADERS)
                .GET();
        HttpRequest request = requestBuilder.build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    // 帶上 session header的GET request
    public String getHttpRequestWithSessionHeader(String url) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();
        // 設置 Headers
        for (Map.Entry<String, String> entry : WITH_SESSION_API_HEADERS.entrySet()) {
            requestBuilder.setHeader(entry.getKey(), entry.getValue());
        }
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.headers().map().containsKey("x-ig-set-www-claim")) {
                WITH_SESSION_API_HEADERS.put("x-ig-www-claim", response.headers().map().get("x-ig-set-www-claim").get(0));
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response.body();
    }

    // 取得當下dash MPD XML檔案
    public MPD getMPDXMLByDashPlayBackUrl(String dashABRPlaybackUrl) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dashABRPlaybackUrl))
                .headers(NORMAL_HEADERS)
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (response.statusCode() != 200) {
            log.error("getMPDXMLByDashPlayBackUrl error: {}", response.body());
            return null;
        }
        try {
            return xmlMapper.readValue(response.body(), MPD.class);
        } catch (JsonProcessingException e) {
            log.error("getMPDXMLByDashPlayBackUrl error: {}", e.getMessage());
            return null;
        }
    }

    // 挖掘 Instagram segment 直播影片的方法 (用HEAD request)
    public HttpResponse<byte[]> digitHasSource(String digitURL) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(digitURL))
                .headers(NORMAL_HEADERS)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            log.error("digitHasSource error: {}", e.getMessage());
            return null;
        }
    }
}
