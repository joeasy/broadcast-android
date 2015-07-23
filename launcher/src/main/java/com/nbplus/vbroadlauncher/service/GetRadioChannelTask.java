package com.nbplus.vbroadlauncher.service;

import android.net.Uri;
import android.os.Message;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.nbplus.vbroadlauncher.data.BaseApiResult;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.RadioChannelInfo;

import org.basdroid.volley.GsonRequest;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

/**
 * Created by basagee on 2015. 6. 2..
 */
public class GetRadioChannelTask extends BaseServerApiAsyncTask {

    @Override
    protected BaseApiResult doInBackground(Void... voids) {
        RequestQueue requestQueue = Volley.newRequestQueue(mContext);
        RadioChannelInfo response = null;

        Uri.Builder builder = Uri.parse(mServerPath).buildUpon();
        builder.appendQueryParameter("DEVICE_ID", LauncherSettings.getInstance(mContext).getDeviceID());
        String url = builder.toString();

//        int retryCount = 0;
//        while (retryCount < 3) {        // retry 3 times
            RequestFuture<RadioChannelInfo> future = RequestFuture.newFuture();

            GsonRequest request = new GsonRequest(Request.Method.GET, url, null, RadioChannelInfo.class, future, future);
            request.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 3, 1.0f));
            requestQueue.add(request);

            try {
                response = future.get(); // this will block (forever)
                Thread.sleep(1000);
//                break;
            } catch (InterruptedException e) {
                // exception handling
                e.printStackTrace();
            } catch (ExecutionException e) {
                // exception handling
                e.printStackTrace();
            }
