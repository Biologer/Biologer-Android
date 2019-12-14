package org.biologer.biologer;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by brjovanovic on 1/8/2018.
 */

public class EventTypesRvAdapter extends RecyclerView.Adapter<EventTypesRvAdapter.ViewHolder> {

    private Context mContext;
    private String[] events;

    private OnItemClick mListener;

    public EventTypesRvAdapter(Context context, String[] events, OnItemClick onItemClickLitener) {
        this.events = events;
        this.mContext = context;
        mListener = onItemClickLitener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_event_type, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        String type = events[position];

        holder.tvTitle.setText(type);

        if (position == 0) {
            holder.tvTitle.setBackground(mContext.getResources().getDrawable(R.drawable.top_selector));
            holder.line.setVisibility(View.VISIBLE);
        } else if (position == getItemCount() - 1) {
            holder.tvTitle.setBackground(mContext.getResources().getDrawable(R.drawable.bottom_selector));
            holder.line.setVisibility(View.GONE);
        } else {
            holder.tvTitle.setBackground(mContext.getResources().getDrawable(R.drawable.center_selector));
            holder.line.setVisibility(View.VISIBLE);
        }

        holder.tvTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(holder.tvTitle, position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.length;
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tvTitle;
        private View line;

        public ViewHolder(View itemView) {
            super(itemView);
            tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
            line = itemView.findViewById(R.id.line);

        }
    }

    public interface OnItemClick{
        void onItemClick(View view, int position);
    }
}
