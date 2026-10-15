#!/usr/bin/env bash
# Java 17 + Docker required. Uses a disposable local MySQL; no Firebase/production access.
set -euo pipefail
cd "$(dirname "$0")/.."
command -v docker >/dev/null
docker info >/dev/null
moa_test_temp=$(mktemp -d)
moa_test_container="moa-image-test-$$"
cleanup() {
    moa_test_status=$?
    trap - EXIT
    if [ -s "$moa_test_temp/container-id" ]; then
        docker rm -fv "$(cat "$moa_test_temp/container-id")" >/dev/null 2>&1 || true
    fi
    rm -rf "$moa_test_temp"
    exit "$moa_test_status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
umask 077
MOA_IMAGE_TEST_DB_PASSWORD=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')
export MOA_IMAGE_TEST_DB_PASSWORD
printf 'MYSQL_ROOT_PASSWORD=%s\nMYSQL_DATABASE=moa_image_test\n' "$MOA_IMAGE_TEST_DB_PASSWORD" > "$moa_test_temp/mysql.env"
docker run -d --name "$moa_test_container" --env-file "$moa_test_temp/mysql.env" \
    -p 127.0.0.1::3306 mysql:8.0 > "$moa_test_temp/container-id"
moa_test_ready=false
for ((i=0; i<90; i++)); do
    if docker exec "$moa_test_container" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -Nse "SELECT 1"' >/dev/null 2>&1; then
        moa_test_ready=true
        break
    fi
    sleep 1
done
if [ "$moa_test_ready" != true ]; then
    echo 'Disposable MySQL did not become ready.' >&2
    docker logs --tail 20 "$moa_test_container" >&2
    exit 1
fi
moa_test_port=$(docker inspect --format '{{(index (index .NetworkSettings.Ports "3306/tcp") 0).HostPort}}' "$moa_test_container")
export MOA_IMAGE_TEST_JDBC_URL="jdbc:mysql://127.0.0.1:$moa_test_port/moa_image_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export MOA_IMAGE_TEST_DB_USER=root
./gradlew --no-daemon --max-workers="${MOA_IMAGE_TEST_WORKERS:-2}" tripRecordImageMysqlTest
