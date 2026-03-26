package com.campus.activity.activity.perf;

import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.mapper.ActivityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ActivityPerfBenchmarkTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    public void setupRedisMock() {
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), anyString());
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);
    }

    @Test
    public void benchmarkPublicListQpsAndP99() throws Exception {
        for (int i = 0; i < 200; i++) {
            Activity a = new Activity();
            a.setTitle("perf-" + i);
            a.setStatus("ONLINE");
            a.setPublishAt(LocalDateTime.now().minusMinutes(1));
            a.setCreatedBy(1L);
            a.setUpdatedBy(1L);
            a.setCreatedAt(LocalDateTime.now());
            a.setUpdatedAt(LocalDateTime.now());
            a.setVersion(0);
            a.setDeleted(0);
            activityMapper.insert(a);
        }

        int threads = 32;
        long durationMs = 3000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch start = new CountDownLatch(1);
        long endAt = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(durationMs);

        Callable<Void> task = () -> {
            start.await();
            while (System.nanoTime() < endAt) {
                long t0 = System.nanoTime();
                mockMvc.perform(get("/api/v1/activities").param("page", "1").param("size", "10"))
                        .andExpect(status().isOk());
                long t1 = System.nanoTime();
                latencies.add(TimeUnit.NANOSECONDS.toMillis(t1 - t0));
            }
            return null;
        };

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
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

        byte[] out = objectMapper.writeValueAsBytes(java.util.Map.of(
                "requests", count,
                "elapsedMs", elapsedMs,
                "qps", qps,
                "p99Ms", p99,
                "threads", threads
        ));
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/perf"));
        java.nio.file.Files.write(java.nio.file.Path.of("target/perf/activity_perf.json"), out);
    }
}