//            retryCount++;
//        }
        return (BaseApiResult)response;
    }

    @Override
    protected void onPostExecute(BaseApiResult result) {

        // TODO : sample data
//        ArrayList<RadioChannelInfo.RadioChannel> items = new ArrayList<>();
//        RadioChannelInfo.RadioChannel item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "KBS 1FM(클래식FM)";
//        item.channelUrl = "rtsp://kbs-radio.gscdn.com/tunein_1fm/_definst_/tunein_1fm.stream";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "KBS 1라디오(해피FM)";
//        item.channelUrl = "rtsp://kbs-radio.gscdn.com/tunein_1fm/_definst_/tunein_1radio.stream";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "KBS 2라디오(해피FM)";
//        item.channelUrl = "rtsp://kbs-radio.gscdn.com/tunein_1fm/_definst_/tunein_2radio.stream";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "KBS 2FM(쿨FM)";
//        item.channelUrl = "rtsp://kbs-radio.gscdn.com/tunein_2fm/_definst_/tunein_2fm.stream";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC 표준FM";
//        item.channelUrl = "mms://210.105.237.100/mbcam";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U";
//        item.channelUrl = "mms://210.105.237.100/mbcfm";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "SBS 파워FM";
//        item.channelUrl = "mms://114.108.140.39/magicfm_live";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "YTN1";
//        item.channelUrl = "rtmp://183.111.25.94:1935/live/_definst_/fmlive_0624_1.sdp";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "TBS 교통FM";
//        item.channelUrl = "mms://115.84.165.160/fmlive";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "TBN";
//        item.channelUrl = "mms://210.96.79.102/Junju";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM";
//        item.channelUrl = "mms://112.173.165.6/radio";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 음악FM";
//        item.channelUrl = "mms://media.biointernet.com/fm1021";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "극동방송 FM";
//        item.channelUrl = "mms://live.febc.net/LiveFm";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "BBS 부산";
//        item.channelUrl = "mms://218.146.253.176/bbsradiolive";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "WBS 서울";
//        item.channelUrl = "mms://aod.wbsi.kr/wbs897";
//        items.add(item);

//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "YTN2";
//        item.channelUrl = "rtmp://112.175.248.34:1935/live/_definst_/fmlive_0624_1.sdp";
//        items.add(item);


//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC 표준FM 춘천";
//        item.channelUrl = "mms://222.113.53.4/chmbcam";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC 표준FM 청주";
//        item.channelUrl = "mms://211.181.136.136/liveam";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC 표준FM 제주";
//        item.channelUrl = "mms://stream.jejumbc.com/Live_am";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC 표준FM 울산";
//        item.channelUrl = "mmms://onair.usmbc.co.kr/kkangam";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC 표준FM 강릉";
//        item.channelUrl = "mms://118.46.233.19/ams";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC 표준FM 목포";
//        item.channelUrl = "mms://vod.mokpombc.co.kr/encoder-am";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC 표준FM 여수";
//        item.channelUrl = "mms://vod.ysmbc.co.kr/YSAM";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC 표준FM 원주";
//        item.channelUrl = "mms://live.wjmbc.co.kr/fm2";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC 표준FM 충주";
//        item.channelUrl = "mms://VODSTR.cjmbc.co.kr/STFM";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC 표준FM 삼척";
//        item.channelUrl = "mms://121.189.151.7/sfm";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 부산";
//        item.channelUrl = "mms://58.231.196.73/busanmbc-fm-onair-20120228";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 대구";
//        item.channelUrl = "mms://vod1.dgmbc.com/fmlive";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 광주";
//        item.channelUrl = "mms://211.117.193.99/gjmbcfmlive";
//        items.add(item);
//
//
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 춘천";
//        item.channelUrl = "mms://222.113.53.4/chmbcfm";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 청주";
//        item.channelUrl = "mms://211.181.136.136/livefm";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 제주";
//        item.channelUrl = "mms://stream.jejumbc.com/Live_fm";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 울산";
//        item.channelUrl = "mms://onair.usmbc.co.kr/kkangfm";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 강릉";
//        item.channelUrl = "mms://118.46.233.19/fms";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 목포";
//        item.channelUrl = "mms://vod.mokpombc.co.kr/encoder-fm";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 여수";
//        item.channelUrl = "mms://vod.ysmbc.co.kr/YSFM";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 원주";
//        item.channelUrl = "mms://live.wjmbc.co.kr/fm989";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 충주";
//        item.channelUrl = "mms://VODSTR.cjmbc.co.kr/MUFM";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "MBC FM4U 삼척";
//        item.channelUrl = "mms://121.189.151.7/mfm";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "SBS 파워FM 광주";
//        item.channelUrl = "mms://211.224.129.152/kbc_fm";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "SBS 파워FM 울산";
//        item.channelUrl = "mms://218.146.252.66/Iive_FM";
//        items.add(item);
//
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "SBS 파워FM 청주";
//        item.channelUrl = "mms://211.224.129.152/joyfm_live";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "SBS 파워FM 제주";
//        item.channelUrl = "mms://121.254.230.3/FMLIVE";
//        items.add(item);
//
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "iFM 경인방송";
//        item.channelUrl = "mms://www.sunnyfm.co.kr/itv";
//        items.add(item);
//
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "TBS 영어FM";
//        item.channelUrl = "mms://115.84.165.160/efmlive";
//        items.add(item);
//
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "TBN 부산";
//        item.channelUrl = "mms://210.96.79.102/Pusan";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "TBN 광주";
//        item.channelUrl = "mms://210.96.79.102/Kwangju";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "TBN 대구";
//        item.channelUrl = "mms://210.96.79.102/Daegu";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "TBN 대전";
//        item.channelUrl = "mms://210.96.79.102/Daejun";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "TBN 인천";
//        item.channelUrl = "mms://210.96.79.102/Incheon";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "TBN 강원";
//        item.channelUrl = "mms://210.96.79.102/Wonju";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "TBN 전주";
//        item.channelUrl = "mms://210.96.79.102/Junju";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "TBN 울산";
//        item.channelUrl = "mms://210.96.79.102/Ulsan";
//        items.add(item);
//
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "Busan eFM";
//        item.channelUrl = "mms://115.68.15.116/efm";
//        items.add(item);
//
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 대구";
//        item.channelUrl = "mms://121.181.252.130:7080";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 부산";
//        item.channelUrl = "mms://media.biointernet.com/fm1029";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 광주";
//        item.channelUrl = "mms://112.173.165.6/radio";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 전북";
//        item.channelUrl = "mms://211.116.122.4:8080";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 청주";
//        item.channelUrl = "mms://222.116.93.208:7080";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 춘천";
//        item.channelUrl = "mms://220.83.166.120:1422";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 대전";
//        item.channelUrl = "mms://59.26.172.132:8080";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 포항";
//        item.channelUrl = "mms://59.24.49.132:4368";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 경남";
//        item.channelUrl = "http://121.146.93.239:9900";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 제주";
//        item.channelUrl = "mms://vod.cbs.co.kr/jjcbs";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 영동";
//        item.channelUrl = "mms://112.220.87.114:8080";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 전남";
//        item.channelUrl = "mms://222.233.223.196/jncbs_broadcast_audio";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 표준FM 울산";
//        item.channelUrl = "mms://121.176.177.218:8080";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 음악FM 서울";
//        item.channelUrl = "mms://gswidget.cbs.co.kr/cbs939";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "CBS 음악FM 부산";
//        item.channelUrl = "mms://media.biointernet.com/fm1021";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "극동방송 AM";
//        item.channelUrl = "mms://live.febc.net/LiveAm";
//        items.add(item);
//
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "극동방송 제주";
//        item.channelUrl = "mms://live.febc.net/jeju_febc";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "극동방송 창원";
//        item.channelUrl = "mms://live.febc.net/changwon_febc";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "극동방송 목포";
//        item.channelUrl = "mms://live.febc.net/mokpo_febc";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "극동방송 영동";
//        item.channelUrl = "mms://live.febc.net/yeongdong_febc";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "극동방송 포항";
//        item.channelUrl = "mms://live.febc.net/pohang_febc";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "극동방송 울산";
//        item.channelUrl = "mms://live.febc.net/ulsan_febc";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "극동방송 광주";
//        item.channelUrl = "mms://livegj.febc.net/ipradio";
//        items.add(item);
//
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "BBS 대구";
//        item.channelUrl = "mms://bbs5114.codns.com/dgbbslive";
//        items.add(item);
//
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "PBC 부산";
//        item.channelUrl = "mms://218.146.255.6/Busan PBC Live";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "PBC 대전";
//        item.channelUrl = "mms://junsan52.cafe24.com/junsan52_live";
//        items.add(item);
//
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "WBS 전북";
//        item.channelUrl = "mms://aod.wbsi.kr/wbs979";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "WBS 부산";
//        item.channelUrl = "mms://aod.wbsi.kr/wbs1049";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "WBS 광주";
//        item.channelUrl = "mms://aod.wbsi.kr/wbs1079";
//        items.add(item);
//
//        item = new RadioChannelInfo.RadioChannel();
//        item.channelName = "WBS 대구";
//        item.channelUrl = "mms://aod.wbsi.kr/wbs983";
//        items.add(item);
//
//        ((RadioChannelInfo)result).setResultCode("0000");
//        ((RadioChannelInfo)result).setRadioChannelList(items);

        // end of TODO : sample data

        // TODO ;; remove later....

        if (mHandler != null) {
            Message message = new Message();
            message.what = Constants.HANDLER_MESSAGE_GET_RADIO_CHANNEL_TASK;
            message.obj = result;
            mHandler.sendMessage(message);
        }
    }

}
