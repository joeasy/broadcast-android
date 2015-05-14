># 마을방송 전용단말 어플리케이션

## Overview
마을방송 서비스는 마을단위 또는 지역단위의 마을 방송을 위한 단말 서비스이다.
마을방송 전용 단말기를 통하여 마을방송 서비스, 인터넷 라디오, IoT Gateway와 연동 서비스 제공 등을 목표로 한다.

## 마을방송 전용단말 설치 및 설정 
마을방송 전용단말에 대해 필요한 어플리케이션을 설치하고 사용자 환경설정을 수행한다. <br>
설치 후 Phase 4.의 과정까지 완료되면 어플리케이션을 삭제한다. 

<center>![마을방송 단말 SW 구성](images/0_전용단말_설치.png)</center>

> <span style='color:navy'>**위의 그림에서 phase 과정은 웹뷰로 구현**  
> > 필요한 인터페이스 - 상세는 [Native-웹뷰 간 인터페이스 정리](Native_webview_interface.html)   
> * **Javascript to Native**  
> &nbsp;&nbsp;&nbsp;&nbsp;- getDeviceId : mac address를 이용하여 생성한 Device ID  
> &nbsp;&nbsp;&nbsp;&nbsp;- setRegistration : 서버에서 받은 register 결과 전달   
> &nbsp;&nbsp;&nbsp;&nbsp;- startApplication : 런처 실행  
> * **Native to Javascript**  
> &nbsp;&nbsp;&nbsp;&nbsp;- applicationInstalled : 마을방송 어플리케이션 설치 완료 시 전달 

## 단말 종류 및 서비스 어플리케이션  
### 1. 단말 종류에 따른 서비스 어플리케이션 

