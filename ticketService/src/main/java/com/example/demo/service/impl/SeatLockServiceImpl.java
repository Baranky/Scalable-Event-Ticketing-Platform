package com.example.demo.service.impl;

import com.example.demo.exception.LockAcquisitionException;
import com.example.demo.service.DistributedLockService;
import com.example.demo.service.SeatLockService;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Service
public class SeatLockServiceImpl implements SeatLockService {

    private static final String SEAT_LOCK_PREFIX = "seat:lock:";
    private static final String GENERIC_LOCK_PREFIX = "seat:generic:";
    private static final String TOTAL_LOCK_PREFIX = "seat:total:";
    private static final String DISTRIBUTED_LOCK_PREFIX = "lock:seat:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final DistributedLockService distributedLockService;
    private final long lockTtlMinutes;

    private static final String LOCK_SEAT_LUA_SCRIPT = """
            local lockKey = KEYS[1]
            local totalKey = KEYS[2]
            local orderId = ARGV[1]
            local ttl = tonumber(ARGV[2])
            local currentOwner = redis.call('GET', lockKey)
            
            if currentOwner == false then
                -- Kilit yok, oluştur
                redis.call('SET', lockKey, orderId, 'EX', ttl)
                redis.call('INCR', totalKey)
                return 1  -- Başarılı
            elseif currentOwner == orderId then
                -- Kendi kilidimiz, TTL'i yenile
                redis.call('EXPIRE', lockKey, ttl)
                return 1  -- Başarılı (reentrant)
            else
                -- Başkası kilitli
                return 0  -- Başarısız
            end
            """;

    private static final String UNLOCK_SEAT_LUA_SCRIPT = """
            local lockKey = KEYS[1]
            local totalKey = KEYS[2]
            local orderId = ARGV[1]
            
            local currentOwner = redis.call('GET', lockKey)
            
            if currentOwner == orderId then
                redis.call('DEL', lockKey)
                local total = redis.call('DECR', totalKey)
                if total < 0 then
                    redis.call('SET', totalKey, 0)
                end
                return 1  -- Başarılı
            else
                return 0  -- Yetki yok veya kilit yok
            end
            """;

    private static final String LOCK_GENERIC_LUA_SCRIPT = """
            local genericKey = KEYS[1]
            local totalKey = KEYS[2]
            local orderId = ARGV[1]
            local count = tonumber(ARGV[2])
            local maxTotal = tonumber(ARGV[3])
            local ttl = tonumber(ARGV[4])
            
            -- Mevcut toplam kilidi al
            local currentTotal = tonumber(redis.call('GET', totalKey) or 0)
            
            -- Kapasite kontrolü
            if currentTotal + count > maxTotal then
                return -1  -- Yetersiz kapasite
            end
            
            -- Mevcut generic lock kontrolü
            local existingLock = redis.call('GET', genericKey)
            if existingLock and existingLock ~= orderId then
                return -2  -- Başka bir kilit var
            end
            
            -- Kilidi oluştur/güncelle
            redis.call('SET', genericKey, count, 'EX', ttl)
            
            -- Sadece yeni kilit ise total'ı artır
            if not existingLock then
                redis.call('INCRBY', totalKey, count)
            end
            
            return count  -- Başarılı, kilitlenen sayı
            """;

