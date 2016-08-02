package material.danny_jiang.com.mcoypulltorefresh.refresh;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;

import material.danny_jiang.com.mcoypulltorefresh.R;
import material.danny_jiang.com.mcoypulltorefresh.listener.PullAnimationListener;
import material.danny_jiang.com.mcoypulltorefresh.listener.PullWaveListener;
import material.danny_jiang.com.mcoypulltorefresh.utils.DensityUtil;
import material.danny_jiang.com.mcoypulltorefresh.view.RippleView;
import material.danny_jiang.com.mcoypulltorefresh.view.RoundDotView;
import material.danny_jiang.com.mcoypulltorefresh.view.RoundProgressView;
import material.danny_jiang.com.mcoypulltorefresh.view.WaveView;

/**
 * Created by axing on 16/8/2.
 */
public class RadarRefreshLayout extends BaseRefreshLayout {
    private float waveHeight = 180;
    private float headHeight = 100;

    private WaveView waveView;
    private RippleView rippleView;
    private RoundDotView mRoundDotView;
    private RoundProgressView mRoundProgressView;

    private Handler mHandler;
    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public RadarRefreshLayout(Context context) {
        this(context, null, 0);
    }

    public RadarRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RadarRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        /**
         * 初始化headView
         */
        final View headView = LayoutInflater.from(getContext()).inflate(R.layout.view_head, null);
        waveView = (WaveView) headView.findViewById(R.id.draweeView);
        rippleView = (RippleView) headView.findViewById(R.id.ripple);
        mRoundDotView = (RoundDotView) headView.findViewById(R.id.round1);
        mRoundProgressView = (RoundProgressView) headView.findViewById(R.id.round2);

        mRoundProgressView.setVisibility(View.GONE);
        mRoundProgressView.setProListener(new RoundProgressView.Prolistener() {
            @Override
            public void onFinish() {
                rippleView.startReveal();
                mRoundProgressView.animate().scaleX((float) 0.0);
                mRoundProgressView.animate().scaleY((float) 0.0);
            }
        });
        rippleView.setRippleListener(new RippleView.RippleListener() {
            @Override
            public void onRippleFinish() {
                if (mHandler != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            finishRefreshing();
                        }
                    });
                } else {
                    throw new IllegalArgumentException("Handler must not be null");
                }
            }
        });
        /**
         * 设置波浪的高度
         */
        setWaveHeight(DensityUtil.dip2px(getContext(), waveHeight));
        /**
         * 设置headView的高度
         */
        setHeaderHeight(DensityUtil.dip2px(getContext(), headHeight));
        /**
         * 设置headView
         */
        setHeaderView(headView);
        /**
         * 监听波浪变化监听
         */
        setPullWaveListener(new PullWaveListener() {
            @Override
            public void onPulling(BaseRefreshLayout refreshLayout, float fraction) {
                float headW = DensityUtil.dip2px(getContext(), waveHeight);
                waveView.setHeadHeight((int) (DensityUtil.dip2px(getContext(), headHeight) * limitValue(1, fraction)));
                waveView.setWaveHeight((int) (headW * Math.max(0, fraction - 1)));
                waveView.invalidate();

                /*处理圈圈**/
                mRoundDotView.setCir_x((int) (30 * limitValue(1, fraction)));
                mRoundDotView.invalidate();
                mRoundDotView.setVisibility(View.VISIBLE);
                mRoundProgressView.setVisibility(View.GONE);
                mRoundProgressView.animate().scaleX((float) 0.1);
                mRoundProgressView.animate().scaleY((float) 0.1);
            }

            @Override
            public void onPullReleasing(BaseRefreshLayout refreshLayout, float fraction) {
                if (!refreshLayout.isRefreshing) {
                    mRoundDotView.setCir_x((int) (30 * limitValue(1, fraction)));
                    mRoundDotView.invalidate();
                }
            }
        });

        /**
         * 设置松开后的动画监听
         */
        setPullAnimationListener(new PullAnimationListener() {
            @Override
            public void onStartAnimation(BaseRefreshLayout baseRefreshLayout) {
                startWaveAnimation();

                startCircleAnimation();

            }
        });

    }

    /**
     * 执行圆圈方法的动画
     */
    private void startCircleAnimation() {
        /*处理圈圈**/
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(1, 0);
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (mPullToRefreshListener != null) {
                    mPullToRefreshListener.onStartRefresh(RadarRefreshLayout.this);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mRoundDotView.setVisibility(GONE);
                mRoundProgressView.setVisibility(View.VISIBLE);
                mRoundProgressView.animate().scaleX((float) 1.0);
                mRoundProgressView.animate().scaleY((float) 1.0);
                mRoundProgressView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRoundProgressView.setAnimStart();
                    }
                }, 300);
            }
        });

        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mRoundDotView.setCir_x((int) (-value * 40));
                mRoundDotView.invalidate();
            }

        });
        valueAnimator.setInterpolator(new AccelerateInterpolator());
        valueAnimator.setDuration(300);
        valueAnimator.start();
    }

    /**
     * 执行贝塞尔波浪动画效果
     */
    private void startWaveAnimation() {
        waveView.setHeadHeight((int) (DensityUtil.dip2px(getContext(), headHeight)));
        ValueAnimator waveAnimator = ValueAnimator.ofInt(waveView.getWaveHeight(), 0, -300, 0, -100, 0);
        waveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Log.i("anim", "value--->" + (int) animation.getAnimatedValue());
                waveView.setWaveHeight((int) animation.getAnimatedValue());
                waveView.invalidate();

            }
        });

        waveAnimator.setInterpolator(new BounceInterpolator());
        waveAnimator.setDuration(1000);
        waveAnimator.start();
    }

    public void stopRefresh() {
        mRoundProgressView.finishLoading();
    }

    /**
     * 限定值
     * @param a
     * @param b
     * @return
     */
    public float limitValue(float a, float b) {
        float valve = 0;
        final float min = Math.min(a, b);
        final float max = Math.max(a, b);
        valve = valve > min ? valve : min;
        valve = valve < max ? valve : max;
        return valve;
    }
}
