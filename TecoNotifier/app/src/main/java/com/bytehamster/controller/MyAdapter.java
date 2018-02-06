package com.bytehamster.controller;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private final List<ApplicationInfo> mDataset;
    private Context context;
    private PackageManager pm;
    private SharedPreferences prefs;

    public MyAdapter(List<ApplicationInfo> installedApplications, MainActivity c) {
        mDataset = installedApplications;
        context = c;
        pm = c.getPackageManager();
        prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTextView;
        View root;
        ImageView icon;
        ViewHolder(View v) {
            super(v);
            root = v;
            icon = v.findViewById(R.id.icon);
            mTextView = v.findViewById(R.id.title);
        }
    }

    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mTextView.setText(mDataset.get(position).loadLabel(pm));

        holder.icon.setImageDrawable(mDataset.get(position).loadIcon(pm));
        int activated = prefs.getInt(mDataset.get(holder.getAdapterPosition()).packageName, 0b0000);

        ((ImageView) holder.root.findViewById(R.id.t1)).setImageResource(
                (activated & 0b0001) == 0 ? R.drawable.off : R.drawable.on);
        ((ImageView) holder.root.findViewById(R.id.t2)).setImageResource(
                (activated & 0b0010) == 0 ? R.drawable.off : R.drawable.on);
        ((ImageView) holder.root.findViewById(R.id.t3)).setImageResource(
                (activated & 0b0100) == 0 ? R.drawable.off : R.drawable.on);
        ((ImageView) holder.root.findViewById(R.id.t4)).setImageResource(
                (activated & 0b1000) == 0 ? R.drawable.off : R.drawable.on);

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String pname = mDataset.get(holder.getAdapterPosition()).packageName;
                AlertDialog.Builder b = new AlertDialog.Builder(context);
                final View root = View.inflate(context, R.layout.settings_dialog, null);
                b.setView(root);
                b.setTitle(mDataset.get(holder.getAdapterPosition()).loadLabel(pm));
                b.setMessage(R.string.select);
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MyAdapter.this.notifyItemChanged(holder.getAdapterPosition());
                    }
                });
                b.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        MyAdapter.this.notifyItemChanged(holder.getAdapterPosition());
                    }
                });
                refresh(root, pname);

                root.findViewById(R.id.s1).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        prefs.edit().putInt(pname, prefs.getInt(pname, 0b0000) ^ 0b0001).apply();
                        refresh(root, pname);
                    }
                });
                root.findViewById(R.id.s2).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        prefs.edit().putInt(pname, prefs.getInt(pname, 0b0000) ^ 0b0010).apply();
                        refresh(root, pname);
                    }
                });
                root.findViewById(R.id.s3).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        prefs.edit().putInt(pname, prefs.getInt(pname, 0b0000) ^ 0b0100).apply();
                        refresh(root, pname);
                    }
                });
                root.findViewById(R.id.s4).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        prefs.edit().putInt(pname, prefs.getInt(pname, 0b0000) ^ 0b1000).apply();
                        refresh(root, pname);
                    }
                });
                ((CheckBox) root.findViewById(R.id.morse)).setChecked(prefs.getBoolean("MORSE" + pname, false));
                ((CheckBox) root.findViewById(R.id.morse)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        prefs.edit().putBoolean("MORSE" + pname, b).apply();
                    }
                });

                b.show();
            }
        });
    }

    private void refresh(View root, String packagename) {
        int activated = prefs.getInt(packagename, 0b0000);
        updateIcons(root, activated);
    }

    private void updateIcons(View root, int activated) {
        ((ImageView) root.findViewById(R.id.s1)).setImageResource(
                (activated & 0b0001) == 0 ? R.drawable.off : R.drawable.on);
        ((ImageView) root.findViewById(R.id.s2)).setImageResource(
                (activated & 0b0010) == 0 ? R.drawable.off : R.drawable.on);
        ((ImageView) root.findViewById(R.id.s3)).setImageResource(
                (activated & 0b0100) == 0 ? R.drawable.off : R.drawable.on);
        ((ImageView) root.findViewById(R.id.s4)).setImageResource(
                (activated & 0b1000) == 0 ? R.drawable.off : R.drawable.on);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}