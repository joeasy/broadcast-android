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

package com.nbplus.push;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.nbplus.push.data.PushBaseData;
import com.nbplus.push.data.PushConstants;
import com.nbplus.push.data.PushInterfaceData;
import com.nbplus.push.data.PushMessageData;

import org.basdroid.common.NetworkUtils;
import org.basdroid.common.StringUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by basagee on 2015. 6. 30..
 */
public class TcpClient {
    private static final String TAG = TcpClient.class.getSimpleName();

    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private DataOutputStream mDataOut;
    // used to read messages from the server
    private DataInputStream mDataIn;

    PushInterfaceData mInterfaceData;
//    Context mContext;

    private static final int HANDLER_MESSAGE_CHECK_KEEP_ALIVE = 1;
    private static final int HANDLER_MESSAGE_WAIT_PUSH_GW_CONNECTION = 2;
    private static final int HANDLER_MESSAGE_WAIT_KEEP_ALIVE_RESPONSE = 3;
    private TcpClientHandler mHandler;
    int mKeepAliveCheckSeconds = 0;

    int mRequestMessageId = 0;
    int mConnectionRequestId = -1;
    int mKeepAliveRequestId = -1;

    Socket mPushSocket;

    // 핸들러 객체 만들기
    private static class TcpClientHandler extends Handler {
        private final WeakReference<TcpClient> mService;

        public TcpClientHandler(Looper looper, TcpClient service) {
            super(looper);
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            TcpClient service = mService.get();
            if (service != null) {
                service.handleMessage(msg);
            }
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case HANDLER_MESSAGE_CHECK_KEEP_ALIVE :
                sendMessage(getRequestMessage(PushConstants.PUSH_MESSAGE_TYPE_KEEP_ALIVE_REQUEST));
                break;
            case HANDLER_MESSAGE_WAIT_PUSH_GW_CONNECTION :
                Log.e(TAG, "HANDLER_MESSAGE_WAIT_PUSH_GW_CONNECTION expired.. Server did not send response");
                stopClient();
                if (mMessageListener != null) {
                    mMessageListener.onConnectionError();
                }
                break;
            case HANDLER_MESSAGE_WAIT_KEEP_ALIVE_RESPONSE :
                Log.e(TAG, "HANDLER_MESSAGE_WAIT_KEEP_ALIVE_RESPONSE expired.. Server did not send response");
                stopClient();
                if (mMessageListener != null) {
                    mMessageListener.onConnectionError();
                }
                break;
        }
    }

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(/*Context context, */PushInterfaceData data, OnMessageReceived listener) {
        mMessageListener = listener;
//        mContext = context;
        this.mInterfaceData = data;
    }

    public void setInterfaceServer(PushInterfaceData data) {
        this.mInterfaceData = data;
    }

    public byte[] getRequestMessage(final char messageType) {
        return getRequestMessage(messageType, -1, -1);
    }

