import json
import logging
import os
import time
from collections.abc import Callable
from typing import Any

import redis

logger = logging.getLogger(__name__)

# How long to keep serving uncached before probing Redis again. Without this, one blip at boot
# would leave the process permanently uncached until someone redeployed it.
CACHE_RETRY_INTERVAL_SECONDS = 60


class AnimeCacheError(RuntimeError):
    pass


class AnimeCache:
    KEY_PREFIX = "asterion:anime:v1"
    LOCK_TIMEOUT_SECONDS = 45
    LOCK_WAIT_SECONDS = 30

    available = True

    def __init__(self, client: redis.Redis):
        self._client = client

    @classmethod
    def from_environment(cls) -> "AnimeCache":
        redis_url = os.environ.get("REDIS_URL", "").strip()
        if not redis_url:
            raise AnimeCacheError("REDIS_URL is required for the anime service.")
        return cls(redis.Redis.from_url(
            redis_url,
            decode_responses=True,
            socket_connect_timeout=3,
            socket_timeout=3,
            health_check_interval=30,
        ))

    def ping(self) -> None:
        try:
            self._client.ping()
        except redis.RedisError as error:
            raise AnimeCacheError("The anime cache is unavailable.") from error

    def get_json(self, key: str) -> Any | None:
        try:
            payload = self._client.get(self._key(key))
        except redis.RedisError:
            logger.warning("Anime cache read failed for %s, treating as a miss.", key, exc_info=True)
            return None
        if payload is None:
            return None
        try:
            return json.loads(payload)
        except json.JSONDecodeError:
            logger.warning("Anime cache holds invalid JSON for %s, treating as a miss.", key)
            return None

    def set_json(self, key: str, value: Any, ttl_seconds: int) -> None:
        try:
            self._client.setex(
                self._key(key),
                ttl_seconds,
                json.dumps(value, separators=(",", ":"), ensure_ascii=False),
            )
        except redis.RedisError:
            logger.warning("Anime cache write failed for %s, continuing uncached.", key, exc_info=True)

    def get_or_load(
        self,
        key: str,
        ttl_seconds: int,
        loader: Callable[[], Any],
    ) -> Any:
        cached = self.get_json(key)
        if cached is not None:
            return cached

        # The lock only exists to stop a cache miss becoming N concurrent scrapes. If Redis can't
        # give us one, scraping unsynchronised is still far better than failing the request.
        lock = None
        acquired = False
        try:
            lock = self._client.lock(
                self._key(f"lock:{key}"),
                timeout=self.LOCK_TIMEOUT_SECONDS,
                blocking_timeout=self.LOCK_WAIT_SECONDS,
            )
            acquired = lock.acquire(blocking=True)
        except redis.RedisError:
            logger.warning("Anime cache lock unavailable for %s, loading directly.", key, exc_info=True)
            return loader()

        if not acquired:
            # Someone else is already loading this and took longer than LOCK_WAIT_SECONDS.
            # Re-check once in case they finished while we waited, then load it ourselves.
            cached = self.get_json(key)
            if cached is not None:
                return cached
            return loader()

        try:
            cached = self.get_json(key)
            if cached is not None:
                return cached
            value = loader()
            self.set_json(key, value, ttl_seconds)
            return value
        finally:
            try:
                lock.release()
            except redis.exceptions.LockError:
                pass

    def _key(self, key: str) -> str:
        return f"{self.KEY_PREFIX}:{key}"


class NullAnimeCache:
    """
    Stands in when Redis isn't configured or can't be reached.

    Caching is what makes anime fast; it is not what makes anime work. Every request still gets
    real scraped data, just without the cache in front of it. Making Redis mandatory took the
    entire service offline whenever Redis was missing, because the container healthcheck failed
    and the proxy stopped routing to it.
    """

    available = False

    def ping(self) -> None:
        raise AnimeCacheError("The anime cache is not configured.")

    def get_json(self, key: str) -> Any | None:
        return None

    def set_json(self, key: str, value: Any, ttl_seconds: int) -> None:
        return None

    def get_or_load(self, key: str, ttl_seconds: int, loader: Callable[[], Any]) -> Any:
        return loader()


_cache: AnimeCache | NullAnimeCache | None = None
_last_probe_at: float = 0.0


def anime_cache() -> AnimeCache | NullAnimeCache:
    """
    Returns a usable cache, always. Falls back to [NullAnimeCache] when Redis is unreachable and
    re-probes every CACHE_RETRY_INTERVAL_SECONDS so the service heals itself once Redis returns.
    """
    global _cache, _last_probe_at

    if isinstance(_cache, AnimeCache):
        return _cache

    now = time.monotonic()
    if _cache is not None and (now - _last_probe_at) < CACHE_RETRY_INTERVAL_SECONDS:
        return _cache

    _last_probe_at = now
    try:
        cache = AnimeCache.from_environment()
        cache.ping()
    except AnimeCacheError as error:
        if _cache is None:
            logger.warning("Anime cache unavailable (%s). Serving uncached.", error)
        _cache = NullAnimeCache()
    else:
        logger.info("Anime cache connected.")
        _cache = cache
    return _cache


def reset_anime_cache() -> None:
    """Test seam - drops the memoised cache so the next call re-reads the environment."""
    global _cache, _last_probe_at
    _cache = None
    _last_probe_at = 0.0
