package com.dev.ministudio;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.StringReader;
import java.util.Stack;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class XmlPreviewManager {

    private final Context context;

    public XmlPreviewManager(Context context) {
        this.context = context;
    }

    /**
     * 🚀 เมทอดหลัก: แปลง XML ดิบให้กลายเป็น View จริง รองรับโครงสร้างซ้อนกันลึกไม่จำกัดชั้นด้วย Stack
     */
    public View inflateXml(String xmlContent) throws Exception {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("ซอร์สโค้ด XML ว่างเปล่า ไม่สามารถพรีวิวได้");
        }

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xmlContent));

        View rootLayout = null;
        // 🌟 ใช้ Stack ในการเก็บลำดับชั้น Parent เพื่อป้องกันโครงสร้างเลย์เอาต์พังพินาศเวลาซ้อนกันลึก ๆ
        Stack<ViewGroup> parentStack = new Stack<>();
        
        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();

            switch (eventType) {
                case XmlPullParser.START_TAG:
                    // ล้างคำนำหน้าพวกพาร์ทแอนดรอยด์ออก (ถ้ามี)
                    if (tagName != null && tagName.contains(".")) {
                        tagName = tagName.substring(tagName.lastIndexOf(".") + 1);
                    }

                    View newView = createViewFromTagName(tagName);
                    if (newView != null) {
                        // 🎨 สกัดคุณลักษณะต่างๆ นำมาประยุกต์ใช้กับวัตถุตัวนี้
                        applyAttributes(newView, parser);

                        if (rootLayout == null) {
                            // แท็กแรกสุดที่เจอคือรากฐานของหน้าจอ (Root View)
                            rootLayout = newView; 
                        } else if (!parentStack.isEmpty()) {
                            // ถ้ามีตัวก่อนหน้าเป็นกลุ่มก้อน ให้ยัดตัวใหม่เข้าไปเป็นลูก (Child)
                            parentStack.peek().addView(newView);
                        }

                        // ถ้ายอดวิวตัวใหม่นี้เป็นกลุ่ม Layout (ViewGroup) ให้ดันเข้า Stack รอรับลูกชั้นถัดไป
                        if (newView instanceof ViewGroup) {
                            parentStack.push((ViewGroup) newView);
                        }
                    }
                    break;

                case XmlPullParser.END_TAG:
                    // 🌟 เมื่อเจอแท็กปิดของกลุ่ม Layout ให้ดึงออกจาก Stack ถอยกลับขึ้นไป 1 ระดับชั้นอย่างแม่นยำ
                    if (tagName != null && !parentStack.isEmpty()) {
                        View currentTop = parentStack.peek();
                        String currentTopName = currentTop.getClass().getSimpleName();
                        if (tagName.equals(currentTopName)) {
                            parentStack.pop();
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }

        return rootLayout;
    }

    /**
     * 🛠️ คัดแยกสายพันธุ์แท็ก XML เพื่อแปลงร่างกลายเป็น Object บนภาษา Java
     */
    private View createViewFromTagName(String tagName) {
        if (tagName == null) return null;

        switch (tagName) {
            case "LinearLayout":
                return new LinearLayout(context);
            case "TextView":
                return new TextView(context);
            case "Button":
                return new Button(context);
            case "ImageView":
                return new ImageView(context);
            default:
                // แผนสำรอง: แจ้งเตือนไอคอนที่ยังไม่เปิดระบบ
                TextView fallback = new TextView(context);
                fallback.setText("[" + tagName + " ยังไม่รองรับพรีวิวเวอร์ชันนี้]");
                fallback.setTextColor(Color.RED);
                fallback.setPadding(10, 10, 10, 10);
                return fallback;
        }
    }

    /**
     * 🎨 สกัดและพ่นแอตทริบิวต์ลงบนวัตถุ View (รวมถึงการตั้งค่า Layout พิ้นฐาน)
     */
    private void applyAttributes(View view, XmlPullParser parser) {
        // ค่าเริ่มต้นกว้างยาวเป็น WRAP_CONTENT ป้องกันหน้าจอค้างหรือเอ๋อ
        int width = ViewGroup.LayoutParams.WRAP_CONTENT;
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        float weight = 0f;

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attrName = parser.getAttributeName(i);
            String attrValue = parser.getAttributeValue(i);

            if (attrName == null || attrValue == null) continue;

            // คลีนชื่อเผื่อติดเนมสเปซ android: มาด้วย
            if (attrName.contains(":")) {
                attrName = attrName.substring(attrName.indexOf(":") + 1);
            }

            switch (attrName) {
                case "layout_width":
                    width = parseLayoutSize(attrValue);
                    break;
                case "layout_height":
                    height = parseLayoutSize(attrValue);
                    break;
                case "layout_weight":
                    try {
                        weight = Float.parseFloat(attrValue);
                    } catch (Exception ignored) {}
                    break;
                case "orientation":
                    if (view instanceof LinearLayout) {
                        ((LinearLayout) view).setOrientation(
                                "vertical".equalsIgnoreCase(attrValue) ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL
                        );
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
                        if (size > 0) {
                            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
                        }
                    }
                    break;
                case "background":
                    try {
                        if (attrValue.startsWith("#")) {
                            view.setBackgroundColor(Color.parseColor(attrValue));
                        }
                    } catch (Exception ignored) {}
                    break;
                case "src":
                    // 🖼️ รองรับดึงรูปภาพจำลองพื้นฐานจากระบบ Android ติดเครื่องมาใช้งานก่อน
                    if (view instanceof ImageView) {
                        ImageView iv = (ImageView) view;
                        if (attrValue.startsWith("@drawable/")) {
                            // อนาคตสามารถเขียนโค้ดผูกไปดึงพาธไฟล์จริงในโฟลเดอร์ของท่านมาใส่ตรงนี้ได้เลยครับ!
                            iv.setImageResource(android.R.drawable.ic_menu_gallery); 
                        } else {
                            iv.setImageResource(android.R.drawable.ic_menu_report_image);
                        }
                    }
                    break;
            }
        }

        // ประกอบร่าง Layout ทั้งในส่วน กว้าง, ยาว และ น้ำหนักจัดวางให้เข้าที่
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        if (weight > 0) {
            params.weight = weight;
        }
        view.setLayoutParams(params);
    }

    private int parseLayoutSize(String value) {
        if ("match_parent".equalsIgnoreCase(value) || "fill_parent".equalsIgnoreCase(value)) {
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
        if ("wrap_content".equalsIgnoreCase(value)) {
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        if (value.endsWith("dp")) {
            try {
                int dpVal = Integer.parseInt(value.replaceAll("[^0-9]", ""));
                return (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, dpVal, context.getResources().getDisplayMetrics()
                );
            } catch (Exception ignored) {}
        }
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private float parseSizeInSp(String value) {
        try {
            return Float.parseFloat(value.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return -1;
        }
    }
}
