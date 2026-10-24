#!/usr/bin/env bash
# 1.20.4 smoke test (stands in for the client game tests, which need 1.21.4+). Two dedicated 1.20.4 Fabric servers
# start: A with the built jar + Fabric API + Alloy Hub (port 25598), B with only Fabric API (25599). The dev client
# (src/smoketest, with MaLiLib, Mod Menu and Alloy Hub) joins A as op: every block with its toggle on and off (no
# rubber-banding), the Nocturne screen, the MaLiLib menu + hotkeys, the Hub page, then a rejoin (settings kept); then
# joins B: settings unavailable, vanilla movement, no kick. Afterwards A's config file must hold the player's settings.
# Run it through ./headless.sh so no window shows. Logs: build/run/smokeserver*/console.log, build/run/smoke/logs/latest.log
# Screenshots: build/run/smoke/screenshots
set -uo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-$HOME/.local/share/PrismLauncher/java/java-runtime-delta}"
MC=1.20.4 LOADER=0.19.5 INSTALLER=1.1.0 PLAYER=StickySmoke
FABRIC_API=$(find ~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.97.3+1.20.4 -name 'fabric-api-0.97.3+1.20.4.jar' | head -1)
HUB=$(ls libs/alloy-hub-*+1.20.4.jar | head -1)

./gradlew remapJar --no-daemon -q || exit 1
JAR=$(ls build/libs/alloy-stickytoggle-*+1.20.4*.jar | grep -v sources | head -1)
echo "jar: $JAR"
UUID=$(python3 -c "import hashlib,uuid;h=bytearray(hashlib.md5(b'OfflinePlayer:$PLAYER').digest());h[6]=h[6]&0x0f|0x30;h[8]=h[8]&0x3f|0x80;print(uuid.UUID(bytes=bytes(h)))")

PIDS=()
start_server() { # dir port jars...
    local S=$1 PORT=$2; shift 2
    rm -rf "$S/mods" "$S/world" "$S/logs" "$S/config"
    mkdir -p "$S/mods"
    [ -f build/fabric-server-launch.jar ] || curl -sSfL -o build/fabric-server-launch.jar \
        "https://meta.fabricmc.net/v2/versions/loader/$MC/$LOADER/$INSTALLER/server/jar" || exit 1
    cp build/fabric-server-launch.jar "$S/"
    cp "$@" "$S/mods/"
    echo "eula=true" > "$S/eula.txt"
    echo "[{\"uuid\":\"$UUID\",\"name\":\"$PLAYER\",\"level\":4,\"bypassesPlayerLimit\":false}]" > "$S/ops.json"
    cat > "$S/server.properties" <<PROPS
online-mode=false
server-port=$PORT
level-type=minecraft\:flat
spawn-protection=0
view-distance=4
simulation-distance=4
spawn-monsters=false
PROPS
    rm -f "$S/stdin"; mkfifo "$S/stdin"
    (cd "$S" && exec "$JAVA_HOME/bin/java" -Xmx1G -jar fabric-server-launch.jar nogui < stdin > console.log 2>&1) &
    PIDS+=($!)
}
mkdir -p build/run
start_server build/run/smokeserver 25598 "$JAR" "$FABRIC_API" "$HUB"
exec 3> build/run/smokeserver/stdin       # keep the fifos open so the servers don't see EOF
start_server build/run/smokeserver-plain 25599 "$FABRIC_API"
exec 4> build/run/smokeserver-plain/stdin

for S in build/run/smokeserver build/run/smokeserver-plain; do
    for _ in $(seq 1 180); do
        grep -q 'Done (' "$S/console.log" 2>/dev/null && break
        sleep 1
    done
    if ! grep -q 'Done (' "$S/console.log"; then
        echo "$S did not start"; tail -40 "$S/console.log"
        kill "${PIDS[@]}" 2>/dev/null; exit 1
    fi
done
echo "servers started"

rm -rf build/run/smoke/screenshots build/run/smoke/config/stickytoggle-client.json
./gradlew runSmokeTest -PwithHub --no-daemon -q
echo stop >&3; echo stop >&4
exec 3>&- 4>&-
wait "${PIDS[@]}"

echo "--- server with the mod"
grep -E 'stickytoggle|ERROR|Exception|Mixin|moved wrongly|moved too quickly' build/run/smokeserver/console.log | head -40
echo "--- server without the mod"
grep -E 'ERROR|Exception|lost connection|joined' build/run/smokeserver-plain/console.log | head -20
echo "--- client"
grep -E 'StickySmoke' build/run/smoke/logs/latest.log
OK=1
python3 - "$UUID" build/run/smokeserver/config/stickytoggle.json <<'PY' || OK=0
import json, sys
uuid, path = sys.argv[1], sys.argv[2]
p = json.load(open(path))["players"].get(uuid, {})
ok = p.get("slime.bounce") is False and p.get("honey.wallSlide") is True
print("--- server config file:", "PASS (slime off, honey on)" if ok else "FAIL " + json.dumps(p))
sys.exit(0 if ok else 1)
PY
grep -q '\[StickySmoke\] PASS$' build/run/smoke/logs/latest.log && [ $OK = 1 ]
