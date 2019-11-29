public class EndpointGenerator {


    public EndpointGenerator(int srcPort, int routerPort) {
        String[] args = new String[2];
        args[0] = ""+srcPort;
        args[1] = ""+routerPort;
        EndpointGeneric.main(args);
    }

    public static void main(String[] args) {

        //Can instantiate any number of endpoints by adding another thread but changing the port number to a unique one
        //Endpoints should be @ ports 50001-50006

        new Thread() {
            @Override
            public void run() {
                new EndpointGenerator(50001, 50007);
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                new EndpointGenerator(50002, 50014);
            }
        }.start();

    }
}