    /**
     * messageId, correlator는 PUSH_MESSAGE_TYPE_PUSH_RESPONSE / PUSH_MESSAGE_TYPE_PUSH_RESPONSE 에서만 유효하다.
     *
     * @param messageType
     * @param messageId
     * @param correlator
     * @return
     */
    public byte[] getRequestMessage(final char messageType, int messageId, int correlator) {
        byte[] bytes = null;
        ByteBuffer byteBuffer = null;

        boolean invalidType = false;
        switch (messageType) {
            case PushConstants.PUSH_MESSAGE_TYPE_CONNECTION_REQUEST :
                Log.d(TAG, "get message = " + PushConstants.PUSH_MESSAGE_TYPE_CONNECTION_REQUEST);
                byteBuffer = ByteBuffer.allocate(29);        // 인터페이스설계서..
                byteBuffer.put((byte)messageType);
                byteBuffer.putInt(mRequestMessageId);
                byteBuffer.putInt(20);
                if (!StringUtils.isEmptyString(mInterfaceData.sessionKey)) {
                    byteBuffer.put(mInterfaceData.sessionKey.getBytes(), 0, mInterfaceData.sessionKey.length());
                }
                byte[] fillZero;
                int length = (StringUtils.isEmptyString(mInterfaceData.sessionKey)) ? 0 : mInterfaceData.sessionKey.length();
                if (length < 20) {
                    fillZero = new byte[20 - length];
                    Arrays.fill(fillZero, (byte)0);
                    byteBuffer.put(fillZero, 0, 20 - length);
                }
                mConnectionRequestId = mRequestMessageId;
                if (mRequestMessageId < Integer.MAX_VALUE) {
                    mRequestMessageId++;
                } else {
                    mRequestMessageId = 0;
                }

                break;
            case PushConstants.PUSH_MESSAGE_TYPE_KEEP_ALIVE_REQUEST :
                byteBuffer = ByteBuffer.allocate(13);        // 인터페이스설계서..
                byteBuffer.put((byte) messageType);
                byteBuffer.putInt(mRequestMessageId);
                byteBuffer.putInt(4);
                byteBuffer.putInt(mRequestMessageId);
                mKeepAliveRequestId = mRequestMessageId;
                if (mRequestMessageId < Integer.MAX_VALUE) {
                    mRequestMessageId++;
                } else {
                    mRequestMessageId = 0;
                }
                break;
            case PushConstants.PUSH_MESSAGE_TYPE_PUSH_RESPONSE:
                byteBuffer = ByteBuffer.allocate(17);        // 인터페이스설계서..
                byteBuffer.put((byte) messageType);
                byteBuffer.putInt(messageId);
                byteBuffer.putInt(8);
                byteBuffer.put(PushConstants.RESULT_OK.getBytes(), 0, 4);
                byteBuffer.putInt(correlator);
                break;
            case PushConstants.PUSH_MESSAGE_TYPE_KEEP_ALIVE_CHANGE_RESPONSE:
                byteBuffer = ByteBuffer.allocate(13);        // 인터페이스설계서..
                byteBuffer.put((byte) messageType);
                byteBuffer.putInt(messageId);
                byteBuffer.putInt(4);
                byteBuffer.put(PushConstants.RESULT_OK.getBytes(), 0, 4);
                break;
            case PushConstants.PUSH_MESSAGE_TYPE_APP_UPDATE_RESPONSE:
                byteBuffer = ByteBuffer.allocate(13);        // 인터페이스설계서..
                byteBuffer.put((byte) messageType);
                byteBuffer.putInt(messageId);
                byteBuffer.putInt(4);
                byteBuffer.put(PushConstants.RESULT_OK.getBytes(), 0, 4);
                break;
            default:
                invalidType = true;
                break;
        }

        if (!invalidType) {
            bytes = (byteBuffer != null) ? byteBuffer.array() : null;
        }
        return bytes;
    }
    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(byte[] message) {
        if (message == null || message.length <= 0) {
            message = null;
            return;
        }

        if (mDataOut != null) {
            try {
                mDataOut.write(message);
                mDataOut.flush();
                if (mHandler != null && message[0] == PushConstants.PUSH_MESSAGE_TYPE_CONNECTION_REQUEST) {
                    mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_WAIT_PUSH_GW_CONNECTION, 60 * 1000);
                } else if (mHandler != null && message[0] == PushConstants.PUSH_MESSAGE_TYPE_KEEP_ALIVE_REQUEST) {
                    mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_WAIT_KEEP_ALIVE_RESPONSE, 60 * 1000);
                    Log.d(TAG, ">>> send keep-alive.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        message = null;
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {
        Log.i(TAG, "stopClient");

        // send mesage that we are closing the connection
        //sendMessage(Constants.CLOSED_CONNECTION + "Kazy");
        mRun = false;
        if (mPushSocket != null && mPushSocket.isConnected()) {
            try {
                mPushSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mPushSocket = null;

        if (mDataOut != null) {
            try {
                mDataOut.flush();
                mDataOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        mMessageListener = null;
        if (mDataIn != null) {
            try {
                mDataIn.close();
            } catch (IOException e) {
                // do nothing.
                e.printStackTrace();
            }
        }
        mDataIn = null;
        mDataOut = null;

        if (mHandler != null) {
            mHandler.removeMessages(HANDLER_MESSAGE_CHECK_KEEP_ALIVE);
            mHandler.removeMessages(HANDLER_MESSAGE_WAIT_PUSH_GW_CONNECTION);
            mHandler.removeMessages(HANDLER_MESSAGE_WAIT_KEEP_ALIVE_RESPONSE);
            mHandler = null;
        }
        mInterfaceData = null;
    }

    private void setKeepAliveHandler() {
        mHandler.removeMessages(HANDLER_MESSAGE_CHECK_KEEP_ALIVE);
        // set keep alive
        if (mInterfaceData != null && !StringUtils.isEmptyString(mInterfaceData.keepAliveSeconds)) {
            try {
                mKeepAliveCheckSeconds = Integer.parseInt(mInterfaceData.keepAliveSeconds);
                Log.d(TAG, ">> Keep-alive term seconds = " + mKeepAliveCheckSeconds);
                if (mKeepAliveCheckSeconds > 0) {
                    /**
                     * 8월 19일.
                     * 단말상태가 ANR 일수도 있고.. 어떻게될지모르니..
                     * 30분이상 이면 1분 줄이자. //1시간이내라면 1분줄이고 1시간이상이면 2분줄이자.
                     */
                    if (mKeepAliveCheckSeconds >= (60 * 30)/* && mKeepAliveCheckSeconds < (60 * 60)*/) {
                        mKeepAliveCheckSeconds -= 60 * 1;
                    } /*else if (mKeepAliveCheckSeconds >= (60 * 60)) {
                        mKeepAliveCheckSeconds -= 60 * 2;
                    }*/
                    mHandler.sendEmptyMessageDelayed(HANDLER_MESSAGE_CHECK_KEEP_ALIVE, mKeepAliveCheckSeconds * 1000);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                mKeepAliveCheckSeconds = 0;
            }
        } else {
            Log.e(TAG, ">> Can't find keep alive information");
        }
    }

    public void run() {
        mRun = true;
        try {
            //here you must put your computer's IP address.
            if (mInterfaceData == null || StringUtils.isEmptyString(mInterfaceData.interfaceServerAddress) || StringUtils.isEmptyString(mInterfaceData.interfaceServerPort)) {
                if (mMessageListener != null) {
                    //call the method messageReceived from MyActivity class
                    mMessageListener.onConnectionError();
                }
                return;
            }


            final InetAddress serverAddr = InetAddress.getByName(mInterfaceData.interfaceServerAddress);
            int port = Integer.parseInt(mInterfaceData.interfaceServerPort);
            final SocketAddress socketAddress = new InetSocketAddress(serverAddr, port);
            Log.d(TAG, "C: Connecting... serverAddr = " + serverAddr + ", port = " + port);

            if (mPushSocket == null) {
                //create a socket to make the connection with the server
                mPushSocket = new Socket();
                mPushSocket.setReuseAddress(true);
                mPushSocket.setKeepAlive(true);
            }
            mPushSocket.connect(socketAddress);

            try {
                Log.i(TAG, "inside try catch");
                //sends the message to the server
                mDataOut = new DataOutputStream(mPushSocket.getOutputStream());

                //receives the message which the server sends back
                mDataIn = new DataInputStream(mPushSocket.getInputStream());
                byte[] messageBytes;

                mHandler = new TcpClientHandler(Looper.getMainLooper(), this);
                sendMessage(getRequestMessage(PushConstants.PUSH_MESSAGE_TYPE_CONNECTION_REQUEST));

                while (mRun) {
                    // first 1byte - message type
                    PushBaseData receivedData = null;
                    int readBytes = 0;
                    int msgId = -1;
                    int bodyLen = 0;
                    boolean isErrorOccurred = false;

                    messageBytes = null;
                    int messageType = mDataIn.readByte();
                    switch (messageType) {
                        case PushConstants.PUSH_MESSAGE_TYPE_CONNECTION_RESPONSE :
                            Log.d(TAG, "PushConstants.PUSH_MESSAGE_TYPE_CONNECTION_RESPONSE received !!");
                            mHandler.removeMessages(HANDLER_MESSAGE_WAIT_PUSH_GW_CONNECTION);

                            msgId = mDataIn.readInt();
                            messageBytes = new byte[4];
                            bodyLen = mDataIn.read(messageBytes, 0, 4);           // skip body length.
                            messageBytes = new byte[4];
                            mDataIn.read(messageBytes, 0, 4);
                            Log.d(TAG, "Received bodyLen = " + bodyLen + ", result code  = " + new String(messageBytes));
                            if (msgId == mConnectionRequestId && PushConstants.RESULT_OK.equals(new String(messageBytes))) {
                                messageBytes = new byte[20];
                                mDataIn.read(messageBytes, 0, 20);
                                String receivedAuthKey = null;
                                int i;
                                for (i = 0; i < messageBytes.length && messageBytes[i] != 0; i++) { }
                                if (i == 0) {
                                    receivedAuthKey = "";
                                } else {
                                    receivedAuthKey = new String(messageBytes, 0, i, "utf-8");
                                }
                                if (mInterfaceData.deviceAuthKey != null && mInterfaceData.deviceAuthKey.equals(receivedAuthKey)) {
                                    // 서버요청에 따라 연결후 바로 keep-alive
                                    sendMessage(getRequestMessage(PushConstants.PUSH_MESSAGE_TYPE_KEEP_ALIVE_REQUEST));
                                    if (mMessageListener != null) {
                                        //call the method messageReceived from MyActivity class
                                        mMessageListener.onConnected();
                                    }
                                } else {
                                    Log.e(TAG, ">> Device auth key is not matched.. ");
                                    isErrorOccurred = true;
                                }
                            } else {
                                Log.d(TAG, ">> Push connection failed !!!!");
                                //call the method messageReceived from MyActivity class
                                isErrorOccurred = true;
                            }
                            break;
                        case PushConstants.PUSH_MESSAGE_TYPE_KEEP_ALIVE_CHANGE_REQUEST :
                            Log.d(TAG, "PushConstants.PUSH_MESSAGE_TYPE_KEEP_ALIVE_CHANGE_REQUEST received !!");
                            msgId = mDataIn.readInt();
                            bodyLen = mDataIn.readInt();
                            if (bodyLen > 0) {
                                messageBytes = new byte[bodyLen];
                                mDataIn.read(messageBytes, 0, bodyLen);
                                int i;
                                for (i = 0; i < messageBytes.length && messageBytes[i] != 0; i++) { }
                                if (i == 0) {
                                    mInterfaceData.keepAliveSeconds = "30";     // defaults
                                } else {
                                    mInterfaceData.keepAliveSeconds = new String(messageBytes, 0, i, "utf-8");
                                }
                                mHandler.removeMessages(HANDLER_MESSAGE_WAIT_KEEP_ALIVE_RESPONSE);
                                sendMessage(getRequestMessage(PushConstants.PUSH_MESSAGE_TYPE_KEEP_ALIVE_REQUEST));
                                setKeepAliveHandler();
                            }
                            sendMessage(getRequestMessage(PushConstants.PUSH_MESSAGE_TYPE_KEEP_ALIVE_CHANGE_RESPONSE, msgId, -1));
                            break;
                        case PushConstants.PUSH_MESSAGE_TYPE_PUSH_REQUEST :
                            receivedData = new PushMessageData();
                            Log.d(TAG, "PushConstants.PUSH_MESSAGE_TYPE_PUSH_REQUEST received !!");
                            // read message id
                            receivedData.setMessageId(mDataIn.readInt());
                            receivedData.setBodyLength(mDataIn.readInt());
                            ((PushMessageData)receivedData).setCorrelator(mDataIn.readInt());
                            // app id
                            messageBytes = new byte[PushBaseData.MAX_APP_ID_LENGTH];
                            Arrays.fill(messageBytes, (byte) 0);
                            readBytes = mDataIn.read(messageBytes, 0, PushBaseData.MAX_APP_ID_LENGTH);
                            if (readBytes > 0) {
                                int i;
                                for (i = 0; i < messageBytes.length && messageBytes[i] != 0; i++) { }
                                if (i == 0) {
                                    ((PushMessageData) receivedData).setAppId("");
                                } else {
                                    String str = new String(messageBytes, 0, i, "utf-8");
                                    ((PushMessageData) receivedData).setAppId(str);
                                }
                            } else {
                                ((PushMessageData) receivedData).setAppId("");
                            }
                            // repeat key
                            messageBytes = new byte[PushBaseData.MAX_REPEAT_KEY_LENGTH];
                            Arrays.fill(messageBytes, (byte) 0);
                            readBytes = mDataIn.read(messageBytes, 0, PushBaseData.MAX_REPEAT_KEY_LENGTH);
                            if (readBytes > 0) {
                                int i;
                                for (i = 0; i < messageBytes.length && messageBytes[i] != 0; i++) { }
                                if (i == 0) {
                                    ((PushMessageData) receivedData).setRepeatKey("");
                                } else {
                                    String str = new String(messageBytes, 0, i, "utf-8");
                                    ((PushMessageData) receivedData).setRepeatKey(str);
                                }
                            } else {
                                ((PushMessageData) receivedData).setRepeatKey("");
                            }
                            // alert
                            messageBytes = new byte[PushBaseData.MAX_ALERT_LENGTH];
                            Arrays.fill(messageBytes, (byte) 0);
                            readBytes = mDataIn.read(messageBytes, 0, PushBaseData.MAX_ALERT_LENGTH);
                            if (readBytes > 0) {
                                int i;
                                for (i = 0; i < messageBytes.length && messageBytes[i] != 0; i++) { }
                                if (i == 0) {
                                    ((PushMessageData) receivedData).setAlert("");
                                } else {
                                    String str = new String(messageBytes, 0, i, "utf-8");
                                    ((PushMessageData) receivedData).setAlert(str);
                                }
                            } else {
                                ((PushMessageData) receivedData).setAlert("");
                            }
                            // payload
                            // correlator 4byte
                            int excludePayloadLen = PushBaseData.MAX_REPEAT_KEY_LENGTH + PushBaseData.MAX_ALERT_LENGTH + PushBaseData.MAX_APP_ID_LENGTH + 4;
                            if (receivedData.getBodyLength() - excludePayloadLen > 0) {
                                messageBytes = new byte[receivedData.getBodyLength() - excludePayloadLen];
                                Arrays.fill(messageBytes, (byte) 0);
                                readBytes = mDataIn.read(messageBytes, 0, receivedData.getBodyLength() - excludePayloadLen);
                                if (readBytes > 0) {
                                    ((PushMessageData) receivedData).setPayload(new String(messageBytes, 0, receivedData.getBodyLength() - excludePayloadLen, "utf-8"));
                                } else {
                                    ((PushMessageData) receivedData).setPayload("");
                                }
                            } else {
                                ((PushMessageData) receivedData).setPayload("");
                            }

                            sendMessage(getRequestMessage(PushConstants.PUSH_MESSAGE_TYPE_PUSH_RESPONSE, receivedData.getMessageId(), ((PushMessageData) receivedData).getCorrelator()));

                            if (receivedData != null && !StringUtils.isEmptyString(((PushMessageData) receivedData).getPayload()) && mMessageListener != null) {
                                //call the method messageReceived from MyActivity class
                                mMessageListener.messageReceived(receivedData);
                            }
                            break;
                        case PushConstants.PUSH_MESSAGE_TYPE_APP_UPDATE_REQUEST :
                            Log.d(TAG, "PushConstants.PUSH_MESSAGE_TYPE_APP_UPDATE_REQUEST received !! do nothing.. ");
                            msgId = mDataIn.readInt();
                            mDataIn.skipBytes(204);
                            sendMessage(getRequestMessage(PushConstants.PUSH_MESSAGE_TYPE_PUSH_RESPONSE, msgId, -1));
                            break;
                        case PushConstants.PUSH_MESSAGE_TYPE_PUSH_AGENT_UPDATE_REQUEST :
                            Log.d(TAG, "PushConstants.PUSH_MESSAGE_TYPE_PUSH_AGENT_UPDATE_REQUEST received !! do nothing.. ");
                            msgId = mDataIn.readInt();
                            mDataIn.skipBytes(204);
                            sendMessage(getRequestMessage(PushConstants.PUSH_MESSAGE_TYPE_PUSH_RESPONSE, msgId, -1));
                            break;
                        case PushConstants.PUSH_MESSAGE_TYPE_KEEP_ALIVE_RESPONSE :
                            Log.d(TAG, ">>> PUSH_MESSAGE_TYPE_KEEP_ALIVE_RESPONSE.");
                            mHandler.removeMessages(HANDLER_MESSAGE_WAIT_KEEP_ALIVE_RESPONSE);
                            mDataIn.skipBytes(8);
                            messageBytes = new byte[4];
                            readBytes = mDataIn.read(messageBytes, 0, 4);
                            /**
                             * correlator... skip..
                             */
                            int correlator = mDataIn.readInt();
                            if (readBytes > 0) {
                                int i;
                                for (i = 0; i < messageBytes.length && messageBytes[i] != 0; i++) { }
                                if (i == 0) {
                                    Log.d(TAG, "Error... keep-alive body read, but 0 len... reconnection!!");
                                    isErrorOccurred = true;
                                } else {
                                    String str = new String(messageBytes, 0, i, "utf-8");
                                    Log.d(TAG, "response code = " + str);
                                    if (!StringUtils.isEmptyString(str) && PushConstants.RESULT_OK.equals(str)) {
                                        if (correlator == mRequestMessageId - 1) {
                                            Log.d(TAG, "Set next keep-alive handler !!!");
                                            setKeepAliveHandler();
                                        }
                                    } else {
                                        isErrorOccurred = true;
                                    }
                                }

                            } else {
                                Log.d(TAG, "Error... keep-alive body read failed.. reconnection!!");
                                isErrorOccurred = true;
                            }
                            break;
                        default:
                            Log.d(TAG, "UNKNOWN PUSH MESSAGE TYPE received !!");
                            break;
                    }

                    if (messageType == -1) {
                        // maybe closed connection..
                        Log.e(TAG, "TCP client read EOF !!!!");
                        if (mMessageListener != null) {
                            //call the method messageReceived from MyActivity class
                            mMessageListener.connectionClosed();
                        }
                        break;
                    }
                    if (isErrorOccurred) {
                        if (mMessageListener != null) {
                            //call the method messageReceived from MyActivity class
                            mMessageListener.onConnectionError();
                        }
                        break;
                    }
                }
                Log.e(TAG, "TCP connection closed !!!");
            } catch (EOFException e) {
                Log.e(TAG, "EOFException received", e);
                // maybe closed connection..
                if (mMessageListener != null) {
                    //call the method messageReceived from MyActivity class
                    mMessageListener.connectionClosed();
                }
            } catch (Exception e) {
                Log.e(TAG, "S: Error", e);
                if (mMessageListener != null) {
                    //call the method messageReceived from MyActivity class
                    mMessageListener.connectionClosed();
                }
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                if (mPushSocket != null) {
                    mPushSocket.close();
                }
//                mPushSocket = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "C: Error", e);
            if (mMessageListener != null) {
                //call the method messageReceived from MyActivity class
                mMessageListener.onConnectionError();
            }
        }
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(PushBaseData data);
        public void connectionClosed();
        public void onConnectionError();
        public void onConnected();
    }
}