    public SeatLockServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            RedissonClient redissonClient,
            DistributedLockService distributedLockService,
            @Value("${seat.lock.ttl-minutes:5}") long lockTtlMinutes) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.distributedLockService = distributedLockService;
        this.lockTtlMinutes = lockTtlMinutes;
    }

    @Override
    public List<String> lockSeats(String stockId, List<String> seatLabels, String orderId) {
        List<String> lockedSeats = new ArrayList<>();
        long ttlSeconds = lockTtlMinutes * 60;

        for (String seatLabel : seatLabels) {
            String distributedLockKey = DISTRIBUTED_LOCK_PREFIX + stockId + ":" + seatLabel;

            try {
                boolean lockAcquired = distributedLockService.tryLock(
                        distributedLockKey,
                        5,
                        10,
                        TimeUnit.SECONDS
                );

                if (!lockAcquired) {
                    System.out.println("   ⏳ Distributed lock alınamadı: " + seatLabel + " (başka biri işliyor)");
                    continue;
                }

                try {
                    String seatLockKey = SEAT_LOCK_PREFIX + stockId + ":" + seatLabel;
                    String totalLockKey = TOTAL_LOCK_PREFIX + stockId;

                    Long result = redissonClient.getScript().eval(
                            RScript.Mode.READ_WRITE,
                            LOCK_SEAT_LUA_SCRIPT,
                            RScript.ReturnType.INTEGER,
                            Arrays.asList(seatLockKey, totalLockKey),
                            orderId, String.valueOf(ttlSeconds)
                    );

                    if (result != null && result == 1) {
                        lockedSeats.add(seatLabel);
                        System.out.println("    Koltuk kilitlendi: " + seatLabel);
                    } else {
                        Object currentOwner = redisTemplate.opsForValue().get(seatLockKey);
                        System.out.println("    Koltuk zaten kilitli: " + seatLabel + " → Owner: " + currentOwner);
                    }
                } finally {
                    distributedLockService.unlock(distributedLockKey);
                }

            } catch (Exception e) {
                System.err.println("   Hata (" + seatLabel + "): " + e.getMessage());
            }
        }

        return lockedSeats;
    }

    @Override
    public boolean lockGenericSeats(String stockId, int count, String orderId, int totalCount) {
        String distributedLockKey = DISTRIBUTED_LOCK_PREFIX + stockId + ":generic";
        long ttlSeconds = lockTtlMinutes * 60;


        try {
            return distributedLockService.executeWithLock(distributedLockKey, () -> {
                String genericKey = GENERIC_LOCK_PREFIX + stockId + ":" + orderId;
                String totalKey = TOTAL_LOCK_PREFIX + stockId;

                Long result = redissonClient.getScript().eval(
                        RScript.Mode.READ_WRITE,
                        LOCK_GENERIC_LUA_SCRIPT,
                        RScript.ReturnType.INTEGER,
                        Arrays.asList(genericKey, totalKey),
                        orderId, String.valueOf(count), String.valueOf(totalCount), String.valueOf(ttlSeconds)
                );

                if (result != null && result > 0) {
                    System.out.println("   " + count + " generic koltuk kilitlendi (TTL: " + lockTtlMinutes + " dk)");
                    return true;
                } else if (result != null && result == -1) {
                    System.out.println("   Yetersiz kapasite! Mevcut kilitli: " + getLockedCount(stockId) + ", Total: " + totalCount);
                    return false;
                } else {
                    System.out.println("   Kilitleme başarısız: " + result);
                    return false;
                }
            });

        } catch (LockAcquisitionException e) {
            System.err.println("  Distributed lock alınamadı: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("  Hata: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void confirmGenericSale(String stockId, int count, String orderId) {
        String distributedLockKey = DISTRIBUTED_LOCK_PREFIX + stockId + ":generic";

        try {
            distributedLockService.executeWithLock(distributedLockKey, () -> {
                String genericKey = GENERIC_LOCK_PREFIX + stockId + ":" + orderId;
                redisTemplate.delete(genericKey);
                decrementTotalLock(stockId, count);
                System.out.println("✅ Satış onaylandı: " + count + " koltuk, Order: " + orderId);
                return null;
            });
        } catch (Exception e) {
            String genericKey = GENERIC_LOCK_PREFIX + stockId + ":" + orderId;
            redisTemplate.delete(genericKey);
            decrementTotalLock(stockId, count);
            System.out.println("✅ Satış onaylandı (fallback): " + count + " koltuk");
        }
    }

    @Override
    public void unlockGenericSeats(String stockId, int count, String orderId) {
        String distributedLockKey = DISTRIBUTED_LOCK_PREFIX + stockId + ":generic";

        try {
            distributedLockService.executeWithLock(distributedLockKey, () -> {
                String genericKey = GENERIC_LOCK_PREFIX + stockId + ":" + orderId;
                Object existingCount = redisTemplate.opsForValue().get(genericKey);

                if (existingCount != null) {
                    redisTemplate.delete(genericKey);
                    int unlockCount = Integer.parseInt(existingCount.toString());
                    decrementTotalLock(stockId, unlockCount);
                    System.out.println("🔓 Generic kilit açıldı: " + unlockCount + " koltuk, Order: " + orderId);
                }
                return null;
            });
        } catch (Exception e) {
            String genericKey = GENERIC_LOCK_PREFIX + stockId + ":" + orderId;
            Object existingCount = redisTemplate.opsForValue().get(genericKey);
            if (existingCount != null) {
                redisTemplate.delete(genericKey);
                decrementTotalLock(stockId, Integer.parseInt(existingCount.toString()));
            }
        }
    }

    @Override
    public void unlockSeats(String stockId, List<String> seatLabels, String orderId) {
        System.out.println(" Koltuk kilitleri açılıyor: " + seatLabels);

        for (String seatLabel : seatLabels) {
            String distributedLockKey = DISTRIBUTED_LOCK_PREFIX + stockId + ":" + seatLabel;

            try {
                boolean lockAcquired = distributedLockService.tryLock(
                        distributedLockKey, 5, 10, TimeUnit.SECONDS
                );

                if (lockAcquired) {
                    try {
                        String seatLockKey = SEAT_LOCK_PREFIX + stockId + ":" + seatLabel;
                        String totalLockKey = TOTAL_LOCK_PREFIX + stockId;

                        Long result = redissonClient.getScript().eval(
                                RScript.Mode.READ_WRITE,
                                UNLOCK_SEAT_LUA_SCRIPT,
                                RScript.ReturnType.INTEGER,
                                Arrays.asList(seatLockKey, totalLockKey),
                                orderId
                        );

                        if (result != null && result == 1) {
                            System.out.println("   Kilit açıldı: " + seatLabel);
                        }
                    } finally {
                        distributedLockService.unlock(distributedLockKey);
                    }
                }
            } catch (Exception e) {
                System.err.println("   Kilit açma hatası (" + seatLabel + "): " + e.getMessage());
            }
        }
    }

    @Override
    public String isLocked(String stockId, String seatLabel) {
        String key = SEAT_LOCK_PREFIX + stockId + ":" + seatLabel;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public Map<String, String> getLockedSeats(String stockId) {
        Map<String, String> lockedSeats = new HashMap<>();
        String pattern = SEAT_LOCK_PREFIX + stockId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null) {
            for (String key : keys) {
                String seatLabel = key.substring(key.lastIndexOf(":") + 1);
                Object orderId = redisTemplate.opsForValue().get(key);
                if (orderId != null) {
                    lockedSeats.put(seatLabel, orderId.toString());
                }
            }
        }

        return lockedSeats;
    }

    @Override
    public int getLockedCount(String stockId) {
        try {
            String key = TOTAL_LOCK_PREFIX + stockId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Integer.parseInt(value.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getLockedCountForOrder(String stockId, String orderId) {
        try {
            String genericKey = GENERIC_LOCK_PREFIX + stockId + ":" + orderId;
            Object genericCount = redisTemplate.opsForValue().get(genericKey);

            if (genericCount != null) {
                return Integer.parseInt(genericCount.toString());
            }

            String pattern = SEAT_LOCK_PREFIX + stockId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            int count = 0;

            if (keys != null) {
                for (String key : keys) {
                    Object owner = redisTemplate.opsForValue().get(key);
                    if (orderId.equals(owner)) {
                        count++;
                    }
                }
            }

            return count;
        } catch (Exception e) {
            return 0;
        }
    }


    private void decrementTotalLock(String stockId, int count) {
        String key = TOTAL_LOCK_PREFIX + stockId;
        Long newValue = redisTemplate.opsForValue().decrement(key, count);
        if (newValue != null && newValue < 0) {
            redisTemplate.opsForValue().set(key, 0);
        }
    }
}
