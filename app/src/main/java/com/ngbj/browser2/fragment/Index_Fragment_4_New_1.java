package com.ngbj.browser2.fragment;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.ngbj.browser2.R;
import com.ngbj.browser2.activity.SearchActivity;
import com.ngbj.browser2.activity.WebViewGetUrlActivity;
import com.ngbj.browser2.adpter.IndexGridViewAdapter;
import com.ngbj.browser2.adpter.IndexViewPagerAdapter;
import com.ngbj.browser2.adpter.Index_Cool_Adapter;
import com.ngbj.browser2.bean.AdBean;
import com.ngbj.browser2.bean.AdObjectBean;
import com.ngbj.browser2.bean.BookMarkData;
import com.ngbj.browser2.bean.CountData;
import com.ngbj.browser2.bean.HistoryData;
import com.ngbj.browser2.constant.ApiConstants;
import com.ngbj.browser2.event.CollectEvent;
import com.ngbj.browser2.event.History_CollectionEvent;
import com.ngbj.browser2.event.NewsShowFragmentEvent;
import com.ngbj.browser2.event.RefreshDataEvent;
import com.ngbj.browser2.event.TypeEvent;
import com.ngbj.browser2.network.retrofit.helper.RetrofitHelper;
import com.ngbj.browser2.network.retrofit.response.BaseObjectSubscriber;
import com.ngbj.browser2.util.AppUtil;
import com.ngbj.browser2.util.SPHelper;
import com.ngbj.browser2.util.StringUtils;
import com.ngbj.browser2.util.ToastUtil;
import com.socks.library.KLog;
import com.umeng.analytics.MobclickAgent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static android.content.Context.DOWNLOAD_SERVICE;

/***
 * 首页大Fragment -- 第一个
 *
 * 1.是正常
 * 0.是广告
 */
public class Index_Fragment_4_New_1 extends BaseFragment {


    @BindView(R.id.viewPager_gridView)
    ViewPager mViewPagerGridView;//显示gridView的VP

    @BindView(R.id.progressBar)
    ProgressBar pg;

    WebView webview;

    @BindView(R.id.webView_ll)
    LinearLayout webView_ll;

    @BindView(R.id.center_title)
    TextView center_title;

    @BindView(R.id.webView_addpart)
    LinearLayout webView_addpart;

    @BindView(R.id.par3)
    LinearLayout par3;

    @BindView(R.id.part1)
    RelativeLayout part1;

    List<AdBean> adTop2BeanList = new ArrayList<>();
    List<String> list_Title = new ArrayList<>();//标题


    private int totalPage;//总的页数
    private int mPageSize = 8;//每页显示的最大数量
    private List<View> viewPagerList;

    GridView gridView;
    IndexGridViewAdapter mIndexGridViewAdapter;


    SimpleDateFormat simpleDateFormat;
    Date date;
    boolean isRefresh ;
    String saveTitle;
    String saveUrl;
    String currentUrl ;
    String currentTitle;


    //TODO 新增V1.1.0
    /** -----------------------------------------------------------------*/
    @BindView(R.id.cool_recycleView)
    RecyclerView cool_recycleView;
    Index_Cool_Adapter indexCoolAdapter;

