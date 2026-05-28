package com.dev.ministudio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.dev.ministudio.model.FileNode;
import java.io.File;
import java.util.List;

public class FileTreeAdapter extends BaseAdapter {

    private Context context;
    private List<FileNode> fileList;
    private int selectedPosition = -1;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public FileTreeAdapter(Context context, List<FileNode> fileList) {
        this.context = context;
        this.fileList = fileList;
    }

    @Override
    public int getCount() { return fileList != null ? fileList.size() : 0; }
    @Override
    public Object getItem(int position) { return fileList.get(position); }
    @Override
    public long getItemId(int position) { return position; }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    static class ViewHolder {
        LinearLayout itemRoot;
        ImageView imgArrow, imgFileIcon;
        TextView tvFileName;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false);
            holder = new ViewHolder();
            holder.itemRoot = convertView.findViewById(R.id.item_root_layout);
            holder.imgArrow = convertView.findViewById(R.id.img_arrow);
            holder.imgFileIcon = convertView.findViewById(R.id.img_file_icon);
            holder.tvFileName = convertView.findViewById(R.id.tv_file_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FileNode node = fileList.get(position);
        File file = node.file;
        holder.tvFileName.setText(file.getName());

        // Indentation
        int indentPx = (int) (node.depth * 16 * context.getResources().getDisplayMetrics().density);
        holder.itemRoot.setPadding(indentPx, 0, (int) (16 * context.getResources().getDisplayMetrics().density), 0);

        // Reset Icon
        holder.imgFileIcon.setColorFilter(null);
        holder.imgFileIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);

        if (node.isDirectory) {
            holder.imgArrow.setVisibility(View.VISIBLE);
            holder.imgArrow.setImageResource(node.isExpanded ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_right);
            holder.imgFileIcon.setImageResource(R.drawable.ic_myicon08);
            holder.imgFileIcon.setColorFilter(Color.parseColor("#FFA726"));
        } else {
            holder.imgArrow.setVisibility(View.GONE);
            String name = file.getName().toLowerCase();
            
            // ตรวจสอบไฟล์รูปภาพเพื่อทำ Thumbnail
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp")) {
                loadThumbnail(file, holder.imgFileIcon);
            } else {
                applyFileIcon(holder.imgFileIcon, name);
            }
        }

        // Selection style
        holder.itemRoot.setBackgroundColor(position == selectedPosition ? Color.parseColor("#243144") : Color.TRANSPARENT);
        holder.tvFileName.setTextColor(position == selectedPosition ? Color.parseColor("#00E5FF") : Color.WHITE);
        
        return convertView;
    }

    // 🚀 ระบบโหลดภาพแบบ Asynchronous เพื่อไม่ให้ UI กระตุก
    private void loadThumbnail(File file, ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_myicon06); // Placeholder
        new Thread(() -> {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            // ลดความละเอียดภาพให้เล็กลง (InSampleSize)
            options.inSampleSize = 4; 
            options.inJustDecodeBounds = false;
            
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap != null) {
                mainHandler.post(() -> {
                    imageView.setImageBitmap(bitmap);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                });
            }
        }).start();
    }

    private void applyFileIcon(ImageView icon, String name) {
        if (name.endsWith(".java")) icon.setImageDrawable(new TextIconDrawable("C", Color.parseColor("#00E5FF"), true));
        else if (name.endsWith(".xml")) icon.setImageDrawable(new TextIconDrawable("X³", Color.parseColor("#FF6D00"), false));
        else if (name.endsWith(".gradle")) icon.setImageDrawable(new TextIconDrawable("GR", Color.parseColor("#607D8B"), false));
        else icon.setImageResource(R.drawable.ic_myicon06);
    }

    // คงคลาส TextIconDrawable เดิมของคุณไว้ที่นี่...
    public static class TextIconDrawable extends Drawable {
        private final Paint paint;
        private final String text;
        private final int backgroundColor;
        private final boolean isCircle;

        public TextIconDrawable(String text, int backgroundColor, boolean isCircle) {
            this.text = text;
            this.backgroundColor = backgroundColor;
            this.isCircle = isCircle;
            this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int width = getBounds().width();
            int height = getBounds().height();
            paint.setColor(backgroundColor);
            if (isCircle) canvas.drawCircle(width / 2f, height / 2f, Math.min(width, height) / 2f, paint);
            else canvas.drawRoundRect(new RectF(0, 0, width, height), 6f, 6f, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(height * 0.4f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(text, width / 2f, (height / 2f) - ((paint.descent() + paint.ascent()) / 2f), paint);
        }
        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
