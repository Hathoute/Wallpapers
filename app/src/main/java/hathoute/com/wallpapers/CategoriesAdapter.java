package hathoute.com.wallpapers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.MyViewHolder> {

    private Activity mActivity;
    private List<Category> categoryList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private SquareImageView sivCategory;
        public LinearLayout llContainer;
        public TextView tvName;

        private MyViewHolder(View view) {
            super(view);
            sivCategory = view.findViewById(R.id.sivWallpaper);
            llContainer = view.findViewById(R.id.llContainer);
            tvName = view.findViewById(R.id.tvName);
        }
    }

    protected CategoriesAdapter(Activity activity, List<Category> categoryList) {
        mActivity = activity;
        this.categoryList = categoryList;
    }

    @NonNull
    @Override
    public CategoriesAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grid_category, parent, false);

        return new CategoriesAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final CategoriesAdapter.MyViewHolder holder, int position) {
        final Category category = categoryList.get(position);
        holder.sivCategory.setImageBmp(category.thumbnail);
        Palette.from(category.thumbnail).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                int dominantColor = 0;
                if(palette != null)
                    dominantColor = palette.getDominantColor(0);

                // Makes colour darker
                float[] hsv = new float[3];
                Color.colorToHSV(dominantColor, hsv);
                hsv[2] *= 0.8f;

                // Setting the bmp as bg with an alpha (0xFF - 0xAA)
                holder.llContainer.setBackgroundColor(Color.HSVToColor(hsv) - 0x55000000);

                //Todo: optimise finding the best color for Text.
                boolean isBlack = Color.HSVToColor(hsv) - 0xFF7FFFFF > 0;
                holder.tvName.setTextColor(isBlack ? 0xFF000000 : 0xFFFFFFFF);
            }
        });

        holder.tvName.setText(category.fullName);
        holder.sivCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, category.isParent ?
                        CategoriesActivity.class : MainActivity.class);
                intent.putExtra("category", category.name);
                intent.putExtra("categoryName", category.fullName);
                mActivity.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }
}
