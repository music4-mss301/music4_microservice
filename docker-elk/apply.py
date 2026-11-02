#!/usr/bin/env python3
"""
Hạ tầng Docker + ELK cho Music4: 9 Dockerfile, docker-compose.yml đầy đủ, Logstash/Kibana, logback-spring.xml.
Chạy ở GỐC repo (thư mục có pom.xml), thư mục này đặt ở ./docker-elk:
    python docker-elk/apply.py
Chạy lại nhiều lần được. Giữ kiểu xuống dòng (CRLF/LF) của file pom.
"""
import re
import shutil
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path.cwd()
FILES = HERE / "files"
MODULES = ["config-server", "eureka-server", "api-gateway", "user-service", "content-service",
           "engagement-service", "analytics-service", "media-service", "search-admin-service"]
DEP = ("<dependency>\n"
       "            <groupId>net.logstash.logback</groupId>\n"
       "            <artifactId>logstash-logback-encoder</artifactId>\n"
       "            <version>8.0</version>\n"
       "        </dependency>")
changed = []

if not (ROOT / "pom.xml").exists():
    sys.exit("Hãy chạy ở gốc repo (thư mục có pom.xml).")

for f in sorted(FILES.rglob("*")):
    if f.is_file():
        rel = f.relative_to(FILES)
        dst = ROOT / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        existed = dst.exists()
        if not existed or dst.read_bytes() != f.read_bytes():
            shutil.copyfile(f, dst)
            changed.append(("ĐÈ  " if existed else "MỚI ") + str(rel).replace("\\", "/"))


def add_dependency(pom: Path):
    raw = pom.read_bytes().decode("utf-8")
    if "logstash-logback-encoder" in raw:
        return False
    nl = "\r\n" if "\r\n" in raw else "\n"
    text = raw.replace("\r\n", "\n")
    managed = [(m.start(), m.end()) for m in re.finditer(r"<dependencyManagement>.*?</dependencyManagement>", text, re.S)]
    for m in re.finditer(r"<dependencies>.*?</dependencies>", text, re.S):
        if any(a <= m.start() < b for a, b in managed):
            continue
        close = m.end() - len("</dependencies>")
        text = text[:close] + "    " + DEP + "\n    " + text[close:]
        pom.write_bytes(text.replace("\n", nl).encode("utf-8"))
        return True
    return False


for m in MODULES:
    pom = ROOT / m / "pom.xml"
    if pom.exists():
        if add_dependency(pom):
            changed.append(f"SỬA {m}/pom.xml (thêm logstash-logback-encoder)")
    else:
        print("CẢNH BÁO: không thấy", pom)

gi = ROOT / ".gitignore"
if gi.exists():
    t = gi.read_bytes().decode("utf-8")
    if not re.search(r"(?m)^\.env\.docker\s*$", t):
        nl = "\r\n" if "\r\n" in t else "\n"
        gi.write_bytes((t.rstrip("\r\n") + nl + ".env.docker" + nl).encode("utf-8"))
        changed.append("SỬA .gitignore (thêm .env.docker)")

print(f"{len(changed)} thay đổi:")
for c in changed:
    print("  ", c)
print("\nBước tiếp: sao chép .env.docker.example thành .env.docker rồi điền 4 bí mật, sau đó:")
print("  docker compose up -d --build")
