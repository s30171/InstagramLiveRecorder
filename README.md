# Instagram Live Recorder

Instagram Live Recorder 是一個用於錄製 Instagram 直播的工具，並通過指定的參數進行配置。本專案使用 Java 語言開發，並且支援使用 ffmpeg 處理錄製過程。

## 目錄
- [需求](#需求)
- [安裝](#安裝)
- [使用方式(cmd)](#使用方式cmd)
- [使用方式(maven)](#使用方式maven)
- [參數](#參數)

## 需求
- Java 17+
- Maven 3.x
- ffmpeg

## 安裝
1. Clone 此專案至本地端：
   ```bash
   git clone https://github.com/s30171/InstagramLiveRecorder.git
   cd InstagramLiveRecorder
2. 使用 Maven 進行打包：
   ```bash
    mvn clean package
3. 取得 InstagramLiveRecorder.jar：
- 打包完成後會產生於 target/ 資料夾。
- 預設jar 檔案名稱 InstagramLiveRecorder-ver1.jar

## 使用方式(cmd)
1. 執行 InstagramLiveRecorder.jar 並指定參數來錄製 Instagram 直播：
    ```bash
    java -jar InstagramLiveRecorder-ver1.jar -i <outputFilePath> -u <username> -c <csrfToken> -s <sessionId> [其他參數]
2. 範例指令
    ```bash
    java -jar InstagramLiveRecorder-ver1.jar -i output/test.mp4 -u triplescosoms -s 68517975957%3ABK7eS4syKuzu4d%3A26%3DGu736e0ToYSS2PeD5WewIF1ACVAAYeLZCDLf18eebAg -c DDmOTEgdHFADU8GPdDUuOTW1CxD6JsED 
3. 如果需要中斷 (會停止錄影，並等待當下所有的segment下載完成後，輸出檔案)
    ```bash
    Ctrl + C

## 使用方式(maven)
1. 把jitpack repo加入到pom.xml
    ```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

2. 加入dependency
    ```xml
    <dependency>
        <groupId>com.github.s30171</groupId>
        <artifactId>InstagramLiveRecorder</artifactId>
        <version>647506c1b7</version>
    </dependency>
3. 在程式裡面呼叫錄影工具
   ```java
   String username = "{需錄影的用戶名}";
   String csrfToken = "{Instagram登入後的csrfToken}";
   String sessionId = "{Instagram登入後的sessionId}";
   // 在這個物件設定參數
   ParameterSetting parameterSetting = new ParameterSetting();
   // 透過api取得串流資訊
   GetUserStreamInfo getUserStreamInfo = new GetUserStreamInfo(username, csrfToken, sessionId);
   String dashPlaybackUrlByWebInfoApi = getUserStreamInfo.getDashPlaybackUrlByWebInfoApi();
   // 放入dashPlayback url, 開始錄製串流
   MPDRecorder mpdRecorder = new MPDRecorder(dashPlaybackUrlByWebInfoApi, parameterSetting);
   mpdRecorder.process();
4. 如果需要中斷 (會停止錄影，並等待當下所有的segment下載完成後，輸出檔案)
   ```java
   mpdRecorder.stop();
   
## 參數
| 參數名 | 說明                                                                                                   | 必須 | 
| --- |------------------------------------------------------------------------------------------------------|-| 
| `-i` | 輸出檔案路徑，包含檔名與副檔名                                                                                      | 是 |
| `-u` | Instagram 用戶名                                                                                        | 是 | 
| `-c` | Instagram csrfToken                                                                                  | 是 | 
| `-s` | Instagram sessionId                                                                                  | 是 |
| `-ffmpegPath` | ffmpeg 可執行檔的路徑<br/>預設會吃環境變數的ffmpeg                                                                   | |
| `-ffmpegParam` | 自訂 ffmpeg 參數，如輸出格式等<br/>預設 "-c:v copy -c:a aac"                                                      | | 
| `-interval` | 設置挖掘過去直播請求間隔（毫秒），<br/>範圍在 1 到 5000 之間，預設值為 300<br/>間隔越短，挖掘越快，但越不安全<br/>※注意，使用200以下有極高風險會被Meta偵測不正常行為 | |
| `-history` | 是否需要記錄歷史資料                                                                                           | | 
| `--help` | 顯示指令的使用說明                                                                                            | |

