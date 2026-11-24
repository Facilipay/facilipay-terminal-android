#!/usr/bin/env python3
"""Publishes one version of the library to the developer site's Maven repository on S3.

Write-once and safe to re-run:
  1. Every object already under maven/<group>/<version>/ must be in this build, byte for byte. A
     re-run after a partial upload continues; anything that differs fails (publish a new version).
  2. The missing files are uploaded with If-None-Match: *, so S3 itself refuses to overwrite.
  3. maven-metadata.xml is merged (every version kept; latest and release are the highest by
     version number, a final above its release candidates) and written with If-Match on the ETag
     read, so a concurrent writer fails this run instead of being overwritten. Its checksums follow.

Any AWS error fails the run. Needs the AWS CLI (v2.22+ for conditional writes) with credentials.

Usage: publish_maven.py <local maven repository root>
Environment: BUCKET, GROUP_PATH (io/facilipay/terminal-sdk), VERSION.
"""
import hashlib
import json
import os
import pathlib
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

IMMUTABLE = "public, max-age=31536000, immutable"
SHORT = "public, max-age=60"
CONTENT_TYPES = {".pom": "text/xml", ".xml": "text/xml", ".module": "application/json",
                 ".aar": "application/java-archive", ".jar": "application/java-archive"}


def aws(*args: str, ok_404: bool = False) -> subprocess.CompletedProcess:
    done = subprocess.run(["aws", *args], capture_output=True, text=True)
    if done.returncode != 0:
        if ok_404 and "(404)" in done.stderr:
            return done
        sys.exit(f"::error::aws {' '.join(args[:2])} failed: {done.stderr.strip()}")
    return done


def content_type(name: str) -> str:
    return CONTENT_TYPES.get(pathlib.Path(name).suffix, "text/plain")


def version_key(version: str) -> tuple:
    match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)(?:-rc\.(\d+))?", version)
    if not match:
        sys.exit(f"::error::Unexpected version {version!r} in maven-metadata.xml")
    major, minor, patch, rc = match.groups()
    # A final sorts above every release candidate of the same version.
    return int(major), int(minor), int(patch), 1 if rc is None else 0, int(rc or 0)


def put(path: pathlib.Path, key: str, cache: str, *condition: str) -> None:
    aws("s3api", "put-object", "--bucket", bucket, "--key", key, "--body", str(path),
        "--cache-control", cache, "--content-type", content_type(path.name), *condition)


repo = pathlib.Path(sys.argv[1])
bucket, group, version = os.environ["BUCKET"], os.environ["GROUP_PATH"], os.environ["VERSION"]
local = repo / group / version
if not local.is_dir() or not any(local.iterdir()):
    sys.exit(f"::error::Nothing built at {local}")
prefix = f"maven/{group}/{version}/"
work = pathlib.Path(tempfile.mkdtemp())

# 1. What is already published for this version must be exactly this build.
listed = json.loads(aws("s3api", "list-objects-v2", "--bucket", bucket, "--prefix", prefix,
                        "--output", "json").stdout or "{}")
remote = {item["Key"] for item in listed.get("Contents", [])}
if listed.get("IsTruncated"):
    sys.exit("::error::Too many objects under the version folder")
for key in sorted(remote):
    built = local / key[len(prefix):]
    if not built.is_file():
        sys.exit(f"::error::{key} is published but not in this build: publish a new version")
    copy = work / "remote"
    aws("s3api", "get-object", "--bucket", bucket, "--key", key, str(copy))
    if hashlib.sha256(copy.read_bytes()).digest() != hashlib.sha256(built.read_bytes()).digest():
        sys.exit(f"::error::{key} differs from this build: {version} is published, publish a new version")

# 2. Upload what is missing; S3 refuses an overwrite.
for built in sorted(local.iterdir()):
    key = prefix + built.name
    if key not in remote:
        put(built, key, IMMUTABLE, "--if-none-match", "*")
print(f"{len(list(local.iterdir())) - len(remote)} files uploaded, {len(remote)} already there")

# 3. maven-metadata.xml: merge, then write it before its checksums.
metadata_key = f"maven/{group}/maven-metadata.xml"
head = aws("s3api", "head-object", "--bucket", bucket, "--key", metadata_key, ok_404=True)
new_root = ET.parse(repo / group / "maven-metadata.xml").getroot()
if head.returncode == 0:
    etag = json.loads(head.stdout)["ETag"]
    current = work / "maven-metadata.xml"
    aws("s3api", "get-object", "--bucket", bucket, "--key", metadata_key, "--if-match", etag, str(current))
    root = ET.parse(current).getroot()
    condition = ("--if-match", etag)
else:
    root = new_root
    condition = ("--if-none-match", "*")
versions = root.find("versioning/versions")
known = {v.text for v in versions}
for v in new_root.find("versioning/versions"):
    if v.text not in known:
        versions.append(v)
ordered = sorted((v.text for v in versions), key=version_key)
for v in list(versions):
    versions.remove(v)
for text in ordered:
    ET.SubElement(versions, "version").text = text
versioning = root.find("versioning")
(versioning.find("latest") if versioning.find("latest") is not None else ET.SubElement(versioning, "latest")).text = ordered[-1]
finals = [v for v in ordered if "-" not in v]
release = versioning.find("release")
if finals:
    if release is None:
        release = ET.SubElement(versioning, "release")
    release.text = finals[-1]
elif release is not None:
    # Only release candidates so far: <release> names a final version, so leave it out.
    versioning.remove(release)
versioning.find("lastUpdated").text = new_root.find("versioning/lastUpdated").text
merged = work / "maven-metadata.xml"
ET.ElementTree(root).write(merged, xml_declaration=True, encoding="UTF-8")
put(merged, metadata_key, SHORT, *condition)
for algorithm in ("md5", "sha1", "sha256", "sha512"):
    checksum = work / f"maven-metadata.xml.{algorithm}"
    checksum.write_text(hashlib.new(algorithm, merged.read_bytes()).hexdigest())
    put(checksum, f"{metadata_key}.{algorithm}", SHORT)
print(f"maven-metadata.xml: latest {ordered[-1]}, release {finals[-1] if finals else 'none'}")
