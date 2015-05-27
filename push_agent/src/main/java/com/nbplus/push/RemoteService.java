package com.nbplus.push;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class RemoteService extends Service {
    private static final String TAG = OnBootReceiver.class.getName();

    public RemoteService() {
    }

    /** Command to the service to register client binder */
    static final int MSG_REGISTER = 1;
    /** Command to the service to display a message */
    static final int MSG_SAY_HELLO = 2;
    static final int MSG_SAY_HELLO2 = 3;
    static final int MSG_SAY_HELLO3 = 4;

    /**
     * Handler of incoming messages from clients.
     *
     * 아래 사항은 참고하고 있어야 한다. 나중에 서비스 기획이 바뀌는 경우 IPC 수정이 필요하면 아래 내용을 참고해서
     * 적절하게 적용을 해야 한다.
     *
     * In Android there are multiple ways to do IPC, such as Intents (check IntentService),
     * Binders (AIDL or Messenger) and ASHMEM (anonymous shared memory).
     * I selected the Messenger for this example since it transparently queues all requests
     * into a single thread assuring thread-safeness.
     * If you need a multi-threaded service then you should consider the use of AIDL to define
     * your interface (check Android bound services  – implementation example here).
     */
    class ApplicationMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Intent broadcast;
            switch (msg.what) {
                case MSG_REGISTER:
                /*
                 * Do whatever we want with the client messenger: Messenger
                 * clientMessenger = msg.replyTo
                 */
                    Toast.makeText(getApplicationContext(),
                            "Service : received client Messenger!",
                            Toast.LENGTH_SHORT).show();
                    break;
                case MSG_SAY_HELLO:
                    Log.d("XXX", "send broadcast to app");
                    broadcast = new Intent("com.sg.droid.pushservice.intent.RECEIVE");
                    broadcast.addCategory("com.sg.droid.pushdemo1");
                    broadcast.putExtra("extra", "test");
                    sendBroadcast(broadcast);
                    break;
                case MSG_SAY_HELLO2:
                    Log.d("XXX", "send broadcast to app");
                    broadcast = new Intent("com.sg.droid.pushservice.intent.RECEIVE");
                    broadcast.putExtra("extra", "test");
                    broadcast.addCategory("com.sg.droid.pushdemo2");
                    sendBroadcast(broadcast);
                    break;
                case MSG_SAY_HELLO3:
                    Log.d("XXX", "send broadcast to app");
                    broadcast = new Intent("com.sg.droid.pushservice.intent.RECEIVE");
                    broadcast.putExtra("extra", "test");
                    sendBroadcast(broadcast);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.Note
     * that calls to its binder are sequential!
     */
    final Messenger mServiceMessenger = new Messenger(new ApplicationMessageHandler());

    /**
     * Called by the system every time a client explicitly starts the service by calling
     * {@link android.content.Context#startService}, providing the arguments it supplied and a
     * unique integer token representing the start request.  Do not call this method directly.
     * <p/>
     * <p>For backwards compatibility, the default implementation calls
     * {@link #onStart} and returns either {@link #START_STICKY}
     * or {@link #START_STICKY_COMPATIBILITY}.
     * <p/>
     * <p>If you need your application to run on platform versions prior to API
     * level 5, you can use the following model to handle the older {@link #onStart}
     * callback in that case.  The <code>handleCommand</code> method is implemented by
     * you as appropriate:
     * <p/>
     * {@sample development/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.java
     * start_compatibility}
     * <p/>
     * <p class="caution">Note that the system calls this on your
     * service's main thread.  A service's main thread is the same
     * thread where UI operations take place for Activities running in the
     * same process.  You should always avoid stalling the main
     * thread's event loop.  When doing long-running operations,
     * network calls, or heavy disk I/O, you should kick off a new
     * thread, or use {@link android.os.AsyncTask}.</p>
     *
     * @param intent  The Intent supplied to {@link android.content.Context#startService},
     *                as given.  This may be null if the service is being restarted after
     *                its process has gone away, and it had previously returned anything
     *                except {@link #START_STICKY_COMPATIBILITY}.
     * @param flags   Additional data about this start request.  Currently either
     *                0, {@link #START_FLAG_REDELIVERY}, or {@link #START_FLAG_RETRY}.
     * @param startId A unique integer representing this specific request to
     *                start.  Use with {@link #stopSelfResult(int)}.
     * @return The return value indicates what semantics the system should
     * use for the service's current started state.  It may be one of the
     * constants associated with the {@link #START_CONTINUATION_MASK} bits.
     * @see #stopSelfResult(int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand in service ...");
        /**
         * 항상 실행되도록 한다.
         */
        return Service.START_STICKY;//super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding to service ...");
        return mServiceMessenger.getBinder();
    }

    /**
     * TODO : 사용해야 할 것인지는 아직 결정되지 않음.
     * 브로드캐스트 리시버가 등록되어 있는지 확인이 필요한 경우.
     */
    private void checkRegisteredReceiver() {
        PackageManager packageManager = getPackageManager();
        List<String> startupApps = new ArrayList<String>();
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        List<ResolveInfo> activities = packageManager.queryBroadcastReceivers(intent, 0);
        for (ResolveInfo resolveInfo : activities) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null) {
                startupApps.add(activityInfo.name);
            }
        }
    }
}
