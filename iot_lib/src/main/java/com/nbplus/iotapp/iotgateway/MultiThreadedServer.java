/*
 * Copyright (c) 2015. NB Plus (www.nbplus.co.kr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nbplus.iotapp.iotgateway;

import android.os.Handler;
import android.os.Messenger;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by basagee on 2015. 11. 23..
 *
 * Multithreaded Server Advantages
 *
 *    The advantages of a multithreaded server compared to a singlethreaded server are summed up below:
 *        1. Less time is spent outside the accept() call.
 *        2. Long running client requests do not block the whole server
 *    As mentioned earlier the more time the thread calling serverSocket.accept() spends inside this method call,
 *    the more responsive the server will be. Only when the listening thread is inside the accept() call
 *    can clients connect to the server. Otherwise the clients just get an error.

 *    In a singlethreaded server long running requests may make the server unresponsive for a long period.
 *    This is not true for a multithreaded server, unless the long-running request takes up all CPU time time and/or network bandwidth.
 */
public class MultiThreadedServer implements Runnable {
    private static final String TAG = MultiThreadedServer.class.getSimpleName();

    protected int          serverPort   = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected Messenger    mServiceMessenger = null;

    public MultiThreadedServer(Messenger serviceMessenger, int port){
        this.serverPort = port;
        this.mServiceMessenger = serviceMessenger;
    }

    public void run() {
        synchronized(this) {
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while (!isStopped()) {
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    Log.d(TAG, "Server Stopped.") ;
                    return;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }
            new Thread(
                new WorkerRunnable(this.mServiceMessenger, clientSocket, "Multithreaded Server")
            ).start();
        }
        Log.d(TAG, "Server Stopped.") ;
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(boolean sendBroadcast) {
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        } finally {
            if (sendBroadcast) {

            }
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port : " + this.serverPort, e);
        }
    }

}
