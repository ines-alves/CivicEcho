import processor.MessageProcessor;

public class Main {
    public static void main(String[] args) {
        System.out.println("Municipality App Started");

        MessageProcessor processor = new MessageProcessor();

        // Test report creation to database
        processor.testCreateReport();
    }
}