package com.starrtc.demo.demo.audiolive;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.starrtc.demo.R;
import com.starrtc.demo.demo.BaseActivity;
import com.starrtc.demo.demo.MLOC;
import com.starrtc.demo.serverAPI.InterfaceUrls;
import com.starrtc.demo.ui.CircularCoverView;
import com.starrtc.demo.utils.AEvent;
import com.starrtc.demo.utils.ColorUtils;
import com.starrtc.demo.utils.DensityUtils;
import com.starrtc.demo.utils.StarListUtil;
import com.starrtc.starrtcsdk.api.XHClient;
import com.starrtc.starrtcsdk.apiInterface.IXHResultCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AudioLiveListActivity extends BaseActivity implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private ListView vList;
    private MyListAdapter myListAdapter;
    private ArrayList<AudioLiveInfo> mDatas;
    private LayoutInflater mInflater;
    private SwipeRefreshLayout refreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_live_list);
        ((TextView)findViewById(R.id.title_text)).setText("语音直播列表");
        findViewById(R.id.title_left_btn).setVisibility(View.VISIBLE);
        findViewById(R.id.title_left_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.create_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AudioLiveListActivity.this,AudioLiveCreateActivity.class));
            }
        });

        refreshLayout = (SwipeRefreshLayout)findViewById(R.id.refresh_layout);
        //设置刷新时动画的颜色，可以设置4个
        refreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light, android.R.color.holo_orange_light, android.R.color.holo_green_light);
        refreshLayout.setOnRefreshListener(this);

        mDatas = new ArrayList<>();
        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        myListAdapter = new MyListAdapter();
        vList = (ListView) findViewById(R.id.list);
        vList.setAdapter(myListAdapter);
        vList.setOnItemClickListener(this);
        vList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case SCROLL_STATE_IDLE:
                        if(StarListUtil.isListViewReachTopEdge(absListView)){
                            refreshLayout.setEnabled(true);
                        }else{
                            refreshLayout.setEnabled(false);
                        }
                        break;
                }
            }
            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });
    }

    @Override
    public void dispatchEvent(String aEventID, final boolean success, final Object eventObj) {
        super.dispatchEvent(aEventID,success,eventObj);
        switch (aEventID){
            case AEvent.AEVENT_AUDIO_LIVE_GOT_LIST:
                refreshLayout.setRefreshing(false);
                mDatas.clear();
                if(success){
                    ArrayList<AudioLiveInfo> res = (ArrayList<AudioLiveInfo>) eventObj;
                    for(int i = 0;i<res.size();i++){
                        if(res.get(i).isLiveOn.equals("1")){
                            mDatas.add(res.get(i));
                        }
                    }
                    for(int i = 0;i<res.size();i++){
                        if(res.get(i).isLiveOn.equals("0")){
                            mDatas.add(res.get(i));
                        }
                    }
                    myListAdapter.notifyDataSetChanged();
                }
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AudioLiveInfo clickLiveInfo = mDatas.get(position);

        Intent intent = new Intent(AudioLiveListActivity.this, AudioLiveActivity.class);
        intent.putExtra(AudioLiveActivity.LIVE_NAME,clickLiveInfo.name);
        intent.putExtra(AudioLiveActivity.CREATER_ID,clickLiveInfo.creator);
        intent.putExtra(AudioLiveActivity.LIVE_ID,clickLiveInfo.id);
        startActivity(intent);

    }

    @Override
    public void onResume(){
        super.onResume();
        if(MLOC.SERVER_TYPE.equals(MLOC.SERVER_TYPE_PUBLIC)){
            AEvent.addListener(AEvent.AEVENT_AUDIO_LIVE_GOT_LIST,this);
        }
        onRefresh();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(MLOC.SERVER_TYPE.equals(MLOC.SERVER_TYPE_PUBLIC)) {
            AEvent.removeListener(AEvent.AEVENT_AUDIO_LIVE_GOT_LIST, this);
        }
        super.onStop();
    }
    @Override
    public void onRefresh() {
        if(MLOC.SERVER_TYPE.equals(MLOC.SERVER_TYPE_PUBLIC)){
            InterfaceUrls.demoRequestAudioLiveList();
        }else{
            queryAllList();
        }
    }
    private void queryAllList(){
        XHClient.getInstance().getLiveManager().queryList("",MLOC.CHATROOM_LIST_TYPE_AUDIO_LIVE_ALL,new IXHResultCallback() {
            @Override
            public void success(final Object data) {
                refreshLayout.setRefreshing(false);
                mDatas.clear();
                try {
                    JSONArray array = (JSONArray) data;
                    for(int i = array.length()-1;i>=0;i--){
                        AudioLiveInfo info = new AudioLiveInfo();
                        JSONObject obj = array.getJSONObject(i);
                        info.creator = obj.getString("creator");
                        info.id = obj.getString("id");
                        info.name = obj.getString("name");
                        mDatas.add(info);
                    }
                    myListAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(String errMsg) {
                MLOC.d("VideoMettingListActivity",errMsg);
                refreshLayout.setRefreshing(false);
                mDatas.clear();
                myListAdapter.notifyDataSetChanged();
            }
        });
    }

    class MyListAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return mDatas.size();
        }

        @Override
        public Object getItem(int position) {
            return mDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder viewIconImg;
            if(convertView == null){
                viewIconImg = new ViewHolder();
                convertView = mInflater.inflate(R.layout.item_all_list,null);
                viewIconImg.vRoomName = (TextView)convertView.findViewById(R.id.item_id);
                viewIconImg.vCreaterId = (TextView)convertView.findViewById(R.id.item_creater_id);
                viewIconImg.vLiveState = (TextView)convertView.findViewById(R.id.live_flag);
                viewIconImg.vHeadBg =  convertView.findViewById(R.id.head_bg);
                viewIconImg.vHeadImage = (ImageView) convertView.findViewById(R.id.head_img);
                viewIconImg.vHeadCover = (CircularCoverView) convertView.findViewById(R.id.head_cover);
                convertView.setTag(viewIconImg);
            }else{
                viewIconImg = (ViewHolder)convertView.getTag();
            }
            viewIconImg.vRoomName.setText(mDatas.get(position).name);
            viewIconImg.vCreaterId.setText(mDatas.get(position).creator);
            viewIconImg.vHeadBg.setBackgroundColor(ColorUtils.getColor(AudioLiveListActivity.this,mDatas.get(position).name));
            viewIconImg.vHeadCover.setCoverColor(Color.parseColor("#FFFFFF"));
            if(mDatas.get(position).isLiveOn!=null){
                viewIconImg.vLiveState.setVisibility(mDatas.get(position).isLiveOn.equals("1")?View.VISIBLE:View.INVISIBLE);
            }else{
                viewIconImg.vLiveState.setVisibility(View.INVISIBLE);
            }

            int cint = DensityUtils.dip2px(AudioLiveListActivity.this,28);
            viewIconImg.vHeadCover.setRadians(cint, cint, cint, cint,0);
            viewIconImg.vHeadImage.setImageResource(R.drawable.icon_main_mic);
            return convertView;
        }

        class  ViewHolder{
            private TextView vRoomName;
            private TextView vCreaterId;
            public View vHeadBg;
            public CircularCoverView vHeadCover;
            public ImageView vHeadImage;
            public TextView vLiveState;
        }
    }


}
