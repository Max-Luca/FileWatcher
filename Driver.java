public class Driver {
    public static void main(String[] args) {
        try {
            FileWatcher watcher = new FileWatcher();
            watcher.setDirectory("./TestFolder");

            // Create three observers
            Observer o1 = new ConsoleObserver("Observer 1");
            Observer o2 = new ConsoleObserver("Observer 2");
            Observer o3 = new ConsoleObserver("Observer 3");

            // Register them with the FileWatcher
            watcher.addObserver(o1);
            watcher.addObserver(o2);
            watcher.addObserver(o3);

            // Start watching for file changes
            watcher.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