    private void initCoolRecycleView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(),4);
        //设置布局管理器
        cool_recycleView.setLayoutManager(layoutManager);
        //设置Adapter
        indexCoolAdapter = new Index_Cool_Adapter(adTop2BeanList);
        cool_recycleView.setAdapter(indexCoolAdapter);
        //一行代码开启动画 默认CUSTOM动画
        indexCoolAdapter.openLoadAnimation(BaseQuickAdapter.SCALEIN);
        //设置事件
        indexCoolAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                AdBean adBean = adTop2BeanList.get(position);
                MobclickAgent.onEvent(getActivity(), "CoolSiteModel");//大模块点击事件
                if(!TextUtils.isEmpty(adBean.getType()) && adBean.getType().equals("0")){//广告
                    map.put("ad_id",adBean.getId());
                    MobclickAgent.onEvent(mContext, "CoolSiteAd", map);//广告点击事件
                    addAdUserClick(adBean.getId(),"CoolSiteAdUserNum");
                }
                addModleUserClick(adBean.getShow_position());//模块用户点击数

                if("1".equals(adBean.getLink())){
                    ToastUtil.customToastGravity(getActivity(),"敬请期待",2, Gravity.CENTER,0,0);
                    return;
                }
                startWebViewRequestLink(adBean.getLink());
            }
        });
    }


    private void getAdData2_2() {
        indexCoolAdapter.setNewData(adTop2BeanList);
    }

    /** -----------------------------------------------------------------*/



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    //home界面的新闻点击返回的逻辑
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewsShowFragmentEvent(NewsShowFragmentEvent event) {
        if(event.getType() == 4){
            part1.setClickable(false);
            startWebViewRequest(event.getLink());
        }
    }


    int type = 4;
    //点击多窗体时获取的type
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTypeEvent(TypeEvent event) {
        type =  event.getType();
    }




    //刷新
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshDataEvent(RefreshDataEvent event) {
        isRefresh = true;
        if(event.getIndex() == 3){
            if(webView_ll.getVisibility() == View.VISIBLE){
                webview.reload(); //刷新
                return;
            }else{
                refreshAdData();
            }
        }
    }

    //收藏 将当前页地址记录
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCollectEvent(CollectEvent event) {
        if(webView_ll.getVisibility() == View.VISIBLE){
            if(event.getType() == 3){
                saveToBookMarkSql( webview.getTitle(),webview.getUrl());
                Toast.makeText(getActivity(),"收藏成功",Toast.LENGTH_SHORT).show();
            }
        }
    }

    //历史记录 + 收藏 跳转
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHistory_CollectionEvent(History_CollectionEvent event) {
        if(event.getIndex().equals("4"))
            startWebViewRequestLink(event.getLink());
    }



    private void refreshAdData() {
        isNetwork = (boolean) SPHelper.get(getActivity(),"is_network",false);
        if(isNetwork){
            getHomeData();
        }else{

        }
    }


    @SuppressLint("CheckResult")
    private void getHomeData(){
        //初始化
        RetrofitHelper.getAppService()
                .getAdData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new BaseObjectSubscriber<AdObjectBean>(){
                    @Override
                    public void onSuccess(AdObjectBean adObjectBean) {
                        if(null != adObjectBean){
                            adTop2BeanList.clear();
                            adTop2BeanList.addAll(adObjectBean.getCool_site());
                            changeOriData();
                            sendDataToUm();
                        }
                    }
                });
    }

    private void sendDataToUm() {

        if(adTop2BeanList != null && adTop2BeanList.size() != 0){
            for (AdBean adBean:adTop2BeanList) {
                if(adBean.getType().equals("0")){
                    map.put("ad_id",adBean.getId());
                    MobclickAgent.onEvent(mContext, "CoolSiteShowAd", map);//广告展示事件
//                      KLog.d(" -- CoolSiteShowAd -- ");
                }
            }
        }

    }



    private void transformToCountData(AdBean adBean) {
        countData = new CountData();
        countData.setAdShowName(adBean.getTitle());
        countData.setAd_id(adBean.getId());
        countData.setImg_url(adBean.getImg_url());
        countData.setAd_link(adBean.getLink());
        countData.setType(adBean.getType());
        countData.setShow_num(1);//默认展示次数为1，之前都获取1即可
        countData.setShow_position(adBean.getShow_position());
        dbManager.insertUser(countData);
    }


    private void addSqlAndToWeb(AdBean adBean, int type) {
        if("1".equals(adBean.getLink())){
            ToastUtil.customToastGravity(getActivity(),"敬请期待",2, Gravity.CENTER,0,0);
            return;
        }
        startWebViewRequestLink(adBean.getLink());
        addClickCountToSql(adBean.getTitle(),adBean.getId(),type);
    }



    private void changeOriData() {
        //第二部分
        getAdData2_2();
    }


    public static Index_Fragment_4_New_1 getInstance(){
        return new Index_Fragment_4_New_1();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.index_big_fragment_1;
    }

    @Override
    protected void initData() {
        initCoolRecycleView();
        refreshAdData();
    }


    private void getAdData2() {

        //总的页数，取整（这里有三种类型：Math.ceil(3.5)=4:向上取整，只要有小数都+1  Math.floor(3.5)=3：向下取整  Math.round(3.5)=4:四舍五入）
        totalPage = (int) Math.ceil(adTop2BeanList.size() * 1.0 / mPageSize);
        viewPagerList = new ArrayList<>();

        for(int i=0;i<totalPage;i++){
            //每个页面都是inflate出一个新实例
            gridView = (GridView) LayoutInflater.from(getActivity()).inflate(R.layout.index_tag2_item,mViewPagerGridView,false);
            mIndexGridViewAdapter = new IndexGridViewAdapter(getActivity(),adTop2BeanList,i,mPageSize);
            gridView.setAdapter(mIndexGridViewAdapter);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    AdBean adBean = adTop2BeanList.get(position);

                    MobclickAgent.onEvent(getActivity(), "CoolSiteModel");//大模块点击事件
                    KLog.d("CoolSiteModel");
                    if(!TextUtils.isEmpty(adBean.getType()) && adBean.getType().equals("0")){//广告
                        map.put("ad_id",adBean.getId());
                        MobclickAgent.onEvent(mContext, "CoolSiteAd", map);//广告点击事件
                        addAdUserClick(adBean.getId(),"CoolSiteAdUserNum");
                    }
                    addModleUserClick(adBean.getShow_position());//模块用户点击数
                    KLog.d("addModleUserClick");
                    if("1".equals(adBean.getLink())){
                        ToastUtil.customToastGravity(getActivity(),"敬请期待",2, Gravity.CENTER,0,0);
                        return;
                    }
                    startWebViewRequestLink(adBean.getLink());
                }
            });
            //每一个GridView作为一个View对象添加到ViewPager集合中
            viewPagerList.add(gridView);
        }

        //设置ViewPager适配器
        mViewPagerGridView.setAdapter(new IndexViewPagerAdapter(viewPagerList));
    }




    @OnClick(R.id.search_text)
    public void Opensearch(){
        if(StringUtils.isFastClick()){
            return;
        }
        startActivityForResult(new Intent(getActivity(),SearchActivity.class),100);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 100){
            if (data != null){

                String content = data.getStringExtra("content");
//                KLog.d("返回给fragment的内容是：" + content);
                //TODO 显示布局，加载内容
                part1.setClickable(true);
                startWebViewRequest(content);

            }
        }else if(resultCode == 200){
            if(data != null){
                String content = data.getStringExtra("content");
                startWebViewRequestNoClean(content);
            }
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if(webview != null){
            webview.onPause();
            webview.pauseTimers();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        sendDataToUm();

        if(webview != null){
            webview.resumeTimers();
            webview.onResume();
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(webview != null){
            webview.destroy();
            webview.removeAllViews();
            webview = null;
        }
    }

    private void endWebView() {
        center_title.setText("");
        if (webview != null) {
            webview.stopLoading();
            webview.clearFormData();
            webview.clearHistory();
            webview.clearView();
            webview.destroy();
            webview = null;
            webView_addpart.removeAllViews();
        }
    }


    private void startWebViewRequestLink(String urlLink) {
        endWebView();
        webview = new WebView(getActivity());
        webview.setId(R.id.webview);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        webView_addpart.addView(webview,lp);
        setSetting();
        initWebViewClient();
        initWebChromeClient();
        webview.loadUrl(urlLink);
        webView_ll.setVisibility(View.VISIBLE);
    }


    private void startWebViewRequestNoClean(String content) {
        urlLogin(content);
    }

    private void startWebViewRequest(String content) {
//        KLog.d("搜索返回的内容是：" + content);
        endWebView();
        webview = new WebView(AppUtil.getContext());
        webview.setId(R.id.webview);
        webView_addpart.addView(webview);
        setSetting();
        initWebViewClient();
        initWebChromeClient();
        urlLogin(content);
        webView_ll.setVisibility(View.VISIBLE);
    }

    private void urlLogin(String url) {
        //如果输入的url包含协议地址
        if (url.length() >= 4 && url.substring(0, 4).equals("http")) {
            if (StringUtils.isUrl(url)) {
                //加载
                webview.loadUrl(url);
            } else {
                //通过百度搜索关键字
                url = ApiConstants.SOUGOU + "web/sl?keyword=" + url;
                webview.loadUrl(url);
            }
        } else {//如果输入的url不包含协议地址
            String url1 = "http://" + url;
            String url2 = "https://" + url;
            if (StringUtils.isUrl(url1)) {
                //加载
                url1 = ApiConstants.SOUGOU + "web/sl?keyword=" + url;
                webview.loadUrl(url1);
            } else if (StringUtils.isUrl(url2)) {
                //加载
                webview.loadUrl(url2);
            } else {
                //通过百度搜索关键字
//                url = ApiConstants.BAIDUURL + "s?wd=" + url;//URL是根据使用百度搜索某个关键字得到的url截取得到的
                url = ApiConstants.SOUGOU + "web/sl?keyword=" + url;//URL是根据使用百度搜索某个关键字得到的url截取得到的
                webview.loadUrl(url);
            }
        }
    }

    WebSettings webSettings;
    @SuppressLint({"NewApi"})
    private void setSetting() {
        webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);//允许使用js
        webSettings.setSupportZoom(true); //支持屏幕缩放
        webSettings.setBuiltInZoomControls(true);
        //设置是否允许通过 file url 加载的 Javascript 可以访问其他的源(包括http、https等源)
        webview.getSettings().setAllowUniversalAccessFromFileURLs(false);
        webSettings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        webSettings.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        webSettings.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        webSettings.setDisplayZoomControls(false); //隐藏原生的缩放控件
        webview.getSettings().setBlockNetworkImage(false); // 解决图片不显示
        webview.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        //其他细节操作
        webSettings.setDatabaseEnabled(true);
        String dir = getActivity().getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        webSettings.setGeolocationDatabasePath(dir);
        webSettings.setGeolocationEnabled(true);
        webSettings.setAllowFileAccess(true); //设置可以访问文件
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        webSettings.setLoadsImagesAutomatically(true); //支持自动加载图片
        webSettings.setDefaultTextEncodingName("utf-8");//设置编码格式
        webSettings.setDomStorageEnabled(true);//支持DOM API

        //一般情况是由于协议不同引起的，添加下面的设置，如果不是HTTP或HTTPS协议则由浏览器进行解读
//        webview.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:54.0) Gecko/20100101 Firefox/54.0");
//        webview.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.134 Safari/537.36");
        registerForContextMenu(webview);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //设置当一个安全站点企图加载来自一个不安全站点资源时WebView的行为,
            // 在这种模式下,WebView将允许一个安全的起源从其他来源加载内容，即使那是不安全的.
            // 如果app需要安全性比较高，不应该设置此模式
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);//解决app中部分页面非https导致的问题
        }
        //TODO 下载
        webview.setDownloadListener(new MyWebViewDownLoadListener());
    }


    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                    ContextMenu.ContextMenuInfo contextMenuInfo) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
        final WebView.HitTestResult webViewHitTestResult = webview.getHitTestResult();
        if (webViewHitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                webViewHitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            contextMenu.setHeaderTitle("网页中下载图片");
            contextMenu.add(0, 1, 0, "点击保存")
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            String DownloadImageURL = webViewHitTestResult.getExtra();
                            if (URLUtil.isValidUrl(DownloadImageURL)) {
                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DownloadImageURL));
                                request.allowScanningByMediaScanner();
                                //设置图片的保存路径
                                request.setDestinationInExternalPublicDir("SmallBrowse/Pic/",System.currentTimeMillis() + "." + "png");
                                DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(DOWNLOAD_SERVICE);
                                downloadManager.enqueue(request);
                                Toast.makeText(getActivity(), "图片保存到" + "/SmallBrowse/Pic/目录下", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), "下载失败", Toast.LENGTH_LONG).show();
                            }
                            return false;
                        }
                    });
        }
    }



    @BindView(R.id.part3)
    LinearLayout part3;

    @BindView(R.id.edit_title)
    EditText edit_title;


    //显示输入框的地址
    @OnClick(R.id.part1)
    public void part1(){

        String myUrl = "";
        if(!TextUtils.isEmpty(currentUrl)){
            myUrl= currentUrl;
        }else if(!TextUtils.isEmpty(currentTitle)){
            myUrl = currentTitle;
        }

        if(!TextUtils.isEmpty(myUrl)){
            Intent intent = new Intent(getActivity(), WebViewGetUrlActivity.class);
            intent.putExtra("weburl",myUrl);
            startActivityForResult(intent,200);
        }
    }


    private void initWebViewClient() {
        //复写shouldOverrideUrlLoading()方法，使得打开网页时不调用系统浏览器， 而是在本WebView中显示
        webview.setWebViewClient(new WebViewClient(){
            boolean if_load;
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if_load = false;
                currentUrl = url;
//                view.loadUrl(url);//在这里设置对应的操作
                return false;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();// 接受所有网站的证书
                //super.onReceivedSslError(view, handler, error);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if_load = true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if(if_load ){
//               KLog.d("url: " + view.getUrl() + " 标题："  + view.getTitle() + " 原始链接： " + view.getOriginalUrl());

                    if(webview.getSettings().getCacheMode() == WebSettings.LOAD_DEFAULT){

                        if(null != view.copyBackForwardList().getCurrentItem()){
                            saveTitle = view.copyBackForwardList().getCurrentItem().getTitle();
                            saveUrl = view.copyBackForwardList().getCurrentItem().getUrl();

                            if(!TextUtils.isEmpty(saveTitle) &&
                                    !TextUtils.isEmpty(saveUrl)){
                                //TODO 添加到数据库
                                saveToHistorySql(saveTitle,saveUrl);
                            }
                        }
                        if_load = false;
                    }
                }
            }

        });
    }

    private void saveToHistorySql(String saveTitle, String saveUrl) {
        if(!TextUtils.isEmpty(saveTitle) && !TextUtils.isEmpty(saveUrl)){
            HistoryData historyData = new HistoryData();
            historyData.setVisit_link(saveUrl);
            historyData.setTitle(saveTitle);
            historyData.setKeyword(saveTitle);
            historyData.setType("1");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");// HH:mm:ss
            //获取当前时间
            Date date = new Date(System.currentTimeMillis());
            historyData.setCurrentTime(simpleDateFormat.format(date));
            dbManager.insertHistrory(historyData);
        }
    }

    private void saveToBookMarkSql(String saveTitle, String saveUrl) {
        if(!TextUtils.isEmpty(saveTitle) && !TextUtils.isEmpty(saveUrl)){
            BookMarkData bookMarkData = new BookMarkData();
            bookMarkData.setVisit_link(saveUrl);
            bookMarkData.setTitle(saveTitle);
            simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");// HH:mm:ss
            date = new Date(System.currentTimeMillis());//当前时间
            KLog.d("Date获取当前日期时间"+ simpleDateFormat.format(date));
            bookMarkData.setCurrentTime(simpleDateFormat.format(date));
            dbManager.insertBookMark(bookMarkData);
        }
    }


    private void initWebChromeClient() {
        //获取网页进度
        webview.setWebChromeClient(new WebChromeClient(){

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }


            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if(newProgress == 100){
                    pg.setVisibility(View.GONE);//加载完网页进度条消失
                }
                else{
                    pg.setVisibility(View.VISIBLE);//开始加载网页时显示进度条
                    pg.setProgress(newProgress);//设置进度值
                }
                super.onProgressChanged(view, newProgress);
            }

            //获取网页标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                currentTitle = title;
                center_title.setText(title);
                currentUrl = view.getUrl();
            }

        });
    }






}
