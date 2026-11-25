package com.example.handleremptymessagestatus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Demo 4.4.5　sendEmptyMessage 与 handleMessage 状态处理
 *
 * 核心关键路径：
 * 点击按钮 → 子线程 Thread.sleep 模拟耗时 → 用 sendEmptyMessage(what) 发送“空消息”
 * → 主线程 Handler.handleMessage() 按 what 分支 → 集中更新 ProgressBar / TextView / Button。
 *
 * 关键约束：
 * 1. 子线程只负责“发消息”，绝不碰任何控件；
 * 2. 三种状态编号使用语义常量，不用魔法数字；
 * 3. 成功和失败都必须隐藏进度条并恢复按钮，避免状态残留；
 * 4. onDestroy 中清理待处理消息，避免 Handler 继续持有已销毁的 Activity。
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HandlerStatusDemo";

    /** 消息编号：任务开始（加载中） */
    public static final int MSG_LOADING = 1;
    /** 消息编号：任务成功 */
    public static final int MSG_SUCCESS = 2;
    /** 消息编号：任务失败 */
    public static final int MSG_ERROR = 3;

    /** 模拟后台耗时任务的时长 */
    private static final long FAKE_WORK_DURATION_MS = 2000L;

    private TextView tvStateTitle;
    private TextView tvStateDetail;
    private TextView tvMessageInfo;
    private ProgressBar pbLoading;
    private Button btnStart;
    private CheckBox cbForceError;

    /** 后台工作线程，仅用于演示，退出页面时需要中断 */
    private Thread workerThread;
    /** 防止重复点击导致多个线程同时发消息 */
    private boolean taskRunning;

    /**
     * 绑定主线程 Looper 的 Handler：handleMessage() 一定运行在主线程，
     * 因此可以安全地操作界面控件。
     */
    private final Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.d(TAG, "handleMessage 线程=" + Thread.currentThread().getName()
                    + "，收到 what=" + msg.what);

            switch (msg.what) {
                case MSG_LOADING:
                    renderLoadingState();
                    break;
                case MSG_SUCCESS:
                    renderSuccessState();
                    break;
                case MSG_ERROR:
                    renderErrorState();
                    break;
                default:
                    // 未定义的消息编号交回父类处理，避免吞掉未知消息
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStateTitle = findViewById(R.id.tvStateTitle);
        tvStateDetail = findViewById(R.id.tvStateDetail);
        tvMessageInfo = findViewById(R.id.tvMessageInfo);
        pbLoading = findViewById(R.id.pbLoading);
        btnStart = findViewById(R.id.btnStart);
        cbForceError = findViewById(R.id.cbForceError);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFakeTask();
            }
        });

        // 进入页面时回到初始态，保证不会残留上一次的状态
        renderWaitingState();
    }

    /**
     * 启动后台任务：子线程里只做三件事——发 MSG_LOADING、sleep、发结果消息。
     */
    private void startFakeTask() {
        if (taskRunning) {
            Log.d(TAG, "任务进行中，忽略重复点击");
            return;
        }
        taskRunning = true;

        // 关键点：在主线程读取 CheckBox 状态并存成局部变量，
        // 子线程只使用这个副本，不去读取控件，避免子线程访问 UI。
        final boolean willFail = cbForceError.isChecked();

        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "子线程开始：" + Thread.currentThread().getName());

                // ① 任务开始：空消息，只带编号
                mainHandler.sendEmptyMessage(MSG_LOADING);

                boolean interrupted = false;
                try {
                    // ② 模拟耗时工作（真实场景这里是网络/文件读写）
                    Thread.sleep(FAKE_WORK_DURATION_MS);
                } catch (InterruptedException e) {
                    interrupted = true;
                    Thread.currentThread().interrupt();
                    Log.w(TAG, "子线程被中断，放弃本次任务", e);
                }

                if (interrupted || isFinishing() || isDestroyed()) {
                    Log.w(TAG, "页面已退出，不再发送结果消息");
                    return;
                }

                // ③ 任务结束：成功或失败同样是空消息，结果完全由 what 区分
                mainHandler.sendEmptyMessage(willFail ? MSG_ERROR : MSG_SUCCESS);
            }
        }, "fake-worker-thread");

        workerThread.start();
        Log.d(TAG, "已启动子线程：" + workerThread.getName());
    }

    // ==================== 三种状态的界面渲染 ====================
    // 所有控件修改都集中在这里，只在 handleMessage() 中被调用。

    private void renderWaitingState() {
        taskRunning = false;
        pbLoading.setVisibility(View.GONE);
        btnStart.setEnabled(true);

        tvStateTitle.setText(R.string.state_waiting_title);
        tvStateTitle.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvStateDetail.setText(R.string.state_waiting_detail);
        tvMessageInfo.setText(R.string.message_info_none);
    }

    private void renderLoadingState() {
        pbLoading.setVisibility(View.VISIBLE);
        btnStart.setEnabled(false);

        tvStateTitle.setText(R.string.state_loading_title);
        tvStateTitle.setTextColor(ContextCompat.getColor(this, R.color.state_loading));
        tvStateDetail.setText(R.string.state_loading_detail);
        tvMessageInfo.setText(formatMessageInfo("MSG_LOADING", MSG_LOADING));
    }

    private void renderSuccessState() {
        taskRunning = false;
        pbLoading.setVisibility(View.GONE);
        btnStart.setEnabled(true);

        tvStateTitle.setText(R.string.state_success_title);
        tvStateTitle.setTextColor(ContextCompat.getColor(this, R.color.state_success));
        tvStateDetail.setText(R.string.state_success_detail);
        tvMessageInfo.setText(formatMessageInfo("MSG_SUCCESS", MSG_SUCCESS));
    }

    private void renderErrorState() {
        taskRunning = false;
        pbLoading.setVisibility(View.GONE);
        btnStart.setEnabled(true);

        tvStateTitle.setText(R.string.state_error_title);
        tvStateTitle.setTextColor(ContextCompat.getColor(this, R.color.state_error));
        tvStateDetail.setText(R.string.state_error_detail);
        tvMessageInfo.setText(formatMessageInfo("MSG_ERROR", MSG_ERROR));
    }

    private String formatMessageInfo(String whatName, int whatValue) {
        return getString(R.string.message_info_format, whatName, whatValue);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理队列中所有待处理消息，防止 Handler 在页面销毁后继续更新界面
        mainHandler.removeCallbacksAndMessages(null);

        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }
        workerThread = null;
        taskRunning = false;
        Log.d(TAG, "onDestroy：已清理待处理消息并结束子线程");
    }
}
