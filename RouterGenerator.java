public class RouterGenerator {


    public RouterGenerator(int port) {
        String[] args = new String[1];
        args[0] = ""+port;
        RouterGeneric.main(args);
    }

    public static void main(String[] args) {

        //Can instantiate any number of routers by adding another thread but changing the port number to a unique one
        //Routers should be @ ports 50007-50020

        new Thread() {
            @Override
            public void run() {
                new RouterGenerator(50007);
            }
        }.start();      //R1
        new Thread() {
            @Override
            public void run() {
                new RouterGenerator(50008);
            }
        }.start();      //R2
        new Thread() {
            @Override
            public void run() {
                new RouterGenerator(50009);
            }
        }.start();      //R3
        new Thread() {
            @Override
            public void run() {
                new RouterGenerator(50010);
            }
        }.start();      //R4
        new Thread() {
            @Override
            public void run() {
                new RouterGenerator(50011);
            }
        }.start();      //R5
        new Thread() {
            @Override
            public void run() {
                new RouterGenerator(50012);
            }
        }.start();      //R6
        new Thread() {
            @Override
            public void run() {
                new RouterGenerator(50013);
            }
        }.start();      //R7
        new Thread() {
            @Override
            public void run() {
                new RouterGenerator(50014);
            }
        }.start();      //R8


    }
}
