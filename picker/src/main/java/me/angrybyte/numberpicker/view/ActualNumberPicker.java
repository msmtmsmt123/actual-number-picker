
package me.angrybyte.numberpicker.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import me.angrybyte.numberpicker.BuildConfig;
import me.angrybyte.numberpicker.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A horizontal number picker widget. Every aspect of the view is configurable, for more information see the view's attribute set
 * (everything is self-explanatory).
 */
public class ActualNumberPicker extends View {

    // @formatter:off
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ CONTROL_NONE, ARR_LEFT, ARR_RIGHT, FAST_ARR_LEFT, FAST_ARR_RIGHT })
    public @interface Control {} // @formatter:on

    private static final String TAG = ActualNumberPicker.class.getSimpleName();
    private static final int DEFAULT_BAR_COUNT = 11;
    private static final int CONTROL_NONE = 0x00;
    private static final int ARR_LEFT = 0xC1;
    private static final int ARR_RIGHT = 0xC2;
    private static final int FAST_ARR_LEFT = 0xF1;
    private static final int FAST_ARR_RIGHT = 0xF2;
    private static final int CONTROL_TEXT = 0xAA;
    private static final int ANIMATION_LENGTH = 300;

    private Rect mTextBounds = new Rect(0, 0, 0, 0);
    private Point mTextDimens = new Point(0, 0);
    private TextPaint mTextPaint;
    private float mTextSize = -1.0f;
    private boolean mShowText = true;

    private Paint mBarPaint;
    private RectF mBarBounds = new RectF(0, 0, 0, 0);
    private int mBarCount = DEFAULT_BAR_COUNT;
    private int mMinBarWidth = 1;
    private int mBarWidth = mMinBarWidth;
    private boolean mShowBars = true;

    private float mDensityFactor = 1;
    private float mLastX = Float.MAX_VALUE;
    private float mDelta = 0;

    private int mControlsColor = Color.DKGRAY;
    private boolean mShowControls = true;

    private int mFastControlsColor = Color.DKGRAY;
    private boolean mShowFastControls = true;

    private int mMinHeight = 0;
    private int mWidth = 0;
    private int mHeight = 0;

    private int mMinValue = 0;
    private int mMaxValue = 1000;
    private int mValue = 50;

    @Control // one of the constants from the top
    private int mSelectedControl = CONTROL_NONE;
    private int mMaxControlSize = mMinHeight;
    private int mMaxSelectionRadius = mMaxControlSize;
    private long mSelectionAnimationStart = 0;
    private int mCurControlSize = 0;
    private Paint mSelectionPaint;

    private Handler mHandler;
    private SparseArray<Drawable> mControlIcons = new SparseArray<>(4);
    private SparseArray<Point> mControlsCX = new SparseArray<>(4); // centerX and centerY for all selection ripples

    public ActualNumberPicker(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public ActualNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public ActualNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ActualNumberPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Initializes the view from any constructor, utilizing the theme engine, and using the assigned attributes.
     *
     * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style resource that supplies default values for
     *            the view. Can be 0 to not look for defaults
     * @param defStyleRes A resource identifier of a style resource that supplies default values for the view, used only if defStyleAttr is
     *            0 or can not be found in the theme. Can be 0 to not look for defaults
     */
    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mHandler = new Handler();
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ActualNumberPicker, defStyleAttr, defStyleRes);

        mShowBars = attributes.getBoolean(R.styleable.ActualNumberPicker_show_bars, true);
        mShowControls = attributes.getBoolean(R.styleable.ActualNumberPicker_show_controls, true);
        mShowFastControls = attributes.getBoolean(R.styleable.ActualNumberPicker_show_fast_controls, true);

        int barsColor = attributes.getColor(R.styleable.ActualNumberPicker_bar_color, Color.DKGRAY);
        mBarPaint = new Paint();
        mBarPaint.setAntiAlias(true);
        mBarPaint.setStyle(Paint.Style.FILL);
        mBarPaint.setColor(barsColor);

        int mControlsColor = attributes.getColor(R.styleable.ActualNumberPicker_controls_color, Color.DKGRAY);
        int mFastControlsColor = attributes.getColor(R.styleable.ActualNumberPicker_fast_controls_color, Color.DKGRAY);

        int controlsSelectionColor = attributes.getColor(R.styleable.ActualNumberPicker_selection_color, 0xB0444444);
        mSelectionPaint = new Paint();
        mSelectionPaint.setAntiAlias(true);
        mSelectionPaint.setStyle(Paint.Style.FILL);
        mSelectionPaint.setColor(controlsSelectionColor);

        int textColor = attributes.getColor(R.styleable.ActualNumberPicker_text_color, Color.DKGRAY);
        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setLinearText(true);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mTextPaint.setHinting(Paint.HINTING_ON);
        }
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(textColor);

        mTextSize = attributes.getDimension(R.styleable.ActualNumberPicker_text_size, mTextSize);
        if (mTextSize != -1.0f) {
            mTextPaint.setTextSize(mTextSize);
        }

        mShowText = attributes.getBoolean(R.styleable.ActualNumberPicker_show_text, mShowText);

        mMinValue = attributes.getInt(R.styleable.ActualNumberPicker_min_value, mMinValue);
        mMaxValue = attributes.getInt(R.styleable.ActualNumberPicker_max_value, mMaxValue);
        if (mMaxValue <= mMinValue) {
            throw new RuntimeException("Cannot use max_value " + mMaxValue + " because the min_value is " + mMinValue);
        }

        mValue = attributes.getInt(R.styleable.ActualNumberPicker_value, (mMaxValue + mMinValue) / 2);
        if (mValue < mMinValue || mValue > mMaxValue) {
            throw new RuntimeException("Cannot use value " + mValue + " because it is out of range");
        }

        mBarCount = attributes.getInteger(R.styleable.ActualNumberPicker_bars_count, DEFAULT_BAR_COUNT);
        if (mBarCount < 3) {
            mBarCount = DEFAULT_BAR_COUNT;
        }

        mMinBarWidth = context.getResources().getDimensionPixelSize(R.dimen.min_bar_width);
        mBarWidth = attributes.getDimensionPixelSize(R.styleable.ActualNumberPicker_bar_width, mMinBarWidth);
        if (mBarWidth < mMinBarWidth) {
            mBarWidth = mMinBarWidth;
        }

        attributes.recycle();

        // noinspection deprecation
        mControlIcons.put(ARR_LEFT, context.getResources().getDrawable(R.drawable.ic_keyboard_arrow_left_black_24dp));
        // noinspection deprecation
        mControlIcons.put(ARR_RIGHT, context.getResources().getDrawable(R.drawable.ic_keyboard_arrow_right_black_24dp));
        // noinspection deprecation
        mControlIcons.put(FAST_ARR_LEFT, context.getResources().getDrawable(R.drawable.ic_keyboard_2arrows_left_black_24dp));
        // noinspection deprecation
        mControlIcons.put(FAST_ARR_RIGHT, context.getResources().getDrawable(R.drawable.ic_keyboard_2arrows_right_black_24dp));
        mControlIcons.get(ARR_LEFT).setColorFilter(mControlsColor, PorterDuff.Mode.SRC_ATOP);
        mControlIcons.get(ARR_RIGHT).setColorFilter(mControlsColor, PorterDuff.Mode.SRC_ATOP);
        mControlIcons.get(FAST_ARR_LEFT).setColorFilter(mFastControlsColor, PorterDuff.Mode.SRC_ATOP);
        mControlIcons.get(FAST_ARR_RIGHT).setColorFilter(mFastControlsColor, PorterDuff.Mode.SRC_ATOP);

        // update density and metrics/dimensions
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int density; // LDPI is 120
        DisplayMetrics metrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            manager.getDefaultDisplay().getRealMetrics(metrics);
        } else {
            manager.getDefaultDisplay().getMetrics(metrics);
        }
        density = metrics.densityDpi;
        mDensityFactor = density / DisplayMetrics.DENSITY_LOW; // will be 1, 1.2, 1.5... etc

        mMinHeight = context.getResources().getDimensionPixelSize(R.dimen.min_height);
        mMaxControlSize = context.getResources().getDimensionPixelSize(R.dimen.control_size);
        updateControlPositions();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int height;

        if (heightMode == MeasureSpec.EXACTLY) {
            // respect min_height value
            height = Math.max(mMinHeight, heightSize);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            // take whichever is smaller, height <-> parent height
            if (mHeight == 0) {
                // no calculations yet, use min_height
                height = Math.min(mMinHeight, heightSize);
            } else {
                // secondary pass, already calculated height, so use that
                height = Math.min(mHeight, heightSize);
            }
        } else {
            // doesn't matter
            height = Math.max(mMinHeight, heightSize);
        }

        mHeight = height;
        mWidth = calculateWidth(widthSize, widthMode, mHeight); // fast_controls x2, controls x2, text
        mMaxControlSize = Math.min(mHeight, mMaxControlSize);

        // MUST CALL THIS
        setMeasuredDimension(mWidth, mHeight);
        updateTextSize();
        updateControlPositions();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        Log.d(TAG, "Size changing... " + oldW + "x" + oldH + " -> " + w + "x" + h);
        mHeight = Math.max(h, mHeight);
        mWidth = calculateWidth(w, MeasureSpec.EXACTLY, mHeight);
        mMaxControlSize = Math.min(mHeight, mMaxControlSize);
        updateTextSize();
        updateControlPositions();
        super.onSizeChanged(mWidth, mHeight, oldW, oldH);
    }

    private void updateControlPositions() {
        // load icon dimensions
        int leftArrW = mControlIcons.get(ARR_LEFT).getMinimumWidth();
        int leftArrH = mControlIcons.get(ARR_LEFT).getMinimumHeight();
        int rightArrW = mControlIcons.get(ARR_RIGHT).getMinimumWidth();
        int rightArrH = mControlIcons.get(ARR_RIGHT).getMinimumHeight();

        mMaxSelectionRadius = mHeight > mMinHeight ? mMaxControlSize : mMinHeight;

        // left arrow selection ripple
        if (mControlsCX.get(ARR_LEFT) == null) {
            mControlsCX.put(ARR_LEFT, new Point());
        }
        // mControlsCX.get(ARR_LEFT).x = mWidth / 2 - (mShowText ? mTextBounds.width() : mMaxSelectionRadius) / 2 - mMaxSelectionRadius / 2;
        mControlsCX.get(ARR_LEFT).x = mWidth / 2 - mMaxSelectionRadius / 2 - mMaxSelectionRadius / 2;
        mControlsCX.get(ARR_LEFT).y = mHeight / 2;

        // left arrow drawable bounds
        mControlIcons.get(ARR_LEFT).setBounds(mControlsCX.get(ARR_LEFT).x - leftArrW / 2, mControlsCX.get(ARR_LEFT).y - leftArrH / 2,
                mControlsCX.get(ARR_LEFT).x + leftArrW / 2, mControlsCX.get(ARR_LEFT).y + leftArrH / 2);

        // right arrow selection ripple
        if (mControlsCX.get(ARR_RIGHT) == null) {
            mControlsCX.put(ARR_RIGHT, new Point());
        }
        mControlsCX.get(ARR_RIGHT).x = mWidth / 2 + mMaxSelectionRadius / 2 + mMaxSelectionRadius / 2;
        mControlsCX.get(ARR_RIGHT).y = mHeight / 2;

        // right arrow drawable bounds
        mControlIcons.get(ARR_RIGHT).setBounds(mControlsCX.get(ARR_RIGHT).x - rightArrW / 2, mControlsCX.get(ARR_LEFT).y - rightArrH / 2,
                mControlsCX.get(ARR_RIGHT).x + rightArrW / 2, mControlsCX.get(ARR_LEFT).y + rightArrH / 2);
    }

    /**
     * Does necessary calculations to ensure there is enough space for all components (value, right/left controls, and fast controls).<br>
     * Width required is:
     * <ol>
     * <li><b>When all controls are shown</b> - Minimum 5x bigger than the height</li>
     * <li><b>When only one set of controls is shown</b> (right/left OR fast controls) - Minimum 3x bigger than the height</li>
     * <li><b>When no controls are shown</b> - Either a fixed width, a parent-matching width or minimum 5x bigger than the height</li>
     * </ol>
     *
     * @param requestedWidth A value that was requested for width (by {@link #onSizeChanged(int, int, int, int)} or by
     *            {@link #onMeasure(int, int)}), in pixels
     * @param requestedWidthMode An integer constant from {@link android.view.View.MeasureSpec} that declares measuring mode
     * @param height Pre-defined height of the view, in pixels
     * @return Calculated width of the view, in pixels
     */
    private int calculateWidth(int requestedWidth, int requestedWidthMode, int height) {
        if (mShowControls && mShowFastControls) {
            return height * 5;
        } else if (!mShowControls && !mShowFastControls) {
            if (requestedWidthMode == MeasureSpec.EXACTLY) {
                return requestedWidth;
            } else if (requestedWidthMode == MeasureSpec.AT_MOST) {
                return requestedWidth;
            } else {
                return height * 5;
            }
        } else {
            // only one of control sets is visible
            return height * 3;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            updateTextSize();
        }
    }

    /**
     * This sets the optimal text size for the value number. If there is a predefined {@link #mTextSize}, then that value is used. If there
     * is no defined value for text size, this method calculated the optimal size, which is around 60% of the View's height.
     */
    private void updateTextSize() {
        if (mTextSize != -1.0f) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Not calculating text size, a predefined value is set: " + mTextPaint.getTextSize());
            }
            return;
        }

        float size = 14f; // 14px on LDPI x system font factor
        Rect bounds = new Rect(0, 0, 0, 0);
        mTextPaint.setTextSize(size);
        mTextPaint.getTextBounds("00", 0, 1, bounds);

        // this loop exits when text size becomes too big
        while (bounds.height() < mHeight - mHeight * 0.4f) {
            mTextPaint.setTextSize(size++);
            mTextPaint.getTextBounds("AA", 0, 1, bounds);
        }
    }

    /**
     * Updates the indicator text size. Set to {@code -1.0f} to use maximum sized text.
     *
     * @param size A dimension representing the text size
     */
    public void setTextSize(float size) {
        mTextSize = size;
        if (mTextSize != -1.0f) {
            mTextPaint.setTextSize(size);
        }
        updateTextSize();
    }

    /**
     * Measures the given text and saves dimensions to the {@link #mTextDimens} field.
     *
     * @param text Which text to measure
     */
    private void measureText(String text) {
        // accurate measure for height
        mTextPaint.getTextBounds(text, 0, text.length(), mTextBounds);
        mTextDimens.y = Math.abs(mTextBounds.height());

        // accurate measure for width
        mTextDimens.x = (int) Math.floor(mTextPaint.measureText(text));
        // update text bounds, will need it for later
        mTextBounds.set(mTextBounds.left, mTextBounds.top, mTextBounds.left + mTextDimens.x, mTextBounds.bottom);
    }

    /**
     * Checks whether the given [x, y] point fits into the hit point rectangle for any of the controls.
     * 
     * @param x Where is the finger on the X-axis
     * @param y Where is the finger on the Y-axis
     * @return Identifier of the control if the pointer is 'touching' it, or {@link #CONTROL_NONE} if not
     */
    @Control
    private int isTouchingControls(float x, float y) {
        for (int i = 0; i < mControlsCX.size(); i++) {
            int key = mControlsCX.keyAt(i);
            boolean insideX = x > mControlsCX.get(key).x - mMaxSelectionRadius / 2 && x < mControlsCX.get(key).x + mMaxSelectionRadius / 2;
            boolean insideY = y > mControlsCX.get(key).y - mMaxSelectionRadius / 2 && y < mControlsCX.get(key).y + mMaxSelectionRadius / 2;
            if (insideX && insideY) {
                // noinspection ResourceType
                return key;
            }
        }
        return CONTROL_NONE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                getParent().requestDisallowInterceptTouchEvent(true);
                mLastX = event.getX();

                mSelectedControl = isTouchingControls(event.getX(), event.getY());
                if (mSelectedControl != CONTROL_NONE) {
                    mSelectionAnimationStart = System.currentTimeMillis();
                    mCurControlSize = 0;

                    mHandler.removeCallbacks(mAnimationUpdater);
                    mHandler.post(mAnimationUpdater);
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                float percent = event.getX() / (float) mWidth;
                int oldValue = mValue;
                mValue = (int) Math.floor(percent * mMaxValue + mMinValue);
                if (mValue < mMinValue) {
                    mValue = mMinValue;
                } else if (mValue > mMaxValue) {
                    mValue = mMaxValue;
                }

                if (mValue != oldValue) {
                    float thisDelta = mLastX - event.getX();
                    mLastX = event.getX();
                    // 'minus' because we want to go in the opposite direction
                    mDelta -= thisDelta / (mDensityFactor / 2f);
                }

                mHandler.removeCallbacks(mAnimationUpdater);
                mHandler.post(mAnimationUpdater);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                getParent().requestDisallowInterceptTouchEvent(false);
                mLastX = Float.MAX_VALUE;

                mSelectedControl = CONTROL_NONE;
                mSelectionAnimationStart = Long.MAX_VALUE;
                mCurControlSize = 0;

                mHandler.removeCallbacks(mAnimationUpdater);
                mHandler.post(mAnimationUpdater);
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    /**
     * Penner's linear easing function, plotted by time and distance for a motion tween. Can be used for density, width and other properties
     * that should behave the same.
     *
     * @param b The beginning value of the property
     * @param t The current time (or position) of the tween. This can be seconds or frames, steps, ms, whatever – as long as the unit is the
     *            same as is used for the total time
     * @param d The total time of the tween
     * @param c The change between the beginning and destination value of the property
     * @return The new value that has resulted from the equation
     */
    private float linear(float t, float b, float c, float d) {
        return c * (t / d) + b;
    }

    /**
     * Penner's sine easing in function, plotted by time and distance for a motion tween. Can be used for density, width and other
     * properties that should behave the same.
     *
     * @param b The beginning value of the property
     * @param t The current time (or position) of the tween. This can be seconds or frames, steps, ms, whatever – as long as the unit is the
     *            same as is used for the total time
     * @param d The total time of the tween
     * @param c The change between the beginning and destination value of the property
     * @return The new value that has resulted from the equation
     */
    private float easeIn(float t, float b, float c, float d) {
        return -c * (float) Math.cos(t / d * (Math.PI / 2)) + c + b;
    }

    /**
     * Penner's sine easing out function, plotted by time and distance for a motion tween. Can be used for density, width and other
     * properties that should behave the same.
     *
     * @param b The beginning value of the property
     * @param t The current time (or position) of the tween. This can be seconds or frames, steps, ms, whatever – as long as the unit is the
     *            same as is used for the total time
     * @param d The total time of the tween
     * @param c The change between the beginning and destination value of the property
     * @return The new value that has resulted from the equation
     */
    private float easeOut(float t, float b, float c, float d) {
        return c * (float) Math.sin(t / d * (Math.PI / 2)) + b;
    }

    /**
     * Penner's sine easing in and out function, plotted by time and distance for a motion tween. Can be used for density, width and other
     * properties that should behave the same.
     *
     * @param t The current time (or position) of the tween. This can be seconds or frames, steps, ms, whatever – as long as the unit is the
     *            same as is used for the total time
     * @param b The beginning value of the property
     * @param c The change between the beginning and destination value of the property
     * @param d The total time of the tween
     * @return The new value that has resulted from the equation
     */
    private double easeInOut(float t, float b, float c, float d) {
        return -c / 2 * (Math.cos(Math.PI * t / d) - 1) + b;
    }

    /**
     * Repositions the X coordinate back inside the {@code [0-containerW]} range. If X gets bigger than {@code containerW} then it is
     * repositioned to the left, symmetrically to the (X:{@code containerW / 2}) line. Analogously, if X gets smaller than {@code 0} then it
     * is repositioned to the right, symmetrically to the (X:{@code containerW / 2}) line.
     *
     * @param linearBarX Where is the X coordinate now (prior to reposition)
     * @param containerW How wide is the container
     * @return The repositioned X value, which will be inside the {@code [0-containerW]} range
     */
    public float repositionInside(float linearBarX, int containerW) {
        if (linearBarX < 0) {
            return containerW - (-linearBarX % containerW);
        } else {
            return linearBarX % containerW;
        }
    }

    /**
     * Checks whether the text overlaps the bar that is about to be drawn.<br>
     * <b>Note</b>: This method fakes the text width, i.e. increases it to allow for some horizontal padding.
     *
     * @param textBounds Which text bounds to measure
     * @param barBounds Which bar bounds to measure
     * @return {@code True} if bounds overlap each other, {@code false} if not
     */
    private boolean textOverlapsBars(Rect textBounds, RectF barBounds) {
        // increase original text width to give some padding to the text
        int textL = textBounds.left - (int) Math.floor(textBounds.width() * 0.6f);
        int textR = textBounds.right + (int) Math.floor(textBounds.width() * 0.6f);
        int textT = textBounds.top;
        int textB = textBounds.bottom;
        return barBounds.intersects(textL, textT, textR, textB);
    }

    /**
     * Calculates how high the bar needs to be for the given X coordinate. Higher ones appear near the middle, i.e. when X is near the 1/2
     * of the container width
     *
     * @param minHeight Minimum allowed height of the bar
     * @param maxHeight Maximum allowed height of the bar
     * @param barX Where is the bar located on the X-axis
     * @param containerWidth How wide is the view container
     * @return Correct, scaled height of the given bar (determined by the index parameter)
     */
    private int calculateBarHeight(@IntRange(from = 0) int minHeight, @IntRange(from = 0) int maxHeight, float barX, int containerWidth) {
        float height;
        if (barX <= containerWidth / 2) {
            height = easeOut(barX, minHeight, maxHeight - minHeight, containerWidth / 2f);
        } else {
            height = easeIn(barX - containerWidth / 2f, maxHeight, minHeight - maxHeight, containerWidth / 2f);
        }
        return (int) Math.floor(height);
    }

    /**
     * Calculates how transparent the bar needs to be for the given X coordinate. More opaque ones appear near the middle, i.e. when X is
     * near the 1/2 of the container width.
     *
     * @param minOpacity Minimum allowed opacity of the bar (must be between 0 and 255)
     * @param maxOpacity Maximum allowed opacity of the bar (must be between 0 and 255)
     * @param barX Where is the bar located on the X-axis
     * @param containerWidth How wide is the view container
     * @return Correct, scaled height of the given bar (determined by the index parameter)
     */
    private int calculateBarOpacity(@IntRange(from = 0, to = 255) int minOpacity, @IntRange(from = 0, to = 255) int maxOpacity, float barX,
            int containerWidth) {
        float height;
        if (barX <= containerWidth / 2) {
            height = easeOut(barX, minOpacity, maxOpacity - minOpacity, containerWidth / 2f);
        } else {
            height = easeIn(barX - containerWidth / 2f, maxOpacity, minOpacity - maxOpacity, containerWidth / 2f);
        }
        return (int) Math.floor(height);
    }

    /**
     * A periodic updater for animations. This should be kept clean, as it forces a call to the {@link #onDraw(Canvas)} method.
     */
    private Runnable mAnimationUpdater = new Runnable() {
        @Override
        public void run() {
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mShowText) {
            String value = String.valueOf((int) Math.floor(mValue));
            // this will save dimensions to mTextDimens
            measureText(value);
            int x = mWidth / 2 - mTextDimens.x / 2;
            int y = mHeight / 2 + mTextDimens.y / 2;
            canvas.drawText(value, x, y, mTextPaint);
            // update bounds to re-use later
            mTextBounds.set(x, y, x + mTextBounds.width(), y + mTextBounds.height());
        }

        if (mShowBars) {
            // draw all bars, but draw one more in the end with '<=' instead of '<' (to be symmetric)
            int opacity, barH;
            float linearX, insideX, x, y;
            int maxBarH = (int) Math.floor(0.5f * mHeight);
            int minBarH = (int) Math.floor(maxBarH * 0.95f);
            int minOpacity = 50;
            for (int i = 0; i <= mBarCount; i++) {
                // calculate bar X coordinate
                linearX = mDelta + (float) i / (float) mBarCount * (float) mWidth;
                insideX = repositionInside(linearX, mWidth);
                x = (float) Math.floor(easeInOut(insideX, 0f, 1f, mWidth) * mWidth);
                // calculate bar height
                barH = calculateBarHeight(minBarH, maxBarH, x, mWidth);
                // calculate Y coordinate
                y = mHeight / 2 - barH / 2;
                // don't draw if it overlaps the text
                mBarBounds.set(x - mBarWidth / 2f, y, x + mBarWidth, y + barH);
                if (!mShowText || !textOverlapsBars(mTextBounds, mBarBounds)) {
                    opacity = calculateBarOpacity(minOpacity, 255, x, mWidth);
                    mBarPaint.setAlpha(opacity);
                    canvas.drawRoundRect(mBarBounds, mBarBounds.width() / 3f, mBarBounds.width() / 3f, mBarPaint);
                }
            }
        }

        if (mShowControls) {
            float animationProgress = easeOut(System.currentTimeMillis() - mSelectionAnimationStart, 0f, 1f,
                    mSelectionAnimationStart + ANIMATION_LENGTH);
            int alpha = (int) Math.floor((255f - animationProgress * 255f));

            if (mSelectedControl == ARR_LEFT) {
                // left arrow ripple
                mSelectionPaint.setAlpha(alpha);
                mCurControlSize = (int) Math.floor(animationProgress * mMaxSelectionRadius / 2f);
                canvas.drawCircle(mControlsCX.get(ARR_LEFT).x, mControlsCX.get(ARR_LEFT).y, mCurControlSize, mSelectionPaint);

                if (animationProgress < 1f) {
                    mHandler.removeCallbacks(mAnimationUpdater);
                    mHandler.post(mAnimationUpdater);
                }
            } else if (mSelectedControl == ARR_RIGHT) {
                // right arrow ripple
                mSelectionPaint.setAlpha(alpha);
                mCurControlSize = (int) Math.floor(animationProgress * mMaxSelectionRadius / 2f);
                canvas.drawCircle(mControlsCX.get(ARR_RIGHT).x, mControlsCX.get(ARR_RIGHT).y, mCurControlSize, mSelectionPaint);

                if (animationProgress < 1f) {
                    mHandler.removeCallbacks(mAnimationUpdater);
                    mHandler.post(mAnimationUpdater);
                }
            } else {
                // no normal control selected (still fast controls may be)
                mSelectionPaint.setAlpha(255);
                mCurControlSize = 0;
            }

            mControlIcons.get(ARR_LEFT).draw(canvas);
            mControlIcons.get(ARR_RIGHT).draw(canvas);
        }

        // TODO: draw fast controls

    }

}
