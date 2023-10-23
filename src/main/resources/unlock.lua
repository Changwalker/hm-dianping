-- 获取锁中的线程标识
-- 比较线程标识与锁中标识是否一致
-- 如果一致则释放锁（删除）
-- 如果不一致则什么都不做

-- KEYS[1]就是锁的key ARGV[1]就是当前线程标识
if(redis.call('get', KEYS[1]) == ARGV[1]) then
    -- 释放锁 del key
    return redis.call('del', KEYS[1])
end
return 0

