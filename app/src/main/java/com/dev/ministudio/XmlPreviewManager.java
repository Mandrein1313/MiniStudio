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

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.Stack;

public class XmlPreviewManager {

    private final Context context;

    public XmlPreviewManager(Context context) {
        this.context = context;
    }

    /**
     * 🌟 เมทอดหลักสำหรับแปลง XML String ให้กลายเป็น View Hierarchy จริง
     */
    public View inflateXml(String xmlContent) {
        if (TextUtils.isEmpty(xmlContent) || xmlContent.trim().isEmpty()) {
            return createErrorView("ซอร์สโค้ด XML ว่างเปล่า ไม่สามารถพรีวิวได้");
        }

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlContent));

            View rootView = null;
            // ใช้ Stack ควบคุมระดับชั้น Parent-Child เพื่อป้องกันโครงสร้างเลย์เอาต์พังพินาศเวลาซ้อนกันลึกๆ
            Stack<ViewGroup> parentStack = new Stack<>();

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = getCleanTagName(parser.getName());

                    // สร้าง View ตามชนิดของแท็ก
                    View view = createViewFromTag(tagName, parser);
                    if (view != null) {
                        // ประมวลผลและใส่คุณลักษณะต่างๆ (Attributes)
                        applyAttributes(view, parser);

                        // ล็อก Root View ตัวแรกสุด
                        if (rootView == null) {
                            rootView = view;
                        } else if (!parentStack.isEmpty()) {
                            // ยัด View ลูกเข้าไปใน Parent ตัวปัจจุบันที่อยู่บนสุดของ Stack
                            parentStack.peek().addView(view);
                        }

                        // ถ้า View ตัวนี้เป็นกลุ่มก้อนที่บรรจุลูกได้ (ViewGroup) ให้ดันเข้า Stack
                        if (view instanceof ViewGroup) {
                            parentStack.push((ViewGroup) view);
                        }
                    }
                } 
                else if (eventType == XmlPullParser.END_TAG) {
                    String tagName = getCleanTagName(parser.getName());
                    if (!parentStack.isEmpty()) {
                        String currentTopName = parentStack.peek().getClass().getSimpleName();
                        
                        // ป้องกันกรณีแท็กแปลงร่าง ให้ตรวจสอบโครงสร้างหลักแล้ว Pop ออกอย่างแม่นยำ
                        if (tagName.equals(currentTopName) || 
                            (parentStack.peek() instanceof LinearLayout && tagName.toLowerCase().contains("layout")) ||
                            (parentStack.peek() instanceof LinearLayout && tagName.equalsIgnoreCase("spinner"))) {
                            parentStack.pop();
                        }
                    }
                }

                eventType = parser.next();
            }

            return rootView != null ? rootView : createErrorView("ไม่พบ Root View ในไฟล์ XML นี้");

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorView("เกิดข้อผิดพลาดในการพรีวิว:\n" + e.getMessage());
        }
    }

    /**
     * 🛠️ แผนกคัดแยกสายพันธุ์แท็ก XML เพื่อแปลงร่างเป็นอ็อบเจกต์จริงบนแอนดรอยด์
     */
    private View createViewFromTag(String tagName, XmlPullParser parser) {
        String lowerTag = tagName.toLowerCase();

        switch (tagName) {
            case "LinearLayout": 
                return new LinearLayout(context);
            case "FrameLayout": 
                return new FrameLayout(context);
            case "RelativeLayout": 
                return new RelativeLayout(context);
            case "ConstraintLayout": 
                return new ConstraintLayout(context);
            case "CardView": 
                return new CardView(context);
                
            case "ScrollView":
                ScrollView sv = new ScrollView(context);
                sv.setFillViewport(true);
                return sv;
                
            case "DrawerLayout":
                // 🟢 แปลงโครงสร้าง DrawerLayout ให้เป็น LinearLayout พรีวิว เพื่อให้เรนเดอร์เนื้อหาข้างในได้ทันที ไม่ขึ้นตัวแดง
                LinearLayout dlSim = new LinearLayout(context);
                dlSim.setOrientation(LinearLayout.VERTICAL);
                return dlSim;

            case "TextView": 
                return new TextView(context);
                
            case "EditText": 
                EditText editText = new EditText(context);
                editText.setHint("ช่องกรอกข้อมูล...");
                editText.setHintTextColor(Color.GRAY);
                return editText;
                
            case "Button": 
                return new Button(context);
                
            case "ImageView": 
                return new ImageView(context);

            default:
                // 🚀 ระบบ Adaptive Fallback: ถ้าเจอพวก Spinner, RecyclerView หรือแท็กที่ยังไม่รองรับ 
                // ให้แปลงร่างมันเป็น LinearLayout เพื่อรองรับการใส่แอตทริบิวต์และแสดงผลจำลองทันที หน้าจอจะไม่เอ๋ออีกต่อไป
                if (lowerTag.contains("spinner") || lowerTag.contains("view")) {
                    LinearLayout adaptiveLayout = new LinearLayout(context);
                    adaptiveLayout.setOrientation(LinearLayout.HORIZONTAL);
                    adaptiveLayout.setGravity(Gravity.CENTER_VERTICAL);
                    adaptiveLayout.setBackgroundColor(0x1AFFFFFF); // ใส่พื้นหลังโปร่งแสงจางๆ ให้รู้ว่าเป็นคอมโพเนนต์จำลอง
                    adaptiveLayout.setPadding(20, 20, 20, 20);
                    
                    // ใส่ข้อความบอกสัญลักษณ์จำลองข้างใน
                    TextView textLabel = new TextView(context);
                    textLabel.setText(" ❖ [" + tagName + "]");
                    textLabel.setTextColor(Color.LTGRAY);
                    textLabel.setTextSize(13);
                    adaptiveLayout.addView(textLabel);
                    
                    return adaptiveLayout;
                }
                
                // สำหรับแท็กคอนเทนเนอร์ทั่วไปที่หลุดมา
                LinearLayout defaultContainer = new LinearLayout(context);
                defaultContainer.setOrientation(LinearLayout.VERTICAL);
                return defaultContainer;
        }
    }

    /**
     * 🎨 สกัดและแจกจ่ายแอตทริบิวต์ (คุณลักษณะ) ต่างๆ ลงบน View
     */
    private void applyAttributes(View view, XmlPullParser parser) {
        int width = ViewGroup.LayoutParams.WRAP_CONTENT;
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        float weight = 0f;
        int gravityValue = Gravity.NO_GRAVITY;

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attrName = getCleanAttributeName(parser.getAttributeName(i));
            String attrValue = parser.getAttributeValue(i);
            if (attrValue == null) continue;

            switch (attrName) {
                case "layout_width":
                    width = parseLayoutSize(attrValue);
                    break;
                case "layout_height":
                    height = parseLayoutSize(attrValue);
                    break;
                case "layout_weight":
                    try { weight = Float.parseFloat(attrValue); } catch (Exception ignored) {}
                    break;

                case "orientation":
                    if (view instanceof LinearLayout ll) {
                        ll.setOrientation("vertical".equalsIgnoreCase(attrValue) 
                                ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
                    }
                    break;

                case "gravity":
                    gravityValue = parseGravity(attrValue);
                    if (view instanceof LinearLayout ll) {
                        ll.setGravity(gravityValue);
                    } else if (view instanceof TextView tv) {
                        tv.setGravity(gravityValue);
                    }
                    break;

                case "text":
                    if (view instanceof TextView tv) {
                        tv.setText(attrValue);
                    }
                    break;

                case "textColor":
                    if (view instanceof TextView tv) {
                        try { tv.setTextColor(Color.parseColor(attrValue)); } catch (Exception ignored) {}
                    }
                    break;

                case "textSize":
                    if (view instanceof TextView tv) {
                        try {
                            float size = Float.parseFloat(attrValue.replaceAll("[^0-9.]", ""));
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
                        } catch (Exception ignored) {}
                    }
                    break;

                case "background":
                    try {
                        if (attrValue.startsWith("#")) {
                            view.setBackgroundColor(Color.parseColor(attrValue));
                        }
                    } catch (Exception ignored) {}
                    break;

                case "padding":
                    int p = parseDimensionPx(attrValue);
                    view.setPadding(p, p, p, p);
                    break;

                case "src":
                    if (view instanceof ImageView iv) {
                        iv.setImageResource(android.R.drawable.ic_menu_gallery); // ใช้รูปแกลเลอรีระบบจำลอง
                    }
                    break;

                case "hint":
                    if (view instanceof EditText et) {
                        et.setHint(attrValue);
                    }
                    break;
            }
        }

        // เซ็ต LayoutParams ให้วัตถุ
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        if (weight > 0) {
            lp.weight = weight;
        }
        if (gravityValue != Gravity.NO_GRAVITY) {
            lp.gravity = gravityValue;
        }
        view.setLayoutParams(lp);
    }

    // ==================== วิธีจัดการและคำนวณ Helper Methods ====================

    private int parseLayoutSize(String value) {
        if ("match_parent".equalsIgnoreCase(value) || "fill_parent".equalsIgnoreCase(value)) {
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
        if ("wrap_content".equalsIgnoreCase(value)) {
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        return parseDimensionPx(value);
    }

    private int parseDimensionPx(String value) {
        try {
            int num = Integer.parseInt(value.replaceAll("[^0-9]", ""));
            if (value.endsWith("dp") || value.endsWith("dip")) {
                return (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, num, context.getResources().getDisplayMetrics());
            }
            if (value.endsWith("sp")) {
                return (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP, num, context.getResources().getDisplayMetrics());
            }
            return num;
        } catch (Exception e) {
            return 0;
        }
    }

    private int parseGravity(String value) {
        int gravity = Gravity.NO_GRAVITY;
        if (value == null) return gravity;
        String[] parts = value.toLowerCase().split("\\|");
        for (String part : parts) {
            switch (part.trim()) {
                case "center": gravity |= Gravity.CENTER; break;
                case "center_vertical": gravity |= Gravity.CENTER_VERTICAL; break;
                case "center_horizontal": gravity |= Gravity.CENTER_HORIZONTAL; break;
                case "top": gravity |= Gravity.TOP; break;
                case "bottom": gravity |= Gravity.BOTTOM; break;
                case "left": gravity |= Gravity.LEFT; break;
                case "right": gravity |= Gravity.RIGHT; break;
            }
        }
        return gravity;
    }

    private String getCleanTagName(String tag) {
        if (tag == null) return "";
        return tag.contains(".") ? tag.substring(tag.lastIndexOf(".") + 1) : tag;
    }

    private String getCleanAttributeName(String attr) {
        if (attr == null) return "";
        return attr.contains(":") ? attr.substring(attr.lastIndexOf(":") + 1) : attr;
    }

    private View createErrorView(String message) {
        TextView errorView = new TextView(context);
        errorView.setText("❌ " + message);
        errorView.setTextColor(Color.RED);
        errorView.setBackgroundColor(0x33FF0000);
        errorView.setPadding(32, 48, 32, 48);
        errorView.setTextSize(14);
        errorView.setGravity(Gravity.CENTER);
        return errorView;
    }
}
