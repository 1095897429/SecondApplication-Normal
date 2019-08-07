package niaoge.xiaoyu.router.ui.myzone;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appcpx.nativesdk.category.nativead.NativeListScreen;
import com.appcpx.nativesdk.common.bean.AdSourceBean;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.footer.ClassicsFooter;
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadmoreListener;
import com.uber.autodispose.AutoDispose;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;
import com.yilan.sdk.entity.MediaInfo;
import com.yilan.sdk.ui.custom.CustomListener;
import com.yilan.sdk.ui.custom.FeedExpress;
import com.yilan.sdk.ui.video.VideoActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import butterknife.BindView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import niaoge.xiaoyu.router.BuildConfig;
import niaoge.xiaoyu.router.MainApplication;
import niaoge.xiaoyu.router.R;
import niaoge.xiaoyu.router.common.network.HttpManager;
import niaoge.xiaoyu.router.common.network.MyResult;
import niaoge.xiaoyu.router.common.network.RxCallBack;
import niaoge.xiaoyu.router.common.utils.DisplayUtil;
import niaoge.xiaoyu.router.common.utils.MobClickEvent.MobclickAgentUtils;
import niaoge.xiaoyu.router.common.utils.MobClickEvent.UmengEvent;
import niaoge.xiaoyu.router.common.utils.StringToolKit;
import niaoge.xiaoyu.router.common.utils.UIHelper;
import niaoge.xiaoyu.router.common.widget.GifView;
import niaoge.xiaoyu.router.common.widget.NewsListRecyclerView;
import niaoge.xiaoyu.router.common.widget.dialog.NewsCloseDialog;
import niaoge.xiaoyu.router.common.widget.dialog.NewsRedDialog;
import niaoge.xiaoyu.router.common.widget.smartui.XnClassicsHeader;
import niaoge.xiaoyu.router.ui.base.BaseFragment;
import niaoge.xiaoyu.router.ui.base.Constant;
import niaoge.xiaoyu.router.ui.common.bean.UserFocusInfoBean;
import niaoge.xiaoyu.router.ui.home.bean.NewsListBean;
import niaoge.xiaoyu.router.ui.home.bean.NewsRedBean;
import niaoge.xiaoyu.router.ui.home.bean.NewsRedRewardBean;
import niaoge.xiaoyu.router.ui.home.bean.VideoListBean;
import okhttp3.RequestBody;

/***
 * 关注Fragment的子Item
 */
public class FocusItemFragment extends BaseFragment implements FocusItemAdapter.OnItemClickListener, FocusItemAdapter.OnCloseIconClick {

    @BindView(R.id.gifview)
    GifView gifview;
    @BindView(R.id.backtop)
    ImageView backtop;
    @BindView(R.id.tv_error)
    TextView tvError;
    @BindView(R.id.ll_toast)
    LinearLayout llToast;
    @BindView(R.id.ll_nonews)
    LinearLayout ll_nonews;
    @BindView(R.id.recyclerview)
    NewsListRecyclerView recyclerview;
    @BindView(R.id.swiprefresh)
    SmartRefreshLayout swiprefresh;
    private Context mContext;

    private NewsRedDialog newsRedDialog;
    private NewsCloseDialog mNewsCloseDialog;


    private String chaid;
    private int page = 1;
    private boolean isfresh;


    private List<UserFocusInfoBean> mUserFocusInfoBeanList = new ArrayList<>();

    private List<Object> list = new ArrayList<>();
    private NewsRedBean newsRedBean;
    private int comindex = 0;
    private int advindex = 0;
    private int videoindex = 0;
    private int redindex = 0;

    private FocusItemAdapter adapter;
    private boolean isFristLoad = true;
    private String nid = "";

