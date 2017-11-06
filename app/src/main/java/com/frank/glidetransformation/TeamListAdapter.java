package com.frank.glidetransformation;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.request.RequestOptions;
import com.frank.glide.transformations.CropTransformation;
import com.frank.glide.transformations.GlideTransformationUtils;

import java.util.List;

public class TeamListAdapter extends RecyclerView.Adapter<TeamListAdapter.ViewHolder> {

    private Context mContext;
    private List<TeamBean> mDataList;
    private LayoutInflater mLayoutInflater;

    public TeamListAdapter(Context context, List<TeamBean> dataList) {
        this.mContext = context;
        this.mDataList = dataList;
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = mLayoutInflater.inflate(R.layout.item_game, parent, false);
        TeamListAdapter.ViewHolder holder = new ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TeamBean teamBean = mDataList.get(position);
        holder.name.setText(teamBean.getName());
        Activity activity = (Activity) mContext;
        ImageView imageView = holder.image;
        RequestOptions requestOptions = new RequestOptions()
                .transform(new MultiTransformation<>(new CropTransformation(100, 20,
                        GlideTransformationUtils.CROP_TYPE_TOP)));
        Glide.with(activity)
                .load(teamBean.getImage())
                .apply(requestOptions)
                .into(imageView);
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView name;

        public ViewHolder(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.image);
            name = (TextView) itemView.findViewById(R.id.name);
        }
    }
}
