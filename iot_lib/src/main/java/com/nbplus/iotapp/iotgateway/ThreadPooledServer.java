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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by basagee on 2015. 11. 23..

 * The advantages of a thread pooled server compared to a multithreaded server is that you can control
 * the maximum number of threads running at the same time. This has certain advantages.
 *
 * First of all if the requests require a lot of CPU time,
 * RAM or network bandwidth, this may slow down the server if many requests are processed at the same time.
 * For instance, if memory consumption causes the server to swap memory in and out of disk,
 * this will result in a serious performance penalty.
 * By controlling the maximum number of threads you can minimize the risk of resource depletion,
 * both due to limiting the memory taken by the processing of the requests,
 * but also due to the limitation and reuse of the threads.
 * Each thread take up a certain amount of memory too, just to represent the thread itself.
 *
 * Additionally, executing many requests concurrently will slow down all requests processed.
 * For instance, if you process 1.000 requests concurrently and each request takes 1 second,
 * then all requests will take 1.000 seconds to complete.
 * If you instead queue the requests up and process them say 10 at a time,
 * the first 10 requests will complete after 10 seconds, the next 10 will complete after 20 seconds etc.
 * Only the last 10 requests will complete after 1.000 seconds. This gives a better service to the clients.
 */
public class ThreadPooledServer implements Runnable {
    private static final String TAG = ThreadPooledServer.class.getSimpleName();

    protected int          serverPort   = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected ExecutorService threadPool = null;
    protected Messenger    mServiceMessenger = null;

    public static final int DEFAULT_MAX_THREAD_POOL_SIZE = 10;
    protected int threadPoolSize = 0;

    public ThreadPooledServer(Messenger serviceMessenger, int port) {
        this.serverPort = port;
        this.threadPoolSize = DEFAULT_MAX_THREAD_POOL_SIZE;
        this.threadPool = Executors.newFixedThreadPool(this.threadPoolSize);
        this.mServiceMessenger = serviceMessenger;
    }

    public ThreadPooledServer(Messenger serviceMessenger, int port, int maxThreadPoolSize) {
        this.serverPort = port;
        this.threadPoolSize = maxThreadPoolSize;
        this.threadPool = Executors.newFixedThreadPool(this.threadPoolSize);
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
                    break;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }
            this.threadPool.execute(
                    new WorkerRunnable(this.mServiceMessenger, clientSocket, "Thread Pooled Server"));
        }
        this.threadPool.shutdown();
        Log.d(TAG, "Server Stopped.");
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
            throw new RuntimeException("Cannot open port :" + this.serverPort, e);
        }
    }
}
