public class Application {
    public static void main(String[] args) {

        //start the Controller
        new Thread() {
            @Override
            public void run() {
                Controller.main(null);
            }
        }.start();

        //start the Endpoint
        new Thread() {
            @Override
            public void run() {
                EndpointGenerator.main(null);
            }
        }.start();

        //Start the Routers
        new Thread() {
            @Override
            public void run() {
                RouterGenerator.main(null);
            }
        }.start();
    }
}
