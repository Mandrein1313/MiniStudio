package com.dev.ministudio;

import android.content.Context;
import android.content.res.Resources;
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

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.Stack;

public class XmlPreviewManager {

    private final Context context;
    private final ResourceResolver resourceResolver;

    public XmlPreviewManager(Context context) {
        this.context = context;
        this.resourceResolver = new ResourceResolver(context);
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
                if (!parentStack.isEmpty() && 
                    tagName.equals(parentStack.peek().getClass().getSimpleName())) {
                    parentStack.pop();
                }
            }

            eventType = parser.next();
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
            case "LinearLayout": return new LinearLayout(context);
            case "FrameLayout": return new FrameLayout(context);
            case "RelativeLayout": return new RelativeLayout(context);
            case "ConstraintLayout": return new ConstraintLayout(context);
            case "ScrollView": 
                ScrollView sv = new ScrollView(context);
                sv.setFillViewport(true);
                return sv;
            case "RecyclerView":
                RecyclerView rv = new RecyclerView(context);
                rv.setLayoutManager(new LinearLayoutManager(context));
                return rv;
            case "CardView":
                return new CardView(context);
            case "TextView": return new TextView(context);
            case "EditText": return new EditText(context);
            case "Button": return new Button(context);
            case "ImageView": return new ImageView(context);

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
                                "vertical".equalsIgnoreCase(attrValue) ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
                    }
                    break;

                case "text":
                    if (view instanceof TextView) {
                        ((TextView) view).setText(resourceResolver.resolveString(attrValue));
                    }
                    break;

                case "textColor":
                    if (view instanceof TextView) {
                        ((TextView) view).setTextColor(resourceResolver.resolveColor(attrValue));
                    }
                    break;

                case "textSize":
                    if (view instanceof TextView) {
                        float size = resourceResolver.resolveDimension(attrValue);
                        if (size > 0) {
                            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
                        }
                    }
                    break;

                case "background":
                    if (attrValue.startsWith("#")) {
                        try {
                            view.setBackgroundColor(Color.parseColor(attrValue));
                        } catch (Exception ignored) {}
                    } else {
                        view.setBackgroundColor(resourceResolver.resolveColor(attrValue));
                    }
                    break;

                case "padding":
                    int padding = resourceResolver.resolveDimensionPx(attrValue);
                    view.setPadding(padding, padding, padding, padding);
                    break;

                case "src":
                    if (view instanceof ImageView) {
                        ((ImageView) view).setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                    break;

                case "cardCornerRadius":
                    if (view instanceof CardView) {
                        ((CardView) view).setRadius(resourceResolver.resolveDimensionPx(attrValue));
                    }
                    break;

                case "cardElevation":
                    if (view instanceof CardView) {
                        ((CardView) view).setCardElevation(resourceResolver.resolveDimensionPx(attrValue));
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
        return resourceResolver.resolveDimensionPx(value);
    }

    private int parseVisibility(String value) {
        switch (value.toLowerCase()) {
            case "gone": return View.GONE;
            case "invisible": return View.INVISIBLE;
            default: return View.VISIBLE;
        }
    }

    private float parseFloat(String value, float defaultValue) {
        try { return Float.parseFloat(value); } catch (Exception e) { return defaultValue; }
    }

    private String getCleanAttributeName(String attr) {
        if (attr == null) return "";
        return attr.contains(":") ? attr.substring(attr.lastIndexOf(":") + 1) : attr;
    }

    // ====================== RESOURCE RESOLVER ======================
    private static class ResourceResolver {
        private final Context context;
        private final Resources resources;

        ResourceResolver(Context context) {
            this.context = context;
            this.resources = context.getResources();
        }

        String resolveString(String value) {
            if (value == null) return "";
            if (value.startsWith("@string/")) {
                String name = value.substring(8);
                try {
                    int id = resources.getIdentifier(name, "string", context.getPackageName());
                    return id != 0 ? resources.getString(id) : value;
                } catch (Exception e) {
                    return value;
                }
            }
            return value;
        }

        int resolveColor(String value) {
            if (value == null) return Color.BLACK;
            if (value.startsWith("#")) {
                try {
                    return Color.parseColor(value);
                } catch (Exception e) {
                    return Color.BLACK;
                }
            }
            if (value.startsWith("@color/")) {
                String name = value.substring(7);
                try {
                    int id = resources.getIdentifier(name, "color", context.getPackageName());
                    return id != 0 ? resources.getColor(id) : Color.BLACK;
                } catch (Exception e) {
                    return Color.BLACK;
                }
            }
            return Color.BLACK;
        }

        float resolveDimension(String value) {
            if (value.startsWith("@dimen/")) {
                String name = value.substring(7);
                try {
                    int id = resources.getIdentifier(name, "dimen", context.getPackageName());
                    return id != 0 ? resources.getDimension(id) : 0f;
                } catch (Exception e) {
                    return 0f;
                }
            }
            return parseFloatDimension(value);
        }

        int resolveDimensionPx(String value) {
            float dim = resolveDimension(value);
            return (int) dim;
        }

        private float parseFloatDimension(String value) {
            try {
                if (value.endsWith("dp")) {
                    float dp = Float.parseFloat(value.replace("dp", "").trim());
                    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
                }
                if (value.endsWith("sp")) {
                    float sp = Float.parseFloat(value.replace("sp", "").trim());
                    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.getDisplayMetrics());
                }
                return Float.parseFloat(value);
            } catch (Exception e) {
                return 0f;
            }
        }
    }
}