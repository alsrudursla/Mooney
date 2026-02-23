package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import lombok.RequiredArgsConstructor;
import org.example.TradeWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ClientEndpoint
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "websocket.enabled", // 1. application.yml에서 'websocket.enabled' 속성을 찾는다.
    havingValue = "true", // 2. 그 값이 "true"일 때만 이 클래스를 활성화한다.
    matchIfMissing = false // 3. 만약 속성이 없으면 (or 기본값) 비활성화한다.
)
public class StockWebSocketClient {

    private final ApprovalKeyService approvalKeyService;
    private final TradeWebSocketHandler tradeWebSocketHandler;
    private final OfferService offerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kis.websocket-url}")
    private String websocketUrl;

    private Session userSession;

    /** trId:trKey 별 iv/key 저장 (예: "H0STCNT0:005930") */
    private final Map<String, KeyIv> cipherMap = new ConcurrentHashMap<>();

    private final java.util.Set<String> subscribed = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private record KeyIv(byte[] iv, byte[] key) {}

    // 애플리케이션 시작 시 WebSocket 연결
    @PostConstruct
    public void init() {
        connect();
    }

    private void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, URI.create(websocketUrl));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("✅ WebSocket Connected");
        this.userSession = session;

        // 최초 한 번 동기화 트리거 (아래의 scheduled와 동일한 로직 재사용)
        syncSubscriptions();
    }

    @Scheduled(fixedDelay = 10_000) // 10초마다; 필요에 맞게 조절
    public void syncSubscriptions() {
        try {
            // 세션이 열려 있어야 구독 가능
            if (userSession == null || !userSession.isOpen()) return;

            List<String> pendingStockCodes = offerService.getPendingStockCodes();

            // 검증용 고유동성 종목 하나 추가 (선택)
            if (!pendingStockCodes.contains("005930")) {
                pendingStockCodes.add("005930");
            }

            // DB목록 - 이미구독 = 새로 구독할 종목
            for (String code : pendingStockCodes) {
                if (code == null || code.isBlank()) continue;
                if (subscribed.contains(code)) continue; // 이미 구독함
                subscribeStock(code, "H0STCNT0");       // 구독 요청
                subscribed.add(code);                    // 중복방지 등록
            }

            // (옵션) DB에서 제거된 종목을 자동 해지하고 싶으면 아래 로직 추가:
            // Set<String> dbSet = new HashSet<>(pendingStockCodes);
            // for (String code : new HashSet<>(subscribed)) {
            //     if (!dbSet.contains(code) && !"005930".equals(code)) {
            //         unsubscribeStock(code, "H0STCNT0");
            //         subscribed.remove(code);
            //     }
            // }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @OnMessage
    public void onMessage(String message) {
        // 1) 메시지가 JSON인지 먼저 판별
        if (!looksLikeJson(message)) {
            // 복호화된 파이프 텍스트가 여기로 들어왔다면, 절대 readTree() 호출 금지
            handlePipeFrame(message); // "A|B|C|..." → split 처리
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode header = root.path("header");
            String trId = header.path("tr_id").asText("");

            // keep-alive
            if ("PINGPONG".equalsIgnoreCase(trId)) return;

            JsonNode body = root.path("body");

            // (A) 구독 성공: iv/key 저장
            if (body.has("output") && body.path("output").has("iv") && body.path("output").has("key")) {
                String ivHex  = body.path("output").path("iv").asText();
                String keyHex = body.path("output").path("key").asText();
                String trKey  = header.path("tr_key").asText(""); // 없는 경우도 있으니 대비
                cipherMap.put(trId + ":" + trKey, new KeyIv(hexToBytes(ivHex), hexToBytes(keyHex)));
                System.out.printf("🔐 SUBSCRIBE SUCCESS: %s/%s (iv,key 저장)%n", trId, trKey);
                return;
            }

            // (B) 실데이터: content(Base64) → 복호화 → 파이프 split
            if (body.has("content")) {
                String contentB64 = body.path("content").asText();
                String trKey = header.path("tr_key").asText("");
                KeyIv keyIv = cipherMap.get(trId + ":" + trKey);
                if (keyIv == null) {
                    System.out.printf("⚠️ iv/key 없음: %s/%s%n", trId, trKey);
                    return;
                }
                String decrypted = decryptBase64(contentB64, keyIv.iv(), keyIv.key());
                handleDecryptedFrame(trId, trKey, decrypted); // 여기서 split 처리
                return;
            }

            // (C) 그 외
            System.out.println("📨 수신(기타): " + message);

        } catch (Exception e) {
            // 여기서 발생하던 JsonParseException은 더이상 나오면 안 됨
            e.printStackTrace();
        }
    }

    /** message가 JSON처럼 보이는지 매우 보수적으로 판단 */
    private boolean looksLikeJson(String s) {
        if (s == null) return false;
        String t = s.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }

    /** 복호화된 파이프 프레임을 직접 받은 경우(실수로 message에 넣어 호출됐다면)도 안전 처리 */
    /** 파이프 전문에서 체결가만 추출해 matchOrders 호출 (KIS 포맷: enc|TR_ID|count|rec...) */
    private void handlePipeFrame(String frame) {
        if (frame == null || frame.isBlank()) return;

        // 예: 0|H0STCNT0|004|005930^123929^73100^...|005930^...|...
        String[] f = frame.split("\\|", -1);
        if (f.length < 4) {
            System.out.println("⚠️ 예상보다 짧은 파이프 프레임: " + frame);
            return;
        }

        // 채널 위치 동적 판별 (환경에 따라 f[0] 또는 f[1]에 올 수 있음)
        int chIdx;
        if ("H0STCNT0".equalsIgnoreCase(f[1].trim())) chIdx = 1;
        else if ("H0STCNT0".equalsIgnoreCase(f[0].trim())) chIdx = 0;
        else {
            // 체결 채널이 아니면 스킵 (필요하면 다른 채널 분기 추가)
            // System.out.println("ℹ️ not CNT channel: " + Arrays.toString(f));
            return;
        }

        final String trId = f[chIdx].trim();
        final int countIdx = chIdx + 1; // 데이터 건수
        final int recStart = chIdx + 2; // 첫 레코드 시작 인덱스

        int count = parseIntSafe(f[countIdx]);
        if (count <= 0) {
            // 건수가 0이거나 파싱 실패하면, f[recStart..] 전부를 레코드로 간주 (페일세이프)
            count = Math.max(0, f.length - recStart);
        }

        for (int i = 0; i < count; i++) {
            int idx = recStart + i;
            if (idx >= f.length) break;          // 방어
            String rec = f[idx];
            if (rec == null || rec.isBlank()) continue;

            // rec: 종목^시간^체결가^...
            String[] a = rec.split("\\^", -1);
            if (a.length < 3) continue;

            String stockCode = a[0].trim();
            int price        = parseIntSafe(a[2]); // ★ 체결가
            if (!stockCode.isEmpty() && price > 0) {
                System.out.printf("\uD83D\uDCB0 : %d%n", price);
                offerService.matchOrders(stockCode, price);
                tradeWebSocketHandler.sendTrade(stockCode, price);
                // 필요시 디버그:
                System.out.printf("📊 [CNT %s] @ %d | raw:%s%n", stockCode, price, rec);
            }
        }
    }

    /** 정상 경로: content 복호화 후 채널별 파싱 */
    private void handleDecryptedFrame(String trId, String trKey, String decrypted) {
        String[] f = decrypted.split("\\|");
        if ("H0STCNT0".equals(trId)) {
            // 체결: 문서에 맞춰 인덱스 조정
            int price = parseIntSafe(f[3]);  // 예시: [3] 체결가
            String time = f[2];              // 예시: [2] 시간
            String stockCode = (trKey == null || trKey.isBlank()) ? f[0] : trKey;
            offerService.matchOrders(stockCode, price);
            System.out.printf("📊 [CNT %s] 가격:%d 시간:%s | raw:%s%n", stockCode, price, time, decrypted);
        } else if ("H0STASP0".equals(trId)) {
            System.out.printf("📈 [ASP %s] %s%n", trKey, decrypted);
        } else {
            System.out.printf("ℹ️ [%s %s] %s%n", trId, trKey, decrypted);
        }
    }

    // 종목별 구독 요청
    private void subscribeStock(String stockCode, String trId) {
        try {
            String approvalKey = approvalKeyService.getApprovalKey();

            String subscribeMsg = """
            {
              "header": {
                "approval_key": "%s",
                "custtype": "P",
                "tr_type": "1",
                "content-type": "utf-8"
              },
              "body": {
                "input": {
                  "tr_id": "%s",
                  "tr_key": "%s"
                }
              }
            }
            """.formatted(approvalKey, trId, stockCode);

            if (userSession != null && userSession.isOpen()) {
                userSession.getBasicRemote().sendText(subscribeMsg);
                System.out.printf("📩 구독 요청: %s / %s%n", trId, stockCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("❌ WebSocket Closed: " + reason);
        // 재연결/백오프 로직 추가 가능
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
    }

    /* ===== Utils ===== */

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }

    private static String decryptBase64(String base64, byte[] iv, byte[] key) throws Exception {
        byte[] enc = Base64.getDecoder().decode(base64);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // (PKCS7 호환)
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] dec = cipher.doFinal(enc);
        return new String(dec, StandardCharsets.UTF_8);
    }
}