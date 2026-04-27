import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LogAnalyzer {
    public static void main(String[] args) throws Exception {
        // 1. .env 파일 읽기
        Properties props = new Properties();
        props.load(new FileInputStream("scripts/.env"));
        String apiKey = props.getProperty("GEMINI_API_KEY");
        String url = props.getProperty("GEMINI_API_URL") + "?key=" + apiKey;

        // 2. logs 디렉토리에서 모든 app_*.log 파일만 수집
        Path logsDir = Path.of("logs");
        List<Path> logFiles;
        try (var stream = Files.list(logsDir)) {
            logFiles = stream
                .filter(p -> p.getFileName().toString().matches("app_\\d{8}_\\d{6}\\.log"))
                .sorted()
                .toList();
        }

        if (logFiles.size() < 2) {
            System.out.println("비교할 로그 파일이 2개 이상 필요합니다.");
            return;
        }

        // 3. 타임스탬프 기준 정렬 후 최신 2개 선택
        Path prevFile = logFiles.get(logFiles.size() - 2);
        Path currFile = logFiles.get(logFiles.size() - 1);

        System.out.println("이전: " + prevFile.getFileName());
        System.out.println("이번: " + currFile.getFileName());
         
        // 4. 각 파일에서 FINAL PROCESSING STATS 파싱
        String prevStats = extractStats(prevFile);
        String currStats = extractStats(currFile);

        if (prevStats.isEmpty() || currStats.isEmpty()) {
            System.out.println("FINAL PROCESSING STATS 를 찾을 수 없습니다.");
            return;
        }

        // 5. Gemini API로 비교 분석 요청
        String report = analyzeWithGemini(url, prevStats, currStats);
        
        // 6. 결과 출력
        System.out.println("\n===== 성능 분석 리포트 =====");
        System.out.println(report);

        // 7. reports 폴더에 파일 저장
        saveReport(report, prevFile.getFileName().toString(), currFile.getFileName().toString());
    }

    private static String extractStats(Path logFile) throws IOException {
        List<String> lines = Files.readAllLines(logFile);
        StringBuilder stats = new StringBuilder();
        boolean capturing = false;

        for (String line : lines) {
            if (line.contains("FINAL PROCESSING STATS")) {
                capturing = true;
            }
            if (capturing) {
                stats.append(line).append("\n");
            }
            if (capturing && line.contains("Avg DB Time Per Message")) break;
        }
        return stats.toString();
    }

    private static String analyzeWithGemini(String url, String prev, String curr) throws Exception {
        String prompt = """
            다음은 두 번의 성능 테스트 결과야. 한국어로 비교 분석해줘.
        
            1. 핵심 지표(Throughput, Avg Time Per Message, DB Time, Max/Min Operation Time)
                수치 변화와 의미 설명
            2. 성능이 저하된 지표가 있다면 원인 추정 및 개선 방향 제안
            3. 전체적인 성능 변화 요약 (한 줄)
        
            [이전 결과]
            %s
        
            [이번 결과]
            %s
            """.formatted(prev, curr);

        
        String body = """
                {
                  "contents": [{
                    "parts": [{"text": "%s"}]
                  }]
                }
                """.formatted(prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        int start = responseBody.indexOf("\"text\": \"") + 9;
        int end = responseBody.indexOf("\"\n          }\n        ]");

        if (start == 8 || end == -1) {
            return "응답 파싱 실패: " + responseBody;
        }
        
        return responseBody.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\u003e", ">")
                .replace("\\u003c", "<");
    }

    private static void saveReport(String report, String prevFileName, String currFileName) throws IOException {
        Path reportsDir = Path.of("scripts/reports");
        if (!Files.exists(reportsDir)) {
            Files.createDirectories(reportsDir);
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportFileName = "report_" + timestamp + ".txt";
        Path reportPath = reportsDir.resolve(reportFileName);

        String content = """
                ===== 성능 분석 리포트 =====
                생성 시각 : %s
                이전 파일 : %s
                이번 파일 : %s
                
                %s
                """.formatted(timestamp, prevFileName, currFileName, report);

        Files.writeString(reportPath, content);
        System.out.println("\\n리포트가 저장되었습니다: " + reportPath);
    }
}
