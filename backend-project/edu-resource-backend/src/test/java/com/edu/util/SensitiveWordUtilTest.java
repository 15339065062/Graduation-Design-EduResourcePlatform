package com.edu.util;

import org.junit.Assert;
import org.junit.Test;

public class SensitiveWordUtilTest {

    @Test
    public void testFilter() {
        String input = "你这个笨蛋，真是个傻瓜";
        String expected = "你这个**，真是个**";
        String result = SensitiveWordUtil.filter(input);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testContainsSensitive() {
        String input = "这是一个色情网站";
        Assert.assertTrue(SensitiveWordUtil.containsSensitive(input));
        
        String cleanInput = "这是一个教育网站";
        Assert.assertFalse(SensitiveWordUtil.containsSensitive(cleanInput));
    }
    
    @Test
    public void testNullOrEmpty() {
        Assert.assertNull(SensitiveWordUtil.filter(null));
        Assert.assertEquals("", SensitiveWordUtil.filter(""));
        Assert.assertFalse(SensitiveWordUtil.containsSensitive(null));
        Assert.assertFalse(SensitiveWordUtil.containsSensitive(""));
    }
}
