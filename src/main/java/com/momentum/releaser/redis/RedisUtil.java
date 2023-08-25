package com.momentum.releaser.redis;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    /**
     * Redis에 저장된 key 값을 통해 value 값을 반환
     * @param key Redis의 key 값
     * @return 파라미터로 받은 key에 대한 value 값
     */
    public String getData(String key) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        return valueOperations.get(key);
    }

    /**
     * Redis에 key, value 값의 데이터를 저장
     * @param key Redis에 저장할 key 값
     * @param value Redis에 저장할 value 값
     */
    public void setData(String key, String value) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, value);
    }

    /**
     * 유효 시간 동안 (key, value) 쌍의 데이터를 저장
     * @param key Redis의 key 값
     * @param value Redis의 value 값
     * @param duration Redis에 데이터를 저장할 유효 시간
     */
    public void setDataExpire(String key, String value, Long duration) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        Duration expireDuration = Duration.ofSeconds(duration);
        valueOperations.set(key, value, expireDuration);
    }

    /**
     * 해당 key에 해당하는 value가 존재하는지 확인
     * @param key 찾고자 하는 key 값
     * @return Redis key 값 존재 여부
     */
    public boolean existsData(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Redis에 저장된 데이터를 삭제
     * @param key Redis의 key 값
     */
    public void deleteData(String key) {
        redisTemplate.delete(key);
    }
}
