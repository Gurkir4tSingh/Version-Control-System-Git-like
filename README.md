# GitLite VCS
### A Git-like Version Control System in Java
**Data Structures Course Project**

---

## Overview

GitLite is a fully functional version control system built from scratch in Java,
demonstrating core data structures taught in a university Data Structures course.
It mimics the behaviour of Git — tracking file changes, supporting branches,
detecting tampering, and logging all user activity.

---

## Data Structures Used

| Data Structure | Class | Purpose |
|---|---|---|
| **Doubly Linked List** | `VersionLinkedList` | Stores ordered version history per file |
| **Stack (pair)** | `UndoRedoStack` | Undo / redo commit operations |
| **Hash Table** | `FileHashTable` | O(1) average file lookup by name |
| **N-ary Tree** | `BranchTree` | Branch hierarchy (main → feature → sub-feature) |
| **Custom Hash (FNV-1a)** | `HashUtils` | 64-bit version ID generation |
| **SHA-256** | `HashUtils` | Content integrity / tamper detection |
| **LCS DP Table** | `DiffEngine` | Line-by-line diff algorithm |

---

## Features

### Core VCS
- **Track files** — add any file to the repository for version tracking
- **Commit** — snapshot the current content with author + message
- **Commit log** — display full history with linked-list diagram
- **Diff** — compare any two versions line-by-line (LCS-based diff)
- **Undo / Redo** — stack-based, like an editor's undo history
- **Rollback** — hard-reset a file to any previous version

### Security
- **SHA-256 hashing** — every commit stores a cryptographic hash of content
- **Integrity check** — verify that stored versions have not been modified
- **Tampering simulation** — demo of what a hash mismatch looks like
- **Rollback-attack detection** — flags large backward jumps as suspicious
- **Rapid-commit detection** — flags bot-like bursts of commits
- **Mass-delete detection** — flags users deleting many files in a session
- **User activity log** — every action is recorded with timestamp + severity
- **Suspicious log view** — filter to WARNING / SUSPICIOUS / CRITICAL events

### Branching
- **Create branches** — fork from the current branch
- **Switch branches** — checkout any existing branch
- **Branch tree view** — visual N-ary tree diagram of all branches
- **Ancestry path** — trace any branch back to `main`

---

## Project Structure

```
GitLiteVCS/
├── build.sh                          ← Linux/macOS build & run
├── build.bat                         ← Windows build & run
├── README.md
└── src/main/java/gitlite/
    ├── Main.java                     ← Entry point
    │
    ├── model/
    │   ├── FileVersion.java          ← Linked list node (one commit)
    │   └── UserLog.java              ← Audit log entry
    │
    ├── datastructures/
    │   ├── VersionLinkedList.java    ← Doubly linked list
    │   ├── UndoRedoStack.java        ← Stack pair (undo + redo)
    │   ├── FileHashTable.java        ← Hash table (separate chaining)
    │   └── BranchTree.java           ← N-ary tree
    │
    ├── core/
    │   ├── TrackedFile.java          ← File with its own history + stack
    │   ├── Repository.java           ← Central coordinator
    │   └── DiffEngine.java           ← LCS-based line diff
    │
    ├── security/
    │   ├── HashUtils.java            ← SHA-256 + FNV-1a + naive hash
    │   └── SecurityMonitor.java      ← Activity logging + attack detection
    │
    └── ui/
        ├── VCSConsole.java           ← Interactive CLI shell
        └── Demo.java                 ← Automated feature demonstration
```

---

## How to Run

### Requirements
- **Java 17 or later**

### Linux / macOS
```bash
chmod +x build.sh

# Interactive shell
./build.sh

# Automated demo (recommended first run)
./build.sh demo
```

### Windows
```bat
REM Interactive shell
build.bat

REM Automated demo
build.bat demo
```

### Manual compile & run
```bash
mkdir out
find src -name "*.java" > sources.txt
javac --release 17 -d out @sources.txt

# Demo
java -cp out gitlite.Main demo

# Interactive shell
java -cp out gitlite.Main
```

---

## Interactive Shell Commands

```
FILE OPERATIONS
  add <file>                  Track a new file
  edit <file> <content>       Set working copy content
  commit <file> <message>     Save a new version

HISTORY
  log <file>                  Show all commits (linked list diagram)
  diff <file> <vA> <vB>       Compare two versions
  undo <file>                 Undo last commit (stack pop)
  redo <file>                 Redo undone commit (stack pop)
  rollback <file> <version>   Hard rollback to a specific version

INTEGRITY
  verify <file>               Check all version hashes

BRANCHES
  branch <name>               Create a branch
  checkout <name>             Switch branch
  branches                    Show branch tree

SECURITY
  logs                        All activity logs
  suspicious                  Suspicious/critical logs only
  simulate-tamper <f> <txt>   Simulate a hash tampering attack
  simulate-rollback <f>       Simulate a rollback attack

GENERAL
  user <name>                 Switch current user
  status                      Repository overview
  hashtable                   Show hash table internals
  help                        Command reference
  exit                        Quit
```

### Example Session
```
gitlite (main) [alice] > add App.java
  ✓ Tracking file: App.java

gitlite (main) [alice] > edit App.java public class App { }
  ✓ Working copy updated for: App.java

gitlite (main) [alice] > commit App.java "Initial commit"
  ✓ Committed: [v1] ...

gitlite (main) [alice] > edit App.java public class App { int x = 1; }
gitlite (main) [alice] > commit App.java "Added field x"

gitlite (main) [alice] > log App.java
gitlite (main) [alice] > diff App.java 1 2
gitlite (main) [alice] > undo App.java
gitlite (main) [alice] > verify App.java
gitlite (main) [alice] > simulate-rollback App.java
```

---

## Algorithm Notes

### Diff (DiffEngine)
Uses the classic **Longest Common Subsequence (LCS)** dynamic programming
algorithm (O(m × n) time, O(m × n) space) to find the minimal edit distance
between two versions, then backtracks through the DP table to produce the
list of added/removed/unchanged lines.

### Hash Table (FileHashTable)
Uses a **polynomial rolling hash** (djb2-style) for bucket assignment and
**separate chaining** for collision resolution. Automatically resizes at
0.75 load factor.

### Version ID (HashUtils.generateVersionId)
Uses **FNV-1a 64-bit** over `content + author + timestamp`. This is
intentionally different from SHA-256 to illustrate that different hash
functions serve different purposes (speed vs cryptographic strength).

### Rollback Attack Detection (SecurityMonitor)
Flags any rollback where `currentVersion - targetVersion > 3`.
In real systems (e.g. TLS, firmware update protocols) a rollback attack
reverts software to a version with known CVEs, bypassing security patches.

---

## Course Topics Covered

| Topic | Implementation |
|---|---|
| Linked List | `VersionLinkedList` — doubly linked, O(1) append, O(n) search |
| Stack | `UndoRedoStack` — custom linked-list-backed stack, O(1) push/pop |
| Hash Table | `FileHashTable` — open addressing with chaining, auto-resize |
| Tree | `BranchTree` — general N-ary tree, DFS print |
| Hashing | `HashUtils` — SHA-256, FNV-1a, naïve sum (contrast) |
| Algorithm | `DiffEngine` — LCS DP, O(m×n) |
| OOP Design | Repository, TrackedFile, SecurityMonitor separation of concerns |
