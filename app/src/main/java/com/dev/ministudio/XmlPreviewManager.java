package com.dev.ministudio;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.Stack;

public class XmlPreviewManager {

    private final Context context;

    public XmlPreviewManager(Context context) {
        this.context = context;
    }

    public View inflateXml(String xmlContent) throws Exception {
        if (TextUtils.isEmpty(xmlContent) || xmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("XML ว่างเปล่า ไม่สามารถพรีวิวได้");
        }

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xmlContent));

        View rootView = null;
        Stack<ViewGroup> parentStack = new Stack<>();

        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = getCleanTagName(parser.getName());

                View view = createViewFromTag(tagName);
                if (view != null) {
                    applyAttributes(view, parser);

                    if (rootView == null) {
                        rootView = view;
                    } else if (!parentStack.isEmpty()) {
                        parentStack.peek().addView(view);
                    }

                    if (view instanceof ViewGroup) {
                        parentStack.push((ViewGroup) view);
                    }
                }
            } 
            else if (eventType == XmlPullParser.END_TAG) {
                String tagName = getCleanTagName(parser.getName());
                if (!parentStack.isEmpty()) {
                    ViewGroup top = parentStack.peek();
                    if (tagName.equals(top.getClass().getSimpleName())) {
                        parentStack.pop();
                    }
                }
            }

            eventType = parser.next();
        }

        if (rootView == null) {
            throw new Exception("ไม่สามารถสร้าง View จาก XML ได้");
        }

        return rootView;
    }

    private String getCleanTagName(String tag) {
        if (tag == null) return "";
        if (tag.contains(".")) {
            tag = tag.substring(tag.lastIndexOf(".") + 1);
        }
        return tag;
    }

    private View createViewFromTag(String tagName) {
        switch (tagName) {
            case "LinearLayout":
                return new LinearLayout(context);
            case "FrameLayout":
                return new FrameLayout(context);
            case "RelativeLayout":
                return new RelativeLayout(context);
            case "ConstraintLayout":
                return new ConstraintLayout(context);
            case "ScrollView":
                ScrollView scrollView = new ScrollView(context);
                scrollView.setFillViewport(true);
                return scrollView;
            case "TextView":
                return new TextView(context);
            case "EditText":
                return new EditText(context);
            case "Button":
                return new Button(context);
            case "ImageView":
                return new ImageView(context);
            default:
                TextView fallback = new TextView(context);
                fallback.setText("[" + tagName + " ยังไม่รองรับ]");
                fallback.setTextColor(Color.RED);
                fallback.setPadding(16, 16, 16, 16);
                return fallback;
        }
    }

    private void applyAttributes(View view, XmlPullParser parser) {
        ViewGroup.LayoutParams params = createDefaultLayoutParams(view);

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attrName = getCleanAttributeName(parser.getAttributeName(i));
            String attrValue = parser.getAttributeValue(i);

            if (attrValue == null) continue;

            switch (attrName) {
                case "layout_width":
                    params.width = parseLayoutSize(attrValue);
                    break;
                case "layout_height":
                    params.height = parseLayoutSize(attrValue);
                    break;
                case "layout_weight":
                    if (params instanceof LinearLayout.LayoutParams) {
                        ((LinearLayout.LayoutParams) params).weight = parseFloat(attrValue, 0f);
                    }
                    break;
                case "orientation":
                    if (view instanceof LinearLayout) {
                        ((LinearLayout) view).setOrientation(
                                "vertical".equalsIgnoreCase(attrValue) ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL
                        );
                    }
                    break;
                case "gravity":
                    if (view instanceof LinearLayout) {
                        ((LinearLayout) view).setGravity(parseGravity(attrValue));
                    } else if (view instanceof TextView) {
                        ((TextView) view).setGravity(parseGravity(attrValue));
                    }
                    break;
                case "text":
                    if (view instanceof TextView) {
                        ((TextView) view).setText(attrValue);
                    }
                    break;
                case "textColor":
                    if (view instanceof TextView) {
                        try {
                            ((TextView) view).setTextColor(Color.parseColor(attrValue));
                        } catch (Exception ignored) {}
                    }
                    break;
                case "textSize":
                    if (view instanceof TextView) {
                        float size = parseSizeInSp(attrValue);
                        if (size > 0) ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
                    }
                    break;
                case "textStyle":
                    if (view instanceof TextView) {
                        setTextStyle((TextView) view, attrValue);
                    }
                    break;
                case "padding":
                    int padding = parseDimension(attrValue);
                    view.setPadding(padding, padding, padding, padding);
                    break;
                case "background":
                    try {
                        if (attrValue.startsWith("#")) {
                            view.setBackgroundColor(Color.parseColor(attrValue));
                        }
                    } catch (Exception ignored) {}
                    break;
                case "src":
                    if (view instanceof ImageView) {
                        ((ImageView) view).setImageResource(
                                attrValue.startsWith("@drawable/") 
                                    ? android.R.drawable.ic_menu_gallery 
                                    : android.R.drawable.ic_menu_report_image
                        );
                    }
                    break;
                case "scaleType":
                    if (view instanceof ImageView) {
                        setScaleType((ImageView) view, attrValue);
                    }
                    break;
                case "visibility":
                    view.setVisibility(parseVisibility(attrValue));
                    break;
            }
        }

        view.setLayoutParams(params);
    }

    private ViewGroup.LayoutParams createDefaultLayoutParams(View view) {
        if (view.getParent() instanceof LinearLayout) {
            return new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        } else if (view.getParent() instanceof RelativeLayout) {
            return new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        } else if (view.getParent() instanceof ConstraintLayout) {
            return new ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int parseLayoutSize(String value) {
        if ("match_parent".equalsIgnoreCase(value) || "fill_parent".equalsIgnoreCase(value)) {
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
        if ("wrap_content".equalsIgnoreCase(value)) {
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        return parseDimension(value);
    }

    private int parseDimension(String value) {
        if (value.endsWith("dp")) {
            try {
                float dp = Float.parseFloat(value.replace("dp", "").trim());
                return (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()
                );
            } catch (Exception e) {
                return ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        }
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private float parseSizeInSp(String value) {
        try {
            return Float.parseFloat(value.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 14f;
        }
    }

    private int parseGravity(String value) {
        int gravity = Gravity.NO_GRAVITY;
        if (value.contains("center")) gravity |= Gravity.CENTER;
        if (value.contains("left") || value.contains("start")) gravity |= Gravity.START;
        if (value.contains("right") || value.contains("end")) gravity |= Gravity.END;
        if (value.contains("top")) gravity |= Gravity.TOP;
        if (value.contains("bottom")) gravity |= Gravity.BOTTOM;
        return gravity;
    }

    private void setTextStyle(TextView textView, String style) {
        int type = 0;
        if (style.contains("bold")) type |= android.graphics.Typeface.BOLD;
        if (style.contains("italic")) type |= android.graphics.Typeface.ITALIC;
        textView.setTypeface(null, type);
    }

    private void setScaleType(ImageView imageView, String type) {
        switch (type.toLowerCase()) {
            case "center": imageView.setScaleType(ImageView.ScaleType.CENTER); break;
            case "center_crop": imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); break;
            case "fit_xy": imageView.setScaleType(ImageView.ScaleType.FIT_XY); break;
            default: imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
    }

    private int parseVisibility(String value) {
        switch (value.toLowerCase()) {
            case "gone": return View.GONE;
            case "invisible": return View.INVISIBLE;
            default: return View.VISIBLE;
        }
    }

    private float parseFloat(String value, float defaultValue) {
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getCleanAttributeName(String attr) {
        if (attr.contains(":")) {
            return attr.substring(attr.lastIndexOf(":") + 1);
        }
        return attr;
    }
}