    public static FocusItemFragment newInstance(Context mContext, String param1) {
        FocusItemFragment fragment = new FocusItemFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FRAGMENT_PARAM1, param1);
        fragment.mContext = mContext;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_focusitem;
    }


    private void getnewslist() {

        Map<String, Object> map = new HashMap<>();
        map.put("chaid", chaid);
        map.put("pageNo", page);
        map = StringToolKit.MapSercet(map);
        RequestBody requestBody = StringToolKit.map2RequestBody(map);
        HttpManager.getApiStoresSingleton().newsList(requestBody)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
                .subscribe(new RxCallBack<MyResult<NewsListBean>>(this.getContext()) {
                    @Override
                    public void onSuccess(MyResult<NewsListBean> newTagListBeanMyResult) {




                        setlist(isfresh);
                        isfresh = false;
                        if (swiprefresh != null) {
                            swiprefresh.finishRefresh(true);
                            swiprefresh.finishLoadmore(true);
                        }
                    }

                    @Override
                    public void onError(MyResult<NewsListBean> newTagListBeanMyResult) {
                        isfresh = false;
                        if (swiprefresh != null) {
                            swiprefresh.finishRefresh(false);
                            swiprefresh.finishLoadmore(false);
                        }

                        if (ll_nonews == null)
                            return;
                        if (list.size() == 0) {
                            ll_nonews.setVisibility(View.VISIBLE);
                        } else {
                            ll_nonews.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFail(Throwable e) {
                        isfresh = false;
                        if (swiprefresh != null) {
                            swiprefresh.finishRefresh(false);
                            swiprefresh.finishLoadmore(false);
                        }

                        if (ll_nonews == null)
                            return;

                        if (list.size() == 0) {
                            ll_nonews.setVisibility(View.VISIBLE);
                        } else {
                            ll_nonews.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFinish() {
                        isfresh = false;
                    }
                });
    }

    private void getAdv(int count) {
        //一般放置在初始化方法中获取数据
        NativeListScreen listScreen = new NativeListScreen();
        List<String> adid = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Random random = new Random();
            int index = random.nextInt(3);

            if (index == 0) {
                adid.add("142");
            } else if (index == 1) {
                adid.add("143");
            } else if (index == 2) {
                adid.add("144");
            }
        }
        if (getActivity() == null)
            return;

    }




    @Override
    public void onLazyInitView(@Nullable Bundle savedInstanceState) {
        super.onLazyInitView(savedInstanceState);
        fristLoadConfig();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        chaid = getArguments().getString(ARG_FRAGMENT_PARAM2);
        if (isVisibleToUser) {

        }

    }

    private void fristLoadConfig() {
        if ("0".equals(chaid))
            getreddatabackground();//获取信息流红包数据
        gethaotulist(3);//第一次加载3个视频  页面2个，预加载1个。

        Random random = new Random();
        int index = random.nextInt(10);

        if (index == 0)
            getAdv(6);//第一次获取6个广告，比第一次展示的广告4个多两个
        else
            getCsjAdvData(6);

    }

    private void getreddatabackground() {

        Map<String, Object> map = new HashMap<>();
        map = StringToolKit.MapSercet(map);
        RequestBody requestBody = StringToolKit.map2RequestBody(map);
        HttpManager.getApiStoresSingleton().userInformRedpckOne(requestBody)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
                .subscribe(new RxCallBack<MyResult<NewsRedBean>>(this.getContext()) {
                    @Override
                    public void onSuccess(MyResult<NewsRedBean> newsRedBeanMyResult) {
                        if (newsRedBeanMyResult != null) {
                            newsRedBean = newsRedBeanMyResult.getData();
                        }
                        int redcount = 0;
                        for (Object o : list) {
                            if (o instanceof NewsRedBean) {
                                if (redcount == redindex) {
                                    if (newsRedBean != null && newsRedBean.getId() > 0) {
                                        ((NewsRedBean) o).setType(1);
                                        ((NewsRedBean) o).setId(newsRedBean.getId());
                                        ((NewsRedBean) o).setCoins(newsRedBean.getCoins());
                                        ((NewsRedBean) o).setRead_news_time(newsRedBean.getRead_news_time());
                                        ((NewsRedBean) o).setRead_news_num(newsRedBean.getRead_news_num());

                                    } else {
                                        ((NewsRedBean) o).setType(0);
                                    }
                                    break;
                                }
                                redcount++;
                            }
                        }
                        if (adapter != null) {
                            adapter.setSrc(list);
                            adapter.notifyDataSetChanged();
                        }

                    }

                    @Override
                    public void onError(MyResult<NewsRedBean> newsRedBeanMyResult) {

                    }

                    @Override
                    public void onFail(Throwable e) {

                    }

                    @Override
                    public void onFinish() {

                    }
                });
    }

    private void getreddata() {

        Map<String, Object> map = new HashMap<>();
        map = StringToolKit.MapSercet(map);
        RequestBody requestBody = StringToolKit.map2RequestBody(map);
        HttpManager.getApiStoresSingleton().userInformRedpckOne(requestBody)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
                .subscribe(new RxCallBack<MyResult<NewsRedBean>>(this.getContext()) {
                    @Override
                    public void onSuccess(MyResult<NewsRedBean> newsRedBeanMyResult) {
                        if (newsRedBeanMyResult != null) {
                            newsRedBean = newsRedBeanMyResult.getData();
                        }
                        getnewslist();
                    }

                    @Override
                    public void onError(MyResult<NewsRedBean> newsRedBeanMyResult) {
                        getnewslist();
                    }

                    @Override
                    public void onFail(Throwable e) {
                        getnewslist();
                    }

                    @Override
                    public void onFinish() {

                    }
                });
    }


    private void gethaotulist(int count) {
        new FeedExpress().show(null, "", 0, count, new CustomListener() {
            @Override
            public void onShow(View view, MediaInfo mediaInfo) {

            }

            @Override
            public void onClick(View view, MediaInfo mediaInfo) {

            }

            @Override
            public void onError(int i, Throwable throwable) {

            }

            @Override
            public void noData(int i) {

            }

            @Override
            public void onSuccess(int i, List<MediaInfo> list) {
                if (mediaInfoList == null) {
                    mediaInfoList = new ArrayList<>();
                }
                List<MediaInfo> removelist = new ArrayList<>();

                for (MediaInfo newbean : list) {
                    for (MediaInfo oldbean : mediaInfoList) {
                        if (newbean.getVideo_id().equals(oldbean.getVideo_id())) {
                            removelist.add(newbean);
                            break;
                        }
                    }
                }
                list.removeAll(removelist);
                mediaInfoList.addAll(list);
            }
        });

    }

    /**
     * @param isfresh
     */
    private void setlist(boolean isfresh) {


        if (list == null)
            list = new ArrayList<>();

        if (!MainApplication.ChannelOpen) {
            advindex = 0;
            if (advlist != null) {
                advlist.clear();
            }
        }

        if (isfresh) {
            comindex = 0;
            advindex = 0;
            videoindex = 0;
            redindex = 0;
            list.clear();
        }
        int alllength = (top_list != null ? top_list.size() : 0)
                + (com_list != null ? com_list.size() : 0)
                + (mediaInfoList != null ? mediaInfoList.size() : 0)
                + (advlist != null ? advlist.size() : 0);//全部长度

        if (page == 1)
            list.addAll(top_list);//添加置顶新闻list
        for (int i = list.size(); i < alllength; i++) {
            int index = i % 10;//获取第三和第七的下标
            //插入新闻
            if (index != 2 && index != 6 && index != 9 && index != 4) {

                if (com_list.size() <= comindex)
                    break;
                list.add(com_list.get(comindex));
                comindex++;
            } else if (index == 2 || index == 6) {//为各位是3或者7位置时  广告
                //插入广告
                if (advlist.size() <= advindex) {
                    if (com_list.size() <= comindex)
                        break;
                    list.add(com_list.get(comindex));
                    comindex++;
                    continue;
                }
                list.add(advlist.get(advindex));
                advindex++;
            } else if (index == 4) {
                list.add(new NewsRedBean());
            } else if (index == 9) {
                //插入视频
                if (mediaInfoList.size() <= videoindex) {
                    if (com_list.size() <= comindex)
                        break;
                    list.add(com_list.get(comindex));
                    comindex++;
                    continue;
                }
                list.add(mediaInfoList.get(videoindex));
                videoindex++;
            }
        }
        if (list.size() == 0) {
            ll_nonews.setVisibility(View.VISIBLE);
        } else {
            ll_nonews.setVisibility(View.GONE);
        }


        boolean ishave = false;
        for (Object o : list) {
            if (o instanceof NewsRedBean) {
                if (((NewsRedBean) o).getType() == 1) {
                    ishave = true;
                    break;
                }
            }
        }

        if (!ishave && page > 1) {
            int i = list.size() / 10;
            redindex = i - 1;
        }


        int redcount = 0;
        for (Object o : list) {
            if (o instanceof NewsRedBean) {
                ((NewsRedBean) o).setType(0);
                if (redcount == redindex) {
                    if (newsRedBean != null && newsRedBean.getId() > 0) {
                        ((NewsRedBean) o).setType(1);
                        ((NewsRedBean) o).setId(newsRedBean.getId());
                        ((NewsRedBean) o).setCoins(newsRedBean.getCoins());
                        ((NewsRedBean) o).setRead_news_time(newsRedBean.getRead_news_time());
                        ((NewsRedBean) o).setRead_news_num(newsRedBean.getRead_news_num());

                    } else {
                        ((NewsRedBean) o).setType(0);
                    }
                    break;
                }
                redcount++;
            }
        }

        if (adapter != null) {
            adapter.setSrc(list);
            adapter.notifyDataSetChanged();
        } else {
            adapter = new FocusItemAdapter(this.getContext(), list);
            adapter.setOnItemClickListener(FocusItemFragment.this);
            adapter.setOnCloseClick(this);
            recyclerview.setAdapter(adapter);
        }


    }

    @Override
    public void initView() {

        swiprefresh.setRefreshHeader(new XnClassicsHeader(getActivity()));
        swiprefresh.setEnableOverScrollDrag(true);
        swiprefresh.setEnableOverScrollBounce(true);

        swiprefresh.setEnableLoadmoreWhenContentNotFull(true);
        swiprefresh.setRefreshFooter(new ClassicsFooter(getActivity()));
        swiprefresh.setEnableAutoLoadmore(true);
        swiprefresh.setOnRefreshLoadmoreListener(new OnRefreshLoadmoreListener() {
            @Override
            public void onLoadmore(RefreshLayout refreshlayout) {
                page++;

                if ("0".equals(chaid)) {
                    getreddata();
                } else {
                    getnewslist();
                }
                gethaotulist(1);


                Random random = new Random();
                int index = random.nextInt(10);

                if (index == 0)
                    getAdvBackground(2);//第一次获取6个广告，比第一次展示的广告4个多两个
                else
                    getCsjAdvDataBack(2);
            }

            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                page = 1;
                isfresh = true;
                advlist.clear();
                mediaInfoList.clear();
                newsRedBean = null;

                if ("0".equals(chaid)) {
                    getreddata();
                } else {
                    getnewslist();
                }

                gethaotulist(1);

                Random random = new Random();
                int index = random.nextInt(10);

                if (index == 0)
                    getAdvBackground(2);//第一次获取6个广告，比第一次展示的广告4个多两个
                else
                    getCsjAdvDataBack(2);

            }
        });
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        manager.setSmoothScrollbarEnabled(true);
        manager.setAutoMeasureEnabled(true);
        recyclerview.setLayoutManager(manager);
        recyclerview.setHasFixedSize(true);
        recyclerview.setNestedScrollingEnabled(false);//关闭滑动
        recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (backtop != null)
                    if (newState == 2) {
                        backtop.setAlpha(0.5f);
                    } else {
                        backtop.setAlpha(1f);
                    }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (manager.findFirstVisibleItemPosition() > 7) {
                    backtop.setVisibility(View.VISIBLE);
                } else {
                    backtop.setVisibility(View.GONE);
                }
            }
        });

        backtop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recyclerview != null)
                    recyclerview.smoothScrollToPosition(0);
            }
        });
    }

    @Override
    public void OnItemClick(int position) {

        SPUtils.getInstance().put(Constant.isFristNews, false);
        if (position == -1)
            return;
        if (list == null)
            return;
        if (list.size() < position)
            return;

        if (list.get(position) == null)
            return;

        if (list.get(position) instanceof NewsListBean.TopListBean) {
            nid = StringToolKit.dealNullOrEmpty(((NewsListBean.TopListBean) list.get(position)).getNid());
            UIHelper.toNewsDetailActivity(getActivity(), nid);

            ((NewsListBean.TopListBean) list.get(position)).setLook(true);
        } else if (list.get(position) instanceof NewsListBean.ComListBean) {
            nid = StringToolKit.dealNullOrEmpty(((NewsListBean.ComListBean) list.get(position)).getNid());
            UIHelper.toNewsDetailActivity(getActivity(), nid);

            ((NewsListBean.ComListBean) list.get(position)).setLook(true);
        } else if (list.get(position) instanceof VideoListBean) {
            UIHelper.toMainActivity(getActivity(), (VideoListBean) list.get(position), 1);//跳转到视频列表

            ((VideoListBean) list.get(position)).setLook(true);
        } else if (list.get(position) instanceof MediaInfo) {
            if (getActivity() != null)
                VideoActivity.start(getActivity(), (MediaInfo) list.get(position));
        } else if (list.get(position) instanceof NewsRedBean) {
            redindex++;
            ((NewsRedBean) list.get(position)).setType(0);
            clickred();
        }
        adapter.notifyItemChanged(position);
    }

    @Override
    public void clickToColse(int position, int distance, int high) {
        int space = countDistance(distance, high);
        nid = StringToolKit.dealNullOrEmpty(((NewsListBean.ComListBean) list.get(position)).getNid());

        mNewsCloseDialog = new NewsCloseDialog(getActivity(), space);
        mNewsCloseDialog.setEnjoyClickListener(new NewsCloseDialog.EnjoyClickListener() {
            @Override
            public void clickToClose() {
                interestApi(Integer.valueOf(nid), 0, 0);
                list.remove(position);
                adapter.notifyDataSetChanged();
            }
        }).setRubbishClickListener(new NewsCloseDialog.RubbishClickListener() {
            @Override
            public void clickToClose() {
                interestApi(Integer.valueOf(nid), 0, 1);
                list.remove(position);
                adapter.notifyDataSetChanged();
            }
        }).show();
    }

    //469px = 230px + 48px + 191px   230是感兴趣弹窗的高度，48为状态栏系统高度DisplayUtil.getStatusBarHeight(getActivity())，191为全局title + tablayout高度
    public int countDistance(int distance, int hight) {
        int newDistance = 0;
        if (distance < 469) {
            newDistance = distance + hight - DisplayUtil.getStatusBarHeight(getActivity());
        } else {
            newDistance = distance - 230 - DisplayUtil.getStatusBarHeight(getActivity());
        }
        return newDistance;
    }

    //兴趣选择 type （0/不感兴趣   1/感兴趣），srv_record == 1代表内容质量差
    private void interestApi(int nid, int type, int srv_record) {
        Map<String, Object> interestMap = new HashMap<>();
        interestMap.put("nid", nid);
        interestMap.put("type", type);
        interestMap.put("srv_record", srv_record);
        interestMap = StringToolKit.MapSercet(interestMap);
        RequestBody requestBody = StringToolKit.map2RequestBody(interestMap);
        HttpManager.getApiStoresSingleton().interestCheckApi(requestBody)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
                .subscribe(new RxCallBack<MyResult<String>>(this.getContext()) {
                    @Override
                    public void onSuccess(MyResult<String> stringMyResult) {
                    }

                    @Override
                    public void onError(MyResult<String> stringMyResult) {
                        ToastUtils.showShort(stringMyResult.getE_msg());
                    }

                    @Override
                    public void onFail(Throwable e) {
                    }

                    @Override
                    public void onFinish() {
                    }
                });
    }

    private void clickred() {

        if (newsRedBean == null)
            return;

        Map<String, Object> map = new HashMap<>();
        map.put("redpck_id", newsRedBean.getId());
        map = StringToolKit.MapSercet(map);
        RequestBody requestBody = StringToolKit.map2RequestBody(map);
        HttpManager.getApiStoresSingleton().informRedpckAwardCoins(requestBody)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this)))
                .subscribe(new RxCallBack<MyResult<NewsRedRewardBean>>(this.getContext()) {
                    @Override
                    public void onSuccess(MyResult<NewsRedRewardBean> newsRedBeanMyResult) {
                        getreddatabackground();
                        if (newsRedBeanMyResult != null) {
                            NewsRedRewardBean newsRedRewardBean = newsRedBeanMyResult.getData();
                            if (newsRedDialog != null && newsRedDialog.isShowing()) {
                                newsRedDialog.hideDialog();
                            }

                            if (getActivity() != null) {
                                newsRedDialog = new NewsRedDialog(getActivity(), newsRedRewardBean);
                                newsRedDialog.show();

                            }
                        }
                    }

                    @Override
                    public void onError(MyResult<NewsRedRewardBean> newsRedBeanMyResult) {
                    }

                    @Override
                    public void onFail(Throwable e) {
                    }

                    @Override
                    public void onFinish() {

                    }
                });
    }


    public void refresh() {
        if (swiprefresh != null)
            swiprefresh.autoRefresh();
        if (recyclerview != null)
            recyclerview.scrollToPosition(0);
    }


}
