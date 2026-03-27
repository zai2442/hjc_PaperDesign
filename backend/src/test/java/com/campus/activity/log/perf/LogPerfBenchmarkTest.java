package com.campus.activity.log.perf;

import com.campus.activity.log.entity.OperationLog;
import com.campus.activity.log.service.OperationLogService;
import com.campus.activity.testutil.TestTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LogPerfBenchmarkTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void benchmarkLogQueryPerf() throws Exception {
        // Prepare data: 5000 logs
        for (int i = 0; i < 5000; i++) {
            OperationLog log = new OperationLog();
            log.setOperatorId(1L);
            log.setOperatorUsername("admin");
            log.setOpType(i % 2 == 0 ? "OFFLINE" : "DELETE");
            log.setActivityId((long) i);
            log.setActivityTitle("Activity-" + i);
            log.setOpResult(1);
            log.setCreatedAt(LocalDateTime.now().minusMinutes(i));
            operationLogService.log(log);
        }

        String adminToken = TestTokenUtil.createToken("admin", "ROLE_SUPER_ADMIN");

        int concurrentThreads = 50; // Use 50 threads to simulate high load
        long durationMs = 5000;
        ExecutorService pool = Executors.newFixedThreadPool(concurrentThreads);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch start = new CountDownLatch(1);
        long endAt = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(durationMs);

        Callable<Void> task = () -> {
            start.await();
            while (System.nanoTime() < endAt) {
                long t0 = System.nanoTime();
                mockMvc.perform(get("/api/v1/admin/logs")
                                .header("Authorization", "Bearer " + adminToken)
                                .param("page", "1")
                                .param("size", "20")
                                .param("keyword", "Activity-100"))
                        .andExpect(status().isOk());
                long t1 = System.nanoTime();
                latencies.add(TimeUnit.NANOSECONDS.toMillis(t1 - t0));
            }
            return null;
        };

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentThreads; i++) {
            futures.add(pool.submit(task));
        }
        long startNs = System.nanoTime();
        start.countDown();
        for (Future<Void> f : futures) {
            f.get();
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        pool.shutdownNow();

        latencies.sort(Long::compareTo);
        long count = latencies.size();
        double qps = count * 1000.0 / Math.max(1, elapsedMs);
        long p99 = count == 0 ? 0 : latencies.get((int) Math.min(count - 1, Math.ceil(count * 0.99) - 1));

        System.out.println("Log Query Benchmark Results:");
        System.out.println("Total Requests: " + count);
        System.out.println("QPS: " + qps);
        System.out.println("P99 Latency: " + p99 + "ms");

        // Verify P99 < 1s (1000ms)
        assert p99 < 1000;
    }
}
