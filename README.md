# Instagram Live Recorder

Instagram Live Recorder 是一個用於錄製 Instagram 直播的工具，並通過指定的參數進行配置。本專案使用 Java 語言開發，並且支援使用 ffmpeg 處理錄製過程。

## 目錄
- [需求](#需求)
- [安裝](#安裝)
- [使用方式](#使用方式)
- [參數](#參數)

## 需求
- Java 17+
- Maven 3.x
- ffmpeg

## 安裝
1. Clone 此專案至本地端：
   ```bash
   git clone https://github.com/yourusername/InstagramLiveRecorder.git
   cd InstagramLiveRecorder
2. 使用 Maven 進行打包：
   ```bash
    mvn clean package
3. 取得 InstagramLiveRecorder.jar：
- 打包完成後會產生於 target/ 資料夾。
- [InstagramLiveRecorder-ver1.jar](target%2FInstagramLiveRecorder-ver1.jar)

## 使用方式
1. 執行 InstagramLiveRecorder.jar 並指定參數來錄製 Instagram 直播：
    ```bash
    java -jar InstagramLiveRecorder.jar -i <outputFilePath> -u <username> -c <csrfToken> -s <sessionId> [其他參數]
2. 範例指令
    ```bash
    java -jar InstagramLiveRecorder.jar -i output/test.mp4 -u username -u triplescomsoms -s 68517975957%3ABK7eS4syKuzu4d%3A26%3DGu736e0ToYSS2PeD5WewIF1ACVAAYeLZCDLf18eebAg -c DDmOTEgdHFADU8GPdDUuOTW1CxD6JsED 

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

