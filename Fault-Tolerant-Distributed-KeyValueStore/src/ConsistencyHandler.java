public class ConsistencyHandler implements Runnable {

    private String operation;

    public ConsistencyHandler(String operationIn) {

        operation = operationIn;
    }

    @Override
    public void run() {

        if (operation.equalsIgnoreCase("get")) performReadRepair();
        if (operation.equalsIgnoreCase("put")) performHintedHandoff();

    }

    public void performReadRepair() {

    }

    public void performHintedHandoff() {

    }
}
