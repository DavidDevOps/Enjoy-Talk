package com.chatting.myapp;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import java.io.*;
import java.net.*;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    public Socket cSocket = null;
    private String server = "210.222.43.222";  // 서버 ip주소
    private int port = 20914;                  // 포트번호

    public PrintWriter streamOut = null; //다양한 형태의 데이터를 문자열의 형태로 출력하거나, 문자열의 형태로 조합하여 출력
    public BufferedReader streamIn = null; //메모리에 저장하는 곳에 값을 담을때 씀.

    public chatThread cThread = null;

    public TextView tv;
    public EditText nickText;
    public EditText msgText;
    public ScrollView sv;

    public String nickName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sv = (ScrollView)findViewById(R.id.scrollView1);
        tv = (TextView)findViewById(R.id.text01);
        nickText = (EditText)findViewById(R.id.connText);
        msgText = (EditText)findViewById(R.id.chatText);

        logger("채팅을 시작합니다.");
    }

    public void onDestroy() { // 앱이 소멸되면
        super.onDestroy();
        if (cSocket != null) {
            sendMessage("# [" + nickName + "]님이 나가셨습니다.");
        }
    }

    public void connBtnClick(View v) {
        switch (v.getId()) {
            case R.id.connBtn: // 접속버튼
                if (cSocket == null) {
                    nickName = nickText.getText().toString();
                    logger("접속중입니다...");
                    connect(server, port , nickName);
                }
                break;
            case R.id.closeBtn: // 나가기 버튼
                if (cSocket != null) {
                    sendMessage("# [" + nickName + "]님이 나가셨습니다.");
                }
                break;
            case R.id.sendBtn: // 메세지 보내기 버튼
                if (cSocket != null) {
                    String msgString = msgText.getText().toString();
                    if (msgString != null && !"".equals(msgString)) {
                        sendMessage("[" + nickName + "] " + msgString);
                        msgText.setText("");
                    }
                } else {
                    logger("접속을 먼저 해주세요.");
                }
                break;
        }
    }

    private class connectTask extends AsyncTask<String, Void , Socket> {  //AsynTask : 쓰레드와 UI 동시처리 가능
        @Override
        protected Socket doInBackground(String... params) {
            try {
                cSocket = new Socket(server, port);
                streamOut = new PrintWriter(cSocket.getOutputStream(), true);
                streamIn = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            sendMessage("# 새로운 이용자 [" + nickName + "] 님이 들어왔습니다.");

            cThread = new chatThread();
            cThread.start();

            return null;
        }

    }

    public void connect(String server, int port, String user) {
        System.out.println("커넥트 시작");
        new connectTask().execute(server);

    }

    private void logger(String MSG) {
        tv.append(MSG + "\n");     // 텍스트뷰에 메세지를 더해줍니다.
        sv.fullScroll(ScrollView.FOCUS_DOWN); // 스크롤뷰의 스크롤을 내려줍니다.
    }

    private void sendMessage(String MSG) {
        try {
            streamOut.println(MSG);     // 서버에 메세지를 보내줍니다.
        } catch (Exception ex) {
            logger(ex.toString());
        }

    }

    Handler mHandler = new Handler() {   // 스레드에서 메세지를 받을 핸들러.
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: // 채팅 메세지를 받아온다.
                    logger(msg.obj.toString());
                    break;
                case 1: // 소켓접속을 끊는다.
                    try {
                        cSocket.close();
                        cSocket = null;

                        logger("접속이 끊어졌습니다.");

                    } catch (Exception e) {
                        logger("접속이 이미 끊겨 있습니다." + e.getMessage());
                        finish();
                    }
                    break;
            }
        }
    };

    class chatThread extends Thread {
        private boolean flag = false; // 스레드 유지(종료)용 플래그
        public void run() {
            try {
                while (!flag) { // 플래그가 false일경우에 루프
                    String msgs;
                    Message msg = new Message();
                    msg.what = 0;
                    msgs = streamIn.readLine();  // 서버에서 올 메세지를 기다린다.
                    msg.obj = msgs;

                    mHandler.sendMessage(msg); // 핸들러로 메세지 전송

                    if (msgs.equals("# [" + nickName + "]님이 나가셨습니다.")) { // 서버에서 온 메세지가 종료 메세지라면
                        flag = true;   // 스레드 종료를 위해 플래그를 true로 바꿈.
                        msg = new Message();
                        msg.what = 1;   // 종료메세지
                        mHandler.sendMessage(msg);
                    }
                }

            }catch(Exception e) {
                logger(e.getMessage());
            }
        }
    }
}