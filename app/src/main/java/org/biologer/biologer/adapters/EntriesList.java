package org.biologer.biologer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.sql.Entry;
import org.biologer.biologer.sql.Stage;
import org.biologer.biologer.sql.StageDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.biologer.biologer.R.id.slika;

/**
 * Created by brjovanovic on 2/24/2018.
 */

public class EntriesList extends BaseAdapter {
    private final Context mContext;
    private final ArrayList<Entry> mList;

    public EntriesList(Context mContext, ArrayList<Entry> mList) {
        this.mContext = mContext;
        this.mList = mList;
    }

    public void addAll(List<Entry> list, boolean clean) {
        if (clean)
            mList.clear();
        // Reorder list to display new entries at the top
        Collections.reverse(list);

        mList.addAll(list);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        mList.remove(position);
        notifyDataSetChanged();
    }

    public void removeAll() {
        mList.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Entry getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewHolder viewHolder = new ViewHolder();

        if (convertView == null) {

            convertView = inflater.inflate(R.layout.entries_list, parent, false);

            viewHolder.taxon = convertView.findViewById(R.id.taxon_name);
            viewHolder.stage = convertView.findViewById(R.id.stage);
            viewHolder.image = convertView.findViewById(slika);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Entry taxon_entry = getItem(position);
        if (taxon_entry.getTaxonSuggestion() != null) {
            viewHolder.taxon.setText(taxon_entry.getTaxonSuggestion());
        } else {
            viewHolder.taxon.setText("");
        }

        if (taxon_entry.getStage() != null) {
            long i = taxon_entry.getStage();
            Stage s = App.get().getDaoSession().getStageDao().queryBuilder().where(StageDao.Properties.StageId.eq(i)).limit(1).unique();
            viewHolder.stage.setText(StageAndSexLocalization.getStageLocale(mContext, s.getName()));
        } else {
            viewHolder.stage.setText("");
        }

        String useImage;
        if (getItem(position).getSlika1() != null) {
            useImage = getItem(position).getSlika1();
        } else {
            if (getItem(position).getSlika2() != null) {
                useImage = getItem(position).getSlika2();
            } else {
                if (getItem(position).getSlika3() != null) {
                    useImage = getItem(position).getSlika3();
                } else {
                    useImage = "";
                }
            }
        }
        if (useImage != null && useImage.trim().length() > 0) {
            Glide.with(convertView)
                    .load(useImage)
                    .into(viewHolder.image);
        } else {
            viewHolder.image.setImageResource(R.mipmap.ic_kornjaca_kocka);
        }
        return convertView;
    }

    private static class ViewHolder {
        TextView taxon;
        TextView stage;
        ImageView image;
    }

}
