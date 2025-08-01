package org.biologer.biologer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.biologer.biologer.R;
import org.biologer.biologer.sql.TaxonDb;

import java.util.List;
import java.util.Objects;

public class TaxaListAdapter extends ArrayAdapter<TaxonDb> {

    public TaxaListAdapter(@NonNull Context context, int resource, @NonNull List<TaxonDb> taxaLists) {
        super(context, resource, taxaLists);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder rowViewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.taxa_dropdown_list, parent, false);
            rowViewHolder = new ViewHolder();
            rowViewHolder.taxonNames = convertView.findViewById(R.id.textView_taxon_text);
            convertView.setTag(rowViewHolder);
        } else {
            rowViewHolder = (ViewHolder) convertView.getTag();
        }

        String taxon = Objects.requireNonNull(getItem(position)).getLatinName();

        rowViewHolder.taxonNames.setText(taxon);

        return convertView;
    }

    private static class ViewHolder {
        TextView taxonNames;
    }
}