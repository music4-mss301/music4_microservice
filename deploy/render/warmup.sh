#!/usr/bin/env bash
# Đánh thức 7 service Music4 trên Render (gói free ngủ sau 15 phút, cold start 30-60 giây).
# Dùng: ./deploy/render/warmup.sh            (mặc định tiền tố music4-mss301)
#       PREFIX=ten-khac ./deploy/render/warmup.sh
# Gọi /actuator/health/liveness của từng service, thử lại tối đa 12 lần (cách nhau 10 giây).
PREFIX="${PREFIX:-music4-mss301}"
SERVICES="gateway user content engagement analytics media search"
MAX_TRIES=12
SLEEP_SECONDS=10

wake() {
  local name="$1" url="https://${PREFIX}-$1.onrender.com/actuator/health/liveness" code i
  for i in $(seq 1 "$MAX_TRIES"); do
    code=$(curl -s -o /dev/null -m 60 -w '%{http_code}' "$url")
    if [ "$code" = "200" ]; then
      echo "[ OK ] ${name} (lần thử ${i})"
      return 0
    fi
    echo "[ .. ] ${name}: HTTP ${code}, thử lại sau ${SLEEP_SECONDS}s (${i}/${MAX_TRIES})"
    sleep "$SLEEP_SECONDS"
  done
  echo "[FAIL] ${name}: không lên sau $((MAX_TRIES * SLEEP_SECONDS))s -> xem Logs trên Render"
  return 1
}

failed=0
# Chạy song song để 7 service cùng khởi động.
for s in $SERVICES; do
  wake "$s" &
done
for job in $(jobs -p); do
  wait "$job" || failed=$((failed + 1))
done

if [ "$failed" -eq 0 ]; then
  echo "Tất cả 7 service đã sẵn sàng."
else
  echo "Có ${failed} service chưa lên."
  exit 1
fi
