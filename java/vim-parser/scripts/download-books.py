#!/usr/bin/env python3
"""Download sample books for VIM parser testing and development."""

import argparse
import hashlib
import json
import os
import sys
import urllib.request
from datetime import datetime, timezone

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
BOOKS_JSON = os.path.join(SCRIPT_DIR, "books.json")
BOOKS_DIR = os.path.join(SCRIPT_DIR, "..", "samples", "books")
STATUS_JSON = os.path.join(BOOKS_DIR, "books-status.json")

CHUNK_SIZE = 8192


def sha256_file(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        while chunk := f.read(CHUNK_SIZE):
            h.update(chunk)
    return h.hexdigest()


def load_books() -> list:
    with open(BOOKS_JSON, "r", encoding="utf-8") as f:
        return json.load(f)


def load_status() -> dict:
    if os.path.exists(STATUS_JSON):
        with open(STATUS_JSON, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"last_run": None, "books": []}


def save_status(status: dict):
    os.makedirs(os.path.dirname(STATUS_JSON), exist_ok=True)
    with open(STATUS_JSON, "w", encoding="utf-8") as f:
        json.dump(status, f, indent=2, ensure_ascii=False)
        f.write("\n")


def find_status_entry(status: dict, book_id: str) -> dict | None:
    for entry in status["books"]:
        if entry["id"] == book_id:
            return entry
    return None


def download_file(url: str, dest: str) -> bool:
    """Download file from URL, try direct download. Returns True on success."""
    print(f"  Downloading from: {url}")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "portunix-vim-parser/1.0"})
        with urllib.request.urlopen(req, timeout=60) as resp:
            with open(dest, "wb") as out:
                while chunk := resp.read(CHUNK_SIZE):
                    out.write(chunk)
        return True
    except Exception as e:
        print(f"  Failed: {e}")
        return False


def process_book(book: dict, status: dict, force: bool = False) -> dict:
    """Process a single book entry. Returns status entry."""
    book_id = book["id"]
    filename = book["filename"]
    expected_hash = book.get("sha256")
    dest = os.path.join(BOOKS_DIR, filename)

    entry = find_status_entry(status, book_id) or {
        "id": book_id,
        "filename": filename,
        "downloaded": False,
        "hash_match": False,
        "actual_sha256": None,
        "last_checked": None,
    }

    # Skip if already downloaded and hash matches (unless forced)
    if not force and os.path.exists(dest) and entry.get("hash_match"):
        print(f"[SKIP] {book['name']} - already downloaded and verified")
        return entry

    # Check if file exists but needs verification
    if os.path.exists(dest) and not force:
        actual_hash = sha256_file(dest)
        if expected_hash and actual_hash == expected_hash:
            print(f"[OK]   {book['name']} - file exists, hash verified")
            entry.update({
                "downloaded": True,
                "hash_match": True,
                "actual_sha256": actual_hash,
                "last_checked": datetime.now(timezone.utc).isoformat(),
            })
            return entry

    # Try downloading from each source
    os.makedirs(BOOKS_DIR, exist_ok=True)
    downloaded = False
    for url in book.get("sources", []):
        if download_file(url, dest):
            downloaded = True
            break

    if not downloaded:
        print(f"[FAIL] {book['name']} - all sources failed")
        entry.update({
            "downloaded": False,
            "hash_match": False,
            "actual_sha256": None,
            "last_checked": datetime.now(timezone.utc).isoformat(),
            "error": "all sources failed",
        })
        return entry

    # Verify hash
    actual_hash = sha256_file(dest)
    hash_match = (actual_hash == expected_hash) if expected_hash else None

    if expected_hash and not hash_match:
        print(f"[WARN] {book['name']} - hash mismatch!")
        print(f"       Expected: {expected_hash}")
        print(f"       Actual:   {actual_hash}")
    elif expected_hash:
        print(f"[OK]   {book['name']} - downloaded and verified")
    else:
        print(f"[OK]   {book['name']} - downloaded (no hash to verify)")

    entry.update({
        "downloaded": True,
        "hash_match": hash_match if hash_match is not None else False,
        "actual_sha256": actual_hash,
        "last_checked": datetime.now(timezone.utc).isoformat(),
    })
    entry.pop("error", None)
    return entry


def main():
    parser = argparse.ArgumentParser(description="Download sample books for VIM parser testing")
    parser.add_argument("--force", action="store_true", help="Re-download all books even if they exist")
    parser.add_argument("--verify", action="store_true", help="Only verify existing files, do not download")
    parser.add_argument("--book", type=str, help="Download only a specific book by ID")
    args = parser.parse_args()

    books = load_books()
    status = load_status()

    if args.book:
        books = [b for b in books if b["id"] == args.book]
        if not books:
            print(f"Book with id '{args.book}' not found in books.json")
            sys.exit(1)

    print(f"Processing {len(books)} book(s)...\n")

    new_entries = []
    existing_ids = set()

    for book in books:
        existing_ids.add(book["id"])
        if args.verify and not os.path.exists(os.path.join(BOOKS_DIR, book["filename"])):
            print(f"[MISS] {book['name']} - file not found")
            entry = find_status_entry(status, book["id"]) or {
                "id": book["id"],
                "filename": book["filename"],
                "downloaded": False,
                "hash_match": False,
                "actual_sha256": None,
                "last_checked": datetime.now(timezone.utc).isoformat(),
            }
            entry["downloaded"] = False
            entry["hash_match"] = False
            new_entries.append(entry)
            continue

        if args.verify:
            dest = os.path.join(BOOKS_DIR, book["filename"])
            actual_hash = sha256_file(dest)
            expected = book.get("sha256")
            match = (actual_hash == expected) if expected else None
            if expected and not match:
                print(f"[WARN] {book['name']} - hash mismatch!")
                print(f"       Expected: {expected}")
                print(f"       Actual:   {actual_hash}")
            elif expected:
                print(f"[OK]   {book['name']} - hash verified")
            else:
                print(f"[OK]   {book['name']} - exists (no hash to verify)")

            entry = {
                "id": book["id"],
                "filename": book["filename"],
                "downloaded": True,
                "hash_match": bool(match),
                "actual_sha256": actual_hash,
                "last_checked": datetime.now(timezone.utc).isoformat(),
            }
            new_entries.append(entry)
            continue

        entry = process_book(book, status, force=args.force)
        new_entries.append(entry)

    # Keep status entries for books not in current run (partial --book run)
    if args.book:
        for old_entry in status["books"]:
            if old_entry["id"] not in existing_ids:
                new_entries.append(old_entry)

    status["last_run"] = datetime.now(timezone.utc).isoformat()
    status["books"] = new_entries
    save_status(status)

    # Summary
    print()
    ok = sum(1 for e in new_entries if e.get("downloaded") and e.get("hash_match"))
    warn = sum(1 for e in new_entries if e.get("downloaded") and not e.get("hash_match"))
    fail = sum(1 for e in new_entries if not e.get("downloaded"))
    print(f"Summary: {ok} verified, {warn} warnings, {fail} failed")


if __name__ == "__main__":
    main()
