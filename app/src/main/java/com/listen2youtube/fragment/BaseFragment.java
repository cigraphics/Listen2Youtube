package com.listen2youtube.fragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.balysv.materialripple.MaterialRippleLayout;
import com.listen2youtube.R;
import com.listen2youtube.service.SongInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by khang on 29/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public abstract class BaseFragment extends Fragment {

    RecyclerView recyclerView;
    SwipeRefreshLayout refreshLayout;
    TextView tvNoFile;

    View.OnClickListener onItemClick, onIcon1Click;
    BaseAdapter adapter;

    boolean isLandscape;
    String playingId;

    public interface OnPlaySong {
        void playASong(BaseFragment fragment, int position, String tag);
    }

    OnPlaySong onPlaySong;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        return inflater.inflate(R.layout.fragment_music_list, container, false);
    }

    @CallSuper
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNoFile = (TextView) view.findViewById(R.id.tv_show_no_file);
        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);

        initializeAdapter(savedInstanceState);

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(isLandscape ?
                new GridLayoutManager(getContext(), 2) :
                new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        onItemClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClick(v);
            }
        };

        onIcon1Click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onIcon1Click(v);
            }
        };
    }

    public abstract void initializeAdapter(@Nullable Bundle savedInstanceState);

    public void setRefreshing(final boolean refreshing) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(refreshing);
            }
        });
    }

    public void setOnPlaySong(OnPlaySong onPlaySong) {
        this.onPlaySong = onPlaySong;
    }

    public void runOnUIThread(Runnable runnable){
        if (getActivity() != null)
            getActivity().runOnUiThread(runnable);
    }

    public void showToast(final String message) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public abstract void onItemClick(View v);

    public abstract void onIcon1Click(View v);

    public abstract List<SongInfo> getSongList(String tag);

    public String getThumbnailText(String input) {
        String text = input.substring(0, 1);
        final int p;
        if ((p = input.indexOf(" ")) != -1) {
            text += input.substring(p + 1, p + 2);
        }
        if (text.length() < 2)
            text = text.toUpperCase() + input.substring(1, 2);
        return text;
    }

    public abstract class BaseAdapter<T extends BaseDataItem> extends RecyclerView.Adapter<ViewHolder>{
        public static final int DEFAULT = 0;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        BaseDataSet<T> dataSet;

        public BaseAdapter(BaseDataSet<T> baseDataSet) {
            this.dataSet = baseDataSet;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == DEFAULT)
                return new ViewHolder(inflater.inflate(R.layout.music_item, parent, false), true);
            return null;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            T dataItem = dataSet.getItem(position);
            if (dataItem != null)
                holder.bindData(dataItem, position);
        }

        @Override
        public int getItemCount() {
            if (dataSet.getCount() == 0)
                tvNoFile.setVisibility(View.VISIBLE);
            else
                tvNoFile.setVisibility(View.GONE);
            return dataSet.getCount();
        }

        @Override
        public int getItemViewType(int position) {
            return DEFAULT;
        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView thumbnail;
        AppCompatTextView title, description;
        View icon1, icon2;
        MaterialRippleLayout ripple;


        public ViewHolder(View itemView, boolean isDefaultItem) {
            super(itemView);
            if (isDefaultItem){
                thumbnail = (AppCompatImageView) itemView.findViewById(R.id.ivChannelIcon);
                title = (AppCompatTextView) itemView.findViewById(R.id.tvTitle);
                description = (AppCompatTextView) itemView.findViewById(R.id.tvDescription);
                icon1 = itemView.findViewById(R.id.icon1);
                icon2 = itemView.findViewById(R.id.ivPlaying);
                ripple = (MaterialRippleLayout) itemView.findViewById(R.id.ripple);

                ripple.setOnClickListener(onItemClick);
                icon1.setOnClickListener(onIcon1Click);
            }
        }

        public void bindData(BaseDataItem data, int position){
            title.setText(data.getTitle(position));
            description.setText(data.getDescription(position));

            icon1.setBackgroundResource(data.getDrawableResIcon1());
            icon1.setTag(position);

            ripple.getChildView().setTag(position);

            if (data.shouldShowHeadphoneIcon(position))
                icon2.setVisibility(View.VISIBLE);
            else
                icon2.setVisibility(View.GONE);

            data.setThumbnailIcon(thumbnail, position);
        }
    }

    public abstract class BaseDataSet<T extends BaseDataItem> {
        List<T> dataList = new ArrayList<>();
        OnDataSetChanged onDataSetChanged;

        public BaseDataSet(@NonNull OnDataSetChanged onDataSetChanged) {
            this.onDataSetChanged = onDataSetChanged;
        }

        public int getCount() {
            return dataList.size();
        }
        public T getItem(int position){
            return dataList.get(position);
        }

        public void setDataSet(@Nullable List<T> newDataSet, boolean override){
            if (override) {
                if (newDataSet == null)
                    dataList.clear();
                else
                    dataList = newDataSet;
            } else if (newDataSet != null)
                dataList.addAll(newDataSet);
            onDataSetChanged.onChanged();
        }
    }

    public abstract class BaseDataItem {

        public String getTitle(int position) {
            return "";
        }

        public String getDescription(int position) {
            return "";
        }

        public abstract void setThumbnailIcon(AppCompatImageView imageView, int position);

        public abstract int getDrawableResIcon1();

        public abstract boolean shouldShowHeadphoneIcon(int position);


    }

    public interface OnDataSetChanged{
        void onChanged();
    }

}
