package com.campus.activity.activity.perf;

import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.mapper.ActivityMapper;
import com.campus.activity.activity.mapper.RegistrationMapper;
import com.campus.activity.security.LoginUser;
import com.campus.activity.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RegistrationPerfBenchmarkTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    private Long activityId;

    @BeforeEach
    public void setup() {
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("10000"); // 模拟Redis中有库存
        doNothing().when(valueOperations).set(anyString(), anyString());
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);
        when(stringRedisTemplate.execute(any(), any(), any())).thenReturn(1L); // 模拟Lua扣减成功返回剩余库存1

        Activity a = new Activity();
        a.setTitle("Perf Test Activity");
        a.setStatus("ONLINE");
        a.setStockTotal(10000);
        a.setStockAvailable(10000);
        a.setPerUserLimit(1);
        a.setCreatedBy(1L);
        a.setUpdatedBy(1L);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        a.setVersion(0);
        a.setDeleted(0);
        activityMapper.insert(a);
        activityId = a.getId();
    }

    private void mockLogin(Long userId) {
        User u = new User();
        u.setId(userId);
        u.setUsername("user" + userId);
        LoginUser loginUser = new LoginUser(u, Collections.emptyList(), Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities())
        );
    }

    @Test
    public void benchmarkRegistrationQpsAndP99() throws Exception {
        int threads = 32;
        long durationMs = 3000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch start = new CountDownLatch(1);
        long endAt = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(durationMs);

        String reqBody = objectMapper.writeValueAsString(java.util.Map.of("activityId", activityId));

        Callable<Void> task = () -> {
            // 每个线程模拟一个独立用户
            long threadId = Thread.currentThread().getId();
            User u = new User();
            u.setId(threadId);
            u.setUsername("user" + threadId);
            LoginUser loginUser = new LoginUser(u, Collections.emptyList(), Collections.emptyList());

            start.await();
            while (System.nanoTime() < endAt) {
                long t0 = System.nanoTime();
                try {
                    // 每次请求模拟一个不同的用户以避免重复报名报错
                    long dynamicUserId = ThreadLocalRandom.current().nextLong(100000, 999999999);
                    User dynUser = new User();
                    dynUser.setId(dynamicUserId);
                    dynUser.setUsername("user" + dynamicUserId);
                    LoginUser dynamicLoginUser = new LoginUser(dynUser, Collections.emptyList(), Collections.emptyList());

                    org.springframework.security.core.context.SecurityContext ctx = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
                    ctx.setAuthentication(new UsernamePasswordAuthenticationToken(dynamicLoginUser, null, dynamicLoginUser.getAuthorities()));
                    mockMvc.perform(post("/api/v1/registrations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(reqBody)
                                    .with(securityContext(ctx)))
                            .andExpect(status().isOk());
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
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
        java.nio.file.Files.write(java.nio.file.Path.of("target/perf/registration_perf.json"), out);
    }
}
