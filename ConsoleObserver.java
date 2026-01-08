public class ConsoleObserver implements Observer {
    private String name;

    public ConsoleObserver(String name) {
        this.name = name;
    }

    @Override
    public void update(String message) {
        System.out.println("[" + name + "] " + message);
    }
}

