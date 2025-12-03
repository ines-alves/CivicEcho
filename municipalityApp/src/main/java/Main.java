import processor.MessageProcessor;

public class Main {
    public static void main(String[] args) {
        System.out.println("Municipality App Started");

        MessageProcessor processor = new MessageProcessor();
        processor.demonstrateProcessing();
    }
}