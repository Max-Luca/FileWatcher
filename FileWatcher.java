import java.io.File;
import java.util.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileWatcher implements Subject {
    private File directory;
    private Map<String, Long> fileSizes = new HashMap<>();
    private Map<String, Long> lastModifiedTimes = new HashMap<>();
    private Map<String, String> fileIdentities = new HashMap<>(); // filename -> identity (path+size+modified)
    private List<Observer> observers = new ArrayList<>();

    public void setDirectory(String path) {
        directory = new File(path);
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }

    private String getCurrentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        return LocalTime.now().format(formatter);
    }

    private String getFileIdentity(File file) {
        // Use absolute path + size + creation time as a unique identifier
        try {
            Path path = file.toPath();
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            // Use creation time as the identity marker (creation time doesn't change on rename)
            return attrs.creationTime().toString() + "_" + attrs.size();
        } catch (Exception e) {
            // Fallback to size + modified time if creation time not available
            return file.length() + "_" + file.lastModified();
        }
    }

    public void start() throws InterruptedException {
        if (directory == null || !directory.isDirectory()) {
            System.out.println("Invalid directory.");
            return;
        }

        System.out.println("Watching directory: " + directory.getAbsolutePath());
        while (true) {
            File[] files = directory.listFiles();
            Set<String> currentFiles = new HashSet<>();
            Map<String, String> currentIdentities = new HashMap<>(); // filename -> identity
            Map<String, String> identityToFilename = new HashMap<>(); // identity -> filename

            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String identity = getFileIdentity(file);
                    currentFiles.add(fileName);
                    currentIdentities.put(fileName, identity);
                    identityToFilename.put(identity, fileName);
                }
            }

            // Check for renames first (same identity, different name)
            Map<String, String> renames = new HashMap<>(); // oldName -> newName
            for (Map.Entry<String, String> oldEntry : fileIdentities.entrySet()) {
                String oldName = oldEntry.getKey();
                String oldIdentity = oldEntry.getValue();
                
                // If this identity still exists but under a different name
                if (identityToFilename.containsKey(oldIdentity)) {
                    String newName = identityToFilename.get(oldIdentity);
                    if (!oldName.equals(newName) && !currentIdentities.containsKey(oldName)) {
                        renames.put(oldName, newName);
                    }
                }
            }

            // Process renames
            for (Map.Entry<String, String> rename : renames.entrySet()) {
                String oldName = rename.getKey();
                String newName = rename.getValue();
                String time = getCurrentTime();
                
                // Update tracking maps
                String identity = fileIdentities.get(oldName);
                fileIdentities.remove(oldName);
                fileIdentities.put(newName, identity);
                
                Long size = fileSizes.get(oldName);
                fileSizes.remove(oldName);
                fileSizes.put(newName, size);
                
                Long modTime = lastModifiedTimes.get(oldName);
                lastModifiedTimes.remove(oldName);
                lastModifiedTimes.put(newName, modTime);
                
                notifyObservers("[" + time + "] File renamed: " + oldName + " → " + newName);
            }

            // Now check for new files, modifications, and size changes
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String identity = currentIdentities.get(fileName);
                    long currentSize = file.length();
                    long lastModified = file.lastModified();
                    String time = getCurrentTime();

                    // Skip if this was a rename we already handled
                    boolean wasRenamed = renames.containsValue(fileName);

                    // New file (and not a rename)
                    if (!fileSizes.containsKey(fileName) && !wasRenamed) {
                        fileSizes.put(fileName, currentSize);
                        lastModifiedTimes.put(fileName, lastModified);
                        fileIdentities.put(fileName, identity);
                        notifyObservers("[" + time + "] File added: " + fileName);
                    } 
                    // File modification (timestamp changed, but not renamed)
                    else if (fileSizes.containsKey(fileName) && lastModifiedTimes.get(fileName) != lastModified) {
                        lastModifiedTimes.put(fileName, lastModified);
                        fileIdentities.put(fileName, identity);
                        notifyObservers("[" + time + "] File Modification detected: " + fileName);
                    } 
                    // File size changed
                    else if (fileSizes.containsKey(fileName) && fileSizes.get(fileName) != currentSize) {
                        fileSizes.put(fileName, currentSize);
                        fileIdentities.put(fileName, identity);
                        notifyObservers("[" + time + "] File size changed: " + fileName);
                    }
                }
            }

            // Check for deleted files (not renamed)
            for (String oldFile : new HashSet<>(fileSizes.keySet())) {
                if (!currentFiles.contains(oldFile)) {
                    String oldIdentity = fileIdentities.get(oldFile);
                    // Only report as deleted if it wasn't renamed
                    if (!identityToFilename.containsKey(oldIdentity)) {
                        String time = getCurrentTime();
                        fileSizes.remove(oldFile);
                        lastModifiedTimes.remove(oldFile);
                        fileIdentities.remove(oldFile);
                        notifyObservers("[" + time + "] File deleted: " + oldFile);
                    }
                }
            }

            Thread.sleep(3000); // Check every 3 seconds
        }
    }
}
