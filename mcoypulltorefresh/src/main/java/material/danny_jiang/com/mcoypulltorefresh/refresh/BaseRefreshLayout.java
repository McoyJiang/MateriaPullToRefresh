package material.danny_jiang.com.mcoypulltorefresh.refresh;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import material.danny_jiang.com.mcoypulltorefresh.listener.PullAnimationListener;
import material.danny_jiang.com.mcoypulltorefresh.listener.PullToRefreshListener;
import material.danny_jiang.com.mcoypulltorefresh.listener.PullWaveListener;

/**
 * Created by Mcoy
 * 主要用来出来滑动事件，并提供PullToRefreshListener接口
 * 如果要自定义相应效果的下拉刷新可以继承此基类，然后实现PullToRefreshListener
 */
public class BaseRefreshLayout extends FrameLayout {
    //波浪的高度
    protected float mWaveHeight;

    //头部的高度
    protected float mHeadHeight;

    //子控件
    private View mChildView;

    //头部layout
    protected FrameLayout mHeadLayout;

    //刷新的状态
    protected boolean isRefreshing;

    //触摸获得Y的位置
    private float mTouchY;

    //当前Y的位置
    private float mCurrentY;

    //动画的变化率
    private DecelerateInterpolator decelerateInterpolator;


    public BaseRefreshLayout(Context context) {
        this(context, null, 0);
    }

    public BaseRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        Log.i("Mcoy", "init");
    }

    /**
     * 初始化
     */
    private void init() {
        //使用isInEditMode解决可视化编辑器无法识别自定义控件的问题
        if (isInEditMode()) {
            return;
        }

        if (getChildCount() > 1) {
            throw new RuntimeException("只能拥有一个子控件哦");
        }

        //在动画开始的地方快然后慢;
        decelerateInterpolator = new DecelerateInterpolator(10);
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.i("Mcoy", "onAttachedToWindow");

        //添加头部
        FrameLayout headViewLayout = new FrameLayout(getContext());
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        layoutParams.gravity = Gravity.TOP;
        headViewLayout.setLayoutParams(layoutParams);

        mHeadLayout = headViewLayout;

        this.addView(mHeadLayout);


        //获得子控件
        mChildView = getChildAt(0);

        if (mChildView == null) {
            return;
        }
        mChildView.animate().setInterpolator(new DecelerateInterpolator());//设置速率为递减
        mChildView.animate().setUpdateListener(//通过addUpdateListener()方法来添加一个动画的监听器
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int height = (int) mChildView.getTranslationY();//获得mChildView当前y的位置

                        Log.i("Mcoy", "mChildView.getTranslationY----------->" + height);
                        mHeadLayout.getLayoutParams().height = height;
                        mHeadLayout.requestLayout();//重绘

                        if (pullWaveListener != null) {
                            pullWaveListener.onPullReleasing(BaseRefreshLayout.this, height / mHeadHeight);
                        }
                    }
                }
        );

    }

    /**
     * 拦截事件
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(isRefreshing) return true;

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchY = ev.getY();
                mCurrentY = mTouchY;
                break;
            case MotionEvent.ACTION_MOVE:
                float currentY = ev.getY();
                float dy = currentY - mTouchY;
                if (dy > 0 && !canChildScrollUp()) {
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * 响应事件
     * @param e
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (isRefreshing) {
            return super.onTouchEvent(e);
        }

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                mCurrentY = e.getY();

                float dy = mCurrentY - mTouchY;
                dy = Math.min(mWaveHeight * 2, dy);
                dy = Math.max(0, dy);

                if (mChildView != null) {
                    float offsetY = decelerateInterpolator.getInterpolation(dy / mWaveHeight / 2) * dy / 2;
                    mChildView.setTranslationY(offsetY);

                    mHeadLayout.getLayoutParams().height = (int) offsetY;
                    mHeadLayout.requestLayout();

                    if (pullWaveListener != null) {
                        pullWaveListener.onPulling(BaseRefreshLayout.this, offsetY / mHeadHeight);
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                /**
                 * 当手指抬起，
                 * 判断是否有子控件，
                 *      yes 则判断子控件当前的Y坐标是否已经大于自身高度(用户下拉距离已经足够)
                 *          yes 则调用PullToRefreshListener的onRefresh方法
                 *          no  则将子控件重新滚动到0(初始位置)
                 *      no 则将子控件重新滚动到0(初始位置)
                 */
                if (mChildView != null) {
                    if (mChildView.getTranslationY() >= mHeadHeight) {
                        //现将子控件的高度固定在当前Y坐标，当刷新完毕之后再重新回到0位置
                        mChildView.animate().translationY(mHeadHeight).start();
                        isRefreshing = true;
                        if (mPullAnimationListener != null) {
                            mPullAnimationListener.onStartAnimation(BaseRefreshLayout.this);
                        } else {
                            mChildView.animate().translationY(0).start();
                        }
                    } else {
                        mChildView.animate().translationY(0).start();
                    }

                }
                return true;
        }
        return super.onTouchEvent(e);
    }

    /**
     * 用来判断是否可以上拉
     * @return boolean
     */
    public boolean canChildScrollUp() {
        if (mChildView == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 14) {
            if (mChildView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mChildView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mChildView, -1) || mChildView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mChildView, -1);
        }
    }

    /**
     * 设置下拉刷新的接口，在此接口中执行耗时操作
     * 当耗时操作执行完毕之后，需要手动调用stopRefresh方法结束刷新的一些列动画
     */
    protected PullToRefreshListener mPullToRefreshListener;
    public void setPullToRefreshListener(PullToRefreshListener pullToRefreshListener) {
        mPullToRefreshListener = pullToRefreshListener;
    }

    /**
     * 设置下拉手抬起后的动画监听
     */
    private PullAnimationListener mPullAnimationListener;
    protected void setPullAnimationListener(PullAnimationListener mPullAnimationListener)
    {
        this.mPullAnimationListener = mPullAnimationListener;
    }

    /**
     * 设置wave监听
     */
    private PullWaveListener pullWaveListener;
    protected void setPullWaveListener(PullWaveListener pullWaveListener)
    {
        this.pullWaveListener = pullWaveListener;
    }

    public void autoRefresh() {
        //现将子控件的高度固定在当前Y坐标，当刷新完毕之后再重新回到0位置
        mChildView.animate().translationY(mHeadHeight).start();
        isRefreshing = true;
        if (mPullAnimationListener != null) {
            mPullAnimationListener.onStartAnimation(BaseRefreshLayout.this);
        } else {
            mChildView.animate().translationY(0).start();
        }
    }

    /**
     * 刷新结束
     */
    public void finishRefreshing() {
        if (mChildView != null) {
            mChildView.animate().translationY(0).start();
        }
        isRefreshing = false;
    }

    /**
     * 设置头部View
     * @param headerView
     */
    public void setHeaderView(final View headerView) {
        post(new Runnable() {
            @Override
            public void run() {
                mHeadLayout.addView(headerView);
            }
        });
    }

    /**
     * 设置wave的下拉高度
     * @param waveHeight
     */
    public void setWaveHeight(float waveHeight) {
       this.mWaveHeight = waveHeight;
    }

    /**
     * 设置下拉头的高度
     * @param headHeight
     */
    public void setHeaderHeight(float headHeight) {
        this.mHeadHeight = headHeight;
    }
}