|단말 형태 | 주요 기능 | 어플리케이션 타입 | UI/UX 실행 | 
|:----:|----|----|:----:|
| PC | 마을방송 수신 | Web Application | <span style='color:green'>Web Browser</span>
| - | 마을방송 설정 및 문자방송 등 관리자 | Web Application | <span style='color:green'>Web Browser</span>
| 모바일<br>(Smart phone) | 마을방송 수신 | <li>Icon + Web Application<br><li>Native는 실행아이콘과 브라우저 실행 기능만 제공 | <span style='color:green'>Web Browser</span>
| - | 마을방송 송출 |  <li>Icon + Web Application<br><li>Native는 실행아이콘과 브라우저 실행 기능만 제공 | <span style='color:green'>Web Browser</span>
| 전용 단말<br>(Android tablet) | 홈 런처 | Hybrid Application | WebView
| - | 인터넷 라디오 | Native Application | WebView
| - | 마을방송 수신 | Hybrid Application | WebView
| - | 마을방송 송출 | Web Application(Browser) | <span style='color:green'>Web Browser</span>
| - | IoT 클라이언트 | Hybrid Application + Native Service | WebView
| - | 푸시 클라이언트 | Native Service | WebView
| - | 미디어 Control | Native Service | WebView
> 1. <span style='color:navy'>모바일 웹브라우저에서 실행되는 웹앱은 Full screen 모드로 동작해야 한다. 모바일 웹앱의 Full screen 모드 구현은 [모바일에서 놀라운 풀스크린 환경 구축하기](http://www.html5rocks.com/ko/mobile/fullscreen/)를 참조하면 된다. 

### 2. 웹앱 및 Native App간의 데이터 관리 
#### 1) PC / 스마트 폰에서의 데이터 관리 
> <span style='color:navy'>웹브라우저 실행 어플리케이션 데이터 관리 : HTML5 Local storage 또는 IndexedDB를 이용하면 된다. 

<center>![웹어플리케이션 데이터 관리](images/web_storage.png)</center>

#### 1) 전용단말에서의 데이터 관리 및 공유  
> * <span style='color:navy'>웹-Native간 데이터 공유 : 안드로이드 전용단말에서만 데이터 공유가 필요. 안드로이드에서 제공하는 JavascriptInterface를 이용한다. [Native-웹뷰 간 인터페이스 정리](Native_webview_interface.html)에서 상세 인터페이스 들을 확인할 수 있다. 
> * <span style='color:navy'>쿠키 등을 공유한다면 웹뷰 호출시에 CookieSyncManager 등을 이용해서 데이터를 공유한다. 

<center>![웹-Native 데이터 공유](images/web_native_data_share.png)</center>

## 마을방송 서비스 SW 구성(전용단말 기준) 
<center>![마을방송 단말 SW 구성](images/1_sw_architecture.png)</center>

## 어플리케이션 별 상세 기능  
|어플리케이션 | 세부기능 | 비고 |
|:----:|----|----|
|홈런처 | 날씨 위젯 
|- | 시계 위젯 
|- | 달력 위젯 
|- | 서비스 아이콘 메뉴
|- | 어플리케이션 실행 및 마켓플레이스 | 서비스 아이콘 메뉴와 연동.<br> JavascriptInterface 필요 
|인터넷라디오 | 방송 목록 | 
|- | <span style="color:red;"><del>방송별 편성표 | <span style="color:red;"><del>(주)EPG 연동해야 함.  
|- | Media Control | 
|마을방송 수신 클라이언트 | 상세 사용자 연동 기능은 기획 참조 | Web App.으로 구현 
|- | 푸시 메시지 처리 | JavascriptInterface 필요.<br>상태만 전달.<br>미디어는 Native에서 처리  
|- | Media Control | JavascriptInterface 필요 
|마을방송 송출 클라이언트 | 송출 UI 및 서버 연동 등은 WebApp | 송출 시 미디어 컨트롤 제어 <br>JavascriptInterface 필요 
|IoT 클라이언트 | 헬스케어 보기 | 웹앱 
|- | 리모콘 | 웹앱
|- | IoT Gateway 설정 | 웹앱
|- | IoT Gateway 수집데이터 | Native Service 
|푸시 클라이언트 서비스 | 서버 연동 및 푸시 앱 관리 
|미디어 컨트롤 서비스 | 미디어 출력 제어 및 컨트롤 제공


## <a name="list"></a>서비스별 상세 개발 설계 및 인터페이스 
### 전용단말 상세 구현 
1. [Home Launcher](Home_launcher_application.html)
2. [인터넷 라디오](Iradio_client.html)
3. [마을방송 수신 클라이언트](Incoming_broadcast_client.html)
4. [마을방송 송출 클라이언트](Outgoing_broadcast_client.html)
5. [IoT 클라이언트](Iot_client.html)
6. [푸시 클라이언트 서비스](Push_agent_service.html)
7. [미디어 컨트롤 서비스](Media_playback_service.html)

### 웹뷰 및 서버간 인터페이스 정리 
6. [Native-웹뷰 간 인터페이스 정리](Native_webview_interface.html)
7. [서버 연동 인터페이스](Server_interface.html)


### 참고 
#### 1. Android App Manifest
* [Custom permission 참조](http://developer.android.com/guide/topics/manifest/manifest-intro.html)

#### 2. Cookie / Session 관리 
* [WebView 사용시에 쿠키 동기화 문제 해결하기](http://theeye.pe.kr/archives/1179)  
* [PersistentCookieStore](https://github.com/loopj/android-async-http/blob/master/library/src/main/java/com/loopj/android/http/PersistentCookieStore.java)
* 쿠키 관리 - PersistentCookieStore. Preferences or DB 사용 [Like this ...](https://github.com/loopj/android-async-http/blob/master/library/src/main/java/com/loopj/android/http/PersistentCookieStore.java)

~~~java
@Override
public boolean clearExpired(Date date) {
    boolean clearedAny = false;
    SharedPreferences.Editor prefsWriter = cookiePrefs.edit();

    for (ConcurrentHashMap.Entry<String, Cookie> entry : cookies.entrySet()) {
        String name = entry.getKey();
        Cookie cookie = entry.getValue();
        if (cookie.isExpired(date)) {
            // Clear cookies from local store
            cookies.remove(name);

            // Clear cookies from persistent store
            prefsWriter.remove(COOKIE_NAME_PREFIX + name);

            // We've cleared at least one
            clearedAny = true;
        }
    }

    // Update names in persistent store
    if (clearedAny) {
        prefsWriter.putString(COOKIE_NAME_STORE, TextUtils.join(",", cookies.keySet()));
    }
    prefsWriter.commit();

    return clearedAny;
}
~~~
