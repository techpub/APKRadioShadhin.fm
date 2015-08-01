package de.domradio.activity.fragment;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.pkmmte.pkrss.Article;
import com.pkmmte.pkrss.Callback;
import com.pkmmte.pkrss.PkRSS;

import java.util.ArrayList;
import java.util.List;

import de.domradio.R;
import de.domradio.activity.WebActivity;
import de.domradio.activity.adapter.NewsListAdapter;
import de.domradio.activity.dialog.RssChooserDialog;
import de.domradio.domain.FeedTopic;
import de.domradio.domain.News;
import de.domradio.service.AnalyticsTracker;
import de.domradio.service.EventBusCallback;
import de.domradio.service.event.ErrorEvent;
import de.domradio.service.event.SetNewsFeedEvent;
import de.greenrobot.event.EventBus;

public class NewsFragment extends Fragment implements AdapterView.OnItemClickListener, Callback, SwipeRefreshLayout.OnRefreshListener {

    private FeedTopic currentFeed = FeedTopic.ALL;
    private ArrayList<News> news = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private NewsListAdapter newsListAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    private void loadNewsFeed() {
        PkRSS.with(getActivity()).load(currentFeed.getUrl()).callback(this).async();
        swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.news_list, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.news_fragment_swiper);
        swipeRefreshLayout.setOnRefreshListener(this);
        setupRecyclerView(view);
        loadNewsFeed();
        setHasOptionsMenu(true);
        return view;
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        newsListAdapter = new NewsListAdapter(news);
        recyclerView.setAdapter(newsListAdapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.news_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.main_activity_menu_refresh) {
            loadNewsFeed();
        }
        if (item.getItemId() == R.id.main_activity_menu_choose) {
            new RssChooserDialog(getActivity()).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        News n = news.get(position);
        Intent intent = new Intent(getActivity(), WebActivity.class);
        intent.putExtra("title", n.getTitle());
        intent.putExtra("link", n.getLink().toString());
        startActivity(intent);
        AnalyticsTracker.openNews(getActivity().getApplication(), n);
    }

    @EventBusCallback
    public void onEvent(SetNewsFeedEvent e) {
        currentFeed = e.getFeedTopic();
        loadNewsFeed();

    }

    @Override
    public void OnPreLoad() {
        Log.d("NewsFragment", "Preloaded RSS ...");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout.destroyDrawingCache();
            swipeRefreshLayout.clearAnimation();
        }
    }

    @Override
    public void OnLoaded(final List<Article> articles) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    news.clear();
                    for (Article a : articles) {
                        News n = new News(a.getTitle(), a.getDescription(), a.getDate(), a.getSource());
                        news.add(n);
                    }
                    swipeRefreshLayout.setRefreshing(false);
                    newsListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void OnLoadFailed() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EventBus.getDefault().post(new ErrorEvent(getString(R.string.error_feed)));
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    @Override
    public void onRefresh() {
        loadNewsFeed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
