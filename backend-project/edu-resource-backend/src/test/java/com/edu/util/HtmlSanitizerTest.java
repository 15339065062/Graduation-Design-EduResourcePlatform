package com.edu.util;

import org.junit.Assert;
import org.junit.Test;

public class HtmlSanitizerTest {
    @Test
    public void escapesHtml() {
        String input = "<script>alert('x')</script>&\"";
        String out = HtmlSanitizer.sanitizePlainText(input);
        Assert.assertEquals("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;&amp;&quot;", out);
    }

    @Test
    public void trimsAndLimitsLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3000; i++) sb.append('a');
        String out = HtmlSanitizer.sanitizePlainText(sb.toString());
        Assert.assertEquals(2000, out.length());
    }
}

