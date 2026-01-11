# FileWatcher

A Java application that monitors a directory for file changes using the Observer design pattern.

## Features

- Detects file additions, deletions, modifications, renames, and size changes
- Real-time notifications with timestamps
- Multiple observer support

## Usage

-----------------------
javac *.java
java Driver
-----------------------

The application monitors `./TestFolder` by default. Edit `Driver.java` to change the directory path.

## Requirements

Java 8 or higher

## How It Works

FileWatcher polls the directory every 3 seconds and notifies registered observers of any changes.

## Example Output

---------------------------------------------------------------------
[Observer 1] [02:30:15 PM] File added: document.txt
[Observer 1] [02:30:21 PM] File renamed: document.txt → report.txt
[Observer 1] [02:30:27 PM] File Modification detected: report.txt
---------------------------------------------------------------------
