package com.dev.ministudio;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiHtmlFormatter {

    public static String convertMarkdownToHtml(String markdownText) {
        // แตกประโยคและเปลี่ยนกล่องโค้ด ```java ... ``` ให้เป็นโครงสร้าง HTML พร้อมปุ่ม Copy
        String htmlContent = markdownText;
        
        // ตัวแปลงไฮไลต์หัวข้อหรือข้อความหนาเบื้องต้น
        htmlContent = htmlContent.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        
        // ค้นหาบล็อกโค้ด ``` เพื่อสร้างกล่องที่มีปุ่มคัดลอก
        Pattern codeBlockPattern = Pattern.compile("```(\\w*)\\n(.*?)\\n```", Pattern.DOTALL);
        Matcher matcher = codeBlockPattern.matcher(htmlContent);
        StringBuffer sb = new StringBuffer();
        int idCounter = 0;
        
        while (matcher.find()) {
            String lang = matcher.group(1);
            String code = matcher.group(2)
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
            
            String uniqueId = "code_" + idCounter;
            
            // สร้างกล่องโค้ดธีมมืดสุดเท่ พร้อมปุ่ม Copy ด้านขวาบน
            String blockHtml = "<div class='code-container'>" +
                    "  <div class='code-header'>" +
                    "    <span>" + (lang.isEmpty() ? "code" : lang) + "</span>" +
                    "    <button onclick=\"copyToClipboard('" + uniqueId + "', this)\">Copy</button>" +
                    "  </div>" +
                    "  <pre id='" + uniqueId + "'>" + code + "</pre>" +
                    "</div>";
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(blockHtml));
            idCounter++;
        }
        matcher.appendTail(sb);
        htmlContent = sb.toString();
        
        // แทนที่การขึ้นบรรทัดใหม่ให้เป็น <br> ในส่วนที่เป็นข้อความธรรมดา
        htmlContent = htmlContent.replace("\n", "<br>");

        // ประกอบโครงสร้างหน้าเว็บ แม่แบบ CSS สไตล์เดียวกับ IDE สวยงาม
        return "<html><head><style>" +
                "body { background-color: #1E1E1E; color: #E0E0E0; font-family: sans-serif; padding: 10px; font-size: 14px; line-height: 1.5; }" +
                "strong { color: #FFB74D; }" +
                ".code-container { background-color: #2D2D2D; border: 1px solid #3E3E3E; border-radius: 6px; margin: 12px 0; overflow: hidden; }" +
                ".code-header { background-color: #252526; padding: 6px 12px; display: flex; justify-content: space-between; align-items: center; color: #858585; font-size: 11px; font-family: monospace; border-bottom: 1px solid #3E3E3E; }" +
                ".code-header button { background: #3E3E3E; color: #D4D4D4; border: none; padding: 4px 8px; border-radius: 4px; cursor: pointer; font-size: 11px; }" +
                ".code-header button:active { background: #555555; }" +
                "pre { margin: 0; padding: 12px; overflow-x: auto; font-family: monospace; font-size: 13px; color: #9CDCFE; background-color: #1E1E1E; white-space: pre; }" +
                "</style>" +
                "<script>" +
                "function copyToClipboard(elementId, btn) {" +
                "  var text = document.getElementById(elementId).innerText;" +
                "  var elem = document.createElement('textarea');" +
                "  document.body.appendChild(elem);" +
                "  elem.value = text;" +
                "  elem.select();" +
                "  document.execCommand('copy');" +
                "  document.body.removeChild(elem);" +
                "  btn.innerText = 'Copied!';" +
                "  setTimeout(function() { btn.innerText = 'Copy'; }, 2000);" +
                "}" +
                "</script></head><body>" + htmlContent + "</body></html>";
    }
}
