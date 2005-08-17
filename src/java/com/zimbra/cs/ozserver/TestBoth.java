package com.zimbra.cs.ozserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.zimbra.cs.util.Liquid;

class TestBoth {

    private static final int CLIENT_THREADS = 2;
    
    static class TestClientThread extends Thread {
        public TestClientThread(int num) {
            super("TestClientThread-" + num);
            setDaemon(true);
        }

        public void run() {
            while (true) {
                try {
                    TestClient.test();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        
    }
    
    public static void main(String[] args) throws IOException {
        Liquid.toolSetup("DEBUG");
        TestServer.main(null);
        for (int i = 0; i < CLIENT_THREADS; i++) {
            new TestClientThread(i).start();
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        br.readLine();
        TestServer.shutdown();
    }
}
