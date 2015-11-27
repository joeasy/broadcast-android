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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.nbplus.iotlib.data.IoTServiceCommand;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;

/**
 * Created by basagee on 2015. 11. 23..
 */
public class WorkerRunnable implements Runnable {
    private static final String TAG = WorkerRunnable.class.getSimpleName();

    protected Socket clientSocket = null;
    protected String serverText   = null;
    protected WeakReference<Messenger> mServiceMessengerRef = null;
    protected Handler mWorkerRunnableHandler = null;

    // 핸들러를 전달할 방법이 메신저 외에는 없는듯.
    protected Messenger mWorkerMessenger = null;

    // used to send messages
    private DataOutputStream mDataOut;
    // used to read messages from the server
    private DataInputStream mDataIn;
    private boolean mIsStopped;

    public WorkerRunnable(Messenger serviceMessenger, Socket clientSocket, String serverText) {
        this.clientSocket = clientSocket;
        this.serverText   = serverText;
        if (serviceMessenger != null) {
            this.mServiceMessengerRef = new WeakReference<>(serviceMessenger);
        }
    }

    public void run() {
        boolean isSendBroadcast = true;
        try {
            /*
             * creating the handler.
             */
            mWorkerRunnableHandler = new WorkerRunnableHandler(Looper.getMainLooper(), this);
            mWorkerMessenger = new Messenger(mWorkerRunnableHandler);

            // TODO : test handler
            if (mServiceMessengerRef != null) {
                try {
                    Messenger serviceMessenger = mServiceMessengerRef.get();
                    if (serviceMessenger != null) {
                        Message msg = new Message();
                        msg.what = IoTServiceCommand.IOT_GATEWAY_CONNECTED;
                        msg.replyTo = mWorkerMessenger;
                        serviceMessenger.send(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            mDataIn  = new DataInputStream(clientSocket.getInputStream());
            mDataOut = new DataOutputStream(clientSocket.getOutputStream());
            while (!isStopped()) {
                mDataOut.writeBytes("{ \"cmd\":\"123\", \"bb\":\"ccc\" }");
            }
            isSendBroadcast = false;
        } catch (EOFException e) {
            Log.e(TAG, "EOFException received", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException received", e);
        } catch (Exception e) {
            Log.e(TAG, "Unknown Exception received", e);
        } finally {
            stop(isSendBroadcast);
        }
    }

    private synchronized boolean isStopped() {
        return this.mIsStopped;
    }

    public synchronized void stop(boolean sendBroadcast) {
        this.mIsStopped = true;
        try {
            if (this.mDataIn != null) {
                this.mDataIn.close();
            }
        } catch (IOException e) {
        }

        try {
            if (this.mDataOut != null) {
                this.mDataOut.close();
            }
        } catch (IOException e) {
        }

        this.mDataIn = null;
        this.mDataOut = null;

        Log.d(TAG, "End of WorkerRunnable...");
//        if (mWorkerRunnableHandler != null) {
//            mWorkerRunnableHandler.getLooper().quit();
//        }
    }

    // 핸들러 객체 만들기
    private static class WorkerRunnableHandler extends Handler {
        private final WeakReference<WorkerRunnable> mWorkerRunnableRef;

        public WorkerRunnableHandler(Looper looper, WorkerRunnable runnable) {
            super(looper);
            mWorkerRunnableRef = new WeakReference<>(runnable);
        }

        @Override
        public void handleMessage(Message msg) {
            WorkerRunnable runnable = mWorkerRunnableRef.get();
            if (runnable != null) {
                runnable.handleMessage(msg);
            }
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case IoTServiceCommand.IOT_GATEWAY_DISCONNECTED: {
                // TODO : test
                Log.d(TAG, "IoTServiceCommand.IOT_GATEWAY_DISCONNECTED received...");
            }
        }
    }

}
