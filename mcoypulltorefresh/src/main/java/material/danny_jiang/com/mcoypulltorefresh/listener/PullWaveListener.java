package material.danny_jiang.com.mcoypulltorefresh.listener;

import material.danny_jiang.com.mcoypulltorefresh.refresh.BaseRefreshLayout;

/**
 * Created by axing on 16/8/2.
 * wave回调监听
 */
public interface PullWaveListener {
    /**
     * 下拉中
     * @param refreshLayout
     * @param fraction
     */
    void onPulling(BaseRefreshLayout refreshLayout, float fraction);

    /**
     * 下拉松开
     * @param refreshLayout
     * @param fraction
     */
    void onPullReleasing(BaseRefreshLayout refreshLayout, float fraction);
}
