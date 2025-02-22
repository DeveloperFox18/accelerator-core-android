//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.opentok.accelerator.annotation;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.os.Build.VERSION;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import androidx.annotation.NonNull;
import com.opentok.accelerator.R.color;
import com.opentok.accelerator.R.drawable;
import com.opentok.accelerator.R.id;
import com.opentok.accelerator.annotation.Annotatable.AnnotatableType;
import com.opentok.accelerator.annotation.AnnotationsToolbar.ActionsListener;
import com.opentok.accelerator.annotation.utils.AnnotationsVideoRenderer;
import com.opentok.accelerator.core.listeners.SignalListener;
import com.opentok.accelerator.core.signal.SignalInfo;
import com.opentok.accelerator.core.wrapper.OTAcceleratorSession;
import com.opentok.android.Connection;
import com.tokbox.android.logging.OTKAnalytics;
import com.tokbox.android.logging.OTKAnalyticsData;
import com.tokbox.android.logging.OTKAnalyticsData.Builder;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AnnotationsView extends ViewGroup implements ActionsListener, SignalListener {
    private static final String LOG_TAG = AnnotationsView.class.getSimpleName();
    private static final String SIGNAL_TYPE = "otAnnotation";
    private static final String SIGNAL_PLATFORM = "android";
    private AnnotationsPath mCurrentPath = new AnnotationsPath();
    private AnnotationsText mCurrentText = new AnnotationsText();
    private Paint mCurrentPaint;
    private AnnotationsVideoRenderer videoRenderer;
    private int mCurrentColor = 0;
    private int mSelectedColor = 0;
    private float mLineWidth = 10.0F;
    private float mIncomingLineWidth = 0.0F;
    private int mTextSize = 48;
    private AnnotationsManager mAnnotationsManager;
    private static final float TOLERANCE = 5.0F;
    private int width;
    private int height;
    private AnnotationsView.Mode mode;
    private AnnotationsToolbar mToolbar;
    private boolean mAnnotationsActive = false;
    private boolean loaded = false;
    private AnnotationsView.AnnotationsListener mListener;
    private Annotatable mCurrentAnnotatable;
    private boolean defaultLayout = false;
    private String mRemoteConnId;
    private String mPartnerId;
    private OTKAnalyticsData mAnalyticsData;
    private OTKAnalytics mAnalytics;
    private boolean isScreenSharing = false;
    private boolean mSignalMirrored = false;
    private boolean isStartPoint = false;
    private boolean mMirrored = true;
    private String canvasId;
    private boolean clear = false;
    private Context mContext;
    private ViewGroup mContentView;
    private OTAcceleratorSession mSession;
    private String mConnectionId;
    private String mVideoWidth;
    private String mVideoHeight;
    private int mExtraHeight;

    public AnnotationsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public AnnotationsView(Context context, @NonNull OTAcceleratorSession session, String partnerId, boolean isScreenSharing, String connectionId, String videoWidth, String videoHeight, String extraHeight) throws IllegalArgumentException {
        super(context);
        if (session.getConnection() == null) {
            throw new IllegalArgumentException("Session is not connected");
        } else {
            this.mContext = context;
            this.mSession = session;
            this.mPartnerId = partnerId;
            this.mConnectionId = connectionId;
            this.mVideoWidth = videoWidth;
            this.mVideoHeight = videoHeight;
            this.mExtraHeight = Integer.valueOf(extraHeight);
            this.mSession.addSignalListener(AnnotationsView.Mode.Pen.toString(), this);
            this.mSession.addSignalListener(AnnotationsView.Mode.Text.toString(), this);
            this.mSession.addSignalListener(AnnotationsView.Mode.Undo.toString(), this);
            this.mSession.addSignalListener(AnnotationsView.Mode.Clear.toString(), this);
            this.isScreenSharing = isScreenSharing;
            this.init();
        }
    }

    public AnnotationsView(Context context, @NonNull OTAcceleratorSession session, String partnerId, String remoteConnId) throws IllegalArgumentException {
        super(context);
        if (session.getConnection() == null) {
            throw new IllegalArgumentException("Session is not connected");
        } else {
            this.mContext = context;
            this.mSession = session;
            this.mPartnerId = partnerId;
            this.mSession.addSignalListener(AnnotationsView.Mode.Pen.toString(), this);
            this.mSession.addSignalListener(AnnotationsView.Mode.Text.toString(), this);
            this.mSession.addSignalListener(AnnotationsView.Mode.Undo.toString(), this);
            this.mSession.addSignalListener(AnnotationsView.Mode.Clear.toString(), this);
            this.isScreenSharing = false;
            this.mRemoteConnId = remoteConnId;
            this.init();
        }
    }

    public void attachToolbar(@NonNull AnnotationsToolbar toolbar) throws Exception {
        this.addLogEvent("UseToolbar", "Attempt");
        this.mToolbar = toolbar;
        this.mToolbar.setActionListener(this);
        this.addLogEvent("UseToolbar", "Success");
    }

    public AnnotationsToolbar getToolbar() {
        return this.mToolbar;
    }

    public void setVideoRenderer(@NonNull AnnotationsVideoRenderer videoRenderer) {
        this.videoRenderer = videoRenderer;
    }

    public AnnotationsVideoRenderer getVideoRenderer() {
        return this.videoRenderer;
    }

    public void setAnnotationsListener(AnnotationsView.AnnotationsListener listener) {
        this.mListener = listener;
    }

    public void restart() {
        this.clearAll(false, this.mSession.getConnection().getConnectionId());
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.resize();
    }

    private void init() {
        this.addLogEvent("Init", "Attempt");
        String source = this.getContext().getPackageName();
        SharedPreferences prefs = this.getContext().getSharedPreferences("opentok", 0);
        String guidVSol = prefs.getString("guidVSol", (String)null);
        if (null == guidVSol) {
            guidVSol = UUID.randomUUID().toString();
            prefs.edit().putString("guidVSol", guidVSol).commit();
        }

        this.mAnalyticsData = (new Builder("2.0.2", source, "annotationsAccPack", guidVSol)).build();
        this.mAnalytics = new OTKAnalytics(this.mAnalyticsData);
        if (this.mSession != null) {
            this.mAnalyticsData.setSessionId(this.mSession.getSessionId());
            this.mAnalyticsData.setConnectionId(this.mSession.getConnection().getConnectionId());
        }

        if (this.mPartnerId != null) {
            this.mAnalyticsData.setPartnerId(this.mPartnerId);
        }

        this.mAnalytics.setData(this.mAnalyticsData);
        this.setWillNotDraw(false);
        this.mAnnotationsManager = new AnnotationsManager();
        this.mCurrentColor = this.getResources().getColor(color.picker_color_orange);
        this.mSelectedColor = this.mCurrentColor;
        this.mode = AnnotationsView.Mode.Pen;
        this.addLogEvent("Init", "Success");
    }

    private void resize() {
        if (this.getLayoutParams() != null && this.getLayoutParams().width > 0 && this.getLayoutParams().height > 0 && !this.defaultLayout) {
            this.defaultLayout = false;
        } else {
            this.defaultLayout = true;
            this.getScreenRealSize();
            LayoutParams params = this.getLayoutParams();
            params.height = this.height - this.mToolbar.getHeight() - (this.height - this.getDisplayHeight());
            if (this.getActionBarHeight() != 0) {
                params.height -= this.getActionBarHeight();
            }

            params.width = this.width;
            this.setLayoutParams(params);
        }

    }

    private void getScreenRealSize() {
        Display display = ((Activity)this.mContext).getWindowManager().getDefaultDisplay();
        int realWidth;
        int realHeight;
        if (VERSION.SDK_INT >= 17) {
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;
        } else if (VERSION.SDK_INT >= 14) {
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer)mGetRawW.invoke(display);
                realHeight = (Integer)mGetRawH.invoke(display);
            } catch (Exception var6) {
                realWidth = display.getWidth();
                realHeight = display.getHeight();
                Log.e("Display Info", "Couldn't use reflection to get the real display metrics.");
            }
        } else {
            realWidth = display.getWidth();
            realHeight = display.getHeight();
        }

        this.width = realWidth;
        this.height = realHeight;
    }

    private int getDisplayHeight() {
        WindowManager windowManager = (WindowManager)this.getContext().getSystemService("window");
        Point size = new Point();
        int screenHeight = false;
        int screenHeight;
        if (VERSION.SDK_INT >= 11) {
            windowManager.getDefaultDisplay().getSize(size);
            screenHeight = size.y;
        } else {
            Display d = windowManager.getDefaultDisplay();
            screenHeight = d.getHeight();
        }

        return screenHeight;
    }

    private int getDisplayWidth() {
        WindowManager windowManager = (WindowManager)this.getContext().getSystemService("window");
        Point size = new Point();
        int screenWidth = false;
        int screenWidth;
        if (VERSION.SDK_INT >= 11) {
            windowManager.getDefaultDisplay().getSize(size);
            screenWidth = size.x;
        } else {
            Display d = windowManager.getDefaultDisplay();
            screenWidth = d.getWidth();
        }

        return screenWidth;
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = this.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = this.getResources().getDimensionPixelSize(resourceId);
        }

        return result;
    }

    private int getActionBarHeight() {
        int actionBarHeight = false;
        Rect rect = new Rect();
        Window window = ((Activity)this.mContext).getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;
        int contentViewTop = window.findViewById(16908290).getTop();
        int titleBarHeight = contentViewTop - statusBarHeight;
        return titleBarHeight;
    }

    private int getDisplayContentHeight() {
        WindowManager windowManager = (WindowManager)this.getContext().getSystemService("window");
        Point size = new Point();
        int screenHeight = false;
        int actionBarHeight = this.getActionBarHeight();
        int contentTop = this.getStatusBarHeight();
        int screenHeight;
        if (VERSION.SDK_INT >= 11) {
            windowManager.getDefaultDisplay().getSize(size);
            screenHeight = size.y;
        } else {
            Display d = windowManager.getDefaultDisplay();
            screenHeight = d.getHeight();
        }

        return screenHeight - contentTop - actionBarHeight;
    }

    private void drawText() {
        this.invalidate();
    }

    private void beginTouch(float x, float y) {
        this.mCurrentPath.moveTo(x, y);
    }

    private void moveTouch(float x, float y, boolean curved) {
        if (this.mCurrentPath != null && this.mCurrentPath.getPoints() != null && this.mCurrentPath.getPoints().size() > 0) {
            float mX = this.mCurrentPath.getEndPoint().x;
            float mY = this.mCurrentPath.getEndPoint().y;
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= 5.0F || dy >= 5.0F) {
                if (curved) {
                    this.mCurrentPath.quadTo(mX, mY, (x + mX) / 2.0F, (y + mY) / 2.0F);
                } else {
                    this.mCurrentPath.lineTo(x, y);
                }
            }
        }

    }

    private void upTouch() {
        this.upTouch(false);
    }

    private void upTouch(boolean curved) {
        float mLastX = this.mCurrentPath.getEndPoint().x;
        float mLastY = this.mCurrentPath.getEndPoint().y;
        int index = this.mCurrentPath.getPoints().size() - 1;
        float mX = ((PointF)this.mCurrentPath.getPoints().get(index)).x;
        float mY = ((PointF)this.mCurrentPath.getPoints().get(index)).y;
        if (curved) {
            this.mCurrentPath.quadTo(mLastX, mLastY, (mX + mLastX) / 2.0F, (mY + mLastY) / 2.0F);
        } else {
            this.mCurrentPath.lineTo(mX, mY);
        }

    }

    private void clearAll(boolean incoming, String cid) {
        JSONArray jsonArray = new JSONArray();
        if (this.mAnnotationsManager.getAnnotatableList().size() > 0) {
            Iterator var4 = this.mAnnotationsManager.getAnnotatableList().iterator();

            Annotatable annotatable;
            while(var4.hasNext()) {
                annotatable = (Annotatable)var4.next();
                Log.d(LOG_TAG, "clearAll: value of CID ---> : " + annotatable.getCId());
            }

            int i = this.mAnnotationsManager.getAnnotatableList().size() - 1;
            Log.d(LOG_TAG, "clearAll: Total Size ---> : " + this.mAnnotationsManager.getAnnotatableList().size());
            Log.d(LOG_TAG, "clearAll: value of i ---> : " + i);

            for(; i >= 0; this.invalidate()) {
                annotatable = (Annotatable)this.mAnnotationsManager.getAnnotatableList().get(i);
                if (annotatable.getCId().equals(cid)) {
                    this.mAnnotationsManager.getAnnotatableList().remove(i);
                    jsonArray.put(annotatable.getCId());
                    --i;
                } else {
                    --i;
                }
            }

            if (!incoming && !this.isScreenSharing) {
                this.sendAnnotation(this.mode.toString(), (String)null);
            }
        }

    }

    private int userSideAnnotationCount(String cid, AnnotationsManager annoManager) {
        int matchCount = 0;
        Iterator var4 = annoManager.getAnnotatableList().iterator();

        while(var4.hasNext()) {
            Annotatable annotatable = (Annotatable)var4.next();
            if (annotatable.getCId().equals(cid)) {
                ++matchCount;
            }
        }

        return matchCount;
    }

    private void clearAll_old(boolean incoming, String cid) {
        JSONArray jsonArray = new JSONArray();
        int i = this.userSideAnnotationCount(cid, this.mAnnotationsManager);
        Log.d(LOG_TAG, "clearAll: Total Size ---> : " + this.mAnnotationsManager.getAnnotatableList().size());
        Log.d(LOG_TAG, "clearAll: value of i ---> : " + i);

        while(i > 0) {
            Annotatable annotatable = (Annotatable)this.mAnnotationsManager.getAnnotatableList().get(i);
            Log.d(LOG_TAG, "annotatable.getCId() " + annotatable.getCId() + " CID  -> " + cid);
            if (!annotatable.getCId().equals(cid)) {
                Log.d(LOG_TAG, "clearAll: print else conditions : ");
                break;
            }

            Log.d(LOG_TAG, "clearAll: GET CID ---> : " + annotatable.getCId());
            Log.d(LOG_TAG, "clearAll: CID ---> : " + annotatable.getData());
            this.mAnnotationsManager.getAnnotatableList().remove(i);
            jsonArray.put(annotatable.getCId());
            --i;
            this.invalidate();
            Log.d(LOG_TAG, "clearAll: if condition : ");
        }

        Log.d(LOG_TAG, "Total matches found: " + i);
        if (!incoming && !this.isScreenSharing) {
            this.sendAnnotation(this.mode.toString(), (String)null);
        }

    }

    private void undoAnnotation(boolean incoming, String cid) {
        boolean removed = false;
        if (this.mAnnotationsManager.getAnnotatableList().size() > 0) {
            for(int i = this.mAnnotationsManager.getAnnotatableList().size() - 1; !removed && i >= 0; --i) {
                Annotatable annotatable = (Annotatable)this.mAnnotationsManager.getAnnotatableList().get(i);
                if (annotatable.getCId().equals(cid)) {
                    this.mAnnotationsManager.getAnnotatableList().remove(i);
                    removed = true;
                }
            }

            this.invalidate();
            if (!incoming && !this.isScreenSharing) {
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(cid);
                this.sendAnnotation(this.mode.toString(), jsonArray.toString());
            }
        }

    }

    private void createTextAnnotatable(EditText editText, float x, float y) {
        Log.i(LOG_TAG, "Create Text Annotatable");
        this.mCurrentPaint = new Paint();
        this.mCurrentPaint.setAntiAlias(true);
        this.mCurrentPaint.setColor(this.mCurrentColor);
        this.mCurrentPaint.setTextSize((float)this.mTextSize);
        this.mCurrentText = new AnnotationsText(editText, x, y);
    }

    private void createPathAnnotatable(boolean incoming) {
        Log.i(LOG_TAG, "Create Path Annotatable");
        this.mCurrentPaint = new Paint();
        this.mCurrentPaint.setAntiAlias(true);
        this.mCurrentPaint.setColor(this.mCurrentColor);
        this.mCurrentPaint.setStyle(Style.STROKE);
        this.mCurrentPaint.setStrokeJoin(Join.ROUND);
        if (incoming) {
            this.mCurrentPaint.setStrokeWidth(this.mIncomingLineWidth);
        } else {
            this.mCurrentPaint.setStrokeWidth(this.mLineWidth);
        }

        if (this.mode != null && this.mode == AnnotationsView.Mode.Pen) {
            this.mCurrentPath = new AnnotationsPath();
        }

    }

    private void addAnnotatable(String cid) {
        Log.i(LOG_TAG, "Add Annotatable");
        if (this.mode != null) {
            if (this.mode.equals(AnnotationsView.Mode.Pen)) {
                this.mCurrentAnnotatable = new Annotatable(this.mode, this.mCurrentPath, this.mCurrentPaint, cid);
                this.mCurrentAnnotatable.setType(AnnotatableType.PATH);
            } else {
                this.mCurrentAnnotatable = new Annotatable(this.mode, this.mCurrentText, this.mCurrentPaint, cid);
                this.mCurrentAnnotatable.setType(AnnotatableType.TEXT);
            }

            this.mAnnotationsManager.addAnnotatable(this.mCurrentAnnotatable);
        }

    }

    private Bitmap getScreenshot() {
        View v1 = this.mContentView;
        v1.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
        v1.setDrawingCacheEnabled(false);
        return bitmap;
    }

    private void sendAnnotation(String type, String annotation) {
        if (this.mSession != null && !this.isScreenSharing) {
            this.mSession.sendSignal(new SignalInfo(this.mSession.getConnection().getConnectionId(), (String)null, type, annotation), (Connection)null);
        }

    }

    private String buildSignalFromText(float x, float y, String text, boolean start, boolean end) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        boolean mirrored = false;
        int videoWidth = false;
        int videoHeight = false;
        if (this.videoRenderer != null) {
            mirrored = this.videoRenderer.isMirrored();
            int videoWidth = this.videoRenderer.getVideoWidth();
            int var13 = this.videoRenderer.getVideoHeight();
        }

        try {
            jsonObject.put("id", this.mConnectionId);
            jsonObject.put("fromId", this.mSession.getConnection().getConnectionId());
            jsonObject.put("fromX", (double)x);
            jsonObject.put("fromY", (double)y);
            jsonObject.put("color", String.format("#%06X", 16777215 & this.mCurrentColor));
            jsonObject.put("videoWidth", this.mVideoWidth);
            jsonObject.put("videoHeight", this.mVideoHeight);
            jsonObject.put("canvasWidth", this.mVideoWidth);
            jsonObject.put("canvasHeight", this.mVideoHeight);
            jsonObject.put("mirrored", false);
            jsonObject.put("text", text);
            jsonObject.put("font", "16px Arial");
            jsonObject.put("platform", "android");
            jsonObject.put("mode", this.mode);
            jsonArray.put(jsonObject);
        } catch (JSONException var12) {
            var12.printStackTrace();
        }

        return jsonArray.toString();
    }

    private String buildSignalFromPoint(float x, float y, boolean startPoint, boolean endPoint) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        boolean mirrored = false;
        int videoWidth = false;
        int videoHeight = false;
        if (this.videoRenderer != null) {
            mirrored = this.videoRenderer.isMirrored();
            int videoWidth = this.videoRenderer.getVideoWidth();
            int var13 = this.videoRenderer.getVideoHeight();
        }

        try {
            jsonObject.put("id", this.mConnectionId);
            jsonObject.put("fromId", this.mSession.getConnection().getConnectionId());
            jsonObject.put("fromX", (double)this.mCurrentPath.getEndPoint().x);
            jsonObject.put("fromY", (double)this.mCurrentPath.getEndPoint().y);
            jsonObject.put("toX", (double)x);
            jsonObject.put("toY", (double)y);
            jsonObject.put("color", String.format("#%06X", 16777215 & this.mCurrentColor));
            jsonObject.put("lineWidth", 2);
            jsonObject.put("videoWidth", this.mVideoWidth);
            jsonObject.put("videoHeight", this.mVideoHeight);
            jsonObject.put("canvasWidth", this.mVideoWidth);
            jsonObject.put("canvasHeight", this.mVideoHeight);
            jsonObject.put("mirrored", false);
            jsonObject.put("smoothed", false);
            jsonObject.put("startPoint", startPoint);
            jsonObject.put("endPoint", endPoint);
            jsonObject.put("platform", "android");
            jsonObject.put("mode", this.mode);
            jsonArray.put(jsonObject);
        } catch (JSONException var11) {
            var11.printStackTrace();
        }

        return jsonArray.toString();
    }

    private void penAnnotations(String connectionId, String data) {
        this.mode = AnnotationsView.Mode.Pen;

        try {
            JSONArray updates = new JSONArray(data);
            Log.d(LOG_TAG, "penAnnotations " + updates.toString());

            for(int i = 0; i < updates.length(); ++i) {
                JSONObject json = updates.getJSONObject(i);
                String platform = null;
                if (!json.isNull("platform")) {
                    platform = (String)json.get("platform");
                }

                String id = (String)json.get("id");
                this.mSignalMirrored = true;
                boolean initialPoint = false;
                boolean secondPoint = false;
                boolean endPoint = false;
                Number value;
                if (!json.isNull("endPoint")) {
                    if (json.get("endPoint") instanceof Number) {
                        value = (Number)json.get("endPoint");
                        endPoint = value.intValue() == 1;
                    } else {
                        endPoint = (Boolean)json.get("endPoint");
                    }
                }

                if (!json.isNull("startPoint")) {
                    if (json.get("startPoint") instanceof Number) {
                        value = (Number)json.get("startPoint");
                        initialPoint = value.intValue() == 1;
                    } else {
                        initialPoint = (Boolean)json.get("startPoint");
                    }

                    if (initialPoint) {
                        this.isStartPoint = true;
                    } else if (this.isStartPoint) {
                        secondPoint = true;
                        this.isStartPoint = false;
                    }
                }

                if (!json.isNull("color")) {
                    this.mCurrentColor = Color.parseColor(((String)json.get("color")).toLowerCase());
                }

                if (!json.isNull("lineWidth")) {
                    this.mIncomingLineWidth = ((Number)json.get("lineWidth")).floatValue();
                }

                float scale = 1.0F;
                float localWidth = 0.0F;
                float localHeight = 0.0F;
                if (this.videoRenderer != null) {
                    localWidth = (float)this.videoRenderer.getVideoWidth();
                    localHeight = (float)this.videoRenderer.getVideoHeight();
                }

                if (localWidth == 0.0F) {
                    localWidth = (float)this.getDisplayWidth();
                }

                if (localHeight == 0.0F) {
                    try {
                        localHeight = Float.parseFloat(this.mVideoHeight);
                    } catch (NumberFormatException var30) {
                        System.out.println("Invalid input for mVideoHeight: " + this.mVideoHeight);
                        localHeight = 0.0F;
                    }

                    Log.d(LOG_TAG, "penAnnotations local Height " + localHeight);
                }

                Map<String, Float> canvas = new HashMap();
                canvas.put("width", localWidth);
                canvas.put("height", localHeight);
                Map<String, Float> iCanvas = new HashMap();
                if (platform.equals("android")) {
                    iCanvas.put("width", Float.parseFloat((String)json.get("canvasWidth")));
                    iCanvas.put("height", Float.parseFloat((String)json.get("canvasHeight")));
                } else {
                    iCanvas.put("width", ((Number)json.get("canvasWidth")).floatValue());
                    iCanvas.put("height", ((Number)json.get("canvasHeight")).floatValue());
                }

                float canvasRatio = (Float)canvas.get("width") / (Float)canvas.get("height");
                Log.d(LOG_TAG, "penAnnotations canvasRatio " + canvasRatio);
                if (canvasRatio < 0.0F) {
                    scale = (Float)canvas.get("width") / (Float)iCanvas.get("width");
                } else {
                    scale = (Float)canvas.get("height") / (Float)iCanvas.get("height");
                }

                Log.d(LOG_TAG, "penAnnotations canvasRatio 1:: " + scale);
                float centerX = (Float)canvas.get("width") / 2.0F;
                float centerY = (Float)canvas.get("height") / 2.0F;
                float iCenterX = (Float)iCanvas.get("width") / 2.0F;
                float iCenterY = (Float)iCanvas.get("height") / 2.0F;
                Log.d(LOG_TAG, "penAnnotations canvasRatio CX:: " + centerX);
                Log.d(LOG_TAG, "penAnnotations canvasRatio CY:: " + centerY);
                Log.d(LOG_TAG, "penAnnotations canvasRatio ICX:: " + iCenterX);
                Log.d(LOG_TAG, "penAnnotations canvasRatio ICY:: " + iCenterY);
                float fromX = ((Number)json.get("fromX")).floatValue();
                float fromY = ((Number)json.get("fromY")).floatValue();
                float toY = ((Number)json.get("toY")).floatValue();
                float toX = ((Number)json.get("toX")).floatValue();
                if (platform.equals("android") || platform.equals("web")) {
                    fromX = centerX - scale * (iCenterX - ((Number)json.get("fromX")).floatValue());
                    toX = centerX - scale * (iCenterX - ((Number)json.get("toX")).floatValue());
                    fromY = centerY - scale * (iCenterY - ((Number)json.get("fromY")).floatValue());
                    fromY -= (float)this.getStatusBarHeight();
                    toY = centerY - scale * (iCenterY - ((Number)json.get("toY")).floatValue());
                    toY -= (float)this.getStatusBarHeight();
                }

                fromY -= (float)this.getActionBarHeight();
                toY -= (float)this.getActionBarHeight();
                Log.d(LOG_TAG, "penAnnotations fromX 10:: " + fromX);
                Log.d(LOG_TAG, "penAnnotations fromY 11:: " + fromY);
                Log.d(LOG_TAG, "penAnnotations toY 12:: " + toY);
                Log.d(LOG_TAG, "penAnnotations toX 13::" + toX);
                if (this.mSignalMirrored) {
                    Log.i(LOG_TAG, "Signal is mirrored");
                    fromX = (float)this.width - fromX;
                    toX = (float)this.width - toX;
                }

                this.mMirrored = true;
                if (this.mMirrored) {
                    Log.i(LOG_TAG, "Feed is mirrored");
                    fromX = (float)this.width - fromX;
                    toX = (float)this.width - toX;
                }

                boolean smoothed = false;
                if (!json.isNull("smoothed")) {
                    if (json.get("smoothed") instanceof Number) {
                        Number value = (Number)json.get("smoothed");
                        smoothed = value.intValue() == 1;
                    } else {
                        smoothed = (Boolean)json.get("smoothed");
                    }
                }

                if (smoothed) {
                    if (this.isStartPoint) {
                        this.mAnnotationsActive = true;
                        this.createPathAnnotatable(false);
                        this.mCurrentPath.addPoint(new PointF(toX, toY));
                    } else if (secondPoint) {
                        this.beginTouch((toX + this.mCurrentPath.getEndPoint().x) / 2.0F, (toY + this.mCurrentPath.getEndPoint().y) / 2.0F);
                        this.mCurrentPath.addPoint(new PointF(toX, toY));
                    } else {
                        this.moveTouch(toX, toY, true);
                        this.mCurrentPath.addPoint(new PointF(toX, toY));
                        if (endPoint) {
                            try {
                                this.addAnnotatable(connectionId);
                            } catch (Exception var29) {
                                Log.e(LOG_TAG, var29.toString());
                            }

                            this.mAnnotationsActive = false;
                        }
                    }
                } else if (this.isStartPoint && endPoint) {
                    this.mAnnotationsActive = true;
                    this.createPathAnnotatable(false);
                    this.mCurrentPath.addPoint(new PointF(fromX, fromY));
                    this.beginTouch(fromX, fromY);
                    this.moveTouch(toX, toY, false);
                    this.upTouch();

                    try {
                        this.addAnnotatable(connectionId);
                    } catch (Exception var28) {
                        Log.e(LOG_TAG, var28.toString());
                    }
                } else if (this.isStartPoint) {
                    this.mAnnotationsActive = true;
                    this.createPathAnnotatable(false);
                    this.mCurrentPath.addPoint(new PointF(fromX, fromY));
                    this.beginTouch(toX, toY);
                } else if (endPoint) {
                    this.moveTouch(toX, toY, false);
                    this.upTouch();

                    try {
                        this.addAnnotatable(connectionId);
                    } catch (Exception var27) {
                        Log.e(LOG_TAG, var27.toString());
                    }

                    this.mAnnotationsActive = false;
                } else {
                    this.moveTouch(toX, toY, false);
                    this.mCurrentPath.addPoint(new PointF(toX, toY));
                }

                this.invalidate();
            }
        } catch (JSONException var31) {
            var31.printStackTrace();
        }

    }

    private void textAnnotation(String connectionId, String data) {
        this.mode = AnnotationsView.Mode.Text;

        try {
            JSONArray updates = new JSONArray(data);

            for(int i = 0; i < updates.length(); ++i) {
                JSONObject json = updates.getJSONObject(i);
                String id = (String)json.get("id");
                this.mSignalMirrored = true;
                if (!json.isNull("color")) {
                    this.mCurrentColor = Color.parseColor(((String)json.get("color")).toLowerCase());
                }

                String text = (String)json.get("text");
                String font = (String)json.get("font");
                boolean end = true;
                boolean start = true;
                Number value;
                if (!json.isNull("end")) {
                    if (json.get("end") instanceof Number) {
                        value = (Number)json.get("end");
                        end = value.intValue() == 1;
                    } else {
                        end = (Boolean)json.get("end");
                    }
                }

                if (!json.isNull("start")) {
                    if (json.get("start") instanceof Number) {
                        value = (Number)json.get("start");
                        start = value.intValue() == 1;
                    } else {
                        start = (Boolean)json.get("start");
                    }
                }

                float scale = 1.0F;
                float localWidth = 0.0F;
                float localHeight = 0.0F;
                if (this.videoRenderer != null) {
                    localWidth = (float)this.videoRenderer.getVideoWidth();
                    localHeight = (float)this.videoRenderer.getVideoHeight();
                }

                if (localWidth == 0.0F) {
                    localWidth = (float)this.getDisplayWidth();
                }

                if (localHeight == 0.0F) {
                    try {
                        localHeight = Float.parseFloat(this.mVideoHeight);
                    } catch (NumberFormatException var28) {
                        System.out.println("Invalid input for mVideoHeight: " + this.mVideoHeight);
                        localHeight = 0.0F;
                    }

                    Log.d(LOG_TAG, "penAnnotations local Height " + localHeight);
                }

                String platform = null;
                if (!json.isNull("platform")) {
                    platform = (String)json.get("platform");
                }

                Map<String, Float> canvas = new HashMap();
                canvas.put("width", localWidth);
                canvas.put("height", localHeight);
                Map<String, Float> iCanvas = new HashMap();
                if (platform.equals("android")) {
                    iCanvas.put("width", Float.parseFloat((String)json.get("canvasWidth")));
                    iCanvas.put("height", Float.parseFloat((String)json.get("canvasHeight")));
                } else {
                    iCanvas.put("width", ((Number)json.get("canvasWidth")).floatValue());
                    iCanvas.put("height", ((Number)json.get("canvasHeight")).floatValue());
                }

                float canvasRatio = (Float)canvas.get("width") / (Float)canvas.get("height");
                if (canvasRatio < 0.0F) {
                    scale = (Float)canvas.get("width") / (Float)iCanvas.get("width");
                } else {
                    scale = (Float)canvas.get("height") / (Float)iCanvas.get("height");
                }

                Log.i(LOG_TAG, "Scale: " + scale);
                float centerX = (Float)canvas.get("width") / 2.0F;
                float centerY = (Float)canvas.get("height") / 2.0F;
                float iCenterX = (Float)iCanvas.get("width") / 2.0F;
                float iCenterY = (Float)iCanvas.get("height") / 2.0F;
                float textX = centerX - scale * (iCenterX - ((Number)json.get("fromX")).floatValue());
                float textY = centerY - scale * (iCenterY - ((Number)json.get("fromY")).floatValue());
                EditText editText = new EditText(this.getContext());
                editText.setVisibility(0);
                editText.setImeOptions(6);
                editText.setBackgroundResource(drawable.input_text);
                int editTextHeight = 70;
                int editTextWidth = 200;
                android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(editTextWidth, editTextHeight);
                params.topMargin = (int)textX;
                params.leftMargin = (int)textY;
                this.addView(editText, params);
                editText.setSingleLine();
                editText.requestFocus();
                editText.setText(text);
                editText.setTextSize((float)this.mTextSize);
                this.createTextAnnotatable(editText, textX, textY);
                this.mCurrentText.getEditText().setText(text.toString());
                this.mAnnotationsActive = false;
                this.addAnnotatable(connectionId);
                this.mCurrentText = null;
                this.invalidate();
            }
        } catch (JSONException var29) {
            var29.printStackTrace();
        }

    }

    private void addLogEvent(String action, String variation) {
        if (this.mAnalytics != null) {
            this.mAnalytics.logEvent(action, variation);
        }

    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.loaded = false;
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();
        Log.d(LOG_TAG, "onTouchEvent--->: " + this.mode);
        if (this.mode != null) {
            this.mCurrentColor = this.mSelectedColor;
            if (this.mode == AnnotationsView.Mode.Pen) {
                Log.d(LOG_TAG, "onTouchEvent--->1: " + this.mode);
                this.addLogEvent("FreeHand", "Attempt");
                switch(event.getAction()) {
                case 0:
                    this.addLogEvent("StartDrawing", "Attempt");
                    this.mAnnotationsActive = true;
                    this.createPathAnnotatable(false);
                    this.beginTouch(x, y);
                    this.mCurrentPath.addPoint(new PointF(x, y));
                    this.sendAnnotation(this.mode.toString(), this.buildSignalFromPoint(x, y, true, false));
                    this.invalidate();
                    this.addLogEvent("StartDrawing", "Success");
                    break;
                case 1:
                    this.addLogEvent("EndDrawing", "Attempt");
                    this.upTouch();
                    this.sendAnnotation(this.mode.toString(), this.buildSignalFromPoint(x, y, false, true));

                    try {
                        this.addAnnotatable(this.mSession.getConnection().getConnectionId());
                    } catch (Exception var11) {
                        Log.e(LOG_TAG, var11.toString());
                    }

                    this.mAnnotationsActive = false;
                    this.invalidate();
                    this.addLogEvent("EndDrawing", "Success");
                    break;
                case 2:
                    this.moveTouch(x, y, true);
                    this.sendAnnotation(this.mode.toString(), this.buildSignalFromPoint(x, y, false, false));
                    this.mCurrentPath.addPoint(new PointF(x, y));
                    this.invalidate();
                }

                this.addLogEvent("FreeHand", "Success");
            } 
            else if (this.mode == AnnotationsView.Mode.Text) {
                this.addLogEvent("Text", "Attempt");
                this.mAnnotationsActive = true;
                ViewGroup parent = (ViewGroup)this.getParent();
                if (parent == null) {
                    throw new IllegalStateException("AnnotationsView must have a parent ViewGroup!");
                }

                final EditText editText = new EditText(this.getContext());
                editText.setVisibility(0);
                editText.setImeOptions(6);
                editText.setMinHeight((int)TypedValue.applyDimension(1, 50.0F, this.getResources().getDisplayMetrics()));
                editText.setMinWidth((int)TypedValue.applyDimension(1, 150.0F, this.getResources().getDisplayMetrics()));
                int editTextHeight = 70;
                int editTextWidth = 200;
                android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(editTextWidth, editTextHeight);
                params.topMargin = (int)event.getRawY();
                params.leftMargin = (int)event.getRawX();
                editText.setLayoutParams(params);
                editText.setPadding(15, 0, 15, 0);
                editText.setVisibility(0);
                editText.setSingleLine();
                editText.setImeOptions(6);
                editText.requestFocus();
                editText.setTextSize(12.0F);
                Context var10000 = this.getContext();
                this.getContext();
                InputMethodManager imm = (InputMethodManager)var10000.getSystemService("input_method");
                imm.toggleSoftInput(2, 1);
                this.createTextAnnotatable(editText, x, y);
                editText.setBackgroundResource(drawable.input_text);
                editText.addTextChangedListener(new TextWatcher() {
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    public void afterTextChanged(Editable s) {
                    }

                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }
                });
                editText.setOnEditorActionListener(new OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == 6) {
                            InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService("input_method");
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            if (AnnotationsView.this.mCurrentText != null) {
                                AnnotationsView.this.sendAnnotation(AnnotationsView.this.mode.toString(), AnnotationsView.this.buildSignalFromText(x, y, AnnotationsView.this.mCurrentText.getEditText().getText().toString(), false, true));
                            }

                            AnnotationsView.this.mAnnotationsActive = false;

                            try {
                                AnnotationsView.this.addAnnotatable(AnnotationsView.this.mSession.getConnection().getConnectionId());
                            } catch (Exception var6) {
                                Log.e(AnnotationsView.LOG_TAG, var6.toString());
                            }

                            editText.setBackground(null);

                            ViewGroup parent = (ViewGroup) editText.getParent();
                            if (parent != null) {
                                parent.removeView(editText);
                            }
                    
                            // **Fix: Clear focus**
                            editText.clearFocus();

                            AnnotationsView.this.mCurrentText = null;
                            AnnotationsView.this.invalidate();
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                parent.addView(editText);
                this.addLogEvent("Text", "Success");
            }
        } else {
            Log.d("AnnotationView", "mode is null....");
        }

        return true;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mAnnotationsActive) {
            if (this.mCurrentText != null && this.mCurrentText.getEditText() != null && !this.mCurrentText.getEditText().getText().toString().isEmpty()) {
                new TextPaint(this.mCurrentPaint);
                String text = this.mCurrentText.getEditText().getText().toString();
                Paint borderPaint = new Paint();
                borderPaint.setStyle(Style.STROKE);
                borderPaint.setStrokeWidth(5.0F);
                borderPaint.setColor(color.primary);
                Log.d("AnnotationView", " Setting color");
                Rect result = new Rect();
                this.mCurrentPaint.getTextBounds(text, 0, text.length(), result);
                if (text.length() > 10) {
                    String[] strings = text.split("(?<=\\G.{10})");
                    float x = this.mCurrentText.getX();
                    float y = 340.0F;
                    canvas.drawRect(x, y - (float)result.height() - 20.0F + (float)(strings.length * 50), x + (float)result.width() + 20.0F, y, borderPaint);

                    for(int i = 0; i < strings.length; ++i) {
                        canvas.drawText(strings[i], x, y, this.mCurrentPaint);
                        y += 50.0F;
                    }
                } else {
                    canvas.drawRect(this.mCurrentText.getX(), (float)(340 - result.height() - 20), this.mCurrentText.getX() + (float)result.width() + 20.0F, 340.0F, borderPaint);
                    canvas.drawText(this.mCurrentText.getEditText().getText().toString(), this.mCurrentText.getX(), 340.0F, this.mCurrentPaint);
                }
            }

            if (this.mCurrentPath != null) {
                canvas.drawPath(this.mCurrentPath, this.mCurrentPaint);
            }
        }

        Iterator var2 = this.mAnnotationsManager.getAnnotatableList().iterator();

        while(var2.hasNext()) {
            Annotatable drawing = (Annotatable)var2.next();
            if (drawing.getType().equals(AnnotatableType.PATH)) {
                canvas.drawPath(drawing.getPath(), drawing.getPaint());
            }

            if (drawing.getType().equals(AnnotatableType.TEXT)) {
                canvas.drawText(drawing.getText().getEditText().getText().toString(), drawing.getText().x, drawing.getText().y, drawing.getPaint());
            }
        }

    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void onItemSelected(final View v, final boolean selected) {
        ((Activity)this.mContext).runOnUiThread(new Runnable() {
            public void run() {
                if (v.getId() == id.done) {
                    AnnotationsView.this.addLogEvent("DONE", "Attempt");
                    AnnotationsView.this.mode = AnnotationsView.Mode.Clear;
                    AnnotationsView.this.clearAll(false, AnnotationsView.this.mSession.getConnection().getConnectionId());
                    AnnotationsView.this.mListener.onAnnotationsDone();
                    AnnotationsView.this.addLogEvent("DONE", "Success");
                }

                if (v.getId() == id.erase) {
                    AnnotationsView.this.addLogEvent("Erase", "Attempt");
                    AnnotationsView.this.mode = AnnotationsView.Mode.Undo;
                    AnnotationsView.this.undoAnnotation(false, AnnotationsView.this.mSession.getConnection().getConnectionId());
                    AnnotationsView.this.addLogEvent("Erase", "Success");
                }

                if (v.getId() == id.screenshot) {
                    AnnotationsView.this.addLogEvent("ScreenCapture", "Attempt");
                    AnnotationsView.this.mode = AnnotationsView.Mode.Capture;
                    Bitmap bmp;
                    if (AnnotationsView.this.videoRenderer != null) {
                        bmp = AnnotationsView.this.videoRenderer.captureScreenshot();
                        if (bmp != null) {
                            if (AnnotationsView.this.mListener != null) {
                                AnnotationsView.this.mListener.onScreencaptureReady(bmp);
                            }

                            AnnotationsView.this.addLogEvent("ScreenCapture", "Success");
                        }
                    } else {
                        bmp = null;
                        AnnotationsView.this.mListener.onScreencaptureReady(bmp);
                    }
                }

                if (selected) {
                    AnnotationsView.this.setVisibility(0);
                    if (v.getId() == id.picker_color) {
                        AnnotationsView.this.mode = AnnotationsView.Mode.Color;
                        AnnotationsView.this.mToolbar.bringToFront();
                    } else {
                        if (v.getId() == id.type_tool) {
                            AnnotationsView.this.mode = AnnotationsView.Mode.Text;
                        }

                        if (v.getId() == id.draw_freehand) {
                            AnnotationsView.this.mode = AnnotationsView.Mode.Pen;
                        }
                    }
                } else {
                    AnnotationsView.this.mode = null;
                }

                if (!AnnotationsView.this.loaded) {
                    AnnotationsView.this.resize();
                    AnnotationsView.this.loaded = true;
                }

                if (AnnotationsView.this.mode != null) {
                    AnnotationsView.this.mListener.onAnnotationsSelected(AnnotationsView.this.mode);
                }

            }
        });
    }

    public void onColorSelected(int color) {
        this.addLogEvent("PickerColor", "Attempt");
        this.mCurrentColor = color;
        this.mSelectedColor = color;
        this.addLogEvent("PickerColor", "Success");
    }

    public void onSignalReceived(final SignalInfo signalInfo, boolean isSelfSignal) {
        Log.i(LOG_TAG, "Signal info: " + signalInfo.mSignalName);
        ((Activity)this.mContext).runOnUiThread(new Runnable() {
            public void run() {
                if (!AnnotationsView.this.loaded) {
                    AnnotationsView.this.resize();
                    AnnotationsView.this.loaded = true;
                }

                String cid = signalInfo.mSrcConnId;
                String mycid = signalInfo.mDstConnId;
                if (!cid.equals(mycid)) {
                    Log.i(AnnotationsView.LOG_TAG, "Incoming annotation");
                    AnnotationsView.this.setVisibility(0);
                    if (!AnnotationsView.this.loaded) {
                        AnnotationsView.this.resize();
                        AnnotationsView.this.loaded = true;
                    }

                    if (signalInfo.mSignalName != null && signalInfo.mSignalName.contains(AnnotationsView.Mode.Pen.toString())) {
                        Log.i(AnnotationsView.LOG_TAG, "New pen annotations is received");
                        AnnotationsView.this.penAnnotations(signalInfo.mSrcConnId, signalInfo.mData.toString());
                    } else if (signalInfo.mSignalName.equalsIgnoreCase(AnnotationsView.Mode.Undo.toString())) {
                        Log.i(AnnotationsView.LOG_TAG, "New undo annotations is received");
                        AnnotationsView.this.mode = AnnotationsView.Mode.Undo;
                        AnnotationsView.this.undoAnnotation(true, cid);
                    } else if (signalInfo.mSignalName.equalsIgnoreCase(AnnotationsView.Mode.Clear.toString())) {
                        Log.i(AnnotationsView.LOG_TAG, "New clear annotations is received");
                        AnnotationsView.this.mode = AnnotationsView.Mode.Clear;
                        AnnotationsView.this.clearAll(true, cid);
                    } else if (signalInfo.mSignalName.equalsIgnoreCase(AnnotationsView.Mode.Text.toString())) {
                        Log.i(AnnotationsView.LOG_TAG, "New text annotations is received");

                        try {
                            AnnotationsView.this.textAnnotation(cid, signalInfo.mData.toString());
                        } catch (Exception var5) {
                            Log.e(AnnotationsView.LOG_TAG, var5.toString());
                        }
                    } else if (signalInfo.mSignalName.equalsIgnoreCase("otAnnotation_requestPlatform")) {
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("platform", "android");
                            AnnotationsView.this.mSession.sendSignal(new SignalInfo(AnnotationsView.this.mSession.getConnection().getConnectionId(), (String)null, "otAnnotation_mobileScreenShare", jsonObject), (Connection)null);
                        } catch (JSONException var4) {
                            var4.printStackTrace();
                        }
                    }
                }

            }
        });
    }

    public static enum Mode {
        Pen("otAnnotation_pen"),
        Undo("otAnnotation_undo"),
        Clear("otAnnotation_clear"),
        Text("otAnnotation_text"),
        Color("otAnnotation_color"),
        Capture("otAnnotation_capture");

        private String type;

        private Mode(String type) {
            this.type = type;
        }

        public String toString() {
            return this.type;
        }
    }

    public interface AnnotationsListener {
        void onScreencaptureReady(Bitmap var1);

        void onAnnotationsSelected(AnnotationsView.Mode var1);

        void onAnnotationsDone();

        void onError(String var1);
    }
}
